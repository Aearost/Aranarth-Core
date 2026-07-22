const { EmbedBuilder } = require('discord.js');
const config = require('../config');
const pendingReviewManager = require('../utils/pendingReviewManager');
const TEMPLATES = require('../forms/templates');
const { createIssue } = require('../github/githubManager');
const { refreshWorkQueue } = require('./workQueueHandler');
const { createForumThread } = require('./forumHandler');

const EMOJI_TO_PRIORITY = {
  [config.PRIORITY_EMOJIS.P1]: 'P1',
  [config.PRIORITY_EMOJIS.P2]: 'P2',
  [config.PRIORITY_EMOJIS.P3]: 'P3',
  [config.PRIORITY_EMOJIS.P4]: 'P4',
};

const PRIORITY_NAMES = {
  P1: 'Critical Priority',
  P2: 'High Priority',
  P3: 'Medium Priority',
  P4: 'Low Priority / Backlog',
};

const TYPE_DISPLAY = {
  BUG: 'Bug Report',
  IDEA: 'Idea Suggestion',
  ABILITY: 'Ability Suggestion',
};

const CONFIRM_EMOJI = '✅';
const GO_BACK_EMOJI = '⬅️';
const EDIT_CANCEL_EMOJI = '❌';

// userId → { stage: 'awaiting_reason', reviewMessageId, pendingData, submitterUser, typeName, promptMessageId, reason, confirmMessageId }
const pendingRejections = new Map();

// userId → { stage: 'awaiting_field'|'awaiting_value', reviewMessageId, originalPendingData, pendingData, promptMessageId, fieldIndex }
const pendingEdits = new Map();

// messageIds currently being edited — blocks priority/reject reactions from claiming mid-edit
const editingMessages = new Set();

// ── Helpers ────────────────────────────────────────────────────────────────

/**
 * Posts a review embed to the channel with all reaction options.
 * Used both on initial post (via reactionHandler) and after an edit.
 */
async function postReviewMessage(channel, pending) {
  const typeDisplayMap = { BUG: 'Bug Report', IDEA: 'Idea Suggestion', ABILITY: 'Ability Suggestion' };
  const typeName = typeDisplayMap[pending.type] || pending.type;
  const template = TEMPLATES[pending.type];

  const embed = new EmbedBuilder()
    .setTitle(`📥 New ${typeName} — Pending Review`)
    .setDescription(`**Title:** \`${pending.title}\`\nSubmitted by **${pending.displayName}**`)
    .addFields(pending.embedFields)
    .setColor(template.color)
    .setFooter({ text: 'React with a priority to approve, 📝 to edit, or ❌ to reject' })
    .setTimestamp();

  const msg = await channel.send({ embeds: [embed] });
  await msg.react(config.PRIORITY_EMOJIS.P1);
  await msg.react(config.PRIORITY_EMOJIS.P2);
  await msg.react(config.PRIORITY_EMOJIS.P3);
  await msg.react(config.PRIORITY_EMOJIS.P4);
  await msg.react(config.PRIORITY_EMOJIS.EDIT);
  await msg.react(config.PRIORITY_EMOJIS.REJECT);
  return msg;
}

/**
 * Reconstructs pending review data from the embed of a review message.
 * Used as a fallback when a message is not found in pendingReviewManager
 * (e.g. messages created before tracking was in place, or after a data loss).
 * userId will be null — DM to submitter on approval will be skipped.
 */
