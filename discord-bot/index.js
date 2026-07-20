require('dotenv').config();
const fs = require('fs');
const path = require('path');
const {
  Client,
  GatewayIntentBits,
  Partials,
  Events,
  EmbedBuilder,
} = require('discord.js');

const config = require('./config');
const reactionHandler = require('./handlers/reactionHandler');
const messageHandler = require('./handlers/messageHandler');
const timerManager = require('./utils/timerManager');
const statusTracker = require('./utils/statusTracker');

const client = new Client({
  intents: [
    GatewayIntentBits.Guilds,
    GatewayIntentBits.GuildMessages,
    GatewayIntentBits.GuildMessageReactions,
    GatewayIntentBits.GuildMembers,
    GatewayIntentBits.MessageContent,
  ],
  partials: [
    Partials.Message,
    Partials.Channel,
    Partials.Reaction,
  ],
});

client.once(Events.ClientReady, async (c) => {
  console.log(`[Bot] Logged in as ${c.user.tag}`);
  statusTracker.load();
  await setupSupportMessage(client);
  timerManager.restoreTimers(client);
});

client.on(Events.MessageReactionAdd, async (reaction, user) => {
  if (user.bot) return;

  try {
    if (reaction.partial) await reaction.fetch();
    if (reaction.message.partial) await reaction.message.fetch();
  } catch (err) {
    console.error('[Bot] Failed to fetch partial reaction/message:', err.message);
    return;
  }

  await reactionHandler.handle(reaction, user, client);
});

client.on(Events.MessageCreate, async (message) => {
  if (message.author.bot) return;
  await messageHandler.handle(message, client);
});

// ── Support message setup ───────────────────────────────────────────────────

const ACTIVE_MSG_PATH = path.join(__dirname, 'data', 'activeMessage.json');

async function setupSupportMessage(c) {
  const channel = await c.channels.fetch(config.NEW_TICKET_CHANNEL_ID).catch(() => null);
  if (!channel) {
    console.error('[Bot] Could not fetch new-support-ticket channel. Check NEW_TICKET_CHANNEL_ID.');
    return;
  }

  // Check if the saved message still exists
  let savedId = null;
  try {
    const raw = fs.readFileSync(ACTIVE_MSG_PATH, 'utf8');
    savedId = JSON.parse(raw).messageId;
  } catch { /* file not yet created */ }

  if (savedId) {
    try {
      await channel.messages.fetch(savedId);
      console.log('[Bot] Support message already exists — skipping creation.');
      return;
    } catch { /* message was deleted, recreate */ }
  }

  const embed = new EmbedBuilder()
    .setTitle('🌟 Aranarth Support')
    .setDescription(
      'Need help or want to contribute to the server? React with the appropriate emoji below to open a ticket!\n\n' +
      '🐛 **Bug Report** — Found something that\'s not working as intended?\n' +
      '💡 **Idea** — Have a suggestion to improve the server?\n' +
      '✨ **Ability** — Want to suggest a new ability?\n' +
      '❓ **Question** — Have a question for the Council?'
    )
    .setColor(config.COLORS.DEFAULT)
    .setFooter({ text: 'Aranarth Support System' });

  const msg = await channel.send({ embeds: [embed] });
  await msg.react(config.EMOJIS.BUG);
  await msg.react(config.EMOJIS.IDEA);
  await msg.react(config.EMOJIS.ABILITY);
  await msg.react(config.EMOJIS.QUESTION);

  fs.writeFileSync(ACTIVE_MSG_PATH, JSON.stringify({ messageId: msg.id }));
  console.log(`[Bot] Support message created (ID: ${msg.id}).`);
}

client.login(process.env.DISCORD_TOKEN);
