const config = require('../config');
const statusTracker = require('./statusTracker');
const timerManager = require('./timerManager');
const channelManager = require('./channelManager');

const STATUS_LABELS = {
  OPEN: 'Open',
  AWAITING_INFO: 'Awaiting Info',
  RESOLVED: 'Resolved',
  FORCE_CLOSE: 'Force Close',
};

/**
 * Applies a status change to a ticket channel.
 * Used by both the reaction handler and the /ts slash command.
 */
async function applyStatusChange(channel, statusInfo, newStatus, actorDisplayName, client) {
  // Update the pinned status embed
  try {
    const statusMsg = await channel.messages.fetch(statusInfo.messageId);
    await statusMsg.edit({ embeds: [statusTracker.buildStatusEmbed(newStatus)] });
  } catch { /* message gone */ }

  statusTracker.update(channel.id, { status: newStatus });

  if (newStatus === 'OPEN') {
    timerManager.cancel(channel.id);
    await channel.send(`🔎 **${actorDisplayName}** set the status to **Open**.`);

  } else if (newStatus === 'AWAITING_INFO') {
    timerManager.schedule(client, channel.id, statusInfo.userId, 'AWAITING_INFO', config.AWAITING_INFO_MS);
    await channel.send(
      `⏳ **${actorDisplayName}** set the status to **Awaiting Info**.\n` +
      `<@${statusInfo.userId}> — if you don't respond within **5 days**, this channel will automatically close.`
    );
    try {
      const ticketUser = await client.users.fetch(statusInfo.userId);
      const dm = await ticketUser.createDM();
      await dm.send(`⏳ Your ticket in **Aranarth** is awaiting your response. Please reply in ${channel.url} within **5 days** or it will automatically close.`);
    } catch { /* DMs may be closed */ }

  } else if (newStatus === 'RESOLVED') {
    timerManager.schedule(client, channel.id, statusInfo.userId, 'RESOLVED', config.RESOLVED_MS);
    await channel.send(
      `✅ **${actorDisplayName}** marked this ticket as **Resolved**!\n` +
      `This channel will automatically close in **48 hours**. Feel free to ask any follow-up questions before then!`
    );
    try {
      const ticketUser = await client.users.fetch(statusInfo.userId);
      const dm = await ticketUser.createDM();
      await dm.send(`✅ Your ticket in **Aranarth** has been marked as **Resolved**! The channel will close in **48 hours**. You can still ask follow-up questions there: ${channel.url}`);
    } catch { /* DMs may be closed */ }

  } else if (newStatus === 'FORCE_CLOSE') {
    timerManager.cancel(channel.id);
    await channel.send(`🔒 **${actorDisplayName}** closed this ticket.`);
    try {
      const ticketUser = await client.users.fetch(statusInfo.userId);
      const dm = await ticketUser.createDM();
      await dm.send(`🔒 Your ticket in **Aranarth** has been closed by the Council. If you have further questions, feel free to open a new ticket!`);
    } catch { /* DMs may be closed */ }
    await channelManager.deleteChannel(channel, 3000);
    statusTracker.remove(channel.id);
  }
}

module.exports = { applyStatusChange, STATUS_LABELS };
