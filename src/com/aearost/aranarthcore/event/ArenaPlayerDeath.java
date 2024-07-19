package com.aearost.aranarthcore.event;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerRespawnEvent;

import com.aearost.aranarthcore.AranarthCore;

public class ArenaPlayerDeath implements Listener {

	public ArenaPlayerDeath(AranarthCore plugin) {
		Bukkit.getPluginManager().registerEvents(this, plugin);
	}

	/**
	 * Forces the player to respawn in the arena world when they die in it.
	 * 
	 * @param e
	 */
	@EventHandler
	public void onArenaBlockBreak(final PlayerRespawnEvent e) {
		if (e.getPlayer().getWorld().getName().toLowerCase().equals("arena")) {
			e.setRespawnLocation(new Location(Bukkit.getWorld("arena"), 0.5, 105, 0.5, 180, 2));
		}
	}
}
