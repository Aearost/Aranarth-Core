package com.aearost.aranarthcore.commands;

import com.aearost.aranarthcore.objects.AranarthPlayer;
import com.aearost.aranarthcore.utils.AranarthUtils;
import com.aearost.aranarthcore.utils.ChatUtils;
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
				return true;
			}
		} else if (args.length == 2) {
			if (sender instanceof Player player) {
                AranarthPlayer aranarthPlayer = AranarthUtils.getPlayer(player.getUniqueId());

				aranarthPlayer.setNickname(args[1]);
				AranarthUtils.setPlayer(player.getUniqueId(), aranarthPlayer);
				sender.sendMessage(ChatUtils.chatMessage("&7Your nickname has been set to " + args[1]));
				return true;
			}
			return false;
		} else {
			if (sender instanceof Player player) {
                AranarthPlayer aranarthPlayer = AranarthUtils.getPlayer(player.getUniqueId());
				int stringStart = 1;
				if (args[1].equalsIgnoreCase("gradient") || args[1].equalsIgnoreCase("gradientbold")) {
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
				if (args[1].startsWith("gradient")) {
					if (args[1].equalsIgnoreCase("gradient")) {
						nickname = ChatUtils.translateToGradient(args[2], nickname, false);
					} else if (args[1].equalsIgnoreCase("gradientbold")) {
						nickname = ChatUtils.translateToGradient(args[2], nickname, true);
					}

					if (Objects.nonNull(nickname)) {
						aranarthPlayer.setNickname(nickname);
						player.sendMessage(ChatUtils.chatMessage("&7Your nickname has been set to " + nickname));
						return true;
					}
					player.sendMessage(ChatUtils.chatMessage("&cYour nickname could not be set to a gradient"));
					return false;
				}

				aranarthPlayer.setNickname(nickname);
				sender.sendMessage(ChatUtils.chatMessage("&7Your nickname has been set to " + nickname));
				return true;
			}
		}
		return false;
	}
}
