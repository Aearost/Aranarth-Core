const fs = require('fs');
const path = require('path');
const { EmbedBuilder, MessageType } = require('discord.js');
const config = require('../config');
const formManager = require('../forms/formManager');
const TEMPLATES = require('../forms/templates');
const channelManager = require('../utils/channelManager');
const statusTracker = require('../utils/statusTracker');
const timerManager = require('../utils/timerManager');
const { createIssue } = require('../github/issueCreator');
const { applyStatusChange } = require('../utils/statusManager');

const ACTIVE_MSG_PATH = path.join(__dirname, '..', 'data', 'activeMessage.json');

function getActiveMessageId() {
  try {
    return JSON.parse(fs.readFileSync(ACTIVE_MSG_PATH, 'utf8')).messageId;
  } catch {
    return null;
  }
}

async function handle(reaction, user, client) {
  const { message, emoji } = reaction;
  const emojiName = emoji.name;

  // ── 1. Initial support message ──
  const activeId = getActiveMessageId();
  if (message.channel.id === config.NEW_TICKET_CHANNEL_ID && message.id === activeId) {
    await handleInitialReaction(reaction, user, client, emojiName);
    return;
  }

  // ── 2. Confirmation message (submit/edit/cancel) ──
  const session = formManager.getSessionByChannel(message.channel.id);
  if (
    session &&
    session.state === 'CONFIRMING' &&
    session.confirmMessageId === message.id &&
    user.id === session.userId
  ) {
    await handleConfirmReaction(reaction, user, client, session, emojiName, message.channel);
    return;
  }

  // ── 3. Status message in question channels ──
  const statusInfo = statusTracker.get(message.channel.id);
  if (statusInfo && statusInfo.messageId === message.id) {
    await handleStatusReaction(reaction, user, client, message.channel, statusInfo, emojiName);
    return;
  }
}

// ── Initial support message reactions ──────────────────────────────────────

async function handleInitialReaction(reaction, user, client, emojiName) {
  const typeMap = {
    [config.EMOJIS.BUG]: 'BUG',
    [config.EMOJIS.IDEA]: 'IDEA',
    [config.EMOJIS.ABILITY]: 'ABILITY',
    [config.EMOJIS.QUESTION]: 'QUESTION',
  };

  // Always remove the user's reaction to keep the message clean
  try { await reaction.users.remove(user.id); } catch { /* ignore */ }

  const type = typeMap[emojiName];
  if (!type) return;

  // Guard: one active session per user
  if (formManager.hasActiveSession(user.id)) {
    try {
      const dm = await user.createDM();
      await dm.send('❌ You already have an active support ticket open! Please finish or cancel it before opening another.');
    } catch { /* DMs may be closed */ }
    return;
  }

  const guild = reaction.message.guild;
  const { channel } = await channelManager.createTicketChannel(guild, user, type);
  const session = formManager.createSession(user.id, channel.id, type);

  // Notify user with a direct link to their new channel
  try {
    const dm = await user.createDM();
    await dm.send(`✅ Your channel has been created! Click here to jump to it: ${channel.url}`);
  } catch { /* DMs may be closed */ }

  if (type === 'QUESTION') {
    formManager.deleteSession(user.id, channel.id); // Questions use free-form, no form session needed
    await setupQuestionChannel(channel, guild, user, client);
  } else {
    await startForm(channel, user, session);
  }
}

async function setupQuestionChannel(channel, guild, user, client) {
  const member = await guild.members.fetch(user.id);
  const displayName = member.nickname
    ? `${user.username} (${member.nickname})`
    : user.username;

  const welcomeEmbed = new EmbedBuilder()
    .setTitle('❓ Question Channel')
    .setDescription(
      `Welcome, **${displayName}**! 👋\n\n` +
      `This is your personal space to ask the Council your question. Feel free to type freely and include as much detail as you'd like!\n\n` +
      `> 💬 Type your question in this channel and the Council will respond when available.\n` +
      `> 📎 Feel free to attach images or files for additional context.\n` +
      `> ⏰ This channel may be closed automatically once your question is resolved.`
    )
    .setColor(config.COLORS.QUESTION)
    .setFooter({ text: `Opened by ${displayName}` })
    .setTimestamp();

  await channel.send({ embeds: [welcomeEmbed] });
  await channel.send(`<@&${config.COUNCIL_ROLE_ID}> — ${user} has a question for you!`);

  // Post and pin the status message, suppressing the pin notification
  const statusEmbed = statusTracker.buildStatusEmbed('OPEN');
  const statusMsg = await channel.send({ embeds: [statusEmbed] });
  await statusMsg.react(config.STATUS_EMOJIS.OPEN);
  await statusMsg.react(config.STATUS_EMOJIS.RESOLVED);
  await statusMsg.react(config.STATUS_EMOJIS.CLOSE);
  try {
    await statusMsg.pin();
    // Delete the "pinned a message" system notification Discord auto-sends
    const recent = await channel.messages.fetch({ limit: 5 });
    const pinNotice = recent.find(m => m.type === MessageType.ChannelPinnedMessage);
    if (pinNotice) await pinNotice.delete();
  } catch { /* non-critical */ }

  statusTracker.set(channel.id, {
    messageId: statusMsg.id,
    status: 'OPEN',
    userId: user.id,
  });
}

