package com.aearost.aranarthcore.commands.general;

import com.aearost.aranarthcore.objects.Dominion;
import com.aearost.aranarthcore.utils.ChatUtils;
import com.aearost.aranarthcore.utils.DominionUtils;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Handles the auto complete functionality while using the /dominion command.
 */
public class CommandDominionCompleter implements TabCompleter {

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
		if (args[0].isEmpty()) {
			displayedOptions = addDominionSubCommands(displayedOptions);
		} else {
			if (args.length == 1) {
				if (args[0].startsWith("c")) {
					if (args[0].equalsIgnoreCase("c")) {
						displayedOptions.add("create");
						displayedOptions.add("claim");
						displayedOptions.add("conquer");
					} else if ("create".startsWith(args[0])) {
						displayedOptions.add("create");
					} else if ("claim".startsWith(args[0])) {
						displayedOptions.add("claim");
					} else if ("conquer".startsWith(args[0])) {
						displayedOptions.add("conquer");
					} else {
						displayedOptions = addDominionSubCommands(displayedOptions);
					}
				} else if (args[0].startsWith("i")) {
					if (args[0].equals("i") || args[0].equals("in")) {
						displayedOptions.add("invite");
						displayedOptions.add("info");
					} else if ("invite".startsWith(args[0])) {
						displayedOptions.add("invite");
					} else if ("info".startsWith(args[0])) {
						displayedOptions.add("info");
					} else {
						displayedOptions = addDominionSubCommands(displayedOptions);
					}
				} else if (args[0].startsWith("a")) {
					if (args[0].equals("a")) {
						displayedOptions.add("accept");
						displayedOptions.add("ally");
						displayedOptions.add("autoclaim");
					} else if ("accept".startsWith(args[0])) {
						displayedOptions.add("accept");
					} else if ("ally".startsWith(args[0])) {
						displayedOptions.add("ally");
					} else if ("autoclaim".startsWith(args[0])) {
						displayedOptions.add("autoclaim");
					} else {
						displayedOptions = addDominionSubCommands(displayedOptions);
					}
				} else if (args[0].startsWith("l")) {
					if (args[0].equalsIgnoreCase("l")) {
						displayedOptions.add("list");
						displayedOptions.add("leave");
					} else if ("list".startsWith(args[0])) {
						displayedOptions.add("list");
					} else if ("leave".startsWith(args[0])) {
						displayedOptions.add("leave");
					} else {
						displayedOptions = addDominionSubCommands(displayedOptions);
					}
				} else if (args[0].startsWith("r")) {
					if (args[0].equalsIgnoreCase("r") || args[0].equalsIgnoreCase("re")) {
						displayedOptions.add("remove");
						displayedOptions.add("rename");
						displayedOptions.add("resources");
						displayedOptions.add("rebel");
						displayedOptions.add("retreat");
					} else if ("remove".startsWith(args[0])) {
						displayedOptions.add("remove");
					} else if ("rename".startsWith(args[0])) {
						displayedOptions.add("rename");
					} else if ("resources".startsWith(args[0])) {
						displayedOptions.add("resources");
					} else if ("rebel".startsWith(args[0])) {
						displayedOptions.add("rebel");
					} else if ("retreat".startsWith(args[0])) {
						displayedOptions.add("retreat");
					} else {
						displayedOptions = addDominionSubCommands(displayedOptions);
					}
				} else if ("unclaim".startsWith(args[0])) {
					displayedOptions.add("unclaim");
				} else if ("balance".startsWith(args[0])) {
					displayedOptions.add("balance");
				} else if ("home".startsWith(args[0])) {
					displayedOptions.add("home");
				} else if (args[0].startsWith("s")) {
					if (args[0].equalsIgnoreCase("s")) {
						displayedOptions.add("sethome");
						displayedOptions.add("setleader");
						displayedOptions.add("surrender");
					} else if (args[0].equalsIgnoreCase("se") || args[0].equalsIgnoreCase("set")) {
						displayedOptions.add("sethome");
						displayedOptions.add("setleader");
					} else if ("sethome".startsWith(args[0])) {
						displayedOptions.add("sethome");
					} else if ("setleader".startsWith(args[0])) {
						displayedOptions.add("setleader");
					} else if ("surrender".startsWith(args[0])) {
						displayedOptions.add("surrender");
					} else {
						displayedOptions = addDominionSubCommands(displayedOptions);
					}
				} else if (args[0].startsWith("d")) {
					if (args[0].equalsIgnoreCase("d")) {
						displayedOptions.add("disband");
						displayedOptions.add("deposit");
					} else if ("disband".startsWith(args[0])) {
						displayedOptions.add("disband");
					} else if ("deposit".startsWith(args[0])) {
						displayedOptions.add("deposit");
					} else {
						displayedOptions = addDominionSubCommands(displayedOptions);
					}
				} else if (args[0].startsWith("w")) {
					if (args[0].equalsIgnoreCase("w")) {
						displayedOptions.add("who");
						displayedOptions.add("withdraw");
					} else if ("who".startsWith(args[0])) {
						displayedOptions.add("who");
					} else if ("withdraw".startsWith(args[0])) {
						displayedOptions.add("withdraw");
					} else {
						displayedOptions = addDominionSubCommands(displayedOptions);
					}
				} else if ("truce".startsWith(args[0])) {
					displayedOptions.add("truce");
				} else if ("enemy".startsWith(args[0])) {
					displayedOptions.add("enemy");
				} else if ("neutral".startsWith(args[0])) {
					displayedOptions.add("neutral");
				} else if ("map".startsWith(args[0])) {
					displayedOptions.add("map");
				} else if ("food".startsWith(args[0])) {
					displayedOptions.add("food");
				} else {
					displayedOptions = addDominionSubCommands(displayedOptions);
				}
			} else {
				if (args[0].isEmpty()) {
					switch (args[0]) {
						case "create" -> displayedOptions.add("name");
						case "invite", "who" -> {
							for (Player onlinePlayer : Bukkit.getOnlinePlayers()) {
								displayedOptions.add(onlinePlayer.getName());
							}
						}
						case "remove", "setleader" -> {
							if (sender instanceof Player player) {
								boolean resultsFound = false;
								Dominion dominion = DominionUtils.getPlayerDominion(player.getUniqueId());
								if (dominion != null) {
									for (UUID uuid : dominion.getMembers()) {
										displayedOptions.add(Bukkit.getOfflinePlayer(uuid).getName());
									}
								}
							}
						}
						case "info", "ally", "truce", "enemy", "neutral" -> {
							List<Dominion> dominions = DominionUtils.getDominions();
							for (Dominion dominionFromList : dominions) {
								displayedOptions.add(ChatUtils.stripColorFormatting(dominionFromList.getName()));
							}
						}
					}
				} else {
					switch (args[0]) {
						case "invite", "who" -> {
							boolean resultsFound = false;
							for (Player onlinePlayer : Bukkit.getOnlinePlayers()) {
								if (onlinePlayer.getName().toLowerCase().startsWith(args[0].toLowerCase())) {
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
						case "remove", "setleader" -> {
							if (sender instanceof Player player) {
								boolean resultsFound = false;
								Dominion dominion = DominionUtils.getPlayerDominion(player.getUniqueId());
								if (dominion != null) {
									for (UUID uuid : dominion.getMembers()) {
										if (Bukkit.getOfflinePlayer(uuid).getName().toLowerCase().startsWith(args[0].toLowerCase())) {
											displayedOptions.add(Bukkit.getOfflinePlayer(uuid).getName());
											resultsFound = true;
										}
									}

									// If none were found, display all
									if (!resultsFound) {
										for (UUID uuid : dominion.getMembers()) {
											displayedOptions.add(Bukkit.getOfflinePlayer(uuid).getName());
										}
									}
								}
							}
						}
						case "info", "ally", "truce", "enemy", "neutral", "conquer", "surrender", "rebel", "retreat" -> {
							StringBuilder dominionNameBuilder = new StringBuilder();
							for (int i = 1; i < args.length; i++) {
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
		displayedOptions.add("rename");
		displayedOptions.add("ally");
		displayedOptions.add("truce");
		displayedOptions.add("enemy");
		displayedOptions.add("neutral");
		displayedOptions.add("setleader");
		displayedOptions.add("map");
		displayedOptions.add("autoclaim");
		displayedOptions.add("food");
		displayedOptions.add("resources");
		displayedOptions.add("conquer");
		displayedOptions.add("surrender");
		displayedOptions.add("rebel");
		displayedOptions.add("retreat");
		return displayedOptions;
	}
}
