package com.aearost.aranarthcore.commands;

import java.io.IOException;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import com.aearost.aranarthcore.utils.AranarthUtils;
import com.aearost.aranarthcore.utils.ChatUtils;

public class CommandSurvival {

	public static boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
		if (args.length == 1) {
			if (sender instanceof Player) {
				Player player = (Player) sender;
				
				// Teleports you to the survival world spawn
				try {
					AranarthUtils.switchInventory(player, player.getLocation().getWorld().getName(), "world");
				} catch (IOException e) {
					player.sendMessage(ChatUtils.chatMessageError("Something went wrong with changing world."));
					e.printStackTrace();
					return false;
				}
				player.teleport(new Location(Bukkit.getWorld("world"), 0.5, 120, 3, 180, 0));
				player.sendMessage(ChatUtils.chatMessage("&7You have been teleported to &eSurvival!"));
				return true;
			} else {
				sender.sendMessage(ChatUtils.chatMessageError("You must be a player to use this command!"));
			}
		}
		return false;
	}

}
