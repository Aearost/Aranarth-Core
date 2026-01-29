package com.aearost.aranarthcore.event.block;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.event.block.BlockBreakEvent;

/**
 * Removes the light block above the torchflower.
 */
public class TorchflowerBreak {

	public void execute(BlockBreakEvent e) {
		Location location = e.getBlock().getLocation();
		Location locationAbove = new Location(location.getWorld(), location.getX(), location.getY() + 1, location.getZ());

		// Breaking the flower
		if (location.getBlock().getType() == Material.TORCHFLOWER && locationAbove.getBlock().getType() == Material.LIGHT) {
			locationAbove.getBlock().setType(Material.AIR);
		}
		// Breaking the block under the flower
		else if (locationAbove.getBlock().getType() == Material.TORCHFLOWER) {
			Location locationAboveTorchflower = new Location(locationAbove.getWorld(), locationAbove.getX(), locationAbove.getY() + 1, locationAbove.getZ());
			locationAboveTorchflower.getBlock().setType(Material.AIR);
		}
	}

}
