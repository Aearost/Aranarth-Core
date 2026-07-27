package com.aearost.aranarthcore.commands.general;

import com.aearost.aranarthcore.network.NetworkManager;
import com.aearost.aranarthcore.objects.AranarthPlayer;
import com.aearost.aranarthcore.utils.AranarthUtils;
import com.aearost.aranarthcore.utils.ChatUtils;
import com.aearost.aranarthcore.utils.PersistenceUtils;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.UUID;

/**
 * Displays the balance of the player or the specified player.
 */
public class CommandBalance implements CommandExecutor {

	/**
	 * @param sender The user that entered the command.
	 * @param command The command itself.
	 * @param alias The alias of the command.
	 * @param args The arguments of the command.
	 * @return Confirmation of whether the command was a success or not.
	 */
	@Override
	public boolean onCommand(CommandSender sender, Command command, String alias, String[] args) {
		NumberFormat formatter = NumberFormat.getCurrencyInstance();
		if (args.length == 0) {
			if (sender instanceof Player player) {
				AranarthPlayer aranarthPlayer = AranarthUtils.getPlayer(player.getUniqueId());
				player.sendMessage(ChatUtils.chatMessage("&7Your balance is &6" + formatter.format(aranarthPlayer.getBalance())));
				return true;
			} else {
				sender.sendMessage(ChatUtils.chatMessage("&cYou must specify a player! /balance [player]"));
			}
		} else {
			if (args.length == 1) {
				boolean isPlayerFound = false;
				// Does the player exist
				for (OfflinePlayer offlinePlayer : Bukkit.getOfflinePlayers()) {
					if (AranarthUtils.getPlayer(offlinePlayer.getUniqueId()) != null) {
						if (offlinePlayer.getName().equalsIgnoreCase(args[0])) {
							AranarthPlayer aranarthPlayer = AranarthUtils.getPlayer(offlinePlayer.getUniqueId());
							sender.sendMessage(ChatUtils.chatMessage("&e" + aranarthPlayer.getNickname() + "&e's &7balance is &6" + formatter.format(aranarthPlayer.getBalance())));
							return true;
						}
					}
				}
				sender.sendMessage(ChatUtils.chatMessage("&cThis player does not exist!"));
			} else if (args.length == 2) {
				if (sender instanceof Player player) {
					AranarthPlayer aranarthPlayer = AranarthUtils.getPlayer(player.getUniqueId());
					// Only Council admins can run this command
					if (aranarthPlayer.getCouncilRank() != 3) {
						player.sendMessage(ChatUtils.chatMessage("&cInvalid syntax: &e/balance [player]"));
						return false;
					}
				}

				boolean isPlayerFound = false;
				// Does the player exist
				for (OfflinePlayer offlinePlayer : Bukkit.getOfflinePlayers()) {
					if (AranarthUtils.getPlayer(offlinePlayer.getUniqueId()) != null) {
						if (offlinePlayer.getName().equalsIgnoreCase(args[0])) {
							AranarthPlayer aranarthPlayer = AranarthUtils.getPlayer(offlinePlayer.getUniqueId());
							try {
								DecimalFormat df = new DecimalFormat("0.00");

								// If increasing the balance
								if (args[1].charAt(0) == '+') {
									double valueAsDouble = Double.parseDouble(args[1].substring(1));
									double delta = Double.parseDouble(df.format(valueAsDouble));
									aranarthPlayer.setBalance(aranarthPlayer.getBalance() + delta);
									sender.sendMessage(ChatUtils.chatMessage("&e" + aranarthPlayer.getNickname() + "&e's &7balance has been increased by &6" + formatter.format(valueAsDouble)));
									persistAndBroadcastBalanceDelta(offlinePlayer.getUniqueId(), delta);
									return true;
								}
								// If decreasing the balance
								else if (args[1].charAt(0) == '-') {
									double valueAsDouble = Double.parseDouble(args[1].substring(1));
									double delta = -Double.parseDouble(df.format(valueAsDouble));
									aranarthPlayer.setBalance(aranarthPlayer.getBalance() + delta);
									sender.sendMessage(ChatUtils.chatMessage("&e" + aranarthPlayer.getNickname() + "&e's &7balance has been decreased by &6" + formatter.format(valueAsDouble)));
									persistAndBroadcastBalanceDelta(offlinePlayer.getUniqueId(), delta);
									return true;
								}
								// If overriding the balance
								else {
									double valueAsDouble = Double.parseDouble(args[1]);
									double newBalance = Double.parseDouble(df.format(valueAsDouble));
									double delta = newBalance - aranarthPlayer.getBalance();
									aranarthPlayer.setBalance(newBalance);
									sender.sendMessage(ChatUtils.chatMessage("&e" + aranarthPlayer.getNickname() + "&e's &7balance has been set to &6" + formatter.format(valueAsDouble)));
									persistAndBroadcastBalanceDelta(offlinePlayer.getUniqueId(), delta);
									return true;
								}
							} catch (NumberFormatException e) {
								sender.sendMessage(ChatUtils.chatMessage("&cThat value is invalid!"));
							}
						}
					}
				}
				sender.sendMessage(ChatUtils.chatMessage("&cThis player does not exist!"));
			}
		}
		return false;
	}

	private void persistAndBroadcastBalanceDelta(UUID uuid, double delta) {
		PersistenceUtils.saveAranarthPlayerImmediately(uuid);
		if (Bukkit.getPlayer(uuid) == null && NetworkManager.isActive()) {
			NetworkManager.getInstance().publishBalanceAdjust(uuid, delta);
		}
	}
}
