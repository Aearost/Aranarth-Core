package com.aearost.aranarthcore.commands;

import com.aearost.aranarthcore.utils.AranarthUtils;
import com.aearost.aranarthcore.utils.ChatUtils;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/**
 * Teleports the player to the creative world, using the creative inventory.
 */
public class CommandCreative {

	/**
	 * @param sender The user that entered the command.
	 * @param args The arguments of the command.
	 * @return Confirmation of whether the command was a success or not.
	 */
	public static boolean onCommand(CommandSender sender, String[] args) {
		if (args.length == 1) {
			if (sender instanceof Player player) {
				if (!AranarthUtils.isOriginalPlayer(player.getUniqueId())) {
					player.sendMessage(ChatUtils.chatMessage("&cYou do not have access to this world!"));
					return true;
				}
				Location creativeSpawn = new Location(Bukkit.getWorld("creative"), 0, -60, 0, 0, 2);
				AranarthUtils.teleportPlayer(player, player.getLocation(), creativeSpawn, success -> {
					if (success) {
						player.sendMessage(ChatUtils.chatMessage("&7You have been teleported to &eCreative"));
					} else {
						player.sendMessage(ChatUtils.chatMessage("&cYou could not teleport to &eCreative"));
					}
				});

				return true;
			} else {
				sender.sendMessage(ChatUtils.chatMessage("&cYou must be a player to use this command!"));
				return true;
			}
		}
		return false;
	}
}
