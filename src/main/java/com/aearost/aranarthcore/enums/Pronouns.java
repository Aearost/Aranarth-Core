package com.aearost.aranarthcore.enums;

import java.util.Locale;

/**
 * Used to differentiate the pronouns of a player, which determine the rank titles they are shown with.
 */
public enum Pronouns {
	MALE("Peasant", "Esquire", "Knight", "Baron", "Count", "Duke", "Prince", "King", "Emperor"),
	FEMALE("Peasant", "Esquire", "Knight", "Baroness", "Countess", "Duchess", "Princess", "Queen", "Empress"),
	NEUTRAL("Peasant", "Esquire", "Knight", "Banneret", "Consul", "Dux", "Heir", "Monarch", "Sovereign");

	// The rank titles, indexed from Peasant (0) to Emperor (8)
	private final String[] rankNames;

	Pronouns(String... rankNames) {
		this.rankNames = rankNames;
	}

	/**
	 * Provides the display name of the rank for these pronouns.
	 *
	 * @param rank The rank, where 0 is Peasant and 8 is Emperor.
	 * @return The display name of the rank.
	 */
	public String getRankName(int rank) {
		return rankNames[rank];
	}

	/**
	 * Provides the suffix of the rank's lang keys for these pronouns, such as rank.title_baroness.
	 *
	 * @param rank The rank, where 0 is Peasant and 8 is Emperor.
	 * @return The lang key suffix of the rank.
	 */
	public String getRankId(int rank) {
		return rankNames[rank].toLowerCase(Locale.ROOT);
	}

	/**
	 * Provides the pronouns with the given name, such as those synced from the other server.
	 *
	 * @param name The name of the pronouns.
	 * @return The matching pronouns, or MALE if there are none.
	 */
	public static Pronouns fromName(String name) {
		for (Pronouns pronouns : values()) {
			if (pronouns.name().equals(name)) {
				return pronouns;
			}
		}
		return MALE;
	}
}
