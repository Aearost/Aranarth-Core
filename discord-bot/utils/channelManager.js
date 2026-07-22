const { PermissionFlagsBits, MessageFlags } = require('discord.js');
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

  // For form channels (bug/idea/ability) the council has no role — they interact
  // via the priority-review channel after submission, not the form itself.
  // Omitting their ViewChannel overwrite keeps these channels out of their channel
  // list entirely, preventing unwanted notifications.
  // Question channels still need full council access so they can respond.
  const isFormChannel = type !== 'QUESTION';

  const permissionOverwrites = [
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
  ];

  if (!isFormChannel) {
    permissionOverwrites.push({
      // Council role — only on question channels
      id: config.COUNCIL_ROLE_ID,
      allow: [
        PermissionFlagsBits.ViewChannel,
        PermissionFlagsBits.SendMessages,
        PermissionFlagsBits.ReadMessageHistory,
        PermissionFlagsBits.AttachFiles,
        PermissionFlagsBits.EmbedLinks,
        PermissionFlagsBits.ManageMessages,
      ],
    });
  }

  const channel = await guild.channels.create({
    name: channelName,
    parent: config.SUPPORT_CATEGORY_ID,
    permissionOverwrites,
  });

  return { channel, ticketNumber: count };
}

/**
 * Sends a message with notifications suppressed (equivalent to Discord's /silent).
 * Administrators bypass channel permission overwrites and would otherwise receive
 * push notifications for every bot message in form channels — this prevents that.
 * The channel still appears as unread in the sidebar for the ticket creator.
 */
async function sendSilent(channel, payload) {
  if (typeof payload === 'string') {
    return channel.send({ content: payload, flags: MessageFlags.SuppressNotifications });
  }
  return channel.send({ ...payload, flags: MessageFlags.SuppressNotifications });
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

module.exports = { createTicketChannel, deleteChannel, sendSilent };
