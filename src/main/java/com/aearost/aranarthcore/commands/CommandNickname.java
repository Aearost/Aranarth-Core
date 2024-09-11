package com.aearost.aranarthcore.commands;

import com.aearost.aranarthcore.objects.AranarthPlayer;
import com.aearost.aranarthcore.utils.AranarthUtils;
import com.aearost.aranarthcore.utils.ChatUtils;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

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
				StringBuilder nickname = new StringBuilder();
				for (int i = 1; i < args.length; i++) {
					if (i < args.length - 1) {
						nickname.append(args[i]).append(" ");
					} else {
						nickname.append(args[i]);
					}
				}
				aranarthPlayer.setNickname(nickname.toString());
				sender.sendMessage(ChatUtils.chatMessage("&7Your nickname has been set to " + nickname));
				return true;
			}
			return false;
		}
	}
}
