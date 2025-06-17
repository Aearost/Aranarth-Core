package com.aearost.aranarthcore.event.player;

import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.meta.ItemMeta;

import static com.aearost.aranarthcore.items.CustomItemKeys.ARANARTHIUM_INGOT;
import static com.aearost.aranarthcore.items.CustomItemKeys.ARMOR_TYPE;

/**
 * Puts items in the player's inventory if they were in the Enhanced Aranarthium anvil GUI.
 */
public class GuiEnhancedAranarthiumClose {
	public void execute(InventoryCloseEvent e) {
		Inventory inventory = e.getInventory();
		if (inventory.getItem(0) != null) {
			if (inventory.getItem(0).hasItemMeta()) {
				ItemMeta meta = inventory.getItem(0).getItemMeta();
				if (meta.getPersistentDataContainer().has(ARMOR_TYPE)) {
					e.getPlayer().getWorld().dropItemNaturally(e.getPlayer().getLocation(), inventory.getItem(0));
				}
				else if (meta.getPersistentDataContainer().has(ARANARTHIUM_INGOT)) {
					e.getPlayer().getWorld().dropItemNaturally(e.getPlayer().getLocation(), inventory.getItem(0));
				}
			}
		} else if (inventory.getItem(1) != null) {
			if (inventory.getItem(1).hasItemMeta()) {
				ItemMeta meta = inventory.getItem(1).getItemMeta();
				if (meta.getPersistentDataContainer().has(ARMOR_TYPE)) {
					e.getPlayer().getWorld().dropItemNaturally(e.getPlayer().getLocation(), inventory.getItem(1));
				}
				else if (meta.getPersistentDataContainer().has(ARANARTHIUM_INGOT)) {
					e.getPlayer().getInventory().addItem(inventory.getItem(1));
					e.getPlayer().getWorld().dropItemNaturally(e.getPlayer().getLocation(), inventory.getItem(1));
				}
			}
		}
	}
}
