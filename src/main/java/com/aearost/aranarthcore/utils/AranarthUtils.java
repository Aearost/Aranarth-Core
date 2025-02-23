package com.aearost.aranarthcore.utils;

import com.aearost.aranarthcore.objects.AranarthPlayer;
import com.aearost.aranarthcore.objects.Home;
import com.aearost.aranarthcore.objects.PlayerShop;
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

import java.io.IOException;
import java.util.*;

/**
 * Provides a large variety of utility methods for everything related to AranarthCore.
 */
public class AranarthUtils {

	private static final HashMap<UUID, AranarthPlayer> players = new HashMap<>();
	private static List<Home> homes = new ArrayList<>();
	private static final HashMap<Location, Integer> dragonHeads = new HashMap<>();
	private static final HashMap<UUID, List<PlayerShop>> playerShops = new HashMap<>();

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
	 * @param player The player to determine.
	 * @return Confirmation of whether they've played before.
	 */
	public static boolean hasPlayedBefore(Player player) {
		return players.containsKey(player.getUniqueId());
	}

	/**
	 * Sets the username of a player. This is used to update a player's username
	 * value in the case that they changed it.
	 * 
	 * @param player The player whose username will be changed.
	 */
	public static void setUsername(Player player) {
		AranarthPlayer aranarthPlayer = getPlayer(player.getUniqueId());
		aranarthPlayer.setUsername(player.getName());
		players.put(player.getUniqueId(), aranarthPlayer);
	}

	/**
	 * Gets the AranarthPlayer corresponding to an input UUID.
	 * 
	 * @param uuid The UUID of the player to be found.
	 * @return The AranarthPlayer tied to the UUID.
	 */
	public static AranarthPlayer getPlayer(UUID uuid) {
		return players.get(uuid);
	}
	/**
	 * Adds or overrides a player to the players HashMap.
	 *
	 * @param uuid The UUID of the player to be updated.
	 * @param aranarthPlayer The new AranarthPlayer to be used.
	 */
	public static void setPlayer(UUID uuid, AranarthPlayer aranarthPlayer) {
		players.put(uuid, aranarthPlayer);
	}

	/**
	 * Adds a player to the players HashMap.
	 * 
	 * @param uuid The UUID of the player to be added.
	 * @param aranarthPlayer The new AranarthPlayer to be added.
	 */
	public static void addPlayer(UUID uuid, AranarthPlayer aranarthPlayer) {
		players.put(uuid, aranarthPlayer);
	}

	/**
	 * Gets the stored username of a player.
	 * 
	 * @param player The player whose username is to be found.
	 * @return The username of the player.
	 */
	public static String getUsername(OfflinePlayer player) {
		return players.get(player.getUniqueId()).getUsername();
	}

	/**
	 * Gets the stored nickname of a player.
	 *
	 * @param player The player whose nickname is to be found.
	 * @return The nickname of the player, or username if no nickname was found.
	 */
	public static String getNickname(OfflinePlayer player) {
		String nickname = players.get(player.getUniqueId()).getNickname();
		return nickname.isEmpty() ? getUsername(player) : nickname;
	}

	/**
	 * Adds a new homepad location.
	 *
	 * @param location The Location that the homepad is located at.
	 */
	public static void addNewHome(Location location) {
		homes.add(new Home("NEW", location, Material.HEAVY_WEIGHTED_PRESSURE_PLATE));
	}

	/**
	 * Updates the name, location, and/or icon of an existing homepad.
	 *
	 * @param homeName The new name to be used for the home.
	 * @param direction The Location containing the direction of the homepad to be used.
	 * @param icon The Material that will be displayed for the homepad.
	 */
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

	/**
	 * Provides the list of homepads being used.
	 *
	 * @return The List of homepads.
	 */
	public static List<Home> getHomes() {
		return homes;
	}

	/**
	 * Overrides the list of homepads being used.
	 *
	 * @param newHomes The new list of homepads.
	 */
	public static void setHomes(List<Home> newHomes) {
		homes = newHomes;
	}

	/**
	 * Provides the HashMap of AranarthPlayers separated by UUID.
	 *
	 * @return The List of AranarthPlayers
	 */
	public static HashMap<UUID, AranarthPlayer> getAranarthPlayers() {
		return players;
	}

	/**
	 * Provides the homepad at the specified Location.
	 *
	 * @param location The location of the homepad.
	 * @return The homepad at the location.
	 */
	public static Home getHomePad(Location location) {
		for (Home home : homes) {
			if (locationsMatch(location, home.getLocation())) {
				return home;
			}
		}
		return null;
	}

	/**
	 * Removes the homepad at the specified Location.
	 *
	 * @param location The location of the homepad to be removed.
	 */
	public static void removeHomePad(Location location) {
		Home toRemove = null;
		for (Home home : homes) {
			if (locationsMatch(location, home.getLocation())) {
				toRemove = home;
			}
		}
		homes.remove(toRemove);
	}

