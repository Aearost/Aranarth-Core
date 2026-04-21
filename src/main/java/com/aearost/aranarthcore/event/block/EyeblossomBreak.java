package com.aearost.aranarthcore.event.block;

import com.aearost.aranarthcore.AranarthCore;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.scheduler.BukkitRunnable;

/**
 * Removes a light block above open eyeblossoms when broken.
 */
public class EyeblossomBreak {

	public void execute(BlockBreakEvent e) {
		Location location = e.getBlock().getLocation();
		Location locationAbove = new Location(location.getWorld(), location.getX(), location.getY() + 1, location.getZ());

		// Breaking the flower
		if (location.getBlock().getType() == Material.OPEN_EYEBLOSSOM && locationAbove.getBlock().getType() == Material.LIGHT) {
			locationAbove.getBlock().setType(Material.BARRIER);
			new BukkitRunnable() {
				@Override
				public void run() {
					locationAbove.getBlock().setType(Material.AIR);
				}
			}.runTaskLater(AranarthCore.getInstance(), 1);
		}
		// Breaking the block under the flower
		else if (locationAbove.getBlock().getType() == Material.OPEN_EYEBLOSSOM) {
			Location locationAboveOpenEyeblossom = new Location(locationAbove.getWorld(), locationAbove.getX(), locationAbove.getY() + 1, locationAbove.getZ());
			locationAboveOpenEyeblossom.getBlock().setType(Material.BARRIER);
			new BukkitRunnable() {
				@Override
				public void run() {
					locationAboveOpenEyeblossom.getBlock().setType(Material.AIR);
				}
			}.runTaskLater(AranarthCore.getInstance(), 1);
		}
	}
}
