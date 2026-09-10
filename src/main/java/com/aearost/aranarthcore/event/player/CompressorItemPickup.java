package com.aearost.aranarthcore.event.player;

import com.aearost.aranarthcore.objects.AranarthPlayer;
import com.aearost.aranarthcore.utils.AranarthUtils;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.inventory.ItemStack;

/**
 * Automatically compresses a player's entire inventory, and attempts to compress the picked up item as well.
 */
public class CompressorItemPickup {

	public void execute(EntityPickupItemEvent e) {
		if (e.getEntity() instanceof Player player) {
			if (!AranarthUtils.isSurvivalWorld(player.getWorld().getName())) {
				return;
			}

			if (!player.hasPermission("aranarth.compressor")) {
				return;
			}

			// Only attempts to compress if the item being picked up is compressible
			if (!AranarthUtils.isCompressible(e.getItem().getItemStack(), true)) {
				return;
			}

			AranarthPlayer aranarthPlayer = AranarthUtils.getPlayer(player.getUniqueId());
			if (!aranarthPlayer.isCompressingItems()) {
				return;
			}

			e.setCancelled(true);
			ItemStack pickupClone = e.getItem().getItemStack().clone();

			// Determine how to handle the pickup item and whether to include it in compression
			ItemStack additionalItem = null;
			if (AranarthUtils.isItemBeingCompressed(player.getUniqueId(), pickupClone.getType())) {
				int blacklistResult = AranarthUtils.getBlacklistMethod(player, aranarthPlayer, pickupClone);
				if (blacklistResult == 0) {
					// Consume the item but do not include it in compression
					e.getItem().setItemStack(null);
					e.getItem().remove();
				} else if (blacklistResult == 1) {
					// Do not pick up at all - item stays in the world
					e.setCancelled(true);
				} else {
					// Remove from world and include in compression
					e.getItem().setItemStack(null);
					additionalItem = pickupClone;
				}
			} else {
				// Not toggled for compression - pick up directly without compressing it
				e.getItem().setItemStack(null);
				e.getItem().remove();
				AranarthUtils.addCompressedResultsToInventory(player, pickupClone, true);
			}

			// Always compress the rest of the inventory
			AranarthUtils.compressPlayerInventory(player, aranarthPlayer, additionalItem);
		}
	}
}
