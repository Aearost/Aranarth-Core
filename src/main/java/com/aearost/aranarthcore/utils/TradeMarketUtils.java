package com.aearost.aranarthcore.utils;

import com.aearost.aranarthcore.AranarthCore;
import com.aearost.aranarthcore.database.DatabaseManager;
import com.aearost.aranarthcore.objects.MarketDynamics;
import com.aearost.aranarthcore.objects.Shop;
import com.aearost.aranarthcore.objects.TradeMarketData;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.Damageable;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionType;

import java.util.*;

/**
 * Manages trade-derived market pricing and provides a unified view of all market prices.
 */
public class TradeMarketUtils {

    private static final Map<String, TradeMarketData> tradeMarketData = new HashMap<>();

    public record MarketEntry(String key, String displayName, ItemStack displayItem,
                              double basePrice, double currentPrice, double tradePrice, boolean fromServerShop) {}

    public static Map<String, TradeMarketData> getAllTradeMarketData() {
        return tradeMarketData;
    }

    public static void addTradeMarketData(TradeMarketData data) {
        tradeMarketData.put(data.getMarketKey(), data);
    }

    /**
     * Returns a stable, unique string key representing the identity of an item,
     * independent of enchantments and durability.
     */
    public static String getMarketKey(ItemStack item) {
        if (item == null || item.getType() == Material.AIR) return null;
        ItemMeta meta = item.getItemMeta();

        // Custom items are identified by their item model NamespacedKey
        if (meta != null && meta.hasItemModel()) {
            return meta.getItemModel().toString();
        }

        // Potions must match all effects
        if (meta instanceof PotionMeta pm) {
            return buildPotionKey(item.getType(), pm);
        }

        // Damaged tools/armour are excluded from market tracking
        if (meta instanceof Damageable d && d.getDamage() > 0) {
            return null;
        }

        return item.getType().name();
    }

    private static String buildPotionKey(Material mat, PotionMeta pm) {
        StringBuilder sb = new StringBuilder(mat.name()).append(":");
        PotionType baseType = pm.getBasePotionType();
        if (baseType != null) {
            sb.append(baseType.name()).append(":");
        }
        List<PotionEffect> effects = new ArrayList<>(pm.getCustomEffects());
        effects.sort(Comparator.comparing(e -> e.getType().getKey().toString()));
        for (PotionEffect e : effects) {
            sb.append(e.getType().getKey()).append(",")
              .append(e.getAmplifier()).append(",")
              .append(e.getDuration()).append(";");
        }
        return sb.toString();
    }

    /**
     * Returns the display name for an item, preserving color codes (§-coded).
     */
    public static String getDisplayName(ItemStack item) {
        if (item == null) return "Unknown";
        ItemMeta meta = item.getItemMeta();
        if (meta != null && meta.hasDisplayName()) {
            return meta.getDisplayName(); // Already color-coded, colors intact
        }
        String[] words = item.getType().name().toLowerCase().split("_");
        StringBuilder sb = new StringBuilder();
        for (String word : words) {
            if (!word.isEmpty()) {
                sb.append(Character.toUpperCase(word.charAt(0))).append(word.substring(1)).append(' ');
            }
        }
        return sb.toString().trim();
    }

    /**
     * Returns the current known per-unit price for the given market key.
     */
    public static Double getKnownUnitPrice(String marketKey, ItemStack sample) {
        // Server shops (most authoritative)
        List<Shop> serverShops = ShopUtils.getShops().get(null);
        if (serverShops != null) {
            for (Shop shop : serverShops) {
                if (shop.getItem() == null || shop.getSellPrice() <= 0) continue;
                String shopKey = getMarketKey(shop.getItem());
                if (marketKey.equals(shopKey)) {
                    int qty = Math.max(1, shop.getQuantity());
                    return shop.getSellPrice() / qty;
                }
            }
        }
        // Trade market data
        TradeMarketData data = tradeMarketData.get(marketKey);
        if (data != null && data.getTotalUnits() > 0) {
            return data.getCurrentPrice();
        }
        return null;
    }

    /**
     * Records a completed "money for items" trade into the market price history.
     * @param items       The items given in the trade (the "items" side).
     * @param totalMoney  The total money paid for those items.
     */
    public static void recordTradeTransaction(ItemStack[] items, double totalMoney) {
        if (totalMoney <= 0 || items == null) return;

        // Aggregate items by market key
        Map<String, Long> countByKey = new LinkedHashMap<>();
        Map<String, ItemStack> sampleByKey = new LinkedHashMap<>();
        for (ItemStack item : items) {
            if (item == null || item.getType() == Material.AIR) continue;
            String key = getMarketKey(item);
            if (key == null) continue; // damaged - skip
            countByKey.merge(key, (long) item.getAmount(), Long::sum);
            sampleByKey.putIfAbsent(key, item);
        }
        if (countByKey.isEmpty()) return;

        // Allocate money proportionally by market value
        Map<String, Double> allocation = allocateMoney(countByKey, sampleByKey, totalMoney);

        // Update in-memory data and persist async
        for (Map.Entry<String, Double> entry : allocation.entrySet()) {
            String key = entry.getKey();
            double money = entry.getValue();
            long units = countByKey.get(key);
            if (units <= 0 || money <= 0) continue;

            ItemStack sample = sampleByKey.get(key);
            String displayName = getDisplayName(sample);
            String material = sample.getType().name();
            String itemModelKey = null;
            if (sample.getItemMeta() != null && sample.getItemMeta().hasItemModel()) {
                itemModelKey = sample.getItemMeta().getItemModel().toString();
            }

            TradeMarketData existing = tradeMarketData.get(key);
            if (existing == null) {
                existing = new TradeMarketData(key, displayName, material, itemModelKey, 0, 0, money / units);
                tradeMarketData.put(key, existing);
            }
            existing.record(money, units);

            if (DatabaseManager.isActive()) {
                final TradeMarketData toSave = existing;
                Bukkit.getScheduler().runTaskAsynchronously(AranarthCore.getInstance(),
                        () -> DatabaseManager.getInstance().upsertTradeMarketData(toSave));
            }
        }
    }