function reconstructPendingFromEmbed(message) {
  const embed = message.embeds?.[0];
  if (!embed) return null;

  // Parse type from embed title: "📥 New Bug Report — Pending Review"
  let type, label;
  const embedTitle = embed.title || '';
  if (embedTitle.includes('Bug Report'))        { type = 'BUG';     label = 'BUG'; }
  else if (embedTitle.includes('Idea Suggestion'))    { type = 'IDEA';    label = 'IDEA'; }
  else if (embedTitle.includes('Ability Suggestion')) { type = 'ABILITY'; label = 'ABILITY'; }
  else return null;

  const template = TEMPLATES[type];
  const desc = embed.description || '';

  // Parse issue title from: "**Title:** `[BUG] The Title`"
  const titleMatch = desc.match(/\*\*Title:\*\* `([^`]+)`/);
  const issueTitle = titleMatch ? titleMatch[1] : '[Unknown Title]';

  // Parse submitter display name from: "Submitted by **Name**"
  const submittedByMatch = desc.match(/Submitted by \*\*(.+?)\*\*/);
  const displayName = submittedByMatch ? submittedByMatch[1] : 'Unknown';

  // Strip type prefix to get raw title: "[BUG] Title" → "Title"
  const rawTitleMatch = issueTitle.match(/^\[[^\]]+\]\s*(.+)$/);
  const rawTitle = rawTitleMatch ? rawTitleMatch[1] : issueTitle;

  // Reconstruct answers from numbered embed fields: "1. Title", "2. Explanation", ...
  const answers = {};
  const embedFields = [];
  for (const field of (embed.fields || [])) {
    const numMatch = field.name.match(/^(\d+)\.\s+/);
    if (numMatch) {
      const idx = parseInt(numMatch[1], 10) - 1;
      const question = template.questions[idx];
      if (question) {
        answers[question.key] = field.value === '*Not answered*' ? '' : field.value;
      }
      embedFields.push({ name: field.name, value: field.value });
    }
  }

  const body = template.buildBody(answers, displayName, []);

  return { userId: null, type, label, title: issueTitle, rawTitle, body, embedFields, displayName, answers, screenshots: [] };
}

// ── Reaction handler ───────────────────────────────────────────────────────

async function handle(reaction, user, client) {
  const { message, emoji } = reaction;
  const emojiName = emoji.name;

  // Council only
  const guild = message.guild;
  const member = await guild.members.fetch(user.id).catch(() => null);
  if (!member || !member.roles.cache.has(config.COUNCIL_ROLE_ID)) return;

  try { await reaction.users.remove(user.id); } catch { /* ignore */ }

  // ── Check if this is a rejection confirmation reaction ──
  const pendingRejection = pendingRejections.get(user.id);
  if (
    pendingRejection &&
    pendingRejection.stage === 'awaiting_confirm' &&
    pendingRejection.confirmMessageId === message.id
  ) {
    await handleRejectionConfirmation(reaction, user, client, pendingRejection, emojiName);
    return;
  }

  // ── Edit: prompt field selection ──
  // Handled before the claim so the pending review is NOT removed from disk until confirmed.
  if (emojiName === config.PRIORITY_EMOJIS.EDIT) {
    const editPending = pendingReviewManager.get(message.id) ?? reconstructPendingFromEmbed(message);
    if (!editPending || editingMessages.has(message.id)) return;

    editingMessages.add(message.id);
    const template = TEMPLATES[editPending.type];
    pendingEdits.set(user.id, {
      stage: 'awaiting_field',
      reviewMessageId: message.id,
      originalPendingData: JSON.parse(JSON.stringify(editPending)),
      pendingData: JSON.parse(JSON.stringify(editPending)),
      promptMessageId: null,
      fieldIndex: null,
    });

    const fieldList = template.questions.map((q, i) =>
      `**${i + 1}.** ${q.label}${q.hiddenFromFields ? ' *(encoded in title)*' : ''}`
    ).join('\n');
    const prompt = await message.channel.send({
      embeds: [
        new EmbedBuilder()
          .setTitle('✏️ Edit Submission')
          .setDescription(
            `<@${user.id}> — Which field would you like to edit? Reply with its number.\n\n` +
            `${fieldList}\n\nType \`cancel\` to abort.`
          )
          .setColor(config.COLORS.WARNING),
      ],
    });

    const pe = pendingEdits.get(user.id);
    if (pe) pe.promptMessageId = prompt.id;
    return;
  }

  // ── Retrieve and atomically claim the pending review ──
  // Guard: don't claim a message that's currently being edited
  if (editingMessages.has(message.id)) return;
  const pending = pendingReviewManager.get(message.id) ?? reconstructPendingFromEmbed(message);
  if (!pending) return;
  pendingReviewManager.remove(message.id); // no-op if not tracked, safe to call

  const submitterUser = await client.users.fetch(pending.userId).catch(() => null);
  const typeName = TYPE_DISPLAY[pending.type] || pending.type;

  // ── Reject: prompt for reason ──
  if (emojiName === config.PRIORITY_EMOJIS.REJECT) {
    pendingRejections.set(user.id, {
      stage: 'awaiting_reason',
      reviewMessageId: message.id,
      pendingData: pending,
      submitterUser,
      typeName,
      promptMessageId: null,
      reason: null,
      confirmMessageId: null,
    });

    const prompt = await message.channel.send({
      embeds: [
        new EmbedBuilder()
          .setDescription(
            `<@${user.id}> — Please provide a **reason for rejecting** this submission.\n` +
            `> \`${pending.title}\`\n\n` +
            `Type \`cancel\` to abort and restore the submission.`
          )
          .setColor(config.COLORS.WARNING),
      ],
    });

    const pr = pendingRejections.get(user.id);
    if (pr) pr.promptMessageId = prompt.id;
    return;
  }

  // ── Approve with priority ──
  const priority = EMOJI_TO_PRIORITY[emojiName];
  if (!priority) {
    pendingReviewManager.add(message.id, pending);
    return;
  }

  let issueData;
  try {
    issueData = await createIssue(pending.title, pending.body, [pending.label, priority]);
  } catch (err) {
    console.error('[PriorityHandler] Failed to create GitHub issue:', err.message);
    pendingReviewManager.add(message.id, pending);
    try {
      await message.channel.send({
        embeds: [
          new EmbedBuilder()
            .setDescription('❌ Failed to create the GitHub issue. Please try reacting again.')
            .setColor(config.COLORS.ERROR),
        ],
      });
    } catch { /* ignore */ }
    return;
  }

  try {
    if (submitterUser) {
      await submitterUser.send({
        embeds: [
          new EmbedBuilder()
            .setTitle('✅ Submission Approved!')
            .setDescription(
              `Your **${typeName}** has been reviewed and approved by the Council!\n\n` +
              `It has been classified as a **${priority} — ${PRIORITY_NAMES[priority]}**.\n\n` +
              `🔗 [Track your issue on GitHub](${issueData.url})`
            )
            .setColor(config.COLORS.SUCCESS)
            .setTimestamp(),
        ],
      });
    }
  } catch { /* DMs may be closed */ }

  try { await message.delete(); } catch { /* ignore */ }

  try {
    await createForumThread(client, issueData.number, pending.title, pending.embedFields, pending.type, priority);
  } catch (err) {
    console.error('[PriorityHandler] Failed to create forum thread:', err.message);
  }

  try {
    await refreshWorkQueue(client);
  } catch (err) {
    console.error('[PriorityHandler] Failed to refresh work queue:', err.message);
  }
}