	/**
	 * Verifies that the two locations share the same block coordinate.
	 *
	 * @param location1 The first location.
	 * @param location2 The second location.
	 * @return Confirmation of whether the two locations are the same.
	 */
	public static boolean locationsMatch(Location location1, Location location2) {
		if (Objects.nonNull(location1.getWorld()) && Objects.nonNull(location2.getWorld())) {
			return location1.getBlockX() == location2.getBlockX() && location1.getBlockY() == location2.getBlockY()
					&& location1.getBlockZ() == location2.getBlockZ()
					&& location1.getWorld().getName().equals(location2.getWorld().getName());
		} else {
			Bukkit.getLogger().info("One or more of the worlds does not exist!");
			return false;
		}
	}

	/**
	 * Handles switching the player's inventory dependent on the world they are teleporting to.
	 * If the player is teleporting to the arena world, the player will be given iron armor.
	 *
	 * @param player The player whose inventory is being altered.
	 * @param currentWorld The world the player is teleporting from.
	 * @param destinationWorld The world the player is teleporting to.
	 * @throws IOException The exception potentially thrown when converting the inventory to Base64.
	 */
	public static void switchInventory(Player player, String currentWorld, String destinationWorld) throws IOException {
		AranarthPlayer aranarthPlayer = AranarthUtils.getPlayer(player.getUniqueId());

		if (currentWorld.equals(destinationWorld)
				|| (currentWorld.startsWith("world") && destinationWorld.startsWith("world"))) {
			return;
		}

		if (currentWorld.startsWith("world")) {
			aranarthPlayer.setSurvivalInventory(ItemUtils.toBase64(player.getInventory()));
			if (destinationWorld.startsWith("arena")) {
				if (!aranarthPlayer.getArenaInventory().isEmpty()) {
					player.getInventory().setContents(ItemUtils.itemStackArrayFromBase64(aranarthPlayer.getArenaInventory()));
					return;
				}
				player.getInventory().clear();
				player.getInventory().setArmorContents(new ItemStack[] {
						new ItemStack(Material.IRON_BOOTS, 1),
						new ItemStack(Material.IRON_LEGGINGS, 1),
						new ItemStack(Material.IRON_CHESTPLATE, 1),
						new ItemStack(Material.IRON_HELMET, 1)});
				return;
			} else if (destinationWorld.startsWith("creative")) {
				if (!aranarthPlayer.getCreativeInventory().isEmpty()) {
					player.getInventory().setContents(ItemUtils.itemStackArrayFromBase64(aranarthPlayer.getCreativeInventory()));
					return;
				}
			}
			player.getInventory().clear();
		} else if (currentWorld.startsWith("arena")) {
			if (destinationWorld.startsWith("world")) {
				aranarthPlayer.setArenaInventory(ItemUtils.toBase64(player.getInventory()));
				if (!aranarthPlayer.getSurvivalInventory().isEmpty()) {
					player.getInventory().setContents(ItemUtils.itemStackArrayFromBase64(aranarthPlayer.getSurvivalInventory()));
					return;
				}
			} else if (destinationWorld.startsWith("creative")) {
				aranarthPlayer.setArenaInventory(ItemUtils.toBase64(player.getInventory()));
				if (!aranarthPlayer.getCreativeInventory().isEmpty()) {
					player.getInventory().setContents(ItemUtils.itemStackArrayFromBase64(aranarthPlayer.getCreativeInventory()));
					return;
				}
			}
			player.getInventory().clear();
		} else if (currentWorld.startsWith("creative")) {
			if (destinationWorld.startsWith("world")) {
				aranarthPlayer.setCreativeInventory(ItemUtils.toBase64(player.getInventory()));
				if (!aranarthPlayer.getSurvivalInventory().isEmpty()) {
					player.getInventory().setContents(ItemUtils.itemStackArrayFromBase64(aranarthPlayer.getSurvivalInventory()));
					return;
				}
			} else if (destinationWorld.startsWith("arena")) {
				aranarthPlayer.setCreativeInventory(ItemUtils.toBase64(player.getInventory()));
				if (!aranarthPlayer.getArenaInventory().isEmpty()) {
					player.getInventory().setContents(ItemUtils.itemStackArrayFromBase64(aranarthPlayer.getArenaInventory()));
					return;
				}
				player.getInventory().clear();
				player.getInventory().setArmorContents(new ItemStack[] {
						new ItemStack(Material.IRON_BOOTS, 1),
						new ItemStack(Material.IRON_LEGGINGS, 1),
						new ItemStack(Material.IRON_CHESTPLATE, 1),
						new ItemStack(Material.IRON_HELMET, 1)});
				return;
			}
			player.getInventory().clear();
		} else {
			Bukkit.getLogger().info("Something went wrong with the current world name \"" + currentWorld + "\"!");
			return;
		}
		AranarthUtils.setPlayer(player.getUniqueId(), aranarthPlayer);
	}

