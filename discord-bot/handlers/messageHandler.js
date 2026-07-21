const { EmbedBuilder } = require('discord.js');
const config = require('../config');
const formManager = require('../forms/formManager');
const TEMPLATES = require('../forms/templates');
const statusTracker = require('../utils/statusTracker');
const timerManager = require('../utils/timerManager');

async function handle(message, client) {
  // ── Question channel: inactivity timer management ──
  const statusInfo = statusTracker.get(message.channel.id);
  if (statusInfo) {
    if (message.author.id === statusInfo.userId) {
      // Opener responded — cancel any pending inactivity notification and close timers
      timerManager.cancelInactivity(message.channel.id);
      timerManager.cancel(message.channel.id);
    } else {
      // Council responded — (re)start the 1-hour inactivity notification timer
      timerManager.cancel(message.channel.id);
      timerManager.scheduleInactivity(client, message.channel.id, statusInfo.userId);
    }
    return;
  }

  // ── Form channel handling ──
  const session = formManager.getSessionByChannel(message.channel.id);
  if (!session || message.author.id !== session.userId) return;

  // Collect any attachments the user sends at any stage
  if (message.attachments.size > 0) {
    const template = TEMPLATES[session.type];
    let stepLabel;
    if (session.state === 'CONFIRMING') {
      stepLabel = 'Review';
    } else if (session.state === 'EDITING') {
      stepLabel = 'Edit';
    } else {
      const q = template.questions[session.currentStep];
      stepLabel = q ? q.label : 'Unknown';
    }
    message.attachments.forEach(att => {
      session.screenshots.push({
        url: att.url,
        name: att.name,
        contentType: att.contentType || '',
        step: stepLabel,
      });
    });
  }

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

  if (session.screenshots.length > 0) {
    fields.push({
      name: '📎 Attachments',
      value: session.screenshots.map((s, i) => `${i + 1}. [${s.name}](${s.url}) *(${s.step})*`).join('\n').substring(0, 1024),
    });
  }

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
