const fs = require('fs');
const path = require('path');

const FILE = path.join(__dirname, '..', 'data', 'forumThreads.json');

let threads = {};       // issueNumber (string) → threadId
let reverseMap = {};    // threadId → issueNumber (number)

function load() {
  try {
    threads = JSON.parse(fs.readFileSync(FILE, 'utf8'));
    rebuildReverse();
  } catch {
    threads = {};
    reverseMap = {};
  }
}

function save() {
  fs.writeFileSync(FILE, JSON.stringify(threads, null, 2));
}

function rebuildReverse() {
  reverseMap = {};
  for (const [num, threadId] of Object.entries(threads)) {
    reverseMap[threadId] = parseInt(num, 10);
  }
}

function set(issueNumber, threadId) {
  threads[String(issueNumber)] = threadId;
  reverseMap[threadId] = issueNumber;
  save();
}

function getThreadId(issueNumber) {
  return threads[String(issueNumber)] || null;
}

function getIssueNumber(threadId) {
  return reverseMap[threadId] || null;
}

function remove(issueNumber) {
  const threadId = threads[String(issueNumber)];
  if (threadId) delete reverseMap[threadId];
  delete threads[String(issueNumber)];
  save();
}

load();

module.exports = { set, getThreadId, getIssueNumber, remove };
