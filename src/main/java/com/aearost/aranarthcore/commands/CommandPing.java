package com.aearost.aranarthcore.commands;

import com.aearost.aranarthcore.utils.ChatUtils;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/**
 * Allows players to view their own or another player's ping.
 */
public class CommandPing {

	/**
	 * @param sender The user that entered the command.
	 * @param args The arguments of the command.
	 * @return Confirmation of whether the command was a success or not.
	 */
	public static boolean onCommand(CommandSender sender, String[] args) {
		if (args.length == 1) {
			if (sender instanceof Player player) {
                player.sendMessage(ChatUtils.chatMessage("&7Your ping is &e" + player.getPing() + "ms"));
				return true;
			} else {
				sender.sendMessage(ChatUtils.chatMessage("&cYou must specify a player's ping! /ac ping <player>"));
				return false;
			}
		} else {
            for (Player onlinePlayer : Bukkit.getOnlinePlayers()) {
				// If the user is running the command on another user's name
				if (sender.getName().equalsIgnoreCase(args[1])) {
					sender.sendMessage(ChatUtils.chatMessage("&7Your ping is &e" + onlinePlayer.getPing() + "ms"));
                    return true;
				}
				// If the user is running the command on their own name
				else if (onlinePlayer.getName().equalsIgnoreCase(args[1])) {
					sender.sendMessage(ChatUtils.chatMessage("&e" + onlinePlayer.getName() +"'s &7ping is &e" + onlinePlayer.getPing() + "ms"));
                    return true;
				}
			}
            sender.sendMessage(ChatUtils.chatMessage("&cThat player is not online!"));
			return true;
        }
	}

}
