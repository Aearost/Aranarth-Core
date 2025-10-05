package com.aearost.aranarthcore.objects;

import org.bukkit.Location;

import java.util.List;
import java.util.UUID;

/**
 * Handles all necessary functionality relating to a locked container on Aranarth.
 */
public class LockedContainer {

	private UUID owner;
	private List<UUID> trusted;
	private Location[] locations;

	public LockedContainer(UUID owner, List<UUID> trusted, Location[] locations) {
		this.owner = owner;
		this.trusted = trusted;
		this.locations = locations;
	}

	/**
	 * Provides the locked container owner's UUID.
	 * @return the locked container owner's UUID.
	 */
	public UUID getOwner() {
		return owner;
	}

	/**
	 * Updates the locked container's owner's UUID.
	 * @param owner The locked container's owner's UUID.
     */
	public void setOwner(UUID owner) {
		this.owner = owner;
	}

	/**
	 * Provides the list of players that are trusted to open the locked container.
	 * @return the list of players that are trusted to open the locked container.
	 */
	public List<UUID> getTrusted() {
		return this.trusted;
	}

	/**
	 * Updates the list of players that are trusted to open the locked container.
	 * @param trusted The list of players that will be trusted to open the locked container.
	 */
	public void setTrusted(List<UUID> trusted) {
		this.trusted = trusted;
	}

	/**
	 * Provides the current Locations of the locked container.
	 * @return The current Locations of the locked container.
	 */
	public Location[] getLocations() {
		return locations;
	}

	/**
	 * Updates the locations of the locked container.
	 * @param locations The new locations of the locked container.
	 */
	public void setLocations(Location[] locations) {
		this.locations = locations;
	}


}
