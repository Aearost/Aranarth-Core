const fs = require('fs');
const path = require('path');

const FILE = path.join(__dirname, '..', 'data', 'holdTimestamps.json');

// issueNumber (string) → Unix timestamp in ms of when ON HOLD was applied
let timestamps = {};

function load() {
  try {
    timestamps = JSON.parse(fs.readFileSync(FILE, 'utf8'));
  } catch {
    timestamps = {};
  }
}

function save() {
  fs.writeFileSync(FILE, JSON.stringify(timestamps, null, 2));
}

/**
 * Records the current time as the start of a hold for an issue.
 * @param {number} issueNumber
 */
function record(issueNumber) {
  timestamps[String(issueNumber)] = Date.now();
  save();
}

/**
 * Returns the Unix timestamp (ms) of when the hold started, or null if not on hold.
 * @param {number} issueNumber
 */
function getTimestamp(issueNumber) {
  return timestamps[String(issueNumber)] ?? null;
}

/**
 * Removes the hold timestamp for an issue (e.g. when resumed or closed).
 * @param {number} issueNumber
 */
function remove(issueNumber) {
  delete timestamps[String(issueNumber)];
  save();
}

load();

module.exports = { record, getTimestamp, remove };
