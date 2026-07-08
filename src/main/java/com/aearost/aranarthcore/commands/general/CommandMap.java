package com.aearost.aranarthcore.commands.general;

import com.aearost.aranarthcore.utils.ChatUtils;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

/**
 * Sends the player a clickable link to the Aranarth live map.
 */
public class CommandMap implements CommandExecutor {

	/**
	 * @param sender The user that entered the command.
	 * @param command The command itself.
	 * @param alias The alias of the command.
	 * @param args The arguments of the command.
	 * @return Confirmation of whether the command was a success or not.
	 */
	@Override
	public boolean onCommand(CommandSender sender, Command command, String alias, String[] args) {
		sender.sendMessage(ChatUtils.buildMessageWithUrls(ChatUtils.chatMessage("&7Map URL: &ehttps://map.aranarth.net/")));
		return true;
	}

}
