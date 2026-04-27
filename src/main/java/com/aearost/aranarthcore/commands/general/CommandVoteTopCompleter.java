package com.aearost.aranarthcore.commands.general;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;

import java.util.List;
import java.util.stream.Stream;

/**
 * Handles the auto complete functionality while using the /votetop command.
 */
public class CommandVoteTopCompleter implements TabCompleter {

	/**
	 * @param sender The user that entered the command.
	 * @param command The command itself.
	 * @param alias The alias of the command.
	 * @param args The arguments of the command.
	 * @return Confirmation of whether the command was a success or not.
	 */
	@Override
	public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
		if (args.length == 1) {
			return Stream.of("year", "month")
					.filter(s -> s.startsWith(args[0].toLowerCase()))
					.toList();
		}

		if (args.length == 2) {
			String sub = args[0].toLowerCase();
			if (sub.equals("year")) {
				return List.of("YYYY");
			} else if (sub.equals("month")) {
				return List.of("MM-YYYY");
			}
		}

		return List.of();
	}
}
