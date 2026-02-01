package com.aearost.aranarthcore.utils;

import com.aearost.aranarthcore.AranarthCore;
import com.aearost.aranarthcore.enums.Month;
import com.aearost.aranarthcore.enums.Pronouns;
import com.aearost.aranarthcore.enums.Weather;
import com.aearost.aranarthcore.items.arrow.*;
import com.aearost.aranarthcore.objects.*;
import com.projectkorra.projectkorra.BendingPlayer;
import org.bukkit.*;
import org.bukkit.ban.ProfileBanList;
import org.bukkit.block.Block;
import org.bukkit.block.Chest;
import org.bukkit.block.DoubleChest;
import org.bukkit.block.data.Levelled;
import org.bukkit.entity.Player;
import org.bukkit.entity.TextDisplay;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BannerMeta;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.profile.PlayerProfile;
import org.bukkit.scheduler.BukkitRunnable;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;
import java.util.function.Consumer;

import static com.aearost.aranarthcore.objects.CustomItemKeys.ARMOR_TYPE;
import static com.aearost.aranarthcore.objects.CustomItemKeys.ARROW;


/**
 * Provides a large variety of utility methods for everything related to AranarthCore.
 */
public class AranarthUtils {

	private static final HashMap<UUID, AranarthPlayer> players = new HashMap<>();
	private static List<Home> homes = new ArrayList<>();
	private static final HashMap<Location, Integer> dragonHeads = new HashMap<>();
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
	private static List<Home> warps = new ArrayList<>();
	private static final HashMap<UUID, List<Punishment>> punishments = new HashMap<>();
	private static final List<UUID> originalPlayers = new ArrayList<>();
	private static int phantomSpawnDelay = 0;
	private static final HashMap<Boost, LocalDateTime> serverBoosts = new HashMap<>();
	private static final HashMap<UUID, List<Material>> compressibleTypes = new HashMap<>();
	private static final List<CrateType> cratesInUse = new ArrayList<>();
	private static final List<TextDisplay> textHolograms = new ArrayList<>();
	private static final HashMap<UUID, Location> shopLocations = new LinkedHashMap<>();

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
		if (uuid.equals(getUUIDFromUsername("Aearost")) || uuid.equals(getUUIDFromUsername("Aearxst"))
				|| uuid.equals(getUUIDFromUsername("cocomocody")) || uuid.equals(getUUIDFromUsername("Leiks"))
				|| uuid.equals(getUUIDFromUsername("SachsiBua")) || uuid.equals(getUUIDFromUsername("_Seoltang"))
				|| uuid.equals(getUUIDFromUsername("im_Hazel")) || uuid.equals(getUUIDFromUsername("_Breathtaking"))) {
			originalPlayers.add(uuid);
		}
	}

	/**
	 * Determines if the player is one of the original players of Aranarth.
	 * @param uuid The player's UUID.
	 * @return Confirmation whether the player is one of the original players of Aranarth.
	 */
	public static boolean isOriginalPlayer(UUID uuid) {
		if (originalPlayers.contains(uuid)) {
			return true;
		} else {
			return false;
		}
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
		return (nickname == null || nickname.isEmpty()) ? getUsername(player) : nickname;
	}

	/**
	 * Adds a new homepad location.
	 *
	 * @param location The Location that the homepad is located at.
	 */
	public static void addNewHomepad(Location location) {
		homes.add(new Home("NEW", location, Material.HEAVY_WEIGHTED_PRESSURE_PLATE));
	}

	/**
	 * Updates the name, location, and/or icon of an existing homepad.
	 *
	 * @param homeName The new name to be used for the home.
	 * @param direction The Location containing the direction of the homepad to be used.
	 * @param icon The Material that will be displayed for the homepad.
	 */
	public static void updateHomepad(String homeName, Location direction, Material icon) {
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
	public static List<Home> getHomepads() {
		return homes;
	}

	/**
	 * Overrides the list of homepads being used.
	 *
	 * @param newHomes The new list of homepads.
	 */
	public static void setHomepads(List<Home> newHomes) {
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
	public static Home getHomepad(Location location) {
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
	public static void removeHomepad(Location location) {
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

		// Include any world that should share the inventory of Survival
		if (currentWorld.startsWith("smp") || currentWorld.startsWith("resource")) {
			currentWorld = "world";
		}
		if (destinationWorld.startsWith("smp") || destinationWorld.startsWith("resource")) {
			destinationWorld = "world";
		}

		boolean isSurvivalToSurvival = currentWorld.startsWith("world") && destinationWorld.startsWith("world");
		boolean isSameWorld = currentWorld.equals(destinationWorld);

		// No need to change inventory
		if (isSurvivalToSurvival || isSameWorld) {
			if (!destinationWorld.equals("creative")) {
				if (aranarthPlayer.getCouncilRank() != 3) {
					player.setGameMode(GameMode.SURVIVAL);
				}
				return;
			}
		} else {
			// Remove potion effects when changing the world
			for (PotionEffect effect : player.getActivePotionEffects()) {
				player.removePotionEffect(effect.getType());
			}
		}

		if (currentWorld.startsWith("world")) {
			aranarthPlayer.setSurvivalInventory(ItemUtils.toBase64(player.getInventory()));
			if (destinationWorld.startsWith("arena")) {
				if (!aranarthPlayer.getArenaInventory().isEmpty()) {
					player.getInventory().setContents(ItemUtils.itemStackArrayFromBase64(aranarthPlayer.getArenaInventory()));
					player.setGameMode(GameMode.SURVIVAL);
					PermissionUtils.toggleArenaBendingPermissions(player, true);
					PermissionUtils.updateSubElements(player);
					return;
				}
				player.getInventory().clear();
				player.getInventory().setArmorContents(new ItemStack[] {
						new ItemStack(Material.IRON_BOOTS, 1),
						new ItemStack(Material.IRON_LEGGINGS, 1),
						new ItemStack(Material.IRON_CHESTPLATE, 1),
						new ItemStack(Material.IRON_HELMET, 1)});
				player.setGameMode(GameMode.SURVIVAL);
				PermissionUtils.toggleArenaBendingPermissions(player, true);
				PermissionUtils.updateSubElements(player);
				return;
			} else if (destinationWorld.startsWith("creative")) {
				if (!aranarthPlayer.getCreativeInventory().isEmpty()) {
					player.getInventory().setContents(ItemUtils.itemStackArrayFromBase64(aranarthPlayer.getCreativeInventory()));
					player.setGameMode(GameMode.CREATIVE);
					return;
				}
			}
			player.getInventory().clear();
		} else if (currentWorld.startsWith("arena")) {
			if (destinationWorld.startsWith("world")) {
				aranarthPlayer.setArenaInventory(ItemUtils.toBase64(player.getInventory()));
				if (!aranarthPlayer.getSurvivalInventory().isEmpty()) {
					player.getInventory().setContents(ItemUtils.itemStackArrayFromBase64(aranarthPlayer.getSurvivalInventory()));
					player.setGameMode(GameMode.SURVIVAL);
					PermissionUtils.toggleArenaBendingPermissions(player, false);
					PermissionUtils.updateSubElements(player);
					return;
				}
			} else if (destinationWorld.startsWith("creative")) {
				aranarthPlayer.setArenaInventory(ItemUtils.toBase64(player.getInventory()));
				if (!aranarthPlayer.getCreativeInventory().isEmpty()) {
					player.getInventory().setContents(ItemUtils.itemStackArrayFromBase64(aranarthPlayer.getCreativeInventory()));
					player.setGameMode(GameMode.CREATIVE);
					PermissionUtils.toggleArenaBendingPermissions(player, false);
					PermissionUtils.updateSubElements(player);
					return;
				}
			}
			player.getInventory().clear();
		} else if (currentWorld.startsWith("creative")) {
			if (destinationWorld.startsWith("world")) {
				aranarthPlayer.setCreativeInventory(ItemUtils.toBase64(player.getInventory()));
				if (!aranarthPlayer.getSurvivalInventory().isEmpty()) {
					player.getInventory().setContents(ItemUtils.itemStackArrayFromBase64(aranarthPlayer.getSurvivalInventory()));
					player.setGameMode(GameMode.SURVIVAL);
					return;
				}
			} else if (destinationWorld.startsWith("arena")) {
				aranarthPlayer.setCreativeInventory(ItemUtils.toBase64(player.getInventory()));
				if (!aranarthPlayer.getArenaInventory().isEmpty()) {
					player.getInventory().setContents(ItemUtils.itemStackArrayFromBase64(aranarthPlayer.getArenaInventory()));
					player.setGameMode(GameMode.SURVIVAL);
					PermissionUtils.toggleArenaBendingPermissions(player, true);
					PermissionUtils.updateSubElements(player);
					return;
				}
				player.getInventory().clear();
				player.getInventory().setArmorContents(new ItemStack[] {
						new ItemStack(Material.IRON_BOOTS, 1),
						new ItemStack(Material.IRON_LEGGINGS, 1),
						new ItemStack(Material.IRON_CHESTPLATE, 1),
						new ItemStack(Material.IRON_HELMET, 1)});
				player.setGameMode(GameMode.SURVIVAL);
				PermissionUtils.toggleArenaBendingPermissions(player, true);
				PermissionUtils.updateSubElements(player);
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
			player.addPotionEffect(new PotionEffect(PotionEffectType.STRENGTH, 320, 2));
			player.addPotionEffect(new PotionEffect(PotionEffectType.RESISTANCE, 320, 1));
			player.addPotionEffect(new PotionEffect(PotionEffectType.HEALTH_BOOST, 320, 4));
		} else if (isWearingArmorType(player, "dwarven")) {
			player.addPotionEffect(new PotionEffect(PotionEffectType.NIGHT_VISION, 320, 0));
			player.addPotionEffect(new PotionEffect(PotionEffectType.HEALTH_BOOST, 320, 4));
		} else if (isWearingArmorType(player, "elven")) {
			player.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, 320, 2));
			player.addPotionEffect(new PotionEffect(PotionEffectType.JUMP_BOOST, 320, 1));
			player.addPotionEffect(new PotionEffect(PotionEffectType.HEALTH_BOOST, 320, 9));
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

	/**
	 * Applies the waterfall effect at the base of flowing water.
	 */
	public static void applyWaterfallEffect() {
		for (Player player : Bukkit.getOnlinePlayers()) {
			AranarthPlayer aranarthPlayer = AranarthUtils.getPlayer(player.getUniqueId());
			// Skips the particle
			if (new Random().nextInt(100) >= aranarthPlayer.getParticleNum()) {
				continue;
			}

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
			Location loc2 = container.getLocations()[1];
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
			Location loc2 = container.getLocations()[1];
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
		if (block == null) {
			return false;
		}

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
			case "arrowhead" -> new ItemStack(Material.ARROW, 4);
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
		int perksHomeNum = 0;

		int rank = aranarthPlayer.getRank();
		if (rank == 0 || rank == 1) {
			rankHomeNum = 1;
		} else if (rank == 2 || rank == 3) {
			rankHomeNum = 2;
		} else if (rank == 4 || rank == 5) {
			rankHomeNum = 3;
		} else if (rank == 6 || rank == 7) {
			rankHomeNum = 4;
		} else if (rank == 8) {
			rankHomeNum = 5;
		}

		if (aranarthPlayer.getSaintRank() == 1) {
			saintHomeNum = 1;
		} else if (aranarthPlayer.getSaintRank() == 2) {
			saintHomeNum = 3;
		} else if (aranarthPlayer.getSaintRank() == 3) {
			saintHomeNum = 5;
		}

		int perksHomeAmount = aranarthPlayer.getPerks().get(Perk.HOMES);

		return rankHomeNum + saintHomeNum + perksHomeAmount;
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
			if (homeName.equalsIgnoreCase(ChatUtils.stripColorFormatting(home.getName()))) {
				homeToDelete = home;
			}
		}
		aranarthPlayer.getHomes().remove(homeToDelete);
		setPlayer(player.getUniqueId(), aranarthPlayer);
	}

	/**
	 * Provides the Location with a solid block that is directly underneath the player.
	 * @param loc The teleport location.
	 * @return The Location.
	 */
	public static Location getSafeTeleportLocation(Location loc) {
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
			surfaceLoc = solidBlock.getLocation().add(0.5, 1, 0.5); // Center and place feet above
			surfaceLoc.setYaw(loc.getYaw());
			surfaceLoc.setPitch(loc.getPitch());
		} else {
			return null;
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
					// Always will be after the current date if "none" is the end date
					definedMuteDate = LocalDateTime.of(9999, 1, 1, 1, 1);
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

				Punishment punishment = new Punishment(
						uuid, LocalDateTime.now(), "UNMUTE", "The player's mute has automatically ended", null);
				DiscordUtils.addPunishmentToDiscord(punishment);

				OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(uuid);
				if (offlinePlayer.isOnline()) {
					Player player = Bukkit.getPlayer(uuid);
					player.sendMessage(ChatUtils.chatMessage("&7You are no longer muted"));
				}
			}
		}
	}

	/**
	 * Refreshes all banned players.
	 */
	public static void refreshBans() {
		ProfileBanList profileBanList = Bukkit.getBanList(BanList.Type.PROFILE);
		List<BanEntry<PlayerProfile>> toRemove = new ArrayList<>();
		for (BanEntry<? super com.destroystokyo.paper.profile.PlayerProfile> entry : profileBanList.getEntries()) {
			PlayerProfile profile = (PlayerProfile) entry.getBanTarget();
			UUID uuid = profile.getUniqueId();
			// If the player's ban is temporary
			if (entry.getExpiration() != null) {
				// If they are no longer banned
				if (entry.getExpiration().before(new Date())) {
					Punishment punishment = new Punishment(
							uuid, LocalDateTime.now(), "UNBAN", "The player's ban has automatically ended", null);
					DiscordUtils.addPunishmentToDiscord(punishment);
					entry.remove();
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
			case 1 -> "&3۞&r";
			case 2 -> "&6۞&r";
			case 3 -> "&4۞&r";
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
			case 1 -> "&a&l\uD83D\uDD28&r"; // Hammer emoji
			default -> "";
		};
	}

	/**
	 * Confirms if the input coordinate is within the server Spawn.
	 * @param loc The location of the block or entity.
	 * @return Confirmation if the input coordinate is within the server Spawn.
	 */
	public static boolean isSpawnLocation(Location loc) {
		int topRightX = 335;
		int topRightZ = -447;
		int bottomLeftX = -351;
		int bottomLeftZ = 255;

		if (!loc.getWorld().getName().equals("world")) {
			return false;
		}

		int x = loc.getBlockX();
		int z = loc.getBlockZ();

		if ((bottomLeftX < x && x < topRightX) && (bottomLeftZ > z && z > topRightZ)) {
			return true;
		}
		return false;
	}

	/**
	 * Plays the teleport sound effect.
	 * @param player The player to play the sound for.
	 * @return Confirmation that the teleport was successful
	 */
	public static boolean teleportPlayer(Player player, Location from, Location to) {
		// Teleporting seems to manually toggle on a player's bending when it was toggled off
		BendingPlayer bendingPlayer = BendingPlayer.getBendingPlayer(player);
		boolean isToggled = false;
		if (!bendingPlayer.isToggled()) {
			isToggled = true;
		}

		Location locToTeleportTo = getSafeTeleportLocation(to);
		// If i.e over the void
		if (locToTeleportTo == null) {
			player.sendMessage(ChatUtils.chatMessage("&cThis teleport location is unsafe!"));
			return false;
		}
		player.teleport(locToTeleportTo);
		player.playSound(player, Sound.ENTITY_ENDERMAN_TELEPORT, 1F, 0.9F);

		// Saves the player's last location for /ac back
		AranarthPlayer aranarthPlayer = getPlayer(player.getUniqueId());
		aranarthPlayer.setLastKnownTeleportLocation(from);
		setPlayer(player.getUniqueId(), aranarthPlayer);

		try {
			AranarthUtils.switchInventory(player, from.getWorld().getName(), to.getWorld().getName());
			// Toggles off the bending if it should be toggled off
			if (isToggled) {
				bendingPlayer.toggleBending();
			}
			return true;
		} catch (IOException e) {
			player.sendMessage(ChatUtils.chatMessage("&cSomething went wrong with changing world."));
			return false;
		}
	}

	/**
	 * Calculates the number of particles to be displayed based on the player's particle value.
	 * @param particleNum The particle amount from the calculated server functionality.
	 * @param playerParticleNum The amount of particles the player has set to be displayed.
	 * @return The number of particles to display for the player.
	 */
	public static int calculateParticlesForPlayer(int particleNum, int playerParticleNum) {
		double calculatedDouble = particleNum;
		calculatedDouble = particleNum * ((double) playerParticleNum / 100);
		int calculatedInt = (int) Math.floor(calculatedDouble);

		// If the calculated amount yields a value lower than 0 yet the player has particles enabled
		if (calculatedInt == 0 && playerParticleNum != 0) {
			return 1;
		} else {
			return calculatedInt;
		}
	}

	/**
	 * Provides the player's timezone.
	 * @param player The player.
	 * @param callback The player's timezone.
	 */
	public static void getPlayerTimezone(Player player, Consumer<ZoneId> callback) {
		String ip = player.getAddress().getAddress().getHostAddress();
		Bukkit.getScheduler().runTaskAsynchronously(AranarthCore.getInstance(), () -> {
			try {
				URL url = new URL("http://ip-api.com/json/" + ip + "?fields=timezone");
				BufferedReader reader = new BufferedReader(new InputStreamReader(url.openStream()));
				String[] lineSplit = reader.readLine().split("\"");
				ZoneId zoneId = ZoneId.of(lineSplit[3]);
				reader.close();

				// Switch back to main thread for player messaging
				Bukkit.getScheduler().runTask(AranarthCore.getInstance(), () -> callback.accept(zoneId));
			} catch (Exception e) {
				Bukkit.getScheduler().runTask(AranarthCore.getInstance(), () -> callback.accept(null));
			}
		});
	}

	/**
	 * Provides the list of warps.
	 * @return The list of warps.
	 */
	public static List<Home> getWarps() {
		return warps;
	}

	/**
	 * Updates the list of warps.
	 * @param newWarps The list of warps.
	 */
	public static void setWarps(List<Home> newWarps) {
		warps = newWarps;
	}

	/**
	 * Adds a new warp to the list of warps.
	 * @param warp The new warp.
	 */
	public static void addWarp(Home warp) {
		warps.add(warp);
	}

	/**
	 * Removes a warp from the list of warps.
	 * @param warp The warp to be removed.
	 */
	public static void removeWarp(Home warp) {
		warps.remove(warp);
	}

	/**
	 * Provides the HashMap of all punishments for all players.
	 * @return The HashMap of all punishments for all players.
	 */
	public static HashMap<UUID, List<Punishment>> getAllPunishments() {
		return punishments;
	}

	/**
	 * Provides the HashMap of all punishments for a given player's UUID.
	 * @param uuid The player's UUID.
	 * @return The HashMap of all punishments for a given player's UUID.
	 */
	public static List<Punishment> getPunishments(UUID uuid) {
		return punishments.get(uuid);
	}

	/**
	 * Adds a new punishment to the player's List.
	 * @param uuid The player's UUID.
	 * @param punishment The new punishment.
	 * @param isFromStartup Whether the punishment is added from server startup or a true new punishment.
	 */
	public static void addPunishment(UUID uuid, Punishment punishment, boolean isFromStartup) {
		if (punishments.get(uuid) == null) {
			punishments.put(uuid, new ArrayList<>());
		}

		// Send message to #punishment-history in Discord if it's a new punishment being added
		if (!isFromStartup) {
			DiscordUtils.addPunishmentToDiscord(punishment);
		}

		punishments.get(uuid).add(punishment);
	}

	/**
	 * Removes an existing punishment from the player.
	 * @param uuid The player's UUID.
	 * @param punishment The punishment being removed.
	 */
	public static void removePunishment(UUID uuid, Punishment punishment) {
		punishments.get(uuid).remove(punishment);
	}

	/**
	 * Applies passive effects to all players that are in Spawn.
	 */
	public static void applySpawnBuffs() {
		for (Player player : Bukkit.getOnlinePlayers()) {
			if (isSpawnLocation(player.getLocation())) {
				player.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, 100, 4));
				player.addPotionEffect(new PotionEffect(PotionEffectType.JUMP_BOOST, 100, 2));
			}
		}
	}

	/**
	 * Provides the names for the potions that a player has.
	 * Note the inner HashMap reflects only one single potion per outer HashMap.
	 * @param player The player.
	 * @return The HashMap of names and the quantities of the associated potion.
	 */
	public static HashMap<String, HashMap<ItemStack, Integer>> getPlayerPotionNames(Player player) {
		AranarthPlayer aranarthPlayer = getPlayer(player.getUniqueId());
		HashMap<ItemStack, Integer> potions = aranarthPlayer.getPotions();
		HashMap<String, HashMap<ItemStack, Integer>> nameToPotionAmount = new HashMap<>();

		for (ItemStack potion : potions.keySet()) {
			String potionName = null;
			if (potion.getType() == Material.AIR) {
				potions.remove(potion);
				continue;
			}

			// If it is an mcMMO potion
			if (potion.hasItemMeta() && potion.getItemMeta().hasCustomName()) {
				// customName() is in the following format by default
				/*
				TextComponentImpl{content="", style=StyleImpl{obfuscated=not_set, bold=not_set, strikethrough=not_set,
					underlined=not_set, italic=not_set, color=null, shadowColor=null, clickEvent=null, hoverEvent=null,
					insertion=null, font=null}, children=[TextComponentImpl{content="Potion Of Haste",
					style=StyleImpl{obfuscated=false, bold=false, strikethrough=false, underlined=false, italic=false, color=null,
					shadowColor=null, clickEvent=null, hoverEvent=null, insertion=null, font=null}, children=[]}]}
				 */
				String[] componentParts = (potion.getItemMeta().customName().children().get(0) + "").split("\"");
				potionName = componentParts[1];
			} else {
				PotionMeta meta = (PotionMeta) potion.getItemMeta();
				if (meta != null) {
					potionName = addPotionConsumptionMethodToName(potion, ChatUtils.getFormattedItemName(meta.getBasePotionType().name()));
				}
			}
			HashMap<ItemStack, Integer> potionToAdd = new HashMap<>();
			potionToAdd.put(potion, potions.get(potion));
			nameToPotionAmount.put(potionName, potionToAdd);
		}
		return nameToPotionAmount;
	}

	/**
	 * Provides the formatted name for a given potion.
	 * @param potion The potion item.
	 * @param potionName The base name for the potion.
	 * @return The formatted name for a given potion.
	 */
	private static String addPotionConsumptionMethodToName(ItemStack potion, String potionName) {
		String[] partsOfName = potionName.split(" ");
		StringBuilder finalName = new StringBuilder();

		if (potionName.startsWith("Long")) {
			if (potion.getType() == Material.POTION) {
				finalName = new StringBuilder("Extended Potion of ");
			} else if (potion.getType() == Material.SPLASH_POTION) {
				finalName = new StringBuilder("Extended Splash Potion of ");
			} else if (potion.getType() == Material.LINGERING_POTION) {
				finalName = new StringBuilder("Extended Lingering Potion of ");
			}
		} else {
			if (potion.getType() == Material.POTION) {
				finalName = new StringBuilder("Potion of ");
			} else if (potion.getType() == Material.SPLASH_POTION) {
				finalName = new StringBuilder("Splash Potion of ");
			} else if (potion.getType() == Material.LINGERING_POTION) {
				finalName = new StringBuilder("Lingering Potion of ");
			}
		}

		// Handles formatting the actual potion name
		for (int i = 0; i < partsOfName.length; i++) {
			if (partsOfName[i].equals("Long") || partsOfName[i].equals("Strong") || partsOfName[i].equals("of")) {
				continue;
			} else {
				if (i == partsOfName.length - 1) {
					finalName.append(partsOfName[i]);
				} else {
					finalName.append(partsOfName[i]).append(" ");
				}
			}
		}

		if (potionName.startsWith("Strong")) {
			finalName.append(" II");
		}

		return finalName.toString();
	}

	/**
	 * Provides the combined total number of potions that a given player has.
	 * Includes potion from both the in-game rank, and Saint ranks.
	 * @param player The player.
	 * @return The combined total number of potions that a given player has.
	 */
	public static int getMaxPotionNum(Player player) {
		AranarthPlayer aranarthPlayer = getPlayer(player.getUniqueId());
		int rankPotionNum = 0;
		int saintPotionNum = 0;

		if (aranarthPlayer.getRank() == 1) {
			rankPotionNum = 100;
		} else if (aranarthPlayer.getRank() == 2) {
			rankPotionNum = 200;
		} else if (aranarthPlayer.getRank() == 3) {
			rankPotionNum = 350;
		} else if (aranarthPlayer.getRank() == 4) {
			rankPotionNum = 500;
		} else if (aranarthPlayer.getRank() == 5) {
			rankPotionNum = 750;
		} else if (aranarthPlayer.getRank() == 6) {
			rankPotionNum = 1000;
		} else if (aranarthPlayer.getRank() == 7) {
			rankPotionNum = 1500;
		} else if (aranarthPlayer.getRank() == 8) {
			rankPotionNum = 2500;
		} else {
			rankPotionNum = 50;
		}

		if (aranarthPlayer.getSaintRank() == 1) {
			saintPotionNum = 500;
		} else if (aranarthPlayer.getSaintRank() == 2) {
			saintPotionNum = 1000;
		} else if (aranarthPlayer.getSaintRank() == 3) {
			saintPotionNum = 2500;
		}

		return rankPotionNum + saintPotionNum;
	}

	/**
	 * Provides the current number of potions that the player has stored in their potions pouch.
	 * @param player The player.
	 * @return The number of potions that the player has stored in their potions pouch.
	 */
	public static int getPlayerStoredPotionNum(Player player) {
		AranarthPlayer aranarthPlayer = getPlayer(player.getUniqueId());
		if (aranarthPlayer.getPotions() == null || aranarthPlayer.getPotions().isEmpty()) {
			return 0;
		}

		int storedPotionNum = 0;
		for (int amount : aranarthPlayer.getPotions().values()) {
			storedPotionNum += amount;
		}
		return storedPotionNum;
	}

	/**
	 * Provides the combined total number of Quiver slots that a given player has.
	 * Includes quiver slots from both the in-game rank, and Saint ranks.
	 * @param player The player.
	 * @return The combined total number of Quiver slots that a given player has.
	 */
	public static int getMaxQuiverSize(Player player) {
		AranarthPlayer aranarthPlayer = getPlayer(player.getUniqueId());
		int rankQuiverSlotNum = 0;
		int saintQuiverSlotNum = 0;

		if (aranarthPlayer.getRank() == 1) {
			rankQuiverSlotNum = 5;
		} else if (aranarthPlayer.getRank() == 2) {
			rankQuiverSlotNum = 9;
		} else if (aranarthPlayer.getRank() == 3) {
			rankQuiverSlotNum = 12;
		} else if (aranarthPlayer.getRank() == 4) {
			rankQuiverSlotNum = 18;
		} else if (aranarthPlayer.getRank() == 5) {
			rankQuiverSlotNum = 25;
		} else if (aranarthPlayer.getRank() == 6) {
			rankQuiverSlotNum = 30;
		} else if (aranarthPlayer.getRank() == 7) {
			rankQuiverSlotNum = 36;
		} else if (aranarthPlayer.getRank() == 8) {
			rankQuiverSlotNum = 45;
		} else {
			rankQuiverSlotNum = 3;
		}

		if (aranarthPlayer.getSaintRank() == 1) {
			saintQuiverSlotNum = 3;
		} else if (aranarthPlayer.getSaintRank() == 2) {
			saintQuiverSlotNum = 6;
		} else if (aranarthPlayer.getSaintRank() == 3) {
			saintQuiverSlotNum = 9;
		}

		return rankQuiverSlotNum + saintQuiverSlotNum;
	}

	/**
	 * Provides the total number of player shops that a given player can create.
	 * @param player The player.
	 * @return The total number of player shops that a given player can create.
	 */
	public static int getMaxShopNum(Player player) {
		AranarthPlayer aranarthPlayer = getPlayer(player.getUniqueId());
		int rankQuiverSlotNum = 0;
		int saintQuiverSlotNum = 0;

		if (aranarthPlayer.getRank() == 3) {
			rankQuiverSlotNum = 3;
		} else if (aranarthPlayer.getRank() == 4) {
			rankQuiverSlotNum = 7;
		} else if (aranarthPlayer.getRank() == 5) {
			rankQuiverSlotNum = 15;
		} else if (aranarthPlayer.getRank() == 6) {
			rankQuiverSlotNum = 30;
		} else if (aranarthPlayer.getRank() == 7) {
			rankQuiverSlotNum = 50;
		} else if (aranarthPlayer.getRank() == 8) {
			rankQuiverSlotNum = -1;
		} else {
			rankQuiverSlotNum = 0;
		}

		if (aranarthPlayer.getSaintRank() == 1) {
			saintQuiverSlotNum = 3;
		} else if (aranarthPlayer.getSaintRank() == 2) {
			saintQuiverSlotNum = 6;
		} else if (aranarthPlayer.getSaintRank() == 3) {
			saintQuiverSlotNum = 9;
		}

		return rankQuiverSlotNum + saintQuiverSlotNum;
	}

	/**
	 * Determines if the player is currently blacklisting the given item.
	 * @param aranarthPlayer The aranarth player.
	 * @param item The item that is attempting to be picked up.
	 * @return 0 if the item should be deleted, 1 if the item should be ignored, -1 if the item is not blacklisted.
	 */
	public static int isBlacklistingItem(Player player, AranarthPlayer aranarthPlayer, ItemStack item) {
		if (!player.hasPermission("aranarth.blacklist")) {
			return -1;
		}

		List<ItemStack> blacklistedItems = aranarthPlayer.getBlacklist();
		if (blacklistedItems == null || blacklistedItems.isEmpty()) {
			return -1;
		}

		for (ItemStack is : blacklistedItems) {
			if (is.isSimilar(item)) {
				if (aranarthPlayer.isDeletingBlacklistedItems()) {
					return 0;
				} else {
					return 1;
				}
			}
		}
		return -1;
	}

	/**
	 * Provides the value for the manual spawning of phantoms during the month of Obscurvor.
	 * @return The value for the manual spawning of phantoms during the month of Obscurvor.
	 */
	public static int getPhantomSpawnDelay() {
		return phantomSpawnDelay;
	}

	/**
	 * Updates the value for the manual spawning of phantoms during the month of Obscurvor.
	 * @param newPhantomSpawnDelay The new value for the manual spawning of phantoms during the month of Obscurvor.
	 */
	public static void setPhantomSpawnDelay(int newPhantomSpawnDelay) {
		phantomSpawnDelay = newPhantomSpawnDelay;
	}

	/**
	 * Provides all server boosts that are currently active.
	 * @return The HashMap of server boosts that are currently active.
	 */
	public static HashMap<Boost, LocalDateTime> getServerBoosts() {
		return serverBoosts;
	}

	/**
	 * Applies a new server boost or increases the duration of an existing server boost.
	 * @param boost The type of boost being applied. A null value signifies 24 hours.
	 * @param duration The duration of the boost being applied.
	 * @param uuid The username of the player that is applying the boost.
	 */
	public static void addServerBoost(Boost boost, LocalDateTime duration, UUID uuid) {
		String name = "";
		if (boost == Boost.MINER) {
			name = "&8&lBoost of the Miner";
		} else if (boost == Boost.HARVEST) {
			name = "&6&lBoost of the Harvest";
		} else if (boost == Boost.HUNTER) {
			name = "&c&lBoost of the Hunter";
		} else if (boost == Boost.CHI) {
			name = "&f&lBoost of Chi";
		} else {
			name = "&7&lUnspecified Boost";
		}

		// A new boost will automatically apply for 24 hours
		if (duration == null) {
			// Increase by 24 hours if it already exists i.e the same boost was purchased twice
			if (serverBoosts.get(boost) != null) {
				LocalDateTime currentBoostEnd = serverBoosts.get(boost);
				LocalDateTime newBoostEnd = currentBoostEnd.plusDays(1);
				serverBoosts.put(boost, newBoostEnd);
			}
			// Create a new boost as it doesn't exist yet
			else {
				LocalDateTime now = LocalDateTime.now();
				LocalDateTime newBoostEnd = now.plusDays(1);
				serverBoosts.put(boost, newBoostEnd);
			}

			// Handles messages
			if (uuid == null) {
				Bukkit.broadcastMessage(ChatUtils.chatMessage("&7The " + name + " &7has been applied"));
				DiscordUtils.updateBoostInDiscord(null, boost, true);
			} else {Bukkit.broadcastMessage(ChatUtils.translateToColor(""));
				AranarthPlayer aranarthPlayer = AranarthUtils.getPlayer(uuid);
				Bukkit.broadcastMessage(ChatUtils.chatMessage("&7The " + name + " &7has been applied by &e" + aranarthPlayer.getNickname()));
				DiscordUtils.updateBoostInDiscord(uuid, boost, true);
			}
		}
		// Should only be called during server startup
		else {
			serverBoosts.put(boost, duration);
		}
	}

	/**
	 * Removes the specified server boost.
	 * @param boost The boost being removed.
	 */
	public static void removeServerBoost(Boost boost) {
		if (AranarthUtils.getServerBoosts().containsKey(boost)) {
			String name = "";
			if (boost == Boost.MINER) {
				name = "&8&lBoost of the Miner";
			} else if (boost == Boost.HARVEST) {
				name = "&6&lBoost of the Harvest";
			} else if (boost == Boost.HUNTER) {
				name = "&c&lBoost of the Hunter";
			} else if (boost == Boost.CHI) {
				name = "&f&lBoost of Chi";
			} else {
				name = "&7&lUnspecified Boost";
			}
			Bukkit.broadcastMessage(ChatUtils.chatMessage("&7The " + name + " &7has expired"));
			DiscordUtils.updateBoostInDiscord(null, boost, false);
			serverBoosts.remove(boost);
		}
	}

	/**
	 * Provides the messages for all active server boosts, specifying the name and the remaining duration of the boost
	 * @return The formatted name of the boost as the key, and the remaining duration of the boost as the value.
	 */
	public static HashMap<String, String> getActiveServerBoostsMessages() {
		HashMap<String, String> boostMessages = new HashMap<>();
		for (Boost boost : serverBoosts.keySet()) {
			LocalDateTime ldt = serverBoosts.get(boost);
			String name = "";
			if (boost == Boost.MINER) {
				name = "&8&lBoost of the Miner";
			} else if (boost == Boost.HARVEST) {
				name = "&6&lBoost of the Harvest";
			} else if (boost == Boost.HUNTER) {
				name = "&c&lBoost of the Hunter";
			} else if (boost == Boost.CHI) {
				name = "&f&lBoost of Chi";
			} else {
				name = "&7&lUnspecified Boost";
			}
			boostMessages.put(ChatUtils.translateToColor(name), getRemainingBoostDuration(boost));
		}
		return boostMessages;
	}

	/**
	 * Provides a String containing the text value of the remaining amount of time for the currently applied boost.
	 * @param boost The boost.
	 * @return The String containing the text value of the remaining amount of time for the currently applied boost.
	 */
	private static String getRemainingBoostDuration(Boost boost) {
		LocalDateTime expiry = AranarthUtils.getServerBoosts().get(boost);

		Duration duration = Duration.between(LocalDateTime.now(), expiry);
		int minutes = (int) duration.toMinutes();
		int hours = minutes / 60;
		int days = minutes / 1440;

		int remainingHours = hours % 24;
		int remainingMinutes = minutes % 60;

		if (days > 0) {
			return "&e" + days + "d " + remainingHours + "h " + remainingMinutes + "m";
		} else if (hours > 0) {
			return "&e" + hours + "h " + remainingMinutes + "m";
		} else {
			return "&e" + minutes + "m";
		}
	}

	/**
	 * Refreshes server boost functionality and deactivates them once they are no longer active.
	 */
	public static void refreshServerBoosts() {
		List<Boost> toRemove = new ArrayList<>();
		for (Boost boost : serverBoosts.keySet()) {
			String name = "";
			if (boost == Boost.MINER) {
				name = "&8&lBoost of the Miner";
			} else if (boost == Boost.HARVEST) {
				name = "&6&lBoost of the Harvest";
			} else if (boost == Boost.HUNTER) {
				name = "&c&lBoost of the Hunter";
			} else if (boost == Boost.CHI) {
				name = "&f&lBoost of Chi";
			} else {
				name = "&7&lUnspecified Boost";
			}

			if (serverBoosts.get(boost).isBefore(LocalDateTime.now())) {
				Bukkit.broadcastMessage(ChatUtils.chatMessage("&7The " + name + " &7has expired"));
				DiscordUtils.updateBoostInDiscord(null, boost, false);
				serverBoosts.remove(boost);
			}
		}

		if (serverBoosts.containsKey(Boost.MINER)) {
			for (Player player : Bukkit.getOnlinePlayers()) {
				player.addPotionEffect(new PotionEffect(PotionEffectType.HASTE, 220, 1));
			}
		} else if (serverBoosts.containsKey(Boost.HUNTER) || serverBoosts.containsKey(Boost.CHI)) {
			for (Player player : Bukkit.getOnlinePlayers()) {
				player.addPotionEffect(new PotionEffect(PotionEffectType.STRENGTH, 220, 1));
			}
		}
	}

	/**
	 * Provides the HashMap of all UUIDs and the lists of Materials that they are compressing.
	 * @return The HashMap of all UUIDs and the lists of Materials that they are compressing.
	 */
	public static HashMap<UUID, List<Material>> getCompressibleTypes() {
		return compressibleTypes;
	}

	/**
	 * Adds the material to the player's list of compressible items.
	 * @param uuid The UUID of the player.
	 * @param material The Material that is being added to their list.
	 */
	public static void addCompressibleItem(UUID uuid, Material material) {
		List<Material> materials = new ArrayList<>();
		if (compressibleTypes.containsKey(uuid)) {
			materials = compressibleTypes.get(uuid);
		}
		// Avoid duplicate entry of materials
		if (materials.contains(material)) {
			return;
		}
		materials.add(material);
		compressibleTypes.put(uuid, materials);
	}

	/**
	 * Removes the material from the player's list of compressible items.
	 * @param uuid The UUID of the player.
	 * @param material The Material that is being removed to their list.
	 */
	public static void removeCompressibleItem(UUID uuid, Material material) {
		if (compressibleTypes.containsKey(uuid)) {
			List<Material> materials = compressibleTypes.get(uuid);
            materials.remove(material);
			compressibleTypes.put(uuid, materials);
		}
	}

	/**
	 * Determines if the Material is in the player's list of compressible items.
	 * @param uuid The UUID of the player.
	 * @param material The Material that is being verified.
	 */
	public static boolean isItemBeingCompressed(UUID uuid, Material material) {
		if (compressibleTypes.containsKey(uuid)) {
			if (compressibleTypes.get(uuid).contains(material)) {
				return true;
			}
		}
		return false;
	}

	/**
	 * Confirms if the input ItemStack is a compressible item.
	 * @param item The item that is being verified.
	 * @param shouldCheckItemMeta Whether the method should be checking itemMeta.
	 * @return Confirmation if the input item is compressible.
	 */
	public static boolean isCompressible(ItemStack item, boolean shouldCheckItemMeta) {
		if (shouldCheckItemMeta) {
			if (item.hasItemMeta()) {
				return false;
			}
		}

		Material type = item.getType();
		return type == Material.COAL || type ==  Material.RAW_COPPER || type ==  Material.COPPER_NUGGET
				|| type ==  Material.COPPER_INGOT || type == Material.RAW_IRON || type == Material.IRON_NUGGET
				|| type == Material.IRON_INGOT || type == Material.RAW_GOLD || type == Material.GOLD_NUGGET
				|| type == Material.GOLD_INGOT || type == Material.REDSTONE || type == Material.LAPIS_LAZULI
				|| type == Material.DIAMOND || type == Material.EMERALD || type == Material.NETHERITE_INGOT
				|| type == Material.AMETHYST_SHARD || type == Material.RESIN_CLUMP || type == Material.GLOWSTONE_DUST
				|| type == Material.WHEAT || type == Material.MELON_SLICE || type == Material.DRIED_KELP
				|| type == Material.SUGAR_CANE || type == Material.HONEYCOMB || type == Material.SLIME_BALL
				|| type == Material.BONE_MEAL || type == Material.SNOWBALL || type == Material.CLAY_BALL;
	}

	/**
	 * Enables the compressor for all Materials.
	 * @param uuid The player's UUID.
	 */
	public static void compressAllMaterials(UUID uuid) {
		addCompressibleItem(uuid, Material.COAL);
		addCompressibleItem(uuid, Material.RAW_COPPER);
		addCompressibleItem(uuid, Material.COPPER_NUGGET);
		addCompressibleItem(uuid, Material.COPPER_INGOT);
		addCompressibleItem(uuid, Material.RAW_IRON);
		addCompressibleItem(uuid, Material.IRON_NUGGET);
		addCompressibleItem(uuid, Material.IRON_INGOT);
		addCompressibleItem(uuid, Material.RAW_GOLD);
		addCompressibleItem(uuid, Material.GOLD_NUGGET);
		addCompressibleItem(uuid, Material.GOLD_INGOT);
		addCompressibleItem(uuid, Material.REDSTONE);
		addCompressibleItem(uuid, Material.LAPIS_LAZULI);
		addCompressibleItem(uuid, Material.DIAMOND);
		addCompressibleItem(uuid, Material.EMERALD);
		addCompressibleItem(uuid, Material.NETHERITE_INGOT);
		addCompressibleItem(uuid, Material.AMETHYST_SHARD);
		addCompressibleItem(uuid, Material.RESIN_CLUMP);
		addCompressibleItem(uuid, Material.GLOWSTONE_DUST);
		addCompressibleItem(uuid, Material.WHEAT);
		addCompressibleItem(uuid, Material.MELON_SLICE);
		addCompressibleItem(uuid, Material.DRIED_KELP);
		addCompressibleItem(uuid, Material.SUGAR_CANE);
		addCompressibleItem(uuid, Material.HONEYCOMB);
		addCompressibleItem(uuid, Material.SLIME_BALL);
		addCompressibleItem(uuid, Material.BONE_MEAL);
		addCompressibleItem(uuid, Material.SNOWBALL);
		addCompressibleItem(uuid, Material.CLAY_BALL);
	}

	/**
	 * Disables the compressor for all Materials.
	 * @param uuid The player's UUID.
	 */
	public static void stopCompressingAllMaterials(UUID uuid) {
		removeCompressibleItem(uuid, Material.COAL);
		removeCompressibleItem(uuid, Material.RAW_COPPER);
		removeCompressibleItem(uuid, Material.COPPER_NUGGET);
		removeCompressibleItem(uuid, Material.COPPER_INGOT);
		removeCompressibleItem(uuid, Material.RAW_IRON);
		removeCompressibleItem(uuid, Material.IRON_NUGGET);
		removeCompressibleItem(uuid, Material.IRON_INGOT);
		removeCompressibleItem(uuid, Material.RAW_GOLD);
		removeCompressibleItem(uuid, Material.GOLD_NUGGET);
		removeCompressibleItem(uuid, Material.GOLD_INGOT);
		removeCompressibleItem(uuid, Material.REDSTONE);
		removeCompressibleItem(uuid, Material.LAPIS_LAZULI);
		removeCompressibleItem(uuid, Material.DIAMOND);
		removeCompressibleItem(uuid, Material.EMERALD);
		removeCompressibleItem(uuid, Material.NETHERITE_INGOT);
		removeCompressibleItem(uuid, Material.AMETHYST_SHARD);
		removeCompressibleItem(uuid, Material.RESIN_CLUMP);
		removeCompressibleItem(uuid, Material.GLOWSTONE_DUST);
		removeCompressibleItem(uuid, Material.WHEAT);
		removeCompressibleItem(uuid, Material.MELON_SLICE);
		removeCompressibleItem(uuid, Material.DRIED_KELP);
		removeCompressibleItem(uuid, Material.SUGAR_CANE);
		removeCompressibleItem(uuid, Material.HONEYCOMB);
		removeCompressibleItem(uuid, Material.SLIME_BALL);
		removeCompressibleItem(uuid, Material.BONE_MEAL);
		removeCompressibleItem(uuid, Material.SNOWBALL);
		removeCompressibleItem(uuid, Material.CLAY_BALL);
	}

	/**
	 * Provides the list of crates that are currently in use.
	 * @return The list of crates that are currently in use.
	 */
	public static List<CrateType> getCratesInUse() {
		return cratesInUse;
	}

	/**
	 * Adds the crate to be in use.
	 * @param type The type of crate.
	 */
	public static void addCrateInUse(CrateType type) {
		cratesInUse.add(type);
	}

	/**
	 * Removes the crate from being in use.
	 * @param type The type of crate.
	 */
	public static void removeCrateFromUse(CrateType type) {
		cratesInUse.remove(type);
	}

	/**
	 * Updates the name, location, and/or icon of an existing warp.
	 *
	 * @param warpName The new name to be used for the warp.
	 * @param direction The Location containing the direction of the warp to be used.
	 * @param icon The Material that will be displayed for the warp.
	 */
	public static void updateWarp(String warpName, Location direction, Material icon) {
		for (int i = 0; i < warps.size(); i++) {
			if (warps.get(i).getLocation().getBlockX() == direction.getBlockX()
					&& warps.get(i).getLocation().getBlockY() == direction.getBlockY()
					&& warps.get(i).getLocation().getBlockZ() == direction.getBlockZ()) {
				if (!warps.get(i).getName().equals(warpName)) {
					Bukkit.getLogger().info(warpName);
					Bukkit.getLogger().info(warps.get(i).getName());
					continue;
				}

				Home updatedWarp = new Home(warpName, direction, icon);
				warps.set(i, updatedWarp);
			}
		}
	}

	/**
	 * Updates the name, location, and/or icon of an existing home.
	 *
	 * @param player The player updating the home.
	 * @param homeName The new name to be used for the home.
	 * @param direction The Location containing the direction of the home to be used.
	 * @param icon The Material that will be displayed for the home.
	 */
	public static void updateHome(Player player, String homeName, Location direction, Material icon) {
		AranarthPlayer aranarthPlayer = getPlayer(player.getUniqueId());
		List<Home> homes = aranarthPlayer.getHomes();
		for (int i = 0; i < homes.size(); i++) {
			if (homes.get(i).getLocation().getBlockX() == direction.getBlockX()
					&& homes.get(i).getLocation().getBlockY() == direction.getBlockY()
					&& homes.get(i).getLocation().getBlockZ() == direction.getBlockZ()) {

				// Prevents bug where incorrect home icon updates when they share the same location
				if (!homes.get(i).getName().equals(homeName)) {
					continue;
				}

				Home updatedHome = new Home(homeName, direction, icon);
				homes.set(i, updatedHome);
				aranarthPlayer.setHomes(homes);
				setPlayer(player.getUniqueId(), aranarthPlayer);
			}
		}
	}

	/**
	 * Removes all LockedContainers for players that have been inactive for 90 days.
	 */
	public static void removeInactiveLockedContainers() {
		List<Integer> toRemove = new ArrayList<>();
		LocalDateTime now = LocalDateTime.now();
		if (lockedContainers == null) {
			return;
		}

		for (int i = 0; i < lockedContainers.size(); i++) {
			LockedContainer locked = lockedContainers.get(i);
			OfflinePlayer player = Bukkit.getOfflinePlayer(locked.getOwner());
			LocalDateTime localDateTime = null;
			Instant lastPlayed = Instant.ofEpochMilli(player.getLastPlayed());
			localDateTime = LocalDateTime.ofInstant(lastPlayed, ZoneId.systemDefault());
			if (!player.isOnline() && localDateTime.isBefore(now.minusDays(90))) {
				toRemove.add(i);
			}
		}

		// Going in reverse to avoid deleting incorrect ones
		for (int i = lockedContainers.size() - 1; i > 0; i--) {
			if (toRemove.contains(i)) {
				lockedContainers.remove(i);
			}
		}
	}

	/**
	 * Provides the HashMap of Shop Locations.
	 * @return The HashMap of Shop Locations.
	 */
	public static HashMap<UUID, Location> getShopLocations() {
		return shopLocations;
	}

	/**
	 * Creates a new Shop Location that can be teleported to.
	 * @param uuid The shop owner's UUID.
	 * @param location The location of the shop's home.
	 */
	public static void createShopLocation(UUID uuid, Location location) {
		shopLocations.put(uuid, location);
	}

	/**
	 * Deletes an existing Shop Location.
	 * @param uuid The shop owner's UUID.
	 */
	public static void deleteShopLocation(UUID uuid) {
		shopLocations.remove(uuid);
	}

	/**
	 * Plays the jingle when a player sends or receives a teleport request.
	 * @param player The player that sent or received the request.
	 */
	public static void playTeleportSound(Player player) {
		Sound sound = Sound.BLOCK_NOTE_BLOCK_HARP;
		new BukkitRunnable() {
			int runs = 0;
			@Override
			public void run() {
				float pitch = 1.5F;

				switch (runs) {
					case 0 -> pitch = 1.5F;
					case 1 -> pitch = 1.25F;
					case 2 -> pitch = 1.5F;
					case 3 -> pitch = 2F;
					default -> {
						pitch = 0;
					}
				}

				// No sound
				if (pitch != 0) {
					player.playSound(player, sound, 1F, pitch);
				}

				if (runs == 5) {
					cancel();
				}
				runs++;
			}
		}.runTaskTimer(AranarthCore.getInstance(), 0, 3); // Runs every 3 ticks
	}

	/**
	 * Handles logic to update how the tab list displays
	 */
	public static void updateTab() {
		List<UUID> copy = new ArrayList<>();
		for (Player player : Bukkit.getOnlinePlayers()) {
			player.setPlayerListHeader(ChatUtils.translateToColor("&8&l---------------------\n&6&lThe Realm of Aranarth"));
			player.setPlayerListFooter(ChatUtils.translateToColor("&8&l---------------------"));
			copy.add(player.getUniqueId());
		}

		HashMap<UUID, String> displayedNames = new LinkedHashMap<>();
		int i = 0;
		int rank = 15;
		while (!copy.isEmpty()) {
			boolean wasAddedToList = false;
			UUID uuid = copy.get(i);
			AranarthPlayer aranarthPlayer = getPlayer(uuid);
			String display = ChatUtils.formatChatPrefix(Bukkit.getPlayer(uuid));
			display = display.substring(5, display.length() - 1);
			display = display.substring(0, display.length() - 7);

			if (rank == 15 && aranarthPlayer.getCouncilRank() == 3) {
				wasAddedToList = true;
			} else if (rank == 14 && aranarthPlayer.getCouncilRank() == 2) {
				wasAddedToList = true;
			} else if (rank == 13 && aranarthPlayer.getCouncilRank() == 1) {
				wasAddedToList = true;
			} else if (rank == 12 && aranarthPlayer.getArchitectRank() == 1) {
				wasAddedToList = true;
			} else if (rank == 11 && aranarthPlayer.getSaintRank() == 3) {
				wasAddedToList = true;
			} else if (rank == 10 && aranarthPlayer.getSaintRank() == 2) {
				wasAddedToList = true;
			} else if (rank == 9 && aranarthPlayer.getSaintRank() == 1) {
				wasAddedToList = true;
			} else if (rank == 8 && aranarthPlayer.getRank() == 8) {
				wasAddedToList = true;
			} else if (rank == 7 && aranarthPlayer.getRank() == 7) {
				wasAddedToList = true;
			} else if (rank == 6 && aranarthPlayer.getRank() == 6) {
				wasAddedToList = true;
			} else if (rank == 5 && aranarthPlayer.getRank() == 5) {
				wasAddedToList = true;
			} else if (rank == 4 && aranarthPlayer.getRank() == 4) {
				wasAddedToList = true;
			} else if (rank == 3 && aranarthPlayer.getRank() == 3) {
				wasAddedToList = true;
			} else if (rank == 2 && aranarthPlayer.getRank() == 2) {
				wasAddedToList = true;
			} else if (rank == 1 && aranarthPlayer.getRank() == 1) {
				wasAddedToList = true;
			} else {
				wasAddedToList = true;
			}

			// Only remove if the player was added to the list
			if (wasAddedToList) {
				displayedNames.put(uuid, ChatUtils.translateToColor(display));
				copy.remove(uuid);
			}

			// Iterate to next player or reset to first
			i++;
			if (i >= copy.size()) {
				i = 0;
				rank--;
			}
		}

		int j = 0;
		for (UUID uuid : displayedNames.keySet()) {
			Player player = Bukkit.getPlayer(uuid);
			player.setPlayerListName(displayedNames.get(uuid));
			player.setPlayerListOrder(j);
			j++;
		}
	}

}
