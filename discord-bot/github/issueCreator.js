const { Octokit } = require('@octokit/rest');
const config = require('../config');

let octokit = null;

function client() {
  if (!octokit) octokit = new Octokit({ auth: process.env.GITHUB_TOKEN });
  return octokit;
}

/**
 * Creates a GitHub issue and returns its URL.
 * @param {string} title
 * @param {string} body
 * @param {string} label  e.g. 'BUG', 'IDEA', 'ABILITY'
 * @returns {Promise<string>} Issue HTML URL
 */
async function createIssue(title, body, label) {
  const res = await client().issues.create({
    owner: config.GITHUB_OWNER,
    repo: config.GITHUB_REPO,
    title,
    body,
    labels: [label],
  });
  return res.data.html_url;
}

module.exports = { createIssue };
