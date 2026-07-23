const { Octokit } = require('@octokit/rest');
const config = require('../config');

let octokit = null;

function client() {
  if (!octokit) octokit = new Octokit({ auth: process.env.GITHUB_TOKEN });
  return octokit;
}

/**
 * Fetches all open issues (handles pagination, excludes pull requests).
 */
async function fetchOpenIssues() {
  const issues = [];
  let page = 1;
  while (true) {
    const res = await client().issues.listForRepo({
      owner: config.GITHUB_OWNER,
      repo: config.GITHUB_REPO,
      state: 'open',
      per_page: 100,
      page,
    });
    const filtered = res.data.filter(i => !i.pull_request);
    issues.push(...filtered);
    if (res.data.length < 100) break;
    page++;
  }
  return issues;
}

/**
 * Adds a label to an issue.
 */
async function addLabel(issueNumber, label) {
  await client().issues.addLabels({
    owner: config.GITHUB_OWNER,
    repo: config.GITHUB_REPO,
    issue_number: issueNumber,
    labels: [label],
  });
}

/**
 * Removes a label from an issue (no-op if label isn't present).
 */
async function removeLabel(issueNumber, label) {
  try {
    await client().issues.removeLabel({
      owner: config.GITHUB_OWNER,
      repo: config.GITHUB_REPO,
      issue_number: issueNumber,
      name: label,
    });
  } catch (err) {
    if (err.status !== 404) throw err;
  }
}

/**
 * Posts a comment on an issue.
 */
async function addComment(issueNumber, body) {
  await client().issues.createComment({
    owner: config.GITHUB_OWNER,
    repo: config.GITHUB_REPO,
    issue_number: issueNumber,
    body,
  });
}

/**
 * Closes an issue, optionally posting a comment first.
 */
async function closeIssue(issueNumber, comment) {
  if (comment) await addComment(issueNumber, comment);
  await client().issues.update({
    owner: config.GITHUB_OWNER,
    repo: config.GITHUB_REPO,
    issue_number: issueNumber,
    state: 'closed',
  });
}

/**
 * Creates a GitHub issue and returns { url, number }.
 * @param {string} title
 * @param {string} body
 * @param {string[]} labels  Array of label names e.g. ['BUG', 'P2']
 */
async function createIssue(title, body, labels) {
  const res = await client().issues.create({
    owner: config.GITHUB_OWNER,
    repo: config.GITHUB_REPO,
    title,
    body,
    labels: Array.isArray(labels) ? labels : [labels],
    assignees: config.GITHUB_ASSIGNEE ? [config.GITHUB_ASSIGNEE] : [],
  });
  return { url: res.data.html_url, number: res.data.number, data: res.data };
}

module.exports = { fetchOpenIssues, addLabel, removeLabel, addComment, closeIssue, createIssue };
