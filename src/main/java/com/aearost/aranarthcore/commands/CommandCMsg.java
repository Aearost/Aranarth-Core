package com.aearost.aranarthcore.commands;

import com.aearost.aranarthcore.objects.AranarthPlayer;
import com.aearost.aranarthcore.utils.AranarthUtils;
import com.aearost.aranarthcore.utils.ChatUtils;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/**
 * Allows a player to message the council.
 */
public class CommandCMsg {

	/**
	 * @param sender The user that entered the command.
	 * @param args The arguments of the command.
	 */
	public static boolean onCommand(CommandSender sender, String[] args) {
		if (sender instanceof Player player) {
			AranarthPlayer aranarthPlayer = AranarthUtils.getPlayer(player.getUniqueId());
			if (aranarthPlayer.getCouncilRank() == 0) {
				player.sendMessage(ChatUtils.chatMessage("&cYou do not have permission to use this command!"));
				return false;
			}

			evaluateMessage(sender, args);
			return true;
		} else {
			evaluateMessage(sender, args);
			return true;
		}
	}

	private static void evaluateMessage(CommandSender sender, String[] args) {
		if (args.length == 1) {
			sender.sendMessage(ChatUtils.chatMessage("&cInvalid syntax: &e/ac cmsg <message>"));
		} else {
			StringBuilder msg = new StringBuilder();
			for (int i = 1; i < args.length; i++) {
				msg.append(args[i]);
				if (i < args.length - 1) {
					msg.append(" ");
				}
			}
			String assembledMsg = msg.toString();

			String nickname = "";
			Player player = null;
			if (sender instanceof Player) {
				player = (Player) sender;
				nickname = AranarthUtils.getPlayer(player.getUniqueId()).getNickname();
			} else {
				nickname = "&4&lCONSOLE";
			}

			String prefixStart = "&7⊰&r";
			String prefixEnd = "&7⊱&r";
			String prefixReceive = ChatUtils.translateToColor(prefixStart + "&8&lCouncil &e" + nickname + prefixEnd + " &7&o>> &6&o");
			String prefixSend = ChatUtils.translateToColor(prefixStart + "&8&lCouncil &e" + nickname + prefixEnd + " &7&o>> &6&o");

			sender.sendMessage(ChatUtils.translateToColor(prefixSend + assembledMsg));
			for (Player onlinePlayer : Bukkit.getOnlinePlayers()) {
				AranarthPlayer onlineAranarthPlayer = AranarthUtils.getPlayer(onlinePlayer.getUniqueId());
				if (onlineAranarthPlayer.getCouncilRank() > 0) {
					if (onlinePlayer.getUniqueId().equals(player.getUniqueId())) {
						continue;
					}
					onlinePlayer.sendMessage(ChatUtils.translateToColor(prefixReceive + assembledMsg));
				}
			}
		}
	}
}
