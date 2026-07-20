const { EmbedBuilder } = require('discord.js');
const config = require('../config');
const formManager = require('../forms/formManager');
const TEMPLATES = require('../forms/templates');
const statusTracker = require('../utils/statusTracker');
const timerManager = require('../utils/timerManager');

async function handle(message, client) {
  // ── Question channel: reset Awaiting Info timer when opener speaks ──
  const statusInfo = statusTracker.get(message.channel.id);
  if (statusInfo && message.author.id === statusInfo.userId && statusInfo.status === 'AWAITING_INFO') {
    statusTracker.update(message.channel.id, { status: 'OPEN' });
    timerManager.cancel(message.channel.id);
    try {
      const statusMsg = await message.channel.messages.fetch(statusInfo.messageId);
      await statusMsg.edit({ embeds: [statusTracker.buildStatusEmbed('OPEN')] });
    } catch { /* message may be gone */ }
    await message.channel.send('🔎 Status reset to **Open** — the ticket opener has responded.');
    return;
  }

  // ── Form channel handling ──
  const session = formManager.getSessionByChannel(message.channel.id);
  if (!session || message.author.id !== session.userId) return;

  const template = TEMPLATES[session.type];

  if (session.state === 'ANSWERING') {
    await handleAnswer(message, session, template, client);
  } else if (session.state === 'EDITING') {
    await handleEditSelection(message, session, template);
  }
}

async function handleAnswer(message, session, template, client) {
  const question = template.questions[session.currentStep];
  session.answers[question.key] = message.content.trim();

  if (session.editMode) {
    // After re-answering one question, go back to the confirmation screen
    session.editMode = false;
    await showConfirmation(message.channel, session, template, message.author, client);
  } else {
    session.currentStep++;
    if (session.currentStep >= template.questions.length) {
      await showConfirmation(message.channel, session, template, message.author, client);
    } else {
      await sendQuestionEmbed(message.channel, session, template);
    }
  }
}

async function handleEditSelection(message, session, template) {
  const num = parseInt(message.content.trim(), 10);

  if (isNaN(num) || num < 1 || num > template.questions.length) {
    await message.channel.send({
      embeds: [
        new EmbedBuilder()
          .setDescription(`❌ Please reply with a number between **1** and **${template.questions.length}**.`)
          .setColor(config.COLORS.ERROR),
      ],
    });
    return;
  }

  session.currentStep = num - 1;
  session.editMode = true;
  session.state = 'ANSWERING';
  await sendQuestionEmbed(message.channel, session, template);
}

async function sendQuestionEmbed(channel, session, template) {
  const q = template.questions[session.currentStep];
  await channel.send({
    embeds: [
      new EmbedBuilder()
        .setDescription(q.prompt)
        .setColor(template.color)
        .setFooter({ text: `Question ${session.currentStep + 1} of ${template.questions.length}` }),
    ],
  });
}

async function showConfirmation(channel, session, template, user, client) {
  const guild = channel.guild;
  const member = await guild.members.fetch(user.id);
  const displayName = member.nickname
    ? `${user.username} (${member.nickname})`
    : user.username;

  const fields = template.questions.map((q, i) => ({
    name: `${i + 1}. ${q.label}`,
    value: (session.answers[q.key] || '*Not answered*').substring(0, 1024),
  }));

  const previewEmbed = new EmbedBuilder()
    .setTitle(`📋 Review Your ${template.displayName}`)
    .setDescription(
      `Here is a preview of your submission. Please review it carefully before submitting.\n\n` +
      `**Issue title:** \`${template.issueTitle(session.answers)}\``
    )
    .addFields(fields)
    .setColor(template.color)
    .setFooter({ text: `Submitting as ${displayName}` })
    .setTimestamp();

  await channel.send({ embeds: [previewEmbed] });

  const actionEmbed = new EmbedBuilder()
    .setDescription(
      `✅ **Submit** — Create the GitHub issue\n` +
      `✏️ **Edit** — Change a specific answer\n` +
      `❌ **Cancel** — Discard this submission and close the channel`
    )
    .setColor(config.COLORS.DEFAULT);

  const actionMsg = await channel.send({ embeds: [actionEmbed] });
  await actionMsg.react(config.CONFIRM_EMOJIS.SUBMIT);
  await actionMsg.react(config.CONFIRM_EMOJIS.EDIT);
  await actionMsg.react(config.CONFIRM_EMOJIS.CANCEL);

  session.confirmMessageId = actionMsg.id;
  session.state = 'CONFIRMING';
}

module.exports = { handle, showConfirmation, sendQuestionEmbed };
