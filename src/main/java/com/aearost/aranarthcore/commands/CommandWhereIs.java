package com.aearost.aranarthcore.commands;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import com.aearost.aranarthcore.utils.ChatUtils;

public class CommandWhereIs {

	public static boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
		if (args.length == 1) {
			sender.sendMessage(ChatUtils.chatMessageError("You must enter a player's username!"));
		} else {
			boolean isPlayerFound = false;
			for (Player onlinePlayer : Bukkit.getOnlinePlayers()) {
				if (args[1].toLowerCase().equals(onlinePlayer.getName().toLowerCase())) {
					Location location = onlinePlayer.getLocation();
					sender.sendMessage(ChatUtils.chatMessage(onlinePlayer.getDisplayName()
							+ " is at x: " + location.getBlockX() + " | y: " + location.getBlockY() +
							" | z: " + location.getBlockZ()));
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
