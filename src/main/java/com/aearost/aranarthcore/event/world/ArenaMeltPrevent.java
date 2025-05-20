package com.aearost.aranarthcore.event.world;

import org.bukkit.Material;
import org.bukkit.event.block.BlockFadeEvent;

/**
 * Prevents blocks in the arena world from fading.
 */
public class ArenaMeltPrevent {
	public void execute(BlockFadeEvent e) {
		Material material = e.getBlock().getType();
		if (material == Material.ICE || material == Material.PACKED_ICE || material == Material.BLUE_ICE ||
				material == Material.SNOW || material == Material.SNOW_BLOCK) {
			e.setCancelled(true);
		}
	}
}
