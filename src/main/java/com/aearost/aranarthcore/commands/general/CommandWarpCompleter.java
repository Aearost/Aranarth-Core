package com.aearost.aranarthcore.commands.general;

import com.aearost.aranarthcore.utils.AranarthUtils;
import com.aearost.aranarthcore.utils.ChatUtils;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Handles the auto complete functionality while using the /warp command.
 * Note that overrides are handled in CommandOverrides.
 */
public class CommandWarpCompleter implements TabCompleter {

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
			List<String> options = filterWarps(args[0]);
			if (player.hasPermission("aranarth.warp.modify")) {
				Stream.of("create", "delete")
					.filter(s -> args[0].isEmpty() || s.startsWith(args[0].toLowerCase()))
					.forEach(options::add);
			}
			return options;
		}
		if (args.length == 2 && player.hasPermission("aranarth.warp.modify")) {
			if (args[0].equalsIgnoreCase("create")) {
				return args[1].isEmpty() ? List.of("name") : List.of();
			}
			if (args[0].equalsIgnoreCase("delete")) {
				return filterWarps(args[1]);
			}
		}
		return List.of();
	}

	private static List<String> filterWarps(String input) {
		return AranarthUtils.getWarps().stream()
			.map(warp -> ChatUtils.stripColorFormatting(warp.getName()))
			.filter(name -> input.isEmpty() || name.toLowerCase().startsWith(input.toLowerCase()))
			.collect(Collectors.toList());
	}
}
