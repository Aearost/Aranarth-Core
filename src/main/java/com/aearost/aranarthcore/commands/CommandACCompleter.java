package com.aearost.aranarthcore.commands;

import com.aearost.aranarthcore.objects.AranarthPlayer;
import com.aearost.aranarthcore.objects.Dominion;
import com.aearost.aranarthcore.objects.Home;
import com.aearost.aranarthcore.utils.AranarthUtils;
import com.aearost.aranarthcore.utils.ChatUtils;
import com.aearost.aranarthcore.utils.DominionUtils;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

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
			} else {
				if (args.length == 1) {
					displayedOptions = all(player, displayedOptions, args);
					if (displayedOptions.isEmpty()) {
						displayedOptions = noResultsAll(displayedOptions);
					}
				} else {
					displayedOptions = allArgs(sender, displayedOptions, args);
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
	 * Displays the commands available to all players.
	 * @param sender The user that entered the command.
	 * @param displayedOptions The list of options to be displayed.
	 * @param args The arguments of the command.
	 * @return The updated list of options to be displayed.
	 */
	private List<String> all(CommandSender sender, List<String> displayedOptions, String[] args) {
		if (!args[0].isEmpty() && args[0].startsWith("h")) {
			if (args[0].equals("h")) {
				displayedOptions.add("homepad");
				displayedOptions.add("home");
				displayedOptions.add("hat");
			} else if ("hat".startsWith(args[0])) {
				displayedOptions.add("hat");
			} else if ("home".startsWith(args[0])) {
				displayedOptions.add("homepad");
				displayedOptions.add("home");
			} else {
				if ("homepad".startsWith(args[0])) {
					displayedOptions.add("homepad");
				}
			}
		} else if (!args[0].isEmpty() && "nick".startsWith(args[0])) {
			displayedOptions.add("nick");
		} else if (!args[0].isEmpty() && "itemname".startsWith(args[0])) {
			displayedOptions.add("itemname");
		} else if (!args[0].isEmpty() && args[0].startsWith("a")) {
			if (args[0].equals("a")) {
				displayedOptions.add("arena");
				displayedOptions.add("aranarthium");
				displayedOptions.add("avatar");
			} else if ("avatar".startsWith(args[0])) {
				displayedOptions.add("avatar");
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
				displayedOptions.add("compress");
			} else if ("creative".startsWith(args[0])) {
				displayedOptions.add("creative");
			} else if ("calendar".startsWith(args[0])) {
				displayedOptions.add("calendar");
			} else if ("compress".startsWith(args[0])) {
				displayedOptions.add("compress");
			}
		} else if (!args[0].isEmpty() && args[0].startsWith("s")) {
			if (args[0].equals("s")) {
				displayedOptions.add("survival");
				displayedOptions.add("sethome");
				displayedOptions.add("shulker");
				displayedOptions.add("smp");
				displayedOptions.add("seen");
				displayedOptions.add("store");
			} else if ("survival".startsWith(args[0])) {
				displayedOptions.add("survival");
			} else if ("shulker".startsWith(args[0])) {
				displayedOptions.add("shulker");
			} else if ("smp".startsWith(args[0])) {
				displayedOptions.add("smp");
			} else {
				if (args[0].equals("se")) {
					displayedOptions.add("sethome");
					displayedOptions.add("seen");
				} else if ("sethome".startsWith(args[0])) {
					displayedOptions.add("sethome");
				} else if ("seen".startsWith(args[0])) {
					displayedOptions.add("seen");
				} else if ("store".startsWith(args[0])) {
					displayedOptions.add("store");
				}
			}
		} else if (!args[0].isEmpty() && args[0].startsWith("b")) {
			if (args[0].equals("b")) {
				displayedOptions.add("blacklist");
				displayedOptions.add("balance");
				displayedOptions.add("baltop");
				displayedOptions.add("back");
				displayedOptions.add("boosts");
			} else if ("blacklist".startsWith(args[0])) {
				displayedOptions.add("blacklist");
			} else if ("boosts".startsWith(args[0])) {
				displayedOptions.add("boosts");
			}  else {
				if (args[0].startsWith("ba")) {
					if (args[0].equals("ba")) {
						displayedOptions.add("balance");
						displayedOptions.add("baltop");
						displayedOptions.add("back");
					} else if (args[0].equals("bal")) {
						displayedOptions.add("balance");
						displayedOptions.add("baltop");
					} else if ("balance".startsWith(args[0])) {
						displayedOptions.add("balance");
					} else if ("baltop".startsWith(args[0])) {
						displayedOptions.add("baltop");
					} else if ("back".startsWith(args[0])) {
						displayedOptions.add("back");
					}
				}
			}
		} else if (!args[0].isEmpty() && args[0].startsWith("p")) {
			if (args[0].equals("p")) {
				displayedOptions.add("ping");
				displayedOptions.add("potions");
				displayedOptions.add("pronouns");
				displayedOptions.add("pay");
				displayedOptions.add("particles");
			} else if ("ping".startsWith(args[0])) {
				displayedOptions.add("ping");
			} else if ("potions".startsWith(args[0])) {
				displayedOptions.add("potions");
			} else if ("pronouns".startsWith(args[0])) {
				displayedOptions.add("pronouns");
			} else {
				if (args[0].equals("pa")) {
					displayedOptions.add("pay");
					displayedOptions.add("particles");
				} else if ("pay".startsWith(args[0])) {
					displayedOptions.add("pay");
				} else if ("particles".startsWith(args[0])) {
					displayedOptions.add("particles");
				}
			}
		} else if (!args[0].isEmpty() && args[0].startsWith("r")) {
			if (args[0].equals("r")) {
				displayedOptions.add("randomizer");
				displayedOptions.add("ranks");
				displayedOptions.add("rankup");
				displayedOptions.add("rules");
				displayedOptions.add("resource");
			} else {
				if (args[0].equals("ra")) {
					displayedOptions.add("randomizer");
					displayedOptions.add("ranks");
					displayedOptions.add("rankup");
				} else {
					if ("resource".startsWith(args[0])) {
						displayedOptions.add("resource");
					} else if (args[0].equals("ran")) {
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
						} else if ("rules".startsWith(args[0])) {
							displayedOptions.add("rules");
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
		} else if (!args[0].isEmpty() && args[0].startsWith("t")) {
			if (args[0].equals("t")) {
				displayedOptions.add("trust");
				displayedOptions.add("tp");
				displayedOptions.add("tphere");
				displayedOptions.add("tpaccept");
				displayedOptions.add("tpdeny");
				displayedOptions.add("tables");
			} else {
				if (args[0].startsWith("tp")) {
					if (args[0].equals("tp")) {
						displayedOptions.add("tp");
						displayedOptions.add("tphere");
						displayedOptions.add("tpaccept");
						displayedOptions.add("tpdeny");
					} else if ("tphere".startsWith(args[0])) {
						displayedOptions.add("tphere");
					} else if ("tpaccept".startsWith(args[0])) {
						displayedOptions.add("tpaccept");
					} else if ("tpdeny".startsWith(args[0])) {
						displayedOptions.add("tpdeny");
					}
				} else {
					if ("trust".startsWith(args[0])) {
						displayedOptions.add("trust");
					} else if ("tables".startsWith(args[0])) {
						displayedOptions.add("tables");
					}
				}
			}
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
		} else if (!args[0].isEmpty() && "warp".startsWith(args[0])) {
			displayedOptions.add("warp");
		} else if (!args[0].isEmpty() && "msg".startsWith(args[0])) {
			displayedOptions.add("msg");
		} else if (!args[0].isEmpty() && "vote".startsWith(args[0])) {
			displayedOptions.add("vote");
		}
		return displayedOptions;
	}

	/**
	 * Displays the sub-commands available for the given command.
	 * @param displayedOptions The list of options to be displayed.
	 * @param args The arguments of the command.
	 * @return The updated list of options to be displayed.
	 */
	private List<String> allArgs(CommandSender sender, List<String> displayedOptions, String[] args) {
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
			case "ping", "balance", "trust", "untrust", "tp", "tphere", "pay", "msg" -> {
				if (args.length == 2) {
					Player[] onlinePlayers = new Player[Bukkit.getOnlinePlayers().size()];
					Bukkit.getOnlinePlayers().toArray(onlinePlayers);
					boolean wasPlayerFound = false;
					for (Player onlinePlayer : onlinePlayers) {
						// Only display the name if it aligns with one that is currently online
						if (onlinePlayer.getName().toLowerCase().startsWith(args[1].toLowerCase())) {
							wasPlayerFound = true;
							displayedOptions.add(onlinePlayer.getName());
						} else if (args[1].isEmpty()) {
							wasPlayerFound = true;
							displayedOptions.add(onlinePlayer.getName());
						}
					}
					if (!wasPlayerFound) {
						for (Player onlinePlayer : onlinePlayers) {
							displayedOptions.add(onlinePlayer.getName());
						}
					}
				} else if (args.length == 3) {
					if (args[0].equalsIgnoreCase("pay")) {
						if (args[2].isEmpty()) {
							displayedOptions.add("amount");
						}
					} else if (args[0].equalsIgnoreCase("balance")) {
						if (sender instanceof Player player) {
							AranarthPlayer aranarthPlayer = AranarthUtils.getPlayer(player.getUniqueId());
							if (aranarthPlayer.getCouncilRank() == 3) {
								if (args[2].isEmpty()) {
									displayedOptions.add("amount");
								}
							}
						}
					} else if (args[0].equalsIgnoreCase("msg")) {
						if (args[2].isEmpty()) {
							displayedOptions.add("message");
						}
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
							displayedOptions.add(ChatUtils.stripColorFormatting(home.getName()));
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
							if (ChatUtils.stripColorFormatting(home.getName()).toLowerCase().startsWith(argsAsSingleString.toString().toLowerCase())) {
								displayedOptions.add(ChatUtils.stripColorFormatting(home.getName()));
								hasResults = true;
							}
						}
						// If there were no results, show all homes
						if (!hasResults) {
							for (Home home : aranarthPlayer.getHomes()) {
								displayedOptions.add(ChatUtils.stripColorFormatting(home.getName()));
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
							displayedOptions.add(ChatUtils.stripColorFormatting(home.getName()));
						}
					} else {
						for (Home home : aranarthPlayer.getHomes()) {
							if (ChatUtils.stripColorFormatting(home.getName()).toLowerCase().startsWith(argsAsSingleString.toString().toLowerCase())) {
								displayedOptions.add(ChatUtils.stripColorFormatting(home.getName()));
							}
						}
					}
				}
			}
			case "dominion" -> {
				displayedOptions = dominionArgsCompletion(sender, displayedOptions, args);
			}
			case "baltop" -> {
				if (args.length == 2) {
					if (args[1].isEmpty()) {
						displayedOptions.add("page");
					}
				}
			}
			case "particles" -> {
				if (args.length == 2) {
					if (args[1].isEmpty()) {
						displayedOptions.add("100");
						displayedOptions.add("10");
						displayedOptions.add("0");
					}
				}
			}
			case "seen" -> {
				if (args.length == 2) {
					if (args[1].isEmpty()) {
						displayedOptions.add("username");
					}
				}
			}
			case "itemname" -> {
				if (args.length == 2) {
					if (args[1].isEmpty()) {
						displayedOptions.add("remove");
						displayedOptions.add("name");
						displayedOptions.add("gradient");
						displayedOptions.add("gradientbold");
					} else if ("gradient".startsWith(args[1])) {
						displayedOptions.add("gradient");
						displayedOptions.add("gradientbold");
					} else if ("gradientbold".startsWith(args[1])) {
						displayedOptions.add("gradientbold");
					} else if ("remove".startsWith(args[1])) {
						displayedOptions.add("remove");
					}
				} else if (args.length > 2) {
					if (args[1].startsWith("gradient")) {
						if (args[2].isEmpty()) {
							displayedOptions.add("name");
						}
					}
				}
			}
			case "warp" -> {
				// Should always be a player executing this command
				if (sender instanceof Player player) {
					if (args.length == 2) {
						if (args[1].isEmpty()) {
							for (Home warp : AranarthUtils.getWarps()) {
								displayedOptions.add(ChatUtils.stripColorFormatting(warp.getName()));
							}
							if (player.hasPermission("aranarth.warp.modify")) {
								displayedOptions.add("create");
								displayedOptions.add("delete");
							}
						} else {
							boolean wasWarpFound = false;
							for (Home warp : AranarthUtils.getWarps()) {
								if (ChatUtils.stripColorFormatting(warp.getName()).toLowerCase().startsWith(args[1].toLowerCase())) {
									displayedOptions.add(ChatUtils.stripColorFormatting(warp.getName()));
									wasWarpFound = true;
								}
							}
							if (!wasWarpFound) {
								for (Home warp : AranarthUtils.getWarps()) {
									displayedOptions.add(ChatUtils.stripColorFormatting(warp.getName()));
								}
							}

							if (player.hasPermission("aranarth.warp.modify")) {
								if ("create".startsWith(args[1].toLowerCase())) {
									displayedOptions.add("create");
								} else if ("delete".startsWith(args[1].toLowerCase())) {
									displayedOptions.add("delete");
								}
							}
						}
					} else {
						if (player.hasPermission("aranarth.warp.modify")) {
							if (args[1].equalsIgnoreCase("create") || args[1].equalsIgnoreCase("delete")) {
								if (args[2].isEmpty()) {
									if (args[1].equalsIgnoreCase("create")) {
										displayedOptions.add("name");
									} else if (args[1].equalsIgnoreCase("delete")) {
										for (Home warp : AranarthUtils.getWarps()) {
											displayedOptions.add(ChatUtils.stripColorFormatting(warp.getName()));
										}
									}
								} else {
									if (args[1].equalsIgnoreCase("delete")) {
										boolean wasWarpFound = false;
										for (Home warp : AranarthUtils.getWarps()) {
											if (ChatUtils.stripColorFormatting(warp.getName()).toLowerCase().startsWith(args[2].toLowerCase())) {
												displayedOptions.add(ChatUtils.stripColorFormatting(warp.getName()));
												wasWarpFound = true;
											}
										}
										if (!wasWarpFound) {
											for (Home warp : AranarthUtils.getWarps()) {
												displayedOptions.add(ChatUtils.stripColorFormatting(warp.getName()));
											}
										}
									}
								}
							}
						}
					}
				}
			}
			case "compress" -> {
				if (args.length == 2) {
					displayedOptions.add("toggle");
				}
			}
			case "vote" -> {
				if (args.length == 2) {
					if (!args[1].isEmpty() && "stats".startsWith(args[1])) {
						displayedOptions.add("stats");
					} else {
						displayedOptions.add("stats");
					}
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
	private List<String> noResultsAll(List<String> displayedOptions) {
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
		displayedOptions.add("itemname");
		displayedOptions.add("tp");
		displayedOptions.add("tphere");
		displayedOptions.add("tpaccept");
		displayedOptions.add("tpdeny");
		displayedOptions.add("pay");
		displayedOptions.add("baltop");
		displayedOptions.add("particles");
		displayedOptions.add("seen");
		displayedOptions.add("warp");
		displayedOptions.add("msg");
		displayedOptions.add("rules");
		displayedOptions.add("back");
		displayedOptions.add("resource");
		displayedOptions.add("avatar");
		displayedOptions.add("tables");
		displayedOptions.add("hat");
		displayedOptions.add("store");
		displayedOptions.add("boosts");
		displayedOptions.add("compress");
		displayedOptions.add("vote");
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
		} else if (!args[0].isEmpty() && "mute".startsWith(args[0])) {
			displayedOptions.add("mute");
		} else if (!args[0].isEmpty() && args[0].startsWith("b")) {
			if (args[0].equals("b")) {
				displayedOptions.add("ban");
				displayedOptions.add("broadcast");
			} else if (!args[0].isEmpty() && "ban".startsWith(args[0])) {
				displayedOptions.add("ban");
			} else if (!args[0].isEmpty() && "broadcast".startsWith(args[0])) {
				displayedOptions.add("broadcast");
			}
		} else if (!args[0].isEmpty() && "spy".startsWith(args[0])) {
			displayedOptions.add("spy");
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
		} else if (!args[0].isEmpty() && "cmsg".startsWith(args[0])) {
			displayedOptions.add("cmsg");
		}
		displayedOptions = all(sender, displayedOptions, args);
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
			case "whereis", "give", "mute", "unmute", "ban", "unban", "invsee", "spy", "warn", "punishments", "perks" -> {
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
			case "cmsg" -> {
				if (args[1].isEmpty()) {
					displayedOptions.add("message");
				}
			}
		}
		displayedOptions = allArgs(sender, displayedOptions, args);
		return displayedOptions;
	}

	/**
	 * Displays the commands available to council members when the input does not match an existing command.
	 * @param displayedOptions The list of options to be displayed.
	 * @return The updated list of options to be displayed.
	 */
	private List<String> noResultsCouncil(List<String> displayedOptions) {
		// Op-specific commands only
		displayedOptions.add("whereis");
		displayedOptions.add("give");
		displayedOptions.add("mute");
		displayedOptions.add("unmute");
		displayedOptions.add("ban");
		displayedOptions.add("unban");
		displayedOptions.add("invsee");
		displayedOptions.add("spy");
		displayedOptions.add("warn");
		displayedOptions.add("broadcast");
		displayedOptions.add("punishments");
		displayedOptions.add("perks");
		displayedOptions.add("admin");
		displayedOptions.add("cmsg");
		displayedOptions = noResultsAll(displayedOptions);
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
		displayedOptions.add("info");
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
		displayedOptions.add("deposit");
		displayedOptions.add("withdraw");
		return displayedOptions;
	}

	/**
	 * Helper method to add all dominion sub-command arguments to the displayed command options.
	 * @param sender The user that entered the command.
	 * @param displayedOptions The list of options to be displayed.
	 * @param args The arguments of the command.
	 * @return The list of options to be displayed.
	 */
	private static List<String> dominionArgsCompletion(CommandSender sender, List<String> displayedOptions, String[] args) {
		if (args[1].isEmpty()) {
			displayedOptions = addDominionSubCommands(displayedOptions);
		} else {
			if (args.length == 2) {
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
				} else if (args[1].startsWith("i")) {
					if (args[1].equals("i") || args[1].equals("in")) {
						displayedOptions.add("invite");
						displayedOptions.add("info");
					} else if ("invite".startsWith(args[1])) {
						displayedOptions.add("invite");
					} else if ("info".startsWith(args[1])) {
						displayedOptions.add("info");
					} else {
						displayedOptions = addDominionSubCommands(displayedOptions);
					}
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
				} else if ("unclaim".startsWith(args[1])) {
					displayedOptions.add("unclaim");
				} else if ("balance".startsWith(args[1])) {
					displayedOptions.add("balance");
				} else if ("home".startsWith(args[1])) {
					displayedOptions.add("home");
				} else if ("sethome".startsWith(args[1])) {
					displayedOptions.add("sethome");
				} else if (args[1].startsWith("d")) {
					if (args[1].equalsIgnoreCase("d")) {
						displayedOptions.add("disband");
						displayedOptions.add("deposit");
					} else if ("disband".startsWith(args[1])) {
						displayedOptions.add("disband");
					} else if ("deposit".startsWith(args[1])) {
						displayedOptions.add("deposit");
					} else {
						displayedOptions = addDominionSubCommands(displayedOptions);
					}
				} else if (args[1].startsWith("w")) {
					if (args[1].equalsIgnoreCase("w")) {
						displayedOptions.add("who");
						displayedOptions.add("withdraw");
					} else if ("who".startsWith(args[1])) {
						displayedOptions.add("who");
					} else if ("withdraw".startsWith(args[1])) {
						displayedOptions.add("withdraw");
					} else {
						displayedOptions = addDominionSubCommands(displayedOptions);
					}
				} else {
					displayedOptions = addDominionSubCommands(displayedOptions);
				}
			} else {
				if (args[2].isEmpty()) {
					switch (args[1]) {
						case "create" -> displayedOptions.add("name");
						case "invite", "who" -> {
							for (Player onlinePlayer : Bukkit.getOnlinePlayers()) {
								displayedOptions.add(onlinePlayer.getName());
							}
						}
						case "remove" -> {
							if (sender instanceof Player player) {
								boolean resultsFound = false;
								Dominion dominion = DominionUtils.getPlayerDominion(player.getUniqueId());
								if (dominion != null) {
									for (UUID uuid : dominion.getMembers()) {
										displayedOptions.add(Bukkit.getPlayer(uuid).getName());
									}
								}
							}
						}
						case "info" -> {
							List<Dominion> dominions = DominionUtils.getDominions();
							for (Dominion dominionFromList : dominions) {
								displayedOptions.add(ChatUtils.stripColorFormatting(dominionFromList.getName()));
							}
						}
					}
				} else {
					switch (args[1]) {
						case "invite", "who" -> {
							boolean resultsFound = false;
							for (Player onlinePlayer : Bukkit.getOnlinePlayers()) {
								if (onlinePlayer.getName().toLowerCase().startsWith(args[2].toLowerCase())) {
									displayedOptions.add(onlinePlayer.getName());
									resultsFound = true;
								}
							}
							// If none were found, display all
							if (!resultsFound) {
								for (Player onlinePlayer : Bukkit.getOnlinePlayers()) {
									displayedOptions.add(onlinePlayer.getName());
								}
							}
						}
						case "remove" -> {
							if (sender instanceof Player player) {
								boolean resultsFound = false;
								Dominion dominion = DominionUtils.getPlayerDominion(player.getUniqueId());
								if (dominion != null) {
									for (UUID uuid : dominion.getMembers()) {
										if (Bukkit.getPlayer(uuid).getName().toLowerCase().startsWith(args[2].toLowerCase())) {
											displayedOptions.add(Bukkit.getPlayer(uuid).getName());
											resultsFound = true;
										}
									}

									// If none were found, display all
									if (!resultsFound) {
										for (UUID uuid : dominion.getMembers()) {
											displayedOptions.add(Bukkit.getPlayer(uuid).getName());
										}
									}
								}
							}
						}
						case "info" -> {
							StringBuilder dominionNameBuilder = new StringBuilder();
							for (int i = 2; i < args.length; i++) {
								dominionNameBuilder.append(args[i]);
								if (i < args.length - 1) {
									dominionNameBuilder.append(" ");
								}
							}

							List<Dominion> dominions = DominionUtils.getDominions();
							boolean wasFound = false;
							for (Dominion dominionFromList : dominions) {
								if (ChatUtils.stripColorFormatting(dominionFromList.getName()).toLowerCase().startsWith(dominionNameBuilder.toString().toLowerCase())) {
									displayedOptions.add(ChatUtils.stripColorFormatting(dominionFromList.getName()));
									wasFound = true;
								}
							}
							// Display all dominion names if what was entered does not match any dominion names
							if (!wasFound) {
								for (Dominion dominionFromList : dominions) {
									displayedOptions.add(ChatUtils.stripColorFormatting(dominionFromList.getName()));
								}
							}
						}
					}
				}
			}
		}
		return displayedOptions;
	}

}
