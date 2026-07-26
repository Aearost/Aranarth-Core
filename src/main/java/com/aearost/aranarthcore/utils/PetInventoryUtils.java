package com.aearost.aranarthcore.utils;

import com.aearost.aranarthcore.AranarthCore;
import com.aearost.aranarthcore.objects.CustomKeys;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Tameable;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

import java.io.IOException;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Manages food inventories for tamed pets (wolves, cats, horses, donkeys, mules, camels).
 */
public class PetInventoryUtils {

    public static final int FOOD_SLOTS = 9;
    public static final String GUI_TITLE = "Pet Food";

    private static final Map<UUID, UUID> openInventories = new HashMap<>();
    private static final Map<Material, Integer> FOOD_NUTRITION = new HashMap<>();

    private static final Set<Material> WOLF_FOOD = EnumSet.of(
            Material.BEEF, Material.COOKED_BEEF,
            Material.PORKCHOP, Material.COOKED_PORKCHOP,
            Material.MUTTON, Material.COOKED_MUTTON,
            Material.CHICKEN, Material.COOKED_CHICKEN,
            Material.RABBIT, Material.COOKED_RABBIT,
            Material.COD, Material.COOKED_COD,
            Material.SALMON, Material.COOKED_SALMON,
            Material.TROPICAL_FISH,
            Material.ROTTEN_FLESH
    );

    private static final Set<Material> CAT_FOOD = EnumSet.of(
            Material.COD, Material.COOKED_COD,
            Material.SALMON, Material.COOKED_SALMON,
            Material.TROPICAL_FISH,
            Material.PUFFERFISH
    );

    // Horses, donkeys, and mules share the same allowed foods
    private static final Set<Material> HORSE_FOOD = EnumSet.of(
            Material.SUGAR,
            Material.WHEAT,
            Material.APPLE,
            Material.GOLDEN_CARROT,
            Material.GOLDEN_APPLE,
            Material.ENCHANTED_GOLDEN_APPLE,
            Material.HAY_BLOCK
    );

    private static final Set<Material> CAMEL_FOOD = EnumSet.of(
            Material.CACTUS
    );

    static {
        // Wolf / general meat foods
        FOOD_NUTRITION.put(Material.BEEF, 3);
        FOOD_NUTRITION.put(Material.COOKED_BEEF, 8);
        FOOD_NUTRITION.put(Material.PORKCHOP, 3);
        FOOD_NUTRITION.put(Material.COOKED_PORKCHOP, 8);
        FOOD_NUTRITION.put(Material.MUTTON, 2);
        FOOD_NUTRITION.put(Material.COOKED_MUTTON, 6);
        FOOD_NUTRITION.put(Material.CHICKEN, 1);
        FOOD_NUTRITION.put(Material.COOKED_CHICKEN, 6);
        FOOD_NUTRITION.put(Material.RABBIT, 3);
        FOOD_NUTRITION.put(Material.COOKED_RABBIT, 5);
        FOOD_NUTRITION.put(Material.COD, 2);
        FOOD_NUTRITION.put(Material.COOKED_COD, 5);
        FOOD_NUTRITION.put(Material.SALMON, 2);
        FOOD_NUTRITION.put(Material.COOKED_SALMON, 6);
        FOOD_NUTRITION.put(Material.TROPICAL_FISH, 1);
        FOOD_NUTRITION.put(Material.PUFFERFISH, 1);
        FOOD_NUTRITION.put(Material.ROTTEN_FLESH, 4);
        // Horse / donkey / mule foods
        FOOD_NUTRITION.put(Material.SUGAR, 1);
        FOOD_NUTRITION.put(Material.WHEAT, 2);
        FOOD_NUTRITION.put(Material.APPLE, 4);
        FOOD_NUTRITION.put(Material.GOLDEN_CARROT, 6);
        FOOD_NUTRITION.put(Material.GOLDEN_APPLE, 4);
        FOOD_NUTRITION.put(Material.ENCHANTED_GOLDEN_APPLE, 4);
        FOOD_NUTRITION.put(Material.HAY_BLOCK, 18);
        // Camel foods
        FOOD_NUTRITION.put(Material.CACTUS, 2);
    }

    private PetInventoryUtils() {}

    public static boolean isFoodItem(Material material) {
        return FOOD_NUTRITION.containsKey(material);
    }

    /**
     * Returns the set of food materials allowed for the given pet entity.
     */
    public static Set<Material> getAllowedFood(Entity entity) {
        if (entity instanceof org.bukkit.entity.Wolf) return WOLF_FOOD;
        if (entity instanceof org.bukkit.entity.Cat) return CAT_FOOD;
        if (entity instanceof org.bukkit.entity.Horse
                || entity instanceof org.bukkit.entity.Donkey
                || entity instanceof org.bukkit.entity.Mule) return HORSE_FOOD;
        if (entity instanceof org.bukkit.entity.Camel) return CAMEL_FOOD;
        return EnumSet.noneOf(Material.class);
    }

