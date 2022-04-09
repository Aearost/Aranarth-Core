package com.aearost.aranarthcore.utils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;

import org.bukkit.Location;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;

import com.aearost.aranarthcore.objects.AranarthPlayer;
import com.aearost.aranarthcore.objects.Home;

public class AranarthUtils {

	private static HashMap<UUID, AranarthPlayer> players = new HashMap<>();
	private static List<Home> homes = new ArrayList<>();

	public AranarthUtils(boolean isServerStarting) {
		if (isServerStarting) {
			PersistenceUtils.loadHomes();
		} else {
			PersistenceUtils.saveHomes();
		}
	}

	/**
	 * Determines if the player has played on the server before.
	 * 
	 * @param player
	 * @return
	 */
	public static boolean hasPlayedBefore(Player player) {
		return players.containsKey(player.getUniqueId());
	}

	/**
	 * Sets the username of a player. This is used to update a player's username
	 * value in the case that they changed it.
	 * 
	 * @param player
	 */
	public static void setUsername(Player player) {
		AranarthPlayer aranarthPlayer = getPlayer(player.getUniqueId());
		aranarthPlayer.setUsername(player.getName());
		players.put(player.getUniqueId(), aranarthPlayer);
	}

	/**
	 * Gets the AranarthPlayer corresponding to an input UUID.
	 * 
	 * @param uuid
	 * @return
	 */
	public static AranarthPlayer getPlayer(UUID uuid) {
		return players.get(uuid);
	}

	public static void setPlayer(Player player, AranarthPlayer aranarthPlayer) {
		players.put(player.getUniqueId(), aranarthPlayer);
	}

	/**
	 * Adds a player to the players HashMap.
	 * 
	 * @param uuid
	 */
	public static void addPlayer(UUID uuid, AranarthPlayer aranarthPlayer) {
		players.put(uuid, aranarthPlayer);
	}

	/**
	 * Gets the stored username of a player.
	 * 
	 * @param player
	 * @return
	 */
	public static String getUsername(OfflinePlayer player) {
		return players.get(player.getUniqueId()).getUsername();
	}

	public static void addHome(Location location) {
		homes.add(new Home("NEW", location));
	}

	public static void setHomeName(String homeName, Home home) {
		for (int i = 0; i < homes.size(); i++) {
			if (homes.get(i).equals(home)) {
				homes.set(i, new Home(homeName, home.getLocation()));
			}
		}
	}

	public static List<Home> getHomes() {
		return homes;
	}
	
	public static HashMap<UUID, AranarthPlayer> getAranarthPlayers() {
		return players;
	}

	public static Home getHomePad(Location location) {
		for (Home home : homes) {
			if (locationsMatch(location, home.getLocation())) {
				return home;
			}
		}
		return null;
	}
	
	public static void removeHomePad(Location location) {
		Home toRemove = null;
		for (Home home : homes) {
			if (locationsMatch(location, home.getLocation())) {
				toRemove = home;
			}
		}
		homes.remove(toRemove);
	}

	public static boolean locationsMatch(Location location1, Location location2) {
		if (location1.getBlockX() == location2.getBlockX() && location1.getBlockY() == location2.getBlockY()
				&& location1.getBlockZ() == location2.getBlockZ()
				&& location1.getWorld().getName().equals(location2.getWorld().getName())) {
			return true;
		} else {
			return false;
		}
	}

}
