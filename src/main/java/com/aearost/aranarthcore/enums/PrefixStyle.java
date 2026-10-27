package com.aearost.aranarthcore.enums;

/**
 * The ways in which a player's council, saint, and rank prefixes can be displayed.
 */
public enum PrefixStyle {
	FULL,
	SHORT,
	SYMBOL;

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
	 * Encodes the three prefix styles into a single String, used for network and roster syncing.
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
		return new PrefixStyle[]{
				fromString(parts.length > 0 ? parts[0] : null, SYMBOL),
				fromString(parts.length > 1 ? parts[1] : null, SYMBOL),
				fromString(parts.length > 2 ? parts[2] : null, FULL)
		};
	}
}
