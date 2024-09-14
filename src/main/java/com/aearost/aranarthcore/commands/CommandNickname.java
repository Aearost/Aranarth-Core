package com.aearost.aranarthcore.commands;

import com.aearost.aranarthcore.objects.AranarthPlayer;
import com.aearost.aranarthcore.utils.AranarthUtils;
import com.aearost.aranarthcore.utils.ChatUtils;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Objects;

/**
 * Allows players to add nicknames for themselves.
 */
public class CommandNickname {

	/**
	 * @param sender The user that entered the command.
	 * @param args The arguments of the command.
	 * @return Confirmation of whether the command was a success or not.
	 */
	public static boolean onCommand(CommandSender sender, String[] args) {

		if (args.length == 1) {
			if (sender instanceof Player player) {
                AranarthPlayer aranarthPlayer = AranarthUtils.getPlayer(player.getUniqueId());
				aranarthPlayer.setNickname("");
				player.sendMessage(ChatUtils.chatMessage("&7Your nickname has been removed!"));
				return true;
			} else {
				sender.sendMessage(ChatUtils.chatMessage("&cConsole does not have a nickname!"));
				return false;
			}
		} else if (args.length == 2) {
			if (sender instanceof Player player) {
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

				for (Player onlinePlayer : onlinePlayers) {
					// If the player is online
					if (onlinePlayer.getName().equalsIgnoreCase(args[1])) {
						AranarthPlayer aranarthPlayer = AranarthUtils.getPlayer(onlinePlayer.getUniqueId());
						aranarthPlayer.setNickname("");
						sender.sendMessage(ChatUtils.chatMessage("&e" + onlinePlayer.getName() + "'s &7nickname has been removed!"));
						return true;
					}
				}
			}
			return false;
		} else {
			if (sender instanceof Player player) {
                AranarthPlayer aranarthPlayer = AranarthUtils.getPlayer(player.getUniqueId());
				int stringStart = 1;
				if (args[1].equalsIgnoreCase("gradient")) {
					stringStart = 3;
				}

				// Gets the full item name
				StringBuilder nicknameSB = new StringBuilder();
				for (int i = stringStart; i < args.length; i++) {
					nicknameSB.append(args[i]);
					if (i == args.length - 1) {
						break;
					} else {
						nicknameSB.append(" ");
					}
				}

				String nickname = nicknameSB.toString();
				if (args[1].equalsIgnoreCase("gradient")) {
					nickname = ChatUtils.translateToGradient(args[2], nickname);
					if (Objects.isNull(nickname)) {
						player.sendMessage(ChatUtils.chatMessage("&cYour nickname could not be set to a gradient"));
						return false;
					} else {
						aranarthPlayer.setNickname(nickname);
						player.sendMessage(ChatUtils.chatMessage("&cYour nickname has been set to " + nickname));
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
