package com.aearost.aranarthcore.event.block;

import com.aearost.aranarthcore.utils.AranarthUtils;
import org.bukkit.Location;
import org.bukkit.event.block.BlockPlaceEvent;

/**
 * Adds a new entry to the homes HashMap when a Homepad is placed.
 */
public class HomepadPlace {

	public void execute(BlockPlaceEvent e) {
		Location location = e.getBlockPlaced().getLocation();
		AranarthUtils.addNewHome(location);
	}
}
