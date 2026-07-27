package com.aearost.aranarthcore.event.block;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.type.Vault;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.ItemSpawnEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.potion.PotionType;

import java.util.concurrent.ThreadLocalRandom;

/**
 * Applies the Luck effect to Trial Chamber rewards by upgrading low-quality drops into
 * better items drawn from the same loot table's higher-tier entries.
 */
public class TrialChamberLuck {

    private static final double PLAYER_SEARCH_RADIUS = 16.0;
    private static final int BLOCK_SEARCH_RADIUS = 3;

    public void execute(ItemSpawnEvent e) {
        ItemStack item = e.getEntity().getItemStack();
        Material type = item.getType();
        Location loc = e.getLocation();

        if (isTrialSpawnerFoodDrop(type)) {
            if (findNearbyBlock(loc, Material.TRIAL_SPAWNER) != null) {
                attemptUpgrade(e, loc, false);
            }
            return;
        }

        // Gate the vault block scan behind a type check
        boolean couldBeNormalVaultJunk = isNormalVaultJunkDrop(type);
        boolean couldBeOminousVaultJunk = !couldBeNormalVaultJunk && isOminousVaultJunkDrop(item);
        if (!couldBeNormalVaultJunk && !couldBeOminousVaultJunk) {
            return;
        }

        Block vault = findNearbyBlock(loc, Material.VAULT);
        if (vault == null) {
            return;
        }

        boolean ominous = isOminousVault(vault);
        if (ominous && couldBeOminousVaultJunk) {
            attemptUpgrade(e, loc, true);
        } else if (!ominous && couldBeNormalVaultJunk) {
            attemptUpgrade(e, loc, false);
        }
    }

    /**
     * Rolls the upgrade chance and, on success, cancels the spawned item and drops the replacement.
     */
    private void attemptUpgrade(ItemSpawnEvent e, Location loc, boolean ominous) {
        int luckLevel = getHighestLuckLevel(loc);
        if (luckLevel <= 0) {
            return;
        }
        double chance = Math.min(0.25 + (luckLevel - 1) * 0.15, 0.70);
        if (ThreadLocalRandom.current().nextDouble() >= chance) {
            return;
        }

        Material type = e.getEntity().getItemStack().getType();
        ItemStack replacement;
        if (isTrialSpawnerFoodDrop(type)) {
            replacement = pickTrialSpawnerReplacement();
        } else if (ominous) {
            replacement = pickOminousVaultReplacement(luckLevel);
        } else {
            replacement = pickNormalVaultReplacement(luckLevel);
        }

        e.setCancelled(true);
        loc.getWorld().dropItemNaturally(loc, replacement);
    }

    /**
     * Replaces trial spawner food with a key or a beneficial potion.
     */
    private ItemStack pickTrialSpawnerReplacement() {
        int roll = ThreadLocalRandom.current().nextInt(20);
        if (roll < 14) {
            return new ItemStack(Material.TRIAL_KEY, 1);
        }
        if (roll < 17) {
            return createPotion(PotionType.REGENERATION);
        }
        return createPotion(PotionType.SWIFTNESS);
    }

    /**
     * Replaces normal vault junk with higher-tier normal vault drops,
     * where higher Luck levels unlock better item tiers.
     */
    private ItemStack pickNormalVaultReplacement(int luckLevel) {
        int roll = ThreadLocalRandom.current().nextInt(100);
        if (luckLevel >= 3) {
            // Luck III - tier 3 / top-of-pool-3 items
            if (roll < 30) {
                return new ItemStack(Material.DIAMOND, 2);
            }
            if (roll < 65) {
                return new ItemStack(Material.GOLDEN_APPLE, 1);
            }
            if (roll < 85) {
                return new ItemStack(Material.BOLT_ARMOR_TRIM_SMITHING_TEMPLATE, 1);
            }
            return new ItemStack(Material.MUSIC_DISC_PRECIPICE, 1);
        }
        if (luckLevel == 2) {
            // Luck II - tier 2–3 items
            if (roll < 30) {
                return new ItemStack(Material.DIAMOND, 1);
            }
            if (roll < 55) {
                return new ItemStack(Material.DIAMOND, 2);
            }
            if (roll < 80) {
                return new ItemStack(Material.GOLDEN_APPLE, 1);
            }
            return new ItemStack(Material.GUSTER_BANNER_PATTERN, 1);
        }
        // Luck I - tier 2 items only
        if (roll < 40) {
            return new ItemStack(Material.EMERALD, 3);
        }
        if (roll < 75) {
            return new ItemStack(Material.DIAMOND, 1);
        }
        return new ItemStack(Material.WIND_CHARGE, 3);
    }

