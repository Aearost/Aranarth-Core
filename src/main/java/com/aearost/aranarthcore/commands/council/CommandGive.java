package com.aearost.aranarthcore.commands.council;

import com.aearost.aranarthcore.items.AranarthItem;
import com.aearost.aranarthcore.items.incantation.Incantation;
import com.aearost.aranarthcore.network.NetworkManager;
import com.aearost.aranarthcore.utils.AranarthUtils;
import com.aearost.aranarthcore.utils.ChatUtils;
import com.aearost.aranarthcore.utils.DiscordUtils;
import com.aearost.aranarthcore.utils.Lang;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.awt.*;
import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;

/**
 * Provides a specified player an AranarthCore item.
 * Dynamically fetches the associated Item object based on the input.
 */
public class CommandGive {

    /**
     * @param sender The user that entered the command.
     * @param args   The arguments of the command.
     */
    public static void onCommand(CommandSender sender, String[] args) {
        if (sender instanceof Player player) {
            if (!player.hasPermission("aranarth.give")) {
                player.sendMessage(ChatUtils.chatMessage(Lang.get("general.no_permission")));
                return;
            }
        }

        if (args.length < 3) {
            sender.sendMessage(ChatUtils.chatMessage(Lang.get("general.invalid_syntax", "usage", "ac give <player> <item> <quantity>")));
            return;
        } else {
            Player player = null;
            for (Player onlinePlayer : Bukkit.getOnlinePlayers()) {
                if (onlinePlayer.getName().equals(args[1])) {
                    player = onlinePlayer;
                    break;
                }
            }

            if (player != null) {
                boolean isKey = false;
                String fullPathName = "";
                if (args[2].startsWith("Aranarthium")) {
                    fullPathName = "com.aearost.aranarthcore.items.aranarthium.ingots." + args[2];
                } else if (args[2].endsWith("Cluster")) {
                    fullPathName = "com.aearost.aranarthcore.items.aranarthium.clusters." + args[2];
                } else if (args[2].startsWith("Netherite")) {
                    fullPathName = "com.aearost.aranarthcore.items.netherite." + args[2];
                } else if (args[2].endsWith("Helmet") || args[2].endsWith("Chestplate")
                        || args[2].endsWith("Leggings") || args[2].endsWith("Boots")
                        || args[2].endsWith("Elytra")) {
                    fullPathName = "com.aearost.aranarthcore.items.aranarthium.armour." + args[2];
                } else if (args[2].startsWith("Arrow")) {
                    if (args[2].startsWith("Arrowhead")) {
                        fullPathName = "com.aearost.aranarthcore.items.arrowhead." + args[2];
                    } else {
                        fullPathName = "com.aearost.aranarthcore.items.arrow." + args[2];
                    }
                } else if (args[2].startsWith("Key")) {
                    isKey = true;
                    fullPathName = "com.aearost.aranarthcore.items.key." + args[2];
                } else if (args[2].startsWith("Incantation")) {
                    fullPathName = "com.aearost.aranarthcore.items.incantation." + args[2];
                } else {
                    fullPathName = "com.aearost.aranarthcore.items." + args[2];
                }

                Object instance = null;
                try {
                    Class<?> unknownClass = Class.forName(fullPathName);
                    instance = unknownClass.getDeclaredConstructor().newInstance();
                } catch (ClassNotFoundException | InvocationTargetException | InstantiationException
                         | IllegalAccessException | NoSuchMethodException e) {
                    sender.sendMessage(ChatUtils.chatMessage(Lang.get("give.no_such_item")));
                    return;
                }

                if (instance instanceof AranarthItem aranarthItem) {
                    ItemStack item = aranarthItem.getItem();
                    int quantity = 1;
                    if (args.length >= 4) {
                        try {
                            quantity = Integer.parseInt(args[3]);
                            if (quantity <= 0 || quantity > item.getMaxStackSize()) {
                                throw new NumberFormatException();
                            }
                            item.setAmount(quantity);
                        } catch (Exception e) {
                            player.sendMessage(ChatUtils.chatMessage(Lang.get("give.invalid_quantity")));
                            return;
                        }
                    }

                    ItemMeta meta = item.getItemMeta();
                    String itemName = ChatUtils.getFormattedItemName(item.getType().name());
                    if (meta != null && meta.hasDisplayName()) {
                        itemName = meta.getDisplayName();
                    }

                    boolean willBroadcast = isKey && !args[2].equals("KeyVote");
                    boolean isBroadcast = willBroadcast && args.length >= 5 && args[4].equalsIgnoreCase("broadcast");

                    // For crate keys in non-survival worlds, store as pending instead of giving directly
                    if (isKey) {
                        if (isBroadcast) {
                            String broadcastMessage = ChatUtils.chatMessage(Lang.get("give.purchased_broadcast", "player", player.getName(), "item", itemName, "amount", String.valueOf(quantity)));
                            DiscordUtils.donationNotification(player.getName() + " has purchased " + itemName + " x" + quantity, player.getUniqueId(), Color.CYAN);
                            for (Player online : Bukkit.getOnlinePlayers()) {
                                online.sendMessage(broadcastMessage);
                            }
                            Bukkit.getConsoleSender().sendMessage(broadcastMessage);
                            if (NetworkManager.isActive()) {
                                NetworkManager.getInstance().publishBroadcast(broadcastMessage);
                            }
                        }

                        String worldName = player.getWorld().getName();
                        if (!AranarthUtils.isSurvivalWorld(worldName)) {
                            AranarthUtils.addPendingKey(player.getUniqueId(), item, quantity);
                            player.sendMessage(ChatUtils.chatMessage(Lang.get("give.keyclaim_hint")));
                            return;
                        }
                    }

                    // Adds the item to inventory or drops to their feet
                    HashMap<Integer, ItemStack> remainder = player.getInventory().addItem(item);
                    boolean isInventoryFull = !remainder.isEmpty();
                    if (!remainder.isEmpty()) {
                        for (ItemStack value : remainder.values()) {
                            player.getWorld().dropItemNaturally(player.getLocation(), value);
                        }
                    }

                    if (!isKey || !isBroadcast) {
                        if (isInventoryFull) {
                            player.sendMessage(ChatUtils.chatMessage(Lang.get("give.dropped", "item", itemName)));
                        } else {
                            player.sendMessage(ChatUtils.chatMessage(Lang.get("give.received", "item", itemName + " x" + quantity)));
                        }
                        if (isKey && sender instanceof Player senderPlayer && !senderPlayer.getUniqueId().equals(player.getUniqueId())) {
                            sender.sendMessage(ChatUtils.chatMessage(Lang.get("give.success", "player", player.getName(), "amount", String.valueOf(quantity), "item", itemName)));
                        }
                    }
                } else if (instance instanceof Incantation incantation) {
                    ItemStack item = incantation.getItem();
                    int quantity = 1;
                    if (args.length >= 4) {
                        try {
                            quantity = Integer.parseInt(args[3]);
                            if (quantity <= 0 || quantity > item.getMaxStackSize()) {
                                throw new NumberFormatException();
                            }
                            item.setAmount(quantity);
                        } catch (Exception e) {
                            player.sendMessage(ChatUtils.chatMessage(Lang.get("give.invalid_quantity")));
                            return;
                        }
                    }

                    player.getInventory().addItem(item);

                    ItemMeta meta = item.getItemMeta();
                    String itemName = ChatUtils.getFormattedItemName(item.getType().name());
                    if (meta != null) {
                        if (meta.hasDisplayName()) {
                            itemName = meta.getDisplayName();
                        }
                    }

                    player.sendMessage(ChatUtils.chatMessage(Lang.get("give.received", "item", itemName + " x" + quantity)));
                    if (sender instanceof Player playerSender) {
                        if (!playerSender.getUniqueId().equals(player.getUniqueId())) {
                            sender.sendMessage(ChatUtils.chatMessage(Lang.get("give.success", "player", player.getName(), "amount", String.valueOf(quantity), "item", itemName)));
                        }
                    }
                } else {
                    player.sendMessage(ChatUtils.chatMessage(Lang.get("give.no_such_item")));
                }
            } else {
                sender.sendMessage(ChatUtils.chatMessage(Lang.get("player.not_found", "name", args[1])));
            }
        }
    }

}
