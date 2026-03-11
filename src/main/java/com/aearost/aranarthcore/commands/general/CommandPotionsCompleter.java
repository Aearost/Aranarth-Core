package com.aearost.aranarthcore.commands.general;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;

import java.util.ArrayList;
import java.util.List;

/**
 * Handles the auto complete functionality while using the /potions command.
 */
public class CommandPotionsCompleter implements TabCompleter {

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
			if (!args[0].isEmpty() && "add".startsWith(args[0])) {
				displayedOptions.add("add");
			} else if (!args[0].isEmpty() && "list".startsWith(args[0])) {
				displayedOptions.add("list");
			} else if (!args[0].isEmpty() && "remove".startsWith(args[0])) {
				displayedOptions.add("remove");
			} else {
				displayedOptions.add("add");
				displayedOptions.add("list");
				displayedOptions.add("remove");
			}
		} else if (args.length == 2) {
			if (args[0].equals("remove") && args[1].isEmpty()) {
				displayedOptions.add("qty");
			}
		}
		return displayedOptions;
	}
}
