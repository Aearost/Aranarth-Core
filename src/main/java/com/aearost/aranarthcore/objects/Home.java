package com.aearost.aranarthcore.objects;

import com.aearost.aranarthcore.AranarthCore;
import com.aearost.aranarthcore.utils.ChatUtils;
import org.bukkit.Location;
import org.bukkit.Material;

/**
 * Handles all necessary functionality relating to homes in Aranarth.
 *
 * <p>World names use a server-aware convention so homes survive cross-server transfers:
 * <ul>
 *   <li>SMP homes: {@code "smp:world"}, {@code "smp:world_nether"}, {@code "smp:world_the_end"}</li>
 *   <li>Survival/other homes: plain world name (e.g. {@code "world"}, {@code "spawn"})</li>
 * </ul>
 */
public class Home {

	private final String name;
	private Location location;
	private final Material icon;
	/** Canonical world name as stored in the save file (never null after construction). */
	private final String worldName;

	/**
	 * Creates a home at a loaded location.
	 * The worldName is computed automatically from the location's world.
	 */
	public Home(String homeName, Location location, Material icon) {
		homeName = ChatUtils.removeSpecialCharacters(homeName);
		this.name = homeName;
		this.location = location;
		this.icon = icon;
		this.worldName = computeWorldName(location);
	}

	/**
	 * Creates a home with an explicit saved worldName (used when loading from file,
	 * where the Bukkit world may be null because it lives on another server).
	 */
	public Home(String homeName, Location location, Material icon, String worldName) {
		homeName = ChatUtils.removeSpecialCharacters(homeName);
		this.name = homeName;
		this.location = location;
		this.icon = icon;
		this.worldName = worldName != null ? worldName : computeWorldName(location);
	}

	/**
	 * Derives the canonical saved world name from a Location.
	 * SMP worlds get the {@code "smp:"} prefix; all others are stored as-is.
	 */
	private static String computeWorldName(Location location) {
		if (location == null || location.getWorld() == null) return "world";
		String wn = location.getWorld().getName();
		if (AranarthCore.isSmpServer()) {
			if (wn.equals("world"))           return "smp:world";
			if (wn.equals("world_nether"))    return "smp:world_nether";
			if (wn.equals("world_the_end"))   return "smp:world_the_end";
		} else {
			// Survival server: old local SMP world names get the prefix
			if (wn.equals("smp"))             return "smp:world";
			if (wn.equals("smp_nether"))      return "smp:world_nether";
			if (wn.equals("smp_the_end"))     return "smp:world_the_end";
		}
		return wn;
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

	/**
	 * Returns the canonical world name as stored in the save file.
	 * SMP homes use the {@code "smp:"} prefix (e.g. {@code "smp:world"}).
	 * Never null.
	 */
	public String getWorldName() {
		return worldName;
	}

	/** Returns true if this home lives on the SMP server. */
	public boolean isSmpHome() {
		return worldName.startsWith("smp:");
	}
}
