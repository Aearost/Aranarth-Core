package com.aearost.aranarthcore.commands;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

import com.aearost.aranarthcore.utils.ChatUtils;

/**
 * This is the master command for all commands related to AranarthCore
 * All sub-commands have their own classes that are called from here
 * 
 * @author liamh
 *
 */
public class CommandAC implements CommandExecutor {

	@Override
	public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
		sender.sendMessage("IN");
		if (args.length == 0) {
			sender.sendMessage(ChatUtils.chatMessageError("Incorrect syntax: /ac <sub-command>"));
			return false;
		} else {
			boolean commandResult = false;
			if (args[0].toLowerCase().equals("homepad")) {
				
			} else if (args[0].toLowerCase().equals("swimtoggle")) {
				commandResult = CommandHorseSwimToggle.onCommand(sender, cmd, label, args);
			} else if (args[0].toLowerCase().equals("nick")) {
				
			} else if (args[0].toLowerCase().equals("ping")) {
				
			} else if (args[0].toLowerCase().equals("prefix")) {
				
			} else {
				sender.sendMessage(ChatUtils.chatMessageError(""));
			}
			return commandResult;
		}
	}

}
