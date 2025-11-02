package com.aearost.aranarthcore.commands;

import com.aearost.aranarthcore.objects.AranarthPlayer;
import com.aearost.aranarthcore.utils.AranarthUtils;
import com.aearost.aranarthcore.utils.ChatUtils;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/**
 * Allows the player to deny a pending teleport request.
 */
public class CommandTpdecline {

	/**
	 * @param sender The user that entered the command.
	 * @param args The arguments of the command.
	 */
	public static boolean onCommand(CommandSender sender, String[] args) {
		if (sender instanceof Player player) {
			AranarthPlayer aranarthPlayer = AranarthUtils.getPlayer(player.getUniqueId());
			// Prioritize denying teleports to other players
			if (aranarthPlayer.getTeleportToUuid() != null) {
				Player target = Bukkit.getPlayer(aranarthPlayer.getTeleportToUuid());
				// If both players are still online
				if (target != null) {
					target.sendMessage(ChatUtils.chatMessage("&e" + aranarthPlayer.getNickname() + " &7has declined your teleport request"));
				}
				String targetNickname = AranarthUtils.getNickname(Bukkit.getOfflinePlayer(aranarthPlayer.getTeleportToUuid()));
				player.sendMessage(ChatUtils.chatMessage("&7You have declined &e" + targetNickname + "&e's &7teleport request"));
				// If the player logged off, the request should be cleared
				aranarthPlayer.setTeleportToUuid(null);
				aranarthPlayer.setTeleportFromUuid(null);
				AranarthUtils.setPlayer(player.getUniqueId(), aranarthPlayer);
			} else if (aranarthPlayer.getTeleportFromUuid() != null) {
				Player target = Bukkit.getPlayer(aranarthPlayer.getTeleportFromUuid());
				// If both players are still online
				if (target != null) {
					target.sendMessage(ChatUtils.chatMessage("&e" + aranarthPlayer.getNickname() + " &7has declined your teleport request"));
				}
				String targetNickname = AranarthUtils.getNickname(Bukkit.getOfflinePlayer(aranarthPlayer.getTeleportToUuid()));
				player.sendMessage(ChatUtils.chatMessage("&7You have declined &e" + targetNickname + "&e's &7teleport request"));
				// If the player logged off, the request should be cleared
				aranarthPlayer.setTeleportToUuid(null);
				aranarthPlayer.setTeleportFromUuid(null);
				AranarthUtils.setPlayer(player.getUniqueId(), aranarthPlayer);
			} else {
				player.sendMessage(ChatUtils.chatMessage("&cYou do not have any pending teleport requests!"));
				return true;
			}
		} else {
			sender.sendMessage(ChatUtils.chatMessage("&cOnly players can execute this command!"));
			return true;
		}
		return false;
	}

}
