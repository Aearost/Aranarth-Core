package com.aearost.aranarthcore.commands;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import com.aearost.aranarthcore.utils.ChatUtils;

public class CommandPing implements CommandExecutor {

	@Override
	public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {

		if (args.length == 0) {
			if (sender instanceof Player) {
				Player player = (Player) sender;
				player.sendMessage(ChatUtils.chatMessage("&7Your ping is &e" + player.getPing() + "ms"));
				return true;
			} else {
				sender.sendMessage(ChatUtils.chatMessage("&cYou must specify a player's ping! /ping <player>"));
				return false;
			}
		} else {
			Player[] onlinePlayers = new Player[Bukkit.getOnlinePlayers().size()];
			Bukkit.getOnlinePlayers().toArray(onlinePlayers);

			for (Player p : onlinePlayers) {
				// If the player is online
				if (p.getName().toLowerCase().equals(args[0].toLowerCase())) {
					sender.sendMessage(ChatUtils.chatMessage("&e" + p.getName() +"'s &7ping is &e" + p.getPing() + "ms"));
					return true;
				} else {
					sender.sendMessage(ChatUtils.chatMessage("&cThat player is not online!"));
					return false;
				}
			}
		}
		
		return true;
	}

}
