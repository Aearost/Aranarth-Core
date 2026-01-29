package com.aearost.aranarthcore.event.world;

import org.bukkit.Material;
import org.bukkit.event.entity.ItemSpawnEvent;

/**
 * Prevents all items from being dropped in the arena world.
 */
public class ArenaItemDrops {

	public void execute(ItemSpawnEvent e) {
		if (e.getEntity().getItemStack().getType() != Material.IRON_INGOT
				&& e.getEntity().getItemStack().getType() != Material.ARROW) {
			e.setCancelled(true);
		}
	}

}
