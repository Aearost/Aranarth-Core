const fs = require('fs');
const path = require('path');

const FILE = path.join(__dirname, '..', 'data', 'pendingReviews.json');

let reviews = {};

function load() {
  try {
    reviews = JSON.parse(fs.readFileSync(FILE, 'utf8'));
  } catch {
    reviews = {};
  }
}

function save() {
  fs.writeFileSync(FILE, JSON.stringify(reviews, null, 2));
}

// data: { userId, type, label, title, rawTitle, body, embedFields, displayName }
function add(messageId, data) {
  reviews[messageId] = data;
  save();
}

function get(messageId) {
  return reviews[messageId] || null;
}

function remove(messageId) {
  delete reviews[messageId];
  save();
}

load();

module.exports = { add, get, remove };
