package com.aearost.aranarthcore.commands.general;

import com.aearost.aranarthcore.objects.Boost;
import com.aearost.aranarthcore.utils.AranarthUtils;
import com.aearost.aranarthcore.utils.ChatUtils;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.UUID;

/**
 * Allows plays to view all active server boosts.
 * Additionally allows for council members to add or remove boosts.
 */
public class CommandBoosts implements CommandExecutor {

	/**
	 * @param sender The user that entered the command.
	 * @param command The command itself.
	 * @param alias The alias of the command.
	 * @param args The arguments of the command.
	 * @return Confirmation of whether the command was a success or not.
	 */
	@Override
	public boolean onCommand(CommandSender sender, Command command, String alias, String[] args) {
		// Display all active boosts
		if (args.length == 0) {
			displayBoosts(sender);
		}
		else {
			if (sender instanceof Player player) {
				if (!player.hasPermission("aranarth.boosts.modify")) {
					displayBoosts(sender);
					return true;
				}
			}

			if (args.length <= 1) {
				sender.sendMessage(ChatUtils.chatMessage("&cInvalid syntax: &e/boosts <add/remove> <type> [user]"));
				return true;
			} else {
				if (args[0].equals("add") || args[0].equals("remove")) {
					Boost applied = null;
					for (Boost boost : Boost.values()) {
						if (boost.name().equalsIgnoreCase(args[1])) {
							applied = boost;
						}
					}

					if (applied != null) {
						// Adding a boost
						if (args[0].equals("add")) {
							// If a user was entered, verify that the user exists
							UUID uuid = null;
							if (args.length >= 3) {
								uuid = AranarthUtils.getUUIDFromUsername(args[2]);
								if (uuid == null) {
									sender.sendMessage(ChatUtils.chatMessage("&cThis player could not be found"));
									return true;
								}
							}
							AranarthUtils.addServerBoost(applied, null, uuid);
							return true;
						}
						// Removing a boost
						else {
							AranarthUtils.removeServerBoost(applied);
							return true;
						}
					} else {
						sender.sendMessage(ChatUtils.chatMessage("&cThe entered boost does not exist!"));
						return true;
					}
				} else {
					sender.sendMessage(ChatUtils.chatMessage("&cInvalid syntax: &e/boosts <add/remove> <type> <user>"));
					return true;
				}
			}
		}
		return false;
	}

	private void displayBoosts(CommandSender sender) {
		HashMap<Boost, LocalDateTime> boosts = AranarthUtils.getServerBoosts();
		if (boosts.isEmpty()) {
			sender.sendMessage(ChatUtils.chatMessage("&7There are currently no active server boosts"));
		} else {
			sender.sendMessage(ChatUtils.translateToColor("&8      - - - &6&lActive Server Boosts &8- - -"));
			HashMap<String, String> activeBoosts = AranarthUtils.getActiveServerBoostsMessages();
			for (String boost : activeBoosts.keySet()) {
				sender.sendMessage(ChatUtils.translateToColor(boost + " &7| " + activeBoosts.get(boost)));
			}
		}
	}
}
