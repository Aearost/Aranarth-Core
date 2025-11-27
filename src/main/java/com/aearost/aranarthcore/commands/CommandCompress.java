package com.aearost.aranarthcore.commands;

import com.aearost.aranarthcore.objects.AranarthPlayer;
import com.aearost.aranarthcore.utils.AranarthUtils;
import com.aearost.aranarthcore.utils.ChatUtils;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/**
 * Compresses the player's current inventory.
 */
public class CommandCompress {

	/**
	 * @param sender The user that entered the command.
	 * @param args The arguments of the command.
	 */
	public static boolean onCommand(CommandSender sender, String[] args) {

		if (sender instanceof Player player) {
			if (!player.hasPermission("aranarth.compress")) {
				player.sendMessage(ChatUtils.chatMessage("&cYou do not have permission to execute this command!"));
				return true;
			}

			AranarthPlayer aranarthPlayer = AranarthUtils.getPlayer(player.getUniqueId());
			if (aranarthPlayer.getIsCompressingItems()) {
				aranarthPlayer.setIsCompressingItems(false);
				player.sendMessage(ChatUtils.chatMessage("&7You are no longer compressing items"));
			} else {
				aranarthPlayer.setIsCompressingItems(true);
				player.sendMessage(ChatUtils.chatMessage("&7You are now compressing items"));
			}

			AranarthUtils.setPlayer(player.getUniqueId(), aranarthPlayer);
			return true;
		} else {
			sender.sendMessage(ChatUtils.chatMessage("&cThis can only be executed by a player!"));
			return true;
		}
	}

}
