const fs = require('fs');
const path = require('path');
const config = require('../config');

const TIMERS_PATH = path.join(__dirname, '..', 'data', 'timers.json');

// channelId -> timeoutId
const activeTimers = new Map();

function readFile() {
  try {
    return JSON.parse(fs.readFileSync(TIMERS_PATH, 'utf8'));
  } catch {
    return {};
  }
}

function writeFile(data) {
  fs.writeFileSync(TIMERS_PATH, JSON.stringify(data, null, 2));
}

function cancel(channelId) {
  const t = activeTimers.get(channelId);
  if (t) {
    clearTimeout(t.closeId);
    clearTimeout(t.warnId);
    activeTimers.delete(channelId);
  }
  const data = readFile();
  delete data[channelId];
  writeFile(data);
}

function schedule(client, channelId, userId, reason, delayMs) {
  cancel(channelId);

  const fireAt = Date.now() + delayMs;
  const data = readFile();
  data[channelId] = { reason, fireAt, userId };
  writeFile(data);

  const WARN = config.WARN_BEFORE_MS;
  const ids = {};

  if (delayMs > WARN * 2) {
    ids.warnId = setTimeout(() => sendWarning(client, channelId, userId, reason, WARN), delayMs - WARN);
  }
  ids.closeId = setTimeout(() => closeChannel(client, channelId, reason), delayMs);

  activeTimers.set(channelId, ids);
}

async function sendWarning(client, channelId, userId, reason, timeLeftMs) {
  try {
    const channel = await client.channels.fetch(channelId);
    if (!channel) return;
    const minutes = Math.round(timeLeftMs / 60_000);
    const why = reason === 'AWAITING_INFO'
      ? 'no response from the ticket opener'
      : 'being marked as Resolved';
    await channel.send(`⚠️ This channel will automatically close in **${minutes} minutes** due to ${why}.`);

    // DM the ticket opener with the warning
    try {
      const ticketUser = await client.users.fetch(userId);
      const dm = await ticketUser.createDM();
      const dmWhy = reason === 'AWAITING_INFO'
        ? `your ticket is still awaiting your response`
        : `your ticket was marked as Resolved`;
      await dm.send(`⚠️ Your ticket in **Aranarth** will automatically close in **${minutes} minutes** because ${dmWhy}. Jump back in here if needed: ${channel.url}`);
    } catch { /* DMs may be closed */ }
  } catch { /* channel may already be gone */ }
}

async function closeChannel(client, channelId, reason) {
  const formManager = require('../forms/formManager');
  const statusTracker = require('./statusTracker');

  try {
    const channel = await client.channels.fetch(channelId);
    if (!channel) return;

    const statusInfo = statusTracker.get(channelId);

    const why = reason === 'AWAITING_INFO'
      ? 'no response from the ticket opener in 5 days'
      : 'being marked as Resolved with no further activity';

    await channel.send(`🔒 Closing this channel automatically due to ${why}.`);

    // DM the ticket opener so they know why the channel disappeared
    if (statusInfo?.userId) {
      try {
        const ticketUser = await client.users.fetch(statusInfo.userId);
        const dm = await ticketUser.createDM();
        const dmWhy = reason === 'AWAITING_INFO'
          ? 'it was awaiting your response for 5 days with no reply'
          : 'it was marked as Resolved with no further activity';
        await dm.send(`🔒 Your ticket in **Aranarth** has been automatically closed because ${dmWhy}. Feel free to open a new ticket if you need further help!`);
      } catch { /* DMs may be closed */ }
    }

    await new Promise(r => setTimeout(r, 3000));
    await channel.delete();
  } catch { /* ignore if already deleted */ }

  cancel(channelId);
  formManager.deleteSessionByChannel(channelId);
  statusTracker.remove(channelId);
}

function restoreTimers(client) {
  const data = readFile();
  const now = Date.now();
  let count = 0;

  for (const [channelId, info] of Object.entries(data)) {
    const remaining = info.fireAt - now;
    if (remaining <= 0) {
      // Overdue — fire shortly after ready
      setTimeout(() => closeChannel(client, channelId, info.reason), 5000 + count * 1000);
    } else {
      schedule(client, channelId, info.userId, info.reason, remaining);
    }
    count++;
  }

  if (count > 0) console.log(`[TimerManager] Restored ${count} pending auto-close timer(s).`);
}

module.exports = { schedule, cancel, restoreTimers, closeChannel };
