package com.aearost.aranarthcore.commands.general;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

/**
 * Handles the auto complete functionality while using the /boosts command.
 */
public class CommandBoostsCompleter implements TabCompleter {

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
		// Display nothing for users that do not have permission to the sub-commands
		if (sender instanceof Player player) {
			if (!player.hasPermission("aranarth.boosts.modify")) {
				return displayedOptions;
			}
		}

		if (args.length == 1) {
			if (args[0].isEmpty()) {
				displayedOptions.add("add");
				displayedOptions.add("remove");
			} else if ("add".startsWith(args[0])) {
				displayedOptions.add("add");
			} else if ("remove".startsWith(args[0])) {
				displayedOptions.add("remove");
			} else {
				displayedOptions.add("add");
				displayedOptions.add("remove");
			}
		} else if (args.length == 2) {
			displayedOptions.add("MINER");
			displayedOptions.add("HARVEST");
			displayedOptions.add("HUNTER");
			displayedOptions.add("CHI");
		} else if (args.length == 3) {
			Player[] onlinePlayers = new Player[Bukkit.getOnlinePlayers().size()];
			Bukkit.getOnlinePlayers().toArray(onlinePlayers);
			boolean wasPlayerFound = false;
			for (Player onlinePlayer : onlinePlayers) {
				// Only display the name if it aligns with one that is currently online
				if (onlinePlayer.getName().toLowerCase().startsWith(args[2].toLowerCase())) {
					displayedOptions.add(onlinePlayer.getName());
					wasPlayerFound = true;
				} else if (args[2].isEmpty()) {
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
		return displayedOptions;
	}
}
