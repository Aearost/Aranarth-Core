const { EmbedBuilder } = require('discord.js');
const config = require('../config');
const workQueueManager = require('../utils/workQueueManager');
const scoringEngine = require('../utils/scoringEngine');
const councilActivityManager = require('../utils/councilActivityManager');
const { fetchOpenIssues, addLabel, removeLabel, addComment, closeIssue } = require('../github/githubManager');
const { postNoteToForum, postTagChangeToForum, lockForumThread } = require('./forumHandler');

const PRIORITY_COLORS = {
  P1: config.COLORS.P1,
  P2: config.COLORS.P2,
  P3: config.COLORS.P3,
  P4: config.COLORS.P4,
};

const DESC_LIMIT = 200;

function extractDescription(issue) {
  const body = issue.body || '';
  const labels = issue.labels.map(l => l.name);
  const header = labels.includes('BUG') ? '**Detailed Description:**' : '**Explanation of Suggestion**';
  const idx = body.indexOf(header);
  if (idx === -1) return null;
  const afterHeader = body.slice(idx + header.length).replace(/^\n+/, '');
  const nextSection = afterHeader.search(/\n\*\*/);
  const content = (nextSection === -1 ? afterHeader : afterHeader.slice(0, nextSection)).trim();
  if (!content) return null;
  const flat = content.replace(/\s+/g, ' ').trim();
  const truncated = flat.length > DESC_LIMIT ? flat.substring(0, DESC_LIMIT - 3) + '...' : flat;
  return `> *${truncated}*`;
}

function buildQueueEmbed(issue) {
  const priority = scoringEngine.getPriority(issue);
  const status = scoringEngine.getStatus(issue);

  const statusDisplay = {
    wip: 'In Progress',
    'on-hold': 'On Hold',
    normal: 'Open',
  }[status] || 'Open';

  const priorityDisplay = priority || 'Untagged';
  const color = PRIORITY_COLORS[priority] || config.COLORS.DEFAULT;

  const createdTs = Math.floor(new Date(issue.created_at).getTime() / 1000);
  const lastWorked = councilActivityManager.getTimestamp(issue.number);
  const lastUpdatedValue = lastWorked ? `<t:${Math.floor(lastWorked / 1000)}:f>` : 'N/A';
  const description = extractDescription(issue);

  return new EmbedBuilder()
    .setTitle(`${issue.title} · #${issue.number}`)
    .setURL(issue.html_url)
    .setDescription(description ?? null)
    .addFields(
      { name: 'Priority', value: priorityDisplay, inline: true },
      { name: 'Status', value: statusDisplay, inline: true },
      { name: '\u200b', value: '\u200b', inline: true },
      { name: 'Creation Date', value: `<t:${createdTs}:f>`, inline: true },
      { name: 'Last Updated', value: lastUpdatedValue, inline: true },
      { name: '\u200b', value: '\u200b', inline: true },
    )
    .setColor(color);
}

async function postWorkQueueMessage(channel, issue) {
  const embed = buildQueueEmbed(issue);
  const msg = await channel.send({ embeds: [embed] });
  await msg.react(config.WORK_QUEUE_EMOJIS.IN_PROGRESS);
  await msg.react(config.WORK_QUEUE_EMOJIS.TAKE_NOTE);
  await msg.react(config.WORK_QUEUE_EMOJIS.ON_HOLD);
  await msg.react(config.WORK_QUEUE_EMOJIS.CLOSE);
  return msg;
}

async function deleteWorkQueueMessages(client) {
  const channel = await client.channels.fetch(config.WORK_QUEUE_CHANNEL_ID).catch(() => null);
  if (!channel) return;

  // Delete ALL messages in the channel so stale untracked messages from old bot versions are removed too
  let fetched;
  do {
    fetched = await channel.messages.fetch({ limit: 100 });
    if (fetched.size === 0) break;
    try {
      await channel.bulkDelete(fetched);
    } catch {
      // bulkDelete fails for messages older than 14 days — fall back to individual deletes
      for (const msg of fetched.values()) {
        try { await msg.delete(); } catch { /* ignore */ }
      }
    }
  } while (fetched.size === 100);

  workQueueManager.setMessages([]);
}

