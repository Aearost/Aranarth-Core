package com.aearost.aranarthcore.commands;

import java.io.IOException;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import com.aearost.aranarthcore.utils.AranarthUtils;
import com.aearost.aranarthcore.utils.ChatUtils;

public class CommandCreative {

	public static boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
		if (args.length == 1) {
			if (sender instanceof Player) {
				Player player = (Player) sender;
				
				// Teleports you to the creative world spawn
				try {
					AranarthUtils.switchInventory(player, player.getLocation().getWorld().getName(), "creative");
				} catch (IOException e) {
					player.sendMessage(ChatUtils.chatMessageError("Something went wrong with changing world."));
					e.printStackTrace();
					return false;
				}
				player.teleport(new Location(Bukkit.getWorld("creative"), 0.5, -60, 0.5, 0, 2));
				player.sendMessage(ChatUtils.chatMessage("&7You have been teleported to &eCreative!"));
				return true;
			} else {
				sender.sendMessage(ChatUtils.chatMessageError("You must be a player to use this command!"));
			}
		}
		return false;
	}

}