// ── Rejection confirmation ─────────────────────────────────────────────────

async function handleRejectionConfirmation(reaction, user, client, pr, emojiName) {
  try {
    const confirmMsg = await reaction.message.channel.messages.fetch(pr.confirmMessageId).catch(() => null);
    if (confirmMsg) await confirmMsg.delete();
  } catch { /* ignore */ }

  pendingRejections.delete(user.id);

  if (emojiName === GO_BACK_EMOJI) {
    pendingReviewManager.add(pr.reviewMessageId, pr.pendingData);
    return;
  }

  if (emojiName === CONFIRM_EMOJI) {
    try {
      if (pr.submitterUser) {
        await pr.submitterUser.send({
          embeds: [
            new EmbedBuilder()
              .setTitle('❌ Submission Not Accepted')
              .setDescription(
                `Your **${pr.typeName}** titled **"${pr.pendingData.rawTitle}"** was reviewed by the Council and was not accepted at this time.\n\n` +
                `**Reason:** ${pr.reason}\n\n` +
                `If you believe this is a mistake or have additional context to provide, feel free to open a new support ticket.`
              )
              .setColor(config.COLORS.ERROR)
              .setTimestamp(),
          ],
        });
      }
    } catch { /* DMs may be closed */ }

    try {
      const channel = await client.channels.fetch(config.PRIORITY_REVIEW_CHANNEL_ID).catch(() => null);
      if (channel) {
        const reviewMsg = await channel.messages.fetch(pr.reviewMessageId).catch(() => null);
        if (reviewMsg) await reviewMsg.delete();
      }
    } catch { /* ignore */ }
  }
}

// ── Message handler (called from messageHandler.js) ────────────────────────

/**
 * Handles messages sent in the priority review channel.
 * Routes to the active rejection or edit flow for the sending user.
 */
