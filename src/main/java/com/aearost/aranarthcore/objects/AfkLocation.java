package com.aearost.aranarthcore.objects;

import org.bukkit.Location;

/**
 * Handles all necessary functionality relating to an AFK Player on Aranarth
 */
public class AfkLocation {
	private Location location;
	private int seconds;

	public AfkLocation(Location location, int seconds) {
		this.location = location;
		this.seconds = seconds;
	}

	/**
	 * Provides the last known Location of the player.
	 * @return The last known Location of the player.
	 */
	public Location getLocation() {
		return location;
	}

	/**
	 * Updates the last known Location of the player.
	 * @param location The last known Location of the player.
	 */
	public void setLocation(Location location) {
		this.location = location;
	}

	/**
	 * Provides the number of seconds the player has been in the AFK Location.
	 * @return The number of seconds the player has been in the AFK Location.
	 */
	public int getSeconds() {
		return seconds;
	}

	/**
	 * Updates the number of seconds the player has been in the AFK Location.
	 * @param seconds The number of seconds the player has been in the AFK Location.
	 */
	public void setSeconds(int seconds) {
		this.seconds = seconds;
	}
}
