package com.aearost.aranarthcore.commands.general;

import com.aearost.aranarthcore.network.NetworkManager;
import com.aearost.aranarthcore.network.NetworkPlayer;
import com.aearost.aranarthcore.objects.AranarthPlayer;
import com.aearost.aranarthcore.utils.AranarthUtils;
import com.aearost.aranarthcore.utils.ChatUtils;
import com.aearost.aranarthcore.utils.DateUtils;
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
				player.sendMessage(ChatUtils.chatMessage("&cYou cannot use this command!"));
				return true;
			}
		}

		if (args.length == 0) {
			sender.sendMessage(ChatUtils.chatMessage("&cYou must enter a player's username!"));
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

				boolean isOnline = offlinePlayer.isOnline() || remotePlayer != null;
				if (isOnline) {
					sender.sendMessage(ChatUtils.chatMessage(rankPrefix + "&e" + displayName + " &7is currently online"));
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
							sender.sendMessage(ChatUtils.chatMessage(finalRankPrefix + "&e" + finalDisplayName + " &7was last seen " + result));
						});
					} else {
						String result = calculateDisplayDate(offlinePlayer, aranarthPlayer, ZoneId.systemDefault(), sender);
						sender.sendMessage(ChatUtils.chatMessage(rankPrefix + "&e" + displayName + " &7was last seen " + result));
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

		String month = getMonthName(localDateTime.getMonthValue());
		String dateWithSuffix = DateUtils.getDayNumWithSuffix(localDateTime.getDayOfMonth());
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
		return "&e" + month + " " + dateWithSuffix + ", " + year + " &7at &e" + hour + ":" + minute + " " + timezoneName;
	}

}
