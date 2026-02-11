package com.aearost.aranarthcore.commands;

import com.aearost.aranarthcore.utils.AranarthUtils;
import com.aearost.aranarthcore.utils.ChatUtils;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/**
 * Teleports the player to the Spawn world, sharing the Survival inventory.
 */
public class CommandSpawn {

	/**
	 * @param sender The user that entered the command.
	 * @param args The arguments of the command.
	 * @return Confirmation of whether the command was a success or not.
	 */
	public static boolean onCommand(CommandSender sender, String[] args) {
		if (args.length == 1) {
			if (sender instanceof Player player) {
				Location spawn = new Location(Bukkit.getWorld("spawn"), 0.5, 100, 0.5, 180, 0);
				AranarthUtils.teleportPlayer(player, player.getLocation(), spawn, success -> {
					if (success) {
						player.sendMessage(ChatUtils.chatMessage("&7You have been teleported to &eSpawn"));
					} else {
						player.sendMessage(ChatUtils.chatMessage("&cYou could not teleport to &eSpawn"));
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
