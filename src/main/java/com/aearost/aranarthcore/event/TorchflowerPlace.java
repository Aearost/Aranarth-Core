package com.aearost.aranarthcore.event;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.inventory.ItemStack;

import com.aearost.aranarthcore.AranarthCore;

public class TorchflowerPlace implements Listener {

	public TorchflowerPlace(AranarthCore plugin) {
		Bukkit.getPluginManager().registerEvents(this, plugin);
	}

	/**
	 * Adds a light block above the torchflower when placed so the plant emits light
	 * 
	 * @param e
	 */
	@EventHandler
	public void onTorchflowerPlace(final BlockPlaceEvent e) {
		
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
