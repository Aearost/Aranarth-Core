package com.aearost.aranarthcore.event.listener.misc;

import com.aearost.aranarthcore.AranarthCore;
import com.aearost.aranarthcore.utils.AranarthUtils;
import com.aearost.aranarthcore.utils.ChatUtils;
import com.aearost.aranarthcore.utils.CreativeSwapUtils;
import com.aearost.aranarthcore.utils.Lang;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerGameModeChangeEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

/**
 * Swaps to the creative inventory when entering Creative mode in a Survival world, and back when leaving it.
 */
public class CreativeInventorySwapListener implements Listener {

	public CreativeInventorySwapListener(AranarthCore plugin) {
		Bukkit.getPluginManager().registerEvents(this, plugin);
	}

	/**
	 * Swaps the player's inventory whenever their game mode changes, whether by /gamemode, /gmc or a plugin.
	 * @param e The event.
	 */
	@EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
	public void onGameModeChange(PlayerGameModeChangeEvent e) {
		Player player = e.getPlayer();
		GameMode to = e.getNewGameMode();
		boolean isSwapped = CreativeSwapUtils.isSwapped(player.getUniqueId());

		boolean isQuiet = CreativeSwapUtils.isQuiet(player.getUniqueId());

		if (to == GameMode.CREATIVE) {
			// The creative worlds already have their own inventory
			if (isSwapped || CreativeSwapUtils.isSwitchingWorlds(player.getUniqueId())
					|| !AranarthUtils.getInventoryGroup(player.getWorld().getName()).equals("world")) {
				return;
			}
			if (CreativeSwapUtils.swapToCreative(player)) {
				if (!isQuiet) {
					player.sendMessage(ChatUtils.chatMessage(Lang.getFor(player, "gamemode.creative_inventory")));
				}
			} else {
				e.setCancelled(true);
				player.sendMessage(ChatUtils.chatMessage(Lang.getFor(player, "gamemode.swap_failed")));
			}
		} else if (isSwapped && (to == GameMode.SURVIVAL || to == GameMode.ADVENTURE)) {
			if (CreativeSwapUtils.swapToSurvival(player)) {
				if (!isQuiet) {
					player.sendMessage(ChatUtils.chatMessage(Lang.getFor(player, "gamemode.survival_inventory")));
				}
			} else {
				e.setCancelled(true);
				player.sendMessage(ChatUtils.chatMessage(Lang.getFor(player, "gamemode.swap_failed")));
			}
		}
	}

	/**
	 * Gives back the survival inventory of a player whose server stopped while they held their creative inventory.
	 * @param e The event.
	 */
	@EventHandler(priority = EventPriority.LOWEST)
	public void onJoin(PlayerJoinEvent e) {
		CreativeSwapUtils.recoverOnJoin(e.getPlayer());
	}

	/**
	 * Gives back the survival inventory as soon as the player arrives in a world with another inventory, in case
	 * the teleport that brought them there does not switch the inventory itself.
	 * @param e The event.
	 */
	@EventHandler(priority = EventPriority.LOWEST)
	public void onChangedWorld(PlayerChangedWorldEvent e) {
		Player player = e.getPlayer();
		if (!AranarthUtils.getInventoryGroup(player.getWorld().getName()).equals("world")) {
			CreativeSwapUtils.restoreBeforeLeaving(player);
		}
	}

	/**
	 * Gives back the survival inventory before the quit handlers save it.
	 * @param e The event.
	 */
	@EventHandler(priority = EventPriority.LOWEST)
	public void onQuit(PlayerQuitEvent e) {
		CreativeSwapUtils.restoreBeforeLeaving(e.getPlayer());
	}

	/**
	 * Prevents any ender chest from being opened in Creative mode, whether from a block, /ec or a Council tool.
	 * @param e The event.
	 */
	@EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
	public void onEnderChestOpen(InventoryOpenEvent e) {
		if (e.getInventory().getType() == InventoryType.ENDER_CHEST && e.getPlayer().getGameMode() == GameMode.CREATIVE) {
			e.setCancelled(true);
			e.getPlayer().sendMessage(ChatUtils.chatMessage(Lang.getFor(e.getPlayer(), "access.cannot_ender_creative")));
		}
	}

	/**
	 * Prevents the creative inventory from being dropped into a Survival world, i.e from /kill or the void.
	 * @param e The event.
	 */
	@EventHandler(priority = EventPriority.LOWEST)
	public void onDeath(PlayerDeathEvent e) {
		if (CreativeSwapUtils.isSwapped(e.getEntity().getUniqueId())) {
			e.getDrops().clear();
			e.setKeepInventory(true);
		}
	}
}
