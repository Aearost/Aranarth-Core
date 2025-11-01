package com.aearost.aranarthcore.commands;

import com.aearost.aranarthcore.objects.AranarthPlayer;
import com.aearost.aranarthcore.utils.AranarthUtils;
import com.aearost.aranarthcore.utils.ChatUtils;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

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
				player.sendMessage(ChatUtils.chatMessage("&cYou do not have permission to use this command!"));
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
			sender.sendMessage(ChatUtils.chatMessage("&cYou must specify a player to unmute!"));
			return;
		}

		UUID uuid = AranarthUtils.getUUIDFromUsername(args[1]);
		if (uuid != null) {
			AranarthPlayer aranarthPlayer = AranarthUtils.getPlayer(uuid);
			aranarthPlayer.setMuteEndDate("");
			AranarthUtils.setPlayer(uuid, aranarthPlayer);
			sender.sendMessage(ChatUtils.chatMessage("&e" + aranarthPlayer.getNickname() + " &7has been unmuted"));
			for (Player player : Bukkit.getOnlinePlayers()) {
				if (player.getUniqueId().equals(uuid)) {
					player.sendMessage(ChatUtils.chatMessage("&7You have been unmuted"));
				}
			}
		} else {
			sender.sendMessage(ChatUtils.chatMessage("&cThis player could not be found!"));
		}
	}
}
