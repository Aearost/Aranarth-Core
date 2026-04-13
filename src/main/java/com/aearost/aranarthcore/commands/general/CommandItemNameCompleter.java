package com.aearost.aranarthcore.commands.general;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;

import java.util.ArrayList;
import java.util.List;

/**
 * Handles the auto complete functionality while using the /itemname command.
 */
public class CommandItemNameCompleter implements TabCompleter {

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
			if (args[0].isEmpty()) {
				displayedOptions.add("remove");
				displayedOptions.add("name");
				displayedOptions.add("gradient");
				displayedOptions.add("gradientbold");
			} else if ("gradient".startsWith(args[0])) {
				displayedOptions.add("gradient");
				displayedOptions.add("gradientbold");
			} else if ("gradientbold".startsWith(args[0])) {
				displayedOptions.add("gradientbold");
			} else if ("remove".startsWith(args[0])) {
				displayedOptions.add("remove");
			}
		} else {
			if (args.length == 2) {
				if (args[0].startsWith("gradient")) {
					if (args[1].isEmpty()) {
						displayedOptions.add("#hex1,#hex2,...");
					}
				}
			} else if (args.length == 3) {
				if (args[0].startsWith("gradient")) {
					if (args[2].isEmpty()) {
						displayedOptions.add("name");
					}
				}
			}

		}
		return displayedOptions;
	}
}
