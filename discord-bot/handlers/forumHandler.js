const { EmbedBuilder } = require('discord.js');
const config = require('../config');
const forumManager = require('../utils/forumManager');
const forumDeletionManager = require('../utils/forumDeletionManager');
const commentMapManager = require('../utils/commentMapManager');
const { addComment, deleteComment } = require('../github/githubManager');

const TEXT_EXTENSIONS = new Set(['.txt', '.log', '.json', '.yaml', '.yml', '.xml', '.ini', '.cfg', '.toml', '.sh', '.bat', '.properties', '.csv']);
const IMAGE_EXTENSIONS = new Set(['.png', '.jpg', '.jpeg', '.gif', '.webp', '.bmp']);
const MAX_EMBED_BYTES = 20 * 1024; // 20 KB

/**
 * Returns GitHub-flavoured markdown for a single Discord attachment.
 * - Images: inline image syntax so GitHub proxies/caches them permanently.
 * - Small text files: fetched immediately and embedded as a code block.
 * - Everything else: plain hyperlink.
 */
async function buildAttachmentMarkdown(att) {
  const ext = (att.name.match(/\.[^.]+$/) || [''])[0].toLowerCase();

  if (IMAGE_EXTENSIONS.has(ext)) {
    return `![${att.name}](${att.url})`;
  }

  if (TEXT_EXTENSIONS.has(ext) && att.size <= MAX_EMBED_BYTES) {
    try {
      const res = await fetch(att.url);
      if (res.ok) {
        let text = await res.text();
        if (text.length > 3000) text = text.substring(0, 3000) + '\n... (truncated)';
        const lang = ext === '.json' ? 'json' : (ext === '.yaml' || ext === '.yml') ? 'yaml' : ext === '.sh' ? 'bash' : '';
        return `**${att.name}:**\n\`\`\`${lang}\n${text}\n\`\`\``;
      }
    } catch { /* fall through to link */ }
  }

  return `[${att.name}](${att.url})`;
}

/**
 * Ensures a forum thread is unarchived so messages can be sent to it.
 */
async function ensureUnarchived(thread) {
  if (thread.archived) {
    await thread.setArchived(false);
  }
}

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
    const lines = await Promise.all([...message.attachments.values()].map(buildAttachmentMarkdown));
    commentBody += `\n\n**Attachments:**\n${lines.join('\n')}`;
  }

  try {
    const commentId = await addComment(issueNumber, commentBody);
    commentMapManager.set(message.id, commentId);
  } catch (err) {
    console.error(`[ForumHandler] Failed to mirror comment for issue #${issueNumber}:`, err.message);
  }
}

/**
 * Handles deletion of a Discord forum message, deleting the mirrored GitHub comment.
 */
async function handleForumMessageDelete(messageId) {
  const commentId = commentMapManager.getCommentId(messageId);
  if (!commentId) return;
  commentMapManager.remove(messageId);
  try {
    await deleteComment(commentId);
  } catch (err) {
    console.error(`[ForumHandler] Failed to delete GitHub comment ${commentId}:`, err.message);
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

  await ensureUnarchived(thread);
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

  await ensureUnarchived(thread);

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

/**
 * Creates a Discord forum thread for an existing GitHub issue (catch-up sync).
 * Derives type/priority from the issue's labels and uses the issue body as the embed.
 * @param {Client} client
 * @param {object} issue  Raw GitHub issue object from the API
 * @returns {Promise<ThreadChannel|null>}
 */
async function createForumThreadFromIssue(client, issue) {
  const channel = await client.channels.fetch(config.FORUM_CHANNEL_ID).catch(() => null);
  if (!channel) {
    console.error('[ForumHandler] Could not fetch forum channel.');
    return null;
  }

  const labelNames = issue.labels.map(l => l.name.toUpperCase());
  const type     = ['BUG', 'IDEA', 'ABILITY'].find(t => labelNames.includes(t)) || null;
  const priority = ['P1', 'P2', 'P3', 'P4'].find(p => labelNames.includes(p)) || null;

  const colorMap = { BUG: config.COLORS.BUG, IDEA: config.COLORS.IDEA, ABILITY: config.COLORS.ABILITY };
  const color = (type && colorMap[type]) || config.COLORS.DEFAULT;

  const priorityLabels = { P1: '🔴 Critical', P2: '🟠 High', P3: '🟡 Medium', P4: '🟢 Low / Backlog' };
  const priorityDisplay = priority ? priorityLabels[priority] : '⚪ Unset';

  const githubUrl = issue.html_url;

  // Build embed description: priority header + body (Discord cap: 4096 chars total)
  const header = `**Priority:** ${priorityDisplay}\n\n`;
  const maxBodyLen = 4096 - header.length - 50; // leave headroom
  let bodyText = issue.body
    ? (issue.body.length > maxBodyLen
        ? issue.body.substring(0, maxBodyLen) + `\n\n*[...truncated — view on GitHub](${githubUrl})*`
        : issue.body)
    : '*No description provided.*';

  // Discord thread names are capped at 100 chars
  const rawThreadName = `${issue.title} #${issue.number}`;
  const threadName = rawThreadName.length > 100 ? rawThreadName.substring(0, 97) + '...' : rawThreadName;

  const embed = new EmbedBuilder()
    .setTitle(`${issue.title} #${issue.number}`)
    .setDescription(header + bodyText)
    .setColor(color)
    .setFooter({ text: 'Aranarth Issue Tracker · Community feedback is mirrored to GitHub' })
    .setTimestamp(new Date(issue.created_at));

  const thread = await channel.threads.create({
    name: threadName,
    message: { content: `<${githubUrl}>`, embeds: [embed] },
  });

  forumManager.set(issue.number, thread.id);
  console.log(`[ForumHandler] Created catch-up forum thread ${thread.id} for issue #${issue.number}.`);
  return thread;
}

module.exports = { createForumThread, createForumThreadFromIssue, handleForumMessage, handleForumMessageDelete, postNoteToForum, postTagChangeToForum, lockForumThread };
