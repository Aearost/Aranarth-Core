package com.aearost.aranarthcore.commands.council;

import com.aearost.aranarthcore.objects.AranarthPlayer;
import com.aearost.aranarthcore.utils.AranarthUtils;
import com.aearost.aranarthcore.utils.ChatUtils;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/**
 * Toggles spy mode, allowing the player to intercept private messages and dominion chat.
 */
public class CommandSpy {

	/**
	 * @param sender The user that entered the command.
	 * @param args The arguments of the command.
	 */
	public static boolean onCommand(CommandSender sender, String[] args) {
		if (sender instanceof Player player) {
			AranarthPlayer aranarthPlayer = AranarthUtils.getPlayer(player.getUniqueId());

			if (aranarthPlayer.getCouncilRank() != 3) {
				player.sendMessage(ChatUtils.chatMessage("&cYou do not have permission to use this command!"));
				return true;
			}

			if (aranarthPlayer.isInSpyMode()) {
				player.sendMessage(ChatUtils.chatMessage("&7You have &cdisabled &7spy mode"));
				aranarthPlayer.setInSpyMode(false);
			} else {
				player.sendMessage(ChatUtils.chatMessage("&7You have &aenabled &7spy mode"));
				aranarthPlayer.setInSpyMode(true);
			}
			AranarthUtils.setPlayer(player.getUniqueId(), aranarthPlayer);
			return true;
		} else {
			sender.sendMessage(ChatUtils.chatMessage("&cThis command must be executed in-game!"));
			return true;
		}
	}
}
