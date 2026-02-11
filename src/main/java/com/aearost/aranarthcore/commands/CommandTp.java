package com.aearost.aranarthcore.commands;

import com.aearost.aranarthcore.objects.AranarthPlayer;
import com.aearost.aranarthcore.utils.AranarthUtils;
import com.aearost.aranarthcore.utils.ChatUtils;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/**
 * Sends a request to teleport to the input player.
 */
public class CommandTp {

	/**
	 * @param sender The user that entered the command.
	 * @param args The arguments of the command.
	 */
	public static boolean onCommand(CommandSender sender, String[] args) {
		if (sender instanceof Player player) {
			if (args.length == 1) {
				sender.sendMessage(ChatUtils.chatMessage("&cYou must enter a player to teleport to!"));
				return true;
			} else {
				if (AranarthUtils.getUUIDFromUsername(args[1]) != null) {
					Player target = Bukkit.getPlayer(AranarthUtils.getUUIDFromUsername(args[1]));
					if (target != null) {
						if (player.getUniqueId().equals(target.getUniqueId())) {
							player.sendMessage(ChatUtils.chatMessage("&cYou cannot teleport to yourself!"));
							return true;
						}

						AranarthPlayer targetPlayer = AranarthUtils.getPlayer(target.getUniqueId());
						if (targetPlayer.isTogglingTp()) {
							player.sendMessage(ChatUtils.chatMessage("&e" + targetPlayer.getNickname() + " &cis currently not accepting teleport requests"));
							return true;
						}

						AranarthPlayer senderPlayer = AranarthUtils.getPlayer(player.getUniqueId());
						targetPlayer.setTeleportFromUuid(player.getUniqueId());
						AranarthUtils.setPlayer(target.getUniqueId(), targetPlayer);
						player.sendMessage(ChatUtils.chatMessage("&7You have requested to teleport to &e" + targetPlayer.getNickname()));
						target.sendMessage(ChatUtils.chatMessage("&e" + senderPlayer.getNickname() + " &7has requested to teleport to you"));
						target.sendMessage(ChatUtils.chatMessage("&7Use &e/ac tpaccept &7or &e/ac tpdeny"));
						AranarthUtils.playTeleportSound(player);
						AranarthUtils.playTeleportSound(target);
					} else {
						player.sendMessage(ChatUtils.chatMessage("&cThat player is not online!"));
					}
				} else {
					player.sendMessage(ChatUtils.chatMessage("&cThat player is not online!"));
				}
			}
		} else {
			sender.sendMessage(ChatUtils.chatMessage("&cOnly players can execute this command!"));
		}
		return true;
	}

}
