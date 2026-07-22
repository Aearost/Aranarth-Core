const fs = require('fs');
const path = require('path');

const FILE = path.join(__dirname, '..', 'data', 'councilActivity.json');

// issueNumber (string) → Unix timestamp in ms of last council action
let activity = {};

function load() {
  try {
    activity = JSON.parse(fs.readFileSync(FILE, 'utf8'));
  } catch {
    activity = {};
  }
}

function save() {
  fs.writeFileSync(FILE, JSON.stringify(activity, null, 2));
}

/**
 * Records the current time as the last council action for an issue.
 * @param {number} issueNumber
 */
function record(issueNumber) {
  activity[String(issueNumber)] = Date.now();
  save();
}

/**
 * Returns the Unix timestamp (ms) of the last council action, or null if never touched.
 * @param {number} issueNumber
 */
function getTimestamp(issueNumber) {
  return activity[String(issueNumber)] ?? null;
}

/**
 * Removes tracking for a closed issue.
 * @param {number} issueNumber
 */
function remove(issueNumber) {
  delete activity[String(issueNumber)];
  save();
}

load();

module.exports = { record, getTimestamp, remove };
