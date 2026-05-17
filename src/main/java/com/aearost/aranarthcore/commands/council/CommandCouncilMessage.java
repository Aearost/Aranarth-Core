package com.aearost.aranarthcore.commands.council;

import com.aearost.aranarthcore.objects.AranarthPlayer;
import com.aearost.aranarthcore.utils.AranarthUtils;
import com.aearost.aranarthcore.utils.ChatUtils;
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
			AranarthPlayer aranarthPlayer = AranarthUtils.getPlayer(player.getUniqueId());
			if (aranarthPlayer.getCouncilRank() == 0 && aranarthPlayer.getArchitectRank() == 0) {
				player.sendMessage(ChatUtils.chatMessage("&cYou do not have permission to use this command!"));
				return false;
			}

			ChatUtils.evaluateCouncilMessage(sender, args, true);
			return true;
		} else {
			ChatUtils.evaluateCouncilMessage(sender, args, true);
			return true;
		}
	}
}
