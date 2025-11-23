package com.aearost.aranarthcore.event.world;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.event.block.BlockFormEvent;

/**
 * Prevents blocks in the path up to the Stashenleda entrance from being snowed on.
 */
public class StashenledaPathSnowPrevent {
	public void execute(BlockFormEvent e) {
		Material material = e.getBlock().getType();
		if (material == Material.SNOW || material == Material.SNOW_BLOCK) {
			Location loc = e.getBlock().getLocation();
			if (loc.getWorld().getName().equals("smp")) {
				int x = loc.getBlockX();
				int y = loc.getBlockY();
				int z = loc.getBlockZ();
				if (x <= -44 && x >= -70) {
					if (y <= 157 && y >= 112) {
						if (z <= -97 && z >= -158) {
							e.setCancelled(true);
						}
					}
				}
			}

		}
	}
}
