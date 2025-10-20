package com.aearost.aranarthcore.commands;

import com.aearost.aranarthcore.utils.ChatUtils;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/**
 * Mutes the specified player.
 */
public class CommandMute {

	/**
	 * @param sender The user that entered the command.
	 * @param args The arguments of the command.
	 * @return Confirmation of whether the command was a success or not.
	 */
	public static boolean onCommand(CommandSender sender, String[] args) {
		if (sender instanceof Player player) {
			if (player.hasPermission("aranarth.mute")) {
				mutePlayer(sender, args);
			} else {
				player.sendMessage(ChatUtils.chatMessage("&cYou do not have permission to use this command!"));
			}
        } else {
			mutePlayer(sender, args);
        }
        return true;
    }

	/**
	 * Helper method to mute the input player.
	 * @param sender The user that entered the command.
	 * @param args The arguments of the command.
	 */
	private static void mutePlayer(CommandSender sender, String[] args) {
		boolean wasPlayerMuted = false;
		String playerName = args[1];
		for (OfflinePlayer player : Bukkit.getOfflinePlayers()) {
			if (player.getName().equalsIgnoreCase(args[1])) {
				wasPlayerMuted = true;
				playerName = player.getName();
			}
		}

		if (wasPlayerMuted) {
			sender.sendMessage(ChatUtils.chatMessage(playerName + " &rhas been muted"));
			for (Player onlinePlayer : Bukkit.getOnlinePlayers()) {
				if (onlinePlayer.getUniqueId().equals(Bukkit.getOfflinePlayer(playerName).getUniqueId())) {
					onlinePlayer.sendMessage(ChatUtils.chatMessage("&cYou have been muted!"));
				}
			}
		} else {
			sender.sendMessage(ChatUtils.chatMessage(args[1]) + " &rcould not be found");
		}
	}

}