    /**
     * Returns true if the given material is food that the specified pet entity can eat.
     */
    public static boolean isAllowedFood(Entity entity, Material material) {
        return getAllowedFood(entity).contains(material);
    }

    /**
     * Returns true if the entity is a pet type that supports a food inventory.
     */
    public static boolean isPetType(Entity entity) {
        return entity instanceof org.bukkit.entity.Wolf
                || entity instanceof org.bukkit.entity.Cat
                || entity instanceof org.bukkit.entity.Horse
                || entity instanceof org.bukkit.entity.Donkey
                || entity instanceof org.bukkit.entity.Mule
                || entity instanceof org.bukkit.entity.Camel;
    }

    /**
     * Returns true if the entity is tamed and owned by the given player.
     */
    public static boolean isOwnedBy(Entity entity, UUID playerUUID) {
        if (entity instanceof Tameable tameable) {
            return tameable.isTamed() && playerUUID.equals(tameable.getOwnerUniqueId());
        }
        return false;
    }

    public static void trackOpen(UUID playerUUID, UUID petEntityUUID) {
        openInventories.put(playerUUID, petEntityUUID);
    }

    public static UUID getOpenPet(UUID playerUUID) {
        return openInventories.get(playerUUID);
    }

    public static void clearOpen(UUID playerUUID) {
        openInventories.remove(playerUUID);
    }

    /**
     * Loads the food inventory from the pet entity's PDC.
     */
    public static ItemStack[] getFoodItems(Entity entity) {
        PersistentDataContainer pdc = entity.getPersistentDataContainer();
        if (!pdc.has(CustomKeys.PET_FOOD_INVENTORY, PersistentDataType.STRING)) {
            return new ItemStack[FOOD_SLOTS];
        }
        String data = pdc.get(CustomKeys.PET_FOOD_INVENTORY, PersistentDataType.STRING);
        try {
            return ItemUtils.itemStackArrayFromBase64(data);
        } catch (IOException e) {
            Bukkit.getLogger().warning(AranarthCore.LOG_PREFIX
                    + "Failed to load pet food inventory for " + entity.getUniqueId() + ": " + e.getMessage());
            return new ItemStack[FOOD_SLOTS];
        }
    }

    /**
     * Saves the food inventory to the pet entity's PDC.
     */
    public static void setFoodItems(Entity entity, ItemStack[] items) {
        try {
            String data = ItemUtils.itemStackArrayToBase64(items);
            entity.getPersistentDataContainer().set(
                    CustomKeys.PET_FOOD_INVENTORY, PersistentDataType.STRING, data);
        } catch (Exception e) {
            Bukkit.getLogger().warning(AranarthCore.LOG_PREFIX
                    + "Failed to save pet food inventory for " + entity.getUniqueId() + ": " + e.getMessage());
        }
    }

    /**
     * Attempts to auto-consume food from the pet's own inventory to heal itself.
     * Only consumes if the food's nutrition is ≤ the pet's missing health (no waste).
     *
     * @param pet                The pet that was damaged.
     * @param missingHealthAfterHit Health missing after the hit is applied (half-hearts).
     */
    public static void tryAutoEat(LivingEntity pet, double missingHealthAfterHit) {
        ItemStack[] food = getFoodItems(pet);
        for (int i = 0; i < food.length; i++) {
            ItemStack item = food[i];
            if (item == null || item.getType() == Material.AIR) continue;

            int nutrition = FOOD_NUTRITION.getOrDefault(item.getType(), 0);
            if (nutrition <= 0) continue;
            // Don't eat if the food restores more than what's missing (waste)
            if (missingHealthAfterHit < nutrition) continue;

            // Consume one item
            if (item.getAmount() > 1) {
                item.setAmount(item.getAmount() - 1);
            } else {
                food[i] = null;
            }
            setFoodItems(pet, food);

            final double healAmount = nutrition;
            final double maxHealth = pet.getAttribute(
                    org.bukkit.attribute.Attribute.MAX_HEALTH).getValue();
            Bukkit.getScheduler().runTask(AranarthCore.getInstance(), () -> {
                if (!pet.isDead()) {
                    pet.setHealth(Math.min(maxHealth, pet.getHealth() + healAmount));
                    pet.getWorld().playSound(
                            pet.getLocation(), Sound.ENTITY_GENERIC_EAT, 0.8f, 1.0f);
                }
            });
            return;
        }
    }

}
