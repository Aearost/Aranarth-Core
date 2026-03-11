package com.aearost.aranarthcore.commands.general;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

/**
 * Handles the auto complete functionality while using the /teleport command.
 * Note that overrides are handled in CommandOverrides.
 */
public class CommandTeleportCompleter implements TabCompleter {

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
			Player[] onlinePlayers = new Player[Bukkit.getOnlinePlayers().size()];
			Bukkit.getOnlinePlayers().toArray(onlinePlayers);
			boolean wasPlayerFound = false;
			for (Player onlinePlayer : onlinePlayers) {
				// Only display the name if it aligns with one that is currently online
				if (onlinePlayer.getName().toLowerCase().startsWith(args[0].toLowerCase())) {
					wasPlayerFound = true;
					displayedOptions.add(onlinePlayer.getName());
				} else if (args[0].isEmpty()) {
					wasPlayerFound = true;
					displayedOptions.add(onlinePlayer.getName());
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
