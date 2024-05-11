package com.aearost.aranarthcore.utils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ArmorMeta;
import org.bukkit.inventory.meta.trim.TrimPattern;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import com.aearost.aranarthcore.objects.AranarthPlayer;
import com.aearost.aranarthcore.objects.Home;

public class AranarthUtils {

	private static HashMap<UUID, AranarthPlayer> players = new HashMap<>();
	private static List<Home> homes = new ArrayList<>();
	private static HashMap<UUID, List<ItemStack>> blacklistedItems = new HashMap<>();
	private static HashMap<Location, Integer> dragonHeads = new HashMap<>();

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
	
	public static List<ItemStack> getBlacklistedItems(UUID uuid) {
		return blacklistedItems.get(uuid);
	}
	
	public static void updateBlacklistedItems(UUID uuid, List<ItemStack> newBlacklistedItems) {
		blacklistedItems.put(uuid, newBlacklistedItems);
	}
	
	public static boolean hasBlacklistedItems(UUID uuid) {
		if (Objects.nonNull(blacklistedItems.get(uuid))) {
			return blacklistedItems.get(uuid).size() > 0;
		}
		return false;
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
	
	public static void switchInventory(Player player, String currentWorld, String destinationWorld) throws IOException {
		AranarthPlayer aranarthPlayer = AranarthUtils.getPlayer(player.getUniqueId());
		if (currentWorld.equals("world") || currentWorld.equals("arena")) {
			// Do not change inventory unless heading to Creative
			if (destinationWorld.equals("creative")) {
				aranarthPlayer.setSurvivalInventory(ItemUtils.toBase64(player.getInventory()));
				if (!aranarthPlayer.getCreativeInventory().equals("")) {
					player.getInventory().setContents(ItemUtils.itemStackArrayFromBase64(aranarthPlayer.getCreativeInventory()));
				} else {
					player.getInventory().clear();
				}
			}
		} else if (currentWorld.equals("creative")) {
			if (destinationWorld.equals("world") || destinationWorld.equals("arena")) {
				// Do not change inventory unless heading to Survival or Arena
				aranarthPlayer.setCreativeInventory(ItemUtils.toBase64(player.getInventory()));
				if (!aranarthPlayer.getSurvivalInventory().equals("")) {
					player.getInventory().setContents(ItemUtils.itemStackArrayFromBase64(aranarthPlayer.getSurvivalInventory()));
				} else {
					player.getInventory().clear();
				}
			}
		} else {
			Bukkit.getLogger().info("Something went wrong with the current world name!");
			return;
		}
		AranarthUtils.setPlayer(player.getUniqueId(), aranarthPlayer);
		player.updateInventory();
	}

	public static void updateArmourTrimEffects() {
		for (AranarthPlayer aranarthPlayer : players.values()) {
			Player player = Bukkit.getPlayer(aranarthPlayer.getUsername());
			if (Objects.nonNull(player)) {
				ItemStack[] armor = player.getInventory().getArmorContents();
				for (ItemStack is : armor) {
					if (Objects.nonNull(is)) {
						// Elytras cannot have trims thus must be ignored
						if (is.getType() == Material.ELYTRA) {
							continue;
						}
						ArmorMeta armorMeta = (ArmorMeta) is.getItemMeta();
						if (armorMeta.hasTrim()) {
							if (armorMeta.getTrim().getPattern() == TrimPattern.RAISER) {
								player.addPotionEffect(new PotionEffect(PotionEffectType.JUMP_BOOST, 320, 2));
							} else if (armorMeta.getTrim().getPattern() == TrimPattern.SILENCE) {
								player.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, 320, 2));
							}  else if (armorMeta.getTrim().getPattern() == TrimPattern.SHAPER) {
								// There is no amplifier to this effect
								player.addPotionEffect(new PotionEffect(PotionEffectType.DOLPHINS_GRACE, 320, 0));
							}  else if (armorMeta.getTrim().getPattern() == TrimPattern.EYE) {
								// IDEA: See nearby players via Glowing effect - https://www.spigotmc.org/threads/make-everybody-glow-to-one-player.465348/
							} 
						}
					}
				}
			}
		}
	}
	
	public static void toggleBlacklistIgnoreOrDelete(UUID uuid, boolean isDeletingBlacklistedItems) {
		players.get(uuid).setIsDeletingBlacklistedItems(isDeletingBlacklistedItems);
	}
	
	public static void updateDragonHead(Location location) {
		// If no dragon heads have been running since the server was started
		if (dragonHeads.size() == 0) {
			dragonHeads.put(location, Integer.valueOf(4));
		} else {
			for (Location locationInMap : dragonHeads.keySet()) {
				// If the coordinate is already added
				if (location.getX() == locationInMap.getX()
						&& location.getY() == locationInMap.getY()
						&& location.getZ() == locationInMap.getZ()) {
					Integer newAmount = Integer.valueOf(dragonHeads.get(location).intValue() + 4);
					dragonHeads.put(location, newAmount);
				} else {
					dragonHeads.put(location, Integer.valueOf(4));
				}
			}
		}
	}
	
	public static int getDragonHeadFuelAmount(Location location) {
		return dragonHeads.get(location).intValue();
	}
	
	public static void decrementDragonHeadFuelAmount(Location location) {
		Integer newAmount = Integer.valueOf(dragonHeads.get(location).intValue() - 1);
		dragonHeads.put(location, newAmount);
	}
	
}
