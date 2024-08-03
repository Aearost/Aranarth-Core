package com.aearost.aranarthcore.event;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.data.Levelled;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.inventory.ItemStack;

import com.aearost.aranarthcore.AranarthCore;

public class PitcherPlantPlace implements Listener {

	public PitcherPlantPlace(AranarthCore plugin) {
		Bukkit.getPluginManager().registerEvents(this, plugin);
	}

	/**
	 * Adds a light block above the pitcher plant when placed so the plant emits light.
	 * @param e The event.
	 */
	@EventHandler
	public void onPitcherPlantPlace(final BlockPlaceEvent e) {
		ItemStack item = e.getItemInHand();
		if (item.getType() == Material.PITCHER_PLANT) {
			Location location = e.getBlockPlaced().getLocation();
			Location locationAbove = new Location(location.getWorld(), location.getX(), location.getY() + 2, location.getZ());
			if (locationAbove.getBlock().getType() == Material.AIR) {
				locationAbove.getBlock().setType(Material.LIGHT);
				Levelled level = (Levelled) locationAbove.getBlock().getBlockData();
				level.setLevel(10);
				locationAbove.getBlock().setBlockData(level, true);
			}
			
			
		}
		
	}

}
