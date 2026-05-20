package com.aearost.aranarthcore.commands.council;

import com.aearost.aranarthcore.AranarthCore;
import com.aearost.aranarthcore.utils.ChatUtils;
import com.aearost.aranarthcore.utils.DiscordUtils;
import org.bukkit.command.CommandSender;

/**
 * Refreshes all linked accounts' Discord roles to align with their in-game ranks.
 */
public class CommandDiscordReload {

	/**
	 * @param sender The user that entered the command.
	 * @param args The arguments of the command.
	 */
	public static boolean onCommand(CommandSender sender, String[] args) {
		if (!AranarthCore.isPublicServer()) {
			sender.sendMessage(ChatUtils.chatMessage("&cThis command cannot be used on the test server."));
			return false;
		}
		sender.sendMessage(ChatUtils.chatMessage("&7Refreshing Discord roles for all linked accounts..."));
		DiscordUtils.updateAllDiscordRoles();
		sender.sendMessage(ChatUtils.chatMessage("&aDiscord role refresh complete."));
		return true;
	}
}
