package com.aearost.aranarthcore.commands.general;

import com.aearost.aranarthcore.utils.AranarthUtils;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;

import java.util.List;

/**
 * Handles the auto complete functionality while using the /pay command.
 */
public class CommandPayCompleter implements TabCompleter {

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
			return filterPlayers(args[0]);
		}
		if (args.length == 2 && args[1].isEmpty()) {
			return List.of("amount");
		}
		return List.of();
	}

	private static List<String> filterPlayers(String input) {
		return AranarthUtils.getNetworkPlayerNames(input);
	}
}
