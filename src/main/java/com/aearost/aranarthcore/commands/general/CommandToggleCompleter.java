package com.aearost.aranarthcore.commands.general;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Handles the auto complete functionality while using the /toggle command.
 */
public class CommandToggleCompleter implements TabCompleter {

	private static final List<String> TOGGLE_OPTIONS = List.of(
		"blacklist", "bluefire", "changeclaim", "chat", "chestlock",
		"compressor", "daymessage", "gradientchat", "inventory", "messages", "pethurt",
		"shulker", "spawnboost", "teleport", "weathermessage"
	);

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
			return filter(TOGGLE_OPTIONS, args[0]);
		}
		if (args.length == 2) {
			if (args[0].equalsIgnoreCase("gradientchat")) {
				return filter(List.of("bold", "#hex1,#hex2,..."), args[1]);
			}
			if (args[0].equalsIgnoreCase("blacklist")) {
				return filter(List.of("off", "ignore", "trash"), args[1]);
			}
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
}
