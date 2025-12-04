package com.aearost.aranarthcore.commands;

import com.aearost.aranarthcore.objects.AranarthPlayer;
import com.aearost.aranarthcore.objects.Punishment;
import com.aearost.aranarthcore.utils.AranarthUtils;
import com.aearost.aranarthcore.utils.ChatUtils;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.Sound;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.UUID;

/**
 * Warns the specified player.
 */
public class CommandWarn {

	/**
	 * @param sender The user that entered the command.
	 * @param args The arguments of the command.
	 * @return Confirmation of whether the command was a success or not.
	 */
	public static boolean onCommand(CommandSender sender, String[] args) {
		if (sender instanceof Player player) {
			if (player.hasPermission("aranarth.warn")) {
				warnPlayer(sender, args);
			} else {
				player.sendMessage(ChatUtils.chatMessage("&cYou do not have permission to use this command!"));
			}
        } else {
			warnPlayer(sender, args);
        }
        return true;
    }

	/**
	 * Helper method to warn the input player.
	 * @param sender The user that entered the command.
	 * @param args The arguments of the command.
	 */
	private static void warnPlayer(CommandSender sender, String[] args) {
		if (args.length == 1) {
			sender.sendMessage(ChatUtils.chatMessage("&cInvalid syntax: &e/ac warn <player> <reason>"));
			return;
		}

		UUID senderUuid = null;
		if (sender instanceof Player senderPlayer) {
			senderUuid = senderPlayer.getUniqueId();
		}

		boolean wasPlayerWarned = false;
		String playerName = args[1];
		String nickname = "";
		for (OfflinePlayer player : Bukkit.getOfflinePlayers()) {
			if (player.getName().equalsIgnoreCase(args[1])) {
				wasPlayerWarned = true;
				playerName = player.getName();
				nickname = AranarthUtils.getNickname(player);
				AranarthPlayer aranarthPlayer = AranarthUtils.getPlayer(player.getUniqueId());

				if (args.length >= 3) {
					sender.sendMessage(ChatUtils.chatMessage("&e" + nickname + " &7has been warned"));
					StringBuilder reason = new StringBuilder();
					for (int i = 2; i < args.length; i++) {
						reason.append(args[i]);
						if (i < args.length - 1) {
							reason.append(" ");
						}
					}

					Punishment punishment = new Punishment(AranarthUtils.getUUIDFromUsername(args[1]), LocalDateTime.ofInstant(Instant.now(),
							ZoneId.systemDefault()), "WARN", reason.toString(), senderUuid);
					AranarthUtils.addPunishment(AranarthUtils.getUUIDFromUsername(args[1]), punishment, false);

					for (Player onlinePlayer : Bukkit.getOnlinePlayers()) {
						if (onlinePlayer.getUniqueId().equals(Bukkit.getOfflinePlayer(playerName).getUniqueId())) {
							onlinePlayer.sendMessage(ChatUtils.chatMessage("&cYou have been warned!"));
							onlinePlayer.playSound(onlinePlayer.getLocation(), Sound.ENTITY_GHAST_HURT, 1F, 1.1F);
						}
					}
				} else {
					sender.sendMessage(ChatUtils.chatMessage("&cYou must specify a player and a reason for the warn!"));
					return;
				}
			}
		}

		if (!wasPlayerWarned) {
			sender.sendMessage(ChatUtils.chatMessage("&e" + args[1]) + " &ccould not be found");
		}
	}


}
