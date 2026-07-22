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

  return (priorityBase * typeMultiplier + ageBonus + activityBonus) * onHoldMultiplier;
}

function isWip(issue) {
  return issue.labels.some(l => l.name === 'WIP');
}

function isOnHold(issue) {
  return issue.labels.some(l => l.name === 'ON HOLD');
}

/**
 * Selects top issues. WIP always included; remaining slots (up to 5) filled by score.
 */
function selectTop(issues) {
  const wipIssues = issues.filter(isWip);
  const others = issues.filter(i => !isWip(i));

  const scored = others
    .map(i => ({ issue: i, score: score(i) }))
    .sort((a, b) => b.score - a.score);

  const remaining = Math.max(0, 5 - wipIssues.length);
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
