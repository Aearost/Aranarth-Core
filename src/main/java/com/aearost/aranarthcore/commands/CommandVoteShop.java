package com.aearost.aranarthcore.commands;

import com.aearost.aranarthcore.gui.GuiVoteShop;
import com.aearost.aranarthcore.utils.ChatUtils;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/**
 * Provides the player with a GUI of the vote shop.
 */
public class CommandVoteShop {

	/**
	 * @param sender The user that entered the command.
	 * @param args The arguments of the command.
	 */
	public static boolean onCommand(CommandSender sender, String[] args) {
		if (sender instanceof Player player) {
			GuiVoteShop gui = new GuiVoteShop(player);
			gui.openGui();
			return true;
		} else {
			sender.sendMessage(ChatUtils.chatMessage("&cOnly players can execute this command!"));
			return true;
		}
	}
}
