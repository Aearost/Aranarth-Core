This directory contains the internationalisation language files for AranarthCore.

Each file follows the Java .properties format and is named after its locale:
  en_US.properties - English (default)
  fr_FR.properties - French
  de_DE.properties - German
  pt_BR.properties - Brazilian Portuguese

All .properties files in this directory are excluded from version control via .gitignore.
This is because their large size causes commit analysis to take several minutes to complete.

Every locale file must be a strict 1:1 key match with en_US.properties.
When adding new keys to en_US, add the corresponding key to all other locale files as well.
