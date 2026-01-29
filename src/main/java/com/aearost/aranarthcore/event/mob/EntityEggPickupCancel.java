package com.aearost.aranarthcore.event.mob;

import org.bukkit.Material;
import org.bukkit.event.entity.EntityPickupItemEvent;

/**
 * Deals with cancelling zombies, baby zombies, and zombie villagers from
 * picking up eggs which prevented despawning.
 */
public class EntityEggPickupCancel {

	public void execute(EntityPickupItemEvent e) {
		if (e.getItem().getItemStack().getType() == Material.EGG) {
			e.setCancelled(true);
		}
	}

}
