package com.aearost.aranarthcore.objects;

import java.util.UUID;

/**
 * Handles all necessary functionality relating to an avatar on Aranarth.
 */
public class Avatar {
	private UUID uuid;
	private String startInGame;
	private String endInGame;
	private String startInRealLife;
	private String endInRealLife;
	private char element;

	public Avatar(UUID uuid, String startInGame, String endInGame, String startInRealLife, String endInRealLife, char element) {
		this.uuid = uuid;
		this.startInGame = startInGame;
		this.endInGame = endInGame;
		this.startInRealLife = startInRealLife;
		this.endInRealLife = endInRealLife;
		this.element = element;
	}

	/**
	 * Provides the punished player's UUID.
	 * @return the punished player's UUID.
	 */
	public UUID getUuid() {
		return uuid;
	}

	/**
	 * Updates the punished player's UUID.
	 * @param uuid The punished player's UUID.
     */
	public void setUuid(UUID uuid) {
		this.uuid = uuid;
	}

	/**
	 * Provides the start date from the in-game date of the avatar's reign.
	 */
	public String getStartInGame() {
		return startInGame;
	}

	/**
	 * Updates the start date from the in-game date of the avatar's reign.
	 * @param startInGame The start date from the in-game date of the avatar's reign.
	 */
	public void setStartInGame(String startInGame) {
		this.startInGame = startInGame;
	}

	/**
	 * Provides the end date from the in-game date of the avatar's reign.
	 */
	public String getEndInGame() {
		return endInGame;
	}

	/**
	 * Updates the end date from the in-game date of the avatar's reign.
	 * @param endInGame The end date from the in-game date of the avatar's reign.
	 */
	public void setEndInGame(String endInGame) {
		this.endInGame = endInGame;
	}

	/**
	 * Provides the start date from the in real life date of the avatar's reign.
	 */
	public String getStartInRealLife() {
		return startInRealLife;
	}

	/**
	 * Updates the start date from the in real life date of the avatar's reign.
	 * @param startInRealLife The start date from the in real life date of the avatar's reign.
	 */
	public void setStartInRealLife(String startInRealLife) {
		this.startInRealLife = startInRealLife;
	}

	/**
	 * Provides the end date from the in real life date of the avatar's reign.
	 */
	public String getEndInRealLife() {
		return endInRealLife;
	}

	/**
	 * Updates the end date from the in real life date of the avatar's reign.
	 * @param endInRealLife The end date from the in real life date of the avatar's reign.
	 */
	public void setEndInRealLife(String endInRealLife) {
		this.endInRealLife = endInRealLife;
	}

	/**
	 * Provides the reason of the punishment.
	 * @return The reason of the punishment.
	 */
	public char getElement() {
		return element;
	}

	/**
	 * Updates the original element of the avatar.
	 * @param element The original element of the avatar.
	 */
	public void setElement(char element) {
		this.element = element;
	}

}
