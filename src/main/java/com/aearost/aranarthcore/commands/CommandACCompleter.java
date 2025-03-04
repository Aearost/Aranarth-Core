package com.aearost.aranarthcore.commands;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

/**
 * Handles the auto complete functionality while using the /ac command.
 */
public class CommandACCompleter implements TabCompleter {

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
			if (sender instanceof Player player) {
				if (player.getName().equalsIgnoreCase("Aearost")) {
					displayedOptions = displayForOp(player, displayedOptions, args);
					if (displayedOptions.isEmpty()) {
						displayedOptions = displayNoResultsForOp(displayedOptions);
					}
				} else {
					displayedOptions = displayForAll(player, displayedOptions, args);
					if (displayedOptions.isEmpty()) {
						displayedOptions = displayNoResultsForAll(displayedOptions);
					}
				}
			}
		}

		// For all commands that have sub-commands
		if (args.length > 1) {
            displayedOptions = displayArgumentsFromOptions(displayedOptions, args);
		}
		return displayedOptions;
	}

	/**
	 * Displays the commands only available to specified players, as well as all other commands.
	 * @param player The user that entered the command.
	 * @param displayedOptions The list of options to be displayed.
	 * @param args The arguments of the command.
	 * @return The updated list of options to be displayed.
	 */
	private List<String> displayForOp(Player player, List<String> displayedOptions, String[] args) {
		if (!args[0].isEmpty() && "whereis".startsWith(args[0])) {
			displayedOptions.add("whereis");
		} else if (!args[0].isEmpty() && "itemname".startsWith(args[0])) {
			displayedOptions.add("itemname");
		} else {
			displayedOptions = displayForAll(player, displayedOptions, args);
		}

		return displayedOptions;
	}

	/**
	 * Displays the commands available to all players.
	 * @param player The user that entered the command.
	 * @param displayedOptions The list of options to be displayed.
	 * @param args The arguments of the command.
	 * @return The updated list of options to be displayed.
	 */
	private List<String> displayForAll(Player player, List<String> displayedOptions, String[] args) {
		if (!args[0].isEmpty() && "homepad".startsWith(args[0])) {
			displayedOptions.add("homepad");
		} else if (!args[0].isEmpty() && "nick".startsWith(args[0])) {
			displayedOptions.add("nick");
		} else if (!args[0].isEmpty() && "arena".startsWith(args[0])) {
			displayedOptions.add("arena");
		} else if (!args[0].isEmpty() && "creative".startsWith(args[0])) {
			displayedOptions.add("creative");
		} else if (!args[0].isEmpty() && args[0].startsWith("s")) {
			if (args[0].equals("s")) {
				displayedOptions.add("survival");
				displayedOptions.add("swimtoggle");
				displayedOptions.add("shulker");
			} else if ("swimtoggle".startsWith(args[0])) {
				displayedOptions.add("swimtoggle");
			} else if ("survival".startsWith(args[0])) {
				displayedOptions.add("survival");
			} else if ("shulker".startsWith(args[0])) {
				displayedOptions.add("shulker");
			}
		} else if (!args[0].isEmpty() && "blacklist".startsWith(args[0])) {
			displayedOptions.add("blacklist");
		} else if (!args[0].isEmpty() && args[0].startsWith("p")) {
			if (args[0].equals("p")) {
				displayedOptions.add("ping");
				displayedOptions.add("potions");
				displayedOptions.add("prefix");
			} else if ("ping".startsWith(args[0])) {
				displayedOptions.add("ping");
			} else if ("potions".startsWith(args[0])) {
				displayedOptions.add("potions");
			} else if ("prefix".startsWith(args[0])) {
				displayedOptions.add("prefix");
			}
		} else if (!args[0].isEmpty() && "randomizer".startsWith(args[0])) {
			displayedOptions.add("randomizer");
		}
		return displayedOptions;
	}

	/**
	 * Displays the commands available to all players when the input does not match an existing command.
	 * @param displayedOptions The list of options to be displayed.
	 * @return The updated list of options to be displayed.
	 */
	private List<String> displayNoResultsForOp(List<String> displayedOptions) {
		displayedOptions.add("whereis");
		displayedOptions.add("itemname");
		displayedOptions = displayNoResultsForAll(displayedOptions);
		return displayedOptions;
	}

	/**
	 * Displays the commands available to all players when the input does not match an existing command.
	 * @param displayedOptions The list of options to be displayed.
	 * @return The updated list of options to be displayed.
	 */
	private List<String> displayNoResultsForAll(List<String> displayedOptions) {
		displayedOptions.add("homepad");
		displayedOptions.add("swimtoggle");
		displayedOptions.add("nick");
		displayedOptions.add("ping");
		displayedOptions.add("prefix");
		displayedOptions.add("arena");
		displayedOptions.add("survival");
		displayedOptions.add("creative");
		displayedOptions.add("blacklist");
		displayedOptions.add("potions");
		displayedOptions.add("shulker");
		displayedOptions.add("randomizer");
		return displayedOptions;
	}

	/**
	 * Displays the sub-commands available for the given command.
	 * @param displayedOptions The list of options to be displayed.
	 * @param args The arguments of the command.
	 * @return The updated list of options to be displayed.
	 */
	private List<String> displayArgumentsFromOptions(List<String> displayedOptions, String[] args) {
		switch (args[0]) {
			case "homepad" -> {
				if (args.length == 2) {
					if (!args[1].isEmpty() && "create".startsWith(args[1])) {
						displayedOptions.add("create");
					} else if (!args[1].isEmpty() && "give".startsWith(args[1])) {
						displayedOptions.add("give");
					} else {
						displayedOptions.add("create");
						displayedOptions.add("give");
					}
				} else {
					if (args[1].equalsIgnoreCase("give")) {
						Player[] onlinePlayers = new Player[Bukkit.getOnlinePlayers().size()];
						Bukkit.getOnlinePlayers().toArray(onlinePlayers);
						for (Player onlinePlayer : onlinePlayers) {
							// Only display the name if it aligns with one that is currently online
							if (onlinePlayer.getName().toLowerCase().startsWith(args[2].toLowerCase())) {
								displayedOptions.add(onlinePlayer.getName());
							} else if (args[1].isEmpty()) {
								displayedOptions.add(onlinePlayer.getName());
							}
						}
					}
				}
			}
			case "ping" -> {
				Player[] onlinePlayers = new Player[Bukkit.getOnlinePlayers().size()];
				Bukkit.getOnlinePlayers().toArray(onlinePlayers);
				for (Player onlinePlayer : onlinePlayers) {
					// Only display the name if it aligns with one that is currently online
					if (onlinePlayer.getName().toLowerCase().startsWith(args[1].toLowerCase())) {
						displayedOptions.add(onlinePlayer.getName());
					} else if (args[1].isEmpty()) {
						displayedOptions.add(onlinePlayer.getName());
					}
				}
			}
			case "blacklist" -> {
				if (!args[1].isEmpty() && "ignore".startsWith(args[1])) {
					displayedOptions.add("ignore");
				} else if (!args[1].isEmpty() && "trash".startsWith(args[1])) {
					displayedOptions.add("trash");
				} else {
					displayedOptions.add("ignore");
					displayedOptions.add("trash");
				}
			}
			case "potions" -> {
				if (!args[1].isEmpty() && "add".startsWith(args[1])) {
					displayedOptions.add("add");
				} else if (!args[1].isEmpty() && "list".startsWith(args[1])) {
					displayedOptions.add("list");
				} else {
					displayedOptions.add("add");
					displayedOptions.add("list");
				}
			}
			case "randomizer" -> {
				if (args[1].isEmpty()) {
					displayedOptions.add("pattern");
				}
			}
		}
		return displayedOptions;
	}

}
