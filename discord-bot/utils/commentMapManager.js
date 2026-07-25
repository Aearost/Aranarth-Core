const fs = require('fs');
const path = require('path');

const FILE = path.join(__dirname, '..', 'data', 'commentMap.json');

// { discordToGithub: { discordMsgId: githubCommentId }, githubToDiscord: { githubCommentId: discordMsgId } }
let data = { discordToGithub: {}, githubToDiscord: {} };

function load() {
  try {
    data = JSON.parse(fs.readFileSync(FILE, 'utf8'));
    if (!data.discordToGithub) data.discordToGithub = {};
    if (!data.githubToDiscord) data.githubToDiscord = {};
  } catch {
    data = { discordToGithub: {}, githubToDiscord: {} };
  }
}

function save() {
  fs.writeFileSync(FILE, JSON.stringify(data, null, 2));
}

function set(discordMsgId, githubCommentId) {
  const id = String(githubCommentId);
  data.discordToGithub[discordMsgId] = id;
  data.githubToDiscord[id] = discordMsgId;
  save();
}

function getCommentId(discordMsgId) {
  return data.discordToGithub[discordMsgId] || null;
}

function getDiscordMessageId(githubCommentId) {
  return data.githubToDiscord[String(githubCommentId)] || null;
}

function remove(discordMsgId) {
  const commentId = data.discordToGithub[discordMsgId];
  if (commentId) delete data.githubToDiscord[commentId];
  delete data.discordToGithub[discordMsgId];
  save();
}

function removeByCommentId(githubCommentId) {
  const id = String(githubCommentId);
  const discordMsgId = data.githubToDiscord[id];
  if (discordMsgId) delete data.discordToGithub[discordMsgId];
  delete data.githubToDiscord[id];
  save();
  return discordMsgId || null;
}

load();

module.exports = { set, getCommentId, getDiscordMessageId, remove, removeByCommentId };
