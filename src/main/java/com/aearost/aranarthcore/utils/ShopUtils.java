package com.aearost.aranarthcore.utils;

import com.aearost.aranarthcore.objects.Shop;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.event.block.SignChangeEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Vector;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;


public class ShopUtils {

    private static final HashMap<UUID, List<Shop>> shops = new HashMap<>();
    private static final HashMap<Shop, Item> shopToHologram = new HashMap<>();

    /**
     * Confirms whether the location (a sign) is a player or server shop or not.
     * @param location The location of the sign.
     * @return Confirmation whether the location is a player or server shop or not.
     */
    public boolean isSignPlayerShop(Location location) {
        return getShopFromLocation(location) != null;
    }

    /**
     * Provides the current list of player shops.
     * @return The list of player shops.
     */
    public static HashMap<UUID, List<Shop>> getShops() {
        return shops;
    }

    /**
     * Provides the shop at the input sign location.
     * @param location The location of the sign.
     * @return The player shop if it exists.
     */
    public static Shop getShopFromLocation(Location location) {
        for (UUID uuid : shops.keySet()) {
            for (Shop shop : shops.get(uuid)) {
                if (shop.getLocation().equals(location)) {
                    return shop;
                }
            }
        }
        return null;
    }

    /**
     * Adding the input shop by the associated UUID.
     * @param uuid The UUID. Null if it is a server shop.
     * @param newShop The new player shop.
     */
    public static void addShop(UUID uuid, Shop newShop) {
        List<Shop> uuidShops = shops.get(uuid);
        if (uuidShops == null) {
            uuidShops = new ArrayList<>();
        }
        uuidShops.add(newShop);
        shops.put(uuid, uuidShops);
    }

    /**
     * Handles the logic of creating or updating a shop.
     * @param e The event.
     * @param player The player who created the shop.
     * @param shopItem The item being bought or sold in the shop.
     * @param quantity The quantity of the item being bought or sold in the shop.
     * @param buyPrice The price to buy the item from the shop.
     * @param sellPrice The price to sell the item to the shop.
     */
    public static void createOrUpdateShop(SignChangeEvent e, Player player, ItemStack shopItem, int quantity, double buyPrice, double sellPrice) {
        HashMap<UUID, List<Shop>> shops = ShopUtils.getShops();
        if (shops == null) {
            shops = new HashMap<>();
        }

        List<Shop> playerShops = null;
        UUID uuid = null;
        if (player != null) {
            uuid = player.getUniqueId();
        }

        playerShops = shops.get(uuid);
        if (playerShops == null) {
            playerShops = new ArrayList<>();
        }

        Block sign = e.getBlock();
        Shop existingShop = ShopUtils.getShopFromLocation(sign.getLocation());
        Shop newShop = null;
        newShop = new Shop(uuid, e.getBlock().getLocation(), shopItem, quantity, buyPrice, sellPrice);

        // If the shop exists, remove it
        if (existingShop != null) {
            ShopUtils.removeShop(uuid, sign.getLocation());
        }

        ShopUtils.addShop(uuid, newShop);
        if (player != null) {
            e.setLine(0, ChatUtils.translateToColor("&6&l[Shop]"));
            e.setLine(1, ChatUtils.translateToColor("&0&l" + ChatUtils.stripColorFormatting(e.getLines()[1]).toUpperCase()));
            e.setLine(2, ChatUtils.translateToColor(fixPriceLine(ChatUtils.stripColorFormatting(e.getLines()[2])).toUpperCase()));
            e.setLine(3, ChatUtils.translateToColor("&0" + player.getName()));
        } else {
            e.setLine(0, ChatUtils.translateToColor("&6&l[Server Shop]"));
            e.setLine(1, ChatUtils.translateToColor("&0&l" + ChatUtils.stripColorFormatting(e.getLines()[1]).toUpperCase()));
            e.setLine(2, ChatUtils.translateToColor(fixPriceLine(ChatUtils.stripColorFormatting(e.getLines()[2]).toUpperCase())));
            e.setLine(3, ChatUtils.getFormattedItemName(shopItem.getType().name()));
        }

        if (existingShop == null) {
            e.getPlayer().sendMessage(ChatUtils.chatMessage("&7You have created a new shop!"));
            initializeHologramAtLocation(e.getBlock().getLocation());
        } else {
            e.getPlayer().sendMessage(ChatUtils.chatMessage("&7You have updated this shop!"));
            removeHologramFromLocation(e.getBlock().getLocation());
            initializeHologramAtLocation(e.getBlock().getLocation());
        }
        e.getPlayer().playSound(e.getPlayer(), Sound.BLOCK_NOTE_BLOCK_XYLOPHONE, 1F, 1);
    }