    private static Map<String, Double> allocateMoney(Map<String, Long> countByKey,
                                                      Map<String, ItemStack> sampleByKey,
                                                      double totalMoney) {
        // Get known unit prices for each type
        Map<String, Double> knownPrices = new HashMap<>();
        for (String key : countByKey.keySet()) {
            Double p = getKnownUnitPrice(key, sampleByKey.get(key));
            if (p != null && p > 0) {
                knownPrices.put(key, p);
            }
        }

        // Compute a fallback unit price (average of known, or 1.0)
        double fallbackUnitPrice = knownPrices.values().stream()
                .mapToDouble(d -> d).average().orElse(1.0);

        Map<String, Double> weights = new LinkedHashMap<>();
        double totalWeight = 0;
        for (Map.Entry<String, Long> e : countByKey.entrySet()) {
            double unitPrice = knownPrices.getOrDefault(e.getKey(), fallbackUnitPrice);
            double w = e.getValue() * unitPrice;
            weights.put(e.getKey(), w);
            totalWeight += w;
        }

        // Proportional allocation
        Map<String, Double> allocation = new LinkedHashMap<>();
        for (Map.Entry<String, Double> e : weights.entrySet()) {
            allocation.put(e.getKey(),
                    totalWeight > 0 ? totalMoney * e.getValue() / totalWeight : totalMoney / countByKey.size());
        }
        return allocation;
    }

    /**
     * Returns a sorted list of all market entries (server shops + trade-derived)
     * suitable for display in the market price GUI.
     */
    public static List<MarketEntry> getAllEntries() {
        List<MarketEntry> entries = new ArrayList<>();
        Set<String> addedKeys = new HashSet<>();

        // Server shop entries
        List<Shop> serverShops = ShopUtils.getShops().get(null);
        if (serverShops != null) {
            for (Shop shop : serverShops) {
                if (shop.getItem() == null || shop.getSellPrice() <= 0) continue;
                String key = getMarketKey(shop.getItem());
                if (key == null || addedKeys.contains(key)) continue;
                addedKeys.add(key);

                String shopKey = MarketUtils.getShopKey(shop);
                MarketDynamics dynamics = MarketUtils.getMarketData(shopKey);
                int qty = Math.max(1, shop.getQuantity());
                double baseUnitPrice = dynamics != null
                        ? dynamics.getDefaultSellPrice() / qty
                        : shop.getSellPrice() / qty;
                double currentUnitPrice = shop.getSellPrice() / qty;
                String displayName = getDisplayName(shop.getItem());
                TradeMarketData tradeData = tradeMarketData.get(key);
                double tradeUnitPrice = (tradeData != null && tradeData.getTotalUnits() > 0)
                        ? tradeData.getCurrentPrice() : 0;
                entries.add(new MarketEntry(key, displayName, shop.getItem().clone(),
                        baseUnitPrice, currentUnitPrice, tradeUnitPrice, true));
            }
        }

        // Trade-derived entries not covered by server shops
        for (TradeMarketData data : tradeMarketData.values()) {
            if (addedKeys.contains(data.getMarketKey())) continue;
            entries.add(new MarketEntry(data.getMarketKey(), data.getDisplayName(),
                    buildDisplayItem(data), 0, data.getCurrentPrice(), 0, false));
        }

        entries.sort(Comparator.comparing(e -> ChatUtils.stripColorFormatting(e.displayName()).toLowerCase()));
        return entries;
    }

    /**
     * Reconstructs a display ItemStack from a TradeMarketData entry.
     */
    public static ItemStack buildDisplayItem(TradeMarketData data) {
        Material mat;
        try {
            mat = Material.valueOf(data.getMaterial());
        } catch (IllegalArgumentException e) {
            mat = Material.PAPER;
        }
        ItemStack item = new ItemStack(mat);
        if (data.getItemModelKey() != null || !data.getDisplayName().isEmpty()) {
            ItemMeta meta = item.getItemMeta();
            if (meta != null) {
                if (data.getItemModelKey() != null) {
                    NamespacedKey nk = NamespacedKey.fromString(data.getItemModelKey());
                    if (nk != null) meta.setItemModel(nk);
                }
                meta.setDisplayName(data.getDisplayName()); // §-coded, colors already applied
                item.setItemMeta(meta);
            }
        }
        return item;
    }

    /**
     * Formats a price as a currency string with always exactly 2 decimal places.
     */
    public static String formatPrice(double price) {
        return String.format("%.2f", price);
    }
}
