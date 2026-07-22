module.exports = {
  SUPPORT_CATEGORY_ID: '1528534493533700340',
  NEW_TICKET_CHANNEL_ID: '1528534669094555709',
  COUNCIL_ROLE_ID: '1436877816796020836',
  GITHUB_OWNER: 'Aearost',
  GITHUB_REPO: 'Aranarth-Core',
  GITHUB_ASSIGNEE: 'Aearost',

  EMOJIS: {
    BUG: '🐛',
    IDEA: '💡',
    ABILITY: '✨',
    QUESTION: '❓',
  },

  STATUS_EMOJIS: {
    OPEN: '🔎',
    RESOLVED: '✅',
    CLOSE: '🔒',
  },

  CONFIRM_EMOJIS: {
    SUBMIT: '✅',
    EDIT: '✏️',
    CANCEL: '❌',
  },

  // Auto-close delays
  INACTIVITY_NOTIFY_MS: 60 * 60 * 1000,        // 1 hour after council responds with no activity → notify player
  INACTIVITY_CLOSE_MS: 5 * 24 * 60 * 60 * 1000, // 5 days after notification → close
  RESOLVED_MS: 48 * 60 * 60 * 1000,             // 48 hours after resolved → close
  WARN_BEFORE_MS: 60 * 60 * 1000,               // warn 1 hour before close
  FORM_INACTIVITY_MS: 24 * 60 * 60 * 1000,      // 24 hours of no activity in a bug/idea/ability form channel → delete

  COLORS: {
    BUG: 0xFF4444,
    IDEA: 0xFFD700,
    ABILITY: 0x9B59B6,
    QUESTION: 0x3498DB,
    SUCCESS: 0x2ECC71,
    WARNING: 0xF39C12,
    ERROR: 0xFF4444,
    DEFAULT: 0x5865F2,
    P1: 0xE53935,
    P2: 0xFB8C00,
    P3: 0xFDD835,
    P4: 0x43A047,
  },

  TICKET_PREFIXES: {
    BUG: 'bug',
    IDEA: 'idea',
    ABILITY: 'ability',
    QUESTION: 'support',
  },

  PRIORITY_REVIEW_CHANNEL_ID: '1529242982912360508',
  WORK_QUEUE_CHANNEL_ID: '1529244791634792558',
  FORUM_CHANNEL_ID: '1529310558673834005',

  // Emoji characters used for priority review (emoji.name returns the Unicode char)
  PRIORITY_EMOJIS: {
    P1: '🔴',
    P2: '🟠',
    P3: '🟡',
    P4: '🟢',
    EDIT: '📝',
    REJECT: '❌',
  },

  // Emoji characters used for work queue status reactions
  WORK_QUEUE_EMOJIS: {
    IN_PROGRESS: '▶️',
    TAKE_NOTE: '💬',
    ON_HOLD: '⏸️',
    CLOSE: '🔒',
  },
};
