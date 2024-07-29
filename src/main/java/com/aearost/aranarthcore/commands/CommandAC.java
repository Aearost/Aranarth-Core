package com.aearost.aranarthcore.commands;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

import com.aearost.aranarthcore.utils.ChatUtils;

/**
 * This is the master command for all commands related to AranarthCore.
 * All sub-commands have their own classes that are called from here.
 */
public class CommandAC implements CommandExecutor {

	/**
	 * @param sender The user that entered the command.
	 * @param command The command itself.
	 * @param alias The alias of the command.
	 * @param args The arguments of the command.
	 * @return Confirmation of whether the command was a success or not.
	 */
	@Override
	public boolean onCommand(CommandSender sender, Command command, String alias, String[] args) {
		if (args.length == 0) {
			sender.sendMessage(ChatUtils.chatMessageError("Incorrect syntax: /ac <sub-command>"));
			return false;
		} else {
			boolean commandResult = false;
			if (args[0].equalsIgnoreCase("homepad")) {
				commandResult = CommandHomePad.onCommand(sender, args);
			} else if (args[0].equalsIgnoreCase("swimtoggle")) {
				commandResult = CommandMountSwimToggle.onCommand(sender);
			} else if (args[0].equalsIgnoreCase("nick")) {
				commandResult = CommandNickname.onCommand(sender,args);
			} else if (args[0].equalsIgnoreCase("ping")) {
				commandResult = CommandPing.onCommand(sender, args);
			} else if (args[0].equalsIgnoreCase("prefix")) {
				commandResult = CommandPrefix.onCommand(sender, args);
			} else if (args[0].equalsIgnoreCase("arena")) {
				commandResult = CommandArena.onCommand(sender, args);
			} else if (args[0].equalsIgnoreCase("survival")) {
				commandResult = CommandSurvival.onCommand(sender, args);
			} else if (args[0].equalsIgnoreCase("creative")) {
				commandResult = CommandCreative.onCommand(sender, args);
			} else if (args[0].equalsIgnoreCase("blacklist")) {
				commandResult = CommandBlacklist.onCommand(sender, args);
			} else if (args[0].equalsIgnoreCase("potions")) {
				commandResult = CommandPotions.onCommand(sender, args);
			} else if (args[0].equalsIgnoreCase("whereis")) {
				commandResult = CommandWhereIs.onCommand(sender, args);
			} else {
				sender.sendMessage(ChatUtils.chatMessageError("Please enter a valid sub-command!"));
			}
			return commandResult;
		}
	}

}
