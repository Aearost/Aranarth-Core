const { EmbedBuilder } = require('discord.js');
const config = require('../config');
const workQueueManager = require('../utils/workQueueManager');
const scoringEngine = require('../utils/scoringEngine');
const { fetchOpenIssues, addLabel, removeLabel, addComment, closeIssue } = require('../github/githubManager');
const { postNoteToForum, lockForumThread } = require('./forumHandler');

const PRIORITY_COLORS = {
  P1: config.COLORS.P1,
  P2: config.COLORS.P2,
  P3: config.COLORS.P3,
  P4: config.COLORS.P4,
};

function buildQueueEmbed(issue) {
  const priority = scoringEngine.getPriority(issue);
  const status = scoringEngine.getStatus(issue);

  const statusDisplay = {
    wip: '▶️ In Progress',
    'on-hold': '⏸️ On Hold',
    normal: '⚪ Open',
  }[status] || '⚪ Open';

  const priorityDisplay = priority || 'Untagged';
  const color = PRIORITY_COLORS[priority] || config.COLORS.DEFAULT;

  return new EmbedBuilder()
    .setTitle(`${issue.title} · #${issue.number}`)
    .setURL(issue.html_url)
    .setDescription(issue.html_url)
    .addFields(
      { name: 'Priority', value: priorityDisplay, inline: true },
      { name: 'Status', value: statusDisplay, inline: true },
    )
    .setColor(color)
    .setTimestamp();
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

  for (const item of workQueueManager.getAll()) {
    try {
      const msg = await channel.messages.fetch(item.messageId).catch(() => null);
      if (msg) await msg.delete();
    } catch { /* already deleted */ }
  }
  workQueueManager.setMessages([]);
}

/**
 * Clears and repopulates the work queue channel based on current GitHub issue scores.
 */
async function refreshWorkQueue(client) {
  console.log('[WorkQueue] Refreshing...');
  try {
    const issues = await fetchOpenIssues();
    const selected = scoringEngine.selectTop(issues);

    await deleteWorkQueueMessages(client);

    const channel = await client.channels.fetch(config.WORK_QUEUE_CHANNEL_ID).catch(() => null);
    if (!channel) {
      console.error('[WorkQueue] Could not fetch work queue channel.');
      return;
    }

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
      workQueueManager.setMessages([{ messageId: emptyMsg.id, issueNumber: null, isEmpty: true }]);
      return;
    }

    const newMessages = [];
    for (const issue of selected) {
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
    console.log(`[WorkQueue] Posted ${newMessages.length} item(s).`);
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
  if (!item || item.isEmpty) return;

  const { issueNumber, issueTitle, status } = item;
  const displayName = member.nickname
    ? `${user.username} (${member.nickname})`
    : user.username;

  // ── In Progress ──
  if (emojiName === config.WORK_QUEUE_EMOJIS.IN_PROGRESS) {
    if (status === 'wip') return; // Already WIP — no-op
    try {
      if (status === 'on-hold') await removeLabel(issueNumber, 'ON HOLD');
      await addLabel(issueNumber, 'WIP');
      await refreshWorkQueue(client);
    } catch (err) {
      console.error('[WorkQueue] Failed to set WIP:', err.message);
    }

  // ── Take Note ──
  } else if (emojiName === config.WORK_QUEUE_EMOJIS.TAKE_NOTE) {
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
    try {
      if (status === 'wip') await removeLabel(issueNumber, 'WIP');
      await addLabel(issueNumber, 'ON HOLD');
      await refreshWorkQueue(client);
    } catch (err) {
      console.error('[WorkQueue] Failed to set ON HOLD:', err.message);
    }

  // ── Close ──
  } else if (emojiName === config.WORK_QUEUE_EMOJIS.CLOSE) {
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
    } catch (err) {
      console.error('[WorkQueue] Failed to post note:', err.message);
    }

  } else if (pending.type === 'close') {
    const closeComment = `🔒 **Closed by ${displayName}:**\n\n${message.content.trim()}`;
    try {
      await closeIssue(pending.issueNumber, closeComment);
      await lockForumThread(client, pending.issueNumber);
    } catch (err) {
      console.error('[WorkQueue] Failed to close issue:', err.message);
    }
    await refreshWorkQueue(client);
  }

  return true;
}

module.exports = { refreshWorkQueue, handleReaction, handleWorkQueueMessage };
