package com.aearost.aranarthcore.commands.general;

import com.aearost.aranarthcore.utils.AranarthUtils;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;

import java.util.ArrayList;
import java.util.List;

/**
 * Handles the auto complete functionality while using the /avatar command.
 */
public class CommandAvatarCompleter implements TabCompleter {

	/**
	 * @param sender The user that entered the command.
	 * @param command The command itself.
	 * @param alias The alias of the command.
	 * @param args The arguments of the command.
	 * @return Confirmation of whether the command was a success or not.
	 */
	@Override
	public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
		if (!sender.hasPermission("aranarth.avatar.set")) {
			return List.of();
		}
		if (args.length == 1) {
			List<String> displayedOptions = new ArrayList<>();
			for (String option : List.of("set", "end")) {
				if (option.startsWith(args[0].toLowerCase())) {
					displayedOptions.add(option);
				}
			}
			return displayedOptions;
		} else if (args.length == 2 && args[0].equalsIgnoreCase("set")) {
			return AranarthUtils.getNetworkPlayerNames(args[1]);
		}
		return List.of();
	}
}
