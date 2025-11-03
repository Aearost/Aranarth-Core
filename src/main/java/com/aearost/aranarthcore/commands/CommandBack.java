package com.aearost.aranarthcore.commands;

import com.aearost.aranarthcore.objects.AranarthPlayer;
import com.aearost.aranarthcore.utils.AranarthUtils;
import com.aearost.aranarthcore.utils.ChatUtils;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.io.IOException;

/**
 * Teleports the player back to their last known location.
 */
public class CommandBack {

	/**
	 * @param sender The user that entered the command.
	 * @param args The arguments of the command.
	 */
	public static boolean onCommand(CommandSender sender, String[] args) {
		if (sender instanceof Player player) {
			AranarthPlayer aranarthPlayer = AranarthUtils.getPlayer(player.getUniqueId());
			if (aranarthPlayer.getLastKnownTeleportLocation() != null) {
				// Teleports you to the survival world spawn
				try {
					AranarthUtils.switchInventory(player, player.getLocation().getWorld().getName(), aranarthPlayer.getLastKnownTeleportLocation().getWorld().getName());
				} catch (IOException e) {
					player.sendMessage(ChatUtils.chatMessage("&cSomething went wrong with changing world."));
					return false;
				}

				player.teleport(aranarthPlayer.getLastKnownTeleportLocation());
				AranarthUtils.playTeleportSound(player);
				player.sendMessage(ChatUtils.chatMessage("&7You have returned to your previous location"));
				aranarthPlayer.setLastKnownTeleportLocation(null);
				AranarthUtils.setPlayer(player.getUniqueId(), aranarthPlayer);
				return true;
			} else {
				player.sendMessage(ChatUtils.chatMessage("&cYou do not have a previous location to teleport to!"));
				return true;
			}
		} else {
			sender.sendMessage(ChatUtils.chatMessage("&cOnly players can execute this command!"));
			return true;
		}
	}

}
