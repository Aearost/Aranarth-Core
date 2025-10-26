package com.aearost.aranarthcore.commands;

import com.aearost.aranarthcore.utils.ChatUtils;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.time.Duration;

/**
 * Bans the specified player.
 */
public class CommandBan {

	/**
	 * @param sender The user that entered the command.
	 * @param args The arguments of the command.
	 * @return Confirmation of whether the command was a success or not.
	 */
	public static boolean onCommand(CommandSender sender, String[] args) {
		if (sender instanceof Player player) {
			if (player.hasPermission("aranarth.ban")) {
				banPlayer(sender, args);
			} else {
				player.sendMessage(ChatUtils.chatMessage("&cYou do not have permission to use this command!"));
			}
        } else {
			banPlayer(sender, args);
        }
        return true;
    }

	/**
     * Helper method to ban the input player.
	 * @param sender The user that entered the command.
	 * @param args The arguments of the command.
	 */
	private static void banPlayer(CommandSender sender, String[] args) {
		boolean wasPlayerBanned = false;
		OfflinePlayer player = null;
		for (OfflinePlayer offlinePlayer : Bukkit.getOfflinePlayers()) {
			if (player.getName().equalsIgnoreCase(args[1])) {
				player = offlinePlayer;
			}
		}

		if (wasPlayerBanned) {
			player.ban("You have been banned!", Duration.ZERO, null);
			sender.sendMessage(ChatUtils.chatMessage(player + " &rhas been banned"));
		} else {
			sender.sendMessage(ChatUtils.chatMessage("&c" + args[1]) + " &ecould not be found");
		}
	}

}
