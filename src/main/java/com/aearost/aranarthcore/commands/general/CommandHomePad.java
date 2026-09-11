package com.aearost.aranarthcore.commands.general;

import com.aearost.aranarthcore.objects.Home;
import com.aearost.aranarthcore.utils.AranarthUtils;
import com.aearost.aranarthcore.utils.ChatUtils;
import com.aearost.aranarthcore.utils.Lang;
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
				player.sendMessage(ChatUtils.chatMessage(Lang.get("general.invalid_syntax", "usage", "homepad <create|delete|reorder>")));
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
                                            ChatUtils.chatMessage(Lang.get("homepad.created", "name", homeName)));
                                    return true;
                                } else {
                                    player.sendMessage(ChatUtils.chatMessage(Lang.get("home.cannot_use_quote")));
                                }
                            } else {
                                player.sendMessage(ChatUtils.chatMessage(Lang.get("home.cannot_rename")));
                            }
                        } else {
                            player.sendMessage(
                                    ChatUtils.chatMessage(Lang.get("home.must_stand_on_pad")));
                        }
                    }
                    case "delete" -> {
                        if (args.length >= 2) {
                            try {
                                final int homeIndex = Integer.parseInt(args[1]);
                                List<Home> homes = AranarthUtils.getHomepads();
                                if (Objects.isNull(homes) || homes.isEmpty()) {
                                    player.sendMessage(ChatUtils.chatMessage(Lang.get("home.no_homes")));
                                    return false;
                                }
                                if (homeIndex < 0 || homeIndex >= homes.size()) {
                                    player.sendMessage(ChatUtils.chatMessage(Lang.get("home.index_not_found")));
                                    return false;
                                }
                                String homeName = homes.get(homeIndex).getName();
                                homes.remove(homeIndex);
                                player.sendMessage(ChatUtils.chatMessage(Lang.get("homepad.deleted", "name", homeName)));
                                return true;
                            } catch (NumberFormatException e) {
                                player.sendMessage(ChatUtils.chatMessage(Lang.get("general.invalid_number")));
                            }
                        } else {
                            player.sendMessage(ChatUtils.chatMessage(Lang.get("homepad.enter_index")));
                        }
                    }
                    case "reorder" -> {
                        if (args.length >= 3) {
                            try {
                                final int homeNumber = Integer.parseInt(args[1]);
                                final int newNumber = Integer.parseInt(args[2]);
                                if (newNumber == homeNumber) {
                                    sender.sendMessage(ChatUtils.chatMessage(Lang.get("homepad.reorder_same")));
                                    return false;
                                }

                                List<Home> homes = AranarthUtils.getHomepads();
                                ArrayList<Home> newHomes = new ArrayList<>();
                                if (Objects.isNull(homes) || homes.isEmpty()) {
                                    sender.sendMessage(ChatUtils.chatMessage(Lang.get("home.no_homes")));
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
                                        Lang.get("homepad.reordered", "name", homes.get(homeNumber).getName())));
                                return true;
                            } catch (NumberFormatException e) {
                                sender.sendMessage(ChatUtils.chatMessage(Lang.get("home.updated")));
                            }
                        }
                    }
                    default -> {
                        player.sendMessage(ChatUtils.chatMessage(Lang.get("general.invalid_toggle")));
                    }
                }
			}
		} else {
			sender.sendMessage(ChatUtils.chatMessage(Lang.get("general.no_permission_console")));
		}
		return false;
	}

}