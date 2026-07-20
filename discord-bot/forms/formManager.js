// Active form sessions: userId -> session
const activeSessions = new Map();
// Channel -> userId lookup
const channelToUser = new Map();

function createSession(userId, channelId, type) {
  const session = {
    userId,
    channelId,
    type,
    currentStep: 0,
    answers: {},
    state: 'ANSWERING', // 'ANSWERING' | 'CONFIRMING' | 'EDITING'
    editMode: false,
    confirmMessageId: null,
  };
  activeSessions.set(userId, session);
  channelToUser.set(channelId, userId);
  return session;
}

function getSession(userId) {
  return activeSessions.get(userId) || null;
}

function getSessionByChannel(channelId) {
  const userId = channelToUser.get(channelId);
  return userId ? activeSessions.get(userId) || null : null;
}

function hasActiveSession(userId) {
  return activeSessions.has(userId);
}

function deleteSession(userId, channelId) {
  activeSessions.delete(userId);
  if (channelId) channelToUser.delete(channelId);
}

function deleteSessionByChannel(channelId) {
  const userId = channelToUser.get(channelId);
  if (userId) activeSessions.delete(userId);
  channelToUser.delete(channelId);
}

module.exports = {
  createSession,
  getSession,
  getSessionByChannel,
  hasActiveSession,
  deleteSession,
  deleteSessionByChannel,
};
