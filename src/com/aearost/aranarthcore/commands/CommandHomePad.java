package com.aearost.aranarthcore.commands;

import java.util.Objects;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import com.aearost.aranarthcore.utils.AranarthUtils;
import com.aearost.aranarthcore.utils.ChatUtils;
import com.aearost.aranarthcore.utils.ItemUtils;

public class CommandHomePad implements CommandExecutor {

	@Override
	public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {

		if (sender instanceof Player) {
			Player player = (Player) sender;

			if (args.length == 0) {
				player.sendMessage(ChatUtils.chatMessageError("You must enter parameters!"));
				return false;
			} else {
				if (args[0].equals("give")) {
					if (args.length > 1) {
						Player playerInArg = null;
						for (Player onlinePlayer : Bukkit.getOnlinePlayers()) {
							if (onlinePlayer.getName().toLowerCase().equals(args[1].toLowerCase())) {
								playerInArg = onlinePlayer;
							}
						}
						if (playerInArg != null) {
							ItemStack homepadIS = ItemUtils.getItem("homepad");
							ItemUtils.giveItem(homepadIS, playerInArg, sender);
						} else {
							player.sendMessage(ChatUtils.chatMessageError(args[1] + " is not a valid player name!"));
						}
					} else {
						player.sendMessage(ChatUtils.chatMessageError("You must enter a player name!"));
					}
				} else if (args[0].equals("create")) {
					// Must be on a valid homepad
					if (Objects.nonNull(AranarthUtils.getHomePad(player.getLocation()))) {
						if (AranarthUtils.getHomePad(player.getLocation()).getHomeName().equals("Unnamed")) {
							String homeName = "";
							// Get everything after the create parameter and space-separated
							for (int i = 1; i < args.length; i++) {
								if (i == args.length - 1) {
									homeName += args[i];
								} else {
									homeName += args[i] + " ";
								}
							}
							// Ensures the name is alpha-numeric
							if (homeName.matches("[a-zA-Z0-9& ]+")) {
								Location locationDirection = player.getLocation();
								locationDirection.setX(locationDirection.getBlockX() + 0.5);
								locationDirection.setZ(locationDirection.getBlockZ() + 0.5);
								AranarthUtils.setHomeName(homeName, AranarthUtils.getHomePad(player.getLocation()), locationDirection);
								player.sendMessage(ChatUtils.chatMessage("&7Home &e" + homeName + " &7has been created"));
								return true;
							} else {
								player.sendMessage(ChatUtils.chatMessageError("You must use alphanumeric characters!"));
								return false;
							}
						} else {
							player.sendMessage(ChatUtils.chatMessageError("You cannot rename a homepad!"));
							return false;
						}
					} else {
						player.sendMessage(ChatUtils.chatMessageError("You must be standing on a Home Pad to use this command!"));
						return false;
					}
				} else {
					player.sendMessage(ChatUtils.chatMessageError("That is not a valid parameter!"));
					return false;
				}
			}

		} else {
			sender.sendMessage(ChatUtils.chatMessageError("This must be executed in-game!"));
		}

		return true;
	}

}
