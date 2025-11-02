package com.aearost.aranarthcore.commands;

import com.aearost.aranarthcore.objects.AranarthPlayer;
import com.aearost.aranarthcore.utils.AranarthUtils;
import com.aearost.aranarthcore.utils.ChatUtils;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/**
 * Allows the player to accept a pending teleport request.
 */
public class CommandTpaccept {

	/**
	 * @param sender The user that entered the command.
	 * @param args The arguments of the command.
	 */
	public static boolean onCommand(CommandSender sender, String[] args) {
		if (sender instanceof Player player) {
			AranarthPlayer aranarthPlayer = AranarthUtils.getPlayer(player.getUniqueId());
			// Prioritize teleporting to other players if both requests exist
			if (aranarthPlayer.getTeleportToUuid() != null) {
				Player target = Bukkit.getPlayer(aranarthPlayer.getTeleportToUuid());
				// If both players are still online
				if (target != null) {
					player.teleport(target.getLocation());
					player.sendMessage(ChatUtils.chatMessage("&7You have teleported to &e" + AranarthUtils.getNickname(target)));
					player.playSound(player.getLocation(), Sound.ENTITY_ENDERMAN_TELEPORT, 1F, 0.9F);
					target.sendMessage(ChatUtils.chatMessage("&e" + aranarthPlayer.getNickname() + " &7has teleported to you"));
				}
				// If the player logged off, the request should be cleared
				aranarthPlayer.setTeleportToUuid(null);
				aranarthPlayer.setTeleportFromUuid(null);
				AranarthUtils.setPlayer(player.getUniqueId(), aranarthPlayer);
			} else if (aranarthPlayer.getTeleportFromUuid() != null) {
				Player target = Bukkit.getPlayer(aranarthPlayer.getTeleportToUuid());
				// If both players are still online
				if (target != null) {
					target.teleport(player.getLocation());
					target.sendMessage(ChatUtils.chatMessage("&7You have teleported to &e" + aranarthPlayer.getNickname()));
					target.playSound(player.getLocation(), Sound.ENTITY_ENDERMAN_TELEPORT, 1F, 0.9F);
					player.sendMessage(ChatUtils.chatMessage("&e" + AranarthUtils.getNickname(target) + " &7has teleported to you"));
				}
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
