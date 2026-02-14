package com.aearost.aranarthcore.objects;

import org.bukkit.Location;
import org.bukkit.entity.EntityType;

import java.util.UUID;

/**
 * Handles all necessary functionality relating to Sentinel mobs on Aranarth.
 */
public class Sentinel {
	private UUID uuid;
	private EntityType type;
	private Location location;

	public Sentinel(UUID uuid, EntityType type, Location location) {
		this.uuid = uuid;
		this.type = type;
		this.location = location;
	}

	/**
	 * Provides the sentinel's UUID.
	 * @return the sentinel's UUID.
	 */
	public UUID getUuid() {
		return uuid;
	}

	/**
	 * Updates the sentinel's UUID.
	 * @param uuid The sentinel's UUID.
     */
	public void setUuid(UUID uuid) {
		this.uuid = uuid;
	}

	/**
	 * Provides the EntityType of the sentinel.
	 * @return The EntityType of the sentinel.
	 */
	public EntityType getType() {
		return type;
	}

	/**
	 * Updates the EntityType of the sentinel.
	 * @param type The EntityType of the sentinel.
	 */
	public void setType(EntityType type) {
		this.type = type;
	}

	/**
	 * Provides the last known Location of the sentinel.
	 * @return The last known Location of the sentinel.
	 */
	public Location getLocation() {
		return location;
	}

	/**
	 * Updates the last known Location of the sentinel.
	 * @param location The last known Location of the sentinel.
	 */
	public void setLocation(Location location) {
		this.location = location;
	}
}
