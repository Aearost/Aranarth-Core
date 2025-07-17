package com.aearost.aranarthcore.event.listener.misc;

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

public class PlayerTeleportBetweenWorldsListener implements Listener {

	public PlayerTeleportBetweenWorldsListener(AranarthCore plugin) {
		Bukkit.getPluginManager().registerEvents(this, plugin);
	}

	/**
	 * Ensures that a player's inventory is updated when teleported manually via /tp.
	 * @param e The event.
	 */
	@EventHandler
	public void onTeleport(PlayerTeleportEvent e) {
		if (e.getCause() == TeleportCause.COMMAND) {
			Player player = e.getPlayer();
			String currentWorld = e.getFrom().getWorld().getName();
			String destinationWorld = e.getTo().getWorld().getName();
			Bukkit.getLogger().info("A");
			if (!currentWorld.equals(destinationWorld)) {
				Bukkit.getLogger().info("B");
				GameMode newMode = GameMode.SURVIVAL;
				if (destinationWorld.equalsIgnoreCase("creative")) {
					newMode = GameMode.CREATIVE;
				}

				try {
					Bukkit.getLogger().info("C");
					AranarthUtils.switchInventory(player, currentWorld, destinationWorld);
					player.setGameMode(newMode);
					for (PotionEffect effect : player.getActivePotionEffects()) {
						Bukkit.getLogger().info("D");
						player.removePotionEffect(effect.getType());
					}
				} catch (IOException exception) {
					player.sendMessage(ChatUtils.chatMessage("&cSomething went wrong with changing world."));
					e.setCancelled(true);
				}
			} else {
				Bukkit.getLogger().info("E");
				try {
					Bukkit.getLogger().info("F");
					AranarthUtils.switchInventory(player, currentWorld, currentWorld);
				} catch (IOException exception) {
					Bukkit.getLogger().info("G");
					player.sendMessage(ChatUtils.chatMessage("&cSomething went wrong with changing world."));
					e.setCancelled(true);
				}
			}
		}
	}
}
