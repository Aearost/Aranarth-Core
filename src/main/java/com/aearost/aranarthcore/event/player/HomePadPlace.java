package com.aearost.aranarthcore.event.player;

import com.aearost.aranarthcore.AranarthCore;
import com.aearost.aranarthcore.items.HomePad;
import com.aearost.aranarthcore.utils.AranarthUtils;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.inventory.ItemStack;

public class HomePadPlace implements Listener {

	public HomePadPlace(AranarthCore plugin) {
		Bukkit.getPluginManager().registerEvents(this, plugin);
	}

	/**
	 * Adds a new entry to the homes HashMap when a homepad is placed.
	 * @param e The event.
	 */
	@EventHandler
	public void onHomePadPlace(final BlockPlaceEvent e) {
		ItemStack item = e.getItemInHand();
		if (item.isSimilar((HomePad.getHomePad()))) {
			Location location = e.getBlockPlaced().getLocation();
			AranarthUtils.addNewHome(location);
		}
	}
}
