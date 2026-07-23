const fs = require('fs');
const path = require('path');

const FILE = path.join(__dirname, '..', 'data', 'forumDeletions.json');

const DELETE_DELAY_MS = 7 * 24 * 60 * 60 * 1000; // 7 days after closure

// threadId (string) -> Unix timestamp (ms) of scheduled deletion
let pending = {};

// threadId -> timeoutId
const activeTimers = new Map();

function load() {
  try { pending = JSON.parse(fs.readFileSync(FILE, 'utf8')); } catch { pending = {}; }
}

function save() {
  fs.writeFileSync(FILE, JSON.stringify(pending, null, 2));
}

/**
 * Schedules a forum thread for deletion in 7 days.
 * @param {Client} client
 * @param {string} threadId
 */
function schedule(client, threadId) {
  const deleteAt = Date.now() + DELETE_DELAY_MS;
  pending[threadId] = deleteAt;
  save();
  const id = setTimeout(() => deleteThread(client, threadId), DELETE_DELAY_MS);
  activeTimers.set(threadId, id);
  console.log(`[ForumDeletion] Scheduled deletion of thread ${threadId} in 7 days.`);
}

async function deleteThread(client, threadId) {
  clearTimeout(activeTimers.get(threadId));
  activeTimers.delete(threadId);
  delete pending[threadId];
  save();

  try {
    const thread = await client.channels.fetch(threadId).catch(() => null);
    if (!thread) return;
    await thread.delete();
    console.log(`[ForumDeletion] Deleted closed forum thread ${threadId}.`);
  } catch (err) {
    console.error(`[ForumDeletion] Failed to delete thread ${threadId}:`, err.message);
  }
}

/**
 * Restores any pending deletions from disk on bot startup.
 * @param {Client} client
 */
function restore(client) {
  const now = Date.now();
  let count = 0;
  for (const [threadId, deleteAt] of Object.entries(pending)) {
    const remaining = deleteAt - now;
    const delay = remaining <= 0 ? 5000 + count * 500 : remaining;
    const id = setTimeout(() => deleteThread(client, threadId), delay);
    activeTimers.set(threadId, id);
    count++;
  }
  if (count > 0) console.log(`[ForumDeletion] Restored ${count} pending forum thread deletion(s).`);
}

load();

module.exports = { schedule, restore };
