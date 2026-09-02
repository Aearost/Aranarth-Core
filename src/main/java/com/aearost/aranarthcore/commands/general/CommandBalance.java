package com.aearost.aranarthcore.commands.general;

import com.aearost.aranarthcore.database.DatabaseManager;
import com.aearost.aranarthcore.network.NetworkManager;
import com.aearost.aranarthcore.network.NetworkPlayer;
import com.aearost.aranarthcore.objects.AranarthPlayer;
import com.aearost.aranarthcore.utils.AranarthUtils;
import com.aearost.aranarthcore.utils.ChatUtils;
import com.aearost.aranarthcore.utils.PersistenceUtils;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.Map;
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
				UUID targetUuid = AranarthUtils.getUUIDFromUsernameOrNickname(args[0]);
				if (targetUuid == null) {
					sender.sendMessage(ChatUtils.chatMessage("&cThis player does not exist!"));
					return true;
				}
				String displayName = resolveDisplayName(targetUuid, args[0]);
				double balance = resolveBalance(targetUuid);
				sender.sendMessage(ChatUtils.chatMessage("&e" + displayName + "&e's &7balance is &6" + formatter.format(balance)));
				return true;
			} else if (args.length == 2) {
				if (sender instanceof Player player) {
					AranarthPlayer aranarthPlayer = AranarthUtils.getPlayer(player.getUniqueId());
					// Only Council admins can run this command
					if (aranarthPlayer.getCouncilRank() != 3) {
						player.sendMessage(ChatUtils.chatMessage("&cInvalid syntax: &e/balance [player]"));
						return false;
					}
				}

				UUID targetUuid = AranarthUtils.getUUIDFromUsernameOrNickname(args[0]);
				if (targetUuid == null) {
					sender.sendMessage(ChatUtils.chatMessage("&cThis player does not exist!"));
					return true;
				}
				AranarthPlayer aranarthPlayer = AranarthUtils.getPlayer(targetUuid);
				if (aranarthPlayer == null) {
					sender.sendMessage(ChatUtils.chatMessage("&e" + args[0] + " &cis not on this server - balance changes require the player to be present"));
					return true;
				}
				String displayName = resolveDisplayName(targetUuid, args[0]);
				try {
					DecimalFormat df = new DecimalFormat("0.00");

					// If increasing the balance
					if (args[1].charAt(0) == '+') {
						double valueAsDouble = Double.parseDouble(args[1].substring(1));
						double delta = Double.parseDouble(df.format(valueAsDouble));
						aranarthPlayer.setBalance(aranarthPlayer.getBalance() + delta);
						sender.sendMessage(ChatUtils.chatMessage("&e" + displayName + "&e's &7balance has been increased by &6" + formatter.format(valueAsDouble)));
						persistAndBroadcastBalanceDelta(targetUuid, delta);
						return true;
					}
					// If decreasing the balance
					else if (args[1].charAt(0) == '-') {
						double valueAsDouble = Double.parseDouble(args[1].substring(1));
						double delta = -Double.parseDouble(df.format(valueAsDouble));
						aranarthPlayer.setBalance(aranarthPlayer.getBalance() + delta);
						sender.sendMessage(ChatUtils.chatMessage("&e" + displayName + "&e's &7balance has been decreased by &6" + formatter.format(valueAsDouble)));
						persistAndBroadcastBalanceDelta(targetUuid, delta);
						return true;
					}
					// If overriding the balance
					else {
						double valueAsDouble = Double.parseDouble(args[1]);
						double newBalance = Double.parseDouble(df.format(valueAsDouble));
						double delta = newBalance - aranarthPlayer.getBalance();
						aranarthPlayer.setBalance(newBalance);
						sender.sendMessage(ChatUtils.chatMessage("&e" + displayName + "&e's &7balance has been set to &6" + formatter.format(valueAsDouble)));
						persistAndBroadcastBalanceDelta(targetUuid, delta);
						return true;
					}
				} catch (NumberFormatException e) {
					sender.sendMessage(ChatUtils.chatMessage("&cThat value is invalid!"));
				}
			}
		}
		return false;
	}

	/**
	 * Resolves the display name for a player UUID, checking local memory, remote roster, then Bukkit cache.
	 */
	private static String resolveDisplayName(UUID uuid, String fallback) {
		AranarthPlayer ap = AranarthUtils.getPlayer(uuid);
		if (ap != null) {
			return ap.getNickname();
		}
		if (NetworkManager.isActive()) {
			NetworkPlayer remote = NetworkManager.getInstance().getRemotePlayer(uuid);
			if (remote != null) {
				String nick = remote.getNickname();
				return (nick == null || nick.isEmpty()) ? remote.getUsername() : ChatUtils.stripColorFormatting(nick);
			}
		}
		String bukkit = Bukkit.getOfflinePlayer(uuid).getName();
		return bukkit != null ? bukkit : fallback;
	}

	/**
	 * Resolves the balance for a player UUID, checking local memory first then the DB.
	 */
	private static double resolveBalance(UUID uuid) {
		AranarthPlayer ap = AranarthUtils.getPlayer(uuid);
		if (ap != null) {
			return ap.getBalance();
		}
		if (DatabaseManager.isActive()) {
			Map<UUID, DatabaseManager.BalanceEntry> balances = DatabaseManager.getInstance().loadAllPlayerBalances();
			DatabaseManager.BalanceEntry entry = balances.get(uuid);
			if (entry != null) {
				return entry.balance();
			}
		}
		return 0.0;
	}

	private void persistAndBroadcastBalanceDelta(UUID uuid, double delta) {
		PersistenceUtils.saveAranarthPlayerImmediately(uuid);
		if (NetworkManager.isActive()) {
			NetworkManager.getInstance().publishBalanceAdjust(uuid, delta);
		}
	}
}
