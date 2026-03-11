package com.aearost.aranarthcore.commands.general;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;

import java.util.ArrayList;
import java.util.List;

/**
 * Handles the auto complete functionality while using the /blacklist command.
 */
public class CommandBlacklistCompleter implements TabCompleter {

	/**
	 * @param sender The user that entered the command.
	 * @param command The command itself.
	 * @param alias The alias of the command.
	 * @param args The arguments of the command.
	 * @return Confirmation of whether the command was a success or not.
	 */
	@Override
	public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
		List<String> displayedOptions = new ArrayList<>();
		if (args.length == 1) {
			if (!args[0].isEmpty() && "ignore".startsWith(args[0])) {
				displayedOptions.add("ignore");
			} else if (!args[0].isEmpty() && "trash".startsWith(args[0])) {
				displayedOptions.add("trash");
			} else {
				displayedOptions.add("ignore");
				displayedOptions.add("trash");
			}
		}
		return displayedOptions;
	}
}
