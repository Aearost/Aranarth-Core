package com.aearost.aranarthcore.commands;

import com.aearost.aranarthcore.objects.AranarthPlayer;
import com.aearost.aranarthcore.objects.Punishment;
import com.aearost.aranarthcore.utils.AranarthUtils;
import com.aearost.aranarthcore.utils.ChatUtils;
import com.aearost.aranarthcore.utils.DiscordUtils;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

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
				if (args.length == 1) {
					sender.sendMessage(ChatUtils.chatMessage("&cInvalid syntax: &e/ac punishments <player> [<remove> <number>]"));
					return true;
				}
				// List the punishments
				if (args.length == 2) {
					listPunishments(sender, args);
				} else {
					removePunishment(sender, args);
				}
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
							case "UNMUTE" -> "Unmuted";
							case "UNBAN" -> "Unbanned";
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
	}

	/**
	 * Helper method to remove a punishment from the input player.
	 * @param sender The user that entered the command.
	 * @param args The arguments of the command.
	 */
	private static void removePunishment(CommandSender sender, String[] args) {
		for (OfflinePlayer player : Bukkit.getOfflinePlayers()) {
			if (player.getName().equalsIgnoreCase(args[1])) {
				AranarthPlayer aranarthPlayer = AranarthUtils.getPlayer(player.getUniqueId());
				List<Punishment> punishments = AranarthUtils.getPunishments(player.getUniqueId());
				if (punishments == null || punishments.isEmpty()) {
					sender.sendMessage(ChatUtils.chatMessage("&e" + aranarthPlayer.getNickname() + " &7has no logged punishments"));
					return;
				} else {
					if (args.length < 4) {
						sender.sendMessage(ChatUtils.chatMessage("&cInvalid syntax: &e/ac punishments <player> [<remove> <number>]"));
						return;
					}

					if (!args[2].equals("remove")) {
						sender.sendMessage(ChatUtils.chatMessage("&cInvalid syntax: &e/ac punishments <player> [<remove> <number>]"));
						return;
					}

					int slotToRemove = 0;
					try {
						slotToRemove = Integer.parseInt(args[3]);
						if (slotToRemove > 0) {
							if (punishments.size() >= slotToRemove) {
								String type = punishments.get(slotToRemove - 1).getType();
								UUID senderUuid = null;
								if (sender instanceof Player playerSender) {
									senderUuid = playerSender.getUniqueId();
								}
								Punishment punishmentBeingRemoved = punishments.get(slotToRemove - 1);

								Punishment punishment = new Punishment(
										player.getUniqueId(), LocalDateTime.now(), "REMOVE_" + type, punishmentBeingRemoved.getReason(), senderUuid);
								DiscordUtils.addPunishmentToDiscord(punishment);
								AranarthUtils.removePunishment(player.getUniqueId(), punishmentBeingRemoved);
								sender.sendMessage(ChatUtils.chatMessage("&7You have removed &e" + aranarthPlayer.getNickname() + "&e's &7punishment successfully"));
							} else {
								throw new NumberFormatException();
							}
						} else {
							throw new NumberFormatException();
						}
					} catch (NumberFormatException e) {
						sender.sendMessage(ChatUtils.chatMessage("&cThat punishment number is invalid"));
						return;
					}
				}
			}
		}
	}

}
