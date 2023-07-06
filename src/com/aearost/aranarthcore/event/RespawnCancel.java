package com.aearost.aranarthcore.event;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerRespawnEvent;

import com.aearost.aranarthcore.AranarthCore;

public class RespawnCancel implements Listener {

	public RespawnCancel(AranarthCore plugin) {
		Bukkit.getPluginManager().registerEvents(this, plugin);
	}

	/**
	 * Deals with manually teleporting a user to the world spawn so they
	 * don't respawn in the approximate area (the exact block).
	 * 
	 * @author Aearost
	 *
	 */
	@EventHandler
	public void onPlayerRespawn(PlayerRespawnEvent e) {
		World respawnWorld = e.getRespawnLocation().getWorld();
		double x = e.getRespawnLocation().getBlockX();
		double z = e.getRespawnLocation().getBlockZ();
		// Only place the user here if they don't have a bed to respawn at
		if (respawnWorld.getName().equals("world") && x == 0 && z == 3) {
			e.setRespawnLocation(new Location(respawnWorld, x, 120, z, 180, 0));
		}
	}

}
