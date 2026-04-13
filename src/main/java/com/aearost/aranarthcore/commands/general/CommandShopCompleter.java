package com.aearost.aranarthcore.commands.general;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Handles the auto complete functionality while using the /shop command.
 */
public class CommandShopCompleter implements TabCompleter {

	private static final List<String> MODIFY_OPTIONS = List.of("create", "delete");

	/**
	 * @param sender The user that entered the command.
	 * @param command The command itself.
	 * @param alias The alias of the command.
	 * @param args The arguments of the command.
	 * @return Confirmation of whether the command was a success or not.
	 */
	@Override
	public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
		if (!(sender instanceof Player player)) {
			return List.of();
		}
		if (args.length == 1) {
			List<String> options = filterPlayers(args[0]);
			if (player.hasPermission("aranarth.shop.modify")) {
                options.addAll(filter(MODIFY_OPTIONS, args[0]));
			}
			return options;
		}
		if (args.length == 2 && player.hasPermission("aranarth.warp.modify")) {
			if (args[0].equalsIgnoreCase("create") || args[0].equalsIgnoreCase("delete")) {
				return args[1].isEmpty() ? List.of("username") : List.of();
			}
		}
		return List.of();
	}

	private static List<String> filter(List<String> options, String input) {
		return options.stream()
			.filter(s -> input.isEmpty() || s.toLowerCase().startsWith(input.toLowerCase()))
			.collect(Collectors.toList());
	}

	private static List<String> filterPlayers(String input) {
		return Bukkit.getOnlinePlayers().stream()
			.map(Player::getName)
			.filter(name -> input.isEmpty() || name.toLowerCase().startsWith(input.toLowerCase()))
			.collect(Collectors.toList());
	}
}
