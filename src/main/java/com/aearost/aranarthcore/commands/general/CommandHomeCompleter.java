package com.aearost.aranarthcore.commands.general;

import com.aearost.aranarthcore.objects.AranarthPlayer;
import com.aearost.aranarthcore.utils.AranarthUtils;
import com.aearost.aranarthcore.utils.ChatUtils;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Handles the auto complete functionality while using the /home command.
 */
public class CommandHomeCompleter implements TabCompleter {

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
		AranarthPlayer aranarthPlayer = AranarthUtils.getPlayer(player.getUniqueId());
		String input = String.join(" ", args);
		List<String> names = new ArrayList<>();
		if (player.getBedSpawnLocation() != null) {
			names.add(AranarthUtils.BED_HOME_NAME);
		}
		// Legacy homes named "Bed" are skipped since /home Bed always goes to the bed spawn
		aranarthPlayer.getHomes().stream()
			.map(home -> ChatUtils.stripColorFormatting(home.getName()))
			.filter(name -> !AranarthUtils.isReservedHomeName(name))
			.forEach(names::add);
		return names.stream()
			.filter(name -> input.isEmpty() || name.toLowerCase().startsWith(input.toLowerCase()))
			.collect(Collectors.toList());
	}
}
