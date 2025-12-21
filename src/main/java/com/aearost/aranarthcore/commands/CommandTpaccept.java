package com.aearost.aranarthcore.commands;

import com.aearost.aranarthcore.objects.AranarthPlayer;
import com.aearost.aranarthcore.utils.AranarthUtils;
import com.aearost.aranarthcore.utils.ChatUtils;
import org.bukkit.Bukkit;
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
			// Player is accepting somebody's /ac tphere request
			if (aranarthPlayer.getTeleportToUuid() != null) {
				Player target = Bukkit.getPlayer(aranarthPlayer.getTeleportToUuid());
				String targetNickname = AranarthUtils.getNickname(Bukkit.getOfflinePlayer(aranarthPlayer.getTeleportToUuid()));
				// If both players are still online
				if (target != null) {
					if (target.getLocation().getWorld().getName().startsWith("smp")) {
						if (!AranarthUtils.isOriginalPlayer(player.getUniqueId())) {
							player.sendMessage(ChatUtils.chatMessage("&cYou are not permitted to enter the SMP!"));
							target.sendMessage(ChatUtils.chatMessage("&e" + aranarthPlayer.getNickname() + " &cis not permitted to enter the SMP!"));
							return true;
						}
					}

					boolean wasSuccessful = AranarthUtils.teleportPlayer(player, player.getLocation(), target.getLocation());
					if (wasSuccessful) {
						player.sendMessage(ChatUtils.chatMessage("&7You have teleported to &e" + targetNickname));
						target.sendMessage(ChatUtils.chatMessage("&e" + aranarthPlayer.getNickname() + " &7has teleported to you"));
					} else {
						player.sendMessage(ChatUtils.chatMessage("&cSomething went wrong with teleporting..."));
					}
				} else {
					player.sendMessage(ChatUtils.chatMessage("&e" + targetNickname + " &cis no longer online"));
				}
				// If the player logged off, the request should be cleared
				aranarthPlayer.setTeleportToUuid(null);
				aranarthPlayer.setTeleportFromUuid(null);
				AranarthUtils.setPlayer(player.getUniqueId(), aranarthPlayer);
				return true;
			}
			// Player is accepting somebody's /ac tp request
			else if (aranarthPlayer.getTeleportFromUuid() != null) {
				Player target = Bukkit.getPlayer(aranarthPlayer.getTeleportFromUuid());
				AranarthPlayer targetPlayer = AranarthUtils.getPlayer(target.getUniqueId());
				// If both players are still online
				if (target != null) {
					if (player.getLocation().getWorld().getName().startsWith("smp")) {
						if (!AranarthUtils.isOriginalPlayer(target.getUniqueId())) {
							target.sendMessage(ChatUtils.chatMessage("&cYou are not permitted to enter the SMP!"));
							player.sendMessage(ChatUtils.chatMessage("&e" + targetPlayer.getNickname() + " &cis not permitted to enter the SMP!"));
							return true;
						}
					}

					boolean wasSuccessful = AranarthUtils.teleportPlayer(target, target.getLocation(), player.getLocation());
					if (wasSuccessful) {
						target.sendMessage(ChatUtils.chatMessage("&7You have teleported to &e" + aranarthPlayer.getNickname()));
						player.sendMessage(ChatUtils.chatMessage("&e" + targetPlayer.getNickname() + " &7has teleported to you"));
					} else {
						player.sendMessage(ChatUtils.chatMessage("&cSomething went wrong with teleporting..."));
					}
				} else {
					player.sendMessage(ChatUtils.chatMessage("&e" + targetPlayer.getNickname() + " &cis no longer online"));
				}
				// If the player logged off, the request should be cleared
				aranarthPlayer.setTeleportToUuid(null);
				aranarthPlayer.setTeleportFromUuid(null);
				AranarthUtils.setPlayer(player.getUniqueId(), aranarthPlayer);
				return true;
			} else {
				player.sendMessage(ChatUtils.chatMessage("&cYou do not have any pending teleport requests!"));
				return true;
			}
		} else {
			sender.sendMessage(ChatUtils.chatMessage("&cOnly players can execute this command!"));
			return true;
		}
    }

}