    /**
     * Removes redundant decimal values from the price line.
     * @param line The line.
     * @return The line without the extra decimal values.
     */
    private static String fixPriceLine(String line) {
        String[] parts = line.split(" ");
        DecimalFormat df = new DecimalFormat("0.00");
        if (parts[0].equalsIgnoreCase("B")) {
            if (parts.length == 2) {
                double priceAsDouble = Double.parseDouble(parts[1]);
                double trimmedPrice = Double.parseDouble(df.format(priceAsDouble));
                String asString = (trimmedPrice + "");
                if (asString.contains(".")) {
                    String decimalAmount = asString.split("\\.")[1];
                    if (decimalAmount.equals("0") || decimalAmount.equals("00")) {
                        return "&0&lB &r" + asString.split("\\.")[0];
                    }
                }
                return "&0&lB &r" + trimmedPrice;
            } else {
                double buyPriceAsDouble = Double.parseDouble(parts[1]);
                double trimmedBuyPrice = Double.parseDouble(df.format(buyPriceAsDouble));
                double sellPriceAsDouble = Double.parseDouble(parts[4]);
                double trimmedSellPrice = Double.parseDouble(df.format(sellPriceAsDouble));
                String buyAsString = (trimmedBuyPrice + "");
                if (buyAsString.contains(".")) {
                    String decimalAmount = buyAsString.split("\\.")[1];
                    if (decimalAmount.equals("0") || decimalAmount.equals("00")) {
                        buyAsString = "&0&lB &r" + buyAsString.split("\\.")[0];
                    } else {
                        buyAsString = "&0&lB &r" + buyAsString;
                    }
                }
                String sellAsString = (trimmedSellPrice + "");
                if (sellAsString.contains(".")) {
                    String decimalAmount = sellAsString.split("\\.")[1];
                    if (decimalAmount.equals("0") || decimalAmount.equals("00")) {
                        sellAsString = "&0&lS &r" + sellAsString.split("\\.")[0];
                    } else {
                        sellAsString = "&0&lS &r" + sellAsString;
                    }
                }


                return buyAsString + " | " + sellAsString;
            }
        } else {
            double priceAsDouble = Double.parseDouble(parts[1]);
            double trimmedPrice = Double.parseDouble(df.format(priceAsDouble));
            String asString = (trimmedPrice + "");
            if (asString.contains(".")) {
                String decimalAmount = asString.split("\\.")[1];
                if (decimalAmount.equals("0") || decimalAmount.equals("00")) {
                    return "&0&lS &r" + asString.split("\\.")[0];
                }
            }
            return "&0&lS &r" + trimmedPrice;
        }
    }

    /**
     * Removes the player shop at the associated location for the input UUID.
     * @param uuid The UUID.
     * @param location The location of the sign of the shop.
     */
    public static void removeShop(UUID uuid, Location location) {
        List<Shop> shops = ShopUtils.shops.get(uuid);
        int shopSlotToDelete = -1;
        for (int i = 0; i < shops.size(); i++) {
            if (shops.get(i).getLocation().equals(location)) {
                shopSlotToDelete = i;
                break;
            }
        }
        // Only delete if a shop was found
        if (shopSlotToDelete != -1) {
            removeHologramFromLocation(shops.get(shopSlotToDelete).getLocation());
            shops.remove(shopSlotToDelete);
        }
    }

    /**
     * Creates a hologram of the shop item at the sign's location.
     * @param loc The location of the shop.
     */
    public static void initializeHologramAtLocation(Location loc) {
        Shop shop = getShopFromLocation(loc);
        if (shop != null) {
            ItemStack item = shop.getItem().clone();
            item.setAmount(1);

            loc = loc.clone();
            // Player shop
            if (shop.getUuid() != null) {
                loc.add(0.5, -0.1, 0.5);
            }
            // Server shop
            else {
                loc.add(0.5, 0.8, 0.5);
            }

            Item hologram = loc.getWorld().spawn(loc, Item.class, entity -> {
                entity.setItemStack(item);
                entity.setGravity(false);
                entity.setVelocity(new Vector(0, 0, 0));
                entity.setPickupDelay(Integer.MAX_VALUE);
                entity.setUnlimitedLifetime(true);
                entity.setPersistent(true);
            });

            shopToHologram.put(shop, hologram);
        }
    }

    /**
     * Removes a hologram of the shop item at the sign's location.
     * @param loc The location of the shop.
     */
    public static void removeHologramFromLocation(Location loc) {
        Shop shop = getShopFromLocation(loc);
        if (shop != null) {
            Item hologram = shopToHologram.get(shop);
            if (hologram != null) {
                hologram.remove();
                shopToHologram.remove(shop);
            }
        }
    }

    /**
     * Creates all holograms for all shops.
     */
    public static void initializeAllHolograms() {
        for (UUID uuid : shops.keySet()) {
            for (Shop shop : shops.get(uuid)) {
                initializeHologramAtLocation(shop.getLocation());
            }
        }
    }

    /**
     * Removes all holograms for all shops.
     */
    public static void removeAllHolograms() {
        for (UUID uuid : shops.keySet()) {
            for (Shop shop : shops.get(uuid)) {
                removeHologramFromLocation(shop.getLocation());
            }
        }
    }

}
