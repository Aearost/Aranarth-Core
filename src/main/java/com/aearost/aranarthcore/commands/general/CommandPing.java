package com.aearost.aranarthcore.commands.general;

import com.aearost.aranarthcore.utils.ChatUtils;
import com.aearost.aranarthcore.utils.Lang;
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
					player.sendMessage(ChatUtils.chatMessage(Lang.get("ping.self_good", "ping", String.valueOf(ping))));
				} else if (ping <= 250) {
					player.sendMessage(ChatUtils.chatMessage(Lang.get("ping.self_ok", "ping", String.valueOf(ping))));
				} else {
					player.sendMessage(ChatUtils.chatMessage(Lang.get("ping.self_bad", "ping", String.valueOf(ping))));
				}

				return true;
			} else {
				sender.sendMessage(ChatUtils.chatMessage(Lang.get("general.invalid_syntax", "usage", "ping <player>")));
				return true;
			}
		} else {
            for (Player onlinePlayer : Bukkit.getOnlinePlayers()) {
				// If the user is running the command on themselves
				if (sender.getName().equalsIgnoreCase(args[0])) {
					int ping = onlinePlayer.getPing();
					if (ping <= 150) {
						sender.sendMessage(ChatUtils.chatMessage(Lang.get("ping.self_good", "ping", String.valueOf(ping))));
					} else if (ping <= 250) {
						sender.sendMessage(ChatUtils.chatMessage(Lang.get("ping.self_ok", "ping", String.valueOf(ping))));
					} else {
						sender.sendMessage(ChatUtils.chatMessage(Lang.get("ping.self_bad", "ping", String.valueOf(ping))));
					}
                    return true;
				}
				// If the user is running the command on another player
				else if (onlinePlayer.getName().equalsIgnoreCase(args[0])) {
					int ping = onlinePlayer.getPing();
					if (ping <= 150) {
						sender.sendMessage(ChatUtils.chatMessage(Lang.get("ping.other_good", "player", onlinePlayer.getName(), "ping", String.valueOf(ping))));
					} else if (ping <= 250) {
						sender.sendMessage(ChatUtils.chatMessage(Lang.get("ping.other_ok", "player", onlinePlayer.getName(), "ping", String.valueOf(ping))));
					} else {
						sender.sendMessage(ChatUtils.chatMessage(Lang.get("ping.other_bad", "player", onlinePlayer.getName(), "ping", String.valueOf(ping))));
					}
					return true;
				}
			}
            sender.sendMessage(ChatUtils.chatMessage(Lang.get("player.offline", "name", args[0])));
			return true;
        }
	}

}
