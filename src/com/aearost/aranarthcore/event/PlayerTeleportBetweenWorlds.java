package com.aearost.aranarthcore.event;

import java.io.IOException;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.event.player.PlayerTeleportEvent.TeleportCause;

import com.aearost.aranarthcore.AranarthCore;
import com.aearost.aranarthcore.utils.AranarthUtils;
import com.aearost.aranarthcore.utils.ChatUtils;

public class PlayerTeleportBetweenWorlds implements Listener {

	public PlayerTeleportBetweenWorlds(AranarthCore plugin) {
		Bukkit.getPluginManager().registerEvents(this, plugin);
	}

	/**
	 * Ensures that a player's inventory is updated when teleported manually via /tp.
	 * 
	 * @param e
	 */
	@EventHandler
	public void onTeleport(final PlayerTeleportEvent e) {
		if (e.getCause() == TeleportCause.COMMAND) {
			Player player = e.getPlayer();
			String currentWorld = e.getFrom().getWorld().getName();
			String destinationWorld = e.getTo().getWorld().getName();
			if (!currentWorld.equals(destinationWorld)) {
				try {
					AranarthUtils.switchInventory(player, currentWorld, destinationWorld);
				} catch (IOException exception) {
					player.sendMessage(ChatUtils.chatMessageError("Something went wrong with changing world."));
					exception.printStackTrace();
					e.setCancelled(true);
				}
			}
		}
	}
}
