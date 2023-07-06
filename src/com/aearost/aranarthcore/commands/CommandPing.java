package com.aearost.aranarthcore.commands;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import com.aearost.aranarthcore.utils.ChatUtils;

public class CommandPing {

	public static boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
		if (args.length == 1) {
			if (sender instanceof Player) {
				Player player = (Player) sender;
				player.sendMessage(ChatUtils.chatMessage("&7Your ping is &e" + player.getPing() + "ms"));
				return true;
			} else {
				sender.sendMessage(ChatUtils.chatMessageError("You must specify a player's ping! /ac ping <player>"));
				return false;
			}
		} else {
			boolean isPlayerFound = false;
			for (Player onlinePlayer : Bukkit.getOnlinePlayers()) {
				// If the user is running the command on another user's name
				if (sender.getName().toLowerCase().equals(args[1].toLowerCase())) {
					sender.sendMessage(ChatUtils.chatMessage("&7Your ping is &e" + onlinePlayer.getPing() + "ms"));
					isPlayerFound = true;
					return true;
				}
				// If the user is running the command on their own name
				else if (onlinePlayer.getName().toLowerCase().equals(args[1].toLowerCase())) {
					sender.sendMessage(ChatUtils.chatMessage("&e" + onlinePlayer.getName() +"'s &7ping is &e" + onlinePlayer.getPing() + "ms"));
					isPlayerFound = true;
					return true;
				}
			}
			if (!isPlayerFound) {
				sender.sendMessage(ChatUtils.chatMessageError("That player is not online!"));
			}
		}
		return false;
	}

}
