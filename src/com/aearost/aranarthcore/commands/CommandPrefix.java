package com.aearost.aranarthcore.commands;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import com.aearost.aranarthcore.objects.AranarthPlayer;
import com.aearost.aranarthcore.utils.AranarthUtils;
import com.aearost.aranarthcore.utils.ChatUtils;

public class CommandPrefix {

	public static boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {

		if (args.length == 1) {
			sender.sendMessage(ChatUtils.chatMessageError("Incorrect syntax: /prefix [player] <prefix>"));
			return false;
		} else {
			if (args.length == 2) {
				if (sender instanceof Player) {
					Player player = (Player) sender;
					AranarthPlayer aranarthPlayer = AranarthUtils.getPlayer(player.getUniqueId());
					
					if (args[1].toLowerCase().equals("off")) {
						aranarthPlayer.setPrefix("");
						player.sendMessage(ChatUtils.chatMessage("&7Your prefix has been removed!"));
						return true;
					}
					
					aranarthPlayer.setPrefix(args[1]);
					sender.sendMessage(ChatUtils.chatMessage("&7Your prefix has been set to " + args[1]));
					return true;
				} else {
					sender.sendMessage(ChatUtils.chatMessageError("Incorrect syntax: /ac prefix [player] <prefix>"));
					return false;
				}
			} else {
				Player[] onlinePlayers = new Player[Bukkit.getOnlinePlayers().size()];
				Bukkit.getOnlinePlayers().toArray(onlinePlayers);
				
				for (Player p : onlinePlayers) {
					// If the player is online
					if (p.getName().toLowerCase().equals(args[1].toLowerCase())) {
						AranarthPlayer aranarthPlayer = AranarthUtils.getPlayer(p.getUniqueId());
						
						if (args[2].toLowerCase().equals("off")) {
							aranarthPlayer.setPrefix("");
							sender.sendMessage(ChatUtils.chatMessage("&e" + p.getName() + "'s &7prefix has been removed!"));
							return true;
						} else {
							String prefix = "";
							for (int i = 2; i < args.length; i++) {
								if (i < args.length - 1) {
									prefix += args[i] + " ";
								} else {
									prefix += args[i];
								}
							}
							aranarthPlayer.setPrefix(prefix);
							sender.sendMessage(ChatUtils.chatMessage("&e" + args[1] + "'s &7prefix has been set to " + prefix));
							return true;
						}
					}
				}
				
				if (sender instanceof Player) {
					// If the player is using a multi-word prefix
					Player player = (Player) sender;
					AranarthPlayer aranarthPlayer = AranarthUtils.getPlayer(player.getUniqueId());
					String prefix = "";
					for (int i = 1; i < args.length; i++) {
						if (i < args.length - 1) {
							prefix += args[i] + " ";
						} else {
							prefix += args[i];
						}
					}
					aranarthPlayer.setPrefix(prefix);
					sender.sendMessage(ChatUtils.chatMessage("&7Your prefix has been set to " + prefix));
					return true;
				}
				
				sender.sendMessage(ChatUtils.chatMessageError("That player is not online!"));
				return false;
				
				
			}
		}
	}

}
