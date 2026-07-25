const fs = require('fs');
const path = require('path');

const FILE = path.join(__dirname, '..', 'data', 'searchedIssues.json');
const MAX = 5;

let searched = [];

function load() {
  try {
    const raw = JSON.parse(fs.readFileSync(FILE, 'utf8'));
    searched = Array.isArray(raw) ? raw : [];
  } catch {
    searched = [];
  }
}

function save() {
  fs.writeFileSync(FILE, JSON.stringify(searched, null, 2));
}

function getAll() {
  return [...searched];
}

function has(n) {
  return searched.includes(Number(n));
}

function add(n) {
  if (searched.length >= MAX) return false;
  const num = Number(n);
  if (searched.includes(num)) return false;
  searched.push(num);
  save();
  return true;
}

function remove(n) {
  const num = Number(n);
  const idx = searched.indexOf(num);
  if (idx !== -1) {
    searched.splice(idx, 1);
    save();
  }
}

function removeIfInTop5(top5Numbers) {
  const set = new Set(top5Numbers.map(Number));
  const before = searched.length;
  searched = searched.filter(n => !set.has(n));
  if (searched.length !== before) save();
}

function isFull() {
  return searched.length >= MAX;
}

load();

module.exports = { getAll, has, add, remove, removeIfInTop5, isFull };
