package com.aearost.aranarthcore.commands.council;

import com.aearost.aranarthcore.objects.AranarthPlayer;
import com.aearost.aranarthcore.objects.Punishment;
import com.aearost.aranarthcore.utils.AranarthUtils;
import com.aearost.aranarthcore.utils.ChatUtils;
import com.aearost.aranarthcore.utils.Lang;
import com.aearost.aranarthcore.utils.PunishmentUtils;
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
				player.sendMessage(ChatUtils.chatMessage(Lang.getFor(player, "general.no_permission")));
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
			sender.sendMessage(ChatUtils.chatMessage(Lang.getFor(sender, "general.invalid_syntax", "usage", "ac mute <player> <duration> <reason>")));
			return;
		}

		UUID uuid = AranarthUtils.getUUIDFromUsername(args[1]);
		if (uuid == null) {
			sender.sendMessage(ChatUtils.chatMessage(Lang.getFor(sender, "player.not_found", "name", args[1])));
			return;
		}

		StringBuilder reason = new StringBuilder();
		for (int i = 3; i < args.length; i++) {
			reason.append(args[i]);
			if (i < args.length - 1) {
				reason.append(" ");
			}
		}
		mute(sender, uuid, args[2], reason.toString(), null, false);
	}

	/**
	 * Mutes the player.
	 * @param sender The user issuing the mute.
	 * @param uuid The UUID of the player being muted.
	 * @param duration The duration of the mute, such as 15m, 1h, 1d or 1w, or -1 for a permanent mute.
	 * @param reason The reason of the mute.
	 * @param category The key of the offence category, or null if it was issued without one.
	 * @param notifyCouncil Whether the Council is alerted about the player.
	 * @return Whether the player was muted.
	 */
	public static boolean mute(CommandSender sender, UUID uuid, String duration, String reason, String category, boolean notifyCouncil) {
		UUID senderUuid = null;
		if (sender instanceof Player senderPlayer) {
			senderUuid = senderPlayer.getUniqueId();
		}

		OfflinePlayer player = Bukkit.getOfflinePlayer(uuid);
		String nickname = AranarthUtils.getNickname(player);
		AranarthPlayer aranarthPlayer = AranarthUtils.getPlayer(uuid);
		if (!PunishmentUtils.outranks(sender, uuid)) {
			sender.sendMessage(ChatUtils.chatMessage(Lang.getFor(sender, "mute.cannot_mute")));
			return false;
		}

		if (!aranarthPlayer.getMuteEndDate().isEmpty()) {
			sender.sendMessage(ChatUtils.chatMessage(Lang.getFor(sender, "player.already_muted", "name", aranarthPlayer.getNickname())));
			return false;
		}

		String unmuteDate = getUnmuteDate(sender, duration);
		if (unmuteDate == null) {
			return false;
		}
		aranarthPlayer.setMuteEndDate(unmuteDate);
		AranarthUtils.setPlayer(uuid, aranarthPlayer);
		AranarthUtils.addMutedPlayer(uuid);

		Punishment punishment = new Punishment(uuid, LocalDateTime.ofInstant(Instant.now(),
				ZoneId.systemDefault()), "MUTE", reason, senderUuid, category);
		AranarthUtils.addPunishment(uuid, punishment, false);

		PunishmentUtils.notifyMute(uuid, nickname, duration, reason);
		PunishmentUtils.issued(punishment, nickname, duration, -1, notifyCouncil);
		return true;
	}

	/**
	 * Provides when the mute ends in the yymmddhhmm format, or "none" for a permanent mute.
	 * @param sender The user issuing the mute, who is told if the duration is invalid.
	 * @param duration The duration of the mute.
	 * @return The end date of the mute, or null if the duration is invalid.
	 */
	private static String getUnmuteDate(CommandSender sender, String duration) {
		// Permanent mute
		if (duration.equals("-1")) {
			return "none";
		}

		char last = duration.charAt(duration.length() - 1);
		String timeAsString = duration.substring(0, duration.length() - 1);
		int time;
		try {
			time = Integer.parseInt(timeAsString);
		} catch (NumberFormatException e) {
			sender.sendMessage(ChatUtils.chatMessage(Lang.getFor(sender, "general.invalid_number")));
			return null;
		}
		if (time <= 0) {
			sender.sendMessage(ChatUtils.chatMessage(Lang.getFor(sender, "general.invalid_number")));
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
			sender.sendMessage(ChatUtils.chatMessage(Lang.getFor(sender, "mute.invalid_time_unit")));
			return null;
		}

		// The end date is stored with a 2-digit year, so a mute must end before 2100
		if (date.getYear() > 2099) {
			sender.sendMessage(ChatUtils.chatMessage(Lang.getFor(sender, "general.invalid_number")));
			return null;
		}

		// Format of yymmddhhmm
		return String.valueOf(date.getYear()).substring(2)
				+ appendZero(date.getMonthValue())
				+ appendZero(date.getDayOfMonth())
				+ appendZero(date.getHour())
				+ appendZero(date.getMinute());
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
