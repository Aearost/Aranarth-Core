package com.aearost.aranarthcore.commands;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import com.aearost.aranarthcore.gui.GuiBlacklist;
import com.aearost.aranarthcore.utils.AranarthUtils;
import com.aearost.aranarthcore.utils.ChatUtils;

public class CommandBlacklist {

	public static boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
		if (sender instanceof Player) {
			Player player = (Player) sender;
			if (args.length == 1) {
				GuiBlacklist gui = new GuiBlacklist(player);
				gui.openGui();
				return true;
			} else {
				if (args[1].equals("ignore")) {
					AranarthUtils.toggleBlacklistIgnoreOrDelete(player.getUniqueId(), false);
					player.sendMessage(ChatUtils.chatMessage("&7You will now ignore blacklisted items"));
				} else if (args[1].equals("trash")) {
					AranarthUtils.toggleBlacklistIgnoreOrDelete(player.getUniqueId(), true);
					player.sendMessage(ChatUtils.chatMessage("&7You will now trash blacklisted items"));
				} else {
					player.sendMessage(ChatUtils.chatMessageError("Please enter a valid blacklist sub-command!"));
				}
			}
			return false;
		} else {
			return false;
		}
		
	}
}
