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

// userId → { stage: 'awaiting_reason', reviewMessageId, pendingData, submitterUser, typeName, promptMessageId, reason, confirmMessageId }
const pendingRejections = new Map();

// userId → { stage: 'awaiting_field'|'awaiting_value', reviewMessageId, pendingData, promptMessageId, fieldIndex }
const pendingEdits = new Map();

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
    .setFooter({ text: 'React with a priority to approve, ✏️ to edit, or ❌ to reject' })
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

  // ── Retrieve and atomically claim the pending review ──
  const pending = pendingReviewManager.get(message.id);
  if (!pending) return;
  pendingReviewManager.remove(message.id);

  const submitterUser = await client.users.fetch(pending.userId).catch(() => null);
  const typeName = TYPE_DISPLAY[pending.type] || pending.type;

  // ── Edit: prompt field selection ──
  if (emojiName === config.PRIORITY_EMOJIS.EDIT) {
    const template = TEMPLATES[pending.type];
    pendingEdits.set(user.id, {
      stage: 'awaiting_field',
      reviewMessageId: message.id,
      pendingData: pending,
      promptMessageId: null,
      fieldIndex: null,
    });

    const fieldList = template.questions.map((q, i) => `**${i + 1}.** ${q.label}`).join('\n');
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
    pendingEdits.delete(message.author.id);
    pendingReviewManager.add(pe.reviewMessageId, pe.pendingData);
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

  // ── Stage 2: new value input ──
  if (pe.stage === 'awaiting_value') {
    const pending = pe.pendingData;
    const question = template.questions[pe.fieldIndex];

    // Apply the edit
    pending.answers[question.key] = message.content.trim();

    // Rebuild title, body, and embed fields from updated answers
    pending.title = template.issueTitle(pending.answers);
    pending.rawTitle = pending.answers.title;
    pending.body = template.buildBody(pending.answers, pending.displayName, pending.screenshots || []);

    const newEmbedFields = template.questions.map((q, i) => ({
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

    // Delete the old review message
    try {
      const reviewMsg = await message.channel.messages.fetch(pe.reviewMessageId).catch(() => null);
      if (reviewMsg) await reviewMsg.delete();
    } catch { /* ignore */ }

    pendingEdits.delete(message.author.id);

    // Re-post the updated review message and register it
    const newMsg = await postReviewMessage(message.channel, pending);
    pendingReviewManager.add(newMsg.id, pending);
  }
}

module.exports = { handle, handleReviewMessage };
