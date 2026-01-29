package com.aearost.aranarthcore.commands;

import com.aearost.aranarthcore.gui.GuiRanks;
import com.aearost.aranarthcore.utils.ChatUtils;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/**
 * Displays all ranks that are available in Aranarth.
 */
public class CommandRanks {

	/**
	 * @param sender The user that entered the command.
	 * @param args The arguments of the command.
	 */
	public static boolean onCommand(CommandSender sender, String[] args) {
		if (sender instanceof Player player) {
			GuiRanks gui = new GuiRanks(player);
			gui.openGui();
		} else {
			sender.sendMessage(ChatUtils.chatMessage("&cYou must be a player to execute this command!"));
		}
		return true;
	}

}
