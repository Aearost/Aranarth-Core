function skipOrValue(val) {
  return !val || val.toLowerCase() === 'skip' ? 'N/A' : val;
}

const TEMPLATES = {
  BUG: {
    color: 0xFF4444,
    emoji: '🐛',
    displayName: 'Bug Report',
    label: 'BUG',
    questions: [
      {
        key: 'title',
        label: 'Title',
        prompt: '**Step 1 of 6 — Title**\nWhat is the title of this bug? Keep it short and descriptive.',
      },
      {
        key: 'description',
        label: 'Detailed Description',
        prompt: '**Step 2 of 6 — Detailed Description**\nWith as much information as possible, explain the issue.',
      },
      {
        key: 'expectedActual',
        label: 'Expected vs Actual Behaviour',
        prompt: '**Step 3 of 6 — Expected vs Actual Behaviour**\nWhat is expected to happen, compared to what is actually happening?',
      },
      {
        key: 'steps',
        label: 'Steps to Reproduce',
        prompt: '**Step 4 of 6 — Steps to Reproduce**\nWhat is done to cause the issue to occur?',
      },
      {
        key: 'proposedFix',
        label: 'Proposed Fix (if applicable)',
        prompt: '**Step 5 of 6 — Proposed Fix** *(optional)*\nAny suggestions on how to implement a fix? Type `skip` to leave this blank.',
        optional: true,
      },
      {
        key: 'additionalContext',
        label: 'Additional Context',
        prompt: '**Step 6 of 6 — Additional Context** *(optional)*\nAdd any other context about the problem here. Type `skip` to leave this blank.',
        optional: true,
      },
    ],
    buildBody(answers, reportedBy) {
      return [
        `**Detailed Description:**\n${answers.description}`,
        `**Expected vs Actual Behaviour:**\n${answers.expectedActual}`,
        `**Steps to Reproduce:**\n${answers.steps}`,
        `**Proposed Fix (if applicable):**\n${skipOrValue(answers.proposedFix)}`,
        `**Reported By**\n${reportedBy}`,
        `**Additional context**\n${skipOrValue(answers.additionalContext)}`,
      ].join('\n\n');
    },
    issueTitle: (answers) => `[BUG] ${answers.title}`,
  },

  IDEA: {
    color: 0xFFD700,
    emoji: '💡',
    displayName: 'Idea Suggestion',
    label: 'IDEA',
    questions: [
      {
        key: 'title',
        label: 'Title',
        prompt: '**Step 1 of 5 — Title**\nWhat is the title of your idea?',
      },
      {
        key: 'explanation',
        label: 'Explanation of Suggestion',
        prompt: '**Step 2 of 5 — Explanation of Suggestion**\nA clear and concise description of what you want to happen.',
      },
      {
        key: 'purpose',
        label: 'Purpose of Suggestion',
        prompt: '**Step 3 of 5 — Purpose of Suggestion**\nWhat will adding this suggestion do for the server? Who will benefit from it?',
      },
      {
        key: 'implementation',
        label: 'Proposed Way to Implement',
        prompt: '**Step 4 of 5 — Proposed Way to Implement**\nHow would you suggest this is implemented?',
      },
      {
        key: 'additionalContext',
        label: 'Additional Context',
        prompt: '**Step 5 of 5 — Additional Context** *(optional)*\nAdd any other context or screenshots about the feature request here. Type `skip` to leave this blank.',
        optional: true,
      },
    ],
    buildBody(answers, suggestedBy) {
      return [
        `**Explanation of Suggestion**\n${answers.explanation}`,
        `**Purpose of Suggestion**\n${answers.purpose}`,
        `**Proposed Way to Implement**\n${answers.implementation}`,
        `**Suggested By**\n${suggestedBy}`,
        `**Additional context**\n${skipOrValue(answers.additionalContext)}`,
      ].join('\n\n');
    },
    issueTitle: (answers) => `[IDEA] ${answers.title}`,
  },

  ABILITY: {
    color: 0x9B59B6,
    emoji: '✨',
    displayName: 'Ability Suggestion',
    label: 'ABILITY',
    questions: [
      {
        key: 'title',
        label: 'Title',
        prompt: '**Step 1 of 5 — Title**\nWhat is the title of your ability suggestion?',
      },
      {
        key: 'element',
        label: 'Element/Sub-Element',
        prompt: '**Step 2 of 5 — Element/Sub-Element**\nWhat is the name of the element and/or its Sub-Element?',
      },
      {
        key: 'explanation',
        label: 'Explanation of Suggestion',
        prompt: '**Step 3 of 5 — Explanation of Suggestion**\nA clear and concise description of what the ability will look like.',
      },
      {
        key: 'howToUse',
        label: 'How to Use Ability',
        prompt: '**Step 4 of 5 — How to Use Ability**\nWhat kinds of clicks, is there a charge, specific block source, required items, etc.?',
      },
      {
        key: 'additionalContext',
        label: 'Additional Context',
        prompt: '**Step 5 of 5 — Additional Context** *(optional)*\nAdd any other context or screenshots about the ability here. Type `skip` to leave this blank.',
        optional: true,
      },
    ],
    buildBody(answers, suggestedBy) {
      return [
        `**Element/Sub-Element**\n${answers.element}`,
        `**Explanation of Suggestion**\n${answers.explanation}`,
        `**How to Use Ability**\n${answers.howToUse}`,
        `**Suggested By**\n${suggestedBy}`,
        `**Additional context**\n${skipOrValue(answers.additionalContext)}`,
      ].join('\n\n');
    },
    issueTitle: (answers) => `[ABILITY] ${answers.title}`,
  },
};

module.exports = TEMPLATES;
