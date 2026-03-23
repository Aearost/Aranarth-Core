package com.aearost.aranarthcore.event.player;

import com.aearost.aranarthcore.objects.AranarthPlayer;
import com.aearost.aranarthcore.utils.AranarthUtils;
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

		if (!player.hasPermission("aranarth.inventory")) {
			return;
		}

		AranarthPlayer aranarthPlayer = AranarthUtils.getPlayer(player.getUniqueId());
		if (aranarthPlayer.isTogglingInventoryAssist()) {
			return;
		}

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
					if (!(itemStack.getItemMeta() instanceof BlockStateMeta)) {
						inventory.setItem(placedSlot, new ItemStack(contents[i]));
						inventory.setItem(i, null);
						player.updateInventory();
						return;
					}
				}

				// If that slot is a shulker box, cycle through it as well
				if (itemStack.getItemMeta() instanceof BlockStateMeta im) {
					if (im.getBlockState() instanceof ShulkerBox shulker) {
						if (player.hasPermission("aranarth.shulker")) {
							Inventory shulkerInventory = shulker.getInventory();
							ItemStack[] shulkerContents = shulkerInventory.getContents();
							for (int j = 0; j < shulkerInventory.getSize(); j++) {
								if (shulkerContents[j] != null) {
									if (shulkerContents[j].isSimilar(e.getItemInHand())) {
										inventory.setItem(placedSlot, new ItemStack(shulkerContents[j]));
										shulkerInventory.setItem(j, null);
										shulker.update();
										im.setBlockState(shulker);
										itemStack.setItemMeta(im);
										inventory.setItem(i, itemStack);
										contents[i].setItemMeta(im);
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

}
