package com.aearost.aranarthcore.utils;

import com.aearost.aranarthcore.AranarthCore;
import com.aearost.aranarthcore.event.listener.misc.InvisibilityListener;
import com.aearost.aranarthcore.objects.AranarthPlayer;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;

import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Gives players their creative inventory while they are in Creative mode in a Survival world, so items can never
 * cross between Creative mode and Survival.
 */
public class CreativeSwapUtils {

	private static final Set<UUID> swappedPlayers = ConcurrentHashMap.newKeySet();
	private static final Set<UUID> switchingWorlds = ConcurrentHashMap.newKeySet();
	private static final Set<UUID> quietPlayers = ConcurrentHashMap.newKeySet();
	private static final NamespacedKey SWAPPED_KEY = new NamespacedKey(AranarthCore.getInstance(), "creative_inventory_swapped");

	/**
	 * Provides whether the player is currently holding their creative inventory in a Survival world.
	 *
	 * @param uuid The player's UUID.
	 * @return Confirmation of whether the creative inventory is swapped in.
	 */
	public static boolean isSwapped(UUID uuid) {
		return swappedPlayers.contains(uuid);
	}

	/**
	 * Marks the player as having their inventory switched between worlds.
	 *
	 * @param uuid The player's UUID.
	 * @return Whether this call started the switch, in which case it must also end it.
	 */
	public static boolean beginWorldSwitch(UUID uuid) {
		return switchingWorlds.add(uuid);
	}

	/**
	 * Ends the switch started by beginWorldSwitch().
	 *
	 * @param uuid The player's UUID.
	 */
	public static void endWorldSwitch(UUID uuid) {
		switchingWorlds.remove(uuid);
	}

	/**
	 * Provides whether the player's inventory is being switched between worlds.
	 *
	 * @param uuid The player's UUID.
	 * @return Confirmation of whether the inventory is being switched.
	 */
	public static boolean isSwitchingWorlds(UUID uuid) {
		return switchingWorlds.contains(uuid);
	}

	/**
	 * Runs the task without telling the player about their inventory being swapped.
	 *
	 * @param uuid The player's UUID.
	 * @param task The task.
	 */
	public static void runQuietly(UUID uuid, Runnable task) {
		boolean started = quietPlayers.add(uuid);
		try {
			task.run();
		} finally {
			if (started) {
				quietPlayers.remove(uuid);
			}
		}
	}

	/**
	 * Provides whether the player is not told about their inventory being swapped.
	 *
	 * @param uuid The player's UUID.
	 * @return Confirmation of whether the player is not notified.
	 */
	public static boolean isQuiet(UUID uuid) {
		return quietPlayers.contains(uuid);
	}

	/**
	 * Keeps the creative inventory of a swapped player current, so little is lost if the server stops without them leaving.
	 *
	 * @param player The player.
	 */
	public static void snapshotCreativeInventory(Player player) {
		if (!isSwapped(player.getUniqueId())) {
			return;
		}
		AranarthPlayer aranarthPlayer = AranarthUtils.getPlayer(player.getUniqueId());
		if (aranarthPlayer != null) {
			aranarthPlayer.setCreativeInventory(ItemUtils.itemStackArrayToBase64(player.getInventory().getContents()));
		}
	}

	/**
	 * Provides the player's survival inventory to be saved, which is the stored one while their creative inventory is swapped in.
	 *
	 * @param player The player.
	 * @return The serialized survival inventory.
	 */
	public static String getSurvivalInventory(Player player) {
		AranarthPlayer aranarthPlayer = AranarthUtils.getPlayer(player.getUniqueId());
		if (aranarthPlayer != null && isSwapped(player.getUniqueId())) {
			return aranarthPlayer.getSurvivalInventory();
		}
		return ItemUtils.itemStackArrayToBase64(player.getInventory().getContents());
	}

