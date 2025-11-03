package com.aearost.aranarthcore.utils;

import com.aearost.aranarthcore.enums.Month;
import com.aearost.aranarthcore.enums.Pronouns;
import com.aearost.aranarthcore.enums.Weather;
import com.aearost.aranarthcore.items.arrow.*;
import com.aearost.aranarthcore.objects.AranarthPlayer;
import com.aearost.aranarthcore.objects.Home;
import com.aearost.aranarthcore.objects.LockedContainer;
import com.aearost.aranarthcore.objects.PlayerShop;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.Chest;
import org.bukkit.block.DoubleChest;
import org.bukkit.block.data.Levelled;
import org.bukkit.entity.Player;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BannerMeta;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.*;

import static com.aearost.aranarthcore.items.CustomItemKeys.ARMOR_TYPE;
import static com.aearost.aranarthcore.items.CustomItemKeys.ARROW;


/**
 * Provides a large variety of utility methods for everything related to AranarthCore.
 */
public class AranarthUtils {

	private static final HashMap<UUID, AranarthPlayer> players = new HashMap<>();
	private static List<Home> homes = new ArrayList<>();
	private static final HashMap<Location, Integer> dragonHeads = new HashMap<>();
	private static final HashMap<UUID, List<PlayerShop>> playerShops = new HashMap<>();
	private static final HashMap<UUID, BannerMeta> playerBanners = new HashMap<>();
	private static List<LockedContainer> lockedContainers;
	private static int day;
	private static int weekday;
	private static Month month;
	private static int year;
	private static int stormDuration;
	private static int stormDelay;
	private static boolean hasStormedInMonth;
	private static int currentTime;
	private static int windPlayTimer;
	private static boolean isPlayingWindSound;
	private static int cherryParticleDelay;
	private static Weather weather;
	private static final List<UUID> mutedPlayers = new ArrayList<>();

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
				|| (currentWorld.startsWith("world") && destinationWorld.startsWith("world"))
				|| (currentWorld.startsWith("smp") && destinationWorld.startsWith("smp"))
				|| (currentWorld.startsWith("world") && destinationWorld.startsWith("smp"))
				|| (currentWorld.startsWith("smp") && destinationWorld.startsWith("world"))) {
			return;
		}

		if (currentWorld.startsWith("world") || currentWorld.startsWith("smp")) {
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
			if (destinationWorld.startsWith("world") || destinationWorld.startsWith("smp")) {
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
			if (destinationWorld.startsWith("world") || destinationWorld.startsWith("smp")) {
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
	 * Cycles through online players and verifies if they are wearing a full set of Aranarthium armor.
	 */
	public static void applyArmourEffects() {
		for (AranarthPlayer aranarthPlayer : players.values()) {
			if (Objects.nonNull(aranarthPlayer.getUsername())) {
				Player player = Bukkit.getPlayer(aranarthPlayer.getUsername());
				if (Objects.nonNull(player)) {
					ItemStack[] armor = player.getInventory().getArmorContents();
					if (armor[0] != null && armor[1] != null && armor[2] != null && armor[3] != null) {
						verifyAndApplyAranarthiumArmourEffects(player);
					}
				}
			}
		}
	}

	/**
	 * Ensures that the given player is wearing a full set of Aranarthium Armour and applies its effects.
	 * @param player The player being verified.
	 */
	private static void verifyAndApplyAranarthiumArmourEffects(Player player) {
		if (isWearingArmorType(player, "aquatic")) {
			player.addPotionEffect(new PotionEffect(PotionEffectType.DOLPHINS_GRACE, 320, 0));
			player.addPotionEffect(new PotionEffect(PotionEffectType.CONDUIT_POWER, 320, 0));
			player.addPotionEffect(new PotionEffect(PotionEffectType.HEALTH_BOOST, 320, 4));
		} else if (isWearingArmorType(player, "ardent")) {
			player.addPotionEffect(new PotionEffect(PotionEffectType.STRENGTH, 320, 1));
			player.addPotionEffect(new PotionEffect(PotionEffectType.RESISTANCE, 320, 1));
			player.addPotionEffect(new PotionEffect(PotionEffectType.HEALTH_BOOST, 320, 9));
		} else if (isWearingArmorType(player, "dwarven")) {
			player.addPotionEffect(new PotionEffect(PotionEffectType.NIGHT_VISION, 320, 0));
			player.addPotionEffect(new PotionEffect(PotionEffectType.HEALTH_BOOST, 320, 4));
		} else if (isWearingArmorType(player, "elven")) {
			player.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, 320, 2));
			player.addPotionEffect(new PotionEffect(PotionEffectType.JUMP_BOOST, 320, 1));
			player.addPotionEffect(new PotionEffect(PotionEffectType.HEALTH_BOOST, 320, 4));
		} else if (isWearingArmorType(player, "scorched")) {
			player.addPotionEffect(new PotionEffect(PotionEffectType.FIRE_RESISTANCE, 320, 0));
			player.addPotionEffect(new PotionEffect(PotionEffectType.RESISTANCE, 320, 0));
			player.addPotionEffect(new PotionEffect(PotionEffectType.HEALTH_BOOST, 320, 4));
		} else if (isWearingArmorType(player, "soulbound")) {
			player.addPotionEffect(new PotionEffect(PotionEffectType.HEALTH_BOOST, 320, 4));
		}
	}

	/**
	 * Verifies if the input armor type is fully equipped by the player.
	 * @param player The player being verified.
	 * @param type The armor type to be verified.
	 * @return Confirmation whether the specified type is fully equipped.
	 */
	public static boolean isWearingArmorType(Player player, String type) {
		ItemStack[] armor = player.getInventory().getArmorContents();
		int counter = 0;
		for (ItemStack is : player.getInventory().getArmorContents()) {
			if (is == null) {
				return false;
			}

			if (is.hasItemMeta()) {
				if (is.getItemMeta().getPersistentDataContainer().has(ARMOR_TYPE, PersistentDataType.STRING)) {
					if (is.getItemMeta().getPersistentDataContainer().get(ARMOR_TYPE, PersistentDataType.STRING).equals(type)) {
						counter++;
					}
				}
			}
		}
		// Ensures that all 4 pieces of armour are Dwarven
        return counter == 4;
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

	/**
	 * Provides the current list of player shops.
	 * @return The list of player shops.
	 */
	public static HashMap<UUID, List<PlayerShop>> getShops() {
		return playerShops;
	}

	/**
	 * Provides the shop at the input sign location.
	 * @param location The location of the sign.
	 * @return The player shop if it exists.
	 */
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

	/**
	 * Adding the input shop by the associated UUID.
	 * @param uuid The UUID. Null if it is a server shop.
	 * @param newShop The new player shop.
	 */
	public static void addShop(UUID uuid, PlayerShop newShop) {
		List<PlayerShop> shops = playerShops.get(uuid);
		if (shops == null) {
			shops = new ArrayList<>();
		}
		shops.add(newShop);
		playerShops.put(uuid, shops);
	}

	/**
	 * Removes the player shop at the associated location for the input UUID.
	 * @param uuid The UUID.
	 * @param location The location of the sign of the shop.
	 */
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

	/**
	 * Provides the current server day.
	 * @return The server day.
	 */
	public static int getDay() {
		return day;
	}

	/**
	 * Updates the current server day.
	 * @param newDay The new server day.
	 */
	public static void setDay(int newDay) {
		day = newDay;
	}

	/**
	 * Provides the current server weekday.
	 * @return The server weekday.
	 */
	public static int getWeekday() {
		return weekday;
	}

	/**
	 * Updates the current server weekday.
	 * @param newWeekday The new server weekday.
	 */
	public static void setWeekday(int newWeekday) {
		weekday = newWeekday;
	}

	/**
	 * Provides the current server month.
	 * @return The server month.
	 */
	public static Month getMonth() {
		return month;
	}

	/**
	 * Updates the current server month.
	 * @param newMonth The new server month.
	 */
	public static void setMonth(Month newMonth) {
		if (newMonth != month) {
			// Ensures that each month has a new value
			hasStormedInMonth = false;
			stormDelay = 0;
			stormDuration = 0;
		}
		month = newMonth;

	}

	/**
	 * Provides the current server year.
	 * @return The server year.
	 */
	public static int getYear() {
		return year;
	}

	/**
	 * Updates the current server year.
	 * @param newYear The new server year.
	 */
	public static void setYear(int newYear) {
		year = newYear;
	}

	/**
	 * Provides the intended duration of the current storm.
	 * @return The duration of the current storm.
	 */
	public static int getStormDuration() {
		return stormDuration;
	}

	/**
	 * Updates the value of the current storm duration.
	 * @param newStormDuration The new duration of the storm.
	 */
	public static void setStormDuration(int newStormDuration) {
		stormDuration = newStormDuration;
	}

	/**
	 * Provides the current delay between storms.
	 * @return The current delay between storms.
	 */
	public static int getStormDelay() {
		return stormDelay;
	}

	/**
	 * Updates the value of the delay between storms.
	 * @param newStormDelay The new delay between storms.
	 */
	public static void setStormDelay(int newStormDelay) {
		stormDelay = newStormDelay;
	}

	/**
	 * Provides the confirmation whether it has stormed since server startup or new month.
	 * @return Confirmation if it has stormed since the server startup or new month.
	 */
	public static boolean getHasStormedInMonth() {
		return hasStormedInMonth;
	}

	/**
	 * Updates the value of whether it has stormed in the month since the server startup or new month.
	 * @param newHasStormedInMonth The new confirmation whether it has stormed since the server startup or new month.
	 */
	public static void setHasStormedInMonth(boolean newHasStormedInMonth) {
		hasStormedInMonth = newHasStormedInMonth;
	}

	/**
	 * Provides the current timer since the last wind began.
	 * @return The current timer since the last wind began.
	 */
	public static int getWindPlayTimer() {
		return windPlayTimer;
	}

	/**
	 * Updates the current timer since the last wind began.
	 * @param newWindPlayTimer The new current timer since the last wind began.
	 */
	public static void setWindPlayTimer(int newWindPlayTimer) {
		windPlayTimer = newWindPlayTimer;
	}

	/**
	 * Provides confirmation whether the wind sound is currently playing.
	 * @return Confirmation whether the wind sound is currently playing.
	 */
	public static boolean getIsPlayingWindSound() {
		return isPlayingWindSound;
	}

	/**
	 * Updates the value of whether the wind sound is currently playing.
	 * @param newIsPlayingWindSound The new value of whether the wind sound is currently playing.
	 */
	public static void setIsPlayingWindSound(boolean newIsPlayingWindSound) {
		isPlayingWindSound = newIsPlayingWindSound;
	}

	/**
	 * Provides the current delay since the last display of cherry leaf particles.
	 * @return The delay since the last display of cherry leaf particles.
	 */
	public static int getCherryParticleDelay() {
		return cherryParticleDelay;
	}

	/**
	 * Updates the value of the current delay since the last display of cherry leaf particles.
	 * @param newCherryParticleDelay The new value of the delay since the last display of cherry leaf particles.
	 */
	public static void setCherryParticleDelay(int newCherryParticleDelay) {
		cherryParticleDelay = newCherryParticleDelay;
	}

	/**
	 * Provides the banner the player is editing.
	 * @param uuid The player's UUID.
	 * @return The banner's meta.
	 */
	public static BannerMeta getPlayerBanner(UUID uuid) {
		return playerBanners.get(uuid);
	}

	/**
	 * Updates the value of the player's banner.
	 * @param uuid The UUID of the player.
	 * @param bannerMeta The banner's meta.
	 */
	public static void setPlayerBanner(UUID uuid, BannerMeta bannerMeta) {
		// If the player is done with editing the banner
		if (bannerMeta == null) {
			playerBanners.remove(uuid);
		}

		playerBanners.put(uuid, bannerMeta);
	}

	/**
	 * Confirms if the input item is indeed a crop.
	 * @param type The type of item it is.
	 * @return Confirmation of whether the block is a crop or not.
	 */
	public static boolean isBlockCrop(Material type) {
		return type == Material.WHEAT || type == Material.CARROTS
				|| type == Material.POTATOES || type == Material.BEETROOTS
				|| type == Material.NETHER_WART;
	}

	public static void applyWaterfallEffect() {
		for (Player player : Bukkit.getOnlinePlayers()) {
			Location playerLoc = player.getLocation();
			int radius = 40;

			int px = playerLoc.getBlockX();
			int py = playerLoc.getBlockY();
			int pz = playerLoc.getBlockZ();

			World world = player.getWorld();

			for (int x = -radius; x <= radius; x++) {
				for (int z = -radius; z <= radius; z++) {
					for (int y = py + 10; y >= py - 10; y--) {
						Block start = world.getBlockAt(px + x, y, pz + z);

						// Only check flowing water blocks (NOT source blocks)
						if (start.getType() != Material.WATER) continue;

						if (start.getBlockData() instanceof Levelled startLevel) {
							if (startLevel.getLevel() == 0) {
								continue; // Skip source blocks
							}
						} else {
							continue;
						}

						// Check how far this water can fall
						int fallHeight = 0;
						Block landing = null;

						for (int dy = 1; dy <= 10; dy++) {
							Block below = start.getRelative(0, -dy, 0);

							if (below.getType() == Material.WATER) {
								if (below.getBlockData() instanceof Levelled landingLevel) {
									if (landingLevel.getLevel() == 0) {
										// Found a source block as landing
										fallHeight = dy;
										landing = below;
										break;
									}
								}
							} else if (!below.getType().isAir()) {
								// Hit something solid or non-water — stop
								break;
							}
						}

						// Trigger particle effect if flow into source block from at least 2 blocks up
						if (fallHeight >= 2 && landing != null) {
							Location particleLoc = landing.getLocation().add(0.5, 1.2, 0.5);

							if (player.getLocation().distanceSquared(particleLoc) <= radius * radius) {
								Particle.DustOptions whiteDust = new Particle.DustOptions(Color.fromRGB(255, 255, 255), 1.0f);
								player.spawnParticle(Particle.DUST, particleLoc, 5, 0.6, 0.3, 0.6, whiteDust);
							}
						}
					}
				}
			}
		}
	}

	/**
	 * Returns a HashMap containing the quantity of a given potion that a player has.
	 * @param player The player running the command.
	 * @return The HashMap of the potions the player has, as well as the quantity of the potion.
	 */
	public static HashMap<ItemStack, Integer> getPotionsAndAmounts(Player player) {
		List<ItemStack> potions = getPlayer(player.getUniqueId()).getPotions();
		HashMap<ItemStack, Integer> potionsAndAmounts = new HashMap<>();

		// Counts how many potions of the same type there are
		for (ItemStack potionToCount : potions) {
			// Add the potion if it doesn't exist
			if (potionsAndAmounts.get(potionToCount) == null) {
				potionsAndAmounts.put(potionToCount, 1);
				continue;
			}

			int amount = potionsAndAmounts.get(potionToCount);
			amount++;
			potionsAndAmounts.put(potionToCount, amount);
		}

		return potionsAndAmounts;
	}

	/**
	 * Provides the current state of the weather.
	 * @return The current state of the weather.
	 */
	public static Weather getWeather() {
		return weather;
	}

	/**
	 * Updates the value of the current state of the weather.
	 * @param newWeather The new value of the current state of the weather.
	 */
	public static void setWeather(Weather newWeather) {
		weather = newWeather;
	}

	/**
	 * Provides the list of locked containers.
	 * @return The list of locked containers.
	 */
	public static List<LockedContainer> getLockedContainers() {
		if (lockedContainers == null || lockedContainers.isEmpty()) {
			return null;
		}
		return lockedContainers;
	}

	/**
	 * Updates the list of locked containers.
	 * @param newLockedContainers The new list of locked containers.
	 */
	public static void setLockedContainers(List<LockedContainer> newLockedContainers) {
		lockedContainers = newLockedContainers;
	}

	/**
	 * Adds a new locked container to the list.
	 * @param lockedContainer The locked container to be added to the list.
	 */
	public static void addLockedContainer(LockedContainer lockedContainer) {
		if (getLockedContainers() == null || getLockedContainers().isEmpty()) {
			lockedContainers = new ArrayList<>();
		}
		lockedContainers.add(lockedContainer);
	}

	/**
	 * Removes a container if it was a locked container in the list.
	 * @param locations The locations of the container being removed.
	 * @return 0 if the whole container was removed, 1 if one of the container locations was removed, -1 if unsuccessful
	 */
	public static int removeLockedContainer(Location[] locations) {
		if (lockedContainers == null || lockedContainers.isEmpty()) {
			return -1;
		}

		boolean isLockedContainer = false;
		boolean isDoubleContainer = false;
		int i = 0;

		while (i < lockedContainers.size()) {
			Location loc1 = lockedContainers.get(i).getLocations()[0];
			Location loc2 = lockedContainers.get(i).getLocations()[1];
			isDoubleContainer = loc1 != null && loc2 != null;

			if (isDoubleContainer) {
				// If the whole container is being removed
				if (locations[1] != null) {
					if (isSameLocation(loc1, locations[0]) && isSameLocation(loc2, locations[1])) {
						isLockedContainer = true;
						lockedContainers.remove(i);
						return 0;
					}
				}
				// Only one of the two locations is being removed
				else {
					// Breaking left chest
					if (isSameLocation(loc1, locations[0])) {
						locations[0] = loc2;
						isLockedContainer = true;
						break;
					}
					// Breaking right chest
					else if (isSameLocation(loc2, locations[0])) {
						locations[0] = loc1;
						isLockedContainer = true;
						break;
					}
				}
			} else {
				if (isSameLocation(loc1, locations[0])) {
					isLockedContainer = true;
					break;
				}
			}
			i++;
		}

		if (isLockedContainer) {
			if (isDoubleContainer) {
				lockedContainers.get(i).setLocations(locations);
				return 1;
			} else {
				lockedContainers.remove(i);
				return 0;
			}
		} else {
			return -1;
		}
	}

	/**
	 * Provides the Location[] of the container, locked or unlocked.
	 * If it is a double chest, the left chest is the first index, right is the second.
	 * @param container The container that the location is being fetched for.
	 * @return The Location[] of the container.
	 */
	public static Location[] getLocationsOfContainer(Block container) {
		Location loc1 = container.getLocation();
		Location loc2 = null;
		if (container.getState() instanceof Chest chest) {
			InventoryHolder holder = chest.getInventory().getHolder();
			// Chests can be two blocks wide and the non-clicked block may be the locked container
			if (holder instanceof DoubleChest doubleChest) {
				Chest leftChest = (Chest) doubleChest.getLeftSide();
				Chest rightChest = (Chest) doubleChest.getRightSide();
				loc1 = leftChest.getLocation();
				loc2 = rightChest.getLocation();
			}
		}
		return new Location[] { loc1, loc2 };
	}

	/**
	 * Adds a player to the list of trusted players to access the container.
	 * @param uuidToAdd The UUID of the player being added to the container.
	 * @param location The location of the container.
	 */
	public static void addPlayerToContainer(UUID uuidToAdd, Location location) {
		List<UUID> trusted = null;
		for (LockedContainer container : getLockedContainers()) {
			Location loc1 = container.getLocations()[0];
			Location loc2 = container.getLocations()[0];
			if (loc2 == null) {
				if (isSameLocation(loc1, location)) {
					trusted = container.getTrusted();
					// If the UUID is already there, do nothing
					if (!trusted.contains(uuidToAdd)) {
						trusted.add(uuidToAdd);
					}
				}
			} else {
				if (isSameLocation(loc1, location) || isSameLocation(loc2, location)) {
					trusted = container.getTrusted();
					// If the UUID is already there, do nothing
					if (!trusted.contains(uuidToAdd)) {
						trusted.add(uuidToAdd);
					}
				}
			}
		}
	}

	/**
	 * Removes a player from the list of trusted players to access the container.
	 * @param uuidToRemove The UUID of the player being removed from the container.
	 * @param location The location of the container.
	 */
	public static boolean removePlayerFromContainer(UUID uuidToRemove, Location location) {
		List<UUID> trusted = null;
		for (LockedContainer container : getLockedContainers()) {
			Location loc1 = container.getLocations()[0];
			Location loc2 = container.getLocations()[0];
			if (loc2 == null) {
				if (isSameLocation(loc1, location)) {
					trusted = container.getTrusted();
					if (trusted.contains(uuidToRemove)) {
						trusted.remove(uuidToRemove);
						return true;
					}
				}
			} else {
				if (isSameLocation(loc1, location) || isSameLocation(loc2, location)) {
					trusted = container.getTrusted();
					if (trusted.contains(uuidToRemove)) {
						trusted.remove(uuidToRemove);
						return true;
					}
				}
			}
		}
		return false;
	}

	/**
	 * Confirms if the input block is a container block.
	 * @param block The block being verified.
	 * @return Confirmation whether the input block is a container block.
	 */
	public static boolean isContainerBlock(Block block) {
		return block.getType() == Material.CHEST
				|| block.getType() == Material.TRAPPED_CHEST
				|| block.getType() == Material.BARREL
				|| block.getType().name().endsWith("SHULKER_BOX");
	}

	/**
	 * Helper method to determine if the input locations are the same.
	 * @param loc1 The first location.
	 * @param loc2 The second location.
	 * @return Confirmation whether the input locations are the same.
	 */
	private static boolean isSameLocation(Location loc1, Location loc2) {
		return loc1.getBlockX() == loc2.getX()
				&& loc1.getBlockY() == loc2.getY()
				&& loc1.getBlockZ() == loc2.getZ();
	}

	/**
	 * Provides the LockedContainer at the given block, if it is indeed a locked container.
	 * @param block The block being searched.
	 * @return The LockedContainer object if found, or null if not.
	 */
	public static LockedContainer getLockedContainerAtBlock(Block block) {
		if (getLockedContainers() == null || getLockedContainers().isEmpty()) {
			return null;
		}

		if (isContainerBlock(block)) {
			for (LockedContainer container : getLockedContainers()) {
				Location loc1 = container.getLocations()[0];
				Location loc2 = container.getLocations()[1];
				// If it is a single chest/container
				if (loc2 == null) {
					if (isSameLocation(loc1, block.getLocation())) {
						return container;
					}
				} else {
					if (isSameLocation(loc1, block.getLocation()) || isSameLocation(loc2, block.getLocation())) {
						return container;
					}
				}
			}
		}
		return null;
	}

	/**
	 * Confirms whether the player is trusted to the container they are interacting with.
	 * @param player The player clicking the block.
	 * @param block The block that was clicked.
	 * @return Confirmation whether the player is trusted to the container.
	 */
	public static boolean canOpenContainer(Player player, Block block) {
		List<LockedContainer> lockedContainers = getLockedContainers();
		if (lockedContainers == null || lockedContainers.isEmpty()) {
			return true;
		}

		if (isContainerBlock(block)) {
			LockedContainer lockedContainer = getLockedContainerAtBlock(block);
			if (lockedContainer != null) {
				List<UUID> trusted = lockedContainer.getTrusted();
				if (trusted.contains(player.getUniqueId())) {
					return true;
				}
			}
			// The container is not locked
			else {
				return true;
			}
		}
		return false;
	}

	/**
	 * Verifies if the launched arrow and the arrow from the Quiver are the same.
	 * @param launchedArrow The launched arrow.
	 * @param quiverArrow The arrow in the quiver.
	 * @return The arrow if it matches.
	 */
	public static ItemStack verifyIsSameArrow(ItemStack launchedArrow, ItemStack quiverArrow) {
		// Basic or special arrow
		if (launchedArrow.getType() == Material.ARROW) {
			if (quiverArrow.getType() == Material.ARROW) {
				if (launchedArrow.hasItemMeta()) {
					if (quiverArrow.hasItemMeta()) {
						// Both have meta
						ItemMeta launchedMeta = launchedArrow.getItemMeta();
						ItemMeta quiverMeta = quiverArrow.getItemMeta();
						if (launchedMeta.getPersistentDataContainer().has(ARROW)) {
							if (quiverMeta.getPersistentDataContainer().has(ARROW)) {
								String launchedType = launchedMeta.getPersistentDataContainer().get(ARROW, PersistentDataType.STRING);
								String quiverType = quiverMeta.getPersistentDataContainer().get(ARROW, PersistentDataType.STRING);
								if (launchedType.equals(quiverType)) {
									return launchedArrow;
								} else {
									return null;
								}
							}
						}
						// One of them is not a Special arrow but has meta somehow
						Bukkit.getLogger().info("Something went wrong with identifying the arrows...");
						return null;
					} else {
						return null;
					}
				} else {
					if (quiverArrow.hasItemMeta()) {
						return null;
					} else {
						// Both are regular arrows
						return launchedArrow;
					}
				}
			} else {
				return null;
			}
		}
		// Spectral arrow
		else if (launchedArrow.getType() == Material.SPECTRAL_ARROW) {
			if (quiverArrow.getType() == Material.SPECTRAL_ARROW) {
				return launchedArrow;
			}
		}
		// Tipped arrow
		else {
			if (quiverArrow.hasItemMeta()) {
				if (launchedArrow.getItemMeta() instanceof PotionMeta launchedMeta
						&& quiverArrow.getItemMeta() instanceof PotionMeta quiverMeta) {
                    if (launchedMeta.getBasePotionType() == quiverMeta.getBasePotionType()) {
						return launchedArrow;
					} else {
						return null;
					}
				}
			} else {
				return null;
			}
		}

		return null;
	}

	/**
	 * Provides the ItemStack with a single quantity of the input arrow type.
	 * @param arrowType The type of custom arrow.
	 * @return The arrow with a single quantity.
	 */
	public static ItemStack getArrowFromType(String arrowType) {
		return switch (arrowType) {
			case "iron" -> new ArrowIron().getItem();
			case "gold" -> new ArrowGold().getItem();
			case "amethyst" -> new ArrowAmethyst().getItem();
			case "obsidian" -> new ArrowObsidian().getItem();
			case "diamond" -> new ArrowDiamond().getItem();
			default -> null;
		};
	}

	/**
	 * Provides the UUID associated to the given username.
	 * @param username The username being verified.
	 * @return The UUID of the associated player.
	 */
	public static UUID getUUIDFromUsername(String username) {
		for (UUID uuid : players.keySet()) {
			if (getPlayer(uuid).getUsername() == null) {
				continue;
			}

			if (getPlayer(uuid).getUsername().equals(username)) {
				return uuid;
			}
		}
		return null;
	}

	/**
	 * Provides the combined total number of homes that a given player can set.
	 * Includes homes from both the in-game rank, and Saint ranks.
	 * @param player The player.
	 * @return The combined total number of homes that a given player can set.
	 */
	public static int getMaxHomeNum(Player player) {
		AranarthPlayer aranarthPlayer = getPlayer(player.getUniqueId());
		int rankHomeNum = 0;
		int saintHomeNum = 0;

		if (aranarthPlayer.getRank() <= 2) {
			rankHomeNum = 1;
		} else if (aranarthPlayer.getRank() <= 3) {
			rankHomeNum = 2;
		} else if (aranarthPlayer.getRank() <= 4) {
			rankHomeNum = 3;
		} else if (aranarthPlayer.getRank() <= 6) {
			rankHomeNum = 4;
		} else if (aranarthPlayer.getRank() == 8) {
			rankHomeNum = 5;
		} else {
			rankHomeNum = 1;
		}

		if (aranarthPlayer.getSaintRank() == 1) {
			saintHomeNum = 2;
		} else if (aranarthPlayer.getSaintRank() == 2) {
			saintHomeNum = 5;
		} else if (aranarthPlayer.getSaintRank() == 3) {
			saintHomeNum = 10;
		}

		return rankHomeNum + saintHomeNum;
	}

	/**
	 * Adds a new home to the player.
	 * Assumes validation was done.
	 * @param player The player.
	 * @param home The player's home.
     */
	public static void addPlayerHome(Player player, Home home) {
		AranarthPlayer aranarthPlayer = getPlayer(player.getUniqueId());
		aranarthPlayer.getHomes().add(home);
		setPlayer(player.getUniqueId(), aranarthPlayer);
	}

	/**
	 * Deletes one of the player's homes.
	 * @param player The player.
	 * @param homeName The player's home name that they will be deleting.
	 */
	public static void deletePlayerHome(Player player, String homeName) {
		AranarthPlayer aranarthPlayer = getPlayer(player.getUniqueId());
		Home homeToDelete = null;
		for (Home home : aranarthPlayer.getHomes()) {
			if (homeName.equalsIgnoreCase(ChatUtils.stripColorFormatting(home.getHomeName()))) {
				homeToDelete = home;
			}
		}
		aranarthPlayer.getHomes().remove(homeToDelete);
		setPlayer(player.getUniqueId(), aranarthPlayer);
	}

	/**
	 * Provides the Location with a solid block that is directly underneath the player.
	 * @param player The player.
	 * @return The Location.
	 */
	public static Location getSolidBlockUnderneathPlayer(Player player) {
		Location loc = player.getLocation();
		World world = loc.getWorld();

		// Start from the player's current Y position and go downward
		int x = loc.getBlockX();
		int z = loc.getBlockZ();
		int y = loc.getBlockY();

		Block solidBlock = null;

		for (int currentY = y; currentY >= world.getMinHeight(); currentY--) {
			Block currentBlock = world.getBlockAt(x, currentY, z);
			Material type = currentBlock.getType();

			// Check if this block is solid and not water/lava
			if (type.isSolid() && type != Material.WATER && type != Material.LAVA) {
				solidBlock = currentBlock;
				break;
			}
		}

		// If a solid block was found, place the player just above it
		Location surfaceLoc = null;
		if (solidBlock != null) {
			surfaceLoc = solidBlock.getLocation().add(0.0, 1, 0.0); // Center and place feet above
			surfaceLoc.setYaw(loc.getYaw());
			surfaceLoc.setPitch(loc.getPitch());
		} else {
			player.sendMessage(ChatUtils.chatMessage("&cYou cannot set a home here!"));
		}
		return surfaceLoc;
	}

	/**
	 * Refreshes all muted players.
	 */
	public static void refreshMutes() {
		// Initial startup of server
		if (mutedPlayers.isEmpty()) {
			for (UUID uuid : players.keySet()) {
				AranarthPlayer aranarthPlayer = getPlayer(uuid);
				if (!aranarthPlayer.getMuteEndDate().isEmpty()) {
					mutedPlayers.add(uuid);
				}
			}
		} else {
			LocalDateTime currentDate = LocalDateTime.now();
			List<UUID> toRemove = new ArrayList<>();
			for (UUID uuid : mutedPlayers) {
				AranarthPlayer aranarthPlayer = getPlayer(uuid);
				String muteEndDate = aranarthPlayer.getMuteEndDate();

				if (muteEndDate.isEmpty()) {
					continue;
				}

				LocalDateTime definedMuteDate = null;
				if (!muteEndDate.equals("none")) {
					try {
						int year = Integer.parseInt("20" + muteEndDate.substring(0, 2));
						int month = Integer.parseInt(trimZero(muteEndDate.substring(2, 4)));
						int day = Integer.parseInt(trimZero(muteEndDate.substring(4, 6)));
						int hour = Integer.parseInt(trimZero(muteEndDate.substring(6, 8)));
						int minute = Integer.parseInt(trimZero(muteEndDate.substring(8, 10)));
						definedMuteDate = LocalDateTime.of(year, month, day, hour, minute);
					} catch (NumberFormatException e) {
						Bukkit.getLogger().info("Something went wrong with parsing the player's mute date...");
						return;
					}
				} else {
					// Always will be before the current date if "none" is the end date
					definedMuteDate = LocalDateTime.of(2000, 1, 1, 1, 1);
				}

				if (definedMuteDate.isBefore(currentDate)) {
					toRemove.add(uuid);
				}
			}

			for (UUID uuid : toRemove) {
				AranarthPlayer aranarthPlayer = getPlayer(uuid);
				AranarthUtils.removeMutedPlayer(uuid);
				aranarthPlayer.setMuteEndDate("");
				AranarthUtils.setPlayer(uuid, aranarthPlayer);

				OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(uuid);
				Bukkit.getLogger().info(offlinePlayer.getName() + " has been unmuted");
				if (offlinePlayer.isOnline()) {
					Player player = Bukkit.getPlayer(uuid);
					player.sendMessage(ChatUtils.chatMessage("&7You are no longer muted"));
				}
			}
		}
	}

	/**
	 * Trims the leading zero if the value is only one digit.
	 * @param value The value.
	 * @return The trimmed value.
	 */
	private static String trimZero(String value) {
		if (value.startsWith("0")) {
			return value.substring(1);
		} else {
			return value;
		}
	}

	/**
	 * Adds the player's UUID to the list of muted players.
	 * @param uuid The player's UUID.
	 */
	public static void addMutedPlayer(UUID uuid) {
		mutedPlayers.add(uuid);
	}

	/**
	 * Removes the player's UUID from the list of muted players.
	 * @param uuid The player's UUID.
	 */
	public static void removeMutedPlayer(UUID uuid) {
		mutedPlayers.remove(uuid);
	}

	/**
	 * Provides the String portion of the player's rank.
	 * @param aranarthPlayer The AranarthPlayer that is being analyzed.
	 * @return The String portion of the player's rank.
	 */
	public static String getRank(AranarthPlayer aranarthPlayer) {
		int rank = aranarthPlayer.getRank();
		if (aranarthPlayer.getPronouns() == Pronouns.MALE) {
			switch (rank) {
				case 1: return "&d[&aEsquire&d] &r";
				case 2: return "&7[&fKnight&7] &r";
				case 3: return "&5[&dBaron&5] &r";
				case 4: return "&8[&7Count&8] &r";
				case 5: return "&6[&eDuke&6] &r";
				case 6: return "&6[&bPrince&6] &r";
				case 7: return "&6[&9King&6] &r";
				case 8: return "&6[&4Emperor&6] &r";
			}
		} else {
			switch (rank) {
				case 1: return "&d[&aEsquire&d] &r";
				case 2: return "&7[&fKnight&7] &r";
				case 3: return "&5[&dBaroness&5] &r";
				case 4: return "&8[&7Countess&8] &r";
				case 5: return "&6[&eDuchess&6] &r";
				case 6: return "&6[&bPrincess&6] &r";
				case 7: return "&6[&9Queen&6] &r";
				case 8: return "&6[&4Empress&6] &r";
			}
		}
		return "&8[&aPeasant&8] &r";
	}

	/**
	 * Provides the String portion of the player's Saint rank.
	 * @param aranarthPlayer The AranarthPlayer that is being analyzed.
	 * @return The String portion of the player's Saint rank.
	 */
	public static String getSaintRank(AranarthPlayer aranarthPlayer) {
		int saintRank = aranarthPlayer.getSaintRank();
		return switch (saintRank) {
			case 1 -> "&b⚜&r";
			case 2 -> "&e⚜&r";
			case 3 -> "&c⚜&r";
			default -> "";
		};
	}

	/**
	 * Provides the String portion of the player's Council rank.
	 * @param aranarthPlayer The AranarthPlayer that is being analyzed.
	 * @return The String portion of the player's Council rank.
	 */
	public static String getCouncilRank(AranarthPlayer aranarthPlayer) {
		int councilRank = aranarthPlayer.getCouncilRank();
		return switch (councilRank) {
			case 1 -> "&3۞ &r";
			case 2 -> "&6۞ &r";
			case 3 -> "&4۞ &r";
			default -> "";
		};
	}

	/**
	 * Provides the String portion of the player's Architect rank.
	 * @param aranarthPlayer The AranarthPlayer that is being analyzed.
	 * @return The String portion of the player's Architect rank.
	 */
	public static String getArchitectRank(AranarthPlayer aranarthPlayer) {
		int architectRank = aranarthPlayer.getArchitectRank();
		return switch (architectRank) {
			case 1 -> "&a&l\uD83D\uDD28 &r"; // Hammer emoji
			default -> "";
		};
	}
}
