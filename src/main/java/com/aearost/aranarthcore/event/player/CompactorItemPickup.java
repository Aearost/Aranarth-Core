package com.aearost.aranarthcore.event.player;

import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.inventory.ItemStack;

/**
 * Automatically compacts picked up items into their block form.
 */
public class CompactorItemPickup {

	public void execute(EntityPickupItemEvent e) {
		Player player = (Player) e.getEntity();
		if (!player.hasPermission("aranarth.compact")) {
			return;
		}

		ItemStack pickupItem = e.getItem().getItemStack();
//		if (pickupItem.getType())

		// Erase the item that's picked up
		// Cancel the event
		// Then add the compressed item into the inventory
		// Then the shulker item pickup will only register the compressed item and not the original

	}
}
