package com.aearost.aranarthcore.commands;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import com.aearost.aranarthcore.items.HomePad;
import com.aearost.aranarthcore.objects.Home;
import com.aearost.aranarthcore.utils.AranarthUtils;
import com.aearost.aranarthcore.utils.ChatUtils;
import com.aearost.aranarthcore.utils.ItemUtils;

public class CommandHomePad {

	public static boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {

		if (sender instanceof Player) {
			Player player = (Player) sender;

			if (args.length == 1) {
				player.sendMessage(ChatUtils.chatMessageError("You must enter parameters!"));
				return false;
			} else {
				if (args[1].equals("give")) {
					if (args.length > 2) {
						Player playerInArg = null;
						for (Player onlinePlayer : Bukkit.getOnlinePlayers()) {
							if (onlinePlayer.getName().toLowerCase().equals(args[2].toLowerCase())) {
								playerInArg = onlinePlayer;
							}
						}
						if (playerInArg != null) {
							ItemStack homepadItem = HomePad.getHomePad();
							ItemUtils.giveItem(homepadItem, playerInArg, sender);
						} else {
							player.sendMessage(ChatUtils.chatMessageError(args[1] + " is not a valid player name!"));
						}
					} else {
						player.sendMessage(ChatUtils.chatMessageError("You must enter a player name!"));
					}
				} else if (args[1].equals("create")) {
					// Must be on a valid homepad
					if (Objects.nonNull(AranarthUtils.getHomePad(player.getLocation()))) {
						if (AranarthUtils.getHomePad(player.getLocation()).getHomeName().equals("NEW")) {
							String homeName = "";
							// Get everything after the create parameter and space-separated
							for (int i = 2; i < args.length; i++) {
								if (i == args.length - 1) {
									homeName += args[i];
								} else {
									homeName += args[i] + " ";
								}
							}
							// Ensures the name is alpha-numeric
//							if (homeName.matches("[a-zA-Z0-9& -]+")) {
							if (homeName.matches("^[^'\"\n\r\t]+$")) {
								Location locationDirection = player.getLocation();
								locationDirection.setX(locationDirection.getBlockX() + 0.5);
								locationDirection.setZ(locationDirection.getBlockZ() + 0.5);
								AranarthUtils.updateHome(homeName, locationDirection,
										Material.HEAVY_WEIGHTED_PRESSURE_PLATE);
								player.sendMessage(
										ChatUtils.chatMessage("&7Home &e" + homeName + " &7has been created"));
								return true;
							} else {
								player.sendMessage(ChatUtils.chatMessageError("You cannot use the \" character!"));
								return false;
							}
						} else {
							player.sendMessage(ChatUtils.chatMessageError("You cannot rename a homepad!"));
							return false;
						}
					} else {
						player.sendMessage(
								ChatUtils.chatMessageError("You must be standing on a Home Pad to use this command!"));
						return false;
					}
				} else if (args[1].equals("reorder")) {
					if (args.length >= 4) {
						try {
							final int homeNumber = Integer.parseInt(args[2]);
							final int newNumber = Integer.parseInt(args[3]);
							if (newNumber == homeNumber){
								sender.sendMessage( ChatUtils.chatMessageError("Please enter a different number to reorder!"));
								return false;
							}
							
							List<Home> homes = AranarthUtils.getHomes();
							ArrayList<Home> newHomes = new ArrayList<Home>();
							if (Objects.isNull(homes) || homes.size() == 0) {
								sender.sendMessage(ChatUtils.chatMessageError("There are no homes!"));
								return false;
							}
							
							// 0 2
							// 2 0

//						0 1 2 3 4
//						1 2 0 3 4
//						0 1 2 3 4	
							for (int i = 0; i < homes.size(); i++) {
								if (i == homeNumber) {
									continue;
								}

								if (i == newNumber && homeNumber < newNumber) {
									newHomes.add(homes.get(i));
									newHomes.add(homes.get(homeNumber));
									continue;
								}
								if (i == newNumber && homeNumber > newNumber) {
									newHomes.add(homes.get(homeNumber));
									newHomes.add(homes.get(i));
									continue;
								}
								newHomes.add(homes.get(i));
							}
							AranarthUtils.setHomes(newHomes);
							sender.sendMessage(ChatUtils.chatMessage(
									"&7You have updated the slot number of " + homes.get(homeNumber).getHomeName()));
						} catch (NumberFormatException e) {
							sender.sendMessage(ChatUtils.chatMessageError("A home could not be updated!"));
							return false;
						}
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