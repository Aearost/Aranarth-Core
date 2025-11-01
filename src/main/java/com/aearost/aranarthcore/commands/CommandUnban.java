package com.aearost.aranarthcore.commands;

import com.aearost.aranarthcore.utils.AranarthUtils;
import com.aearost.aranarthcore.utils.ChatUtils;
import org.bukkit.BanList;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.ban.ProfileBanList;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

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
			sender.sendMessage(ChatUtils.chatMessage("&cYou must specify a player to unban!"));
			return;
		}

		UUID uuid = AranarthUtils.getUUIDFromUsername(args[1]);
		if (uuid != null) {
			OfflinePlayer player = Bukkit.getOfflinePlayer(uuid);
			ProfileBanList profileBanList = Bukkit.getBanList(BanList.Type.PROFILE);
			profileBanList.pardon(player.getPlayerProfile());
			sender.sendMessage(ChatUtils.chatMessage("&e" + AranarthUtils.getNickname(player) + " &7has been unbanned"));
		} else {
			sender.sendMessage(ChatUtils.chatMessage("&cThis player could not be found!"));
		}
	}
}
