module.exports = {
  SUPPORT_CATEGORY_ID: '1528534493533700340',
  NEW_TICKET_CHANNEL_ID: '1528534669094555709',
  COUNCIL_ROLE_ID: '1436877816796020836',
  GITHUB_OWNER: 'Aearost',
  GITHUB_REPO: 'Aranarth-Core',

  EMOJIS: {
    BUG: '🐛',
    IDEA: '💡',
    ABILITY: '✨',
    QUESTION: '❓',
  },

  STATUS_EMOJIS: {
    OPEN: '🔎',
    AWAITING_INFO: '⏳',
    RESOLVED: '✅',
    FORCE_CLOSE: '🔒',
  },

  CONFIRM_EMOJIS: {
    SUBMIT: '✅',
    EDIT: '✏️',
    CANCEL: '❌',
  },

  // Auto-close delays
  AWAITING_INFO_MS: 5 * 24 * 60 * 60 * 1000, // 5 days
  RESOLVED_MS: 48 * 60 * 60 * 1000,           // 48 hours
  WARN_BEFORE_MS: 60 * 60 * 1000,             // warn 1 hour before close

  COLORS: {
    BUG: 0xFF4444,
    IDEA: 0xFFD700,
    ABILITY: 0x9B59B6,
    QUESTION: 0x3498DB,
    SUCCESS: 0x2ECC71,
    WARNING: 0xF39C12,
    ERROR: 0xFF4444,
    DEFAULT: 0x5865F2,
  },

  TICKET_PREFIXES: {
    BUG: 'bug',
    IDEA: 'idea',
    ABILITY: 'ability',
    QUESTION: 'question',
  },
};
