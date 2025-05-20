package com.aearost.aranarthcore.event.player;

import org.bukkit.Bukkit;
import org.bukkit.block.ShulkerBox;
import org.bukkit.entity.HumanEntity;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BlockStateMeta;
import org.bukkit.inventory.meta.ItemMeta;

/**
 * Handles updating the shulker inventory when the GUI is closed.
 */
public class GuiShulkerClose {
	public void execute(InventoryCloseEvent e) {
		Inventory inventory = e.getInventory();
		HumanEntity player = e.getPlayer();
		ItemStack heldItem = e.getPlayer().getInventory().getItemInMainHand();
		ItemMeta meta = heldItem.hasItemMeta() ? heldItem.getItemMeta() : Bukkit.getItemFactory().getItemMeta(heldItem.getType());

		if (meta instanceof BlockStateMeta im) {
			if (im.getBlockState() instanceof ShulkerBox shulker) {
				shulker.getInventory().setContents(inventory.getContents());
				im.setBlockState(shulker);
				heldItem.setItemMeta(im);
				return;
			}
		}
	}
}
