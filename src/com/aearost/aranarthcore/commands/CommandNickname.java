package com.aearost.aranarthcore.commands;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import com.aearost.aranarthcore.objects.AranarthPlayer;
import com.aearost.aranarthcore.utils.AranarthUtils;
import com.aearost.aranarthcore.utils.ChatUtils;

public class CommandNickname {

	public static boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {

		if (args.length == 1) {
			if (sender instanceof Player) {
				Player player = (Player) sender;
				AranarthPlayer aranarthPlayer = AranarthUtils.getPlayer(player.getUniqueId());
				aranarthPlayer.setNickname("");
				player.sendMessage(ChatUtils.chatMessage("&7Your nickname has been removed!"));
				return true;
			} else {
				sender.sendMessage(ChatUtils.chatMessageError("Console does not have a nickname!"));
				return false;
			}
		} else if (args.length == 2) {

			if (sender instanceof Player) {
				Player player = (Player) sender;
				AranarthPlayer aranarthPlayer = AranarthUtils.getPlayer(player.getUniqueId());

				aranarthPlayer.setNickname(args[1]);
				AranarthUtils.setPlayer(player.getUniqueId(), aranarthPlayer);
				sender.sendMessage(ChatUtils.chatMessage("&7Your nickname has been set to " + args[1]));
				return true;
			}
			// Can only remove somebody else's nickname if done through the console
			else {
				Player[] onlinePlayers = new Player[Bukkit.getOnlinePlayers().size()];
				Bukkit.getOnlinePlayers().toArray(onlinePlayers);

				for (Player p : onlinePlayers) {
					// If the player is online
					if (p.getName().toLowerCase().equals(args[1].toLowerCase())) {
						AranarthPlayer aranarthPlayer = AranarthUtils.getPlayer(p.getUniqueId());
						aranarthPlayer.setNickname("");
						sender.sendMessage(ChatUtils.chatMessage("&e" + p.getName() + "'s &7nickname has been removed!"));
						return true;
					}
				}
			}
			return false;
		} else {
			if (sender instanceof Player) {
				Player player = (Player) sender;
				AranarthPlayer aranarthPlayer = AranarthUtils.getPlayer(player.getUniqueId());
				String nickname = "";
				for (int i = 1; i < args.length; i++) {
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
			return false;
		}
	}
}
