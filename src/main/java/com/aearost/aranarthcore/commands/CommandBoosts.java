package com.aearost.aranarthcore.commands;

import com.aearost.aranarthcore.objects.Boost;
import com.aearost.aranarthcore.utils.AranarthUtils;
import com.aearost.aranarthcore.utils.ChatUtils;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.UUID;

/**
 * Allows plays to view all active server boosts.
 * Additionally allows for council members to add or remove boosts.
 */
public class CommandBoosts {

	/**
	 * @param sender The user that entered the command.
	 * @param args The arguments of the command.
	 * @return Confirmation of whether the command was a success or not.
	 */
	public static boolean onCommand(CommandSender sender, String[] args) {
		// Display all active boosts
		if (args.length == 1) {
			HashMap<Boost, LocalDateTime> boosts = AranarthUtils.getServerBoosts();
			if (boosts.isEmpty()) {
				sender.sendMessage(ChatUtils.chatMessage("&7There are currently no active server boosts"));
				return true;
			} else {
				sender.sendMessage(ChatUtils.translateToColor("&8      - - - &6&lActive Server Boosts &8- - -"));
				HashMap<String, String> activeBoosts = AranarthUtils.getActiveServerBoostsMessages();
				for (String boost : activeBoosts.keySet()) {
					sender.sendMessage(ChatUtils.translateToColor(boost + " &7| " + activeBoosts.get(boost)));
				}
				return true;
			}
		}
		else {
			if (sender instanceof Player player) {
				if (!player.hasPermission("aranarth.boosts.modify")) {
					player.sendMessage(ChatUtils.chatMessage("&cInvalid syntax: &e/ac boosts"));
					return true;
				}
			}

			if (args.length <= 2) {
				sender.sendMessage(ChatUtils.chatMessage("&cInvalid syntax: &e/ac boosts <add/remove> <type> [user]"));
				return true;
			} else {
				if (args[1].equals("add") || args[1].equals("remove")) {
					Boost applied = null;
					for (Boost boost : Boost.values()) {
						if (boost.name().equalsIgnoreCase(args[2])) {
							applied = boost;
						}
					}

					if (applied != null) {
						// Adding a boost
						if (args[1].equals("add")) {
							// If a user was entered, verify that the user exists
							UUID uuid = null;
							if (args.length >= 4) {
								uuid = AranarthUtils.getUUIDFromUsername(args[3]);
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
					sender.sendMessage(ChatUtils.chatMessage("&cInvalid syntax: &e/ac boosts <add/remove> <type> <user>"));
					return true;
				}
			}
		}
	}
}
