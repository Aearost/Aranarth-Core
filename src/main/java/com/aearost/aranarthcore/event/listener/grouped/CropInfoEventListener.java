package com.aearost.aranarthcore.event.listener.grouped;

import com.aearost.aranarthcore.AranarthCore;
import com.aearost.aranarthcore.utils.CropUtils;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryMoveItemEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.ItemStack;

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
	 * Refreshes seeds in the container and in the player's own inventory.
	 */
	@EventHandler
	public void onInventoryOpen(InventoryOpenEvent e) {
		if (e.getPlayer() instanceof Player player) {
			CropUtils.refreshInventory(e.getInventory(), e.getPlayer().getWorld());
			CropUtils.refreshInventory(player.getInventory(), e.getPlayer().getWorld());
		}
	}

	/**
	 * Removes the seed lore when a player closes a non-player inventory.
	 */
	@EventHandler
	public void onInventoryClose(InventoryCloseEvent e) {
		if (e.getPlayer() instanceof Player player && e.getInventory().getType() != InventoryType.PLAYER) {
			ItemStack[] contents = e.getInventory().getContents();
			for (int i = 0; i < contents.length; i++) {
				ItemStack item = contents[i];
				if (item != null && CropUtils.isCropSeed(item.getType())) {
					ItemStack noLore = new ItemStack(item.getType());
					noLore.setAmount(item.getAmount());
					e.getInventory().setItem(i, noLore);
				}
			}
		}
	}

	/**
	 * Removes the seed lore when an item enters a hopper, allowing sorting systems to work.
	 */
	@EventHandler
	public void onHopperEnter(InventoryMoveItemEvent e) {
		if (e.getDestination().getType() == InventoryType.HOPPER) {
			if (CropUtils.isCropSeed(e.getItem().getType())) {
				ItemStack noLore = new ItemStack(e.getItem().getType());
				noLore.setAmount(e.getItem().getAmount());
				e.setItem(noLore);
			}
		}
	}
}

