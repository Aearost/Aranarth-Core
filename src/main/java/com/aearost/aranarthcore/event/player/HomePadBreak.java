package com.aearost.aranarthcore.event.player;

import com.aearost.aranarthcore.AranarthCore;
import com.aearost.aranarthcore.items.HomePad;
import com.aearost.aranarthcore.utils.AranarthUtils;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.inventory.ItemStack;

import java.util.Objects;

public class HomePadBreak implements Listener {

	public HomePadBreak(AranarthCore plugin) {
		Bukkit.getPluginManager().registerEvents(this, plugin);
	}

	/**
	 * Removes a homepad from the homes HashMap when it is destroyed.
	 * @param e The event.
	 */
	@EventHandler
	public void onHomePadBreak(final BlockBreakEvent e) {
		Location location = e.getBlock().getLocation();
		Location locationAbove = new Location(location.getWorld(), location.getX(), location.getY() + 1, location.getZ());
		if (Objects.nonNull(AranarthUtils.getHomePad(location))) {
			e.setCancelled(true);
			AranarthUtils.removeHomePad(location);
			location.getWorld().dropItemNaturally(location, HomePad.getHomePad());
			location.getBlock().setType(Material.AIR);
		} else if (Objects.nonNull(AranarthUtils.getHomePad(locationAbove))) {
			AranarthUtils.removeHomePad(locationAbove);
			locationAbove.getWorld().dropItemNaturally(locationAbove, HomePad.getHomePad());
			locationAbove.getBlock().setType(Material.AIR);
			location.getWorld().dropItemNaturally(location, new ItemStack(e.getBlock().getType()));
			location.getBlock().setType(Material.AIR);
		}
		
	}

}
