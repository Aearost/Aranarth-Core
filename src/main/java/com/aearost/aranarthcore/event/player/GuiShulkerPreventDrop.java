package com.aearost.aranarthcore.event.player;

import org.bukkit.Bukkit;
import org.bukkit.block.ShulkerBox;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.meta.BlockStateMeta;
import org.bukkit.inventory.meta.ItemMeta;

/**
 * Prevents the player from dropping the held shulker box when in an inventory
 */
public class GuiShulkerPreventDrop {
	public void execute(InventoryClickEvent e) {
		Inventory inventory = e.getInventory();
		if (e.getCurrentItem() != null) {
			ItemMeta meta = e.getCurrentItem().hasItemMeta() ? e.getCurrentItem().getItemMeta() : Bukkit.getItemFactory().getItemMeta(e.getCurrentItem().getType());
			if (meta instanceof BlockStateMeta im) {
				if (im.getBlockState() instanceof ShulkerBox) {
					e.setCancelled(true);
				}
			}
		}
	}
}
