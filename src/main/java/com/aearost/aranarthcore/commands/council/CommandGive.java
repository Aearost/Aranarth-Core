package com.aearost.aranarthcore.commands.council;

import com.aearost.aranarthcore.AranarthCore;
import com.aearost.aranarthcore.items.AranarthItem;
import com.aearost.aranarthcore.items.essence.Essence;
import com.aearost.aranarthcore.items.essence.EssenceBlank;
import com.aearost.aranarthcore.network.NetworkManager;
import com.aearost.aranarthcore.network.NetworkPlayer;
import com.aearost.aranarthcore.utils.AranarthUtils;
import com.aearost.aranarthcore.utils.ChatUtils;
import com.aearost.aranarthcore.utils.DiscordUtils;
import com.aearost.aranarthcore.utils.EssenceImbueUtils;
import com.aearost.aranarthcore.utils.Lang;
import com.aearost.aranarthcore.utils.PersistenceUtils;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.awt.*;
import java.lang.reflect.InvocationTargetException;
import java.util.Collections;
import java.util.HashMap;
import java.util.UUID;

import static com.aearost.aranarthcore.objects.CustomKeys.COUPON_DISCOUNT;

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
                player.sendMessage(ChatUtils.chatMessage(Lang.getFor(player, "general.no_permission")));
                return;
            }
        }

        if (args.length < 3) {
            sender.sendMessage(ChatUtils.chatMessage(Lang.getFor(sender, "general.invalid_syntax", "usage", "ac give <player> <item> <quantity>")));
            return;
        } else {
            // Store purchases are dispatched on both servers, so they must only be processed on
            // the Survival server, which forwards the key to the SMP server if the player is there
            boolean isStoreKeyPurchase = args[2].startsWith("Key") && !args[2].equals("KeyVote")
                    && args.length >= 5 && args[4].equalsIgnoreCase("broadcast");
            if (isStoreKeyPurchase && AranarthCore.isSmpServer() && !(sender instanceof Player)) {
                return;
            }

            Player player = null;
            for (Player onlinePlayer : Bukkit.getOnlinePlayers()) {
                if (onlinePlayer.getName().equals(args[1])) {
                    player = onlinePlayer;
                    break;
                }
            }

            if (player != null) {
                // Inline handling for store coupons
                if (args[2].equals("Coupon10") || args[2].equals("Coupon30")) {
                    if (sender instanceof Player senderPlayer) {
                        if (!senderPlayer.hasPermission("aranarth.give.coupon")) {
                            senderPlayer.sendMessage(ChatUtils.chatMessage(Lang.getFor(senderPlayer, "general.no_permission")));
                            return;
                        }
                    }
                    int discountPercentage = args[2].equals("Coupon10") ? 10 : 30;
                    ItemStack coupon = createCoupon(discountPercentage);
                    ItemMeta couponMeta = coupon.getItemMeta();
                    HashMap<Integer, ItemStack> remainder = player.getInventory().addItem(coupon);
                    if (!remainder.isEmpty()) {
                        for (ItemStack value : remainder.values()) {
                            player.getWorld().dropItemNaturally(player.getLocation(), value);
                        }
                    }
                    String itemName = couponMeta.getDisplayName();
                    player.sendMessage(ChatUtils.chatMessage(Lang.getFor(player, "give.received", "item", itemName + " x1")));
                    if (sender instanceof Player senderPlayer && !senderPlayer.getUniqueId().equals(player.getUniqueId())) {
                        sender.sendMessage(ChatUtils.chatMessage(Lang.getFor(sender, "give.success", "player", player.getName(), "amount", "1", "item", itemName)));
                    }
                    return;
                }

                boolean isKey = args[2].startsWith("Key");
                Object instance = createItemInstance(args[2]);
                if (instance == null) {
                    sender.sendMessage(ChatUtils.chatMessage(Lang.getFor(sender, "give.no_such_item")));
                    return;
                }

                if (instance instanceof AranarthItem aranarthItem) {
                    ItemStack item = aranarthItem.getItem();
                    int quantity = 1;
                    // The blank Essence takes an optional imbued percentage instead of a quantity
                    if (aranarthItem instanceof EssenceBlank) {
                        if (args.length >= 4) {
                            try {
                                double percent = Double.parseDouble(args[3]);
                                if (percent < 0 || percent > 100) {
                                    throw new NumberFormatException();
                                }
                                EssenceImbueUtils.setImbuedXp(item, (int) Math.round(EssenceImbueUtils.getMaxImbuedXp() * percent / 100));
                            } catch (NumberFormatException e) {
                                sender.sendMessage(ChatUtils.chatMessage(Lang.getFor(sender, "particles.invalid_range")));
                                return;
                            }
                        }
                    } else if (args.length >= 4) {
                        try {
                            quantity = Integer.parseInt(args[3]);
                            if (quantity <= 0 || quantity > item.getMaxStackSize()) {
                                throw new NumberFormatException();
                            }
                            item.setAmount(quantity);
                        } catch (Exception e) {
                            sender.sendMessage(ChatUtils.chatMessage(Lang.getFor(sender, "give.invalid_quantity")));
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
                            DiscordUtils.donationNotification(player.getName() + " has purchased " + itemName + " x" + quantity, player.getUniqueId(), Color.CYAN);
                            for (Player online : Bukkit.getOnlinePlayers()) {
                                online.sendMessage(ChatUtils.chatMessage(Lang.getFor(online, "give.purchased_broadcast", "player", player.getName(), "item", itemName, "amount", String.valueOf(quantity))));
                            }
                            if (NetworkManager.isActive()) {
                                NetworkManager.getInstance().publishLangBroadcast("give.purchased_broadcast", "player", player.getName(), "item", itemName, "amount", String.valueOf(quantity));
                            }
                        }

                        String worldName = player.getWorld().getName();
                        if (!AranarthUtils.isSurvivalWorld(worldName)) {
                            AranarthUtils.addPendingKey(player.getUniqueId(), item, quantity);
                            player.sendMessage(ChatUtils.chatMessage(Lang.getFor(player, "give.keyclaim_hint")));
                            if (!isBroadcast && sender instanceof Player senderPlayer && !senderPlayer.getUniqueId().equals(player.getUniqueId())) {
                                sender.sendMessage(ChatUtils.chatMessage(Lang.getFor(sender, "give.success", "player", player.getName(), "amount", String.valueOf(quantity), "item", itemName)));
                            }
                            // Persist immediately so /keyclaim can see the keys even if the player
                            // switches servers before the next periodic save.
                            final UUID pendingUuid = player.getUniqueId();
                            Bukkit.getScheduler().runTaskAsynchronously(AranarthCore.getInstance(),
                                    () -> PersistenceUtils.syncVoteKeysForPlayerToDatabase(pendingUuid));
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
                            player.sendMessage(ChatUtils.chatMessage(Lang.getFor(player, "give.dropped", "item", itemName)));
                        } else {
                            player.sendMessage(ChatUtils.chatMessage(Lang.getFor(player, "give.received", "item", itemName + " x" + quantity)));
                        }
                        if (sender instanceof Player senderPlayer && !senderPlayer.getUniqueId().equals(player.getUniqueId())) {
                            sender.sendMessage(ChatUtils.chatMessage(Lang.getFor(sender, "give.success", "player", player.getName(), "amount", String.valueOf(quantity), "item", itemName)));
                        }
                    }
                } else if (instance instanceof Essence essence) {
                    ItemStack item = essence.getItem();
                    int quantity = 1;
                    if (args.length >= 4) {
                        try {
                            quantity = Integer.parseInt(args[3]);
                            if (quantity <= 0 || quantity > item.getMaxStackSize()) {
                                throw new NumberFormatException();
                            }
                            item.setAmount(quantity);
                        } catch (Exception e) {
                            sender.sendMessage(ChatUtils.chatMessage(Lang.getFor(sender, "give.invalid_quantity")));
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

                    player.sendMessage(ChatUtils.chatMessage(Lang.getFor(player, "give.received", "item", itemName + " x" + quantity)));
                    if (sender instanceof Player playerSender) {
                        if (!playerSender.getUniqueId().equals(player.getUniqueId())) {
                            sender.sendMessage(ChatUtils.chatMessage(Lang.getFor(sender, "give.success", "player", player.getName(), "amount", String.valueOf(quantity), "item", itemName)));
                        }
                    }
                } else {
                    sender.sendMessage(ChatUtils.chatMessage(Lang.getFor(sender, "give.no_such_item")));
                }
            } else {
                // Player not on this server - check if this is a store key purchase and
                // the player is on the remote server, so we can deliver cross-server.
                if (isStoreKeyPurchase && NetworkManager.isActive()) {
                    UUID remoteUuid = null;
                    String remoteName = null;
                    for (NetworkPlayer np : NetworkManager.getInstance().getRemoteRoster().values()) {
                        if (np.getUsername().equalsIgnoreCase(args[1])) {
                            remoteUuid = np.getUuid();
                            remoteName = np.getUsername();
                            break;
                        }
                    }
                    if (remoteUuid != null) {
                        int quantity = 1;
                        if (args.length >= 4) {
                            try {
                                quantity = Math.max(1, Integer.parseInt(args[3]));
                            } catch (NumberFormatException ignored) {
                            }
                        }
                        // Map class name to key type string
                        String keyType = switch (args[2]) {
                            case "KeyRare" -> "key_rare";
                            case "KeyEpic" -> "key_epic";
                            case "KeyGodly" -> "key_godly";
                            default -> null;
                        };
                        if (keyType != null) {
                            // Derive item display name for the broadcast
                            String itemName;
                            try {
                                Class<?> cls = Class.forName("com.aearost.aranarthcore.items.key." + args[2]);
                                Object inst = cls.getDeclaredConstructor().newInstance();
                                if (inst instanceof AranarthItem ai) {
                                    ItemStack tmp = ai.getItem();
                                    ItemMeta tmpMeta = tmp.getItemMeta();
                                    itemName = (tmpMeta != null && tmpMeta.hasDisplayName())
                                            ? tmpMeta.getDisplayName()
                                            : ChatUtils.getFormattedItemName(tmp.getType().name());
                                } else {
                                    itemName = args[2];
                                }
                            } catch (Exception ignored) {
                                itemName = args[2];
                            }
                            DiscordUtils.donationNotification(remoteName + " has purchased " + itemName + " x" + quantity, remoteUuid, Color.CYAN);
                            for (Player online : Bukkit.getOnlinePlayers()) {
                                online.sendMessage(ChatUtils.chatMessage(Lang.getFor(online, "give.purchased_broadcast", "player", remoteName, "item", itemName, "amount", String.valueOf(quantity))));
                            }
                            NetworkManager.getInstance().publishLangBroadcast("give.purchased_broadcast", "player", remoteName, "item", itemName, "amount", String.valueOf(quantity));
                            NetworkManager.getInstance().publishPurchaseKey(remoteUuid, keyType, quantity);
                            Bukkit.getLogger().info(AranarthCore.LOG_PREFIX + "[STORE] Player " + remoteName + " is on remote server - forwarding " + keyType + " x" + quantity + " cross-server");
                        }
                    } else {
                        sender.sendMessage(ChatUtils.chatMessage(Lang.getFor(sender, "player.not_found", "name", args[1])));
                    }
                } else {
                    // Console dispatches run on both servers, so only a player's command is forwarded.
                    // Otherwise the item would also be delivered by the server the target is on.
                    if (sender instanceof Player senderPlayer && forwardToRemotePlayer(senderPlayer, args)) {
                        return;
                    }
                    sender.sendMessage(ChatUtils.chatMessage(Lang.getFor(sender, "player.not_found", "name", args[1])));
                }
            }
        }
    }

    /**
     * Validates the command on this server and tells the server the target is on to deliver the item.
     *
     * @param sender The player that entered the command.
     * @param args   The arguments of the command.
     * @return True if the target is on the other server, even if the command was then rejected.
     */
    private static boolean forwardToRemotePlayer(Player sender, String[] args) {
        if (!NetworkManager.isActive()) {
            return false;
        }

        NetworkPlayer target = null;
        for (NetworkPlayer np : NetworkManager.getInstance().getRemoteRoster().values()) {
            if (np.getUsername().equalsIgnoreCase(args[1])) {
                target = np;
                break;
            }
        }
        if (target == null) {
            return false;
        }

        ItemStack item;
        int quantity = 1;
        if (args[2].equals("Coupon10") || args[2].equals("Coupon30")) {
            if (!sender.hasPermission("aranarth.give.coupon")) {
                sender.sendMessage(ChatUtils.chatMessage(Lang.getFor(sender, "general.no_permission")));
                return true;
            }
            item = createCoupon(args[2].equals("Coupon10") ? 10 : 30);
        } else {
            Object instance = createItemInstance(args[2]);
            if (instance instanceof AranarthItem aranarthItem) {
                item = aranarthItem.getItem();
            } else if (instance instanceof Essence essence) {
                item = essence.getItem();
            } else {
                sender.sendMessage(ChatUtils.chatMessage(Lang.getFor(sender, "give.no_such_item")));
                return true;
            }

            if (args.length >= 4) {
                boolean isBlankEssence = instance instanceof EssenceBlank;
                try {
                    if (isBlankEssence) {
                        double percent = Double.parseDouble(args[3]);
                        if (percent < 0 || percent > 100) {
                            throw new NumberFormatException();
                        }
                    } else {
                        quantity = Integer.parseInt(args[3]);
                        if (quantity <= 0 || quantity > item.getMaxStackSize()) {
                            throw new NumberFormatException();
                        }
                    }
                } catch (NumberFormatException e) {
                    sender.sendMessage(ChatUtils.chatMessage(Lang.getFor(sender, isBlankEssence ? "particles.invalid_range" : "give.invalid_quantity")));
                    return true;
                }
            }
        }

        ItemMeta meta = item.getItemMeta();
        String itemName = ChatUtils.getFormattedItemName(item.getType().name());
        if (meta != null && meta.hasDisplayName()) {
            itemName = meta.getDisplayName();
        }

        NetworkManager.getInstance().publishGive(target.getUuid(), args);
        sender.sendMessage(ChatUtils.chatMessage(Lang.getFor(sender, "give.success", "player", target.getUsername(), "amount", String.valueOf(quantity), "item", itemName)));
        return true;
    }

    /**
     * @param discountPercentage The discount the coupon provides.
     * @return The store coupon item.
     */
    private static ItemStack createCoupon(int discountPercentage) {
        ItemStack coupon = new ItemStack(Material.PAPER, 1);
        ItemMeta couponMeta = coupon.getItemMeta();
        couponMeta.setMaxStackSize(1);
        couponMeta.setDisplayName(ChatUtils.translateToColor("&6&l" + discountPercentage + "% Store Coupon"));
        couponMeta.setLore(Collections.singletonList(ChatUtils.translateToColor("&7&oSneak and Right-Click to Redeem")));
        couponMeta.getPersistentDataContainer().set(COUPON_DISCOUNT, PersistentDataType.INTEGER, discountPercentage);
        coupon.setItemMeta(couponMeta);
        return coupon;
    }

    /**
     * Dynamically creates the item object associated to the input class name.
     *
     * @param className The simple class name of the item.
     * @return The item object, or null if there is no class by that name.
     */
    private static Object createItemInstance(String className) {
        String fullPathName;
        if (className.startsWith("Aranarthium")) {
            fullPathName = "com.aearost.aranarthcore.items.aranarthium.ingots." + className;
        } else if (className.endsWith("Cluster")) {
            fullPathName = "com.aearost.aranarthcore.items.aranarthium.clusters." + className;
        } else if (className.startsWith("Netherite")) {
            fullPathName = "com.aearost.aranarthcore.items.netherite." + className;
        } else if (className.endsWith("Helmet") || className.endsWith("Chestplate")
                || className.endsWith("Leggings") || className.endsWith("Boots")
                || className.endsWith("Elytra")) {
            fullPathName = "com.aearost.aranarthcore.items.aranarthium.armour." + className;
        } else if (className.startsWith("Arrow")) {
            if (className.startsWith("Arrowhead")) {
                fullPathName = "com.aearost.aranarthcore.items.arrowhead." + className;
            } else {
                fullPathName = "com.aearost.aranarthcore.items.arrow." + className;
            }
        } else if (className.startsWith("Key")) {
            fullPathName = "com.aearost.aranarthcore.items.key." + className;
        } else if (className.startsWith("Essence")) {
            fullPathName = "com.aearost.aranarthcore.items.essence." + className;
        } else {
            fullPathName = "com.aearost.aranarthcore.items." + className;
        }

        try {
            return Class.forName(fullPathName).getDeclaredConstructor().newInstance();
        } catch (ClassNotFoundException | InvocationTargetException | InstantiationException
                 | IllegalAccessException | NoSuchMethodException e) {
            return null;
        }
    }

}
