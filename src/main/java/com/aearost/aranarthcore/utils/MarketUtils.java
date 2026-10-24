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

    // Pressure is measured in full inventories (36 slots * the item's max stack size)
    private static final int INVENTORY_SLOTS = 36;

    // Price floor and ceiling relative to default
    private static final double PRICE_FLOOR_MODIFIER = 0.25;
    private static final double PRICE_CEILING_MODIFIER = 2.5;

    // Per-tick constants
    private static final double DECAY_RATE = 0.95;
    private static final double SENSITIVITY = 0.000691;
    // Immediate drop from sales made since the last tick, applied as price * e^(-SALE_IMPACT * inventoriesSold)
    // 1 inventory drops ~5%, 1 inventory of full shulker boxes (27 inventories) drops ~74%
    private static final double SALE_IMPACT = 0.05;
    // Price rises from 0.25x to 1.0x in ~3.5 days, and from 1.0x to the 2.5x ceiling in ~2.3 days
    private static final double RISE_RATE = 0.01664;
    private static final double PRESSURE_THRESHOLD = 0.01;
    private static final double CHANGE_THRESHOLD = 0.005;
    // A server shop never pays more than this share of the cheapest server shop buy price for the same item
    private static final double MAX_SELL_TO_BUY_RATIO = 0.9;

    /**
     * Whether a server shop's sell price is currently sitting at its floor or ceiling.
     */
    public enum PriceLimit { NONE, MAXIMUM, MINIMUM }

    /**
     * Returns whether the given server shop's sell price is at its ceiling, its floor, or neither.
     * A price within the change threshold of a limit is considered to be at that limit,
     * since the price tick will not move it any closer.
     */
    public static PriceLimit getPriceLimit(Shop shop) {
        MarketDynamics data = marketData.get(getShopKey(shop));
        if (data == null || data.getDefaultSellPrice() <= 0 || shop.getSellPrice() <= 0) {
            return PriceLimit.NONE;
        }
        double sellPrice = shop.getSellPrice();
        double defaultPrice = data.getDefaultSellPrice();
        if (defaultPrice * PRICE_CEILING_MODIFIER - sellPrice <= CHANGE_THRESHOLD) {
            return PriceLimit.MAXIMUM;
        } else if (sellPrice - defaultPrice * PRICE_FLOOR_MODIFIER <= CHANGE_THRESHOLD) {
            return PriceLimit.MINIMUM;
        }
        return PriceLimit.NONE;
    }

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
        MarketDynamics data = marketData.get(key);
        if (data == null) {
            return;
        }
        double delta = quantitySold / (double) (INVENTORY_SLOTS * shop.getItem().getMaxStackSize());

        // Lower the price right away instead of at the next tick, so a burst of sales cannot all get the high price
        Shop liveShop = ShopUtils.getShopFromLocation(shop.getLocation());
        if (liveShop != null && liveShop.getUuid() == null && liveShop.getSellPrice() > 0 && data.getDefaultSellPrice() > 0) {
            double floor = data.getDefaultSellPrice() * PRICE_FLOOR_MODIFIER;
            double dropped = Math.max(floor, liveShop.getSellPrice() * Math.exp(-SALE_IMPACT * delta));
            dropped = Math.round(clampToBuyPrices(liveShop, dropped) * 100.0) / 100.0;
            data.setAppliedPressure(data.getAppliedPressure() + delta);
            if (dropped != liveShop.getSellPrice()) {
                liveShop.setSellPrice(dropped);
                data.setCurrentPriceModifier(dropped / data.getDefaultSellPrice());
                refreshServerShopSign(liveShop);
            }
        }
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
            shop.setSellPrice(Math.round(clampToBuyPrices(shop, adminTypedSellPrice) * 100.0) / 100.0);
        } else {
            double modifier = existing.getCurrentPriceModifier();
            existing.setDefaultSellPrice(adminTypedSellPrice);
            double newPrice = Math.round(clampToBuyPrices(shop, adminTypedSellPrice * modifier) * 100.0) / 100.0;
            shop.setSellPrice(newPrice);
        }
        // The sign is written by the sign change event, so it is corrected once that has finished
        Bukkit.getScheduler().runTask(AranarthCore.getInstance(), () -> refreshServerShopSign(shop));
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
                    // Sales made since the last tick hit the price all at once instead of trickling in
                    double newSales = Math.max(0.0, combinedPressure - data.getAppliedPressure());
                    double newPressure = combinedPressure * DECAY_RATE;
                    data.setSellPressure(newPressure);
                    data.setAppliedPressure(newPressure);

                    double newPrice;
                    if (newPressure > PRESSURE_THRESHOLD) {
                        // Sell pressure is driving the price down
                        double dropRate = newSales * SALE_IMPACT + newPressure * SENSITIVITY;
                        if (currentPrice > defaultPrice) {
                            double excess = currentPrice / defaultPrice - 1.0;
                            dropRate *= 1.0 + excess * 2.0;
                        }
                        newPrice = Math.max(defaultPrice * PRICE_FLOOR_MODIFIER, currentPrice * Math.exp(-dropRate));
                    } else {
                        // No meaningful pressure - price recovers toward ceiling
                        newPrice = Math.min(defaultPrice * PRICE_CEILING_MODIFIER, currentPrice * (1.0 + RISE_RATE));
                    }

                    newPrice = clampToBuyPrices(shop, newPrice);

                    if (Math.abs(newPrice - currentPrice) > CHANGE_THRESHOLD) {
                        double modifier = newPrice / defaultPrice;
                        data.setCurrentPriceModifier(modifier);
                        double rounded = Math.round(newPrice * 100.0) / 100.0;
                        shop.setSellPrice(rounded);
                        refreshServerShopSign(shop);
                    } else if (getPriceLimit(shop) != PriceLimit.NONE) {
                        // Ensures signs that were already at a limit before the indicator existed get colored
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
     * Caps a server shop's sell price so that, per item, it is at most MAX_SELL_TO_BUY_RATIO of the
     * cheapest buy price of any server shop selling the same item (including this one).
     */
    public static double clampToBuyPrices(Shop shop, double sellPrice) {
        List<Shop> serverShops = ShopUtils.getShops().get(null);
        if (serverShops == null || shop.getItem() == null || shop.getQuantity() <= 0) {
            return sellPrice;
        }
        String itemKey = TradeMarketUtils.getMarketKey(shop.getItem());
        if (itemKey == null) {
            return sellPrice;
        }
        double cheapestBuyPerUnit = Double.MAX_VALUE;
        for (Shop other : serverShops) {
            if (other.getBuyPrice() <= 0 || other.getQuantity() <= 0 || other.getItem() == null) {
                continue;
            }
            if (!itemKey.equals(TradeMarketUtils.getMarketKey(other.getItem()))) {
                continue;
            }
            cheapestBuyPerUnit = Math.min(cheapestBuyPerUnit, other.getBuyPrice() / other.getQuantity());
        }
        if (cheapestBuyPerUnit == Double.MAX_VALUE) {
            return sellPrice;
        }
        double maxSellPrice = cheapestBuyPerUnit * MAX_SELL_TO_BUY_RATIO * shop.getQuantity();
        return Math.min(sellPrice, maxSellPrice);
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

        // Colors the sell price green at its maximum and red at its minimum
        PriceLimit limit = getPriceLimit(shop);
        String sellColor = limit == PriceLimit.MAXIMUM ? "&a" : limit == PriceLimit.MINIMUM ? "&c" : "";

        String priceLine;
        if (buyPrice > 0 && sellPrice > 0) {
            priceLine = "&0&lB &r" + formatPrice(buyPrice) + " | &0&lS &r" + sellColor + formatPrice(sellPrice);
        } else if (sellPrice > 0) {
            priceLine = "&0&lS &r" + sellColor + formatPrice(sellPrice);
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
