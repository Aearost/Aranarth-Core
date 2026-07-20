const config = require('../config');
const statusTracker = require('../utils/statusTracker');
const { applyStatusChange, STATUS_LABELS } = require('../utils/statusManager');

async function handle(interaction, client) {
  // Council-only
  const member = interaction.member;
  if (!member.roles.cache.has(config.COUNCIL_ROLE_ID)) {
    await interaction.reply({ content: '❌ This command is restricted to Council members.', ephemeral: true });
    return;
  }

  // Must be used inside a ticket channel
  const statusInfo = statusTracker.get(interaction.channelId);
  if (!statusInfo) {
    await interaction.reply({ content: '❌ This command can only be used inside a support ticket channel.', ephemeral: true });
    return;
  }

  const newStatus = interaction.options.getString('status');
  const displayName = member.nickname
    ? `${interaction.user.username} (${member.nickname})`
    : interaction.user.username;

  // Acknowledge ephemerally so the command itself doesn't clutter the channel
  await interaction.reply({ content: `✅ Status set to **${STATUS_LABELS[newStatus]}**.`, ephemeral: true });

  await applyStatusChange(interaction.channel, statusInfo, newStatus, displayName, client);
}

module.exports = { handle };
