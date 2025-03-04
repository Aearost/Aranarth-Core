package com.aearost.aranarthcore.event.world;

import com.aearost.aranarthcore.AranarthCore;
import com.aearost.aranarthcore.utils.AranarthUtils;
import com.aearost.aranarthcore.utils.ChatUtils;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.event.player.PlayerTeleportEvent.TeleportCause;
import org.bukkit.potion.PotionEffect;

import java.io.IOException;

public class PlayerTeleportBetweenWorlds implements Listener {

	public PlayerTeleportBetweenWorlds(AranarthCore plugin) {
		Bukkit.getPluginManager().registerEvents(this, plugin);
	}

	/**
	 * Ensures that a player's inventory is updated when teleported manually via /tp.
	 * @param e The event.
	 */
	@EventHandler
	public void onTeleport(final PlayerTeleportEvent e) {
		if (e.getCause() == TeleportCause.COMMAND) {
			Player player = e.getPlayer();
			String currentWorld = e.getFrom().getWorld().getName();
			String destinationWorld = e.getTo().getWorld().getName();
			if (!currentWorld.equals(destinationWorld)) {
				GameMode newMode = GameMode.SURVIVAL;
				if (destinationWorld.equalsIgnoreCase("creative")) {
					newMode = GameMode.CREATIVE;
				}

				try {
					AranarthUtils.switchInventory(player, currentWorld, destinationWorld);
					player.setGameMode(newMode);
					for (PotionEffect effect : player.getActivePotionEffects()) {
						player.removePotionEffect(effect.getType());
					}
				} catch (IOException exception) {
					player.sendMessage(ChatUtils.chatMessage("&cSomething went wrong with changing world."));
					e.setCancelled(true);
				}
			} else {
				try {
					AranarthUtils.switchInventory(player, currentWorld, currentWorld);
				} catch (IOException exception) {
					player.sendMessage(ChatUtils.chatMessage("&cSomething went wrong with changing world."));
					e.setCancelled(true);
				}
			}
		}
	}
}
