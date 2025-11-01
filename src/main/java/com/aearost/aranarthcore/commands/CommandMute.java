package com.aearost.aranarthcore.commands;

import com.aearost.aranarthcore.objects.AranarthPlayer;
import com.aearost.aranarthcore.utils.AranarthUtils;
import com.aearost.aranarthcore.utils.ChatUtils;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.time.LocalDateTime;

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
		if (args.length == 1) {
			sender.sendMessage(ChatUtils.chatMessage("&cYou must specify a player to mute!"));
			return;
		}

		boolean wasPlayerMuted = false;
		String playerName = args[1];
		String nickname = "";
		for (OfflinePlayer player : Bukkit.getOfflinePlayers()) {
			if (player.getName().equalsIgnoreCase(args[1])) {
				wasPlayerMuted = true;
				playerName = player.getName();
				nickname = AranarthUtils.getNickname(player);
				AranarthPlayer aranarthPlayer = AranarthUtils.getPlayer(player.getUniqueId());

				// Permanent mute
				if (args.length == 2) {
					aranarthPlayer.setMuteEndDate("none");
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
					String unmuteDate = "";
					unmuteDate += (year + "").substring(2); // Gets last 2 digits
					unmuteDate += appendZero(month);
					unmuteDate += appendZero(day);
					unmuteDate += appendZero(hour);
					unmuteDate += appendZero(minute);
					aranarthPlayer.setMuteEndDate(unmuteDate);
				}
				AranarthUtils.setPlayer(player.getUniqueId(), aranarthPlayer);
			}
		}

		if (wasPlayerMuted) {
			sender.sendMessage(ChatUtils.chatMessage("&e" + nickname + " &7has been muted"));
			for (Player onlinePlayer : Bukkit.getOnlinePlayers()) {
				if (onlinePlayer.getUniqueId().equals(Bukkit.getOfflinePlayer(playerName).getUniqueId())) {
					onlinePlayer.sendMessage(ChatUtils.chatMessage("&cYou have been muted!"));
				}
			}
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
