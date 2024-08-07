package com.aearost.aranarthcore.commands;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import com.aearost.aranarthcore.items.HomePad;
import com.aearost.aranarthcore.objects.Home;
import com.aearost.aranarthcore.utils.AranarthUtils;
import com.aearost.aranarthcore.utils.ChatUtils;
import com.aearost.aranarthcore.utils.ItemUtils;

/**
 * Allows the player to creative and use a homepad.
 */
public class CommandHomePad {

	/**
	 * @param sender The user that entered the command.
	 * @param args The arguments of the command.
	 * @return Confirmation of whether the command was a success or not.
	 */
	public static boolean onCommand(CommandSender sender, String[] args) {
		if (sender instanceof Player player) {
            if (args.length == 1) {
				player.sendMessage(ChatUtils.chatMessageError("You must enter parameters!"));
				return false;
			} else {
                switch (args[1]) {
                    case "give" -> {
                        if (args.length > 2) {
                            Player playerInArg = null;
                            for (Player onlinePlayer : Bukkit.getOnlinePlayers()) {
                                if (onlinePlayer.getName().equalsIgnoreCase(args[2])) {
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
                    }
                    case "create" -> {
                        // Must be on a valid homepad
                        if (Objects.nonNull(AranarthUtils.getHomePad(player.getLocation()))) {
                            if (Objects.requireNonNull(AranarthUtils.getHomePad(player.getLocation())).getHomeName().equals("NEW")) {
                                StringBuilder homeName = new StringBuilder();
                                // Get everything after the create parameter and space-separated
                                for (int i = 2; i < args.length; i++) {
                                    if (i == args.length - 1) {
                                        homeName.append(args[i]);
                                    } else {
                                        homeName.append(args[i]).append(" ");
                                    }
                                }
                                if (homeName.toString().matches("^[^'\"\n\r\t]+$")) {
                                    Location locationDirection = player.getLocation();
                                    locationDirection.setX(locationDirection.getBlockX() + 0.5);
                                    locationDirection.setZ(locationDirection.getBlockZ() + 0.5);
                                    AranarthUtils.updateHome(homeName.toString(), locationDirection,
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
                    }
                    case "reorder" -> {
                        if (args.length >= 4) {
                            try {
                                final int homeNumber = Integer.parseInt(args[2]);
                                final int newNumber = Integer.parseInt(args[3]);
                                if (newNumber == homeNumber) {
                                    sender.sendMessage(ChatUtils.chatMessageError("Please enter a different number to reorder!"));
                                    return false;
                                }

                                List<Home> homes = AranarthUtils.getHomes();
                                ArrayList<Home> newHomes = new ArrayList<Home>();
                                if (Objects.isNull(homes) || homes.isEmpty()) {
                                    sender.sendMessage(ChatUtils.chatMessageError("There are no homes!"));
                                    return false;
                                }

                                for (int i = 0; i < homes.size(); i++) {
                                    if (i == homeNumber) {
                                        continue;
                                    }
                                    if (i == newNumber && homeNumber < newNumber) {
                                        newHomes.add(homes.get(i));
                                        newHomes.add(homes.get(homeNumber));
                                        continue;
                                    }
                                    if (i == newNumber) {
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
                    }
                    default -> {
                        player.sendMessage(ChatUtils.chatMessageError("That is not a valid parameter!"));
                        return false;
                    }
                }
			}

		} else {
			sender.sendMessage(ChatUtils.chatMessageError("This must be executed in-game!"));
		}

		return true;
	}

}