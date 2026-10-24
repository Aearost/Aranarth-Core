package com.aearost.aranarthcore.event.listener;

import com.aearost.aranarthcore.AranarthCore;
import com.aearost.aranarthcore.utils.EnchantingUtils;
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
            // Equal levels only combine upwards below the vanilla max, so over-leveled enchants can only come from their source
            if (leftLevel == rightLevel && leftLevel < ench.getMaxLevel()) {
                combined = leftLevel + 1;
            } else {
                combined = Math.max(leftLevel, rightLevel);
            }
            // Cap at the over-leveled max, without ever lowering a level that is already on an input
            int max = Math.max(EnchantingUtils.getOverLeveledMax(ench), Math.max(leftLevel, rightLevel));
            expected.put(ench, Math.min(combined, max));
        }

        // Apply corrections for any enchant whose expected level exceeds vanilla max
        // Enchants missing from the result were rejected by vanilla, such as conflicting or unsupported enchants
        Map<Enchantment, Integer> resultEnchants = getEnchants(result);
        ItemStack fixed = result.clone();
        ItemMeta meta = fixed.getItemMeta();

        boolean changed = false;
        for (Map.Entry<Enchantment, Integer> entry : expected.entrySet()) {
            Enchantment ench = entry.getKey();
            int expectedLevel = entry.getValue();
            if (expectedLevel <= ench.getMaxLevel() || !resultEnchants.containsKey(ench)) continue;

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