/**
 * Clears and repopulates the work queue channel based on current GitHub issue scores.
 * @param {*} client
 * @param {number|null} excludeIssueNumber - Issue number to exclude (e.g. one just closed whose state may not have propagated yet)
 */
async function refreshWorkQueue(client, excludeIssueNumber = null) {
  console.log('[WorkQueue] Refreshing...');
  try {
    let issues = await fetchOpenIssues();
    if (excludeIssueNumber != null) issues = issues.filter(i => i.number !== excludeIssueNumber);
    const selected = scoringEngine.selectTop(issues);

    await deleteWorkQueueMessages(client);

    const channel = await client.channels.fetch(config.WORK_QUEUE_CHANNEL_ID).catch(() => null);
    if (!channel) {
      console.error('[WorkQueue] Could not fetch work queue channel.');
      return;
    }

    // Legend message always appears at the top
    const legendMsg = await channel.send({
      embeds: [
        new EmbedBuilder()
          .setTitle('📋 To-Do List Legend')
          .setDescription(
            `${config.WORK_QUEUE_EMOJIS.IN_PROGRESS} **In Progress** — Mark this issue as actively being worked on\n` +
            `${config.WORK_QUEUE_EMOJIS.TAKE_NOTE} **Progress Note** — Add a note to the issue (mirrored to GitHub)\n` +
            `${config.WORK_QUEUE_EMOJIS.ON_HOLD} **On Hold** — Pause work on this issue\n` +
            `${config.WORK_QUEUE_EMOJIS.CLOSE} **Close** — Mark the issue as resolved and close it`
          )
          .setColor(config.COLORS.DEFAULT),
      ],
    });
    const newMessages = [{ messageId: legendMsg.id, issueNumber: null, isLegend: true }];

    if (selected.length === 0) {
      const emptyMsg = await channel.send({
        embeds: [
          new EmbedBuilder()
            .setTitle('🎉 Queue Empty!')
            .setDescription('There are no open issues to work on. Great job keeping up!')
            .setColor(config.COLORS.SUCCESS)
            .setTimestamp(),
        ],
      });
      newMessages.push({ messageId: emptyMsg.id, issueNumber: null, isEmpty: true });
      workQueueManager.setMessages(newMessages);
      return;
    }

    const wipIssues = selected.filter(i => scoringEngine.isWip(i));
    const otherIssues = selected.filter(i => !scoringEngine.isWip(i));

    if (wipIssues.length > 0) {
      const wipSepMsg = await channel.send({
        embeds: [
          new EmbedBuilder()
            .setDescription('**─────────────────────────────────**\n▶️  **In Progress**\n**─────────────────────────────────**')
            .setColor(0x2B2D31),
        ],
      });
      newMessages.push({ messageId: wipSepMsg.id, issueNumber: null, isSeparator: true });
    }

    for (const issue of wipIssues) {
      const msg = await postWorkQueueMessage(channel, issue);
      newMessages.push({
        messageId: msg.id,
        issueNumber: issue.number,
        issueTitle: issue.title,
        issueUrl: issue.html_url,
        priority: scoringEngine.getPriority(issue),
        type: scoringEngine.getType(issue),
        status: scoringEngine.getStatus(issue),
      });
    }

    if (wipIssues.length > 0 && otherIssues.length > 0) {
      const sepMsg = await channel.send({
        embeds: [
          new EmbedBuilder()
            .setDescription('**─────────────────────────────────**\n📋  **To Review**\n**─────────────────────────────────**')
            .setColor(0x2B2D31),
        ],
      });
      newMessages.push({ messageId: sepMsg.id, issueNumber: null, isSeparator: true });
    }

    for (const issue of otherIssues) {
      const msg = await postWorkQueueMessage(channel, issue);
      newMessages.push({
        messageId: msg.id,
        issueNumber: issue.number,
        issueTitle: issue.title,
        issueUrl: issue.html_url,
        priority: scoringEngine.getPriority(issue),
        type: scoringEngine.getType(issue),
        status: scoringEngine.getStatus(issue),
      });
    }

    workQueueManager.setMessages(newMessages);
    console.log(`[WorkQueue] Posted ${wipIssues.length} WIP + ${otherIssues.length} other item(s).`);
  } catch (err) {
    console.error('[WorkQueue] Error during refresh:', err.message);
  }
}

