package com.aearost.aranarthcore.commands;

import com.aearost.aranarthcore.utils.ChatUtils;
import org.bukkit.Material;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

/**
 * Allows a player to toggle the shulker-filling functionality
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
                }

                if (item.hasItemMeta()) {
                    if (args.length == 2) {
                        if (args[1].equalsIgnoreCase("remove")) {
                            ItemMeta meta = item.getItemMeta();
                            meta.setDisplayName(null);
                            item.setItemMeta(meta);
                            player.getInventory().setItemInMainHand(item);
                            player.sendMessage(ChatUtils.chatMessage("&7You have removed the name from this item"));
                            return true;
                        }
                    } if (args.length > 2) {
                        int stringStart = 1;
                        if (args[1].equalsIgnoreCase("gradient")) {
                            stringStart++;
                        }

                        // Gets the full item name
                        StringBuilder itemName = new StringBuilder();
                        for (int i = 1; i < args.length; i++) {
                            itemName.append(args[i]);
                            if (i == args.length - 1) {
                                break;
                            } else {
                                itemName.append(" ");
                            }
                        }

                        ItemMeta meta = item.getItemMeta();
                        meta.setDisplayName(ChatUtils.translateToColor(itemName.toString()));
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
