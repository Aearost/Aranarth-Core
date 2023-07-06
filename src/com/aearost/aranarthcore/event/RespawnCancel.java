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
	 * Deals with manually teleporting a user to the world spawn.
	 * 
	 * @author Aearost
	 *
	 */
	@EventHandler
	public void onEggPickupCancel(PlayerRespawnEvent e) {
		World respawnWorld = e.getRespawnLocation().getWorld();
		double x = e.getRespawnLocation().getX();
		double z = e.getRespawnLocation().getZ();
		e.setRespawnLocation(new Location(respawnWorld, x, 120, z, 180, 0));
	}

}
