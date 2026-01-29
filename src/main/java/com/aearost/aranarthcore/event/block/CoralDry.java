package com.aearost.aranarthcore.event.block;

import org.bukkit.event.block.BlockFadeEvent;

/**
 * Prevents coral blocks from drying out in the survival world.
 */
public class CoralDry {
	public void execute(BlockFadeEvent e) {
		if (e.getBlock().getWorld().getName().equalsIgnoreCase("world") ||
				e.getBlock().getWorld().getName().equalsIgnoreCase("smp") ||
			e.getBlock().getWorld().getName().equalsIgnoreCase("creative")) {
			e.setCancelled(true);
		}
	}
}
