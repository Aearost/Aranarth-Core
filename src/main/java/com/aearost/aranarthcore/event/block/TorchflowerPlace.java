package com.aearost.aranarthcore.event.block;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.inventory.ItemStack;

/**
 * Adds a light block above the torchflower when placed so the plant emits light.
 */
public class TorchflowerPlace {

	public void execute(BlockPlaceEvent e) {
		ItemStack item = e.getItemInHand();
		if (item.getType() == Material.TORCHFLOWER) {
			Location location = e.getBlockPlaced().getLocation();
			Location locationAbove = new Location(location.getWorld(), location.getX(), location.getY() + 1, location.getZ());
			if (locationAbove.getBlock().getType() == Material.AIR) {
				locationAbove.getBlock().setType(Material.LIGHT);
			}
		}
	}
}
