package com.aearost.aranarthcore.commands;

import com.aearost.aranarthcore.gui.GuiStore;
import com.aearost.aranarthcore.objects.StorePage;
import com.aearost.aranarthcore.utils.ChatUtils;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/**
 * Provides the player with the server store GUI.
 */
public class CommandStore {

	/**
	 * @param sender The user that entered the command.
	 * @param args The arguments of the command.
	 */
	public static boolean onCommand(CommandSender sender, String[] args) {
		if (sender instanceof Player player) {
			GuiStore gui = new GuiStore(player, StorePage.MAIN);
			gui.openGui();
			return true;
		} else {
			sender.sendMessage(ChatUtils.chatMessage("&cThis command must be executed in-game!"));
			return true;
		}
	}


}
