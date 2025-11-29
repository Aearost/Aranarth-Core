package com.aearost.aranarthcore.utils;

import com.aearost.aranarthcore.objects.Shop;
import org.bukkit.Location;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;

public class ShopUtils {

    private static final HashMap<UUID, List<Shop>> shops = new HashMap<>();

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
            shops.remove(shopSlotToDelete);
        }
    }

}
