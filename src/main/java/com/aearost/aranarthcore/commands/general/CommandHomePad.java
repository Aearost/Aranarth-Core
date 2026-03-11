package com.aearost.aranarthcore.commands.general;

import com.aearost.aranarthcore.objects.Home;
import com.aearost.aranarthcore.utils.AranarthUtils;
import com.aearost.aranarthcore.utils.ChatUtils;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Allows the player to creative and use a homepad.
 */
public class CommandHomePad implements CommandExecutor {

    /**
     * @param sender The user that entered the command.
     * @param command The command itself.
     * @param alias The alias of the command.
     * @param args The arguments of the command.
     * @return Confirmation of whether the command was a success or not.
     */
    @Override
    public boolean onCommand(CommandSender sender, Command command, String alias, String[] args) {
		if (sender instanceof Player player) {
            if (args.length == 0) {
				player.sendMessage(ChatUtils.chatMessage("&cYou must enter parameters!"));
				return true;
			} else {
                switch (args[0]) {
                    case "create" -> {
                        // Must be on a valid homepad
                        if (Objects.nonNull(AranarthUtils.getHomepad(player.getLocation()))) {
                            if (AranarthUtils.getHomepad(player.getLocation()).getName().equals("NEW")) {
                                StringBuilder homeName = new StringBuilder();
                                // Get everything after the create parameter and space-separated
                                for (int i = 1; i < args.length; i++) {
                                    if (i == args.length - 1) {
                                        homeName.append(args[i]);
                                    } else {
                                        homeName.append(args[i]).append(" ");
                                    }
                                }
                                if (homeName.toString().matches("^[^\"\n\r\t]+$")) {
                                    Location locationDirection = player.getLocation();
                                    locationDirection.setX(locationDirection.getBlockX() + 0.5);
                                    locationDirection.setZ(locationDirection.getBlockZ() + 0.5);
                                    AranarthUtils.updateHomepad(homeName.toString(), locationDirection,
                                            Material.HEAVY_WEIGHTED_PRESSURE_PLATE);
                                    player.sendMessage(
                                            ChatUtils.chatMessage("&7Home &e" + homeName + " &7has been created"));
                                    return true;
                                } else {
                                    player.sendMessage(ChatUtils.chatMessage("&cYou cannot use the \" character!"));
                                }
                            } else {
                                player.sendMessage(ChatUtils.chatMessage("&cYou cannot rename a homepad!"));
                            }
                        } else {
                            player.sendMessage(
                                    ChatUtils.chatMessage("&cYou must be standing on a Home Pad to use this command!"));
                        }
                    }
                    case "reorder" -> {
                        if (args.length >= 3) {
                            try {
                                final int homeNumber = Integer.parseInt(args[2]);
                                final int newNumber = Integer.parseInt(args[3]);
                                if (newNumber == homeNumber) {
                                    sender.sendMessage(ChatUtils.chatMessage("&cPlease enter a different number to reorder!"));
                                    return false;
                                }

                                List<Home> homes = AranarthUtils.getHomepads();
                                ArrayList<Home> newHomes = new ArrayList<>();
                                if (Objects.isNull(homes) || homes.isEmpty()) {
                                    sender.sendMessage(ChatUtils.chatMessage("&cThere are no homes!"));
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
                                AranarthUtils.setHomepads(newHomes);
                                sender.sendMessage(ChatUtils.chatMessage(
                                        "&7You have updated the slot number of " + homes.get(homeNumber).getName()));
                                return true;
                            } catch (NumberFormatException e) {
                                sender.sendMessage(ChatUtils.chatMessage("&cA home could not be updated!"));
                            }
                        }
                    }
                    default -> {
                        player.sendMessage(ChatUtils.chatMessage("&cThat is not a valid parameter!"));
                    }
                }
			}
		} else {
			sender.sendMessage(ChatUtils.chatMessage("&cThis must be executed in-game!"));
		}
		return false;
	}

}