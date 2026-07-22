const fs = require('fs');
const path = require('path');

const FILE = path.join(__dirname, '..', 'data', 'workQueue.json');

let queue = { messages: [] };
// In-memory pending operations keyed by userId
// value: { type: 'close'|'note', issueNumber, issueTitle, queueMessageId, promptMessageId }
const pendingOps = new Map();

function load() {
  try {
    queue = JSON.parse(fs.readFileSync(FILE, 'utf8'));
    if (!queue.messages) queue.messages = [];
  } catch {
    queue = { messages: [] };
  }
}

function save() {
  fs.writeFileSync(FILE, JSON.stringify(queue, null, 2));
}

function getAll() {
  return queue.messages;
}

function getByMessageId(messageId) {
  return queue.messages.find(m => m.messageId === messageId) || null;
}

function setMessages(messages) {
  queue.messages = messages;
  save();
}

function updateStatus(messageId, status) {
  const item = queue.messages.find(m => m.messageId === messageId);
  if (item) {
    item.status = status;
    save();
  }
}

function setPendingOp(userId, op) {
  pendingOps.set(userId, op);
}

function getPendingOp(userId) {
  return pendingOps.get(userId) || null;
}

function clearPendingOp(userId) {
  pendingOps.delete(userId);
}

load();

module.exports = { getAll, getByMessageId, setMessages, updateStatus, setPendingOp, getPendingOp, clearPendingOp };
