package com.aearost.aranarthcore.event.player;

import org.bukkit.block.ShulkerBox;
import org.bukkit.entity.Player;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.BlockStateMeta;

/**
 * Provides functionality to automatically replenish a player's slot with a stack from their inventory.
 */
public class PlayerAutoReplenishSlot {
	public void execute(BlockPlaceEvent e) {
		Player player = e.getPlayer();
		if (e.getItemInHand().getAmount() - 1 == 0) {
			PlayerInventory inventory = player.getInventory();
			ItemStack[] contents = inventory.getContents();
			int placedSlot = 0;
			if (e.getHand() == EquipmentSlot.HAND) {
				placedSlot = inventory.getHeldItemSlot();
			} else {
				placedSlot = 40; // Hardcoded value of off-hand slot
			}

			// Searches the player's inventory for another stack of the item
			for (int i = 0; i < inventory.getSize(); i++) {
				ItemStack itemStack = contents[i];
				if (itemStack == null) {
					continue;
				}

				// If the slot is another one of the same item, switch it
				if (i != placedSlot && itemStack.isSimilar(e.getItemInHand())) {
					contents[placedSlot] = contents[i].clone();
					contents[i].setAmount(0);
					inventory.setContents(contents);
					player.updateInventory();
					return;
				}

				// If that slot is a shulker box, cycle through it as well
				if (itemStack.getItemMeta() instanceof BlockStateMeta im) {
					if (im.getBlockState() instanceof ShulkerBox shulker) {
						Inventory shulkerInventory = shulker.getInventory();
						ItemStack[] shulkerContents = shulkerInventory.getContents();
						for (int j = 0; j < shulkerInventory.getSize(); j++) {
							if (shulkerContents[j] != null) {
								if (shulkerContents[j].isSimilar(e.getItemInHand())) {
									contents[placedSlot] = shulkerContents[j].clone();
									shulkerContents[j].setAmount(0);
									shulkerInventory.setContents(shulkerContents);
									im.setBlockState(shulker);
									contents[i].setItemMeta(im);
									player.getInventory().setContents(contents);
									player.updateInventory();
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
