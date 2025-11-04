package com.aearost.aranarthcore.commands;

import com.aearost.aranarthcore.objects.AranarthPlayer;
import com.aearost.aranarthcore.utils.AranarthUtils;
import com.aearost.aranarthcore.utils.ChatUtils;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.time.*;
import java.time.format.TextStyle;
import java.util.Locale;
import java.util.UUID;

/**
 * Provides the input player's last known time on the server.
 */
public class CommandSeen {

	/**
	 * @param sender The user that entered the command.
	 * @param args The arguments of the command.
	 */
	public static boolean onCommand(CommandSender sender, String[] args) {
		if (args.length == 1) {
			sender.sendMessage(ChatUtils.chatMessage("&cYou must enter a player's username!"));
			return true;
		} else {
			UUID uuid = AranarthUtils.getUUIDFromUsername(args[1]);
			if (uuid != null) {
				OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(uuid);
				AranarthPlayer aranarthPlayer = AranarthUtils.getPlayer(uuid);
				if (offlinePlayer.isOnline()) {
					sender.sendMessage(ChatUtils.chatMessage("&e" + aranarthPlayer.getNickname() + " &7is currently online"));
					return true;
				} else {
					ZoneId timezone = null;
					if (sender instanceof Player player) {
						AranarthUtils.getPlayerTimezone(player, zoneId -> calculateDisplayDate(offlinePlayer, aranarthPlayer, zoneId, sender));
					} else {
						calculateDisplayDate(offlinePlayer, aranarthPlayer, ZoneId.systemDefault(), sender);
					}
					return true;
				}
			} else {
				sender.sendMessage(ChatUtils.chatMessage("&cThis player could not be found!"));
				return true;
			}
		}
	}

	/**
	 * Provides the name of the input month.
	 * @param month The number of the month.
	 * @return The name of the input month.
	 */
	private static String getMonthName(int month) {
        return switch (month) {
            case 1 -> "January";
            case 2 -> "February";
            case 3 -> "March";
            case 4 -> "April";
            case 5 -> "May";
            case 6 -> "June";
            case 7 -> "July";
            case 8 -> "August";
            case 9 -> "September";
            case 10 -> "October";
            case 11 -> "November";
            case 12 -> "December";
            default -> "NULL";
        };
    }

	/**
	 * Provides the day number with the suffix.
	 * @param day The day.
	 * @return The day with the suffix.
	 */
	private static String getDayNumWithSuffix(int day) {
		String dayNumAsString = day + "";
		if (dayNumAsString.length() > 1) {
			if (dayNumAsString.endsWith("11")) {
				dayNumAsString += "th";
			} else if (dayNumAsString.endsWith("12")) {
				dayNumAsString += "th";
			} else if (dayNumAsString.endsWith("13")) {
				dayNumAsString += "th";
			} else {
				if (dayNumAsString.endsWith("1")) {
					dayNumAsString += "st";
				} else if (dayNumAsString.endsWith("2")) {
					dayNumAsString += "nd";
				} else if (dayNumAsString.endsWith("3")) {
					dayNumAsString += "rd";
				} else {
					dayNumAsString += "th";
				}
			}
		} else {
			if (dayNumAsString.endsWith("1")) {
				dayNumAsString += "st";
			} else if (dayNumAsString.endsWith("2")) {
				dayNumAsString += "nd";
			} else if (dayNumAsString.endsWith("3")) {
				dayNumAsString += "rd";
			} else {
				dayNumAsString += "th";
			}
		}
		return dayNumAsString;
	}

	/**
	 * Determines the date to display in the player's timezone.
	 * @param offlinePlayer The player who is offline.
	 * @param aranarthPlayer The player that executed the command.
	 * @param timezone The player's timezone.
	 * @param sender The sender that executed the command.
	 */
	private static void calculateDisplayDate(OfflinePlayer offlinePlayer, AranarthPlayer aranarthPlayer, ZoneId timezone, CommandSender sender) {
		LocalDateTime localDateTime = null;
		Instant lastPlayed = Instant.ofEpochMilli(offlinePlayer.getLastPlayed());
		if (timezone == null) {
			timezone = ZoneId.systemDefault();
		}
		localDateTime = LocalDateTime.ofInstant(lastPlayed, timezone);

		String month = getMonthName(localDateTime.getMonthValue());
		String dateWithSuffix = getDayNumWithSuffix(localDateTime.getDayOfMonth());
		int year = localDateTime.getYear();

		int hourAsInt = localDateTime.getHour();
		String hour;
		if (hourAsInt < 10) {
			hour = "0" + hourAsInt;
		} else {
			hour = hourAsInt + "";
		}

		int minuteAsInt = localDateTime.getMinute();
		String minute;
		if (minuteAsInt < 10) {
			minute = "0" + minuteAsInt;
		} else {
			minute = minuteAsInt + "";
		}

		if (timezone == null) {

		}
		String timezoneName = timezone.getDisplayName(TextStyle.SHORT, Locale.ENGLISH);
		sender.sendMessage(ChatUtils.chatMessage("&e" + aranarthPlayer.getNickname() + " &7was last seen &e"
				+ month + " " + dateWithSuffix + ", " + year + " &7at &e" + hour + ":" + minute + " " + timezoneName));
	}

}