/**
 * Handles a reaction on a work queue message.
 */
async function handleReaction(reaction, user, client) {
  const { message, emoji } = reaction;
  const emojiName = emoji.name;

  // Remove reaction regardless of outcome to keep channel clean
  try { await reaction.users.remove(user.id); } catch { /* ignore */ }

  // Council only
  const guild = message.guild;
  const member = await guild.members.fetch(user.id).catch(() => null);
  if (!member || !member.roles.cache.has(config.COUNCIL_ROLE_ID)) return;

  const item = workQueueManager.getByMessageId(message.id);
  if (!item || item.isEmpty || item.isSeparator || item.isLegend) return;

  const { issueNumber, issueTitle, status } = item;
  const displayName = member.nickname
    ? `${user.username} (${member.nickname})`
    : user.username;

  // ── In Progress (toggle) ──
  if (emojiName === config.WORK_QUEUE_EMOJIS.IN_PROGRESS) {
    try {
      if (status === 'wip') {
        await removeLabel(issueNumber, 'WIP');
        await postTagChangeToForum(client, issueNumber, 'wip-removed', displayName);
      } else {
        if (status === 'on-hold') await removeLabel(issueNumber, 'ON HOLD');
        await addLabel(issueNumber, 'WIP');
        await postTagChangeToForum(client, issueNumber, 'wip', displayName);
      }
      councilActivityManager.record(issueNumber);
      await refreshWorkQueue(client);
    } catch (err) {
      console.error('[WorkQueue] Failed to toggle WIP:', err.message);
    }

  // ── Take Note ──
  } else if (emojiName === config.WORK_QUEUE_EMOJIS.TAKE_NOTE) {
    if (workQueueManager.getPendingOp(user.id)) return;
    workQueueManager.setPendingOp(user.id, {
      type: 'note',
      issueNumber,
      issueTitle,
      queueMessageId: message.id,
      promptMessageId: null,
    });
    const prompt = await message.channel.send({
      embeds: [
        new EmbedBuilder()
          .setDescription(`💬 <@${user.id}> — Reply with your progress note for **${issueTitle} · #${issueNumber}**. Type \`cancel\` to abort.`)
          .setColor(config.COLORS.DEFAULT),
      ],
    });
    const op = workQueueManager.getPendingOp(user.id);
    if (op) op.promptMessageId = prompt.id;

  // ── On Hold ──
  } else if (emojiName === config.WORK_QUEUE_EMOJIS.ON_HOLD) {
    if (status === 'on-hold') return; // Already on hold — no-op
    if (workQueueManager.getPendingOp(user.id)) return;
    workQueueManager.setPendingOp(user.id, {
      type: 'on-hold',
      issueNumber,
      issueTitle,
      previousStatus: status,
      queueMessageId: message.id,
      promptMessageId: null,
    });
    const prompt = await message.channel.send({
      embeds: [
        new EmbedBuilder()
          .setDescription(`⏸️ <@${user.id}> — Reply with a note for placing **${issueTitle} · #${issueNumber}** on hold, \`skip\` to apply with no note, or \`cancel\` to abort.`)
          .setColor(config.COLORS.WARNING),
      ],
    });
    const op = workQueueManager.getPendingOp(user.id);
    if (op) op.promptMessageId = prompt.id;

  // ── Close ──
  } else if (emojiName === config.WORK_QUEUE_EMOJIS.CLOSE) {
    if (workQueueManager.getPendingOp(user.id)) return;
    workQueueManager.setPendingOp(user.id, {
      type: 'close',
      issueNumber,
      issueTitle,
      queueMessageId: message.id,
      promptMessageId: null,
    });
    const prompt = await message.channel.send({
      embeds: [
        new EmbedBuilder()
          .setDescription(`🔒 <@${user.id}> — Reply with a closing comment for **${issueTitle} · #${issueNumber}**. Type \`cancel\` to abort.`)
          .setColor(config.COLORS.WARNING),
      ],
    });
    const op = workQueueManager.getPendingOp(user.id);
    if (op) op.promptMessageId = prompt.id;
  }
}

