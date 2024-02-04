package com.aearost.aranarthcore.commands;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import com.aearost.aranarthcore.gui.GuiBlacklist;

public class CommandBlacklist {

	public static boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
		if (sender instanceof Player) {
			Player player = (Player) sender;
			GuiBlacklist gui = new GuiBlacklist(player);
			gui.openGui();
			return true;
		} else {
			return false;
		}
		
	}
}
