package com.aearost.aranarthcore.event.block;

import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.block.data.type.Leaves;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.inventory.ItemStack;

import java.util.Random;

/**
 * Drops extra logs during the month of Follivor.
 */
public class LogExtraDrops {
	public void execute(BlockBreakEvent e) {
		if (e.getBlock().getType().name().endsWith("_LOG") && !e.getBlock().getType().name().endsWith("_STRIPPED_LOG")) {
			Location loc = e.getBlock().getLocation();
			// Circles through blocks until a leaf is reached
			for (int y = loc.getBlockY() + 1; y < 255; y++) {
				Block block = e.getBlock().getWorld().getBlockAt(loc.getBlockX(), y, loc.getBlockZ());
				if (block.getBlockData() instanceof Leaves leaves) {
					// Only has a chance to double if natural leaves
					if (!leaves.isPersistent()) {
						// 20% chance of logs doubling
						if (new Random().nextInt(5) == 0) {
							e.getBlock().getWorld().dropItemNaturally(e.getBlock().getLocation(), new ItemStack(e.getBlock().getType()));
						}
					}
					return;
				}
			}
		}
	}
}
