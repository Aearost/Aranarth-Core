package com.aearost.aranarthcore.commands;

import com.aearost.aranarthcore.objects.AranarthPlayer;
import com.aearost.aranarthcore.utils.AranarthUtils;
import com.aearost.aranarthcore.utils.ChatUtils;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.text.NumberFormat;

/**
 * Allows the player to pay another player.
 */
public class CommandPay {

	/**
	 * @param sender The user that entered the command.
	 * @param args The arguments of the command.
	 */
	public static boolean onCommand(CommandSender sender, String[] args) {
		if (sender instanceof Player player) {
			if (args.length <= 2) {
				player.sendMessage(ChatUtils.chatMessage("&cInvalid syntax! &e/ac pay <player> <amount>"));
				return true;
			} else {
				OfflinePlayer target = Bukkit.getOfflinePlayer(AranarthUtils.getUUIDFromUsername(args[1]));
				if (target != null) {

					NumberFormat formatter = NumberFormat.getCurrencyInstance();
					String formattedAmount = "";
					double amount = 0.00;

					try {
						amount = Double.parseDouble(args[2]);
						formattedAmount = formatter.format(amount);
						String noCommas = (formattedAmount.substring(1)).replaceAll(",", "");
						amount = Double.parseDouble(noCommas); // The actual value will be two decimals
					} catch (NumberFormatException e) {
						player.sendMessage(ChatUtils.chatMessage("&cThat is not a valid number!"));
						return true;
					}

					if (amount < 0) {
						player.sendMessage(ChatUtils.chatMessage("&cYou cannot pay someone a negative amount!"));
						return true;
					} else if (amount == 0) {
						player.sendMessage(ChatUtils.chatMessage("&cYou cannot pay someone &6$0.00&c!"));
						return true;
					} else {
						AranarthPlayer aranarthPlayerSender = AranarthUtils.getPlayer(player.getUniqueId());
						if (aranarthPlayerSender.getBalance() >= amount) {
							AranarthPlayer aranarthPlayerReceiver = AranarthUtils.getPlayer(target.getUniqueId());
							aranarthPlayerSender.setBalance(aranarthPlayerSender.getBalance() - amount);
							aranarthPlayerReceiver.setBalance(aranarthPlayerReceiver.getBalance() + amount);
							player.sendMessage(ChatUtils.chatMessage("&7You have paid &e" + aranarthPlayerReceiver.getNickname() + " &6" + formattedAmount));
							if (target.isOnline()) {
								Player onlineTarget = Bukkit.getPlayer(target.getUniqueId());
								onlineTarget.sendMessage(ChatUtils.chatMessage("&7You have received &6" + formattedAmount + " &7from &e" + aranarthPlayerSender.getNickname()));
							}
							return true;
						} else {
							player.sendMessage(ChatUtils.chatMessage("&cYou do not have enough money for this!"));
							return true;
						}
					}
				} else {
					player.sendMessage(ChatUtils.chatMessage("&e" + args[1] + " &ccould not be found"));
					return true;
				}
			}
		} else {
			sender.sendMessage(ChatUtils.chatMessage("&cOnly players can execute this command!"));
			return true;
		}
    }

}