	/**
	 * Handles applying armor trim effects.
	 */
	public static void updateArmorTrimEffects() {
		for (AranarthPlayer aranarthPlayer : players.values()) {
			if (Objects.nonNull(aranarthPlayer.getUsername())) {
				Player player = Bukkit.getPlayer(aranarthPlayer.getUsername());
				if (Objects.nonNull(player)) {
					if (verifyPlayerHasArmorTrim(player, TrimPattern.RAISER)) {
						player.addPotionEffect(new PotionEffect(PotionEffectType.JUMP_BOOST, 320, 2));
					}
					if (verifyPlayerHasArmorTrim(player, TrimPattern.SILENCE)) {
						player.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, 320, 2));
					}
					if (verifyPlayerHasArmorTrim(player, TrimPattern.SHAPER)) {
						// There is no amplifier to this effect
						player.addPotionEffect(new PotionEffect(PotionEffectType.DOLPHINS_GRACE, 320, 0));
					}
//					if (verifyPlayerHasArmorTrim(player, TrimPattern.EYE)) {
//						// IDEA: See nearby players via Glowing effect - https://www.spigotmc.org/threads/make-everybody-glow-to-one-player.465348/
//					}
				}
			}
		}
	}

	/**
	 * Verifies if the player has the specified armor trim.
	 *
	 * @param player The player to be verified.
	 * @param trimPattern The trim to be verified.
	 * @return Confirmation of whether the player has the specified trim.
	 */
	public static boolean verifyPlayerHasArmorTrim(Player player, TrimPattern trimPattern) {
		ItemStack[] armor = player.getInventory().getArmorContents();
		for (ItemStack is : armor) {
			if (Objects.nonNull(is)) {
				// Elytras cannot have trims thus must be ignored
				if (is.getType() == Material.ELYTRA) {
					continue;
				}
				if (is.getItemMeta() instanceof ArmorMeta armorMeta) {
                    if (armorMeta.hasTrim()) {
						if (Objects.nonNull(armorMeta.getTrim())) {
							if (armorMeta.getTrim().getPattern() == trimPattern) {
								return true;
							}
						}
					}
				}
			}
		}
		return false;
	}

	/**
	 * Handles updates to the fuel source of a dragon head at a particular location.
	 *
	 * @param location The Location of the dragon head.
	 * @param isPoweredByRedstone Whether the head is powered by redstone.
	 * @return The amount of fuel that will be added to the dragon head.
	 */
	public static int updateDragonHead(Location location, boolean isPoweredByRedstone) {
		int amount = 8;
		if (isPoweredByRedstone) {
			amount = amount * 2;
		}
		// If no dragon heads have been running since the server was started
		if (dragonHeads.isEmpty()) {
			dragonHeads.put(location, amount);
		} else {
			for (Location locationInMap : dragonHeads.keySet()) {
				// If the coordinate is already added
				if (location.getX() == locationInMap.getX()
						&& location.getY() == locationInMap.getY()
						&& location.getZ() == locationInMap.getZ()) {
					Integer newAmount = dragonHeads.get(location) + amount;
					dragonHeads.put(location, newAmount);
				} else {
					dragonHeads.put(location, amount);
				}
			}
		}
		return amount;
	}

	/**
	 * Provides the quantity of fuel that remains in the input dragon head's Location.
	 *
	 * @param location The Location of the dragon head.
	 * @return The quantity of fuel that remains.
	 */
	public static int getDragonHeadFuelAmount(Location location) {
		if (Objects.isNull(dragonHeads.get(location))) {
			return -1;
		} else {
			return dragonHeads.get(location);
		}
		
	}

	/**
	 * Handles the decrementing of fuel in the dragon head at the input Location.
	 *
	 * @param location The Location of the dragon head to be decremented.
	 */
	public static void decrementDragonHeadFuelAmount(Location location) {
		Integer newAmount = dragonHeads.get(location) - 1;
		dragonHeads.put(location, newAmount);
	}

	public static HashMap<UUID, List<PlayerShop>> getShops() {
		return playerShops;
	}

	public static PlayerShop getShop(Location location) {
		if (getShops() != null) {
			for (UUID uuid : playerShops.keySet()) {
				for (PlayerShop shop : playerShops.get(uuid)) {
					if (shop.getLocation().equals(location)) {
						return shop;
					}
				}
			}

		}
		return null;
	}

	public static void addShop(UUID uuid, PlayerShop newShop) {
		List<PlayerShop> shops = playerShops.get(uuid);
		if (shops == null) {
			shops = new ArrayList<>();
		}
		shops.add(newShop);
		playerShops.put(uuid, shops);
	}

	public static void removeShop(UUID uuid, Location location) {
		List<PlayerShop> shops = playerShops.get(uuid);
		int shopSlotToDelete = -1;
		for (int i = 0; i < shops.size(); i++) {
			if (shops.get(i).getLocation().equals(location)) {
				shopSlotToDelete = i;
				break;
			}
		}
		// Only delete if a shop was found
		if (shopSlotToDelete != -1) {
			shops.remove(shopSlotToDelete);
		}
	}
}
