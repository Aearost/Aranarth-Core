const fs = require('fs');
const path = require('path');
const { EmbedBuilder } = require('discord.js');

const STATUS_PATH = path.join(__dirname, '..', 'data', 'statusMessages.json');

// In-memory: channelId -> { messageId, status, userId }
const statusMessages = new Map();

function load() {
  try {
    const data = JSON.parse(fs.readFileSync(STATUS_PATH, 'utf8'));
    for (const [channelId, info] of Object.entries(data)) {
      statusMessages.set(channelId, info);
    }
  } catch {
    // File missing or empty is fine
  }
}

function save() {
  const obj = {};
  for (const [k, v] of statusMessages.entries()) obj[k] = v;
  fs.writeFileSync(STATUS_PATH, JSON.stringify(obj, null, 2));
}

function set(channelId, info) {
  statusMessages.set(channelId, info);
  save();
}

function get(channelId) {
  return statusMessages.get(channelId) || null;
}

function update(channelId, fields) {
  const existing = statusMessages.get(channelId);
  if (!existing) return;
  Object.assign(existing, fields);
  save();
}

function remove(channelId) {
  statusMessages.delete(channelId);
  save();
}

function buildStatusEmbed(status) {
  const MAP = {
    OPEN: {
      emoji: '🔎',
      label: 'Open',
      color: 0x2ECC71,
      desc: 'This ticket is open and active.',
    },
    RESOLVED: {
      emoji: '✅',
      label: 'Resolved',
      color: 0x3498DB,
      desc: 'This ticket has been marked as resolved.\nThis channel will automatically close in **48 hours**.',
    },
    CLOSE: {
      emoji: '🔒',
      label: 'Closing...',
      color: 0xFF4444,
      desc: 'This ticket is being closed.',
    },
  };

  const s = MAP[status] || MAP.OPEN;
  return new EmbedBuilder()
    .setTitle(`${s.emoji} Ticket Status — ${s.label}`)
    .setDescription(
      `${s.desc}\n\n` +
      '**Council: react below to change status**\n' +
      '🔎 Open · ✅ Resolved · 🔒 Close'
    )
    .setColor(s.color)
    .setFooter({ text: '🔒 Staff controls — reactions restricted to Council' })
    .setTimestamp();
}

module.exports = { load, set, get, update, remove, buildStatusEmbed };
