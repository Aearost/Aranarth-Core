package com.aearost.aranarthcore.event.listener;

import com.aearost.aranarthcore.AranarthCore;
import org.bukkit.Bukkit;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.PrepareAnvilEvent;
import org.bukkit.event.player.PlayerExpChangeEvent;
import org.bukkit.inventory.AnvilInventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.Damageable;
import org.bukkit.inventory.meta.EnchantmentStorageMeta;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.List;
import java.util.Map;

import static com.aearost.aranarthcore.objects.CustomKeys.*;

/**
 * Handles Mending and Unbreaking enchantment support for the Wrench.
 */
public class WrenchEnchantListener implements Listener {

    private static final List<Enchantment> ALLOWED = List.of(
            Enchantment.UNBREAKING,
            Enchantment.MENDING
    );

    public WrenchEnchantListener(AranarthCore plugin) {
        Bukkit.getPluginManager().registerEvents(this, plugin);
    }

    @EventHandler
    public void onPrepareAnvil(PrepareAnvilEvent e) {
        AnvilInventory anvil = e.getInventory();
        ItemStack left = anvil.getItem(0);
        ItemStack right = anvil.getItem(1);

        if (!isWrench(left) || right == null || !right.hasItemMeta()) {
            return;
        }
        if (!(right.getItemMeta() instanceof EnchantmentStorageMeta bookMeta)) {
            return;
        }

        Map<Enchantment, Integer> stored = bookMeta.getStoredEnchants();
        boolean hasAllowed = stored.keySet().stream().anyMatch(ALLOWED::contains);
        if (!hasAllowed) {
            return;
        }

        // Build the output wrench with the allowed enchantments merged in
        ItemStack result = left.clone();
        ItemMeta resultMeta = result.getItemMeta();

        int xpCost = 0;
        for (Map.Entry<Enchantment, Integer> entry : stored.entrySet()) {
            Enchantment ench = entry.getKey();
            int bookLevel = entry.getValue();
            if (!ALLOWED.contains(ench)) {
                continue;
            }

            int current = resultMeta.getEnchantLevel(ench);
            int newLevel;
            if (current == bookLevel) {
                // Combine up to max
                newLevel = Math.min(bookLevel + 1, ench.getMaxLevel());
            } else {
                newLevel = Math.max(current, bookLevel);
            }
            resultMeta.addEnchant(ench, newLevel, true);
            xpCost += newLevel;
        }

        result.setItemMeta(resultMeta);
        e.setResult(result);
        e.getView().setRepairCost(xpCost);
    }

    @EventHandler
    public void onPlayerExpChange(PlayerExpChangeEvent e) {
        int gained = e.getAmount();
        if (gained <= 0) {
            return;
        }

        Player player = e.getPlayer();
        ItemStack wrench = findMendingWrench(player);
        if (wrench == null) {
            return;
        }

        ItemMeta meta = wrench.getItemMeta();
        if (!(meta instanceof Damageable damageable)) {
            return;
        }

        int damage = damageable.getDamage();
        if (damage <= 0) {
            return;
        }

        // 2 durability repaired per 1 XP (vanilla Mending rate)
        int xpNeeded = (int) Math.ceil(damage / 2.0);
        int xpUsed = Math.min(gained, xpNeeded);
        damageable.setDamage(Math.max(0, damage - xpUsed * 2));
        wrench.setItemMeta(meta);

        e.setAmount(gained - xpUsed);
    }

    private static boolean isWrench(ItemStack item) {
        if (item == null || !item.hasItemMeta()) {
            return false;
        }
        return item.getItemMeta().getPersistentDataContainer().has(WRENCH, PersistentDataType.STRING);
    }

    /**
     * Returns the wrench in the player's main or off hand that has Mending and has taken damage.
     */
    private static ItemStack findMendingWrench(Player player) {
        for (ItemStack candidate : List.of(
                player.getInventory().getItemInMainHand(),
                player.getInventory().getItemInOffHand())) {
            if (!isWrench(candidate)) {
                continue;
            }
            ItemMeta m = candidate.getItemMeta();
            if (m.getEnchantLevel(Enchantment.MENDING) < 1) {
                continue;
            }
            if (m instanceof Damageable d && d.getDamage() > 0) {
                return candidate;
            }
        }
        return null;
    }
}
