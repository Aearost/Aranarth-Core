package com.aearost.aranarthcore.commands;

import com.aearost.aranarthcore.objects.AranarthPlayer;
import com.aearost.aranarthcore.utils.AranarthUtils;
import com.aearost.aranarthcore.utils.ChatUtils;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.text.DecimalFormat;

/**
 * Displays the balance of the player or the specified player.
 */
public class CommandBalance {

	/**
	 * @param sender The user that entered the command.
	 * @param args The arguments of the command.
	 * @return Confirmation of whether the command was a success or not.
	 */
	public static boolean onCommand(CommandSender sender, String[] args) {
		DecimalFormat df = new DecimalFormat("0.00");
		if (args.length >= 1) {
			if (args.length == 1) {
				if (sender instanceof Player player) {
					AranarthPlayer aranarthPlayer = AranarthUtils.getPlayer(player.getUniqueId());
					player.sendMessage(ChatUtils.chatMessage("&7Your balance is &6$" + df.format(aranarthPlayer.getBalance())));
				}
				return true;
			} else {
				if (args.length == 2) {
					boolean isPlayerFound = false;
					// Does the player exist
					for (OfflinePlayer offlinePlayer : Bukkit.getOfflinePlayers()) {
						if (AranarthUtils.getPlayer(offlinePlayer.getUniqueId()) != null) {
							if (offlinePlayer.getName().equalsIgnoreCase(args[1])) {
								AranarthPlayer aranarthPlayer = AranarthUtils.getPlayer(offlinePlayer.getUniqueId());
								sender.sendMessage(ChatUtils.chatMessage("&e" + offlinePlayer.getName() + "'s &7balance is &6$" + df.format(aranarthPlayer.getBalance())));
								isPlayerFound = true;
								break;
							}
						}
					}
					if (!isPlayerFound) {
						sender.sendMessage(ChatUtils.chatMessage("&cThis player does not exist!"));
					}
				} else if (args.length == 3) {
					if (sender instanceof Player player) {
						if (player.getName().equals("Aearost")) {
							boolean isPlayerFound = false;
							// Does the player exist
							for (OfflinePlayer offlinePlayer : Bukkit.getOfflinePlayers()) {
								if (AranarthUtils.getPlayer(offlinePlayer.getUniqueId()) != null) {
									if (offlinePlayer.getName().equalsIgnoreCase(args[1])) {
										AranarthPlayer aranarthPlayer = AranarthUtils.getPlayer(offlinePlayer.getUniqueId());
										try {
											aranarthPlayer.setBalance(Double.parseDouble(df.format(args[2])));
											player.sendMessage(ChatUtils.chatMessage("&e" + offlinePlayer.getName() + "'s balance has been set to &6$" + df.format(args[2])));
											isPlayerFound = true;
										} catch (NumberFormatException e) {
											player.sendMessage(ChatUtils.chatMessage("&cThat value is invalid!"));
										}
									}
								}
							}
							if (!isPlayerFound) {
								sender.sendMessage(ChatUtils.chatMessage("&cThis player does not exist!"));
							}
						} else {
							return false;
						}
					}
				}
				return true;
			}
		}
		return false;
	}

}
