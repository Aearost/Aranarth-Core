package com.aearost.aranarthcore.commands;

import com.aearost.aranarthcore.objects.AranarthPlayer;
import com.aearost.aranarthcore.utils.AranarthUtils;
import com.aearost.aranarthcore.utils.ChatUtils;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/**
 * Sets the player in admin mode, allowing them to bypass land claims, chest protections, etc.
 */
public class CommandAdmin {

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

			if (aranarthPlayer.getIsInAdminMode()) {
				player.sendMessage(ChatUtils.chatMessage("&7You have &cdisabled &7admin mode"));
				aranarthPlayer.setIsInAdminMode(false);
			} else {
				player.sendMessage(ChatUtils.chatMessage("&7You have &aenabled &7admin mode"));
				aranarthPlayer.setIsInAdminMode(true);
			}
			AranarthUtils.setPlayer(player.getUniqueId(), aranarthPlayer);
			return true;
		} else {
			sender.sendMessage(ChatUtils.chatMessage("&cThis command must be executed in-game!"));
			return true;
		}
	}


}
