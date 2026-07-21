const https = require('https');
const { Octokit } = require('@octokit/rest');
const config = require('../config');

let octokit = null;

function client() {
  if (!octokit) octokit = new Octokit({ auth: process.env.GITHUB_TOKEN });
  return octokit;
}

/**
 * Downloads a URL and returns a Buffer.
 */
function downloadBuffer(url) {
  return new Promise((resolve, reject) => {
    https.get(url, (res) => {
      const chunks = [];
      res.on('data', chunk => chunks.push(chunk));
      res.on('end', () => resolve(Buffer.concat(chunks)));
      res.on('error', reject);
    }).on('error', reject);
  });
}

/**
 * Uploads a file to the GitHub repo and returns its raw URL.
 * @param {string} fileName  original filename (e.g. "image.png")
 * @param {string} discordUrl  Discord CDN URL to download from
 * @param {string} issueLabel  e.g. 'BUG'
 * @returns {Promise<string>} Permanent raw.githubusercontent.com URL
 */
async function uploadAttachment(fileName, discordUrl, issueLabel) {
  const buf = await downloadBuffer(discordUrl);
  const timestamp = Date.now();
  const safeName = fileName.replace(/[^a-zA-Z0-9._-]/g, '_');
  const repoPath = `attachments/${issueLabel.toLowerCase()}/${timestamp}_${safeName}`;

  await client().repos.createOrUpdateFileContents({
    owner: config.GITHUB_OWNER,
    repo: config.GITHUB_REPO,
    path: repoPath,
    message: `chore: add ticket attachment ${safeName}`,
    content: buf.toString('base64'),
  });

  return `https://raw.githubusercontent.com/${config.GITHUB_OWNER}/${config.GITHUB_REPO}/master/${repoPath}`;
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

module.exports = { createIssue, uploadAttachment };
