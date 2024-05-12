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
		if (args.length == 0) {
			sender.sendMessage(ChatUtils.chatMessageError("Incorrect syntax: /ac <sub-command>"));
			return false;
		} else {
			boolean commandResult = false;
			if (args[0].toLowerCase().equals("homepad")) {
				commandResult = CommandHomePad.onCommand(sender, cmd, label, args);
			} else if (args[0].toLowerCase().equals("swimtoggle")) {
				commandResult = CommandMountSwimToggle.onCommand(sender, cmd, label, args);
			} else if (args[0].toLowerCase().equals("nick")) {
				commandResult = CommandNickname.onCommand(sender, cmd, label, args);
			} else if (args[0].toLowerCase().equals("ping")) {
				commandResult = CommandPing.onCommand(sender, cmd, label, args);
			} else if (args[0].toLowerCase().equals("prefix")) {
				commandResult = CommandPrefix.onCommand(sender, cmd, label, args);
			} else if (args[0].toLowerCase().equals("arena")) {
				commandResult = CommandArena.onCommand(sender, cmd, label, args);
			} else if (args[0].toLowerCase().equals("survival")) {
				commandResult = CommandSurvival.onCommand(sender, cmd, label, args);
			} else if (args[0].toLowerCase().equals("creative")) {
				commandResult = CommandCreative.onCommand(sender, cmd, label, args);
			} else if (args[0].toLowerCase().equals("blacklist")) {
				commandResult = CommandBlacklist.onCommand(sender, cmd, label, args);
			} else if (args[0].toLowerCase().equals("potions")) {
				commandResult = CommandPotion.onCommand(sender, cmd, label, args);
			} else {
				sender.sendMessage(ChatUtils.chatMessageError("Please enter a valid sub-command!"));
			}
			return commandResult;
		}
	}

}
