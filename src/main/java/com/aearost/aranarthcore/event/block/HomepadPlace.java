package com.aearost.aranarthcore.event.block;

import com.aearost.aranarthcore.utils.AranarthUtils;
import org.bukkit.Location;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.inventory.meta.ItemMeta;

import static com.aearost.aranarthcore.items.CustomItemKeys.HOMEPAD;

/**
 * Adds a new entry to the homes HashMap when a Homepad is placed.
 */
public class HomepadPlace {

	public void execute(BlockPlaceEvent e) {
		if (e.getItemInHand().hasItemMeta()) {
			ItemMeta meta = e.getItemInHand().getItemMeta();
			if (meta.getPersistentDataContainer().has(HOMEPAD)) {
				Location location = e.getBlockPlaced().getLocation();
				AranarthUtils.addNewHome(location);
			}
		}
	}
}
