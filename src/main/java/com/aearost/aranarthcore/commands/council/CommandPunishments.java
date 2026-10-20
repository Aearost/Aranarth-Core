package com.aearost.aranarthcore.commands.council;

import com.aearost.aranarthcore.objects.AranarthPlayer;
import com.aearost.aranarthcore.objects.Punishment;
import com.aearost.aranarthcore.utils.AranarthUtils;
import com.aearost.aranarthcore.utils.ChatUtils;
import com.aearost.aranarthcore.utils.DiscordUtils;
import com.aearost.aranarthcore.utils.Lang;
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
					sender.sendMessage(ChatUtils.chatMessage(Lang.getFor(sender, "general.invalid_syntax", "usage", "ac punishments <player> [<remove> <number>]")));
					return true;
				}
				// List the punishments
				if (args.length == 2) {
					listPunishments(sender, args);
				} else {
					removePunishment(sender, args);
				}
			} else {
				player.sendMessage(ChatUtils.chatMessage(Lang.getFor(player, "general.no_permission")));
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
					sender.sendMessage(ChatUtils.chatMessage(Lang.getFor(sender, "punishment.none", "player", aranarthPlayer.getNickname())));
					return;
				} else {
					sender.sendMessage(ChatUtils.translateToColor(Lang.getFor(sender, "punishment.list_header", "player", aranarthPlayer.getNickname())));
					for (int i = 0; i < punishments.size(); i++) {
						Punishment punishment = punishments.get(i);
						String type = Lang.getFor(sender, switch (punishment.getType()) {
							case "WARN" -> "punishment.type_warn";
							case "MUTE" -> "punishment.type_mute";
							case "BAN" -> "punishment.type_ban";
							case "UNMUTE" -> "punishment.type_unmute";
							case "UNBAN" -> "punishment.type_unban";
							default -> "punishment.type_other";
						});

						String appliedBy = Lang.getFor(sender, "punishment.console");
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
							day = "0" + day;
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

						sender.sendMessage(ChatUtils.translateToColor(Lang.getFor(sender, "punishment.list_entry", "number", i + 1,
								"type", type, "by", appliedBy, "date", dayOfYear, "time", time, "reason", punishment.getReason())));
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
					sender.sendMessage(ChatUtils.chatMessage(Lang.getFor(sender, "punishment.none", "player", aranarthPlayer.getNickname())));
					return;
				} else {
					if (args.length < 4) {
						sender.sendMessage(ChatUtils.chatMessage(Lang.getFor(sender, "general.invalid_syntax", "usage", "ac punishments <player> [<remove> <number>]")));
						return;
					}

					if (!args[2].equals("remove")) {
						sender.sendMessage(ChatUtils.chatMessage(Lang.getFor(sender, "general.invalid_syntax", "usage", "ac punishments <player> [<remove> <number>]")));
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
								sender.sendMessage(ChatUtils.chatMessage(Lang.getFor(sender, "punishment.removed", "number", String.valueOf(slotToRemove), "player", aranarthPlayer.getNickname())));
							} else {
								throw new NumberFormatException();
							}
						} else {
							throw new NumberFormatException();
						}
					} catch (NumberFormatException e) {
						sender.sendMessage(ChatUtils.chatMessage(Lang.getFor(sender, "punishment.invalid_number")));
						return;
					}
				}
			}
		}
	}

}
