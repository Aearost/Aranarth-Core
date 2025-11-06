package com.aearost.aranarthcore.event.world;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.event.block.BlockSpreadEvent;

/**
 * Prevents fire from spreading in Spawn.
 */
public class SpawnFireSpreadPrevent {
	public void execute(BlockSpreadEvent e) {
		if (e.getBlock().getType() == Material.FIRE) {
			Bukkit.getLogger().info("FIRE CANCELLED");
			e.setCancelled(true);
		}
	}
}
