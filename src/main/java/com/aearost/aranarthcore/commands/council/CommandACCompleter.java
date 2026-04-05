package com.aearost.aranarthcore.commands.council;

import com.aearost.aranarthcore.objects.AranarthPlayer;
import com.aearost.aranarthcore.utils.AranarthUtils;
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
		if (sender instanceof Player player) {
			AranarthPlayer aranarthPlayer = AranarthUtils.getPlayer(player.getUniqueId());
			// Additional commands will display for council
			if (aranarthPlayer.getCouncilRank() > 0) {
				if (args.length == 1) {
					displayedOptions = council(player, displayedOptions, args);
					if (displayedOptions.isEmpty()) {
						displayedOptions = noResultsCouncil(displayedOptions);
					}
				} else {
					displayedOptions = councilArgs(sender, displayedOptions, args);
				}
			}
		} else if (sender instanceof ConsoleCommandSender) {
			displayedOptions = council(sender, displayedOptions, args);
			if (displayedOptions.isEmpty()) {
				displayedOptions = noResultsCouncil(displayedOptions);
			}
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
	private List<String> council(CommandSender sender, List<String> displayedOptions, String[] args) {
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
		} else if (!args[0].isEmpty() && "admin".startsWith(args[0])) {
			displayedOptions.add("admin");
		} else if (!args[0].isEmpty() && args[0].startsWith("w")) {
			if (args[0].equals("w")) {
				displayedOptions.add("whereis");
				displayedOptions.add("warn");
			} else {
				if (!args[0].isEmpty() && "whereis".startsWith(args[0])) {
					displayedOptions.add("whereis");
				} else if (!args[0].isEmpty() && "warn".startsWith(args[0])) {
					displayedOptions.add("warn");
				}
			}
		} else if (!args[0].isEmpty() && args[0].startsWith("m")) {
			if (args[0].equals("m")) {
				displayedOptions.add("mute");
				displayedOptions.add("msg");
			} else if (!args[0].isEmpty() && "mute".startsWith(args[0])) {
				displayedOptions.add("mute");
			} else if (!args[0].isEmpty() && "msg".startsWith(args[0])) {
				displayedOptions.add("msg");
			}
		} else if (!args[0].isEmpty() && args[0].startsWith("b")) {
			if (args[0].equals("b")) {
				displayedOptions.add("ban");
				displayedOptions.add("broadcast");
			} else if (!args[0].isEmpty() && "ban".startsWith(args[0])) {
				displayedOptions.add("ban");
			} else if (!args[0].isEmpty() && "broadcast".startsWith(args[0])) {
				displayedOptions.add("broadcast");
			}
		} else if (!args[0].isEmpty() && "invsee".startsWith(args[0])) {
			displayedOptions.add("invsee");
		} else if (!args[0].isEmpty() && args[0].startsWith("u")) {
			if (args[0].equals("u")) {
				displayedOptions.add("unmute");
				displayedOptions.add("unban");
			} else {
				if (args[0].equals("un")) {
					displayedOptions.add("unmute");
					displayedOptions.add("unban");
				} else {
					if ("unmute".startsWith(args[0])) {
						displayedOptions.add("unmute");
					} else if ("unban".startsWith(args[0])) {
						displayedOptions.add("unban");
					}
				}
			}
		} else if (!args[0].isEmpty() && args[0].startsWith("p")) {
			if (args[0].equals("p")) {
				displayedOptions.add("punishments");
				displayedOptions.add("perks");
			} else if (!args[0].isEmpty() && "punishments".startsWith(args[0])) {
				displayedOptions.add("punishments");
			} else if (!args[0].isEmpty() && "perks".startsWith(args[0])) {
				displayedOptions.add("perks");
			}
		} else if (!args[0].isEmpty() && args[0].startsWith("c")) {
			if (args[0].equalsIgnoreCase("c")) {
				displayedOptions.add("clearchat");
			}  else if (!args[0].isEmpty() && "clearchat".startsWith(args[0])) {
				displayedOptions.add("clearchat");
			}
		} else if (!args[0].isEmpty() && args[0].startsWith("s")) {
			if (args[0].equals("s")) {
				displayedOptions.add("speed");
				displayedOptions.add("sudo");
			} else if (!args[0].isEmpty() && "speed".startsWith(args[0])) {
				displayedOptions.add("speed");
			} else if (!args[0].isEmpty() && "sudo".startsWith(args[0])) {
				displayedOptions.add("sudo");
			}
		} else if (!args[0].isEmpty() && args[0].startsWith("t")) {
			if (args[0].equalsIgnoreCase("t")) {
				displayedOptions.add("time");
				displayedOptions.add("tp");
				displayedOptions.add("tpf");
			} else if (!args[0].isEmpty() && "time".startsWith(args[0])) {
				displayedOptions.add("time");
			} else if (!args[0].isEmpty() && args[0].startsWith("tp")) {
				if (args[0].equalsIgnoreCase("tp")) {
					displayedOptions.add("tp");
					displayedOptions.add("tpf");
				} else if (!args[0].isEmpty() && "tp".startsWith(args[0])) {
					displayedOptions.add("tp");
				} else if (!args[0].isEmpty() && "tpf".startsWith(args[0])) {
					displayedOptions.add("tpf");
				}
			}
		} else if (!args[0].isEmpty() && "dateset".startsWith(args[0])) {
			displayedOptions.add("dateset");
		} else if (!args[0].isEmpty() && "vanish".startsWith(args[0])) {
			displayedOptions.add("vanish");
		}
		return displayedOptions;
	}

	/**
	 * Displays the sub-commands available for the given command.
	 * @param displayedOptions The list of options to be displayed.
	 * @param args The arguments of the command.
	 * @return The updated list of options to be displayed.
	 */
	private List<String> councilArgs(CommandSender sender, List<String> displayedOptions, String[] args) {
		switch (args[0]) {
			case "whereis", "give", "mute", "unmute", "ban", "unban", "invsee", "warn", "punishments", "perks", "sudo" -> {
				// List of online players
				if (args.length == 2) {
					Player[] onlinePlayers = new Player[Bukkit.getOnlinePlayers().size()];
					Bukkit.getOnlinePlayers().toArray(onlinePlayers);
					boolean wasPlayerFound = false;
					for (Player onlinePlayer : onlinePlayers) {
						// Only display the name if it aligns with one that is currently online
						if (onlinePlayer.getName().toLowerCase().startsWith(args[1].toLowerCase())) {
							displayedOptions.add(onlinePlayer.getName());
							wasPlayerFound = true;
						} else if (args[1].isEmpty()) {
							displayedOptions.add(onlinePlayer.getName());
							wasPlayerFound = true;
						}
					}
					if (!wasPlayerFound) {
						for (Player onlinePlayer : onlinePlayers) {
							displayedOptions.add(onlinePlayer.getName());
						}
					}
				}
				// More than 1 sub-command
				else {
					switch (args[0]) {
						case "give" -> {
							if (args[2].isEmpty()) {
								displayedOptions.add("item");
							}
						}
						case "mute", "ban" -> {
							if (args.length == 3) {
								if (args[2].isEmpty()) {
									displayedOptions.add("1m");
									displayedOptions.add("1h");
									displayedOptions.add("1d");
									displayedOptions.add("1w");
									displayedOptions.add("-1");
								}
							} else if (args.length == 4) {
								if (args[3].isEmpty()) {
									displayedOptions.add("reason");
								}
							}
						}
						case "warn" -> {
							if (args[2].isEmpty()) {
								displayedOptions.add("reason");
							}
						}
						case "punishments" -> {
							if (args.length == 3) {
								if (!args[2].isEmpty() && "remove".startsWith(args[2])) {
									displayedOptions.add("remove");
								} else {
									displayedOptions.add("remove");
								}
							} else if (args.length == 4) {
								if (args[3].isEmpty()) {
									displayedOptions.add("number");
								}
							}
						}
						case "perks" -> {
							// /ac perks <player> <perk> <value>
							// Specifying the perk
							if (args.length == 3) {
								if (args[2].isEmpty()) {
									displayedOptions.add("compressor");
									displayedOptions.add("randomizer");
									displayedOptions.add("blacklist");
									displayedOptions.add("tables");
									displayedOptions.add("itemname");
									displayedOptions.add("chat");
									displayedOptions.add("shulker");
									displayedOptions.add("inventory");
									displayedOptions.add("homes");
									displayedOptions.add("itemframe");
									displayedOptions.add("bluefire");
									displayedOptions.add("discord");
								} else if ("randomizer".startsWith(args[2])) {
									displayedOptions.add("randomizer");
								} else if (args[2].startsWith("b")) {
									if (args[2].equals("b") || args[2].equals("bl")) {
										displayedOptions.add("blacklist");
										displayedOptions.add("bluefire");
									} else if ("blacklist".startsWith(args[2])) {
										displayedOptions.add("blacklist");
									} else if ("bluefire".startsWith(args[2])) {
										displayedOptions.add("bluefire");
									}
								} else if ("tables".startsWith(args[2])) {
									displayedOptions.add("tables");
								} else if ("shulker".startsWith(args[2])) {
									displayedOptions.add("shulker");
								} else if ("homes".startsWith(args[2])) {
									displayedOptions.add("homes");
								} else if ("discord".startsWith(args[2])) {
									displayedOptions.add("discord");
								} else if (args[2].startsWith("c")) {
									if (args[2].equals("c")) {
										displayedOptions.add("compress");
										displayedOptions.add("chat");
									} else if ("chat".startsWith(args[2])) {
										displayedOptions.add("chat");
									} else if ("compressor".startsWith(args[2])) {
										displayedOptions.add("compressor");
									}
								} else if (args[2].startsWith("i")) {
									if (args[2].equals("i")) {
										displayedOptions.add("inventory");
										displayedOptions.add("itemname");
										displayedOptions.add("itemframe");
									} else if ("inventory".startsWith(args[2])) {
										displayedOptions.add("inventory");
									} else {
										if ("item".startsWith(args[2])) {
											displayedOptions.add("itemname");
											displayedOptions.add("itemframe");
										} else {
											if ("itemname".startsWith(args[2])) {
												displayedOptions.add("itemname");
											} else if ("itemframe".startsWith(args[2])) {
												displayedOptions.add("itemframe");
											}
										}
									}
								} else {
									displayedOptions.add("compressor");
									displayedOptions.add("randomizer");
									displayedOptions.add("blacklist");
									displayedOptions.add("tables");
									displayedOptions.add("itemname");
									displayedOptions.add("chat");
									displayedOptions.add("shulker");
									displayedOptions.add("inventory");
									displayedOptions.add("homes");
									displayedOptions.add("itemframe");
									displayedOptions.add("bluefire");
									displayedOptions.add("discord");
								}
							}
						}
						case "sudo" -> {
							if (args[2].isEmpty()) {
								displayedOptions.add("command");
							}
						}
					}
				}
			}
			case "avatar" -> {
				if (args[1].isEmpty()) {
					displayedOptions.add("set");
				} else if ("set".startsWith(args[1])) {
					displayedOptions.add("set");
				}
			}
			case "broadcast" -> {
				if (args[1].isEmpty()) {
					displayedOptions.add("msg");
				}
			}
			case "boosts" -> {
				if (args.length == 2) {
					if (args[1].isEmpty()) {
						displayedOptions.add("add");
						displayedOptions.add("remove");
					} else if ("add".startsWith(args[1])) {
						displayedOptions.add("add");
					} else if ("remove".startsWith(args[1])) {
						displayedOptions.add("remove");
					} else {
						displayedOptions.add("add");
						displayedOptions.add("remove");
					}
				} else if (args.length == 3) {
					displayedOptions.add("MINER");
					displayedOptions.add("HARVEST");
					displayedOptions.add("HUNTER");
					displayedOptions.add("CHI");
				} else if (args.length == 4) {
					Player[] onlinePlayers = new Player[Bukkit.getOnlinePlayers().size()];
					Bukkit.getOnlinePlayers().toArray(onlinePlayers);
					boolean wasPlayerFound = false;
					for (Player onlinePlayer : onlinePlayers) {
						// Only display the name if it aligns with one that is currently online
						if (onlinePlayer.getName().toLowerCase().startsWith(args[1].toLowerCase())) {
							displayedOptions.add(onlinePlayer.getName());
							wasPlayerFound = true;
						} else if (args[1].isEmpty()) {
							displayedOptions.add(onlinePlayer.getName());
							wasPlayerFound = true;
						}
					}
					if (!wasPlayerFound) {
						for (Player onlinePlayer : onlinePlayers) {
							displayedOptions.add(onlinePlayer.getName());
						}
					}
				}
			}
			case "vote" -> {
				if (args.length == 2) {
					if (!args[1].isEmpty() && "test".startsWith(args[1])) {
						displayedOptions.add("test");
					} else {
						displayedOptions.add("test");
					}
				}
			}
			case "msg" -> {
				if (args[1].isEmpty()) {
					displayedOptions.add("message");
				}
			}
			case "speed" -> {
				if (args[1].isEmpty()) {
					displayedOptions.add("1");
					displayedOptions.add("10");
				}
			}
			case "time" -> {
				if (args[1].isEmpty()) {
					displayedOptions.add("day");
					displayedOptions.add("noon");
					displayedOptions.add("night");
					displayedOptions.add("midnight");
				}
			}
			case "tp", "tpf" -> {
				if (args.length == 2) {
					if (args[1].isEmpty()) {
						if (args[0].equalsIgnoreCase("tp")) {
							displayedOptions.add("username");
						}
						displayedOptions.add("x");
					}
				} else if (args.length == 3) {
					boolean hasOnlinePlayer = false;
					// Checks to see if the field is an online player's username
					for (Player onlinePlayer : Bukkit.getOnlinePlayers()) {
						if (args[1].equalsIgnoreCase(onlinePlayer.getName())) {
							hasOnlinePlayer = true;
						}
					}

					// If teleporting players
					if (hasOnlinePlayer) {
						if (args[2].isEmpty()) {
							if (args[0].equalsIgnoreCase("tp")) {
								displayedOptions.add("username");
							}
						}
					}
					// If teleporting self to coordinates
					else {
						if (args[2].isEmpty()) {
							displayedOptions.add("y");
						}
					}
				} else if (args.length == 4) {
					if (args[3].isEmpty()) {
						displayedOptions.add("z");
					}
				}  else if (args.length == 5) {
					if (args[4].isEmpty()) {
						displayedOptions.add("yaw");
					}
				}  else if (args.length == 6) {
					if (args[5].isEmpty()) {
						displayedOptions.add("pitch");
					}
				}
			}
			case "dateset" -> {
				if (args[1].isEmpty()) {
					displayedOptions.add("month");
					displayedOptions.add("day");
					displayedOptions.add("weekday");
					displayedOptions.add("year");
				}
			}
		}
		return displayedOptions;
	}

	/**
	 * Displays the commands available to council members when the input does not match an existing command.
	 * @param displayedOptions The list of options to be displayed.
	 * @return The updated list of options to be displayed.
	 */
	private List<String> noResultsCouncil(List<String> displayedOptions) {
		// Council-specific commands only
		displayedOptions.add("whereis");
		displayedOptions.add("give");
		displayedOptions.add("mute");
		displayedOptions.add("unmute");
		displayedOptions.add("ban");
		displayedOptions.add("unban");
		displayedOptions.add("invsee");
		displayedOptions.add("warn");
		displayedOptions.add("broadcast");
		displayedOptions.add("punishments");
		displayedOptions.add("perks");
		displayedOptions.add("admin");
		displayedOptions.add("msg");
		displayedOptions.add("speed");
		displayedOptions.add("time");
		displayedOptions.add("tp");
		displayedOptions.add("tpf");
		displayedOptions.add("clearchat");
		displayedOptions.add("dateset");
		displayedOptions.add("vanish");
		displayedOptions.add("sudo");
		return displayedOptions;
	}

}