async function handleReviewMessage(message, client) {
  // ── Rejection reason flow ──
  const pr = pendingRejections.get(message.author.id);
  if (pr && pr.stage === 'awaiting_reason') {
    try { await message.delete(); } catch { /* ignore */ }

    if (pr.promptMessageId) {
      try {
        const promptMsg = await message.channel.messages.fetch(pr.promptMessageId).catch(() => null);
        if (promptMsg) await promptMsg.delete();
      } catch { /* ignore */ }
    }

    if (message.content.trim().toLowerCase() === 'cancel') {
      pendingRejections.delete(message.author.id);
      pendingReviewManager.add(pr.reviewMessageId, pr.pendingData);
      return;
    }

    pr.reason = message.content.trim();
    pr.stage = 'awaiting_confirm';

    const confirmMsg = await message.channel.send({
      embeds: [
        new EmbedBuilder()
          .setTitle('Confirm Rejection')
          .setDescription(
            `**Submission:** \`${pr.pendingData.title}\`\n` +
            `**Reason:** ${pr.reason}\n\n` +
            `✅ Confirm — Notify the submitter and remove the submission\n` +
            `⬅️ Go Back — Restore the submission for re-review`
          )
          .setColor(config.COLORS.WARNING)
          .setTimestamp(),
      ],
    });

    await confirmMsg.react(CONFIRM_EMOJI);
    await confirmMsg.react(GO_BACK_EMOJI);
    pr.confirmMessageId = confirmMsg.id;
    return;
  }

  // ── Edit flow ──
  const pe = pendingEdits.get(message.author.id);
  if (!pe) return;

  try { await message.delete(); } catch { /* ignore */ }

  if (pe.promptMessageId) {
    try {
      const promptMsg = await message.channel.messages.fetch(pe.promptMessageId).catch(() => null);
      if (promptMsg) await promptMsg.delete();
    } catch { /* ignore */ }
  }

  if (message.content.trim().toLowerCase() === 'cancel') {
    editingMessages.delete(pe.reviewMessageId);
    pendingEdits.delete(message.author.id);
    return;
  }

  const template = TEMPLATES[pe.pendingData.type];

  // ── Stage 1: field selection ──
  if (pe.stage === 'awaiting_field') {
    const num = parseInt(message.content.trim(), 10);

    if (isNaN(num) || num < 1 || num > template.questions.length) {
      const prompt = await message.channel.send({
        embeds: [
          new EmbedBuilder()
            .setDescription(
              `❌ <@${message.author.id}> — Please reply with a number between **1** and **${template.questions.length}**. Type \`cancel\` to abort.`
            )
            .setColor(config.COLORS.ERROR),
        ],
      });
      pe.promptMessageId = prompt.id;
      return;
    }

    pe.fieldIndex = num - 1;
    pe.stage = 'awaiting_value';

    const question = template.questions[pe.fieldIndex];
    const currentValue = pe.pendingData.answers[question.key] || 'N/A';

    const prompt = await message.channel.send({
      embeds: [
        new EmbedBuilder()
          .setTitle(`✏️ Editing: ${question.label}`)
          .setDescription(
            `**Current value:**\n${currentValue}\n\n` +
            `<@${message.author.id}> — Reply with the new value. Type \`cancel\` to abort.`
          )
          .setColor(config.COLORS.WARNING),
      ],
    });
    pe.promptMessageId = prompt.id;
    return;
  }

  // ── Stage 2: new value input — apply immediately, update embed in-place ──
  if (pe.stage === 'awaiting_value') {
    const pending = pe.pendingData;
    const question = template.questions[pe.fieldIndex];

    pending.answers[question.key] = message.content.trim();

    pending.title = template.issueTitle(pending.answers);
    pending.rawTitle = pending.answers.title;
    pending.body = template.buildBody(pending.answers, pending.displayName, pending.screenshots || []);

    const visibleQuestions = template.questions.filter(q => !q.hiddenFromFields);
    const newEmbedFields = visibleQuestions.map((q, i) => ({
      name: `${i + 1}. ${q.label}`,
      value: (pending.answers[q.key] || '*Not answered*').substring(0, 1024),
    }));
    if (pending.screenshots && pending.screenshots.length > 0) {
      newEmbedFields.push({
        name: '📎 Attachments',
        value: pending.screenshots.map((s, i) => `${i + 1}. [${s.name}](${s.url})`).join('\n').substring(0, 1024),
      });
    }
    pending.embedFields = newEmbedFields;

    // Persist updated data under the same message ID
    pendingReviewManager.add(pe.reviewMessageId, pending);

    // Edit the review embed in-place (reactions are preserved)
    try {
      const reviewMsg = await message.channel.messages.fetch(pe.reviewMessageId).catch(() => null);
      if (reviewMsg) {
        const typeDisplayMap = { BUG: 'Bug Report', IDEA: 'Idea Suggestion', ABILITY: 'Ability Suggestion' };
        const updatedEmbed = new EmbedBuilder()
          .setTitle(`📥 New ${typeDisplayMap[pending.type] || pending.type} — Pending Review`)
          .setDescription(`**Title:** \`${pending.title}\`\nSubmitted by **${pending.displayName}**`)
          .addFields(newEmbedFields)
          .setColor(template.color)
          .setFooter({ text: 'React with a priority to approve, 📝 to edit, or ❌ to reject' })
          .setTimestamp();
        await reviewMsg.edit({ embeds: [updatedEmbed] });
      }
    } catch (err) {
      console.error('[PriorityHandler] Failed to update review embed:', err.message);
    }

    // Release the edit lock and clear the session
    editingMessages.delete(pe.reviewMessageId);
    pendingEdits.delete(message.author.id);
  }
}

module.exports = { handle, handleReviewMessage };
