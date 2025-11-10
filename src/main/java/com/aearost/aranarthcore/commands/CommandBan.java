package com.aearost.aranarthcore.commands;

import com.aearost.aranarthcore.objects.AranarthPlayer;
import com.aearost.aranarthcore.objects.Punishment;
import com.aearost.aranarthcore.utils.AranarthUtils;
import com.aearost.aranarthcore.utils.ChatUtils;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.UUID;

/**
 * Bans the specified player.
 */
public class CommandBan {

	/**
	 * @param sender The user that entered the command.
	 * @param args The arguments of the command.
	 * @return Confirmation of whether the command was a success or not.
	 */
	public static boolean onCommand(CommandSender sender, String[] args) {
		if (sender instanceof Player player) {
			if (player.hasPermission("aranarth.ban")) {
				banPlayer(sender, args);
			} else {
				player.sendMessage(ChatUtils.chatMessage("&cYou do not have permission to use this command!"));
			}
        } else {
			banPlayer(sender, args);
        }
        return true;
    }

	/**
     * Helper method to ban the input player.
	 * @param sender The user that entered the command.
	 * @param args The arguments of the command.
	 */
	private static void banPlayer(CommandSender sender, String[] args) {
		if (args.length == 1) {
			sender.sendMessage(ChatUtils.chatMessage("&cYou must specify a player to ban!"));
			return;
		}

		UUID senderUuid = null;
		if (sender instanceof Player senderPlayer) {
			senderUuid = senderPlayer.getUniqueId();
		}

		boolean wasPlayerBanned = false;
		String playerName = args[1];
		String nickname = "";
		for (OfflinePlayer player : Bukkit.getOfflinePlayers()) {
			if (player.getName().equalsIgnoreCase(args[1])) {
				wasPlayerBanned = true;
				playerName = player.getName();
				nickname = AranarthUtils.getNickname(player);
				AranarthPlayer aranarthPlayer = AranarthUtils.getPlayer(player.getUniqueId());

				// Permanent ban
				if (args.length == 2) {
					// Additionally will ban the player's IP
					if (player.isOnline()) {
						Player bannedOnlinePlayer = Bukkit.getPlayer(player.getUniqueId());
						Punishment punishment = new Punishment(bannedOnlinePlayer.getUniqueId(), LocalDateTime.ofInstant(Instant.now(),
								ZoneId.systemDefault()), "ban", "Permanent ban", senderUuid);
						AranarthUtils.addPunishment(bannedOnlinePlayer.getUniqueId(), punishment);
						bannedOnlinePlayer.banIp("You have been banned from Aranarth", (Duration) null, null, true);
						for (Player online : Bukkit.getOnlinePlayers()) {
							if (online.getAddress().getAddress().equals(bannedOnlinePlayer.getAddress().getAddress())) {
								if (online.equals(bannedOnlinePlayer)) {
									continue;
								}
								Punishment otherOnlinePlayerPunishment = new Punishment(online.getUniqueId(), LocalDateTime.ofInstant(Instant.now(),
										ZoneId.systemDefault()), "ban", "Permanent ban", senderUuid);
								AranarthUtils.addPunishment(online.getUniqueId(), otherOnlinePlayerPunishment);
								online.kickPlayer("You have been banned from Aranarth due to " + bannedOnlinePlayer.getName() + "'s actions!");
							}
						}
					} else {
						Punishment punishment = new Punishment(player.getUniqueId(), LocalDateTime.ofInstant(Instant.now(),
								ZoneId.systemDefault()), "ban", "Permanent ban", senderUuid);
						AranarthUtils.addPunishment(player.getUniqueId(), punishment);
						player.ban("You have been banned from Aranarth", (Duration) null, null);
					}

				}
				// Temporary ban
				else {
					char last = args[2].charAt(args[2].length() - 1);
					String timeAsString = args[2].substring(0, args[2].length() - 1);
					int time = 0;
					try {
						time = Integer.parseInt(timeAsString);
					} catch (NumberFormatException e) {
						sender.sendMessage(ChatUtils.chatMessage("&cThat is not a valid number!"));
						return;
					}

					LocalDateTime date = LocalDateTime.now();

					// Minute
					if (last == 'm') {
						date = date.plusMinutes(time);
					}
					// Hour
					else if (last == 'h') {
						date = date.plusHours(time);
					}
					// Day
					else if (last == 'd') {
						date = date.plusDays(time);
					}
					// Week
					else if (last == 'w') {
						date = date.plusWeeks(time);
					} else {
						sender.sendMessage(ChatUtils.chatMessage("&cThat is not a valid variable of time!"));
						return;
					}

					// Duration and reason was specified
					if (args.length >= 4) {
						StringBuilder reason = new StringBuilder();
						for (int i = 3; i < args.length; i++) {
							reason.append(args[i]);
							if (i < args.length - 1) {
								reason.append(" ");
							}
						}

						if (player.isOnline()) {
							Player bannedOnlinePlayer = Bukkit.getPlayer(player.getUniqueId());
							Punishment punishment = new Punishment(bannedOnlinePlayer.getUniqueId(), LocalDateTime.ofInstant(Instant.now(),
									ZoneId.systemDefault()), "ban", reason.toString(), senderUuid);
							AranarthUtils.addPunishment(bannedOnlinePlayer.getUniqueId(), punishment);
							bannedOnlinePlayer.banIp("You have been banned from Aranarth", date.atZone(ZoneId.systemDefault()).toInstant(), null, true);
							for (Player online : Bukkit.getOnlinePlayers()) {
								if (online.getAddress().getAddress().equals(bannedOnlinePlayer.getAddress().getAddress())) {
									if (online.equals(bannedOnlinePlayer)) {
										continue;
									}
									Punishment otherOnlinePlayerPunishment = new Punishment(online.getUniqueId(), LocalDateTime.ofInstant(Instant.now(),
											ZoneId.systemDefault()), "ban", reason.toString(), senderUuid);
									AranarthUtils.addPunishment(online.getUniqueId(), otherOnlinePlayerPunishment);
									online.kickPlayer("You have been banned from Aranarth due to " + bannedOnlinePlayer.getName() + "'s actions!");
								}
							}
						} else {
							Punishment punishment = new Punishment(player.getUniqueId(), LocalDateTime.ofInstant(Instant.now(),
									ZoneId.systemDefault()), "ban", reason.toString(), senderUuid);
							AranarthUtils.addPunishment(player.getUniqueId(), punishment);
							player.ban("You have been banned from Aranarth", date.atZone(ZoneId.systemDefault()).toInstant(), null);
						}
					} else {
						sender.sendMessage(ChatUtils.chatMessage("&cYou must specify a duration and a reason for the ban!"));
						return;
					}
				}
			}
		}

		if (wasPlayerBanned) {
			sender.sendMessage(ChatUtils.chatMessage("&e" + nickname + " &7has been banned"));
		} else {
			sender.sendMessage(ChatUtils.chatMessage("&e" + args[1]) + " &ccould not be found");
		}
	}

	/**
	 * Appends a zero before the value if it is less than 10.
	 * @param value The value.
	 * @return The value with an appended zero if applicable.
	 */
	private static String appendZero(int value) {
		if (value < 10) {
			return "0" + value;
		} else {
			return "" + value;
		}
	}

}
