package com.aearost.aranarthcore.commands.general;

import com.aearost.aranarthcore.utils.AranarthUtils;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Handles the auto complete functionality while using the /boosts command.
 */
public class CommandBoostsCompleter implements TabCompleter {

	private static final List<String> ACTIONS = List.of("add", "remove");
	private static final List<String> BOOST_TYPES = List.of("CHI", "HARVEST", "HUNTER", "MINER");

	/**
	 * @param sender The user that entered the command.
	 * @param command The command itself.
	 * @param alias The alias of the command.
	 * @param args The arguments of the command.
	 * @return Confirmation of whether the command was a success or not.
	 */
	@Override
	public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
		if (sender instanceof Player player && !player.hasPermission("aranarth.boosts.modify")) {
			return List.of();
		}
		if (args.length == 1) {
			return filter(ACTIONS, args[0]);
		}
		if (args.length == 2) {
			return new ArrayList<>(BOOST_TYPES);
		}
		if (args.length == 3) {
			return filterPlayers(args[2]);
		}
		return List.of();
	}

	private static List<String> filter(List<String> options, String input) {
		if (input.isEmpty()) {
			return new ArrayList<>(options);
		}
		return options.stream()
			.filter(s -> s.toLowerCase().startsWith(input.toLowerCase()))
			.collect(Collectors.toList());
	}

	private static List<String> filterPlayers(String input) {
		return AranarthUtils.getNetworkPlayerNames(input);
	}
}
