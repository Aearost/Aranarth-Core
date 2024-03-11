package com.aearost.aranarthcore.commands;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import com.aearost.aranarthcore.gui.GuiPlayerInvView;
import com.aearost.aranarthcore.utils.ChatUtils;

public class CommandInvView {

	public static boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
		if (sender instanceof Player) {
			Player player = (Player) sender;
			if (!(player.getName().equals("Aearost") || player.getName().equals("Aearxst"))) {
				player.sendMessage(ChatUtils.chatMessageError("You are not authorized to use this command!"));
			}
			
			if (args.length == 1) {
				sender.sendMessage(ChatUtils.chatMessageError("Incorrect syntax: /ac invview <player>"));
			} else {
				// Logic goes here
				sender.sendMessage(ChatUtils.chatMessage("&a&lSuccess!"));
				// Fix to ensure proper username is entered
				GuiPlayerInvView gui = new GuiPlayerInvView(player, Bukkit.getPlayer(args[1]));
				gui.openGui();
			}
		} else {
			sender.sendMessage(ChatUtils.chatMessageError("You must be a player to use this command!"));
		}
		return false;
	}

}
