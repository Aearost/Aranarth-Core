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
				player.sendMessage(ChatUtils.chatMessage(Lang.get("general.no_permission")));
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
			sender.sendMessage(ChatUtils.chatMessage(Lang.get("general.invalid_syntax", "usage", "ac warn <player> <reason>")));
			return;
		}

		UUID senderUuid = null;
		if (sender instanceof Player senderPlayer) {
			senderUuid = senderPlayer.getUniqueId();
		}

		boolean wasPlayerWarned = false;
		String playerName = args[1];
		String nickname = "";
		UUID targetUuid = AranarthUtils.getUUIDFromUsername(args[1]);
		if (targetUuid != null) {
			wasPlayerWarned = true;
			OfflinePlayer player = Bukkit.getOfflinePlayer(targetUuid);
			AranarthPlayer aranarthPlayer = AranarthUtils.getPlayer(targetUuid);
			playerName = aranarthPlayer.getUsername();
			nickname = AranarthUtils.getNickname(player);
			if (!PunishmentUtils.outranks(sender, targetUuid)) {
				sender.sendMessage(ChatUtils.chatMessage(Lang.get("warn.cannot_warn")));
				return;
			}

			if (args.length >= 3) {
				StringBuilder reason = new StringBuilder();
				for (int i = 2; i < args.length; i++) {
					reason.append(args[i]);
					if (i < args.length - 1) {
						reason.append(" ");
					}
				}

				Punishment punishment = new Punishment(targetUuid, LocalDateTime.ofInstant(Instant.now(),
						ZoneId.systemDefault()), "WARN", reason.toString(), senderUuid);
				AranarthUtils.addPunishment(targetUuid, punishment, false);

				PunishmentUtils.notifyWarn(targetUuid, nickname, reason.toString());
				PunishmentUtils.issued(punishment, nickname, "", -1);
			} else {
				sender.sendMessage(ChatUtils.chatMessage(Lang.get("warn.must_specify_reason")));
				return;
			}
		}

		if (!wasPlayerWarned) {
			sender.sendMessage(ChatUtils.chatMessage(Lang.get("player.not_found", "name", args[1])));
		}
	}


}
