package com.aearost.aranarthcore.event.listener.grouped;

import com.aearost.aranarthcore.AranarthCore;
import com.aearost.aranarthcore.items.produce.Produce;
import com.aearost.aranarthcore.utils.CropUtils;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryMoveItemEvent;
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
	 * Removes the seed lore when a player closes a non-player inventory.
	 */
	@EventHandler
	public void onInventoryClose(InventoryCloseEvent e) {
		if (e.getPlayer() instanceof Player && e.getInventory().getType() != InventoryType.PLAYER) {
			ItemStack[] contents = e.getInventory().getContents();
			for (int i = 0; i < contents.length; i++) {
				ItemStack noLore = withoutLore(contents[i]);
				if (noLore != null) {
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
			ItemStack noLore = withoutLore(e.getItem());
			if (noLore != null) {
				e.setItem(noLore);
			}
		}
	}

	/**
	 * Provides the seed or produce without its seasonal lore.
	 * @param item The item.
	 * @return The item without lore, or null if the item never shows seasonal lore.
	 */
	private static ItemStack withoutLore(ItemStack item) {
		if (item == null) {
			return null;
		}
		if (Produce.getType(item) != null) {
			return CropUtils.hasSeasonalLore(item) ? Produce.withoutLore(item) : null;
		}
		if (!CropUtils.isCropSeed(item.getType())) {
			return null;
		}
		ItemStack noLore = new ItemStack(item.getType());
		noLore.setAmount(item.getAmount());
		return noLore;
	}
}

