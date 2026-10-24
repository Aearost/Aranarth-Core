package com.aearost.aranarthcore.commands.general;

import com.aearost.aranarthcore.network.NetworkManager;
import com.aearost.aranarthcore.network.NetworkPlayer;
import com.aearost.aranarthcore.objects.AranarthPlayer;
import com.aearost.aranarthcore.utils.AranarthUtils;
import com.aearost.aranarthcore.utils.ChatUtils;
import com.aearost.aranarthcore.utils.DateUtils;
import com.aearost.aranarthcore.utils.Lang;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.TextStyle;
import java.util.Locale;
import java.util.UUID;

/**
 * Provides the input player's last known time on the server.
 */
public class CommandSeen implements CommandExecutor {

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
			if (!player.hasPermission("aranarth.seen")) {
				player.sendMessage(ChatUtils.chatMessage(Lang.getFor(player, "seen.cannot_use")));
				return true;
			}
		}

		if (args.length == 0) {
			sender.sendMessage(ChatUtils.chatMessage(Lang.getFor(sender, "general.invalid_syntax", "usage", "seen <player>")));
			return true;
		} else {
			UUID uuid = AranarthUtils.getUUIDFromUsernameOrNickname(args[0]);
			if (uuid != null) {
				OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(uuid);
				AranarthPlayer aranarthPlayer = AranarthUtils.getPlayer(uuid);

				// Check if the player is online on another server in the network
				NetworkPlayer remotePlayer = NetworkManager.isActive()
						? NetworkManager.getInstance().getRemotePlayer(uuid) : null;

				// Resolve display name regardless of which server the player is on
				String rankPrefix;
				String displayName;
				if (aranarthPlayer != null) {
					rankPrefix = AranarthUtils.getRank(aranarthPlayer);
					displayName = aranarthPlayer.getNickname();
				} else if (remotePlayer != null) {
					rankPrefix = "";
					String remoteNick = remotePlayer.getNickname();
					displayName = (remoteNick == null || remoteNick.isEmpty())
							? remotePlayer.getUsername()
							: ChatUtils.stripColorFormatting(remoteNick);
				} else {
					rankPrefix = "";
					String bukkit = offlinePlayer.getName();
					displayName = bukkit != null ? bukkit : args[0];
				}

				boolean isOnline = AranarthUtils.isVisiblyOnline(sender, uuid);
				if (isOnline) {
					sender.sendMessage(ChatUtils.chatMessage(Lang.getFor(sender, "seen.online", "prefix", rankPrefix, "player", displayName)));
					return true;
				} else {
					if (sender instanceof Player player) {
						final String finalRankPrefix = rankPrefix;
						final String finalDisplayName = displayName;
						AranarthUtils.getPlayerTimezone(player, zoneId -> {
							String result = calculateDisplayDate(
								offlinePlayer,
								aranarthPlayer,
								zoneId,
								sender
							);
							sender.sendMessage(ChatUtils.chatMessage(Lang.getFor(sender, "seen.last_seen", "prefix", finalRankPrefix, "player", finalDisplayName, "date", result)));
						});
					} else {
						String result = calculateDisplayDate(offlinePlayer, aranarthPlayer, ZoneId.systemDefault(), sender);
						sender.sendMessage(ChatUtils.chatMessage(Lang.getFor(sender, "seen.last_seen", "prefix", rankPrefix, "player", displayName, "date", result)));
					}
					return true;
				}
			} else {
				sender.sendMessage(ChatUtils.chatMessage(Lang.getFor(sender, "player.not_found", "name", args[0])));
				return true;
			}
		}
	}

	/**
	 * Provides the lang key of the name of the input month.
	 * @param month The number of the month.
	 * @return The lang key of the name of the input month.
	 */
	private static String getMonthName(int month) {
        return switch (month) {
            case 1 -> "date.month_january";
            case 2 -> "date.month_february";
            case 3 -> "date.month_march";
            case 4 -> "date.month_april";
            case 5 -> "date.month_may";
            case 6 -> "date.month_june";
            case 7 -> "date.month_july";
            case 8 -> "date.month_august";
            case 9 -> "date.month_september";
            case 10 -> "date.month_october";
            case 11 -> "date.month_november";
            case 12 -> "date.month_december";
            default -> "NULL";
        };
    }


	/**
	 * Determines the date to display in the player's timezone.
	 * @param offlinePlayer The player who is offline.
	 * @param aranarthPlayer The player that executed the command.
	 * @param timezone The player's timezone.
	 * @param sender The sender that executed the command.
	 */
	public static String calculateDisplayDate(OfflinePlayer offlinePlayer, AranarthPlayer aranarthPlayer, ZoneId timezone, CommandSender sender) {
		LocalDateTime localDateTime = null;
		Instant lastPlayed = Instant.ofEpochMilli(offlinePlayer.getLastPlayed());
		if (timezone == null) {
			timezone = ZoneId.systemDefault();
		}
		localDateTime = LocalDateTime.ofInstant(lastPlayed, timezone);

		String month = Lang.getFor(sender, getMonthName(localDateTime.getMonthValue()));
		String dateWithSuffix = DateUtils.getDayNumWithSuffix(sender, localDateTime.getDayOfMonth());
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
		return Lang.getFor(sender, "seen.date_format", "month", month, "day", dateWithSuffix, "year", year, "time", hour + ":" + minute, "zone", timezoneName);
	}

}
