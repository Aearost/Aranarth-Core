package com.aearost.aranarthcore.commands.general;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

/**
 * Handles the auto complete functionality while using the /shop command.
 */
public class CommandShopCompleter implements TabCompleter {

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
			if (args.length == 1) {
				if (args[0].isEmpty()) {
					for (Player onlinePlayer : Bukkit.getOnlinePlayers()) {
						displayedOptions.add(onlinePlayer.getName());
					}
					if (player.hasPermission("aranarth.shop.modify")) {
						displayedOptions.add("create");
						displayedOptions.add("delete");
					}
				} else {
					boolean wasShopFound = false;
					for (Player onlinePlayer : Bukkit.getOnlinePlayers()) {
						if (onlinePlayer.getName().toLowerCase().startsWith(args[0].toLowerCase())) {
							displayedOptions.add(onlinePlayer.getName());
							wasShopFound = true;
						}
					}
					if (!wasShopFound) {
						for (Player onlinePlayer : Bukkit.getOnlinePlayers()) {
							displayedOptions.add(onlinePlayer.getName());
						}
					}

					if (player.hasPermission("aranarth.warp.modify")) {
						if ("create".startsWith(args[0].toLowerCase())) {
							displayedOptions.add("create");
						} else if ("delete".startsWith(args[0].toLowerCase())) {
							displayedOptions.add("delete");
						}
					}
				}
			} else {
				if (player.hasPermission("aranarth.warp.modify")) {
					if (args[0].equalsIgnoreCase("create") || args[0].equalsIgnoreCase("delete")) {
						if (args[0].equalsIgnoreCase("create")) {
							if (args[1].isEmpty()) {
								displayedOptions.add("username");
							}
						} else if (args[0].equalsIgnoreCase("delete")) {
							if (args[1].isEmpty()) {
								displayedOptions.add("username");
							}
						}
					}
				}
			}
		}
		return displayedOptions;
	}
}
