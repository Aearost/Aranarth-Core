package com.aearost.aranarthcore.event.block;

import com.aearost.aranarthcore.utils.AranarthUtils;
import org.bukkit.event.block.BlockFadeEvent;

/**
 * Prevents coral blocks from drying out in the survival world.
 */
public class CoralDry {
	public void execute(BlockFadeEvent e) {
		if (e.getBlock().getWorld().getName().equalsIgnoreCase("world") ||
				AranarthUtils.isSmpWorld(e.getBlock().getWorld().getName()) ||
			e.getBlock().getWorld().getName().equalsIgnoreCase("creative")) {
			e.setCancelled(true);
		}
	}
}
