package com.aearost.aranarthcore.commands;

import com.aearost.aranarthcore.utils.AranarthUtils;
import com.aearost.aranarthcore.utils.ChatUtils;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/**
 * Teleports the player to the SMP world, sharing the SMP inventory.
 */
public class CommandSMP {

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

				Location smpSpawn = new Location(Bukkit.getWorld("smp"), 0.5, 120, 3, 180, 0);
				AranarthUtils.teleportPlayer(player, player.getLocation(), smpSpawn, success -> {
					if (success) {
						player.sendMessage(ChatUtils.chatMessage("&7You have been teleported to the &eSMP"));
					} else {
						player.sendMessage(ChatUtils.chatMessage("&cYou could not teleport to the &eSMP"));
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
