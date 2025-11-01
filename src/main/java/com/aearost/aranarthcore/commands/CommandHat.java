package com.aearost.aranarthcore.commands;

import com.aearost.aranarthcore.utils.ChatUtils;
import org.bukkit.command.CommandSender;

/**
 * Places the player's held item on their head.
 */
public class CommandHat {

	/**
	 * @param sender The user that entered the command.
	 * @param args The arguments of the command.
	 */
	public static boolean onCommand(CommandSender sender, String[] args) {
		sender.sendMessage(ChatUtils.chatMessage("&4This feature is not currently implemented."));
		return true;
	}

}
