package com.aearost.aranarthcore.utils;

import com.aearost.aranarthcore.AranarthCore;
import com.aearost.aranarthcore.database.DatabaseManager;
import com.aearost.aranarthcore.network.NetworkManager;
import com.aearost.aranarthcore.objects.MarketDynamics;
import com.aearost.aranarthcore.objects.Shop;
import org.bukkit.Bukkit;
import org.bukkit.block.Block;
import org.bukkit.block.Sign;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Manages dynamic market pricing for server shops.
 */
public class MarketUtils {

    private static final Map<String, MarketDynamics> marketData = new HashMap<>();

    // 36 slots * 64 items per slot
    private static final double INVENTORY_EQUIVALENT = 2304.0;

    // Price floor and ceiling relative to default
    private static final double PRICE_FLOOR_MODIFIER = 0.25;
    private static final double PRICE_CEILING_MODIFIER = 2.5;

    // Per-tick constants
    private static final double DECAY_RATE = 0.95;
    private static final double SENSITIVITY = 0.000691;
    private static final double RISE_RATE = 0.00127;
    private static final double PRESSURE_THRESHOLD = 0.01;
    private static final double CHANGE_THRESHOLD = 0.005;

    /**
     * Returns the unique shop key for the given shop ("worldName,x,y,z").
     */
    public static String getShopKey(Shop shop) {
        return shop.getWorldName() + "," + shop.getLocation().getBlockX()
                + "," + shop.getLocation().getBlockY()
                + "," + shop.getLocation().getBlockZ();
    }

    /**
     * Returns the MarketDynamics for the given key, or null if none exists.
     */
    public static MarketDynamics getMarketData(String key) {
        return marketData.get(key);
    }

    /**
     * Adds or replaces a MarketDynamics entry in the in-memory map.
     */
    public static void addMarketData(MarketDynamics data) {
        marketData.put(data.getShopKey(), data);
    }

    /**
     * Returns the full in-memory market data map.
     */
    public static Map<String, MarketDynamics> getAllMarketData() {
        return marketData;
    }

    /**
     * Adds sell pressure for the given server shop.
     */
    public static void addSellPressure(Shop shop, int quantitySold) {
        String key = getShopKey(shop);
        if (marketData.get(key) == null) {
            return;
        }
        double delta = quantitySold / INVENTORY_EQUIVALENT;
        // Write atomically to DB so both servers contribute to the same counter
        Bukkit.getScheduler().runTaskAsynchronously(AranarthCore.getInstance(),
                () -> DatabaseManager.getInstance().addMarketSellPressure(key, delta));
    }

    /**
     * Called when an admin creates or updates a server shop sign.
     */
    public static void initOrUpdateServerShop(Shop shop, double adminTypedSellPrice) {
        String key = getShopKey(shop);
        MarketDynamics existing = marketData.get(key);
        if (existing == null) {
            MarketDynamics data = new MarketDynamics(key, adminTypedSellPrice, 1.0, 0.0);
            marketData.put(key, data);
            shop.setSellPrice(adminTypedSellPrice);
        } else {
            double modifier = existing.getCurrentPriceModifier();
            existing.setDefaultSellPrice(adminTypedSellPrice);
            double newPrice = Math.round(adminTypedSellPrice * modifier * 100.0) / 100.0;
            shop.setSellPrice(newPrice);
        }
    }

    /**
     * Runs the hourly market price tick on the Survival server.
     */
    public static void runPriceTick(AranarthCore plugin) {
        if (AranarthCore.isSmpServer()) {
            return;
        }

        List<Shop> serverShops = ShopUtils.getShops().get(null);
        if (serverShops == null) {
            return;
        }

        // Load fresh pressure values from DB (includes SMP contributions) then apply on main thread
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            List<Object[]> rows = DatabaseManager.getInstance().loadAllMarketDynamics();
            Map<String, Double> freshPressures = new HashMap<>();
            for (Object[] row : rows) {
                freshPressures.put((String) row[0], (double) row[3]);
            }

            Bukkit.getScheduler().runTask(plugin, () -> {
                for (Shop shop : serverShops) {
                    if (shop.getSellPrice() <= 0) {
                        continue;
                    }

                    String key = getShopKey(shop);
                    MarketDynamics data = marketData.get(key);
                    if (data == null) {
                        continue;
                    }

                    double currentPrice = shop.getSellPrice();
                    double defaultPrice = data.getDefaultSellPrice();

                    // Use fresh DB pressure (combines Survival + SMP sells since last tick)
                    double combinedPressure = freshPressures.getOrDefault(key, data.getSellPressure());
                    double newPressure = combinedPressure * DECAY_RATE;
                    data.setSellPressure(newPressure);

                    double newPrice;
                    if (newPressure > PRESSURE_THRESHOLD) {
                        // Sell pressure is driving the price down
                        double dropRate = newPressure * SENSITIVITY;
                        if (currentPrice > defaultPrice) {
                            double excess = currentPrice / defaultPrice - 1.0;
                            dropRate *= 1.0 + excess * 2.0;
                        }
                        newPrice = Math.max(defaultPrice * PRICE_FLOOR_MODIFIER, currentPrice * (1.0 - dropRate));
                    } else {
                        // No meaningful pressure - price recovers toward ceiling
                        newPrice = Math.min(defaultPrice * PRICE_CEILING_MODIFIER, currentPrice * (1.0 + RISE_RATE));
                    }

                    if (Math.abs(newPrice - currentPrice) > CHANGE_THRESHOLD) {
                        double modifier = newPrice / defaultPrice;
                        data.setCurrentPriceModifier(modifier);
                        double rounded = Math.round(newPrice * 100.0) / 100.0;
                        shop.setSellPrice(rounded);
                        refreshServerShopSign(shop);
                    }
                }

                // Sync post-tick state (decayed pressure + updated modifiers) to DB
                PersistenceUtils.syncMarketDynamicsToDatabase();

                // Notify SMP to reload market data and refresh its signs
                if (NetworkManager.isActive()) {
                    NetworkManager.getInstance().publishMarketUpdate();
                }
            });
        });
    }

    /**
     * Updates the physical sign in the world to reflect the shop's current sell price.
     */
    public static void refreshServerShopSign(Shop shop) {
        if (shop.getLocation() == null) {
            return;
        }
        Block block = shop.getLocation().getBlock();
        if (!(block.getState() instanceof Sign sign)) {
            return;
        }

        double buyPrice = shop.getBuyPrice();
        double sellPrice = shop.getSellPrice();

        String priceLine;
        if (buyPrice > 0 && sellPrice > 0) {
            priceLine = "&0&lB &r" + formatPrice(buyPrice) + " | &0&lS &r" + formatPrice(sellPrice);
        } else if (sellPrice > 0) {
            priceLine = "&0&lS &r" + formatPrice(sellPrice);
        } else {
            // Buy-only shop - sell price not shown, nothing to update
            return;
        }

        sign.setLine(2, ChatUtils.translateToColor(priceLine));
        sign.update(true);
    }

    /**
     * Formats a price to at most 2 decimal places, removing unnecessary trailing zeros.
     */
    private static String formatPrice(double price) {
        // Round to 2 decimal places first to eliminate floating point artifacts
        price = Math.round(price * 100.0) / 100.0;
        if (price == Math.floor(price)) {
            return String.valueOf((int) price);
        }
        String s = String.format("%.2f", price);
        // Remove a single trailing zero (e.g. "78.50" -> "78.5") but keep "78.57" as-is
        if (s.endsWith("0")) {
            s = s.substring(0, s.length() - 1);
        }
        return s;
    }
}
