package com.aearost.aranarthcore.event.player;

import java.io.IOException;

import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerRespawnEvent;

import com.aearost.aranarthcore.AranarthCore;
import com.aearost.aranarthcore.utils.AranarthUtils;
import com.aearost.aranarthcore.utils.ChatUtils;

public class RespawnCancel implements Listener {

	public RespawnCancel(AranarthCore plugin) {
		Bukkit.getPluginManager().registerEvents(this, plugin);
	}

	/**
	 * Deals with manually teleporting a user to the world spawn, so they
	 * don't respawn in the approximate area (the exact block).
	 * @param e The event.
	 */
	@EventHandler
	public void onPlayerRespawn(final PlayerRespawnEvent e) {
		
		World respawnWorld = e.getRespawnLocation().getWorld();
		double x = e.getRespawnLocation().getBlockX();
		double z = e.getRespawnLocation().getBlockZ();
		// Only place the user here if they don't have a bed to respawn at
		if (respawnWorld.getName().equals("world")) {
			Player player = e.getPlayer();
			player.setGameMode(GameMode.SURVIVAL);
			// Teleports you to the arena world aligning directly with the Enter Arena sign
			try {
				AranarthUtils.switchInventory(player, player.getLocation().getWorld().getName(), "world");
			} catch (IOException ex) {
				player.sendMessage(ChatUtils.chatMessage("&cSomething went wrong with changing world."));
				return;
			}
			if (x == 0 && z == 3) {
				e.setRespawnLocation(new Location(respawnWorld, x, 120, z, 180, 0));
			}
		}
	}

}
