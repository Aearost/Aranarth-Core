package com.aearost.aranarthcore.event.block;

import com.aearost.aranarthcore.AranarthCore;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.data.Ageable;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.inventory.ItemStack;

import java.util.Random;

public class PitcherPlantBreak implements Listener {

	public PitcherPlantBreak(AranarthCore plugin) {
		Bukkit.getPluginManager().registerEvents(this, plugin);
	}

	/**
	 * Drops the pitcher plant pods when the plant is harvested.
	 * @param e The event.
	 */
	@EventHandler
	public void onPitcherPlantBreak(final BlockBreakEvent e) {
		// For breaking the pitcher plant crop
		Location location = e.getBlock().getLocation();
		if (location.getBlock().getType() == Material.PITCHER_CROP) {
			Ageable pitcherPlant = (Ageable) location.getBlock().getBlockData();
			if (pitcherPlant.getAge() == pitcherPlant.getMaximumAge()) {
				Random r = new Random();
				// Will randomly select 0, 1, or 2 seeds
				int amountOfSeeds = r.nextInt(2);
				if (amountOfSeeds > 0) {
					location.getWorld().dropItemNaturally(location, new ItemStack(Material.PITCHER_POD, amountOfSeeds));
				}
			}
		}
		
		// For breaking a manually planted pitcher plant
		Location locationAbove = new Location(location.getWorld(), location.getX(), location.getY() + 1, location.getZ());
		Location locationTwoAbove = new Location(location.getWorld(), location.getX(), location.getY() + 2, location.getZ());
		
		// Breaking top half of plant
		if (locationAbove.getBlock().getType() == Material.LIGHT) {
			locationAbove.getBlock().setType(Material.AIR);
		}
		// Breaking bottom half of plant
		else if (locationTwoAbove.getBlock().getType() == Material.LIGHT) {
				locationTwoAbove.getBlock().setType(Material.AIR);
		}
		// Breaking the block under the plant
		 else if (locationAbove.getBlock().getType() == Material.PITCHER_PLANT) {
			Location locationAbovePitcherPlant = new Location(location.getWorld(), location.getX(), location.getY() + 3, location.getZ());
			if (locationAbovePitcherPlant.getBlock().getType() == Material.LIGHT) {
				locationAbovePitcherPlant.getBlock().setType(Material.AIR);
			}
		}
	}
}
