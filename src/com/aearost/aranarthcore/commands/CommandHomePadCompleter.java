package com.aearost.aranarthcore.commands;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

public class CommandHomePadCompleter implements TabCompleter {

	/**
	 * Handles the auto complete functionality while using the /homepad command.
	 */
	@Override
	public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
		List<String> displayedOptions = new ArrayList<>();
		
		if (args.length == 1) {
			if (!args[0].equals("") && "give".startsWith(args[0])) {
				displayedOptions.add("give");
			} else if (!args[0].equals("") && "create".startsWith(args[0])) {
				displayedOptions.add("create");
			} else {
				displayedOptions.add("give");
				displayedOptions.add("create");
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
