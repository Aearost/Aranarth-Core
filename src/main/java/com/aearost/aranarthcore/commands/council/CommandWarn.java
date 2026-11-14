package com.aearost.aranarthcore.commands.council;

import com.aearost.aranarthcore.objects.Punishment;
import com.aearost.aranarthcore.utils.AranarthUtils;
import com.aearost.aranarthcore.utils.ChatUtils;
import com.aearost.aranarthcore.utils.Lang;
import com.aearost.aranarthcore.utils.PunishmentUtils;
import org.bukkit.Bukkit;
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
				player.sendMessage(ChatUtils.chatMessage(Lang.getFor(player, "general.no_permission")));
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
			sender.sendMessage(ChatUtils.chatMessage(Lang.getFor(sender, "general.invalid_syntax", "usage", "ac warn <player> <reason>")));
			return;
		}

		UUID targetUuid = AranarthUtils.getUUIDFromUsername(args[1]);
		if (targetUuid == null) {
			sender.sendMessage(ChatUtils.chatMessage(Lang.getFor(sender, "player.not_found", "name", args[1])));
			return;
		}
		if (args.length < 3) {
			if (PunishmentUtils.outranks(sender, targetUuid)) {
				sender.sendMessage(ChatUtils.chatMessage(Lang.getFor(sender, "warn.must_specify_reason")));
			} else {
				sender.sendMessage(ChatUtils.chatMessage(Lang.getFor(sender, "warn.cannot_warn")));
			}
			return;
		}

		StringBuilder reason = new StringBuilder();
		for (int i = 2; i < args.length; i++) {
			reason.append(args[i]);
			if (i < args.length - 1) {
				reason.append(" ");
			}
		}
		warn(sender, targetUuid, reason.toString(), null, false);
	}

	/**
	 * Warns the player.
	 * @param sender The user issuing the warning.
	 * @param targetUuid The UUID of the player being warned.
	 * @param reason The reason of the warning.
	 * @param category The key of the offence category, or null if it was issued without one.
	 * @param notifyCouncil Whether the Council is alerted about the player.
	 * @return Whether the player was warned.
	 */
	public static boolean warn(CommandSender sender, UUID targetUuid, String reason, String category, boolean notifyCouncil) {
		if (!PunishmentUtils.outranks(sender, targetUuid)) {
			sender.sendMessage(ChatUtils.chatMessage(Lang.getFor(sender, "warn.cannot_warn")));
			return false;
		}

		UUID senderUuid = null;
		if (sender instanceof Player senderPlayer) {
			senderUuid = senderPlayer.getUniqueId();
		}
		String nickname = AranarthUtils.getNickname(Bukkit.getOfflinePlayer(targetUuid));

		Punishment punishment = new Punishment(targetUuid, LocalDateTime.ofInstant(Instant.now(),
				ZoneId.systemDefault()), "WARN", reason, senderUuid, category);
		AranarthUtils.addPunishment(targetUuid, punishment, false);

		PunishmentUtils.notifyWarn(targetUuid, nickname, reason);
		PunishmentUtils.issued(punishment, nickname, "", -1, notifyCouncil);
		return true;
	}


}
