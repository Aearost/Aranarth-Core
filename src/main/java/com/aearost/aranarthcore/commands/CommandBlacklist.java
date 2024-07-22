package com.aearost.aranarthcore.commands;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import com.aearost.aranarthcore.gui.GuiBlacklist;
import com.aearost.aranarthcore.utils.AranarthUtils;
import com.aearost.aranarthcore.utils.ChatUtils;

/**
 * Allows the player to prevent specified items from being picked up.
 */
public class CommandBlacklist {

	/**
	 * @param sender The user that entered the command.
	 * @param args The arguments of the command.
	 * @return Confirmation of whether the command was a success or not.
	 */
	public static boolean onCommand(CommandSender sender, String[] args) {
		if (sender instanceof Player player) {
            if (args.length == 1) {
				GuiBlacklist gui = new GuiBlacklist(player);
				gui.openGui();
				return true;
			} else {
				if (args[1].equals("ignore")) {
					AranarthUtils.toggleBlacklistIgnoreOrDelete(player.getUniqueId(), false);
					player.sendMessage(ChatUtils.chatMessage("&7You will now ignore blacklisted items"));
					return true;
				} else if (args[1].equals("trash")) {
					AranarthUtils.toggleBlacklistIgnoreOrDelete(player.getUniqueId(), true);
					player.sendMessage(ChatUtils.chatMessage("&7You will now trash blacklisted items"));
					return true;
				} else {
					player.sendMessage(ChatUtils.chatMessageError("Please enter a valid blacklist sub-command!"));
				}
			}
        }
        return false;
    }
}
