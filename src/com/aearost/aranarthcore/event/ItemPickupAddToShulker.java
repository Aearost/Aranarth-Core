package com.aearost.aranarthcore.event;

import org.bukkit.Bukkit;
import org.bukkit.block.ShulkerBox;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BlockStateMeta;

import com.aearost.aranarthcore.AranarthCore;

public class ItemPickupAddToShulker implements Listener {

	public ItemPickupAddToShulker(AranarthCore plugin) {
		Bukkit.getPluginManager().registerEvents(this, plugin);
	}

	/**
	 * Automatically places picked up items that have incomplete stacks in a shulker
	 * box into that stack.
	 * 
	 * @param e
	 */
	@EventHandler
	public void onItemPickup(final EntityPickupItemEvent e) {
		if (e.getEntity() instanceof Player) {
			Player player = (Player) e.getEntity();
			ItemStack pickupItem = e.getItem().getItemStack();

			// Disregard if the item is a shulker box
//			if (e.getItem().getItemStack().getItemMeta() instanceof BlockStateMeta) {
//				BlockStateMeta meta = (BlockStateMeta) e.getItem().getItemStack().getItemMeta();
//				if (meta.getBlockState() instanceof ShulkerBox) {
//					return;
//				}
//			}
			
			ItemStack[] inventory = player.getInventory().getStorageContents();
			for (ItemStack is : inventory) {
				// Skip the slot if it's empty
				if (is == null) {
					continue;
				}				
				
				if (is.getItemMeta() instanceof BlockStateMeta) {
					BlockStateMeta im = (BlockStateMeta) is.getItemMeta();
					if (im.getBlockState() instanceof ShulkerBox) {
						ShulkerBox shulker = (ShulkerBox) im.getBlockState();
						for (ItemStack shulkerStack : shulker.getInventory().getContents()) {
							// Skips the slot in the shulker box if it's empty
							if (shulkerStack == null) {
								continue;
							} else {
								if (shulkerStack.isSimilar(pickupItem)) {
									// Logic to add to the shulker box stack
									
									return;
								}
							}
						}
					}
				}
			}

		}
	}

}
