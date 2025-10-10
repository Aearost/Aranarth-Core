package com.aearost.aranarthcore.objects;

import org.bukkit.Bukkit;
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

	@Override
	public String toString() {
		String divider = "----------";
		String owner = Bukkit.getOfflinePlayer(getOwner()).getName() + " (" + getOwner() + ")";
		StringBuilder trustedSB = new StringBuilder();
		for (int i = 0; i < getTrusted().size(); i++) {
			if (i == getTrusted().size() - 1) {
				trustedSB.append(Bukkit.getOfflinePlayer(getTrusted().get(i)).getName()).append(" (").append(getTrusted().get(i)).append(")");
			} else {
				trustedSB.append(Bukkit.getOfflinePlayer(getTrusted().get(i)).getName()).append(" (").append(getTrusted().get(i)).append(") | ");
			}
		}
		String trusted = trustedSB.toString();
		String x1 = "x1: " + locations[0].getBlockX() + " | ";
		String y1 = "y1: " + locations[0].getBlockY() + " | ";
		String z1 = "z1: " + locations[0].getBlockZ();
		String x2 = "";
		String y2 = "";
		String z2 = "";
		if (locations[1] != null) {
			x2 = "x2: " + locations[1].getBlockX() + " | ";
			y2 = "y2: " + locations[1].getBlockY() + " | ";
			z2 = "z2: " + locations[1].getBlockZ();
		}
		String loc1 = x1 + y1 + z1;
		String loc2 = x2 + y2 + z2;
		if (loc2.isEmpty()) {
			return owner + "\n" + trusted + "\n" + loc1;
		} else {
			return owner + "\n" + trusted + "\n" + loc1 + "\n" + loc2;
		}
	}

}
