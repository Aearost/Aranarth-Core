package com.aearost.aranarthcore.commands;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

public class CommandACCompleter implements TabCompleter {

	/**
	 * Handles the auto complete functionality while using the /ac command.
	 */
	@Override
	public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
		List<String> displayedOptions = new ArrayList<>();

		if (args.length == 1) {
			if (!args[0].equals("") && "homepad".startsWith(args[0])) {
				displayedOptions.add("homepad");
			} else if (!args[0].equals("") && "nick".startsWith(args[0])) {
				displayedOptions.add("nick");
			} else if (!args[0].equals("") && "ping".startsWith(args[0])) {
				displayedOptions.add("ping");
			} else if (!args[0].equals("") && "prefix".startsWith(args[0])) {
				displayedOptions.add("prefix");
			} else if (!args[0].equals("") && "arena".startsWith(args[0])) {
				displayedOptions.add("arena");
			} else if (!args[0].equals("") && "creative".startsWith(args[0])) {
				displayedOptions.add("creative");
			} else if (!args[0].equals("") && args[0].startsWith("s")) {
				if (args[0].equals("s")) {
					displayedOptions.add("survival");
					displayedOptions.add("swimtoggle");
				} else if (!args[0].equals("") && "swimtoggle".startsWith(args[0])) {
					displayedOptions.add("swimtoggle");
				} else if (!args[0].equals("") && "survival".startsWith(args[0])) {
					displayedOptions.add("survival");
				} else {
					// Uncaught potential here!!!
				}
			}
			// Show all sub-commands of /ac
			else {
				displayedOptions.add("homepad");
				displayedOptions.add("swimtoggle");
				displayedOptions.add("nick");
				displayedOptions.add("ping");
				displayedOptions.add("prefix");
				displayedOptions.add("arena");
				displayedOptions.add("survival");
				displayedOptions.add("creative");
			}
		}

		if (args.length > 1) {
			if (args[0].equals("homepad")) {
				if (args.length == 2) {
					if (!args[1].equals("") && "create".startsWith(args[1])) {
						displayedOptions.add("create");
					} else if (!args[1].equals("") && "give".startsWith(args[1])) {
						displayedOptions.add("give");
					}
					else {
						displayedOptions.add("create");
						displayedOptions.add("give");
					}
				} else {
					if (args[1].toLowerCase().equals("give")) {
						Player[] onlinePlayers = new Player[Bukkit.getOnlinePlayers().size()];
						Bukkit.getOnlinePlayers().toArray(onlinePlayers);
						for (int i = 0; i < onlinePlayers.length; i++) {
							// Only display the name if it aligns with one that is currently online
							if (onlinePlayers[i].getName().toLowerCase().startsWith(args[2].toLowerCase())) {
								displayedOptions.add(onlinePlayers[i].getName());
							} else if (args[1].equals("")) {
								displayedOptions.add(onlinePlayers[i].getName());
							}
						}
					}
				}

			} else if (args[0].equals("ping")) {
				Player[] onlinePlayers = new Player[Bukkit.getOnlinePlayers().size()];
				Bukkit.getOnlinePlayers().toArray(onlinePlayers);
				for (int i = 0; i < onlinePlayers.length; i++) {
					// Only display the name if it aligns with one that is currently online
					if (onlinePlayers[i].getName().toLowerCase().startsWith(args[1].toLowerCase())) {
						displayedOptions.add(onlinePlayers[i].getName());
					} else if (args[1].equals("")) {
						displayedOptions.add(onlinePlayers[i].getName());
					}
				}
			}
		}

		return displayedOptions;
	}

}
