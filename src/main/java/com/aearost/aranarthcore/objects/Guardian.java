package com.aearost.aranarthcore.objects;

import org.bukkit.Location;
import org.bukkit.entity.EntityType;

import java.util.UUID;

/**
 * Handles all necessary functionality relating to Guardian mobs on Aranarth.
 */
public class Guardian {
	private UUID uuid;
	private EntityType type;
	private Location location;

	public Guardian(UUID uuid, EntityType type, Location location) {
		this.uuid = uuid;
		this.type = type;
		this.location = location;
	}

	/**
	 * Provides the guardian's UUID.
	 * @return the guardian's UUID.
	 */
	public UUID getUuid() {
		return uuid;
	}

	/**
	 * Updates the guardian's UUID.
	 * @param uuid The guardian's UUID.
     */
	public void setUuid(UUID uuid) {
		this.uuid = uuid;
	}

	/**
	 * Provides the EntityType of the Guardian.
	 * @return The EntityType of the Guardian.
	 */
	public EntityType getType() {
		return type;
	}

	/**
	 * Updates the EntityType of the Guardian.
	 * @param type The EntityType of the Guardian.
	 */
	public void setType(EntityType type) {
		this.type = type;
	}

	/**
	 * Provides the last known Location of the Guardian.
	 * @return The last known Location of the Guardian.
	 */
	public Location getLocation() {
		return location;
	}

	/**
	 * Updates the last known Location of the Guardian.
	 * @param location The last known Location of the Guardian.
	 */
	public void setLocation(Location location) {
		this.location = location;
	}
}
