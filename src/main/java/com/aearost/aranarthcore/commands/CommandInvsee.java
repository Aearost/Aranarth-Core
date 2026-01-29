package com.aearost.aranarthcore.commands;

import com.aearost.aranarthcore.utils.ChatUtils;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/**
 * Allows the player to see another player's inventory.
 */
public class CommandInvsee {

	/**
	 * @param sender The user that entered the command.
	 * @param args The arguments of the command.
	 */
	public static boolean onCommand(CommandSender sender, String[] args) {
		if (sender instanceof Player player) {
			if (player.hasPermission("aranarth.invsee")) {
				sender.sendMessage(ChatUtils.chatMessage("&4This feature is not currently implemented."));
				return true;
			}
		}
		return false;
	}

}
