package com.aearost.aranarthcore.event;

import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.FoodLevelChangeEvent;

import com.aearost.aranarthcore.AranarthCore;

public class ArenaHungerLossPrevent implements Listener {

	public ArenaHungerLossPrevent(AranarthCore plugin) {
		Bukkit.getPluginManager().registerEvents(this, plugin);
	}

	/**
	 * Prevents hunger from being lost in the arena world.
	 * 
	 * @param e
	 */
	@EventHandler
	public void onArenaBlockMelt(final FoodLevelChangeEvent e) {
		if (e.getEntity().getWorld().getName().toLowerCase().equals("arena")) {
			e.setCancelled(true);
		}
	}
}
