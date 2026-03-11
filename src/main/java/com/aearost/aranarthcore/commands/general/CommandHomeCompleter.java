package com.aearost.aranarthcore.commands.general;

import com.aearost.aranarthcore.objects.AranarthPlayer;
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
		List<String> displayedOptions = new ArrayList<>();
		if (sender instanceof Player player) {
			AranarthPlayer aranarthPlayer = AranarthUtils.getPlayer(player.getUniqueId());
			if (args.length == 0) {
				// Show all homes
				for (Home home : aranarthPlayer.getHomes()) {
					displayedOptions.add(ChatUtils.stripColorFormatting(home.getName()));
				}
			} else {
				// Display all homes starting with what's entered, otherwise show all
				boolean hasResults = false;
				for (Home home : aranarthPlayer.getHomes()) {
					StringBuilder argsAsSingleString = new StringBuilder();
					for (int i = 0; i < args.length; i++) {
						argsAsSingleString.append(args[i]);
						if (i < args.length - 1) {
							argsAsSingleString.append(" ");
						}
					}
					if (ChatUtils.stripColorFormatting(home.getName()).toLowerCase().startsWith(argsAsSingleString.toString().toLowerCase())) {
						displayedOptions.add(ChatUtils.stripColorFormatting(home.getName()));
						hasResults = true;
					}
				}
				// If there were no results, show all homes
				if (!hasResults) {
					for (Home home : aranarthPlayer.getHomes()) {
						displayedOptions.add(ChatUtils.stripColorFormatting(home.getName()));
					}
				}
			}
		}
		return displayedOptions;
	}
}
