package com.aearost.aranarthcore.utils;

import org.bukkit.NamespacedKey;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.Damageable;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import static com.aearost.aranarthcore.objects.CustomKeys.*;

/**
 * Central registry of every custom Aranarth item and where it is allowed to be used.
 */
public class CustomItemUtils {

    public enum Station {
        CRAFTING, ANVIL, GRINDSTONE, SMITHING, CARTOGRAPHY, LOOM, STONECUTTER, FURNACE, BREWING
    }

    private static final Map<NamespacedKey, Set<Station>> USAGES = new LinkedHashMap<>();

    static {
        // Aranarthium armour can be enchanted, renamed, repaired and disenchanted
        USAGES.put(ARMOR_TYPE, EnumSet.of(Station.ANVIL, Station.GRINDSTONE));
        // Netherite Elytra are forged into Aranarthium Elytra in an anvil
        USAGES.put(NETHERITE_ITEM, EnumSet.of(Station.ANVIL, Station.GRINDSTONE));
        // Wrenches may receive Mending and Unbreaking in an anvil
        USAGES.put(WRENCH, EnumSet.of(Station.ANVIL, Station.GRINDSTONE));
        // Enhanced ingot recipes, forging Aranarthium armour, and crushing into Essences with a Mace
        USAGES.put(ARANARTHIUM_INGOT, EnumSet.of(Station.CRAFTING, Station.ANVIL));
        // Aranarthium Ingot and Essence recipes
        USAGES.put(CLUSTER, EnumSet.of(Station.CRAFTING));
        // Homepad recipe
        USAGES.put(CHORUS_DIAMOND, EnumSet.of(Station.CRAFTING));
        // Enchanted Golden Apple recipe
        USAGES.put(GOD_APPLE_FRAGMENT, EnumSet.of(Station.CRAFTING));
        // Uncrafting back into Sugarcane
        USAGES.put(SUGARCANE_BLOCK, EnumSet.of(Station.CRAFTING));
        // Blank Essences are used in Essence recipes
        USAGES.put(ESSENCE_IMBUED_XP, EnumSet.of(Station.CRAFTING));
        // Imbued Essences (gear that an Essence was applied to is not considered an Essence item)
        USAGES.put(ESSENCE_TYPE, EnumSet.noneOf(Station.class));
        USAGES.put(HOMEPAD, EnumSet.noneOf(Station.class));
        USAGES.put(HONEY_GLAZED_HAM, EnumSet.noneOf(Station.class));
        // Arrows and arrowheads are only made in the custom Fletching Table
        USAGES.put(ARROW, EnumSet.noneOf(Station.class));
        USAGES.put(ARROW_HEAD, EnumSet.noneOf(Station.class));
        USAGES.put(QUIVER, EnumSet.noneOf(Station.class));
        USAGES.put(CRATE_KEY, EnumSet.noneOf(Station.class));
        USAGES.put(COUPON_DISCOUNT, EnumSet.noneOf(Station.class));
        USAGES.put(HEAD_TEXTURE, EnumSet.noneOf(Station.class));
        USAGES.put(BREW_RECIPE, EnumSet.noneOf(Station.class));
    }

    /**
     * Provides the identity key of the custom item.
     * @param item The item.
     * @return The identity key, or null if the item is not a custom Aranarth item.
     */
    public static NamespacedKey getIdentityKey(ItemStack item) {
        if (item == null || !item.hasItemMeta()) {
            return null;
        }
        PersistentDataContainer pdc = item.getItemMeta().getPersistentDataContainer();
        for (NamespacedKey key : USAGES.keySet()) {
            if (!pdc.has(key)) {
                continue;
            }
            // Gear that an Essence was applied to carries the Essence type, but is not an Essence itself
            if (key.equals(ESSENCE_TYPE) && getMaxDurability(item) > 0) {
                continue;
            }
            return key;
        }
        return null;
    }

    /**
     * Provides a value uniquely identifying the type of custom item, used to compare if two items are the same custom item.
     * @param item The item.
     * @return The identity, or null if the item is not a custom Aranarth item.
     */
    public static String getIdentity(ItemStack item) {
        NamespacedKey key = getIdentityKey(item);
        if (key == null) {
            return null;
        }
        PersistentDataContainer pdc = item.getItemMeta().getPersistentDataContainer();
        String value = pdc.has(key, PersistentDataType.STRING) ? pdc.get(key, PersistentDataType.STRING) : "";
        return key.getKey() + ":" + value + ":" + item.getType().name();
    }

