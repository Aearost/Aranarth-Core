package com.aearost.aranarthcore.commands;

import com.aearost.aranarthcore.utils.AranarthUtils;
import com.aearost.aranarthcore.utils.ChatUtils;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/**
 * Manually sets the player to be AFK.
 */
public class CommandAfk {

	/**
	 * @param sender The user that entered the command.
	 * @param args The arguments of the command.
	 */
	public static boolean onCommand(CommandSender sender, String[] args) {
		if (sender instanceof Player player) {
			AranarthUtils.toggleAfkStatus(player.getUniqueId());
			return true;
		} else {
			sender.sendMessage(ChatUtils.chatMessage("&cThis can only be executed in-game!"));
			return true;
		}
	}
}
