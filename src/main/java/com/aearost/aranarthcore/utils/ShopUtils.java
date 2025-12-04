package com.aearost.aranarthcore.utils;

import com.aearost.aranarthcore.objects.AranarthPlayer;
import com.aearost.aranarthcore.objects.Shop;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.block.*;
import org.bukkit.entity.Display;
import org.bukkit.entity.Entity;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.entity.Player;
import org.bukkit.event.block.SignChangeEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.BoundingBox;
import org.bukkit.util.Transformation;
import org.bukkit.util.Vector;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.text.DecimalFormat;
import java.util.*;


public class ShopUtils {

    private static final HashMap<UUID, List<Shop>> shops = new HashMap<>();
    private static final HashMap<Shop, ItemDisplay> shopToHologram = new HashMap<>();

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
        Location loc = sign.getLocation();
        Shop existingShop = ShopUtils.getShopFromLocation(loc);

        // If the shop exists, remove it
        if (existingShop != null) {
            ShopUtils.removeShop(existingShop);
        }

        Shop newShop = new Shop(uuid, e.getBlock().getLocation(), shopItem, quantity, buyPrice, sellPrice);
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
        } else {
            e.getPlayer().sendMessage(ChatUtils.chatMessage("&7You have updated this shop!"));
        }

        initializeShopHologram(newShop);
        e.getPlayer().playSound(e.getPlayer(), Sound.BLOCK_NOTE_BLOCK_XYLOPHONE, 1F, 1);
    }

    /**
     * Removes redundant decimal values from the price line.
     * @param line The line.
     * @return The line without the extra decimal values.
     */
    private static String fixPriceLine(String line) {
        String[] parts = line.split(" ");
        // Cannot use the Currency formatter as it would take up extra characters
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
     * Removes the shop from the shops hashmap.
     * @param shop The shop.
     */
    public static void removeShop(Shop shop) {
        List<Shop> uuidShops = shops.get(shop.getUuid());
        int shopSlotToDelete = -1;
        for (int i = 0; i < uuidShops.size(); i++) {
            if (uuidShops.get(i).equals(shop)) {
                shopSlotToDelete = i;
                break;
            }
        }

        // Only delete if a shop was found
        if (shopSlotToDelete != -1) {
            removeShopHologram(shop);
            uuidShops.remove(shopSlotToDelete);
            shops.put(shop.getUuid(), uuidShops);
        }
    }

    /**
     * Creates a hologram of the shop item.
     * @param shop The shop.
     */
    public static void initializeShopHologram(Shop shop) {
        if (shop != null) {
            ItemStack item = shop.getItem().clone();
            item.setAmount(1);

            Location loc = shop.getLocation().clone();
            // Player shop
            if (shop.getUuid() != null) {
                loc.add(0.5, 0.15, 0.5);
            }
            // Server shop
            else {
                loc.add(0.5, 1, 0.5);
            }

            ItemDisplay hologram = loc.getWorld().spawn(loc, ItemDisplay.class);
            hologram.setItemStack(item);
            hologram.setBillboard(Display.Billboard.CENTER);
            hologram.setGravity(false);
            hologram.setVelocity(new Vector(0, 0, 0));
            hologram.setInvulnerable(true);
            hologram.setPersistent(true);

            // Decreases the size of the display
            Vector3f scale = new Vector3f(0.35F, 0.35F, 0.35F);
            Quaternionf rotate180 = new Quaternionf().rotateY((float) Math.PI);
            Transformation transformation = new Transformation(new Vector3f(0, 0, 0), rotate180, scale, new Quaternionf());
            hologram.setTransformation(transformation);

            shopToHologram.put(shop, hologram);
        }
    }

    /**
     * Removes a hologram of the shop item.
     * @param shop The shop.
     */
    public static void removeShopHologram(Shop shop) {
        if (shop != null) {
            ItemDisplay hologram = shopToHologram.get(shop);
            if (hologram != null) {
                hologram.remove();

                // Bug when server is restarted that holograms are created and orphaned
                // This cleans them up
                Location loc = hologram.getLocation();
                BoundingBox box = BoundingBox.of(loc, 0.5, 3, 0.5);
                Collection<Entity> nearby = loc.getWorld().getNearbyEntities(box);
                for (Entity entity : nearby) {
                    if (entity instanceof ItemDisplay) {
                        entity.remove();
                    }
                }

                shopToHologram.put(shop, hologram);
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
                initializeShopHologram(shop);
            }
        }
    }

    /**
     * Removes all holograms for all shops.
     */
    public static void removeAllHolograms() {
        for (UUID uuid : shops.keySet()) {
            for (Shop shop : shops.get(uuid)) {
                removeShopHologram(shop);
            }
        }
    }

    /**
     * Provides the shop with its quantities and prices maxed out, tailored to the player making the transaction.
     * @param shop The original shop object.
     * @param player The player that is interacting with the shop.
     * @param isBuying Whether the player is buying or not from the shop.
     * @return The bulk variant of the shop.
     */
    public static Shop getBulkShop(Shop shop, Player player, boolean isBuying) {
        AranarthPlayer aranarthPlayer = AranarthUtils.getPlayer(player.getUniqueId());

        int playerFreeInventorySpace = 0;
        int playerQuantityOfShopItem = 0;

        // Calculates the available space for the item, and the quantity already in the player's inventory
        for (ItemStack inventoryItem : player.getInventory().getStorageContents().clone()) {
            if (inventoryItem == null || inventoryItem.getType() == Material.AIR) {
                playerFreeInventorySpace += 64;
                continue;
            }

            if (inventoryItem.isSimilar(shop.getItem())) {
                playerQuantityOfShopItem += inventoryItem.getAmount();
                int extraSpaceInStack = inventoryItem.getMaxStackSize() - inventoryItem.getAmount();
                if (extraSpaceInStack > 0) {
                    playerFreeInventorySpace += extraSpaceInStack;
                }
            }
        }

        int chestFreeInventorySpace = 0;
        int chestQuantityOfShopItem = 0;

        // Calculates the available space for the item, and the quantity already in the chest's inventory
        // Only for player shops
        if (shop.getUuid() != null) {
            Inventory chestInventory = null;
            BlockState chestBlockState = shop.getLocation().getBlock().getRelative(BlockFace.DOWN).getState();
            Container container = (Container) chestBlockState;
            chestInventory = container.getInventory();
            if (chestInventory.getHolder() instanceof DoubleChest doubleChest) {
                chestInventory = doubleChest.getInventory(); // Get the full 54 slot inventory
            }

            for (ItemStack chestItem : chestInventory.getContents()) {
                if (chestItem == null || chestItem.getType() == Material.AIR) {
                    chestFreeInventorySpace += 64;
                    continue;
                }

                if (chestItem.isSimilar(shop.getItem())) {
                    chestQuantityOfShopItem += chestItem.getAmount();
                    int extraSpaceInStack = chestItem.getMaxStackSize() - chestItem.getAmount();
                    if (extraSpaceInStack > 0) {
                        chestFreeInventorySpace += extraSpaceInStack;
                    }
                }
            }
        }

        // Buying
        if (isBuying) {
            int bulkQuantity = shop.getQuantity();
            double bulkBuyPrice = shop.getBuyPrice();
            boolean canPurchaseMore = true;
            int multiplier = 2;

            // Calculate if the quantity can be purchased with the new multiplier
            while (canPurchaseMore) {
                int newQuantity = shop.getQuantity() * multiplier;
                double newBuyPrice = shop.getBuyPrice() * multiplier;

                // Verifies the player will have enough balance
                if (aranarthPlayer.getBalance() < newBuyPrice) {
                    canPurchaseMore = false;
                }

                // Verifies the player will have enough inventory space
                if (playerFreeInventorySpace < newQuantity) {
                    canPurchaseMore = false;
                }

                // If a player shop, verify the chest has enough of the item
                if (shop.getUuid() != null) {
                    if (chestQuantityOfShopItem < newQuantity) {
                        canPurchaseMore = false;
                    }
                }

                // Increase to the next multiplier to see if it can be purchased
                if (canPurchaseMore) {
                    bulkBuyPrice = newBuyPrice;
                    bulkQuantity = newQuantity;
                    multiplier++;
                }
            }
            return new Shop(shop.getUuid(), shop.getLocation(), shop.getItem().clone(), bulkQuantity, bulkBuyPrice, 0);
        }
        // Selling
        else {
            int bulkQuantity = shop.getQuantity();
            double bulkSellPrice = shop.getSellPrice();
            boolean canSellMore = true;
            int multiplier = 2;

            // Calculate if the quantity can be sold with the new multiplier
            while (canSellMore) {
                int newQuantity = shop.getQuantity() * multiplier;
                double newSellPrice = shop.getSellPrice() * multiplier;

                // Verifies the player has enough of the item to sell
                if (playerQuantityOfShopItem < newQuantity) {
                    canSellMore = false;
                }

                // If a player shop
                if (shop.getUuid() != null) {
                    // Verifies there is enough space in the chest for the items to be sold
                    if (chestFreeInventorySpace < newQuantity) {
                        canSellMore = false;
                    }

                    // Verifies the owner of the shop will have enough balance
                    if (AranarthUtils.getPlayer(shop.getUuid()).getBalance() < newSellPrice) {
                        canSellMore = false;
                    }
                }

                // Increase to the next multiplier to see if it can be sold
                if (canSellMore) {
                    bulkSellPrice = newSellPrice;
                    bulkQuantity = newQuantity;
                    multiplier++;
                }
            }
            return new Shop(shop.getUuid(), shop.getLocation(), shop.getItem().clone(), bulkQuantity, 0, bulkSellPrice);
        }
    }
}
