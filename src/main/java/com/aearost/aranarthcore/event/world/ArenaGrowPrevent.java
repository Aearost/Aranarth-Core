package com.aearost.aranarthcore.event.world;

import org.bukkit.Material;
import org.bukkit.event.block.BlockGrowEvent;

/**
 * Prevents blocks in the arena world from growing.
 */
public class ArenaGrowPrevent {
	public void execute(BlockGrowEvent e) {
		Material material = e.getBlock().getType();
		if (material == Material.VINE || material == Material.CAVE_VINES_PLANT) {
			e.setCancelled(true);
		}
	}
}
