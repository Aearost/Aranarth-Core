const fs = require('fs');
const path = require('path');

const COUNTERS_PATH = path.join(__dirname, '..', 'data', 'counters.json');

function read() {
  try {
    return JSON.parse(fs.readFileSync(COUNTERS_PATH, 'utf8'));
  } catch {
    return { bug: 0, idea: 0, ability: 0, question: 0 };
  }
}

function write(data) {
  fs.writeFileSync(COUNTERS_PATH, JSON.stringify(data, null, 2));
}

/**
 * Increments and returns the next counter value for the given type key.
 * @param {'bug'|'idea'|'ability'|'question'} type
 */
function nextCounter(type) {
  const counters = read();
  counters[type] = (counters[type] || 0) + 1;
  write(counters);
  return counters[type];
}

module.exports = { nextCounter };
