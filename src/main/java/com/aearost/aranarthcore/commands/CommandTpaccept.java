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
							clearTeleportRequests(player, target);
							return true;
						}
					}

					target.sendMessage(ChatUtils.chatMessage("&e" + aranarthPlayer.getNickname() + " &7has accepted your teleport request"));
					AranarthUtils.teleportPlayer(player, player.getLocation(), target.getLocation(), success -> {
						if (success) {
							player.sendMessage(ChatUtils.chatMessage("&7You have teleported to &e" + targetNickname));
							target.sendMessage(ChatUtils.chatMessage("&e" + aranarthPlayer.getNickname() + " &7has teleported to you"));
							target.playSound(player, Sound.ENTITY_ENDERMAN_TELEPORT, 1F, 0.9F);
						} else {
							player.sendMessage(ChatUtils.chatMessage("&cYou could not teleport to &e" + targetNickname));
							target.sendMessage(ChatUtils.chatMessage("&e" + aranarthPlayer.getNickname() + " &ccould not teleport to you"));
						}
						clearTeleportRequests(player, target);
					});
				} else {
					player.sendMessage(ChatUtils.chatMessage("&e" + targetNickname + " &cis no longer online"));
					clearTeleportRequests(player, target);
				}
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
							clearTeleportRequests(player, target);
							return true;
						}
					}

					player.sendMessage(ChatUtils.chatMessage("&7You have accepted &e" + targetPlayer.getNickname() + "&7's teleport request"));
					target.sendMessage(ChatUtils.chatMessage("&e" + aranarthPlayer.getNickname() + " &7has accepted your teleport request"));
					AranarthUtils.teleportPlayer(target, target.getLocation(), player.getLocation(), success -> {
						if (success) {
							target.sendMessage(ChatUtils.chatMessage("&7You have teleported to &e" + aranarthPlayer.getNickname()));
							player.sendMessage(ChatUtils.chatMessage("&e" + targetPlayer.getNickname() + " &7has teleported to you"));
							player.playSound(player, Sound.ENTITY_ENDERMAN_TELEPORT, 1F, 0.9F);
						} else {
							target.sendMessage(ChatUtils.chatMessage("&cYou could not teleport to &e" + aranarthPlayer.getNickname()));
							player.sendMessage(ChatUtils.chatMessage("&e" + targetPlayer.getNickname() + " &ccould not teleport to you"));
						}
						clearTeleportRequests(player, target);
					});
				} else {
					player.sendMessage(ChatUtils.chatMessage("&e" + targetPlayer.getNickname() + " &cis no longer online"));
					clearTeleportRequests(player, target);
				}
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

	/**
	 * Clears the pending teleport requests that the player has.
	 * @param player1 The first player.
	 * @param player2 The second player.
	 */
	private static void clearTeleportRequests(Player player1, Player player2) {
		AranarthPlayer aranarthPlayer1 = AranarthUtils.getPlayer(player1.getUniqueId());
		AranarthPlayer aranarthPlayer2 = AranarthUtils.getPlayer(player2.getUniqueId());
		aranarthPlayer1.setTeleportToUuid(null);
		aranarthPlayer1.setTeleportFromUuid(null);
		aranarthPlayer2.setTeleportToUuid(null);
		aranarthPlayer2.setTeleportFromUuid(null);
		AranarthUtils.setPlayer(player1.getUniqueId(), aranarthPlayer1);
		AranarthUtils.setPlayer(player2.getUniqueId(), aranarthPlayer2);
	}

}
