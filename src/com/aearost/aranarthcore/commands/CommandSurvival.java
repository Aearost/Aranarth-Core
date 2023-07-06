package com.aearost.aranarthcore.commands;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import com.aearost.aranarthcore.utils.ChatUtils;

public class CommandSurvival {

	public static boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
		if (args.length == 1) {
			if (sender instanceof Player) {
				Player player = (Player) sender;
				
				// Teleports you to the arena world aligning directly with the Enter Arena sign
				player.teleport(new Location(Bukkit.getWorld("survival"), 0.5, 120, 3, 180, 0));
				return true;
			} else {
				sender.sendMessage(ChatUtils.chatMessageError("You must be a player to use this command!"));
			}
		}
		return false;
	}

}
