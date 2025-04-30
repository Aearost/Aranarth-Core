package com.aearost.aranarthcore.commands;

import com.aearost.aranarthcore.utils.ChatUtils;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.Objects;

/**
 * Allows specified players to rename any item to any name, supporting all colors.
 */
public class CommandItemName {

    /**
     * @param sender The user that entered the command.
     * @param args   The arguments of the command.
     * @return Confirmation of whether the command was a success or not.
     */
    public static boolean onCommand(CommandSender sender, String[] args) {
        if (sender instanceof Player player) {
            if (!player.getName().equalsIgnoreCase("Aearost")) {
                player.sendMessage(ChatUtils.chatMessage("&cYou do not have permission to use this command!"));
                return false;
            }

            if (args.length == 1) {
                sender.sendMessage(ChatUtils.chatMessage("&cYou must enter an item name!"));
            } else {
                ItemStack item = player.getInventory().getItemInMainHand();
                if (item.getType() == Material.AIR) {
                    player.sendMessage(ChatUtils.chatMessage("&cYou must be holding an item to use this command!"));
                } else {
                    ItemMeta meta = item.getItemMeta();
                    if (args.length >= 2 && args[1].equalsIgnoreCase("remove")) {
                        meta.setDisplayName(null);
                        player.sendMessage(ChatUtils.chatMessage("&7You have removed the name from this item"));
                        item.setItemMeta(meta);
                        player.getInventory().setItemInMainHand(item);
                        return true;
                    } else {
                        int stringStart = 1;
                        if (args[1].startsWith("gradient")) {
                            // Start at the actual name and not the attribute
                            stringStart = 3;
                        }

                        // Gets the full item name
                        StringBuilder itemNameSB = new StringBuilder();
                        for (int i = stringStart; i < args.length; i++) {
                            itemNameSB.append(args[i]);
                            if (i == args.length - 1) {
                                break;
                            } else {
                                itemNameSB.append(" ");
                            }
                        }

                        String itemName = itemNameSB.toString();
                        Bukkit.getLogger().info("Full name: |" + itemName + "|");
                        if (args[1].startsWith("gradient")) {
                            if (args[1].equalsIgnoreCase("gradient")) {
                                itemName = ChatUtils.translateToGradient(args[2], itemName, false);
                            } else if (args[1].equalsIgnoreCase("gradientbold")) {
                                itemName = ChatUtils.translateToGradient(args[2], itemName, true);
                            }

                            if (Objects.isNull(itemName)) {
                                player.sendMessage(ChatUtils.chatMessage("&cYour item could not be renamed as a gradient"));
                                return false;
                            } else {
                                meta.setDisplayName(itemName);
                            }
                        } else {
                            itemName = ChatUtils.translateToColor(itemName);
                            meta.setDisplayName(itemName);
                        }

                        item.setItemMeta(meta);
                        player.getInventory().setItemInMainHand(item);
                        player.sendMessage(ChatUtils.chatMessage("&7You have named this item " + itemName));
                        return true;
                    }
                }
            }
        }
        return false;
    }
}
