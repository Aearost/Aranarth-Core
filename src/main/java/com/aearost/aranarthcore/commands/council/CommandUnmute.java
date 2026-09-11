package com.aearost.aranarthcore.commands.council;

import com.aearost.aranarthcore.objects.AranarthPlayer;
import com.aearost.aranarthcore.objects.Punishment;
import com.aearost.aranarthcore.utils.AranarthUtils;
import com.aearost.aranarthcore.utils.ChatUtils;
import com.aearost.aranarthcore.utils.Lang;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Unmutes the specified player before their mute duration ends.
 */
public class CommandUnmute {

	/**
	 * @param sender The user that entered the command.
	 * @param args The arguments of the command.
	 * @return Confirmation of whether the command was a success or not.
	 */
	public static boolean onCommand(CommandSender sender, String[] args) {
		if (sender instanceof Player player) {
			if (player.hasPermission("aranarth.unmute")) {
				unmutePlayer(sender, args);
			} else {
				player.sendMessage(ChatUtils.chatMessage(Lang.get("general.no_permission")));
			}
        } else {
			unmutePlayer(sender, args);
        }
        return true;
    }

	/**
     * Helper method to unmute the input player.
	 * @param sender The user that entered the command.
	 * @param args The arguments of the command.
	 */
	private static void unmutePlayer(CommandSender sender, String[] args) {
		if (args.length == 1) {
			sender.sendMessage(ChatUtils.chatMessage(Lang.get("general.invalid_syntax", "usage", "ac unmute <player> <reason>")));
			return;
		}

		UUID senderUuid = null;
		if (sender instanceof Player senderPlayer) {
			senderUuid = senderPlayer.getUniqueId();
		}

		UUID uuid = AranarthUtils.getUUIDFromUsername(args[1]);
		if (uuid != null) {
			if (args.length >= 3) {
				StringBuilder reason = new StringBuilder();
				for (int i = 2; i < args.length; i++) {
					reason.append(args[i]);
					if (i < args.length - 1) {
						reason.append(" ");
					}
				}

				AranarthPlayer aranarthPlayer = AranarthUtils.getPlayer(uuid);
				if (aranarthPlayer.getMuteEndDate().isEmpty()) {
					sender.sendMessage(ChatUtils.chatMessage(Lang.get("unmute.not_muted", "name", aranarthPlayer.getNickname())));
					return;
				}
				aranarthPlayer.setMuteEndDate("");
				AranarthUtils.setPlayer(uuid, aranarthPlayer);
				AranarthUtils.removeMutedPlayer(uuid);

				Punishment punishment = new Punishment(uuid, LocalDateTime.now(), "UNMUTE", reason.toString(), senderUuid);
				AranarthUtils.addPunishment(uuid, punishment, false);
				sender.sendMessage(ChatUtils.chatMessage(Lang.get("unmute.player_unmuted", "name", aranarthPlayer.getNickname())));

				for (Player player : Bukkit.getOnlinePlayers()) {
					if (player.getUniqueId().equals(uuid)) {
						player.sendMessage(ChatUtils.chatMessage(Lang.get("mute.unmuted")));
					}
				}
			} else {
				sender.sendMessage(ChatUtils.chatMessage(Lang.get("unmute.must_specify_reason")));
			}
		} else {
			sender.sendMessage(ChatUtils.chatMessage(Lang.get("player.not_found", "name", args[1])));
		}
	}
}
