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
			} else if (!args[0].equals("") && "swimtoggle".startsWith(args[0])) {
				displayedOptions.add("swimtoggle");
			} else if (!args[0].equals("") && "nick".startsWith(args[0])) {
				displayedOptions.add("nick");
			} else if (!args[0].equals("") && "ping".startsWith(args[0])) {
				displayedOptions.add("ping");
			} else if (!args[0].equals("") && "prefix".startsWith(args[0])) {
				displayedOptions.add("prefix");
			} else {
				displayedOptions.add("homepad");
				displayedOptions.add("swimtoggle");
				displayedOptions.add("nick");
				displayedOptions.add("ping");
				displayedOptions.add("prefix");
			}
		} else if (args.length == 2) {
			// Only display the name if it aligns with one that is currently online
			if (args[0].equals("give")) {
				Player[] onlinePlayers = new Player[Bukkit.getOnlinePlayers().size()];
				Bukkit.getOnlinePlayers().toArray(onlinePlayers);
				for (int i = 0; i < onlinePlayers.length; i++) {
					if (onlinePlayers[i].getName().toLowerCase().startsWith(args[1].toLowerCase())) {
						displayedOptions.add(onlinePlayers[i].getName());
					} else if (args[1].equals("")) {
						displayedOptions.add(onlinePlayers[i].getName());
					}
				}
			} else if (args[0].equals("create")) {
				if (args[1].equals("")) {
					displayedOptions.add("name");
				}
			}
		}
		
		return displayedOptions;
	}
	
}
