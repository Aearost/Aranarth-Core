const http = require('http');
const crypto = require('crypto');
const commentMapManager = require('./commentMapManager');
const forumManager = require('./forumManager');

function verify(secret, body, sig) {
  if (!secret) return true;
  const hmac = crypto.createHmac('sha256', secret).update(body).digest('hex');
  try {
    return crypto.timingSafeEqual(Buffer.from(`sha256=${hmac}`), Buffer.from(sig || ''));
  } catch {
    return false;
  }
}

function start(client) {
  const port = process.env.WEBHOOK_PORT || 3000;
  const secret = process.env.GITHUB_WEBHOOK_SECRET || '';

  const server = http.createServer(async (req, res) => {
    if (req.method !== 'POST' || req.url !== '/github-webhook') {
      res.writeHead(404); res.end(); return;
    }
    let body = '';
    req.on('data', d => body += d);
    req.on('end', async () => {
      const sig = req.headers['x-hub-signature-256'] || '';
      if (!verify(secret, body, sig)) {
        res.writeHead(401); res.end('Unauthorized'); return;
      }
      res.writeHead(200); res.end('OK');
      try {
        const payload = JSON.parse(body);
        if (payload.action === 'deleted' && payload.comment) {
          const commentId = payload.comment.id;
          const discordMsgId = commentMapManager.getDiscordMessageId(commentId);
          if (!discordMsgId) return;
          const issueNumber = payload.issue?.number;
          if (!issueNumber) return;
          const threadId = forumManager.getThreadId(issueNumber);
          if (!threadId) return;
          try {
            const thread = await client.channels.fetch(threadId).catch(() => null);
            if (!thread) return;
            const msg = await thread.messages.fetch(discordMsgId).catch(() => null);
            if (msg) await msg.delete();
          } catch (err) {
            console.error('[Webhook] Failed to delete Discord message:', err.message);
          }
          commentMapManager.removeByCommentId(commentId);
        }
      } catch (err) {
        console.error('[Webhook] Failed to parse payload:', err.message);
      }
    });
  });

  server.listen(port, () => console.log(`[Webhook] Server listening on port ${port}`));
}

module.exports = { start };
