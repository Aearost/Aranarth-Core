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
			} else if (!args[0].isEmpty() && (args[0].startsWith("b") || args[0].startsWith("bl"))) {
				if (args[0].equals("b") || args[0].equals("bl")) {
					displayedOptions.add("bluefire");
					displayedOptions.add("blacklist");
				} else if ("bluefire".startsWith(args[0])) {
					displayedOptions.add("bluefire");
				} else if ("blacklist".startsWith(args[0])) {
					displayedOptions.add("blacklist");
				} else {
					displayedOptions = displayNoResults();
				}
			} else if (!args[0].isEmpty() && args[0].startsWith("s")) {
				if (args[0].equals("s")) {
					displayedOptions.add("spawnboost");
					displayedOptions.add("shulker");
				} else if ("spawnboost".startsWith(args[0])) {
					displayedOptions.add("spawnboost");
				} else if ("shulker".startsWith(args[0])) {
					displayedOptions.add("shulker");
				} else {
					displayedOptions = displayNoResults();
				}
			} else if (!args[0].isEmpty() && "inventory".startsWith(args[0])) {
				displayedOptions.add("inventory");
			} else if (!args[0].isEmpty() && "pethurt".startsWith(args[0])) {
				displayedOptions.add("pethurt");
			} else if (!args[0].isEmpty() && "gradientchat".startsWith(args[0])) {
				displayedOptions.add("gradientchat");
			} else if (!args[0].isEmpty() && args[0].startsWith("c")) {
				if (args[0].equals("c")) {
					displayedOptions.add("chat");
					displayedOptions.add("changeclaim");
					displayedOptions.add("compressor");
					displayedOptions.add("chestlock");
				} else if (args[0].equalsIgnoreCase("ch")) {
					displayedOptions.add("chat");
					displayedOptions.add("changeclaim");
					displayedOptions.add("chestlock");
				} else if (args[0].equalsIgnoreCase("cha")) {
					displayedOptions.add("chat");
					displayedOptions.add("changeclaim");
				} else if ("chat".startsWith(args[0])) {
					displayedOptions.add("chat");
				} else if ("changeclaim".startsWith(args[0])) {
					displayedOptions.add("changeclaim");
				} else if ("compressor".startsWith(args[0])) {
					displayedOptions.add("compressor");
				} else if ("chestlock".startsWith(args[0])) {
					displayedOptions.add("chestlock");
				} else {
					displayedOptions = displayNoResults();
				}
			} else {
				displayedOptions = displayNoResults();
			}
		} else if (args.length == 2) {
			if (args[0].equalsIgnoreCase("gradientchat")) {
				if (args[1].isEmpty()) {
					displayedOptions.add("bold");
					displayedOptions.add("#hex1,#hex2,...");
				} else if ("bold".startsWith(args[1].toLowerCase())) {
					displayedOptions.add("bold");
				} else {
					displayedOptions.add("#hex1,#hex2,...");
				}
			} else {
				if (args[1].isEmpty()) {
					displayedOptions.add("off");
					displayedOptions.add("ignore");
					displayedOptions.add("trash");
				} else {
					if ("off".startsWith(args[1])) {
						displayedOptions.add("off");
					} else if ("ignore".startsWith(args[1])) {
						displayedOptions.add("ignore");
					} else if ("trash".startsWith(args[1])) {
						displayedOptions.add("trash");
					} else {
						displayedOptions.add("off");
						displayedOptions.add("ignore");
						displayedOptions.add("trash");
					}
				}
			}
		}
		return displayedOptions;
	}

	private List<String> displayNoResults() {
		List<String> displayedOptions = new ArrayList<>();
		displayedOptions.add("messages");
		displayedOptions.add("chat");
		displayedOptions.add("teleport");
		displayedOptions.add("changeclaim");
		displayedOptions.add("bluefire");
		displayedOptions.add("spawnboost");
		displayedOptions.add("inventory");
		displayedOptions.add("shulker");
		displayedOptions.add("blacklist");
		displayedOptions.add("compressor");
		displayedOptions.add("chestlock");
		displayedOptions.add("pethurt");
		displayedOptions.add("gradientchat");
		return displayedOptions;
	}
}
