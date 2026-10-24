package com.aearost.aranarthcore.commands.general;

import com.aearost.aranarthcore.objects.AranarthPlayer;
import com.aearost.aranarthcore.utils.AranarthUtils;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.List;

/**
 * Tab completes /creative, which only has the Council-exclusive expand subcommand.
 */
public class CommandCreativeCompleter implements TabCompleter {

	@Override
	public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
		if (!(sender instanceof Player player)) {
			return List.of();
		}
		AranarthPlayer aranarthPlayer = AranarthUtils.getPlayer(player.getUniqueId());
		if (aranarthPlayer.getCouncilRank() != 3) {
			return List.of();
		}
		if (args.length == 1 && "expand".startsWith(args[0].toLowerCase())) {
			return List.of("expand");
		}
		if (args.length == 2 && args[0].equalsIgnoreCase("expand") && args[1].isEmpty()) {
			return List.of("rings");
		}
		return List.of();
	}
}
