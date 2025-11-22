package com.aearost.aranarthcore.event.player;

import com.aearost.aranarthcore.objects.AranarthPlayer;
import com.aearost.aranarthcore.utils.AranarthUtils;
import org.bukkit.Sound;
import org.bukkit.block.ShulkerBox;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BlockStateMeta;

import java.util.Objects;

/**
 * Automatically places picked up items that have incomplete stacks in a shulker
 * box into that stack. This only works when you have at least 1 free inventory slot.
 */
public class ShulkerItemPickup {

	public void execute(EntityPickupItemEvent e) {
		Player player = (Player) e.getEntity();
		ItemStack pickupItem = e.getItem().getItemStack();
		AranarthPlayer aranarthPlayer = AranarthUtils.getPlayer(player.getUniqueId());
		if (Objects.nonNull(aranarthPlayer.getBlacklist())) {
			for (ItemStack blacklistedItem : aranarthPlayer.getBlacklist()) {
				if (pickupItem.getType() == blacklistedItem.getType()) {
					if (player.hasPermission("aranarth.blacklist")) {
						return;
					}
				}
			}
		}

		int amountRemaining = pickupItem.getAmount();

		// Skip the logic if the item being picked up is a shulker box
		if (pickupItem.getItemMeta() instanceof BlockStateMeta im) {
			if (im.getBlockState() instanceof ShulkerBox) {
				return;
			}
		}

		ItemStack[] inventory = player.getInventory().getStorageContents();
		for (ItemStack is : inventory) {
			// Skip the slot if it's empty
			if (is != null) {
				if (is.getItemMeta() instanceof BlockStateMeta im) {
					if (im.getBlockState() instanceof ShulkerBox shulker) {
						if (!aranarthPlayer.getIsAddingToShulker()) {
							return;
						}

						if ((player.getOpenInventory().getTitle()).equals("Shulker") && player.getOpenInventory().getType() == InventoryType.CHEST) {
							e.setCancelled(true);
							return;
						}

						if (!player.hasPermission("aranarth.shulker")) {
							return;
						}

						Inventory shulkerInventory = shulker.getInventory();

						// Cycle through all slots in slots that are shulker boxes
						for (int shulkerSlot = 0; shulkerSlot < shulkerInventory
								.getContents().length; shulkerSlot++) {
							ItemStack shulkerStack = shulkerInventory.getContents()[shulkerSlot];
							// Skips the slot in the shulker box if it's empty
							if (shulkerStack != null) {
								if (shulkerStack.isSimilar(pickupItem)) {
									// Logic to add to the shulker box stack
									while (amountRemaining > 0) {
										// Fill up an empty stack until it's full while removing one amount each
										// iteration
										if (shulkerStack.getAmount() < shulkerStack.getMaxStackSize()) {
											shulkerStack.setAmount(shulkerStack.getAmount() + 1);
											amountRemaining--;
											e.getItem().getItemStack().setAmount(pickupItem.getAmount() - 1);
										} else {
											break;
										}
									}
									// Prevents the default pickup behaviour and
									e.setCancelled(true);
									e.getItem().remove();
									shulkerInventory.setItem(shulkerSlot, shulkerStack);
									im.setBlockState(shulker);
									is.setItemMeta(im);
								}
							}
						}
					}
				}
			}
		}

		// If there was quantity put in a shulker box and quantity remains
		if (e.isCancelled()) {
			player.playSound(player.getLocation(), Sound.ENTITY_ITEM_PICKUP, 0.2F, 2F);
			pickupItem.setAmount(amountRemaining);
			player.getInventory().addItem(pickupItem);
		}
	}
}
