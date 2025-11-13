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
import java.util.Date;
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
		if (args.length < 4) {
			sender.sendMessage(ChatUtils.chatMessage("&cInvalid syntax: &e/ac mute <player> <duration> <reason>"));
			return;
		}

		UUID senderUuid = null;
		if (sender instanceof Player senderPlayer) {
			senderUuid = senderPlayer.getUniqueId();
		}

		StringBuilder reasonBuilder = new StringBuilder();
		for (int i = 3; i < args.length; i++) {
			reasonBuilder.append(args[i]);
			if (i < args.length - 1) {
				reasonBuilder.append(" ");
			}
		}
		String reason = reasonBuilder.toString();

		boolean wasPlayerBanned = false;
		String nickname = "";
		OfflinePlayer player = Bukkit.getOfflinePlayer(AranarthUtils.getUUIDFromUsername(args[1]));
		if (player != null) {
			wasPlayerBanned = true;
			nickname = AranarthUtils.getNickname(player);
			AranarthPlayer aranarthPlayer = AranarthUtils.getPlayer(player.getUniqueId());

			Date unbanDate = null;
			// If it is not a permanent ban
			if (!args[2].equals("-1")) {
				LocalDateTime localDateTime = getEndDateOfBan(args[2], sender);
				// If the date could not be determined
				if (localDateTime == null) {
					return;
				} else {
					unbanDate = Date.from(localDateTime.atZone(ZoneId.systemDefault()).toInstant());
				}
			}

			LocalDateTime currentTime =  LocalDateTime.ofInstant(Instant.now(), ZoneId.systemDefault());
			player.ban(reason, unbanDate, null);
			Punishment punishment = new Punishment(player.getUniqueId(), currentTime, "BAN", reason, senderUuid);
			AranarthUtils.addPunishment(player.getUniqueId(), punishment, false);
		} else {
			sender.sendMessage(ChatUtils.chatMessage("&e" + args[1]) + " &ccould not be found");
			return;
		}

		sender.sendMessage(ChatUtils.chatMessage("&e" + nickname + " &7has been banned"));
	}

	/**
	 * Provides the end date of a ban.
	 * @param duration The duration of the ban.
	 * @param sender The sender of the command.
	 * @return The end date of the ban.
	 */
	private static LocalDateTime getEndDateOfBan(String duration, CommandSender sender) {
		char last = duration.charAt(duration.length() - 1);
		String timeAsString = duration.substring(0, duration.length() - 1);
		int time = 0;
		try {
			time = Integer.parseInt(timeAsString);
		} catch (NumberFormatException e) {
			sender.sendMessage(ChatUtils.chatMessage("&cThat is not a valid number!"));
			return null;
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
			return null;
		}

		return date;
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
