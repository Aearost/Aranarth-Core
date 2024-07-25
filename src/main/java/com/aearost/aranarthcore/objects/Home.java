package com.aearost.aranarthcore.objects;

import org.bukkit.Location;
import org.bukkit.Material;

/**
 * Handles all necessary functionality relating to a homepad on Aranarth.
 */
public class Home {

	private final String homeName;
	private Location location;
	private final Material icon;
	
	public Home(String homeName, Location location, Material icon) {
		this.homeName = homeName;
		this.location = location;
		this.icon = icon;
	}

	/**
	 * Provides the current home name.
	 * @return the current home name.
	 */
	public String getHomeName() {
		return homeName;
	}

	/**
	 * Provides the current Location of the homepad.
	 * @return The current Location.
	 */
	public Location getLocation() {
		return location;
	}

	/**
	 * Updates the Location of the player.
	 * @param location The new Location.
	 */
	public void setLocation(Location location) {
		this.location = location;
	}

	/**
	 * Provides the current Icon of the homepad.
	 * @return The current Icon.
	 */
	public Material getIcon() {
		return icon;
	}
}
