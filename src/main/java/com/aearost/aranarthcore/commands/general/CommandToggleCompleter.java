package com.aearost.aranarthcore.commands.general;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;

import java.util.ArrayList;
import java.util.List;

/**
 * Handles the auto complete functionality while using the /toggle command.
 */
public class CommandToggleCompleter implements TabCompleter {

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
			if (!args[0].isEmpty() && "messages".startsWith(args[0])) {
				displayedOptions.add("messages");
			} else if (!args[0].isEmpty() && "teleport".startsWith(args[0])) {
				displayedOptions.add("teleport");
			} else if (!args[0].isEmpty() && "bluefire".startsWith(args[0])) {
				displayedOptions.add("bluefire");
			} else if (!args[0].isEmpty() && args[0].startsWith("c")) {
				if (!args[0].isEmpty() && (args[0].equalsIgnoreCase("c")
						|| args[0].equalsIgnoreCase("ch") || args[0].equalsIgnoreCase("cha"))) {
					displayedOptions.add("chat");
					displayedOptions.add("changeclaim");
				} else if (!args[0].isEmpty() && "chat".startsWith(args[0])) {
					displayedOptions.add("chat");
				} else if (!args[0].isEmpty() && "changeclaim".startsWith(args[0])) {
					displayedOptions.add("changeclaim");
				} else {
					displayedOptions.add("messages");
					displayedOptions.add("chat");
					displayedOptions.add("teleport");
					displayedOptions.add("changeclaim");
					displayedOptions.add("bluefire");
				}
			} else {
				displayedOptions.add("messages");
				displayedOptions.add("chat");
				displayedOptions.add("teleport");
				displayedOptions.add("changeclaim");
				displayedOptions.add("bluefire");
			}
		}
		return displayedOptions;
	}
}
