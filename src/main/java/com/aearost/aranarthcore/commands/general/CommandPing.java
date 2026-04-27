package com.aearost.aranarthcore.commands.general;

import com.aearost.aranarthcore.utils.ChatUtils;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/**
 * Allows players to view their own or another player's ping.
 */
public class CommandPing implements CommandExecutor {

	/**
	 * @param sender The user that entered the command.
	 * @param command The command itself.
	 * @param alias The alias of the command.
	 * @param args The arguments of the command.
	 * @return Confirmation of whether the command was a success or not.
	 */
	@Override
	public boolean onCommand(CommandSender sender, Command command, String alias, String[] args) {
		if (args.length == 0) {
			if (sender instanceof Player player) {
				int ping = player.getPing();
				if (ping <= 150) {
					player.sendMessage(ChatUtils.chatMessage("&7Your ping is &a" + ping + "ms"));
				} else if (ping <= 250) {
					player.sendMessage(ChatUtils.chatMessage("&7Your ping is &e" + ping + "ms"));
				} else {
					player.sendMessage(ChatUtils.chatMessage("&7Your ping is &c" + ping + "ms"));
				}

				return true;
			} else {
				sender.sendMessage(ChatUtils.chatMessage("&cYou must specify a player's ping! /ping <player>"));
				return true;
			}
		} else {
            for (Player onlinePlayer : Bukkit.getOnlinePlayers()) {
				// If the user is running the command on themselves
				if (sender.getName().equalsIgnoreCase(args[0])) {
					int ping = onlinePlayer.getPing();
					if (ping <= 150) {
						sender.sendMessage(ChatUtils.chatMessage("&7Your ping is &a" + ping + "ms"));
					} else if (ping <= 250) {
						sender.sendMessage(ChatUtils.chatMessage("&7Your ping is &e" + ping + "ms"));
					} else {
						sender.sendMessage(ChatUtils.chatMessage("&7Your ping is &c" + ping + "ms"));
					}
                    return true;
				}
				// If the user is running the command on another player
				else if (onlinePlayer.getName().equalsIgnoreCase(args[0])) {
					int ping = onlinePlayer.getPing();
					if (ping <= 150) {
						sender.sendMessage(ChatUtils.chatMessage("&e" + onlinePlayer.getName() +"'s &7ping is &a" + ping + "ms"));
					} else if (ping <= 250) {
						sender.sendMessage(ChatUtils.chatMessage("&e" + onlinePlayer.getName() +"'s &7ping is &e" + ping + "ms"));
					} else {
						sender.sendMessage(ChatUtils.chatMessage("&e" + onlinePlayer.getName() +"'s &7ping is &c" + ping + "ms"));
					}
					return true;
				}
			}
            sender.sendMessage(ChatUtils.chatMessage("&cThat player is not online!"));
			return true;
        }
	}

}
