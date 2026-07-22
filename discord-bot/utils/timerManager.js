const fs = require('fs');
const path = require('path');
const { MessageFlags } = require('discord.js');
const config = require('../config');

const TIMERS_PATH = path.join(__dirname, '..', 'data', 'timers.json');

// channelId -> timeoutId  (persisted close timers)
const activeTimers = new Map();
// channelId -> timeoutId  (ephemeral 1-hour inactivity notification timers)
const inactivityTimers = new Map();

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

// ── Persisted close timers ─────────────────────────────────────────────────

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

// ── Ephemeral inactivity notification timers ───────────────────────────────

function cancelInactivity(channelId) {
  const id = inactivityTimers.get(channelId);
  if (id) {
    clearTimeout(id);
    inactivityTimers.delete(channelId);
  }
}

function scheduleInactivity(client, channelId, userId) {
  cancelInactivity(channelId);
  const id = setTimeout(
    () => triggerInactivityNotification(client, channelId, userId),
    config.INACTIVITY_NOTIFY_MS
  );
  inactivityTimers.set(channelId, id);
}

async function triggerInactivityNotification(client, channelId, userId) {
  inactivityTimers.delete(channelId);

  const statusTracker = require('./statusTracker');

  try {
    const channel = await client.channels.fetch(channelId);
    if (!channel) return;

    const closeDays = Math.round(config.INACTIVITY_CLOSE_MS / (24 * 60 * 60 * 1000));

    await channel.send(
      `💬 It looks like your question may have been answered! If you have no further questions, ` +
      `this channel will automatically close in **${closeDays} days**. Feel free to keep chatting if you need more help!`
    );

    try {
      const ticketUser = await client.users.fetch(userId);
      const dm = await ticketUser.createDM();
      await dm.send(
        `💬 It looks like your question in **#${channel.name}** in **Aranarth** may have been answered! ` +
        `The channel will automatically close in **${closeDays} days** if there's no further activity. ` +
        `Jump back in if you need more help: ${channel.url}`
      );
    } catch { /* DMs may be closed */ }

    // Start the actual close timer
    schedule(client, channelId, userId, 'INACTIVITY_CLOSE', config.INACTIVITY_CLOSE_MS);
  } catch { /* channel may be gone */ }
}

// ── Warning and close ──────────────────────────────────────────────────────

async function sendWarning(client, channelId, userId, reason, timeLeftMs) {
  try {
    const channel = await client.channels.fetch(channelId);
    if (!channel) return;
    const minutes = Math.round(timeLeftMs / 60_000);
    const why = reason === 'FORM_INACTIVITY'
      ? 'inactivity in your submission form'
      : reason === 'INACTIVITY_CLOSE'
        ? 'inactivity after your question appeared to be answered'
        : 'being marked as Resolved';
    const warnPayload = { content: `⚠️ This channel will automatically close in **${minutes} minutes** due to ${why}.` };
    if (reason === 'FORM_INACTIVITY') warnPayload.flags = MessageFlags.SuppressNotifications;
    await channel.send(warnPayload);

    try {
      const ticketUser = await client.users.fetch(userId);
      const dm = await ticketUser.createDM();
      const dmWhy = reason === 'FORM_INACTIVITY'
        ? 'your form submission has been inactive for nearly 24 hours'
        : reason === 'INACTIVITY_CLOSE'
          ? 'it has been inactive for a while'
          : 'it was marked as Resolved';
      await dm.send(`⚠️ Your ticket **#${channel.name}** in **Aranarth** will automatically close in **${minutes} minutes** because ${dmWhy}. Jump back in here if needed: ${channel.url}`);
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

    let why, dmWhy, userId;
    if (reason === 'FORM_INACTIVITY') {
      why = 'abandonment — no activity for 24 hours';
      dmWhy = 'it was inactive for 24 hours without being submitted or cancelled';
      // Try the live session first, fall back to the persisted timer record
      userId = formManager.getSessionByChannel(channelId)?.userId
        ?? readFile()[channelId]?.userId;
    } else if (reason === 'INACTIVITY_CLOSE') {
      why = 'inactivity after the question appeared to be answered';
      dmWhy = 'it was inactive for too long after appearing to be answered';
      userId = statusInfo?.userId;
    } else {
      why = 'being marked as Resolved with no further activity';
      dmWhy = 'it was marked as Resolved with no further activity';
      userId = statusInfo?.userId;
    }

    const closePayload = { content: `🔒 Closing this channel automatically due to ${why}.` };
    if (reason === 'FORM_INACTIVITY') closePayload.flags = MessageFlags.SuppressNotifications;
    await channel.send(closePayload);

    if (userId) {
      try {
        const ticketUser = await client.users.fetch(userId);
        const dm = await ticketUser.createDM();
        await dm.send(`🔒 Your ticket **#${channel.name}** in **Aranarth** has been automatically closed because ${dmWhy}. Feel free to open a new ticket if you need further help!`);
      } catch { /* DMs may be closed */ }
    }

    await new Promise(r => setTimeout(r, 3000));
    await channel.delete();
  } catch { /* ignore if already deleted */ }

  cancel(channelId);
  cancelInactivity(channelId);
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
      setTimeout(() => closeChannel(client, channelId, info.reason), 5000 + count * 1000);
    } else {
      schedule(client, channelId, info.userId, info.reason, remaining);
    }
    count++;
  }

  if (count > 0) console.log(`[TimerManager] Restored ${count} pending auto-close timer(s).`);
}

module.exports = { schedule, cancel, scheduleInactivity, cancelInactivity, restoreTimers, closeChannel };
