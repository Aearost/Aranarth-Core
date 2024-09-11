package com.aearost.aranarthcore.commands;

import com.aearost.aranarthcore.objects.AranarthPlayer;
import com.aearost.aranarthcore.utils.AranarthUtils;
import com.aearost.aranarthcore.utils.ChatUtils;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/**
 * Allows a player to add a prefix to themselves.
 */
public class CommandPrefix {

	/**
	 * @param sender The user that entered the command.
	 * @param args The arguments of the command.
	 * @return Confirmation of whether the command was a success or not.
	 */
	public static boolean onCommand(CommandSender sender, String[] args) {

		if (args.length == 1) {
			sender.sendMessage(ChatUtils.chatMessage("&cIncorrect syntax: /ac prefix [player] <prefix>"));
			return false;
		} else {
			if (args.length == 2) {
				if (sender instanceof Player player) {
                    AranarthPlayer aranarthPlayer = AranarthUtils.getPlayer(player.getUniqueId());
					
					if (args[1].equalsIgnoreCase("off")) {
						aranarthPlayer.setPrefix("");
						player.sendMessage(ChatUtils.chatMessage("&7Your prefix has been removed!"));
						return true;
					}
					
					aranarthPlayer.setPrefix(args[1]);
					sender.sendMessage(ChatUtils.chatMessage("&7Your prefix has been set to " + args[1]));
					return true;
				} else {
					sender.sendMessage(ChatUtils.chatMessage("&cIncorrect syntax: /ac prefix [player] <prefix>"));
					return false;
				}
			} else {
				Player[] onlinePlayers = new Player[Bukkit.getOnlinePlayers().size()];
				Bukkit.getOnlinePlayers().toArray(onlinePlayers);
				
				for (Player p : onlinePlayers) {
					// If the player is online
					if (p.getName().equalsIgnoreCase(args[1])) {
						AranarthPlayer aranarthPlayer = AranarthUtils.getPlayer(p.getUniqueId());
						
						if (args[2].equalsIgnoreCase("off")) {
							aranarthPlayer.setPrefix("");
							sender.sendMessage(ChatUtils.chatMessage("&e" + p.getName() + "'s &7prefix has been removed!"));
                        } else {
							StringBuilder prefix = new StringBuilder();
							for (int i = 2; i < args.length; i++) {
								if (i < args.length - 1) {
									prefix.append(args[i]).append(" ");
								} else {
									prefix.append(args[i]);
								}
							}
							aranarthPlayer.setPrefix(prefix.toString());
							sender.sendMessage(ChatUtils.chatMessage("&e" + args[1] + "'s &7prefix has been set to " + prefix));
                        }
                        return true;
                    }
				}
				
				if (sender instanceof Player player) {
					// If the player is using a multi-word prefix
                    AranarthPlayer aranarthPlayer = AranarthUtils.getPlayer(player.getUniqueId());
					StringBuilder prefix = new StringBuilder();
					for (int i = 1; i < args.length; i++) {
						if (i < args.length - 1) {
							prefix.append(args[i]).append(" ");
						} else {
							prefix.append(args[i]);
						}
					}
					aranarthPlayer.setPrefix(prefix.toString());
					sender.sendMessage(ChatUtils.chatMessage("&7Your prefix has been set to " + prefix));
					return true;
				}
				
				sender.sendMessage(ChatUtils.chatMessage("&cThat player is not online!"));
				return false;
				
				
			}
		}
	}

}
