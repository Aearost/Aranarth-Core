package com.aearost.aranarthcore.commands;

import com.aearost.aranarthcore.objects.AranarthPlayer;
import com.aearost.aranarthcore.objects.Punishment;
import com.aearost.aranarthcore.utils.AranarthUtils;
import com.aearost.aranarthcore.utils.ChatUtils;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.List;

/**
 * Lists the punishments of the specified player.
 */
public class CommandPunishments {

	/**
	 * @param sender The user that entered the command.
	 * @param args The arguments of the command.
	 * @return Confirmation of whether the command was a success or not.
	 */
	public static boolean onCommand(CommandSender sender, String[] args) {
		if (sender instanceof Player player) {
			if (player.hasPermission("aranarth.punishments")) {
				listPunishments(sender, args);
			} else {
				player.sendMessage(ChatUtils.chatMessage("&cYou do not have permission to use this command!"));
			}
        } else {
			listPunishments(sender, args);
        }
        return true;
    }

	/**
	 * Helper method to list the input player's punishments.
	 * @param sender The user that entered the command.
	 * @param args The arguments of the command.
	 */
	private static void listPunishments(CommandSender sender, String[] args) {
		if (args.length == 1) {
			sender.sendMessage(ChatUtils.chatMessage("&cInvalid syntax: &e/ac punishments <player>"));
			return;
		}

		for (OfflinePlayer player : Bukkit.getOfflinePlayers()) {
			if (player.getName().equalsIgnoreCase(args[1])) {
				AranarthPlayer aranarthPlayer = AranarthUtils.getPlayer(player.getUniqueId());
				List<Punishment> punishments = AranarthUtils.getPunishments(player.getUniqueId());
				if (punishments == null || punishments.isEmpty()) {
					sender.sendMessage(ChatUtils.chatMessage("&e" + aranarthPlayer.getNickname() + " &7has no logged punishments"));
					return;
				} else {
					sender.sendMessage(ChatUtils.translateToColor("&8      - - - &6" + aranarthPlayer.getNickname() + "'s &6Punishments &8- - -"));
					for (int i = 0; i < punishments.size(); i++) {
						Punishment punishment = punishments.get(i);
						String type = switch (punishment.getType()) {
                            case "WARN" -> "Warned";
                            case "MUTE" -> "Muted";
                            case "BAN" -> "Banned";
                            default -> "Punished";
                        };

                        String appliedBy = "Console";
						if (punishment.getAppliedBy() != null) {
							appliedBy = AranarthUtils.getNickname(Bukkit.getOfflinePlayer(punishment.getAppliedBy()));
						}
						String year = punishment.getDate().getYear() + "";
						String month = punishment.getDate().getMonthValue() + "";
						if (month.length() == 1) {
							month = "0" + month;
						}
						String day = punishment.getDate().getDayOfMonth() + "";
						if (day.length() == 1) {
							day = "0" + month;
						}
						String hour = punishment.getDate().getHour() + "";
						if (hour.length() == 1) {
							hour = "0" + hour;
						}
						String minute = punishment.getDate().getMinute() + "";
						if (minute.length() == 1) {
							minute = "0" + minute;
						}
						String dayOfYear = month + "/" + day + "/" + year;
						String time = hour + ":" + minute;

						sender.sendMessage(ChatUtils.translateToColor(
								"&8&l[&6&l" + (i + 1) + "&8&l] &e" // Prefix and number of warnings
								+ type + " &7by &e" + appliedBy // Type of punishment and who applied it
								+ " &7| &e" + dayOfYear + " " + time // The date and time of the punishment
								+ " &7| &e" + punishment.getReason())); // The reason for the punishment
					}
					return;
				}
			}
		}

		sender.sendMessage(ChatUtils.chatMessage("&e" + args[1] + " &ccould not be found"));
	}


}
