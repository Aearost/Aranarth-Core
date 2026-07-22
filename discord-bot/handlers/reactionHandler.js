const fs = require('fs');
const path = require('path');
const { EmbedBuilder, MessageType } = require('discord.js');
const config = require('../config');
const formManager = require('../forms/formManager');
const TEMPLATES = require('../forms/templates');
const channelManager = require('../utils/channelManager');
const { sendSilent } = channelManager;
const statusTracker = require('../utils/statusTracker');
const timerManager = require('../utils/timerManager');
const { uploadAttachment } = require('../github/issueCreator');
const pendingReviewManager = require('../utils/pendingReviewManager');
const priorityReactionHandler = require('./priorityReactionHandler');
const workQueueHandler = require('./workQueueHandler');
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

  // ── Priority review channel ──
  if (message.channel.id === config.PRIORITY_REVIEW_CHANNEL_ID) {
    await priorityReactionHandler.handle(reaction, user, client);
    return;
  }

  // ── Work queue channel ──
  if (message.channel.id === config.WORK_QUEUE_CHANNEL_ID) {
    await workQueueHandler.handleReaction(reaction, user, client);
    return;
  }

  // ── 1. Initial support message ──
  const activeId = getActiveMessageId();
  if (message.channel.id === config.NEW_TICKET_CHANNEL_ID && message.id === activeId) {
    await handleInitialReaction(reaction, user, client, emojiName);
    return;
  }

  // ── 2. Form channel reactions ──
  const session = formManager.getSessionByChannel(message.channel.id);
  if (session && user.id === session.userId) {
    // Cancel button (persistent, pinned at top of channel)
    if (session.cancelMessageId === message.id) {
      try { await reaction.users.remove(user.id); } catch { /* ignore */ }
      await handleCancel(message.channel, session, user);
      return;
    }
    // Confirmation message (submit/edit/cancel)
    if (session.state === 'CONFIRMING' && session.confirmMessageId === message.id) {
      await handleConfirmReaction(reaction, user, client, session, emojiName, message.channel);
      return;
    }
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
    timerManager.schedule(client, channel.id, user.id, 'FORM_INACTIVITY', config.FORM_INACTIVITY_MS);
  }
}

