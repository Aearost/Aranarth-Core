package com.aearost.aranarthcore.event.player;

import com.aearost.aranarthcore.items.Homepad;
import com.aearost.aranarthcore.utils.AranarthUtils;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.inventory.ItemStack;

import java.util.Objects;

/**
 * Removes a homepad from the homes HashMap when it is destroyed.
 */
public class HomepadBreak {

	public void execute(BlockBreakEvent e) {
		Location location = e.getBlock().getLocation();
		Location locationAbove = new Location(location.getWorld(), location.getX(), location.getY() + 1, location.getZ());
		if (Objects.nonNull(AranarthUtils.getHomePad(location))) {
			e.setCancelled(true);
			AranarthUtils.removeHomePad(location);
			location.getWorld().dropItemNaturally(location, new Homepad().getItem());
			location.getBlock().setType(Material.AIR);
		} else if (Objects.nonNull(AranarthUtils.getHomePad(locationAbove))) {
			AranarthUtils.removeHomePad(locationAbove);
			locationAbove.getWorld().dropItemNaturally(locationAbove, new Homepad().getItem());
			locationAbove.getBlock().setType(Material.AIR);
			location.getWorld().dropItemNaturally(location, new ItemStack(e.getBlock().getType()));
			location.getBlock().setType(Material.AIR);
		}
		
	}

}
