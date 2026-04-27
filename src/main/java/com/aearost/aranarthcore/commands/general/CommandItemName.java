package com.aearost.aranarthcore.commands.general;

import com.aearost.aranarthcore.utils.ChatUtils;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.Objects;

/**
 * Allows specified players to rename any item to any name, supporting all colors.
 */
public class CommandItemName implements CommandExecutor {

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
            if (!player.hasPermission("aranarth.itemname")) {
                player.sendMessage(ChatUtils.chatMessage("&cYou do not have permission to use this command!"));
                return true;
            }

            if (args.length == 0) {
                sender.sendMessage(ChatUtils.chatMessage("&cYou must enter an item name!"));
                return true;
            } else {
                ItemStack item = player.getInventory().getItemInMainHand();
                if (item.getType() == Material.AIR) {
                    player.sendMessage(ChatUtils.chatMessage("&cYou must be holding an item to use this command!"));
                } else {
                    ItemMeta meta = item.getItemMeta();
                    if (args.length >= 1 && args[0].equalsIgnoreCase("remove")) {
                        meta.setDisplayName(null);
                        player.sendMessage(ChatUtils.chatMessage("&7You have removed the name from this item"));
                        item.setItemMeta(meta);
                        player.getInventory().setItemInMainHand(item);
                        return true;
                    } else {
                        int stringStart = 0;
                        if (args[0].startsWith("gradient")) {
                            if (!player.hasPermission("aranarth.itemname.gradient")) {
                                player.sendMessage(ChatUtils.chatMessage("&cYou do not have permission to use this command!"));
                                return true;
                            }

                            // Start at the actual name and not the attribute
                            stringStart = 2;
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
                        if (args[0].startsWith("gradient")) {
                            if (args.length < 3) {
                                player.sendMessage(ChatUtils.chatMessage("&cYou must specify the colors and the text!"));
                                return true;
                            }

                            if (args[0].equalsIgnoreCase("gradient")) {
                                itemName = ChatUtils.translateToGradient(args[1], itemName, false);
                            } else if (args[0].equalsIgnoreCase("gradientbold")) {
                                itemName = ChatUtils.translateToGradient(args[1], itemName, true);
                            }

                            if (Objects.isNull(itemName)) {
                                player.sendMessage(ChatUtils.chatMessage("&cYour item could not be renamed as a gradient"));
                                return false;
                            }
                            else {
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
        } else {
            sender.sendMessage(ChatUtils.chatMessage("&cThis must be executed in-game!"));
            return true;
        }
        return false;
    }
}
