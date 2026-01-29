package com.aearost.aranarthcore.event.player;

import com.aearost.aranarthcore.utils.AranarthUtils;
import com.aearost.aranarthcore.utils.ChatUtils;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerMoveEvent;

/**
 * Handles Spawn enter and exit messages.
 */
public class SpawnChangeLocation {

	public void execute(PlayerMoveEvent e) {
		// If they did not move to a different coordinate and only their mouse
		if (e.getTo() == null) {
			return;
		}

		if (e.getTo().getWorld().getName().startsWith("world")) {
			Location from = e.getFrom();
			Location to = e.getTo();
			boolean fromSpawn = AranarthUtils.isSpawnLocation(from.getBlock().getLocation());
			boolean toSpawn = AranarthUtils.isSpawnLocation(to.getBlock().getLocation());

			// If it's the same location
			if (from.getX() == to.getX() && from.getZ() == to.getZ() && from.getWorld().getName().equals(to.getWorld().getName())) {
				return;
			} else {
				Player player = e.getPlayer();
				// Entering spawn
				if (!fromSpawn && toSpawn) {
					player.sendMessage(ChatUtils.chatMessage("&7You have entered the Spawn"));
				}
				// Leaving spawn
				else if (fromSpawn && !toSpawn) {
					player.sendMessage(ChatUtils.chatMessage("&7You have left the Spawn"));
				}
			}
		}
	}
}
