package com.aearost.aranarthcore.commands;

import java.util.Objects;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import com.aearost.aranarthcore.utils.AranarthUtils;
import com.aearost.aranarthcore.utils.ChatUtils;

public class CommandHomePad implements CommandExecutor {

	@Override
	public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {

		if (sender instanceof Player) {
			Player player = (Player) sender;
			
			if (Objects.nonNull(AranarthUtils.getHomePad(player.getLocation()))) {
				
				if (args.length == 0) {
					player.sendMessage(ChatUtils.chatMessage("&cYou must enter a home name!"));
					return false;
				}
				
				if (AranarthUtils.getHomePad(player.getLocation()).getHomeName().equals("NEW")) {
					String homeName = "";
					for (int i = 0; i < args.length; i++) {
						if (i == args.length - 1) {
							homeName += args[i];
						} else {
							homeName += args[i] + " ";
						}
					}
					if (homeName.matches("[a-zA-Z0-9& ]+")) {
						AranarthUtils.setHomeName(homeName, AranarthUtils.getHomePad(player.getLocation()));
						player.sendMessage(ChatUtils.chatMessage("&7Home &e" + homeName + " &7has been created"));
						return true;
					} else {
						player.sendMessage(ChatUtils.chatMessage("&cYou must use alphanumeric characters!"));
						return false;
					}
				} else {
					player.sendMessage(ChatUtils.chatMessage("&cYou cannot rename a home pad"));
					return false;
				}
				
			} else {
				player.sendMessage(ChatUtils.chatMessage("&cYou must be standing on a Home Pad to use this command!"));
				return true;
			}
			
		} else {
			sender.sendMessage("This must be executed in-game!");
		}
		
		return true;
	}

}
