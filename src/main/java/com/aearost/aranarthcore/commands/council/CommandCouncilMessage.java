package com.aearost.aranarthcore.commands.council;

import com.aearost.aranarthcore.AranarthCore;
import com.aearost.aranarthcore.utils.ChatUtils;
import com.aearost.aranarthcore.utils.Lang;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/**
 * Allows a player to message the council.
 */
public class CommandCouncilMessage {

	/**
	 * @param sender The user that entered the command.
	 * @param args The arguments of the command.
	 */
	public static boolean onCommand(CommandSender sender, String[] args) {
		if (sender instanceof Player player) {
			if (!player.hasPermission("aranarth.councilchat")) {
				player.sendMessage(ChatUtils.chatMessage(Lang.getFor(player, "general.no_permission")));
				return false;
			}

			ChatUtils.evaluateCouncilMessage(sender, args, true);
			return true;
		} else {
			// Console commands can run on both servers, and each one relays to the other, so only Survival sends it
			if (AranarthCore.isSmpServer()) {
				return true;
			}
			ChatUtils.evaluateCouncilMessage(sender, args, true);
			return true;
		}
	}
}
