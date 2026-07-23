const councilActivityManager = require('./councilActivityManager');
const holdTimestampManager = require('./holdTimestampManager');

const PRIORITY_BASE = { P1: 10000, P2: 1000, P3: 100, P4: 10 };
const DEFAULT_BASE = 1;

function score(issue) {
  const labels = issue.labels.map(l => l.name);

  let priorityBase = DEFAULT_BASE;
  for (const [p, val] of Object.entries(PRIORITY_BASE)) {
    if (labels.includes(p)) { priorityBase = val; break; }
  }

  const typeMultiplier = labels.includes('BUG') ? 1.5 : 1.0;

  const daysSince = (Date.now() - new Date(issue.created_at).getTime()) / (1000 * 60 * 60 * 24);
  const ageBonus = Math.min(daysSince * 2, 730);

  const activityBonus = Math.min((issue.comments || 0) * 5, 50);

  const onHoldMultiplier = labels.includes('ON HOLD') ? 0.05 : 1.0;

  // Issues untouched by council for longer score progressively higher to prevent them
  // from being perpetually buried. Grows by 3 pts/day after 7 idle days, capped at 150.
  const lastWorked = councilActivityManager.getTimestamp(issue.number);
  const idleDays = lastWorked
    ? (Date.now() - lastWorked) / (1000 * 60 * 60 * 24)
    : daysSince; // never touched → treat age as idle time
  const stalenessBonus = idleDays > 7 ? Math.min((idleDays - 7) * 3, 150) : 0;

  // On-hold issues resurface at a rate that depends on their priority.
  // P1 re-enters scoring within 2 days; P4 can wait up to 20 days per interval.
  // Applied outside the suppression multiplier so the bonus is never dampened.
  const HOLD_INTERVAL_DAYS_BY_PRIORITY = { P1: 2, P2: 5, P3: 10, P4: 20 };
  const HOLD_BONUS_PER_INTERVAL = 100;
  const holdStart = holdTimestampManager.getTimestamp(issue.number);
  const holdIntervalDays = HOLD_INTERVAL_DAYS_BY_PRIORITY[getPriority(issue)] ?? 15;
  const holdAgeBonus = (holdStart && labels.includes('ON HOLD'))
    ? Math.floor((Date.now() - holdStart) / (1000 * 60 * 60 * 24 * holdIntervalDays)) * HOLD_BONUS_PER_INTERVAL
    : 0;

  return (priorityBase * typeMultiplier + ageBonus + activityBonus + stalenessBonus) * onHoldMultiplier + holdAgeBonus;
}

function isWip(issue) {
  return issue.labels.some(l => l.name === 'WIP');
}

function isOnHold(issue) {
  return issue.labels.some(l => l.name === 'ON HOLD');
}

const QUEUE_LIMIT = 5;

/**
 * Selects top issues, capped at QUEUE_LIMIT total.
 * WIP issues take priority slots first (sorted by score), non-WIP fill the rest.
 */
function selectTop(issues) {
  const wipIssues = issues
    .filter(isWip)
    .map(i => ({ issue: i, score: score(i) }))
    .sort((a, b) => b.score - a.score)
    .map(s => s.issue)
    .slice(0, QUEUE_LIMIT);

  const scored = issues
    .filter(i => !isWip(i))
    .map(i => ({ issue: i, score: score(i) }))
    .sort((a, b) => b.score - a.score);

  const remaining = Math.max(0, QUEUE_LIMIT - wipIssues.length);
  return [...wipIssues, ...scored.slice(0, remaining).map(s => s.issue)];
}

function getPriority(issue) {
  const labels = issue.labels.map(l => l.name);
  for (const p of ['P1', 'P2', 'P3', 'P4']) {
    if (labels.includes(p)) return p;
  }
  return null;
}

function getType(issue) {
  const labels = issue.labels.map(l => l.name);
  if (labels.includes('BUG')) return 'BUG';
  if (labels.includes('IDEA')) return 'IDEA';
  if (labels.includes('ABILITY')) return 'ABILITY';
  return null;
}

function getStatus(issue) {
  if (isWip(issue)) return 'wip';
  if (isOnHold(issue)) return 'on-hold';
  return 'normal';
}

module.exports = { score, selectTop, getPriority, getType, getStatus, isWip, isOnHold };