/**
 * Handles a message sent in the work queue channel (for pending close/note operations).
 * Returns true if the message was consumed by a pending operation.
 */
async function handleWorkQueueMessage(message, client) {
  const pending = workQueueManager.getPendingOp(message.author.id);
  if (!pending) return false;

  const displayName = message.member?.nickname
    ? `${message.author.username} (${message.member.nickname})`
    : message.author.username;

  // Clean up the user's reply and the prompt message
  try { await message.delete(); } catch { /* ignore */ }
  if (pending.promptMessageId) {
    try {
      const channel = message.channel;
      const promptMsg = await channel.messages.fetch(pending.promptMessageId).catch(() => null);
      if (promptMsg) await promptMsg.delete();
    } catch { /* ignore */ }
  }

  workQueueManager.clearPendingOp(message.author.id);

  if (message.content.trim().toLowerCase() === 'cancel') return true;

  if (pending.type === 'note') {
    const noteText = message.content.trim();
    const githubComment = `📋 **Progress Note by ${displayName}:**\n\n${noteText}`;
    try {
      await addComment(pending.issueNumber, githubComment);
      await postNoteToForum(client, pending.issueNumber, noteText, displayName);
      councilActivityManager.record(pending.issueNumber);
    } catch (err) {
      console.error('[WorkQueue] Failed to post note:', err.message);
    }

  } else if (pending.type === 'on-hold') {
    try {
      if (pending.previousStatus === 'wip') await removeLabel(pending.issueNumber, 'WIP');
      await addLabel(pending.issueNumber, 'ON HOLD');
      await postTagChangeToForum(client, pending.issueNumber, 'on-hold', displayName);
      const noteText = message.content.trim();
      if (noteText.toLowerCase() !== 'skip') {
        const githubComment = `⏸️ **On Hold note by ${displayName}:**\n\n${noteText}`;
        await addComment(pending.issueNumber, githubComment);
        await postNoteToForum(client, pending.issueNumber, noteText, displayName);
      }
      councilActivityManager.record(pending.issueNumber);
    } catch (err) {
      console.error('[WorkQueue] Failed to set ON HOLD:', err.message);
    }
    await refreshWorkQueue(client);

  } else if (pending.type === 'close') {
    const closeComment = `🔒 **Closed by ${displayName}:**\n\n${message.content.trim()}`;
    try {
      await removeLabel(pending.issueNumber, 'WIP');
      await removeLabel(pending.issueNumber, 'ON HOLD');
      await closeIssue(pending.issueNumber, closeComment);
      await lockForumThread(client, pending.issueNumber);
      councilActivityManager.remove(pending.issueNumber);
    } catch (err) {
      console.error('[WorkQueue] Failed to close issue:', err.message);
    }
    await refreshWorkQueue(client, pending.issueNumber);
  }

  return true;
}

module.exports = { refreshWorkQueue, handleReaction, handleWorkQueueMessage };
