package com.aearost.aranarthcore.commands;

import com.aearost.aranarthcore.objects.AranarthPlayer;
import com.aearost.aranarthcore.objects.Home;
import com.aearost.aranarthcore.utils.AranarthUtils;
import com.aearost.aranarthcore.utils.ChatUtils;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
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
			} else if (sender instanceof ConsoleCommandSender) {
				displayedOptions = displayForOp(sender, displayedOptions, args);
				if (displayedOptions.isEmpty()) {
					displayedOptions = displayNoResultsForOp(displayedOptions);
				}
			}
		}

		// For all commands that have sub-commands
		if (args.length > 1) {
            displayedOptions = displayArgumentsFromOptions(sender, displayedOptions, args);
		}
		return displayedOptions;
	}

	/**
	 * Displays the commands only available to specified players, as well as all other commands.
	 * @param sender The user that entered the command.
	 * @param displayedOptions The list of options to be displayed.
	 * @param args The arguments of the command.
	 * @return The updated list of options to be displayed.
	 */
	private List<String> displayForOp(CommandSender sender, List<String> displayedOptions, String[] args) {
		if (!args[0].isEmpty() && args[0].startsWith("r")) {
			if (args[0].equals("r")) {
				displayedOptions.add("rankset");
			} else {
				if (args[0].equals("ra")) {
					displayedOptions.add("rankset");
				} else {
					if (args[0].equals("ran")) {
						displayedOptions.add("rankset");
					} else {
						if (args[0].equals("rank")) {
							displayedOptions.add("rankset");
						} else {
							if (args[0].equals("ranks")) {
								displayedOptions.add("rankset");
							} else {
								if ("rankset".startsWith(args[0])) {
									displayedOptions.add("rankset");
								}
							}
						}
					}
				}
			}
		} else if (!args[0].isEmpty() && "give".startsWith(args[0])) {
			displayedOptions.add("give");
		} else if (!args[0].isEmpty() && "whereis".startsWith(args[0])) {
			displayedOptions.add("whereis");
		} else if (!args[0].isEmpty() && "itemname".startsWith(args[0])) {
			displayedOptions.add("itemname");
		}
		displayedOptions = displayForAll(sender, displayedOptions, args);
		return displayedOptions;
	}

	/**
	 * Displays the commands available to all players.
	 * @param sender The user that entered the command.
	 * @param displayedOptions The list of options to be displayed.
	 * @param args The arguments of the command.
	 * @return The updated list of options to be displayed.
	 */
	private List<String> displayForAll(CommandSender sender, List<String> displayedOptions, String[] args) {
		if (!args[0].isEmpty() && args[0].startsWith("h")) {
			if ("home".startsWith(args[0])) {
				displayedOptions.add("homepad");
				displayedOptions.add("home");
			} else {
				if ("homepad".startsWith(args[0])) {
					displayedOptions.add("homepad");
				}
			}
		} else if (!args[0].isEmpty() && "nick".startsWith(args[0])) {
			displayedOptions.add("nick");
		} else if (!args[0].isEmpty() && args[0].startsWith("a")) {
			if (args[0].equals("a")) {
				displayedOptions.add("arena");
				displayedOptions.add("aranarthium");
			} else {
				if (args[0].equals("ar")) {
					displayedOptions.add("arena");
					displayedOptions.add("aranarthium");
				} else {
					if ("arena".startsWith(args[0])) {
						displayedOptions.add("arena");
					} else if ("aranarthium".startsWith(args[0])) {
						displayedOptions.add("aranarthium");
					}
				}
			}
		} else if (!args[0].isEmpty() && args[0].startsWith("c")) {
			if (args[0].equals("c")) {
				displayedOptions.add("creative");
				displayedOptions.add("calendar");
			} else if ("creative".startsWith(args[0])) {
				displayedOptions.add("creative");
			} else if ("calendar".startsWith(args[0])) {
				displayedOptions.add("calendar");
			}
		} else if (!args[0].isEmpty() && args[0].startsWith("s")) {
			if (args[0].equals("s")) {
				displayedOptions.add("survival");
				displayedOptions.add("sethome");
				displayedOptions.add("shulker");
				displayedOptions.add("smp");
			} else if ("sethome".startsWith(args[0])) {
				displayedOptions.add("sethome");
			} else if ("survival".startsWith(args[0])) {
				displayedOptions.add("survival");
			} else if ("shulker".startsWith(args[0])) {
				displayedOptions.add("shulker");
			} else if ("smp".startsWith(args[0])) {
				displayedOptions.add("smp");
			}
		} else if (!args[0].isEmpty() && args[0].startsWith("b")) {
			if (args[0].equals("b")) {
				displayedOptions.add("blacklist");
				displayedOptions.add("balance");
			} else if ("blacklist".startsWith(args[0])) {
				displayedOptions.add("blacklist");
			} else if ("balance".startsWith(args[0])) {
				displayedOptions.add("balance");
			}
		} else if (!args[0].isEmpty() && args[0].startsWith("p")) {
			if (args[0].equals("p")) {
				displayedOptions.add("ping");
				displayedOptions.add("potions");
				displayedOptions.add("pronouns");
			} else if ("ping".startsWith(args[0])) {
				displayedOptions.add("ping");
			} else if ("potions".startsWith(args[0])) {
				displayedOptions.add("potions");
			} else if ("pronouns".startsWith(args[0])) {
				displayedOptions.add("pronouns");
			}
		} else if (!args[0].isEmpty() && args[0].startsWith("r")) {
			if (args[0].equals("r")) {
				displayedOptions.add("randomizer");
				displayedOptions.add("ranks");
				displayedOptions.add("rankup");
			} else {
				if (args[0].equals("ra")) {
					displayedOptions.add("randomizer");
					displayedOptions.add("ranks");
					displayedOptions.add("rankup");
				} else {
					if (args[0].equals("ran")) {
						displayedOptions.add("randomizer");
						displayedOptions.add("ranks");
						displayedOptions.add("rankup");
					} else {
						if (args[0].startsWith("rank")) {
							if (args[0].equals("rank")) {
								displayedOptions.add("ranks");
								displayedOptions.add("rankup");
							} else if ("rankup".startsWith(args[0])) {
								displayedOptions.add("rankup");
							} else {
								if ("ranks".startsWith(args[0])) {
									displayedOptions.add("ranks");
								}
							}
						} else if ("randomizer".startsWith(args[0])) {
							displayedOptions.add("randomizer");
						}
					}
				}
			}
		} else if (!args[0].isEmpty() && args[0].startsWith("d")) {
			if (args[0].equals("d")) {
				displayedOptions.add("date");
				displayedOptions.add("dominion");
				displayedOptions.add("delhome");
			} else if ("date".startsWith(args[0])) {
				displayedOptions.add("date");
			} else if ("dominion".startsWith(args[0])) {
				displayedOptions.add("dominion");
			} else if ("delhome".startsWith(args[0])) {
				displayedOptions.add("delhome");
			}
		} else if (!args[0].isEmpty() && "aranarth".startsWith(args[0])) {
			displayedOptions.add("aranarth");
		} else if (!args[0].isEmpty() && "trust".startsWith(args[0])) {
			displayedOptions.add("trust");
		} else if (!args[0].isEmpty() && "lock".startsWith(args[0])) {
			displayedOptions.add("lock");
		} else if (!args[0].isEmpty() && args[0].startsWith("u")) {
			if (args[0].equals("u")) {
				displayedOptions.add("untrust");
				displayedOptions.add("unlock");
			} else {
				if (args[0].equals("un")) {
					displayedOptions.add("untrust");
					displayedOptions.add("unlock");
				} else if (!args[0].isEmpty() && "untrust".startsWith(args[0])) {
					displayedOptions.add("untrust");
				} else if (!args[0].isEmpty() && "unlock".startsWith(args[0])) {
					displayedOptions.add("unlock");
				}
			}
		}
		return displayedOptions;
	}

	/**
	 * Displays the commands available to all players when the input does not match an existing command.
	 * @param displayedOptions The list of options to be displayed.
	 * @return The updated list of options to be displayed.
	 */
	private List<String> displayNoResultsForOp(List<String> displayedOptions) {
		// Op-specific commands only
		displayedOptions.add("whereis");
		displayedOptions.add("itemname");
		displayedOptions.add("give");
		displayedOptions.add("mute");
		displayedOptions.add("unmute");
		displayedOptions.add("ban");
		displayedOptions.add("unban");
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
		displayedOptions.add("nick");
		displayedOptions.add("ping");
		displayedOptions.add("arena");
		displayedOptions.add("survival");
		displayedOptions.add("creative");
		displayedOptions.add("blacklist");
		displayedOptions.add("potions");
		displayedOptions.add("shulker");
		displayedOptions.add("randomizer");
		displayedOptions.add("balance");
		displayedOptions.add("date");
		displayedOptions.add("calendar");
		displayedOptions.add("aranarthium");
		displayedOptions.add("trust");
		displayedOptions.add("untrust");
		displayedOptions.add("lock");
		displayedOptions.add("unlock");
		displayedOptions.add("smp");
		displayedOptions.add("ranks");
		displayedOptions.add("rankup");
		displayedOptions.add("pronouns");
		displayedOptions.add("dominion");
		displayedOptions.add("sethome");
		displayedOptions.add("delhome");
		displayedOptions.add("home");
		return displayedOptions;
	}

	/**
	 * Displays the sub-commands available for the given command.
	 * @param displayedOptions The list of options to be displayed.
	 * @param args The arguments of the command.
	 * @return The updated list of options to be displayed.
	 */
	private List<String> displayArgumentsFromOptions(CommandSender sender, List<String> displayedOptions, String[] args) {
		switch (args[0]) {
			case "homepad" -> {
				if (args.length == 2) {
					if (!args[1].isEmpty() && "create".startsWith(args[1])) {
						displayedOptions.add("create");
					} else {
						displayedOptions.add("create");
					}
				}
			}
			case "ping", "balance", "trust", "untrust" -> {
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
				if (args.length == 2) {
					if (!args[1].isEmpty() && "add".startsWith(args[1])) {
						displayedOptions.add("add");
					} else if (!args[1].isEmpty() && "list".startsWith(args[1])) {
						displayedOptions.add("list");
					} else if (!args[1].isEmpty() && "remove".startsWith(args[1])) {
						displayedOptions.add("remove");
					} else {
						displayedOptions.add("add");
						displayedOptions.add("list");
						displayedOptions.add("remove");
					}
				} else if (args.length == 3) {
					if (args[1].equals("remove") && args[2].isEmpty()) {
						displayedOptions.add("qty");
					}
				}
			}
			case "randomizer" -> {
				if (args[1].isEmpty()) {
					displayedOptions.add("pattern");
				}
			}
			case "pronouns" -> {
				if (args[1].isEmpty()) {
					displayedOptions.add("male");
					displayedOptions.add("female");
				} else {
					if (!args[1].isEmpty() && "male".startsWith(args[1])) {
						displayedOptions.add("male");
					} else if (!args[1].isEmpty() && "female".startsWith(args[1])) {
						displayedOptions.add("female");
					} else {
						displayedOptions.add("male");
						displayedOptions.add("female");
					}
				}
			}
			case "home" -> {
				if (sender instanceof Player player) {
					AranarthPlayer aranarthPlayer = AranarthUtils.getPlayer(player.getUniqueId());
					if (args.length == 1) {
						// Show all homes
						for (Home home : aranarthPlayer.getHomes()) {
							displayedOptions.add(ChatUtils.translateToColor(home.getHomeName()));
						}
					} else {
						// Display all homes starting with what's entered, otherwise show all
						boolean hasResults = false;
						for (Home home : aranarthPlayer.getHomes()) {
							StringBuilder argsAsSingleString = new StringBuilder();
							for (int i = 1; i < args.length; i++) {
								argsAsSingleString.append(args[i]);
								if (i < args.length - 1) {
									argsAsSingleString.append(" ");
								}
							}
							if (ChatUtils.stripColorFormatting(home.getHomeName()).toLowerCase().startsWith(argsAsSingleString.toString().toLowerCase())) {
								displayedOptions.add(ChatUtils.translateToColor(home.getHomeName()));
								hasResults = true;
							}
						}
						// If there were no results, show all homes
						if (!hasResults) {
							for (Home home : aranarthPlayer.getHomes()) {
								displayedOptions.add(ChatUtils.translateToColor(home.getHomeName()));
							}
						}
					}
				}
			}
			case "sethome" -> {
				if (args[1].isEmpty()) {
					displayedOptions.add("name");
				}
			}
			case "delhome" -> {
				if (sender instanceof Player player) {
					// Builds the args into one string
					StringBuilder argsAsSingleString = new StringBuilder();
					for (int i = 1; i < args.length; i++) {
						argsAsSingleString.append(args[i]);
						if (i < args.length - 1) {
							argsAsSingleString.append(" ");
						}
					}

					AranarthPlayer aranarthPlayer = AranarthUtils.getPlayer(player.getUniqueId());
					if (argsAsSingleString.isEmpty()) {
						for (Home home : aranarthPlayer.getHomes()) {
							displayedOptions.add(ChatUtils.translateToColor(home.getHomeName()));
						}
					} else {
						for (Home home : aranarthPlayer.getHomes()) {
							if (ChatUtils.stripColorFormatting(home.getHomeName()).toLowerCase().startsWith(argsAsSingleString.toString().toLowerCase())) {
								displayedOptions.add(ChatUtils.translateToColor(home.getHomeName()));
							}
						}
					}
				}
			}
			case "dominion" -> {
				if (args[1].isEmpty()) {
					displayedOptions = addDominionSubCommands(displayedOptions);
				} else {
					if (args[1].startsWith("c")) {
						if (args[1].equalsIgnoreCase("c")) {
							displayedOptions.add("create");
							displayedOptions.add("claim");
						} else if ("create".startsWith(args[1])) {
							displayedOptions.add("create");
						} else if ("claim".startsWith(args[1])) {
							displayedOptions.add("claim");
						} else {
							displayedOptions = addDominionSubCommands(displayedOptions);
						}
					} else if ("invite".startsWith(args[1])) {
						displayedOptions.add("invite");
					} else if ("accept".startsWith(args[1])) {
						displayedOptions.add("accept");
					} else if (args[1].startsWith("l")) {
						if (args[1].equalsIgnoreCase("l")) {
							displayedOptions.add("list");
							displayedOptions.add("leave");
						} else if ("list".startsWith(args[1])) {
							displayedOptions.add("list");
						} else if ("leave".startsWith(args[1])) {
							displayedOptions.add("leave");
						} else {
							displayedOptions = addDominionSubCommands(displayedOptions);
						}
					} else if ("remove".startsWith(args[1])) {
						displayedOptions.add("remove");
					} else if ("disband".startsWith(args[1])) {
						displayedOptions.add("disband");
					} else if ("unclaim".startsWith(args[1])) {
						displayedOptions.add("unclaim");
					} else if ("balance".startsWith(args[1])) {
						displayedOptions.add("balance");
					} else if ("home".startsWith(args[1])) {
						displayedOptions.add("home");
					} else if ("sethome".startsWith(args[1])) {
						displayedOptions.add("sethome");
					} else if ("who".startsWith(args[1])) {
						displayedOptions.add("who");
					} else if ("members".startsWith(args[1])) {
						displayedOptions.add("members");
					} else {
						displayedOptions = addDominionSubCommands(displayedOptions);
					}
				}
			}
        }
		return displayedOptions;
	}

	/**
	 * Helper method to add all dominion sub-command options to the displayed command options.
	 * @param displayedOptions The current displayed options.
	 * @return The populated displayed options.
	 */
	private static List<String> addDominionSubCommands(List<String> displayedOptions) {
		displayedOptions.add("create");
		displayedOptions.add("invite");
		displayedOptions.add("accept");
		displayedOptions.add("leave");
		displayedOptions.add("remove");
		displayedOptions.add("disband");
		displayedOptions.add("claim");
		displayedOptions.add("unclaim");
		displayedOptions.add("balance");
		displayedOptions.add("home");
		displayedOptions.add("sethome");
		displayedOptions.add("who");
		displayedOptions.add("list");
		displayedOptions.add("members");
		return displayedOptions;
	}

}
