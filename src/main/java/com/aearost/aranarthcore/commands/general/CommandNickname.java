package com.aearost.aranarthcore.commands.general;

import com.aearost.aranarthcore.objects.AranarthPlayer;
import com.aearost.aranarthcore.utils.AranarthUtils;
import com.aearost.aranarthcore.utils.ChatUtils;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Objects;

/**
 * Allows players to add nicknames for themselves.
 */
public class CommandNickname implements CommandExecutor {

	/**
	 * @param sender The user that entered the command.
	 * @param command The command itself.
	 * @param alias The alias of the command.
	 * @param args The arguments of the command.
	 * @return Confirmation of whether the command was a success or not.
	 */
	@Override
	public boolean onCommand(CommandSender sender, Command command, String alias, String[] args) {

		if (sender instanceof Player player) {
			if (!player.hasPermission("aranarth.nick")) {
				player.sendMessage(ChatUtils.chatMessage("&cYou do not have permission to use this command!"));
				return true;
			}

			if (args.length == 0) {
				AranarthPlayer aranarthPlayer = AranarthUtils.getPlayer(player.getUniqueId());
				aranarthPlayer.setNickname("");
				player.sendMessage(ChatUtils.chatMessage("&7Your nickname has been removed!"));
				return true;

			} else if (args.length == 1) {
				AranarthPlayer aranarthPlayer = AranarthUtils.getPlayer(player.getUniqueId());
				if (args[0].contains("&")) {
					if (!player.hasPermission("aranarth.nick.color")) {
						player.sendMessage(ChatUtils.chatMessage("&cYou do not have permission to use this command!"));
						return true;
					}
				}

				if (args[0].contains("#")) {
					if (!player.hasPermission("aranarth.nick.hex")) {
						player.sendMessage(ChatUtils.chatMessage("&cYou do not have permission to use this command!"));
						return true;
					}
				}

				if (ChatUtils.stripColorFormatting(args[0]).length() > 20) {
					player.sendMessage(ChatUtils.chatMessage("&cThis nickname is too long!"));
					return true;
				} else if (ChatUtils.stripColorFormatting(args[0]).isEmpty()) {
					player.sendMessage(ChatUtils.chatMessage("&cYou must enter a nickname!"));
					return true;
				}

				aranarthPlayer.setNickname(args[0]);
				AranarthUtils.setPlayer(player.getUniqueId(), aranarthPlayer);
				sender.sendMessage(ChatUtils.chatMessage("&7Your nickname has been set to " + args[0]));
				return true;
			} else {
				AranarthPlayer aranarthPlayer = AranarthUtils.getPlayer(player.getUniqueId());
				int stringStart = 0;
				if (args[0].equalsIgnoreCase("gradient") || args[0].equalsIgnoreCase("gradientbold")) {
					if (!player.hasPermission("aranarth.nick.gradient")) {
						player.sendMessage(ChatUtils.chatMessage("&cYou do not have permission to use this command!"));
						return true;
					}

					stringStart = 2;
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
				if (args[0].startsWith("gradient")) {
					if (args[0].equalsIgnoreCase("gradient")) {
						nickname = ChatUtils.translateToGradient(args[1], nickname, false);
					} else if (args[0].equalsIgnoreCase("gradientbold")) {
						nickname = ChatUtils.translateToGradient(args[1], nickname, true);
					}

					if (Objects.nonNull(nickname)) {
						if (nicknameSB.toString().length() > 20) {
							player.sendMessage(ChatUtils.chatMessage("&cThis nickname is too long!"));
							return true;
						} else if (nicknameSB.toString().isEmpty()) {
							player.sendMessage(ChatUtils.chatMessage("&cYou must enter a nickname!"));
							return true;
						}

						aranarthPlayer.setNickname(nickname);
						player.sendMessage(ChatUtils.chatMessage("&7Your nickname has been set to " + nickname));
						return true;
					}
					player.sendMessage(ChatUtils.chatMessage("&cYour nickname could not be set to a gradient"));
					return true;
				}

				if (ChatUtils.stripColorFormatting(nickname).length() > 20) {
					player.sendMessage(ChatUtils.chatMessage("&cThis nickname is too long!"));
					return true;
				} else if (ChatUtils.stripColorFormatting(nickname).isEmpty()) {
					player.sendMessage(ChatUtils.chatMessage("&cYou must enter a nickname!"));
					return true;
				}

				aranarthPlayer.setNickname(nickname);
				sender.sendMessage(ChatUtils.chatMessage("&7Your nickname has been set to " + nickname));
				return true;
			}
		} else {
			sender.sendMessage(ChatUtils.chatMessage("&cYou must be a player to execute this command!"));
			return true;
		}
	}
}
