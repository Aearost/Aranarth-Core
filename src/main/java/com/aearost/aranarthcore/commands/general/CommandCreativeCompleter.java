package com.aearost.aranarthcore.commands.general;

import com.aearost.aranarthcore.gui.GuiCreativeItems;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

/**
 * Tab completes /creative, which has the items menu while in a Creative world and the Council-exclusive expand subcommand.
 */
public class CommandCreativeCompleter implements TabCompleter {

	@Override
	public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
		if (!(sender instanceof Player player)) {
			return List.of();
		}
		boolean canExpand = player.hasPermission("aranarth.creative.expand");
		if (args.length == 1) {
			List<String> results = new ArrayList<>();
			if (GuiCreativeItems.canUse(player) && "items".startsWith(args[0].toLowerCase())) {
				results.add("items");
			}
			if (canExpand && "expand".startsWith(args[0].toLowerCase())) {
				results.add("expand");
			}
			return results;
		}
		if (canExpand && args.length == 2 && args[0].equalsIgnoreCase("expand") && args[1].isEmpty()) {
			return List.of("rings");
		}
		return List.of();
	}
}