    /**
     * Determines if the item is a custom Aranarth item.
     * @param item The item.
     * @return Confirmation of whether the item is a custom Aranarth item.
     */
    public static boolean isCustomItem(ItemStack item) {
        return getIdentityKey(item) != null;
    }

    /**
     * Determines if the item may be used in the station. Vanilla items are always allowed.
     * @param item The item.
     * @param station The station.
     * @return Confirmation of whether the item may be used in the station.
     */
    public static boolean isAllowed(ItemStack item, Station station) {
        NamespacedKey key = getIdentityKey(item);
        return key == null || USAGES.get(key).contains(station);
    }

    /**
     * Determines if any of the items are custom Aranarth items that may not be used in the station.
     * @param items The items.
     * @param station The station.
     * @return Confirmation of whether any of the items are disallowed.
     */
    public static boolean containsDisallowed(ItemStack[] items, Station station) {
        for (ItemStack item : items) {
            if (!isAllowed(item, station)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Determines if both items are the same type of custom Aranarth item.
     * @param first The first item.
     * @param second The second item.
     * @return Confirmation of whether both items are the same custom item.
     */
    public static boolean isSameCustomItem(ItemStack first, ItemStack second) {
        String identity = getIdentity(first);
        return identity != null && identity.equals(getIdentity(second));
    }

    /**
     * Provides the max durability of the item, accounting for custom max durability.
     * @param item The item.
     * @return The max durability, or 0 if the item has no durability.
     */
    public static int getMaxDurability(ItemStack item) {
        if (item == null) {
            return 0;
        }
        if (item.hasItemMeta() && item.getItemMeta() instanceof Damageable damageable && damageable.hasMaxDamage()) {
            return damageable.getMaxDamage();
        }
        return item.getType().getMaxDurability();
    }

    /**
     * Creates the result of repairing two of the same custom item together, following the vanilla repair formula.
     * The custom properties of the first item are kept, enchantments are only kept at the level both items share,
     * and curses from either item are kept.
     * @param first The first item.
     * @param second The second item.
     * @return The repaired item, or null if the items cannot be repaired together.
     */
    public static ItemStack createRepairResult(ItemStack first, ItemStack second) {
        if (!isSameCustomItem(first, second)) {
            return null;
        }
        int maxDurability = getMaxDurability(first);
        if (maxDurability <= 0 || maxDurability != getMaxDurability(second)) {
            return null;
        }
        ItemStack result = first.clone();
        result.setAmount(1);
        ItemMeta firstMeta = first.getItemMeta();
        ItemMeta secondMeta = second.getItemMeta();
        ItemMeta resultMeta = result.getItemMeta();
        if (!(resultMeta instanceof Damageable damageable)) {
            return null;
        }

        int firstRemaining = maxDurability - ((Damageable) firstMeta).getDamage();
        int secondRemaining = maxDurability - ((Damageable) secondMeta).getDamage();
        int remaining = firstRemaining + secondRemaining + maxDurability * 5 / 100;
        damageable.setDamage(Math.max(0, maxDurability - remaining));

        for (Enchantment enchantment : firstMeta.getEnchants().keySet()) {
            resultMeta.removeEnchant(enchantment);
        }
        for (Map.Entry<Enchantment, Integer> entry : firstMeta.getEnchants().entrySet()) {
            Enchantment enchantment = entry.getKey();
            int secondLevel = secondMeta.getEnchantLevel(enchantment);
            if (enchantment.isCursed()) {
                resultMeta.addEnchant(enchantment, Math.max(entry.getValue(), secondLevel), true);
            } else if (secondLevel > 0) {
                resultMeta.addEnchant(enchantment, Math.min(entry.getValue(), secondLevel), true);
            }
        }
        for (Map.Entry<Enchantment, Integer> entry : secondMeta.getEnchants().entrySet()) {
            if (entry.getKey().isCursed() && !resultMeta.hasEnchant(entry.getKey())) {
                resultMeta.addEnchant(entry.getKey(), entry.getValue(), true);
            }
        }
        result.setItemMeta(resultMeta);
        return result;
    }
}
