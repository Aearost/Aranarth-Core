const { PermissionFlagsBits } = require('discord.js');
const config = require('../config');
const counterManager = require('./counterManager');

/**
 * Creates a ticket channel in the Support category.
 * Returns { channel, ticketNumber }.
 */
async function createTicketChannel(guild, user, type) {
  const prefix = config.TICKET_PREFIXES[type];
  const count = counterManager.nextCounter(prefix);
  // Sanitize username for channel name (Discord allows a-z, 0-9, hyphens)
  const safeName = user.username.toLowerCase().replace(/[^a-z0-9]/g, '');
  const channelName = `${prefix}-${safeName}-${count}`;

  const botId = guild.members.me.id;

  const channel = await guild.channels.create({
    name: channelName,
    parent: config.SUPPORT_CATEGORY_ID,
    permissionOverwrites: [
      {
        // Deny everyone by default
        id: guild.id,
        deny: [PermissionFlagsBits.ViewChannel],
      },
      {
        // Ticket opener
        id: user.id,
        allow: [
          PermissionFlagsBits.ViewChannel,
          PermissionFlagsBits.SendMessages,
          PermissionFlagsBits.ReadMessageHistory,
          PermissionFlagsBits.AttachFiles,
          PermissionFlagsBits.EmbedLinks,
        ],
      },
      {
        // Council role
        id: config.COUNCIL_ROLE_ID,
        allow: [
          PermissionFlagsBits.ViewChannel,
          PermissionFlagsBits.SendMessages,
          PermissionFlagsBits.ReadMessageHistory,
          PermissionFlagsBits.AttachFiles,
          PermissionFlagsBits.EmbedLinks,
          PermissionFlagsBits.ManageMessages,
        ],
      },
      {
        // Bot
        id: botId,
        allow: [
          PermissionFlagsBits.ViewChannel,
          PermissionFlagsBits.SendMessages,
          PermissionFlagsBits.ReadMessageHistory,
          PermissionFlagsBits.ManageMessages,
          PermissionFlagsBits.AddReactions,
          PermissionFlagsBits.ManageChannels,
          PermissionFlagsBits.EmbedLinks,
          PermissionFlagsBits.AttachFiles,
        ],
      },
    ],
  });

  return { channel, ticketNumber: count };
}

/**
 * Deletes a channel after a short delay.
 */
async function deleteChannel(channel, delayMs = 3000) {
  await new Promise(r => setTimeout(r, delayMs));
  try {
    await channel.delete();
  } catch (e) {
    console.error('[ChannelManager] Failed to delete channel:', e.message);
  }
}

module.exports = { createTicketChannel, deleteChannel };
