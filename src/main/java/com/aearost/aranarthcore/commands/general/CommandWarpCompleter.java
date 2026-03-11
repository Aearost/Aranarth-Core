package com.aearost.aranarthcore.commands.general;

import com.aearost.aranarthcore.objects.Home;
import com.aearost.aranarthcore.utils.AranarthUtils;
import com.aearost.aranarthcore.utils.ChatUtils;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

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
		List<String> displayedOptions = new ArrayList<>();
		if (sender instanceof Player player) {
			if (args.length == 1) {
				if (args[0].isEmpty()) {
					for (Home warp : AranarthUtils.getWarps()) {
						displayedOptions.add(ChatUtils.stripColorFormatting(warp.getName()));
					}
					if (player.hasPermission("aranarth.warp.modify")) {
						displayedOptions.add("create");
						displayedOptions.add("delete");
					}
				} else {
					boolean wasWarpFound = false;
					for (Home warp : AranarthUtils.getWarps()) {
						if (ChatUtils.stripColorFormatting(warp.getName()).toLowerCase().startsWith(args[0].toLowerCase())) {
							displayedOptions.add(ChatUtils.stripColorFormatting(warp.getName()));
							wasWarpFound = true;
						}
					}
					if (!wasWarpFound) {
						for (Home warp : AranarthUtils.getWarps()) {
							displayedOptions.add(ChatUtils.stripColorFormatting(warp.getName()));
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
						if (args[1].isEmpty()) {
							if (args[0].equalsIgnoreCase("create")) {
								displayedOptions.add("name");
							} else if (args[0].equalsIgnoreCase("delete")) {
								for (Home warp : AranarthUtils.getWarps()) {
									displayedOptions.add(ChatUtils.stripColorFormatting(warp.getName()));
								}
							}
						} else {
							if (args[0].equalsIgnoreCase("delete")) {
								boolean wasWarpFound = false;
								for (Home warp : AranarthUtils.getWarps()) {
									if (ChatUtils.stripColorFormatting(warp.getName()).toLowerCase().startsWith(args[1].toLowerCase())) {
										displayedOptions.add(ChatUtils.stripColorFormatting(warp.getName()));
										wasWarpFound = true;
									}
								}
								if (!wasWarpFound) {
									for (Home warp : AranarthUtils.getWarps()) {
										displayedOptions.add(ChatUtils.stripColorFormatting(warp.getName()));
									}
								}
							}
						}
					}
				}
			}
		}
		return displayedOptions;
	}
}
