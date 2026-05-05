package com.aearost.aranarthcore.commands.council;

import com.aearost.aranarthcore.objects.AranarthPlayer;
import com.aearost.aranarthcore.utils.AranarthUtils;
import com.aearost.aranarthcore.utils.ChatUtils;
import com.aearost.aranarthcore.utils.ItemUtils;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.io.IOException;

/**
 * Allows admins to forcefully swap a player's active inventory with one of their stored inventory types.
 */
public class CommandInvSwap {

    /**
     * @param sender The user that entered the command.
     * @param args The arguments of the command.
     */
    public static boolean onCommand(CommandSender sender, String[] args) {
        if (sender instanceof Player player) {
            AranarthPlayer aranarthPlayer = AranarthUtils.getPlayer(player.getUniqueId());
            if (aranarthPlayer.getCouncilRank() != 3) {
                player.sendMessage(ChatUtils.chatMessage("&cYou do not have permission to execute this command!"));
                return true;
            }

            if (args.length < 2) {
                player.sendMessage(ChatUtils.chatMessage("&cInvalid syntax: &e/ac invswap <SURVIVAL|ARENA|CREATIVE> [player]"));
                return true;
            }

            String typeArg = args[1].toUpperCase();
            if (!typeArg.equals("SURVIVAL") && !typeArg.equals("ARENA") && !typeArg.equals("CREATIVE")) {
                player.sendMessage(ChatUtils.chatMessage("&cInvalid type. Valid types: &eSURVIVAL&c, &eARENA&c, or &eCREATIVE"));
                return true;
            }

            Player target;
            AranarthPlayer targetAranarthPlayer;
            if (args.length >= 3) {
                target = Bukkit.getPlayer(args[2]);
                if (target == null) {
                    player.sendMessage(ChatUtils.chatMessage("&cThis player could not be found."));
                    return true;
                }
                targetAranarthPlayer = AranarthUtils.getPlayer(target.getUniqueId());
            } else {
                target = player;
                targetAranarthPlayer = aranarthPlayer;
            }

            // Determine the current inventory type from the target's world
            String worldName = target.getWorld().getName();
            String currentType;
            if (worldName.startsWith("world") || worldName.startsWith("smp") || worldName.startsWith("resource") || worldName.startsWith("spawn")) {
                currentType = "SURVIVAL";
            } else if (worldName.startsWith("arena")) {
                currentType = "ARENA";
            } else if (worldName.equals("creative")) {
                currentType = "CREATIVE";
            } else {
                player.sendMessage(ChatUtils.chatMessage("&cUnable to determine the inventory type for " + (target.equals(player) ? "your" : target.getName() + "'s") + " world."));
                return true;
            }

            if (currentType.equals(typeArg)) {
                String descriptor = target.equals(player) ? "Your" : target.getName() + "'s";
                player.sendMessage(ChatUtils.chatMessage("&c" + descriptor + " inventory type would not change."));
                return true;
            }

            try {
                // Read the stored inventory for the requested type before overwriting
                String storedInventory = switch (typeArg) {
                    case "SURVIVAL" -> targetAranarthPlayer.getSurvivalInventory();
                    case "ARENA" -> targetAranarthPlayer.getArenaInventory();
                    default -> targetAranarthPlayer.getCreativeInventory();
                };

                // Save the current active inventory into the requested type's slot
                String currentInventoryBase64 = ItemUtils.toBase64(target.getInventory());
                switch (typeArg) {
                    case "SURVIVAL" -> targetAranarthPlayer.setSurvivalInventory(currentInventoryBase64);
                    case "ARENA" -> targetAranarthPlayer.setArenaInventory(currentInventoryBase64);
                    default -> targetAranarthPlayer.setCreativeInventory(currentInventoryBase64);
                }

                // Load the stored inventory into the active inventory
                if (!storedInventory.isEmpty()) {
                    target.getInventory().setContents(ItemUtils.itemStackArrayFromBase64(storedInventory));
                } else {
                    target.getInventory().clear();
                }

                AranarthUtils.setPlayer(target.getUniqueId(), targetAranarthPlayer);

                if (target.equals(player)) {
                    player.sendMessage(ChatUtils.chatMessage("&aYour inventory has been swapped to " + typeArg + "."));
                } else {
                    player.sendMessage(ChatUtils.chatMessage("&a" + target.getName() + "'s inventory has been swapped to " + typeArg + "."));
                    target.sendMessage(ChatUtils.chatMessage("&aYour inventory has been swapped to " + typeArg + " by a council member."));
                }
            } catch (IOException e) {
                player.sendMessage(ChatUtils.chatMessage("&cAn error occurred while swapping inventories"));
            }

            return true;
        } else {
            sender.sendMessage(ChatUtils.chatMessage("&cYou must be a player to execute this command!"));
            return true;
        }
    }
}
