package com.aearost.aranarthcore.event.listener;

import com.aearost.aranarthcore.AranarthCore;
import org.bukkit.Bukkit;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.PrepareAnvilEvent;
import org.bukkit.inventory.AnvilInventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.EnchantmentStorageMeta;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.HashMap;
import java.util.Map;

/**
 * Corrects anvil results when over-leveled enchantments are applied.
 */
public class OverLeveledAnvilListener implements Listener {

    // Custom max levels that match WanderingTraderUtils.OVER_LEVELED_ENCHANTS
    private static final Map<Enchantment, Integer> CUSTOM_MAX = new HashMap<>();

    static {
        CUSTOM_MAX.put(Enchantment.SHARPNESS, 6);
        CUSTOM_MAX.put(Enchantment.EFFICIENCY, 6);
        CUSTOM_MAX.put(Enchantment.FORTUNE, 4);
        CUSTOM_MAX.put(Enchantment.PROTECTION, 5);
        CUSTOM_MAX.put(Enchantment.FEATHER_FALLING, 5);
        CUSTOM_MAX.put(Enchantment.LOOTING, 4);
        CUSTOM_MAX.put(Enchantment.FIRE_ASPECT, 3);
        CUSTOM_MAX.put(Enchantment.POWER, 6);
        CUSTOM_MAX.put(Enchantment.UNBREAKING, 4);
        CUSTOM_MAX.put(Enchantment.DEPTH_STRIDER, 4);
        CUSTOM_MAX.put(Enchantment.RESPIRATION, 4);
        CUSTOM_MAX.put(Enchantment.SWIFT_SNEAK, 4);
        CUSTOM_MAX.put(Enchantment.SOUL_SPEED, 4);
        CUSTOM_MAX.put(Enchantment.LUCK_OF_THE_SEA, 4);
        CUSTOM_MAX.put(Enchantment.LURE, 4);
        CUSTOM_MAX.put(Enchantment.FROST_WALKER, 3);
        CUSTOM_MAX.put(Enchantment.KNOCKBACK, 3);
        CUSTOM_MAX.put(Enchantment.PUNCH, 3);
    }

    public OverLeveledAnvilListener(AranarthCore plugin) {
        Bukkit.getPluginManager().registerEvents(this, plugin);
    }

    @EventHandler
    public void onPrepareAnvil(PrepareAnvilEvent e) {
        AnvilInventory inv = e.getInventory();
        ItemStack left = inv.getItem(0);
        ItemStack right = inv.getItem(1);
        ItemStack result = e.getResult();

        if (left == null || right == null || result == null) return;

        Map<Enchantment, Integer> leftEnchants = getEnchants(left);
        Map<Enchantment, Integer> rightEnchants = getEnchants(right);

        // Skip if neither input has any over-leveled enchant
        if (!hasOverLeveled(leftEnchants) && !hasOverLeveled(rightEnchants)) return;

        // Compute expected combined levels (mirrors vanilla combine logic, without the cap)
        Map<Enchantment, Integer> expected = new HashMap<>(leftEnchants);
        for (Map.Entry<Enchantment, Integer> entry : rightEnchants.entrySet()) {
            Enchantment ench = entry.getKey();
            int rightLevel = entry.getValue();
            int leftLevel = expected.getOrDefault(ench, 0);

            int combined;
            if (leftLevel == rightLevel) {
                combined = leftLevel + 1;
            } else {
                combined = Math.max(leftLevel, rightLevel);
            }
            // Cap at our custom max (or vanilla max for enchants not in our list)
            int max = CUSTOM_MAX.getOrDefault(ench, ench.getMaxLevel());
            expected.put(ench, Math.min(combined, max));
        }

        // Apply corrections for any enchant whose expected level exceeds vanilla max
        ItemStack fixed = result.clone();
        ItemMeta meta = fixed.getItemMeta();

        boolean changed = false;
        for (Map.Entry<Enchantment, Integer> entry : expected.entrySet()) {
            Enchantment ench = entry.getKey();
            int expectedLevel = entry.getValue();
            if (expectedLevel <= ench.getMaxLevel()) continue;

            if (meta instanceof EnchantmentStorageMeta bookMeta) {
                bookMeta.addStoredEnchant(ench, expectedLevel, true);
            } else {
                meta.addEnchant(ench, expectedLevel, true);
            }
            changed = true;
        }

        if (changed) {
            fixed.setItemMeta(meta);
            e.setResult(fixed);
        }
    }

    private Map<Enchantment, Integer> getEnchants(ItemStack item) {
        if (!item.hasItemMeta()) return new HashMap<>();
        ItemMeta meta = item.getItemMeta();
        if (meta instanceof EnchantmentStorageMeta bookMeta) {
            return new HashMap<>(bookMeta.getStoredEnchants());
        }
        return new HashMap<>(meta.getEnchants());
    }

    private boolean hasOverLeveled(Map<Enchantment, Integer> enchants) {
        for (Map.Entry<Enchantment, Integer> entry : enchants.entrySet()) {
            if (entry.getValue() > entry.getKey().getMaxLevel()) return true;
        }
        return false;
    }
}
