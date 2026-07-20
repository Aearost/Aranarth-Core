# Aranarth Discord Bot — Setup Guide

## Prerequisites

- [Node.js](https://nodejs.org/) v18 or higher
- A Discord bot application (see below)
- A GitHub Personal Access Token

---

## Step 1 — Create a Discord Bot

1. Go to the [Discord Developer Portal](https://discord.com/developers/applications)
2. Click **New Application**, give it a name (e.g. `Aranarth Support`)
3. Go to the **Bot** tab → click **Add Bot**
4. Under **Privileged Gateway Intents**, enable:
   - **Server Members Intent**
   - **Message Content Intent**
5. Click **Reset Token** and copy your bot token — you'll need it shortly

### Invite the bot to your server

In the **OAuth2 → URL Generator** tab:
- Scopes: `bot`
- Bot Permissions: `Manage Channels`, `View Channels`, `Send Messages`, `Read Message History`, `Add Reactions`, `Manage Messages`, `Embed Links`, `Attach Files`, `Manage Roles` *(needed to set channel overwrites)*

Copy the generated URL and open it to invite the bot.

---

## Step 2 — Create a GitHub Personal Access Token

1. Go to [GitHub → Settings → Developer settings → Personal access tokens → Tokens (classic)](https://github.com/settings/tokens)
2. Click **Generate new token (classic)**
3. Give it a name, set expiration as desired
4. Under **Scopes**, check **`repo`** (this covers creating issues)
5. Click **Generate token** and copy it

---

## Step 3 — Configure Environment Variables

In the `discord-bot/` directory, copy `.env.example` to `.env`:

```
DISCORD_TOKEN=paste_your_discord_bot_token_here
GITHUB_TOKEN=paste_your_github_token_here
```

---

## Step 4 — Install Dependencies & Run

```bash
cd discord-bot
npm install
npm start
```

On first launch the bot will automatically post the support message to the `#new-support-ticket` channel and save its message ID to `data/activeMessage.json`.

For development with auto-restart on file changes:

```bash
npm run dev
```

---

## Notes

- `data/activeMessage.json` is git-ignored (generated at runtime). If you delete the support message in Discord, delete this file too so the bot recreates it on next start.
- `data/counters.json` persists ticket numbers. Back this up if needed.
- `data/timers.json` persists pending auto-close timers across restarts.
- If the bot restarts mid-form, in-progress form sessions are lost (the channels remain but become orphaned). These can be deleted manually.
