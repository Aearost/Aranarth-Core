package com.aearost.aranarthcore.event.block;

import com.aearost.aranarthcore.AranarthCore;
import com.aearost.aranarthcore.enums.Month;
import com.aearost.aranarthcore.utils.AranarthUtils;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.block.data.type.Leaves;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.inventory.ItemStack;

import java.util.Random;

public class LogExtraDrops implements Listener {

	public LogExtraDrops(AranarthCore plugin) {
		Bukkit.getPluginManager().registerEvents(this, plugin);
	}

	/**
	 * Drops extra logs during the month of Follivor.
	 * @param e The event.
	 */
	@EventHandler
	public void onLogDestroy(final BlockBreakEvent e) {
		if (AranarthUtils.getMonth() == Month.FOLLIVOR) {
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
}
