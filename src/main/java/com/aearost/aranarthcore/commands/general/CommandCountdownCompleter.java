package com.aearost.aranarthcore.commands.general;

import com.aearost.aranarthcore.utils.AranarthUtils;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;

import java.util.List;

/**
 * Handles the auto complete functionality while using the /countdown command.
 */
public class CommandCountdownCompleter implements TabCompleter {

	@Override
	public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
		if (args.length == 1) {
			return AranarthUtils.getNetworkPlayerNames(args[0]);
		}
		return List.of();
	}
}
