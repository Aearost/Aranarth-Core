const { EmbedBuilder } = require('discord.js');
const config = require('../config');
const forumManager = require('../utils/forumManager');
const forumDeletionManager = require('../utils/forumDeletionManager');
const { addComment } = require('../github/githubManager');

/**
 * Creates a Discord forum thread for a newly approved GitHub issue.
 * @param {Client} client
 * @param {number} issueNumber
 * @param {string} issueTitle   Full title e.g. "[BUG] Something broken"
 * @param {Array}  embedFields  Array of { name, value } field objects
 * @param {string} type         'BUG' | 'IDEA' | 'ABILITY'
 * @param {string} priority     'P1' | 'P2' | 'P3' | 'P4'
 */
async function createForumThread(client, issueNumber, issueTitle, embedFields, type, priority) {
  const channel = await client.channels.fetch(config.FORUM_CHANNEL_ID).catch(() => null);
  if (!channel) {
    console.error('[ForumHandler] Could not fetch forum channel.');
    return;
  }

  const colorMap = { BUG: config.COLORS.BUG, IDEA: config.COLORS.IDEA, ABILITY: config.COLORS.ABILITY };
  const color = colorMap[type] || config.COLORS.DEFAULT;

  const priorityLabels = { P1: '🔴 Critical', P2: '🟠 High', P3: '🟡 Medium', P4: '🟢 Low / Backlog' };

  const githubUrl = `https://github.com/${config.GITHUB_OWNER}/${config.GITHUB_REPO}/issues/${issueNumber}`;

  const embed = new EmbedBuilder()
    .setTitle(`${issueTitle} #${issueNumber}`)
    .setDescription(`**Priority:** ${priorityLabels[priority] || priority}`)
    .addFields(embedFields)
    .setColor(color)
    .setFooter({ text: 'Aranarth Issue Tracker · Community feedback is mirrored to GitHub' })
    .setTimestamp();

  const thread = await channel.threads.create({
    name: `${issueTitle} #${issueNumber}`,
    message: { content: `<${githubUrl}>`, embeds: [embed] },
  });

  forumManager.set(issueNumber, thread.id);
  console.log(`[ForumHandler] Created forum thread ${thread.id} for issue #${issueNumber}.`);
}

/**
 * Resolves Discord mention tokens in message content to readable names for GitHub.
 * e.g. <@123456> → @username, <@&789> → @RoleName, <#456> → #channel-name
 */
function resolveMentions(content, message) {
  let resolved = content;
  for (const [id, user] of message.mentions.users) {
    resolved = resolved.replace(new RegExp(`<@!?${id}>`, 'g'), `@${user.username}`);
  }
  for (const [id, role] of message.mentions.roles) {
    resolved = resolved.replace(new RegExp(`<@&${id}>`, 'g'), `@${role.name}`);
  }
  for (const [id, channel] of message.mentions.channels) {
    resolved = resolved.replace(new RegExp(`<#${id}>`, 'g'), `#${channel.name}`);
  }
  return resolved;
}

/**
 * Handles a new message in a tracked forum thread, mirroring it to GitHub as a comment.
 */
async function handleForumMessage(message) {
  if (!message.channel.isThread()) return;

  const issueNumber = forumManager.getIssueNumber(message.channel.id);
  if (!issueNumber) return;

  const authorName = message.member?.nickname
    ? `${message.author.username} (${message.member.nickname})`
    : message.author.username;

  const content = resolveMentions(message.content, message);

  let commentBody;
  if (message.reference) {
    try {
      const refMsg = await message.channel.messages.fetch(message.reference.messageId);
      const refAuthor = refMsg.author.bot
        ? 'Aranarth'
        : (refMsg.member?.nickname || refMsg.author.username);
      commentBody = `**💬 [Discord Community] ${authorName}** *(replying to ${refAuthor})*:\n\n${content}`;
    } catch {
      commentBody = `**💬 [Discord Community] ${authorName}:**\n\n${content}`;
    }
  } else {
    commentBody = `**💬 [Discord Community] ${authorName}:**\n\n${content}`;
  }

  if (message.attachments.size > 0) {
    const links = [...message.attachments.values()].map(a => `[${a.name}](${a.url})`).join('\n');
    commentBody += `\n\n**Attachments:**\n${links}`;
  }

  try {
    await addComment(issueNumber, commentBody);
  } catch (err) {
    console.error(`[ForumHandler] Failed to mirror comment for issue #${issueNumber}:`, err.message);
  }
}

/**
 * Posts a formatted progress note embed to the forum thread for an issue.
 */
async function postNoteToForum(client, issueNumber, note, authorName) {
  const threadId = forumManager.getThreadId(issueNumber);
  if (!threadId) return;

  const thread = await client.channels.fetch(threadId).catch(() => null);
  if (!thread) return;

  await thread.send({
    embeds: [
      new EmbedBuilder()
        .setTitle('📋 Progress Note')
        .setDescription(note)
        .setColor(config.COLORS.DEFAULT)
        .setFooter({ text: `Note by ${authorName}` })
        .setTimestamp(),
    ],
  });
}

/**
 * Posts a status-change notification embed to the forum thread when a label changes.
 * @param {string} newStatus  'wip' | 'on-hold'
 * @param {string} markedBy   Display name of the council member who made the change
 */
async function postTagChangeToForum(client, issueNumber, newStatus, markedBy) {
  const threadId = forumManager.getThreadId(issueNumber);
  if (!threadId) return;

  const thread = await client.channels.fetch(threadId).catch(() => null);
  if (!thread) return;

  const statusConfig = {
    wip: {
      title: '▶️ Now In Progress',
      description: 'This issue has been picked up and is actively being worked on.',
      color: config.COLORS.SUCCESS,
    },
    'wip-removed': {
      title: '⬜ No Longer In Progress',
      description: 'This issue has been unmarked as in progress and is back in the queue.',
      color: config.COLORS.DEFAULT,
    },
    'on-hold': {
      title: '⏸️ Placed On Hold',
      description: 'Work on this issue has been paused for now.',
      color: config.COLORS.WARNING,
    },
  };

  const sc = statusConfig[newStatus];
  if (!sc) return;

  await thread.send({
    embeds: [
      new EmbedBuilder()
        .setTitle(sc.title)
        .setDescription(sc.description)
        .setColor(sc.color)
        .setFooter({ text: `Marked by ${markedBy}` })
        .setTimestamp(),
    ],
  });
}

/**
 * Locks the forum thread for a closed issue and posts a closure notice.
 */
async function lockForumThread(client, issueNumber) {
  const threadId = forumManager.getThreadId(issueNumber);
  if (!threadId) return;

  const thread = await client.channels.fetch(threadId).catch(() => null);
  if (!thread) return;

  try {
    await thread.send({
      embeds: [
        new EmbedBuilder()
          .setTitle('🔒 Issue Closed')
          .setDescription('This issue has been resolved and closed. No further comments can be added.')
          .setColor(config.COLORS.SUCCESS)
          .setTimestamp(),
      ],
    });
    await thread.setLocked(true);
    forumDeletionManager.schedule(client, thread.id);
  } catch (err) {
    console.error(`[ForumHandler] Failed to lock thread for issue #${issueNumber}:`, err.message);
  }
}

module.exports = { createForumThread, handleForumMessage, postNoteToForum, postTagChangeToForum, lockForumThread };
