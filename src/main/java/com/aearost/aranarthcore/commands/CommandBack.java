package com.aearost.aranarthcore.commands;

import com.aearost.aranarthcore.objects.AranarthPlayer;
import com.aearost.aranarthcore.utils.AranarthUtils;
import com.aearost.aranarthcore.utils.ChatUtils;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

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
			if (!player.hasPermission("aranarth.back")) {
				player.sendMessage(ChatUtils.chatMessage("&cYou cannot use this command!"));
				return true;
			}

			AranarthPlayer aranarthPlayer = AranarthUtils.getPlayer(player.getUniqueId());
			if (aranarthPlayer.getLastKnownTeleportLocation() != null) {
				AranarthUtils.teleportPlayer(player, player.getLocation(), aranarthPlayer.getLastKnownTeleportLocation(), aranarthPlayer.isInAdminMode(), success -> {
					if (success) {
						player.sendMessage(ChatUtils.chatMessage("&7You have returned to your previous location"));
					} else {
						player.sendMessage(ChatUtils.chatMessage("&cYou could not return to your previous location"));
					}
				});
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
