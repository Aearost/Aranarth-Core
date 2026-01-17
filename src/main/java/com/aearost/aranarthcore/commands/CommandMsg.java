package com.aearost.aranarthcore.commands;

import com.aearost.aranarthcore.objects.AranarthPlayer;
import com.aearost.aranarthcore.utils.AranarthUtils;
import com.aearost.aranarthcore.utils.ChatUtils;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.UUID;

/**
 * Allows a player to send a private message to another player.
 */
public class CommandMsg {

	/**
	 * @param sender The user that entered the command.
	 * @param args The arguments of the command.
	 */
	public static boolean onCommand(CommandSender sender, String[] args) {
		if (sender instanceof Player player) {
			if (args.length <= 2) {
				player.sendMessage(ChatUtils.chatMessage("&cInvalid syntax: &e/ac msg <player> <message>"));
				return true;
			} else {
				UUID targetUuid = AranarthUtils.getUUIDFromUsername(args[1]);
				// If the player has played before
				if (targetUuid != null) {
					Player target = Bukkit.getPlayer(targetUuid);
					AranarthPlayer aranarthPlayer = AranarthUtils.getPlayer(player.getUniqueId());
					AranarthPlayer targetAranarthPlayer = AranarthUtils.getPlayer(targetUuid);

					// If the player is online
					if (target != null) {
						StringBuilder msg = new StringBuilder();
						for (int i = 2; i < args.length; i++) {
							msg.append(args[i]);
							if (i < args.length - 1) {
								msg.append(" ");
							}
						}
						String assembledMsg = msg.toString();

						if (ChatUtils.isPlayerMuted(player)) {
							player.sendMessage(ChatUtils.chatMessage("&cYou are muted and cannot send messages!"));
							return true;
						}

						String prefixStart = "&7⊰&r";
						String prefixEnd = "&7⊱&r";
						String senderPrefix = ChatUtils.translateToColor(prefixStart + "&7&l&oTo: &r&e" + targetAranarthPlayer.getNickname() + prefixEnd + " &7&o>> ");
						String targetPrefix = ChatUtils.translateToColor(prefixStart + "&7&l&oFrom: &r&e" + aranarthPlayer.getNickname() + prefixEnd + " &7&o>> &e&o");

						// Formats to color if the player sending has the permissions
						String formattedMsg = ChatUtils.formatChatMessage(player, assembledMsg);

						player.sendMessage(ChatUtils.translateToColor(senderPrefix + formattedMsg));
						target.sendMessage(ChatUtils.translateToColor(targetPrefix + formattedMsg));

						String adminPrefix = prefixStart + "&r&e" + aranarthPlayer.getNickname() + " &7&o>> &r&e&o" + targetAranarthPlayer.getNickname() + prefixEnd + " &c&o";
						for (Player onlinePlayer : Bukkit.getOnlinePlayers()) {
							AranarthPlayer onlineAranarthPlayer = AranarthUtils.getPlayer(onlinePlayer.getUniqueId());
							if (onlineAranarthPlayer.isInAdminMode()) {
								onlinePlayer.sendMessage(ChatUtils.translateToColor("&8&l[&4&lSPY&8&l] " + adminPrefix + formattedMsg));
							}
						}

						return true;
					} else {
						player.sendMessage(ChatUtils.chatMessage("&e" + targetAranarthPlayer.getNickname() + " &cis not online"));
						return true;
					}
				} else {
					player.sendMessage(ChatUtils.chatMessage("&e" + args[1] + " &ccould not be found"));
					return true;
				}
			}
		} else {
			sender.sendMessage(ChatUtils.chatMessage("&cThis command can only be used by players!"));
			return true;
		}
	}
}
