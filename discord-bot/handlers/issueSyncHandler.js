const { EmbedBuilder } = require('discord.js');
const config = require('../config');
const forumManager = require('../utils/forumManager');
const { fetchOpenIssues, fetchIssueComments } = require('../github/githubManager');
const { createForumThreadFromIssue } = require('./forumHandler');

const COMMENT_DELAY_MS = 1000;  // delay between posting comments in a thread
const ISSUE_DELAY_MS  = 2000;   // delay between processing issues

function sleep(ms) {
  return new Promise(resolve => setTimeout(resolve, ms));
}

/**
 * Checks all open GitHub issues and creates Discord forum threads for any that
 * are missing one. Existing comments on each issue are posted into the thread
 * in chronological order so the thread reads naturally from the start.
 */
async function syncIssues(client) {
  console.log('[IssueSync] Checking for open issues without forum threads...');

  let issues;
  try {
    issues = await fetchOpenIssues();
  } catch (err) {
    console.error('[IssueSync] Failed to fetch open issues:', err.message);
    return;
  }

  const untracked = issues.filter(issue => !forumManager.getThreadId(issue.number));

  if (untracked.length === 0) {
    console.log('[IssueSync] All open issues have forum threads.');
    return;
  }

  console.log(`[IssueSync] Found ${untracked.length} issue(s) without forum threads — creating now.`);

  // Process oldest issues first
  untracked.sort((a, b) => a.number - b.number);

  for (const issue of untracked) {
    try {
      const thread = await createForumThreadFromIssue(client, issue);
      if (!thread) {
        console.warn(`[IssueSync] Skipping comments for issue #${issue.number} — thread creation failed.`);
        continue;
      }

      // Post existing comments in chronological order
      let comments;
      try {
        comments = await fetchIssueComments(issue.number);
      } catch (err) {
        console.error(`[IssueSync] Failed to fetch comments for issue #${issue.number}:`, err.message);
        comments = [];
      }

      for (const comment of comments) {
        await sleep(COMMENT_DELAY_MS);

        const body = comment.body.length > 4000
          ? comment.body.substring(0, 4000) + `\n\n*[...truncated — view full comment on GitHub](${issue.html_url})*`
          : comment.body;

        try {
          await thread.send({
            embeds: [
              new EmbedBuilder()
                .setAuthor({
                  name: comment.user.login,
                  url: `https://github.com/${comment.user.login}`,
                  iconURL: comment.user.avatar_url,
                })
                .setDescription(body)
                .setColor(config.COLORS.DEFAULT)
                .setFooter({ text: '💬 GitHub Comment' })
                .setTimestamp(new Date(comment.created_at)),
            ],
          });
        } catch (err) {
          console.error(`[IssueSync] Failed to post comment on thread for issue #${issue.number}:`, err.message);
        }
      }

      console.log(`[IssueSync] Created thread for issue #${issue.number} with ${comments.length} comment(s).`);
    } catch (err) {
      console.error(`[IssueSync] Failed to process issue #${issue.number}:`, err.message);
    }

    await sleep(ISSUE_DELAY_MS);
  }

  console.log('[IssueSync] Sync complete.');
}

module.exports = { syncIssues };
