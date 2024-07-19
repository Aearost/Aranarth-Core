package com.aearost.aranarthcore.event;

import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerDropItemEvent;

import com.aearost.aranarthcore.AranarthCore;

public class ArenaInventoryItemDropPrevent implements Listener {

	public ArenaInventoryItemDropPrevent(AranarthCore plugin) {
		Bukkit.getPluginManager().registerEvents(this, plugin);
	}

	/**
	 * Prevents players from dropping items in the arena world.
	 * 
	 * @param e
	 */
	@EventHandler
	public void onPlayerDropItem(final PlayerDropItemEvent e) {
		if (e.getPlayer().getWorld().getName().toLowerCase().equals("arena")) {
			e.setCancelled(true);
		}
	}
}
