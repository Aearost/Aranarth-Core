package com.aearost.aranarthcore.objects;

import org.bukkit.Location;
import org.bukkit.Material;

/**
 * Handles all necessary functionality relating to homes in Aranarth.
 */
public class Home {

	private final String name;
	private Location location;
	private final Material icon;
	
	public Home(String homeName, Location location, Material icon) {
		homeName = homeName.replaceAll("\\|", "");
		homeName = homeName.replaceAll("_", "");
		this.name = homeName;
		this.location = location;
		this.icon = icon;
	}

	/**
	 * Provides the current home name.
	 * @return the current home name.
	 */
	public String getName() {
		return name;
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
