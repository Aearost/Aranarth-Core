package com.aearost.aranarthcore.commands;

import com.aearost.aranarthcore.objects.AranarthPlayer;
import com.aearost.aranarthcore.utils.AranarthUtils;
import com.aearost.aranarthcore.utils.ChatUtils;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/**
 * Compresses the player's current inventory.
 */
public class CommandCompact {

	/**
	 * @param sender The user that entered the command.
	 * @param args The arguments of the command.
	 */
	public static boolean onCommand(CommandSender sender, String[] args) {

		if (sender instanceof Player player) {
			if (!player.hasPermission("aranarth.compact")) {
				player.sendMessage(ChatUtils.chatMessage("&cYou do not have permission to execute this command!"));
				return true;
			}

			AranarthPlayer aranarthPlayer = AranarthUtils.getPlayer(player.getUniqueId());
			if (aranarthPlayer.getIsCompactingItems()) {
				aranarthPlayer.setIsCompactingItems(false);
				player.sendMessage(ChatUtils.chatMessage("&7You are no longer compacting items"));
			} else {
				aranarthPlayer.setIsCompactingItems(true);
				player.sendMessage(ChatUtils.chatMessage("&7You are now compacting items"));
			}

			AranarthUtils.setPlayer(player.getUniqueId(), aranarthPlayer);
			return true;
		} else {
			sender.sendMessage(ChatUtils.chatMessage("&cThis can only be executed by a player!"));
			return true;
		}
	}

}
