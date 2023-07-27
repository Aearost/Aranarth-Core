package com.aearost.aranarthcore.event;

import java.util.Random;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.data.Ageable;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.inventory.ItemStack;

import com.aearost.aranarthcore.AranarthCore;

public class PitcherPlantBreak implements Listener {

	public PitcherPlantBreak(AranarthCore plugin) {
		Bukkit.getPluginManager().registerEvents(this, plugin);
	}

	/**
	 * Drops Pitcher Plant seeds when the plant is harvested.
	 * 
	 * @param e
	 */
	@EventHandler
	public void onPitcherPlantBreak(final BlockBreakEvent e) {
		Location location = e.getBlock().getLocation();
		
		if (location.getBlock().getType() == Material.PITCHER_CROP) {
			Ageable pitcherPlant = (Ageable) location.getBlock().getBlockData();
			if (pitcherPlant.getAge() == pitcherPlant.getMaximumAge()) {
				Random r = new Random();
				// Will randomly select 0, 1, or 2 seeds
				location.getWorld().dropItemNaturally(location, new ItemStack(Material.PITCHER_POD, r.nextInt((2) + 1)));
			}
		}
	}
}
