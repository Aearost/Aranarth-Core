package com.aearost.aranarthcore.event.block;

import com.aearost.aranarthcore.AranarthCore;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;

public class TorchflowerBreak implements Listener {

	public TorchflowerBreak(AranarthCore plugin) {
		Bukkit.getPluginManager().registerEvents(this, plugin);
	}

	/**
	 * Removes the light block above the torchflower.
	 * @param e The event.
	 */
	@EventHandler
	public void onTorchflowerBreak(final BlockBreakEvent e) {
		Location location = e.getBlock().getLocation();
		Location locationAbove = new Location(location.getWorld(), location.getX(), location.getY() + 1, location.getZ());
		
		if (location.getBlock().getType() == Material.TORCHFLOWER && locationAbove.getBlock().getType() == Material.LIGHT) {
			locationAbove.getBlock().setType(Material.AIR);
		} else if (locationAbove.getBlock().getType() == Material.TORCHFLOWER) {
			Location locationAboveTorchflower = new Location(locationAbove.getWorld(), locationAbove.getX(), locationAbove.getY() + 1, locationAbove.getZ());
			locationAboveTorchflower.getBlock().setType(Material.AIR);
		}
	}

}
