package com.aearost.aranarthcore.commands;

import com.aearost.aranarthcore.utils.ChatUtils;
import com.aearost.aranarthcore.utils.DiscordUtils;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/**
 * Teleports the player back to their last known location.
 */
public class CommandBroadcast {

	/**
	 * @param sender The user that entered the command.
	 * @param args The arguments of the command.
	 */
	public static boolean onCommand(CommandSender sender, String[] args) {
		if (sender instanceof Player player) {
			if (!player.hasPermission("aranarth.broadcast")) {
				player.sendMessage(ChatUtils.chatMessage("&cYou do not have permission to use this command!"));
				return true;
			}
		}

		if (args.length > 1) {
			if (!args[1].isEmpty()) {
				StringBuilder messageBuilder = new StringBuilder();
				for (int i = 1; i < args.length; i++) {
					messageBuilder.append(args[i]);
					if (i < args.length - 1) {
						messageBuilder.append(" ");
					}
				}
				Bukkit.broadcastMessage(ChatUtils.chatMessage(messageBuilder.toString()));
				DiscordUtils.createNotification(ChatUtils.stripColorFormatting(messageBuilder.toString()), null);
				return true;
			} else {
				sender.sendMessage(ChatUtils.chatMessage("&cYou must enter a message to broadcast!"));
				return true;
			}
		} else {
			sender.sendMessage(ChatUtils.chatMessage("&cInvalid syntax: &e/ac broadcast <msg>"));
			return true;
		}


	}

}
