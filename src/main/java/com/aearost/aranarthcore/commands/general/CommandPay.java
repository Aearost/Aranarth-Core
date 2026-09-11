package com.aearost.aranarthcore.commands.general;

import com.aearost.aranarthcore.network.NetworkManager;
import com.aearost.aranarthcore.network.NetworkPlayer;
import com.aearost.aranarthcore.objects.AranarthPlayer;
import com.aearost.aranarthcore.utils.AranarthUtils;
import com.aearost.aranarthcore.utils.ChatUtils;
import com.aearost.aranarthcore.utils.Lang;
import com.aearost.aranarthcore.utils.PersistenceUtils;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.text.NumberFormat;
import java.util.UUID;

/**
 * Allows the player to pay another player.
 */
public class CommandPay implements CommandExecutor {

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
			if (args.length <= 1) {
				player.sendMessage(ChatUtils.chatMessage(Lang.get("general.invalid_syntax", "usage", "pay <player> <amount>")));
				return true;
			} else {
				UUID uuid = AranarthUtils.getUUIDFromUsernameOrNickname(args[0]);
				OfflinePlayer target = null;
				if (uuid != null) {
					target = Bukkit.getOfflinePlayer(uuid);
				}

				if (uuid != null && target != null) {
					if (target.getUniqueId().equals(player.getUniqueId())) {
						player.sendMessage(ChatUtils.chatMessage(Lang.get("economy.pay_self")));
						return false;
					}

					NumberFormat formatter = NumberFormat.getCurrencyInstance();
					String formattedAmount = "";
					double amount = 0.00;

					try {
						amount = Double.parseDouble(args[1]);
						formattedAmount = formatter.format(amount);
						String noCommas = (formattedAmount.substring(1)).replaceAll(",", "");
						amount = Double.parseDouble(noCommas); // The actual value will be two decimals
					} catch (NumberFormatException e) {
						player.sendMessage(ChatUtils.chatMessage(Lang.get("general.invalid_number")));
						return true;
					}

					if (amount < 0) {
						player.sendMessage(ChatUtils.chatMessage(Lang.get("economy.pay_negative")));
						return true;
					} else if (amount == 0) {
						player.sendMessage(ChatUtils.chatMessage(Lang.get("economy.pay_zero")));
						return true;
					} else {
						AranarthPlayer aranarthPlayerSender = AranarthUtils.getPlayer(player.getUniqueId());
						AranarthPlayer aranarthPlayerReceiver = AranarthUtils.getPlayer(target.getUniqueId());

						// Check whether the receiver is actually online on THIS server.
						// aranarthPlayerReceiver is non-null for all players (data is loaded from MySQL
						// at startup), so we cannot use null-check to detect cross-server targets.
						boolean receiverOnThisServer = Bukkit.getPlayer(target.getUniqueId()) != null;

						if (!receiverOnThisServer) {
							// Receiver is on another server - route payment through the network
							NetworkPlayer networkReceiver = NetworkManager.isActive()
									? NetworkManager.getInstance().getRemoteRoster().get(target.getUniqueId())
									: null;
							if (networkReceiver == null) {
								// Target is fully offline - pay using local in-memory data and persist
								if (aranarthPlayerReceiver == null) {
									player.sendMessage(ChatUtils.chatMessage(Lang.get("player.not_found", "name", args[0])));
									return true;
								}
								if (aranarthPlayerSender.getBalance() >= amount) {
									aranarthPlayerSender.setBalance(aranarthPlayerSender.getBalance() - amount);
									aranarthPlayerReceiver.setBalance(aranarthPlayerReceiver.getBalance() + amount);
									PersistenceUtils.saveAranarthPlayerImmediately(player.getUniqueId());
									PersistenceUtils.saveAranarthPlayerImmediately(target.getUniqueId());
									player.sendMessage(ChatUtils.chatMessage(Lang.get("economy.paid", "player", aranarthPlayerReceiver.getNickname(), "amount", formattedAmount)));
								} else {
									player.sendMessage(ChatUtils.chatMessage(Lang.get("economy.not_enough")));
								}
								return true;
							}
							if (aranarthPlayerSender.getBalance() >= amount) {
								aranarthPlayerSender.setBalance(aranarthPlayerSender.getBalance() - amount);
								// Also update the receiver's in-memory balance on this server so
								// local /balance lookups reflect the payment immediately.
								aranarthPlayerReceiver.setBalance(aranarthPlayerReceiver.getBalance() + amount);
								PersistenceUtils.saveAranarthPlayerImmediately(player.getUniqueId());
								// Tell the remote server to credit the receiver and debit the sender
								// so both servers show correct balances without waiting for a DB reload.
								NetworkManager.getInstance().publishBalanceAdjust(target.getUniqueId(), amount);
								NetworkManager.getInstance().publishBalanceAdjust(player.getUniqueId(), -amount);
								NetworkManager.getInstance().publishPayNotify(target.getUniqueId(), aranarthPlayerSender.getNickname(), formattedAmount);
								player.sendMessage(ChatUtils.chatMessage(Lang.get("economy.paid", "player", networkReceiver.getNickname(), "amount", formattedAmount)));
							} else {
								player.sendMessage(ChatUtils.chatMessage(Lang.get("economy.not_enough")));
							}
							return true;
						}

						if (aranarthPlayerSender.getBalance() >= amount) {
							aranarthPlayerSender.setBalance(aranarthPlayerSender.getBalance() - amount);
							aranarthPlayerReceiver.setBalance(aranarthPlayerReceiver.getBalance() + amount);
							PersistenceUtils.saveAranarthPlayerImmediately(player.getUniqueId());
							PersistenceUtils.saveAranarthPlayerImmediately(target.getUniqueId());
							player.sendMessage(ChatUtils.chatMessage(Lang.get("economy.paid", "player", aranarthPlayerReceiver.getNickname(), "amount", formattedAmount)));
							Player onlineTarget = Bukkit.getPlayer(target.getUniqueId());
							onlineTarget.sendMessage(ChatUtils.chatMessage(Lang.get("economy.received_payment", "amount", formattedAmount, "player", aranarthPlayerSender.getNickname())));
							return true;
						} else {
							player.sendMessage(ChatUtils.chatMessage(Lang.get("pay.not_enough")));
							return true;
						}
					}
				} else {
					player.sendMessage(ChatUtils.chatMessage(Lang.get("pay.player_not_found", "name", args[0])));
					return true;
				}
			}
		} else {
			sender.sendMessage(ChatUtils.chatMessage(Lang.get("general.no_permission_console")));
			return true;
		}
    }

}
