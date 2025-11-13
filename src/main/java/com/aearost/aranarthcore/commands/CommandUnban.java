package com.aearost.aranarthcore.commands;

import com.aearost.aranarthcore.objects.AranarthPlayer;
import com.aearost.aranarthcore.objects.Punishment;
import com.aearost.aranarthcore.utils.AranarthUtils;
import com.aearost.aranarthcore.utils.ChatUtils;
import org.bukkit.BanList;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.ban.ProfileBanList;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Unbans the specified player before their ban duration ends.
 */
public class CommandUnban {

	/**
	 * @param sender The user that entered the command.
	 * @param args The arguments of the command.
	 * @return Confirmation of whether the command was a success or not.
	 */
	public static boolean onCommand(CommandSender sender, String[] args) {
		if (sender instanceof Player player) {
			if (player.hasPermission("aranarth.unban")) {
				unbanPlayer(sender, args);
			} else {
				player.sendMessage(ChatUtils.chatMessage("&cYou do not have permission to use this command!"));
			}
        } else {
			unbanPlayer(sender, args);
        }
        return true;
    }

	/**
     * Helper method to unban the input player.
	 * @param sender The user that entered the command.
	 * @param args The arguments of the command.
	 */
	private static void unbanPlayer(CommandSender sender, String[] args) {
		if (args.length == 1) {
			sender.sendMessage(ChatUtils.chatMessage("&cInvalid syntax: &e/ac unban <player> <reason>"));
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

				OfflinePlayer player = Bukkit.getOfflinePlayer(uuid);
				AranarthPlayer aranarthPlayer = AranarthUtils.getPlayer(player.getUniqueId());

				ProfileBanList profileBanList = Bukkit.getBanList(BanList.Type.PROFILE);
				if (profileBanList.getBanEntry(player.getPlayerProfile()) == null) {
					sender.sendMessage(ChatUtils.chatMessage("&e" + aranarthPlayer.getNickname() + " &cis not currently banned!"));
					return;
				}
				profileBanList.pardon(player.getPlayerProfile());

				Punishment punishment = new Punishment(uuid, LocalDateTime.now(), "UNBAN", reason.toString(), senderUuid);
				AranarthUtils.addPunishment(uuid, punishment, false);
				sender.sendMessage(ChatUtils.chatMessage("&e" + AranarthUtils.getNickname(player) + " &7has been unbanned"));
			} else {
				sender.sendMessage(ChatUtils.chatMessage("&cYou must specify an unban reason"));
			}
		} else {
			sender.sendMessage(ChatUtils.chatMessage("&cThis player could not be found!"));
		}
	}
}
