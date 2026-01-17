package com.aearost.aranarthcore.commands;

import com.aearost.aranarthcore.objects.AranarthPlayer;
import com.aearost.aranarthcore.utils.AranarthUtils;
import com.aearost.aranarthcore.utils.ChatUtils;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/**
 * Allows for the specified player to be trusted to a specified container.
 */
public class CommandTrust {

	/**
	 * @param sender The user that entered the command.
	 * @param args The arguments of the command.
	 * @return Confirmation of whether the command was a success or not.
	 */
	public static boolean onCommand(CommandSender sender, String[] args) {
		if (args.length == 1) {
			sender.sendMessage(ChatUtils.chatMessage("&cYou must specify a player to trust!"));
			return true;
		} else {
			if (sender instanceof Player player) {
				boolean isPlayerFound = false;
				// Does the player exist
				for (OfflinePlayer offlinePlayer : Bukkit.getOfflinePlayers()) {
					if (AranarthUtils.getPlayer(offlinePlayer.getUniqueId()) != null) {
						if (offlinePlayer.getName().equalsIgnoreCase(args[1])) {
							AranarthPlayer aranarthPlayer = AranarthUtils.getPlayer(player.getUniqueId());
							aranarthPlayer.setTrustedPlayerUUID(offlinePlayer.getUniqueId());
							aranarthPlayer.setUntrustedPlayerUUID(null);
							aranarthPlayer.setUnlockingContainer(false);
							aranarthPlayer.setLockingContainer(false);
							AranarthUtils.setPlayer(player.getUniqueId(), aranarthPlayer);
							player.sendMessage(ChatUtils.chatMessage("&7Right-click the container to trust &e" + offlinePlayer.getName()));
							isPlayerFound = true;
							return true;
						}
					}
				}
				if (!isPlayerFound) {
					sender.sendMessage(ChatUtils.chatMessage("&cThis player does not exist!"));
					return true;
				}
			}
		}
		return false;
	}

}
