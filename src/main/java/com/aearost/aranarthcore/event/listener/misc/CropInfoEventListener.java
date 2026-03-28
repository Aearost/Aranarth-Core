package com.aearost.aranarthcore.event.listener.misc;

import com.aearost.aranarthcore.AranarthCore;
import com.aearost.aranarthcore.utils.CropUtils;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryOpenEvent;

/**
 * Updates crop seed item lore to show the current month's grow speed and yield
 * modifiers whenever seeds enter or become visible in a player's inventory.
 * This keeps the displayed values seamlessly current with the active month.
 */
public class CropInfoEventListener implements Listener {

	public CropInfoEventListener(AranarthCore plugin) {
		Bukkit.getPluginManager().registerEvents(this, plugin);
	}

	/**
	 * Fires when a player opens any external inventory (chest, barrel, etc.).
	 * Refreshes seeds in the container and in the player's own inventory.
	 */
	@EventHandler
	public void onInventoryOpen(InventoryOpenEvent e) {
		if (e.getPlayer() instanceof Player player) {
			CropUtils.refreshInventory(e.getInventory());
			CropUtils.refreshInventory(player.getInventory());
		}
	}

}

