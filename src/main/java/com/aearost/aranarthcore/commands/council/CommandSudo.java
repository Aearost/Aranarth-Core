package com.aearost.aranarthcore.commands.council;

import com.aearost.aranarthcore.objects.AranarthPlayer;
import com.aearost.aranarthcore.utils.AranarthUtils;
import com.aearost.aranarthcore.utils.ChatUtils;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/**
 * Runs a command on behalf of the input player.
 */
public class CommandSudo {

	/**
	 * @param sender The user that entered the command.
	 * @param args The arguments of the command.
	 */
	public static boolean onCommand(CommandSender sender, String[] args) {
		if (sender instanceof Player player) {
			if (!player.hasPermission("aranarth.sudo")) {
				player.sendMessage(ChatUtils.chatMessage("&cYou do not have permission to use this command!"));
				return true;
			}
		}

		if (args.length >= 3) {
			for (Player player : Bukkit.getOnlinePlayers()) {
				if (player.getName().equalsIgnoreCase(args[1])) {
					String command = "";
					for (int i = 2; i < args.length; i++) {
						command += args[i];
						if (i == 2 && args[i].startsWith("/")) {
							command = command.replaceAll("/", "");
						}

						if (i < args.length - 1) {
							command += " ";
						}
					}

					player.performCommand(command);
					AranarthPlayer aranarthPlayer = AranarthUtils.getPlayer(player.getUniqueId());
					sender.sendMessage(ChatUtils.chatMessage("&e" + aranarthPlayer.getNickname() + " &7has run &e/" + command));
					return true;
				} else {
					sender.sendMessage(ChatUtils.chatMessage("&cThis player could not be found!"));
				}
			}

			return false;
		} else {
			sender.sendMessage(ChatUtils.chatMessage("&cInvalid syntax: &e/ac sudo <player> <command>"));
			return true;
		}
	}

}