	/**
	 * Stores the player's survival inventory and gives them their creative inventory.
	 *
	 * @param player The player.
	 * @return Confirmation of whether the creative inventory is now swapped in.
	 */
	public static boolean swapToCreative(Player player) {
		UUID uuid = player.getUniqueId();
		if (isSwapped(uuid)) {
			return true;
		}
		AranarthPlayer aranarthPlayer = AranarthUtils.getPlayer(uuid);
		if (aranarthPlayer == null) {
			return false;
		}
		try {
			ItemStack[] creativeInventory = aranarthPlayer.getCreativeInventory().isEmpty()
					? null : ItemUtils.itemStackArrayFromBase64(aranarthPlayer.getCreativeInventory());

			// An open trade is cancelled so the offered items are back in the survival inventory
			TradeManager.handleQuit(player);
			// The 2x2 crafting grid and the cursor are not part of the inventory contents
			player.closeInventory();
			InvisibilityListener.restoreArmorBeforeLeaving(player, player.getWorld().getName());
			aranarthPlayer.setSurvivalInventory(ItemUtils.itemStackArrayToBase64(player.getInventory().getContents()));
			AranarthUtils.setPlayer(uuid, aranarthPlayer);
			// The survival inventory only exists in the AranarthPlayer from now on, so it must be durable right away
			PersistenceUtils.saveAranarthPlayerImmediately(uuid);

			if (creativeInventory != null) {
				player.getInventory().setContents(creativeInventory);
			} else {
				player.getInventory().clear();
			}
			swappedPlayers.add(uuid);
			player.getPersistentDataContainer().set(SWAPPED_KEY, PersistentDataType.BOOLEAN, true);
			return true;
		} catch (Exception e) {
			Bukkit.getLogger().warning(AranarthCore.LOG_PREFIX + "[Inv] Failed to swap " + player.getName()
					+ " to their creative inventory: " + e.getMessage());
			return false;
		}
	}

	/**
	 * Stores the player's creative inventory and gives them back their survival inventory.
	 *
	 * @param player The player.
	 * @return Confirmation of whether the survival inventory is now back.
	 */
	public static boolean swapToSurvival(Player player) {
		UUID uuid = player.getUniqueId();
		if (!isSwapped(uuid)) {
			return true;
		}
		AranarthPlayer aranarthPlayer = AranarthUtils.getPlayer(uuid);
		if (aranarthPlayer == null) {
			return false;
		}
		try {
			ItemStack[] survivalInventory = ItemUtils.itemStackArrayFromBase64(aranarthPlayer.getSurvivalInventory());

			player.closeInventory();
			aranarthPlayer.setCreativeInventory(ItemUtils.itemStackArrayToBase64(player.getInventory().getContents()));
			player.getInventory().setContents(survivalInventory);
			AranarthUtils.setPlayer(uuid, aranarthPlayer);
			swappedPlayers.remove(uuid);
			player.getPersistentDataContainer().remove(SWAPPED_KEY);
			return true;
		} catch (Exception e) {
			Bukkit.getLogger().warning(AranarthCore.LOG_PREFIX + "[Inv] Failed to swap " + player.getName()
					+ " back to their survival inventory: " + e.getMessage());
			return false;
		}
	}

	/**
	 * Gives the player back their survival inventory and Survival mode before they leave the Survival worlds or the
	 * server, so their survival inventory is the one that gets saved.
	 *
	 * @param player The player.
	 */
	public static void restoreBeforeLeaving(Player player) {
		if (!isSwapped(player.getUniqueId())) {
			return;
		}
		if (swapToSurvival(player)) {
			player.setGameMode(GameMode.SURVIVAL);
		}
	}

	/**
	 * Recovers a player who was holding their creative inventory when the server stopped without them leaving.
	 *
	 * @param player The player who joined.
	 */
	public static void recoverOnJoin(Player player) {
		if (!player.getPersistentDataContainer().has(SWAPPED_KEY)) {
			return;
		}
		Bukkit.getLogger().info(AranarthCore.LOG_PREFIX + "[Inv] " + player.getName()
				+ " joined while still holding their creative inventory - restoring their survival inventory");
		player.getPersistentDataContainer().remove(SWAPPED_KEY);
		// The creative items in the player data may be older than their creative inventory, which they could have
		// changed in a creative world since, so they are discarded rather than saved over it
		AranarthPlayer aranarthPlayer = AranarthUtils.getPlayer(player.getUniqueId());
		ItemStack[] survivalInventory = null;
		if (aranarthPlayer != null && !aranarthPlayer.getSurvivalInventory().isEmpty()) {
			try {
				survivalInventory = ItemUtils.itemStackArrayFromBase64(aranarthPlayer.getSurvivalInventory());
			} catch (Exception e) {
				Bukkit.getLogger().warning(AranarthCore.LOG_PREFIX + "[Inv] Failed to read the survival inventory of "
						+ player.getName() + " while recovering from Creative mode: " + e.getMessage());
			}
		}
		// The creative items must never stay in Survival, even if the survival inventory could not be read
		if (survivalInventory != null) {
			player.getInventory().setContents(survivalInventory);
		} else {
			player.getInventory().clear();
		}
		player.setGameMode(GameMode.SURVIVAL);
	}
}
