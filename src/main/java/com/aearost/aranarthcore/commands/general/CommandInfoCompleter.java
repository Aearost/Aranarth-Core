package com.aearost.aranarthcore.commands.general;

import com.aearost.aranarthcore.AranarthCore;
import com.aearost.aranarthcore.utils.AranarthUtils;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;

import java.util.List;

/**
 * Handles the auto complete functionality while using the /info command.
 */
public class CommandInfoCompleter implements TabCompleter {

	/**
	 * @param sender The user that entered the command.
	 * @param command The command itself.
	 * @param alias The alias of the command.
	 * @param args The arguments of the command.
	 * @return Confirmation of whether the command was a success or not.
	 */
	@Override
	public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
		if (args.length == 1) {
			List<String> result = filterPlayers(args[0]);
			Bukkit.getLogger().info(AranarthCore.LOG_PREFIX + "[TabDebug][/info] onTabComplete FIRED — args[0]=\"" + args[0]
					+ "\" returning " + result.size() + " result(s): " + result);
			return result;
		}
		Bukkit.getLogger().info(AranarthCore.LOG_PREFIX + "[TabDebug][/info] onTabComplete FIRED but args.length=" + args.length + " — returning empty");
		return List.of();
	}

	private static List<String> filterPlayers(String input) {
		return AranarthUtils.getNetworkPlayerNames(input);
	}
}