async function setupQuestionChannel(channel, guild, user, client) {
  const member = await guild.members.fetch(user.id);
  const displayName = member.nickname
    ? `${user.username} (${member.nickname})`
    : user.username;

  const welcomeEmbed = new EmbedBuilder()
    .setTitle('❓ General Support')
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

  await sendSilent(channel, { embeds: [introEmbed] });

  // Send a persistent cancel button pinned at the top of the channel
  const cancelEmbed = new EmbedBuilder()
    .setDescription('❌ React below at any time to cancel this submission and close the channel.')
    .setColor(config.COLORS.ERROR);
  const cancelMsg = await sendSilent(channel, { embeds: [cancelEmbed] });
  await cancelMsg.react(config.CONFIRM_EMOJIS.CANCEL);
  try {
    await cancelMsg.pin();
    const recent = await channel.messages.fetch({ limit: 5 });
    const pinNotice = recent.find(m => m.type === MessageType.ChannelPinnedMessage);
    if (pinNotice) await pinNotice.delete();
  } catch { /* non-critical */ }
  session.cancelMessageId = cancelMsg.id;

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
    // Reset the inactivity timer — user is still actively working on the form
    timerManager.schedule(client, channel.id, session.userId, 'FORM_INACTIVITY', config.FORM_INACTIVITY_MS);
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

  await sendSilent(channel, {
    embeds: [
      new EmbedBuilder()
        .setDescription('⏳ Sending your submission for Council review...')
        .setColor(config.COLORS.WARNING),
    ],
  });

  try {
    // Upload attachments first so URLs are permanent
    const screenshots = session.screenshots || [];
    for (const screenshot of screenshots) {
      try {
        screenshot.url = await uploadAttachment(screenshot.name, screenshot.url, template.label);
      } catch (uploadErr) {
        console.error(`[ReactionHandler] Failed to upload attachment ${screenshot.name}:`, uploadErr.message);
      }
    }

    const title = template.issueTitle(session.answers);
    const rawTitle = session.answers.title;
    const body = template.buildBody(session.answers, displayName, screenshots);

    // Build embed fields for the review embed and forum thread
    const embedFields = template.questions.map((q, i) => ({
      name: `${i + 1}. ${q.label}`,
      value: (session.answers[q.key] || '*Not answered*').substring(0, 1024),
    }));
    if (screenshots.length > 0) {
      embedFields.push({
        name: '📎 Attachments',
        value: screenshots.map((s, i) => `${i + 1}. [${s.name}](${s.url})`).join('\n').substring(0, 1024),
      });
    }

    // Send review embed to priority review channel
    const reviewChannel = await client.channels.fetch(config.PRIORITY_REVIEW_CHANNEL_ID).catch(() => null);
    if (!reviewChannel) throw new Error('Priority review channel not found');

    const typeDisplayMap = { BUG: 'Bug Report', IDEA: 'Idea Suggestion', ABILITY: 'Ability Suggestion' };
    const typeName = typeDisplayMap[session.type] || session.type;

    const reviewEmbed = new EmbedBuilder()
      .setTitle(`📥 New ${typeName} — Pending Review`)
      .setDescription(`**Title:** \`${title}\`\nSubmitted by **${displayName}**`)
      .addFields(embedFields)
      .setColor(template.color)
      .setFooter({ text: 'React with a priority to approve, 📝 to edit, or ❌ to reject' })
      .setTimestamp();

    const reviewMsg = await reviewChannel.send({ embeds: [reviewEmbed] });
    await reviewMsg.react(config.PRIORITY_EMOJIS.P1);
    await reviewMsg.react(config.PRIORITY_EMOJIS.P2);
    await reviewMsg.react(config.PRIORITY_EMOJIS.P3);
    await reviewMsg.react(config.PRIORITY_EMOJIS.P4);
    await reviewMsg.react(config.PRIORITY_EMOJIS.EDIT);
    await reviewMsg.react(config.PRIORITY_EMOJIS.REJECT);

    // Store pending review data
    pendingReviewManager.add(reviewMsg.id, {
      userId: user.id,
      type: session.type,
      label: template.label,
      title,
      rawTitle,
      body,
      embedFields,
      displayName,
      answers: { ...session.answers },
      screenshots: [...screenshots],
    });

    // Notify user in the ticket channel
    await sendSilent(channel, {
      embeds: [
        new EmbedBuilder()
          .setTitle('✅ Submission Received!')
          .setDescription(
            `Your **${typeName}** has been sent to the Council for review.\n\n` +
            `You'll receive a DM once it's been reviewed.\n\n` +
            `This channel will be deleted in a few seconds.`
          )
          .setColor(config.COLORS.SUCCESS)
          .setTimestamp(),
      ],
    });

    // DM the user
    try {
      const dm = await user.createDM();
      await dm.send(
        `⏳ Your **${typeName}** titled **"${rawTitle}"** has been received and is pending Council review.\n\n` +
        `You'll be notified here once the Council has reviewed it.`
      );
    } catch { /* DMs may be closed */ }

    formManager.deleteSession(user.id, channel.id);
    timerManager.cancel(channel.id);
    await channelManager.deleteChannel(channel, 5000);

  } catch (err) {
    console.error('[ReactionHandler] Submission failed:', err);

    const errMsg = await sendSilent(channel, {
      embeds: [
        new EmbedBuilder()
          .setTitle('❌ Submission Failed')
          .setDescription(
            'Something went wrong while submitting your report. Please try again.\n\n' +
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

  await sendSilent(channel, { embeds: [editEmbed] });
  session.state = 'EDITING';
}

async function handleCancel(channel, session, user) {
  await sendSilent(channel, {
    embeds: [
      new EmbedBuilder()
        .setTitle('❌ Cancelled')
        .setDescription('Your submission has been cancelled. This channel will be deleted in a few seconds.')
        .setColor(config.COLORS.ERROR),
    ],
  });

  formManager.deleteSession(user.id, channel.id);
  timerManager.cancel(channel.id);
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