    /**
     * Replaces ominous vault junk with higher-tier ominous vault drops,
     * where higher Luck levels unlock bulk blocks and legendary items.
     */
    private ItemStack pickOminousVaultReplacement(int luckLevel) {
        int roll = ThreadLocalRandom.current().nextInt(100);
        if (luckLevel >= 3) {
            // Luck III - legendary ominous vault items
            if (roll < 30) {
                return new ItemStack(Material.DIAMOND_BLOCK, 1);
            }
            if (roll < 65) {
                return new ItemStack(Material.ENCHANTED_GOLDEN_APPLE, 1);
            }
            if (roll < 85) {
                return new ItemStack(Material.FLOW_ARMOR_TRIM_SMITHING_TEMPLATE, 1);
            }
            return new ItemStack(Material.HEAVY_CORE, 1);
        }
        if (luckLevel == 2) {
            // Luck II - block-tier items
            if (roll < 30) {
                return new ItemStack(Material.EMERALD_BLOCK, 1);
            }
            if (roll < 55) {
                return new ItemStack(Material.DIAMOND, 3);
            }
            if (roll < 75) {
                return new ItemStack(Material.GOLDEN_APPLE, 1);
            }
            return new ItemStack(Material.DIAMOND_BLOCK, 1);
        }
        // Luck I - bulk standard items
        if (roll < 35) {
            return new ItemStack(Material.DIAMOND, 2);
        }
        if (roll < 70) {
            return new ItemStack(Material.EMERALD_BLOCK, 1);
        }
        return new ItemStack(Material.IRON_BLOCK, 1);
    }

    private boolean isTrialSpawnerFoodDrop(Material type) {
        return type == Material.BREAD
                || type == Material.COOKED_CHICKEN
                || type == Material.BAKED_POTATO;
    }

    private boolean isNormalVaultJunkDrop(Material type) {
        return type == Material.ARROW
                || type == Material.TIPPED_ARROW
                || type == Material.IRON_INGOT
                || type == Material.HONEY_BOTTLE;
    }

    /**
     * Ominous vault's only junk entry is Tipped Arrow of Slowness IV (4–12 qty).
     */
    private boolean isOminousVaultJunkDrop(ItemStack item) {
        if (item.getType() != Material.TIPPED_ARROW) {
            return false;
        }
        if (!(item.getItemMeta() instanceof PotionMeta meta)) {
            return false;
        }
        PotionType base = meta.getBasePotionType();
        return base == PotionType.SLOWNESS || base == PotionType.STRONG_SLOWNESS || base == PotionType.LONG_SLOWNESS;
    }

    private boolean isOminousVault(Block block) {
        BlockData data = block.getBlockData();
        if (data instanceof Vault vaultData) {
            return vaultData.isOminous();
        }
        return false;
    }

    private Block findNearbyBlock(Location loc, Material... materials) {
        int cx = loc.getBlockX();
        int cy = loc.getBlockY();
        int cz = loc.getBlockZ();
        for (int x = cx - BLOCK_SEARCH_RADIUS; x <= cx + BLOCK_SEARCH_RADIUS; x++) {
            for (int y = cy - BLOCK_SEARCH_RADIUS; y <= cy + BLOCK_SEARCH_RADIUS; y++) {
                for (int z = cz - BLOCK_SEARCH_RADIUS; z <= cz + BLOCK_SEARCH_RADIUS; z++) {
                    Block block = loc.getWorld().getBlockAt(x, y, z);
                    for (Material m : materials) {
                        if (block.getType() == m) {
                            return block;
                        }
                    }
                }
            }
        }
        return null;
    }

    /**
     * Returns the highest Luck amplifier level among all players within nearby blocks.
     */
    private int getHighestLuckLevel(Location loc) {
        int highest = 0;
        for (Player player : loc.getWorld().getPlayers()) {
            if (player.getLocation().distance(loc) > PLAYER_SEARCH_RADIUS) {
                continue;
            }
            PotionEffect luck = player.getPotionEffect(PotionEffectType.LUCK);
            if (luck != null) {
                int level = luck.getAmplifier() + 1;
                if (level > highest) {
                    highest = level;
                }
            }
        }
        return highest;
    }

    private ItemStack createPotion(PotionType type) {
        ItemStack potion = new ItemStack(Material.POTION);
        PotionMeta meta = (PotionMeta) potion.getItemMeta();
        meta.setBasePotionType(type);
        potion.setItemMeta(meta);
        return potion;
    }
}
