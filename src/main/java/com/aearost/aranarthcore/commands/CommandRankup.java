package com.aearost.aranarthcore.commands;

import com.aearost.aranarthcore.gui.GuiRankup;
import com.aearost.aranarthcore.utils.ChatUtils;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/**
 * Displays the Rankup GUI pertaining to the player.
 */
public class CommandRankup {

	/**
	 * @param sender The user that entered the command.
	 * @param args The arguments of the command.
	 */
	public static boolean onCommand(CommandSender sender, String[] args) {
		if (sender instanceof Player player) {
			GuiRankup gui = new GuiRankup(player, "TEMP", "TEMP");
			gui.openGui();
		} else {
			sender.sendMessage(ChatUtils.chatMessage("&cYou must be a player to execute this command!"));
		}
		return true;
	}

}
