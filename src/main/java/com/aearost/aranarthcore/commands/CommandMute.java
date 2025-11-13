package com.aearost.aranarthcore.commands;

import com.aearost.aranarthcore.objects.AranarthPlayer;
import com.aearost.aranarthcore.objects.Punishment;
import com.aearost.aranarthcore.utils.AranarthUtils;
import com.aearost.aranarthcore.utils.ChatUtils;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.UUID;

/**
 * Mutes the specified player.
 */
public class CommandMute {

	/**
	 * @param sender The user that entered the command.
	 * @param args The arguments of the command.
	 * @return Confirmation of whether the command was a success or not.
	 */
	public static boolean onCommand(CommandSender sender, String[] args) {
		if (sender instanceof Player player) {
			if (player.hasPermission("aranarth.mute")) {
				mutePlayer(sender, args);
			} else {
				player.sendMessage(ChatUtils.chatMessage("&cYou do not have permission to use this command!"));
			}
        } else {
			mutePlayer(sender, args);
        }
        return true;
    }

	/**
	 * Helper method to mute the input player.
	 * @param sender The user that entered the command.
	 * @param args The arguments of the command.
	 */
	private static void mutePlayer(CommandSender sender, String[] args) {
		if (args.length < 4) {
			sender.sendMessage(ChatUtils.chatMessage("&cInvalid syntax: &e/ac mute <player> <duration> <reason>"));
			return;
		}

		UUID senderUuid = null;
		if (sender instanceof Player senderPlayer) {
			senderUuid = senderPlayer.getUniqueId();
		}

		boolean wasPlayerMuted = false;
		String nickname = "";
		OfflinePlayer player = Bukkit.getOfflinePlayer(AranarthUtils.getUUIDFromUsername(args[1]));
		if (player != null) {
			wasPlayerMuted = true;
			nickname = AranarthUtils.getNickname(player);
			AranarthPlayer aranarthPlayer = AranarthUtils.getPlayer(player.getUniqueId());
			if (!aranarthPlayer.getMuteEndDate().isEmpty()) {
				sender.sendMessage(ChatUtils.chatMessage("&e" + aranarthPlayer.getNickname() + " &cis already muted!"));
				return;
			}

			String unmuteDate = "";

			// Permanent mute
			if (args[2].equals("-1")) {
				unmuteDate = "none";
			} else {
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

				int year = date.getYear();
				int month = date.getMonthValue();
				int day = date.getDayOfMonth();
				int hour = date.getHour();
				int minute = date.getMinute();

				// Format of yymmddhhmm
				unmuteDate += (year + "").substring(2); // Gets last 2 digits
				unmuteDate += appendZero(month);
				unmuteDate += appendZero(day);
				unmuteDate += appendZero(hour);
				unmuteDate += appendZero(minute);
			}
			aranarthPlayer.setMuteEndDate(unmuteDate);
			AranarthUtils.setPlayer(player.getUniqueId(), aranarthPlayer);
			AranarthUtils.addMutedPlayer(player.getUniqueId());
		} else {
			sender.sendMessage(ChatUtils.chatMessage("&e" + args[1]) + " &ccould not be found");
		}

		if (wasPlayerMuted) {
			StringBuilder reason = new StringBuilder();
			for (int i = 3; i < args.length; i++) {
				reason.append(args[i]);
				if (i < args.length - 1) {
					reason.append(" ");
				}
			}

			Punishment punishment = new Punishment(AranarthUtils.getUUIDFromUsername(args[1]), LocalDateTime.ofInstant(Instant.now(),
					ZoneId.systemDefault()), "MUTE", reason.toString(), senderUuid);
			AranarthUtils.addPunishment(AranarthUtils.getUUIDFromUsername(args[1]), punishment, false);
			sender.sendMessage(ChatUtils.chatMessage("&e" + nickname + " &7has been muted"));

			for (Player onlinePlayer : Bukkit.getOnlinePlayers()) {
				if (onlinePlayer.getUniqueId().equals(Bukkit.getOfflinePlayer(args[1]).getUniqueId())) {
					onlinePlayer.sendMessage(ChatUtils.chatMessage("&cYou have been muted!"));
				}
			}
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
