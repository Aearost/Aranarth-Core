package com.aearost.aranarthcore.commands;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import com.aearost.aranarthcore.objects.AranarthPlayer;
import com.aearost.aranarthcore.utils.AranarthUtils;
import com.aearost.aranarthcore.utils.ChatUtils;

public class CommandNickname implements CommandExecutor {

	@Override
	public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {

		if (args.length == 0) {
			sender.sendMessage(ChatUtils.chatMessage("&cIncorrect syntax: /nickname [player] <nickname>"));
			return false;
		} else {
			if (args.length == 1) {
				
				if (sender instanceof Player) {
					Player player = (Player) sender;
					AranarthPlayer aranarthPlayer = AranarthUtils.getPlayer(player.getUniqueId());
					
					if (args[0].toLowerCase().equals("off")) {
						aranarthPlayer.setNickname("");
						player.sendMessage(ChatUtils.chatMessage("&7Your nickname has been removed!"));
						return true;
					}
					
					aranarthPlayer.setNickname(args[0]);
					sender.sendMessage(ChatUtils.chatMessage("&7Your nickname has been set to " + args[0]));
					return true;
				} else {
					sender.sendMessage(ChatUtils.chatMessage("&cIncorrect syntax: /nickname [player] <nickname>"));
					return false;
				}
			} else {
				Player[] onlinePlayers = new Player[Bukkit.getOnlinePlayers().size()];
				Bukkit.getOnlinePlayers().toArray(onlinePlayers);
				
				for (Player p : onlinePlayers) {
					// If the player is online
					if (p.getName().toLowerCase().equals(args[0].toLowerCase())) {
						AranarthPlayer aranarthPlayer = AranarthUtils.getPlayer(p.getUniqueId());
						
						if (args[1].toLowerCase().equals("off")) {
							aranarthPlayer.setNickname("");
							sender.sendMessage(ChatUtils.chatMessage("&e" + p.getName() + "'s &7nickname has been removed!"));
							return true;
						} else {
							String nickname = "";
							for (int i = 1; i < args.length; i++) {
								if (i < args.length - 1) {
									nickname += args[i] + " ";
								} else {
									nickname += args[i];
								}
							}
							aranarthPlayer.setNickname(nickname);
							sender.sendMessage(ChatUtils.chatMessage("&e" + args[0] + "'s &7nickname has been set to " + nickname));
							return true;
						}
					}
				}
				
				if (sender instanceof Player) {
					// If the player is using a multi-word nickname
					Player player = (Player) sender;
					AranarthPlayer aranarthPlayer = AranarthUtils.getPlayer(player.getUniqueId());
					String nickname = "";
					for (int i = 0; i < args.length; i++) {
						if (i < args.length - 1) {
							nickname += args[i] + " ";
						} else {
							nickname += args[i];
						}
					}
					aranarthPlayer.setNickname(nickname);
					sender.sendMessage(ChatUtils.chatMessage("&7Your nickname has been set to " + nickname));
					return true;
				}
				
				sender.sendMessage(ChatUtils.chatMessage("&cThat player is not online!"));
				return false;
				
				
			}
		}
	}

}
