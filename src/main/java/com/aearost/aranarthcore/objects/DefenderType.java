package com.aearost.aranarthcore.objects;

import org.bukkit.Material;
import org.bukkit.entity.EntityType;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.List;

/**
 * Defines the available Defender variants that can be purchased for a Dominion.
 */
public enum DefenderType {

    ZOMBIE(
            "Zombie",
            EntityType.ZOMBIE,
            Material.ZOMBIE_SPAWN_EGG,
            "Bruiser",
            10_000.0,
            100.0,   // 50 full hearts
            6.0,     // 3 full hearts min damage
            12.0,    // 6 full hearts max damage
            List.of(
                    new PotionEffect(PotionEffectType.RESISTANCE, Integer.MAX_VALUE, 2, false, false) // Resistance III
            )
    ),

    SKELETON(
            "Skeleton",
            EntityType.SKELETON,
            Material.SKELETON_SPAWN_EGG,
            "Archer",
            10_000.0,
            50.0,    // 25 full hearts
            8.0,    // 4 full hearts min arrow damage
            16.0,    // 8 full hearts max arrow damage
            List.of(
                    new PotionEffect(PotionEffectType.SPEED, Integer.MAX_VALUE, 1, false, false),      // Speed II
                    new PotionEffect(PotionEffectType.RESISTANCE, Integer.MAX_VALUE, 0, false, false)   // Resistance I
            )
    );

    private final String displayName;
    private final EntityType entityType;
    private final Material spawnEgg;
    private final String role;
    private final double purchasePrice;
    private final double maxHealth;
    private final double minDamage;
    private final double maxDamage;
    private final List<PotionEffect> permanentEffects;

    DefenderType(String displayName, EntityType entityType, Material spawnEgg, String role,
                 double purchasePrice, double maxHealth, double minDamage, double maxDamage,
                 List<PotionEffect> permanentEffects) {
        this.displayName = displayName;
        this.entityType = entityType;
        this.spawnEgg = spawnEgg;
        this.role = role;
        this.purchasePrice = purchasePrice;
        this.maxHealth = maxHealth;
        this.minDamage = minDamage;
        this.maxDamage = maxDamage;
        this.permanentEffects = permanentEffects;
    }

    public String getDisplayName() {
        return displayName;
    }

    public EntityType getEntityType() {
        return entityType;
    }

    public Material getSpawnEgg() {
        return spawnEgg;
    }

    public String getRole() {
        return role;
    }

    public double getPurchasePrice() {
        return purchasePrice;
    }

    public double getSellPrice() {
        return purchasePrice / 4.0;
    }

    public double getMaxHealth() {
        return maxHealth;
    }

    public double getMinDamage() {
        return minDamage;
    }

    public double getMaxDamage() {
        return maxDamage;
    }

    public List<PotionEffect> getPermanentEffects() {
        return permanentEffects;
    }
}