async function startForm(channel, user, session) {
  const template = TEMPLATES[session.type];

  const introEmbed = new EmbedBuilder()
    .setTitle(`${template.emoji} ${template.displayName}`)
    .setDescription(
      `Welcome, <@${user.id}>! Let's get your ${template.displayName.toLowerCase()} submitted. 📝\n\n` +
      `Answer each question by sending a message in this channel.\n` +
      `For optional questions you can type \`skip\` to leave them blank.\n\n` +
      `This channel will be deleted once your submission is complete or cancelled.`
    )
    .setColor(template.color)
    .setFooter({ text: `${template.questions.length} questions total` });

  await channel.send({ embeds: [introEmbed] });
  await sendFirstQuestion(channel, session, template);
}

async function sendFirstQuestion(channel, session, template) {
  const { sendQuestionEmbed } = require('./messageHandler');
  await sendQuestionEmbed(channel, session, template);
}

// ── Confirmation reactions ──────────────────────────────────────────────────

async function handleConfirmReaction(reaction, user, client, session, emojiName, channel) {
  try { await reaction.users.remove(user.id); } catch { /* ignore */ }

  if (emojiName === config.CONFIRM_EMOJIS.SUBMIT) {
    await handleSubmit(channel, session, user, client);
  } else if (emojiName === config.CONFIRM_EMOJIS.EDIT) {
    await handleEdit(channel, session);
  } else if (emojiName === config.CONFIRM_EMOJIS.CANCEL) {
    await handleCancel(channel, session, user);
  }
}

async function handleSubmit(channel, session, user, client) {
  const template = TEMPLATES[session.type];
  const guild = channel.guild;
  const member = await guild.members.fetch(user.id);
  const displayName = member.nickname
    ? `${user.username} (${member.nickname})`
    : user.username;

  await channel.send({
    embeds: [
      new EmbedBuilder()
        .setDescription('⏳ Submitting your report to GitHub...')
        .setColor(config.COLORS.WARNING),
    ],
  });

  try {
    const title = template.issueTitle(session.answers);
    const body = template.buildBody(session.answers, displayName);
    const issueUrl = await createIssue(title, body, template.label);

    await channel.send({
      embeds: [
        new EmbedBuilder()
          .setTitle('✅ Submitted Successfully!')
          .setDescription(
            `Your ${template.displayName.toLowerCase()} has been submitted to GitHub!\n\n` +
            `🔗 [View Issue](${issueUrl})\n\n` +
            `This channel will be deleted in a few seconds.`
          )
          .setColor(config.COLORS.SUCCESS)
          .setTimestamp(),
      ],
    });

    // DM the user their issue link so they have it after the channel is deleted
    try {
      const dm = await user.createDM();
      await dm.send(
        `✅ Your **${template.displayName}** was submitted successfully to Aranarth!\n\n` +
        `🔗 You can track it here: ${issueUrl}`
      );
    } catch { /* DMs may be closed */ }

    formManager.deleteSession(user.id, channel.id);
    await channelManager.deleteChannel(channel, 5000);
  } catch (err) {
    console.error('[ReactionHandler] GitHub issue creation failed:', err);

    const errMsg = await channel.send({
      embeds: [
        new EmbedBuilder()
          .setTitle('❌ Submission Failed')
          .setDescription(
            'Something went wrong while creating the GitHub issue. Please try again.\n\n' +
            'React ✅ to retry, or ❌ to cancel.'
          )
          .setColor(config.COLORS.ERROR),
      ],
    });

    await errMsg.react(config.CONFIRM_EMOJIS.SUBMIT);
    await errMsg.react(config.CONFIRM_EMOJIS.CANCEL);
    session.confirmMessageId = errMsg.id;
  }
}

async function handleEdit(channel, session) {
  const template = TEMPLATES[session.type];

  const editEmbed = new EmbedBuilder()
    .setTitle('✏️ Edit a Response')
    .setDescription(
      'Which question would you like to change? **Reply with its number.**\n\n' +
      template.questions.map((q, i) => `**${i + 1}.** ${q.label}`).join('\n')
    )
    .setColor(config.COLORS.WARNING);

  await channel.send({ embeds: [editEmbed] });
  session.state = 'EDITING';
}

async function handleCancel(channel, session, user) {
  await channel.send({
    embeds: [
      new EmbedBuilder()
        .setTitle('❌ Cancelled')
        .setDescription('Your submission has been cancelled. This channel will be deleted in a few seconds.')
        .setColor(config.COLORS.ERROR),
    ],
  });

  formManager.deleteSession(user.id, channel.id);
  await channelManager.deleteChannel(channel, 5000);
}

// ── Status reactions (Question channels) ───────────────────────────────────

async function handleStatusReaction(reaction, user, client, channel, statusInfo, emojiName) {
  try { await reaction.users.remove(user.id); } catch { /* ignore */ }

  // Council-only
  const member = await channel.guild.members.fetch(user.id);
  if (!member.roles.cache.has(config.COUNCIL_ROLE_ID)) return;

  const statusMap = {
    [config.STATUS_EMOJIS.OPEN]: 'OPEN',
    [config.STATUS_EMOJIS.RESOLVED]: 'RESOLVED',
    [config.STATUS_EMOJIS.CLOSE]: 'CLOSE',
  };

  const newStatus = statusMap[emojiName];
  if (!newStatus) return;

  const displayName = member.nickname
    ? `${user.username} (${member.nickname})`
    : user.username;

  await applyStatusChange(channel, statusInfo, newStatus, displayName, client);
}

module.exports = { handle };
