package com.aearost.aranarthcore.utils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
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

	public static void setPlayer(UUID uuid, AranarthPlayer aranarthPlayer) {
		players.put(uuid, aranarthPlayer);
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
	
	public static String getNickname(OfflinePlayer player) {
		return players.get(player.getUniqueId()).getNickname();
	}

	//
	public static void addNewHome(Location location) {
		homes.add(new Home("NEW", location, Material.HEAVY_WEIGHTED_PRESSURE_PLATE));
	}
	
	public static void updateHome(String homeName, Location direction, Material icon) {
		for (int i = 0; i < homes.size(); i++) {
			if (homes.get(i).getLocation().getBlockX() == direction.getBlockX()
					&& homes.get(i).getLocation().getBlockY() == direction.getBlockY()
					&& homes.get(i).getLocation().getBlockZ() == direction.getBlockZ()) {
				Home updatedHome = new Home(homeName, direction, icon);
				homes.set(i, updatedHome);
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
	
	public static void switchInventory(Player player, String currentWorld, String destinationWorld) {
		System.out.println("A");
		AranarthPlayer aranarthPlayer = AranarthUtils.getPlayer(player.getUniqueId());
		if (currentWorld.equals("world") || currentWorld.equals("arena")) {
			System.out.println("B");
			// Do not change inventory unless heading to Creative
			if (destinationWorld.equals("creative")) {
				System.out.println("C");
				aranarthPlayer.setSurvivalInventory(player.getInventory());
				player.getInventory().setContents(aranarthPlayer.getCreativeInventory().getContents());
				System.out.println("D");
			}
		} else if (currentWorld.equals("creative")) {
			System.out.println("E");
			if (destinationWorld.equals("world") || destinationWorld.equals("arena")) {
				System.out.println("F");
				// Do not change inventory unless heading to Survival or Arena
				aranarthPlayer.setCreativeInventory(player.getInventory());
				player.getInventory().setContents(aranarthPlayer.getSurvivalInventory().getContents());
				System.out.println("G");
			}
		} else {
			System.out.println("UH OH!!!");
			System.out.println("currentWorld: " + currentWorld);
			System.out.println("destinationWorld: " + destinationWorld);
			Bukkit.getLogger().info("Something went wrong with the current world name!");
		}
		AranarthUtils.setPlayer(player.getUniqueId(), aranarthPlayer);
		player.updateInventory();
		System.out.println("H");
	}

}
