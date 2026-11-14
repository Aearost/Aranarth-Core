package com.aearost.aranarthcore.commands.council;

import com.aearost.aranarthcore.objects.Dominion;
import com.aearost.aranarthcore.objects.Punishment;
import com.aearost.aranarthcore.utils.AranarthUtils;
import com.aearost.aranarthcore.utils.ChatUtils;
import com.aearost.aranarthcore.utils.DominionLevelUtils;
import com.aearost.aranarthcore.utils.Lang;
import com.aearost.aranarthcore.utils.PunishmentUtils;
import com.aearost.aranarthcore.utils.DominionUtils;
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
				player.sendMessage(ChatUtils.chatMessage(Lang.getFor(player, "general.no_permission")));
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
			sender.sendMessage(ChatUtils.chatMessage(Lang.getFor(sender, "general.invalid_syntax", "usage", "ac ban <player> <duration> <reason>")));
			return;
		}

		StringBuilder reasonBuilder = new StringBuilder();
		for (int i = 3; i < args.length; i++) {
			reasonBuilder.append(args[i]);
			if (i < args.length - 1) {
				reasonBuilder.append(" ");
			}
		}

		UUID uuid = AranarthUtils.getUUIDFromUsername(args[1]);
		if (uuid == null) {
			sender.sendMessage(ChatUtils.chatMessage(Lang.getFor(sender, "player.not_found", "name", args[1])));
			return;
		}
		ban(sender, uuid, args[2], reasonBuilder.toString(), null, false);
	}

	/**
	 * Bans the player.
	 * @param sender The user issuing the ban.
	 * @param uuid The UUID of the player being banned.
	 * @param duration The duration of the ban, such as 1h, 1d or 1w, or -1 for a permanent ban.
	 * @param reason The reason of the ban.
	 * @param category The key of the offence category, or null if it was issued without one.
	 * @param notifyCouncil Whether the Council is alerted about the player.
	 * @return Whether the player was banned.
	 */
	public static boolean ban(CommandSender sender, UUID uuid, String duration, String reason, String category, boolean notifyCouncil) {
		UUID senderUuid = null;
		if (sender instanceof Player senderPlayer) {
			senderUuid = senderPlayer.getUniqueId();
		}

		OfflinePlayer player = Bukkit.getOfflinePlayer(uuid);
		String nickname = AranarthUtils.getNickname(player);
		if (!PunishmentUtils.outranks(sender, uuid)) {
			sender.sendMessage(ChatUtils.chatMessage(Lang.getFor(sender, "ban.cannot_ban")));
			return false;
		}

		Date unbanDate = null;
		// If it is not a permanent ban
		if (!duration.equals("-1")) {
			LocalDateTime localDateTime = getEndDateOfBan(duration, sender);
			// If the date could not be determined
			if (localDateTime == null) {
				return false;
			} else {
				unbanDate = Date.from(localDateTime.atZone(ZoneId.systemDefault()).toInstant());
			}
		}

		LocalDateTime currentTime =  LocalDateTime.ofInstant(Instant.now(), ZoneId.systemDefault());
		player.ban(reason, unbanDate, null);
		Punishment punishment = new Punishment(uuid, currentTime, "BAN", reason, senderUuid, category);
		AranarthUtils.addPunishment(uuid, punishment, false);
		PunishmentUtils.issued(punishment, nickname, duration, unbanDate == null ? -1 : unbanDate.getTime(), notifyCouncil);

		// Permanent bans (unbanDate == null) remove the player from their dominion.
		// If they are the leader, the dominion and all its outposts are disbanded.
		if (unbanDate == null) {
			Dominion dominion = DominionUtils.getPlayerDominion(uuid);
			if (dominion != null) {
				if (dominion.getLeader().equals(uuid)) {
					DominionUtils.disbandDominion(dominion);
				} else {
					DominionUtils.removePlayerFromDominion(uuid);
					dominion.getMembers().remove(uuid);
					dominion.getMemberRanks().remove(uuid);
					DominionUtils.updateDominion(dominion);
					DominionLevelUtils.reevaluateDominion(dominion);
				}
			}
		}
		PunishmentUtils.notifyBan(nickname, duration, reason, duration.equals("-1"));
		return true;
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
			sender.sendMessage(ChatUtils.chatMessage(Lang.getFor(sender, "ban.invalid_time_unit")));
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
