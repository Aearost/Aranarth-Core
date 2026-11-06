package com.aearost.aranarthcore.enums;

/**
 * The ways in which council, saint, and rank prefixes can be displayed.
 * Each player chooses how they see everyone's prefixes, while the console, logs, and Discord use the defaults.
 */
public enum PrefixStyle {
	FULL,
	SHORT,
	SYMBOL;

	/**
	 * Provides the default styles, used for the console, logs, Discord, and players who never changed them.
	 *
	 * @return The default council, saint, and rank prefix styles.
	 */
	public static PrefixStyle[] defaults() {
		return new PrefixStyle[]{SYMBOL, SYMBOL, FULL};
	}

	/**
	 * Provides the PrefixStyle matching the input, or the fallback if it does not match any style.
	 *
	 * @param name     The name of the style.
	 * @param fallback The style to use if the name is invalid.
	 * @return The matching PrefixStyle.
	 */
	public static PrefixStyle fromString(String name, PrefixStyle fallback) {
		if (name != null) {
			for (PrefixStyle style : values()) {
				if (style.name().equalsIgnoreCase(name)) {
					return style;
				}
			}
		}
		return fallback;
	}

	/**
	 * Encodes the three prefix styles into a single String, used for persistence and network syncing.
	 *
	 * @param council The council prefix style.
	 * @param saint   The saint prefix style.
	 * @param rank    The rank prefix style.
	 * @return The encoded styles in the order council,saint,rank.
	 */
	public static String encode(PrefixStyle council, PrefixStyle saint, PrefixStyle rank) {
		return council.name() + "," + saint.name() + "," + rank.name();
	}

	/**
	 * Decodes the styles created by encode(), falling back to the default styles for any invalid part.
	 *
	 * @param encoded The encoded styles.
	 * @return The council, saint, and rank prefix styles.
	 */
	public static PrefixStyle[] decode(String encoded) {
		String[] parts = encoded != null ? encoded.split(",") : new String[0];
		PrefixStyle[] styles = defaults();
		for (int i = 0; i < styles.length && i < parts.length; i++) {
			styles[i] = fromString(parts[i], styles[i]);
		}
		return styles;
	}
}
