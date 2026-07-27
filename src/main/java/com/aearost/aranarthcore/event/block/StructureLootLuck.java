package com.aearost.aranarthcore.event.block;

import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.world.LootGenerateEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.EnchantmentStorageMeta;
import org.bukkit.loot.LootTable;
import org.bukkit.loot.LootTables;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Improves the loot rewards from structure chests.
 */
public class StructureLootLuck {

    private enum Family {
        OVERWORLD_RUIN,
        VILLAGE,
        STRONGHOLD,
        END_CITY,
        NETHER_RUIN,   // Nether fortress + ruined portals
        BASTION,
        ANCIENT_CITY,
        MINESHAFT,
        OCEAN
    }

    private static final Set<Material> JUNK_OVERWORLD_RUIN = EnumSet.of(
            Material.ROTTEN_FLESH, Material.BONE, Material.GUNPOWDER,
            Material.STRING, Material.LEATHER, Material.COAL
    );

    private static final Set<Material> JUNK_VILLAGE = EnumSet.of(
            Material.WHEAT_SEEDS, Material.COAL, Material.PAPER,
            Material.WHEAT, Material.FEATHER
    );

    private static final Set<Material> JUNK_STRONGHOLD = EnumSet.of(
            Material.BREAD, Material.COAL, Material.PAPER
    );

    private static final Set<Material> JUNK_END_CITY = EnumSet.of(
            Material.BEETROOT_SEEDS, Material.IRON_INGOT
    );

    private static final Set<Material> JUNK_NETHER_RUIN = EnumSet.of(
            Material.ROTTEN_FLESH, Material.GOLD_NUGGET, Material.IRON_NUGGET, Material.OBSIDIAN
    );

    private static final Set<Material> JUNK_BASTION = EnumSet.of(
            Material.ROTTEN_FLESH, Material.GOLD_NUGGET, Material.IRON_NUGGET
    );

    private static final Set<Material> JUNK_ANCIENT_CITY = EnumSet.of(
            Material.COAL, Material.BONE
    );

    private static final Set<Material> JUNK_MINESHAFT = EnumSet.of(
            Material.RAIL, Material.TORCH, Material.COAL,
            Material.PUMPKIN_SEEDS, Material.MELON_SEEDS
    );

    private static final Set<Material> JUNK_OCEAN = EnumSet.of(
            Material.ROTTEN_FLESH, Material.POISONOUS_POTATO,
            Material.PAPER, Material.FEATHER, Material.GOLD_NUGGET,
            Material.IRON_NUGGET, Material.STONE_AXE, Material.WOODEN_HOE
    );

    public void execute(LootGenerateEvent e) {
        if (e.isPlugin()) {
            return;
        }

        Family family = resolveFamily(e.getLootTable());
        if (family == null) {
            return;
        }

        Entity entity = e.getEntity();
        if (!(entity instanceof Player player)) {
            return;
        }

        int luckLevel = getLuckLevel(player);
        if (luckLevel <= 0) {
            return;
        }

        double upgradeChance = getUpgradeChance(family, luckLevel);
        ThreadLocalRandom rng = ThreadLocalRandom.current();

        List<ItemStack> loot = new ArrayList<>(e.getLoot());
        boolean modified = false;

        for (int i = 0; i < loot.size(); i++) {
            ItemStack item = loot.get(i);
            if (item == null || !isJunk(item.getType(), family)) {
                continue;
            }
            if (rng.nextDouble() >= upgradeChance) {
                continue;
            }
            ItemStack replacement = pickReplacement(family, luckLevel);
            if (replacement != null) {
                loot.set(i, replacement);
                modified = true;
            }
        }

        if (modified) {
            e.setLoot(loot);
        }
    }

    private Family resolveFamily(LootTable lootTable) {
        NamespacedKeyComparator c = new NamespacedKeyComparator(lootTable);
        if (c.matches(LootTables.SIMPLE_DUNGEON, LootTables.DESERT_PYRAMID, LootTables.JUNGLE_TEMPLE,
                LootTables.IGLOO_CHEST, LootTables.PILLAGER_OUTPOST, LootTables.WOODLAND_MANSION)) {
            return Family.OVERWORLD_RUIN;
        }
        if (c.matchesSubstring("village_")) {
            return Family.VILLAGE;
        }
        if (c.matches(LootTables.STRONGHOLD_CORRIDOR, LootTables.STRONGHOLD_CROSSING, LootTables.STRONGHOLD_LIBRARY)) {
            return Family.STRONGHOLD;
        }
        if (c.matches(LootTables.END_CITY_TREASURE)) {
            return Family.END_CITY;
        }
        if (c.matches(LootTables.BASTION_TREASURE, LootTables.BASTION_OTHER,
                LootTables.BASTION_BRIDGE, LootTables.BASTION_HOGLIN_STABLE)) {
            return Family.BASTION;
        }
        if (c.matches(LootTables.NETHER_BRIDGE, LootTables.RUINED_PORTAL)) {
            return Family.NETHER_RUIN;
        }
        if (c.matches(LootTables.ANCIENT_CITY, LootTables.ANCIENT_CITY_ICE_BOX)) {
            return Family.ANCIENT_CITY;
        }
        if (c.matches(LootTables.ABANDONED_MINESHAFT)) {
            return Family.MINESHAFT;
        }
        if (c.matches(LootTables.SHIPWRECK_SUPPLY, LootTables.SHIPWRECK_TREASURE, LootTables.SHIPWRECK_MAP,
                LootTables.UNDERWATER_RUIN_BIG, LootTables.UNDERWATER_RUIN_SMALL)) {
            return Family.OCEAN;
        }
        return null;
    }

    /**
     * Bastions use a steeper curve so that Netherite Upgrade Templates are near-guaranteed
     * with Luck II or III. All other families use the standard curve.
     */
    private double getUpgradeChance(Family family, int luckLevel) {
        if (family == Family.BASTION) {
            // Luck I - 40%, II - 65%, III - 85%
            return Math.min(0.40 + (luckLevel - 1) * 0.25, 0.85);
        }

        // Luck I - 25%, II - 40%, III - 55%
        return Math.min(0.25 + (luckLevel - 1) * 0.15, 0.70);
    }

    private boolean isJunk(Material type, Family family) {
        return switch (family) {
            case OVERWORLD_RUIN -> JUNK_OVERWORLD_RUIN.contains(type);
            case VILLAGE -> JUNK_VILLAGE.contains(type);
            case STRONGHOLD -> JUNK_STRONGHOLD.contains(type);
            case END_CITY -> JUNK_END_CITY.contains(type);
            case BASTION -> JUNK_BASTION.contains(type);
            case NETHER_RUIN -> JUNK_NETHER_RUIN.contains(type);
            case ANCIENT_CITY -> JUNK_ANCIENT_CITY.contains(type);
            case MINESHAFT -> JUNK_MINESHAFT.contains(type);
            case OCEAN -> JUNK_OCEAN.contains(type);
        };
    }

    private ItemStack pickReplacement(Family family, int luckLevel) {
        return switch (family) {
            case OVERWORLD_RUIN -> pickOverworldRuinReplacement(luckLevel);
            case VILLAGE -> pickVillageReplacement(luckLevel);
            case STRONGHOLD -> pickStrongholdReplacement(luckLevel);
            case END_CITY -> pickEndCityReplacement(luckLevel);
            case BASTION -> pickBastionReplacement(luckLevel);
            case NETHER_RUIN -> pickNetherRuinReplacement(luckLevel);
            case ANCIENT_CITY -> pickAncientCityReplacement(luckLevel);
            case MINESHAFT -> pickMineshaftReplacement(luckLevel);
            case OCEAN -> pickOceanReplacement(luckLevel);
        };
    }

    /**
     * Dungeon / jungle temple / desert pyramid / igloo / pillager outpost / woodland mansion.
     */
    private ItemStack pickOverworldRuinReplacement(int luckLevel) {
        ThreadLocalRandom rng = ThreadLocalRandom.current();
        int roll = rng.nextInt(100);
        if (luckLevel >= 3) {
            if (roll < 35) {
                return new ItemStack(Material.GOLDEN_APPLE, 1);
            }
            if (roll < 60) {
                return new ItemStack(Material.DIAMOND, rng.nextInt(1, 3));
            }
            if (roll < 80) {
                return new ItemStack(Material.NAME_TAG, 1);
            }
            return new ItemStack(Material.GOLD_INGOT, rng.nextInt(2, 5));
        }
        if (luckLevel == 2) {
            if (roll < 30) {
                return new ItemStack(Material.DIAMOND, 1);
            }
            if (roll < 60) {
                return new ItemStack(Material.GOLD_INGOT, rng.nextInt(1, 4));
            }
            if (roll < 80) {
                return new ItemStack(Material.GOLDEN_APPLE, 1);
            }
            return new ItemStack(Material.NAME_TAG, 1);
        }
        // Luck I
        if (roll < 55) {
            return new ItemStack(Material.GOLD_INGOT, rng.nextInt(1, 3));
        }
        return new ItemStack(Material.IRON_INGOT, rng.nextInt(2, 5));
    }

    /**
     * All village house/job-site chest types.
     */
    private ItemStack pickVillageReplacement(int luckLevel) {
        ThreadLocalRandom rng = ThreadLocalRandom.current();
        int roll = rng.nextInt(100);
        if (luckLevel >= 3) {
            if (roll < 40) {
                return new ItemStack(Material.EMERALD, rng.nextInt(1, 3));
            }
            if (roll < 70) {
                return new ItemStack(Material.GOLD_INGOT, rng.nextInt(1, 3));
            }
            return new ItemStack(Material.DIAMOND, 1);
        }
        if (luckLevel == 2) {
            if (roll < 60) {
                return new ItemStack(Material.EMERALD, rng.nextInt(1, 3));
            }
            return new ItemStack(Material.GOLD_INGOT, 1);
        }
        // Luck I
        return new ItemStack(Material.EMERALD, 1);
    }

    /**
     * Stronghold corridor / crossing / library.
     */
    private ItemStack pickStrongholdReplacement(int luckLevel) {
        ThreadLocalRandom rng = ThreadLocalRandom.current();
        int roll = rng.nextInt(100);
        if (luckLevel >= 3) {
            if (roll < 35) {
                return new ItemStack(Material.GOLDEN_APPLE, 1);
            }
            if (roll < 60) {
                return new ItemStack(Material.DIAMOND, rng.nextInt(1, 3));
            }
            if (roll < 80) {
                return new ItemStack(Material.ENDER_PEARL, rng.nextInt(1, 3));
            }
            return new ItemStack(Material.GOLD_INGOT, rng.nextInt(2, 5));
        }
        if (luckLevel == 2) {
            if (roll < 40) {
                return new ItemStack(Material.GOLDEN_APPLE, 1);
            }
            if (roll < 70) {
                return new ItemStack(Material.GOLD_INGOT, rng.nextInt(1, 4));
            }
            return new ItemStack(Material.ENDER_PEARL, 1);
        }
        // Luck I
        if (roll < 55) {
            return new ItemStack(Material.IRON_INGOT, rng.nextInt(2, 5));
        }
        return new ItemStack(Material.ENDER_PEARL, 1);
    }

    /**
     * End City treasure.
     */
    private ItemStack pickEndCityReplacement(int luckLevel) {
        ThreadLocalRandom rng = ThreadLocalRandom.current();
        int roll = rng.nextInt(100);
        if (luckLevel >= 3) {
            if (roll < 50) {
                return new ItemStack(Material.DIAMOND, rng.nextInt(2, 5));
            }
            if (roll < 85) {
                return new ItemStack(Material.EMERALD, rng.nextInt(3, 7));
            }
            return new ItemStack(Material.SPIRE_ARMOR_TRIM_SMITHING_TEMPLATE, 1);
        }
        if (luckLevel == 2) {
            if (roll < 55) {
                return new ItemStack(Material.DIAMOND, rng.nextInt(1, 4));
            }
            if (roll < 85) {
                return new ItemStack(Material.EMERALD, rng.nextInt(2, 5));
            }
            return new ItemStack(Material.SPIRE_ARMOR_TRIM_SMITHING_TEMPLATE, 1);
        }
        // Luck I
        if (roll < 65) {
            return new ItemStack(Material.DIAMOND, rng.nextInt(1, 3));
        }
        return new ItemStack(Material.GOLD_INGOT, rng.nextInt(2, 5));
    }

    /**
     * All four bastion remnant chest types.
     */
    private ItemStack pickBastionReplacement(int luckLevel) {
        ThreadLocalRandom rng = ThreadLocalRandom.current();
        int roll = rng.nextInt(100);
        if (luckLevel >= 3) {
            if (roll < 70) {
                return new ItemStack(Material.NETHERITE_UPGRADE_SMITHING_TEMPLATE, 1);
            }
            if (roll < 82) {
                return new ItemStack(Material.ANCIENT_DEBRIS, rng.nextInt(1, 3));
            }
            if (roll < 91) {
                return new ItemStack(Material.NETHERITE_INGOT, 1);
            }
            return createEnchantedBook(Enchantment.SOUL_SPEED, 3);
        }
        if (luckLevel == 2) {
            if (roll < 80) {
                return new ItemStack(Material.NETHERITE_UPGRADE_SMITHING_TEMPLATE, 1);
            }
            if (roll < 88) {
                return new ItemStack(Material.NETHERITE_SCRAP, rng.nextInt(1, 3));
            }
            if (roll < 95) {
                return new ItemStack(Material.ANCIENT_DEBRIS, 1);
            }
            return createEnchantedBook(Enchantment.SOUL_SPEED, 2);
        }
        // Luck I
        if (roll < 40) {
            return new ItemStack(Material.GOLD_INGOT, rng.nextInt(2, 5));
        }
        if (roll < 70) {
            return new ItemStack(Material.NETHERITE_SCRAP, 1);
        }
        if (roll < 85) {
            return new ItemStack(Material.NETHERITE_UPGRADE_SMITHING_TEMPLATE, 1);
        }
        return createEnchantedBook(Enchantment.SOUL_SPEED, 1);
    }

    /**
     * Nether fortress and ruined portals.
     */
    private ItemStack pickNetherRuinReplacement(int luckLevel) {
        ThreadLocalRandom rng = ThreadLocalRandom.current();
        int roll = rng.nextInt(100);
        if (luckLevel >= 3) {
            if (roll < 40) {
                return new ItemStack(Material.GOLDEN_APPLE, 1);
            }
            if (roll < 70) {
                return new ItemStack(Material.DIAMOND, rng.nextInt(1, 3));
            }
            return new ItemStack(Material.GOLD_INGOT, rng.nextInt(3, 6));
        }
        if (luckLevel == 2) {
            if (roll < 45) {
                return new ItemStack(Material.DIAMOND, rng.nextInt(1, 3));
            }
            return new ItemStack(Material.GOLD_INGOT, rng.nextInt(2, 5));
        }
        // Luck I
        return new ItemStack(Material.GOLD_INGOT, rng.nextInt(1, 4));
    }

    /**
     * Ancient City main and ice-box chests.
     * Swift Sneak book level mirrors Luck level (I → I, II → II, III → III).
     */
    private ItemStack pickAncientCityReplacement(int luckLevel) {
        ThreadLocalRandom rng = ThreadLocalRandom.current();
        int roll = rng.nextInt(100);
        if (luckLevel >= 3) {
            if (roll < 30) {
                return createEnchantedBook(Enchantment.SWIFT_SNEAK, 3);
            }
            if (roll < 55) {
                return new ItemStack(Material.RECOVERY_COMPASS, 1);
            }
            if (roll < 80) {
                return new ItemStack(Material.DISC_FRAGMENT_5, rng.nextInt(3, 6));
            }
            return new ItemStack(Material.ECHO_SHARD, rng.nextInt(2, 4));
        }
        if (luckLevel == 2) {
            if (roll < 30) {
                return createEnchantedBook(Enchantment.SWIFT_SNEAK, 2);
            }
            if (roll < 55) {
                return new ItemStack(Material.DISC_FRAGMENT_5, rng.nextInt(2, 4));
            }
            if (roll < 80) {
                return new ItemStack(Material.ECHO_SHARD, rng.nextInt(1, 3));
            }
            return new ItemStack(Material.RECOVERY_COMPASS, 1);
        }
        // Luck I
        if (roll < 35) {
            return createEnchantedBook(Enchantment.SWIFT_SNEAK, 1);
        }
        if (roll < 70) {
            return new ItemStack(Material.ECHO_SHARD, rng.nextInt(1, 3));
        }
        return new ItemStack(Material.AMETHYST_SHARD, rng.nextInt(2, 5));
    }

    /**
     * Abandoned mineshafts.
     */
    private ItemStack pickMineshaftReplacement(int luckLevel) {
        ThreadLocalRandom rng = ThreadLocalRandom.current();
        int roll = rng.nextInt(100);
        if (luckLevel >= 3) {
            if (roll < 40) {
                return new ItemStack(Material.DIAMOND, rng.nextInt(1, 3));
            }
            if (roll < 70) {
                return new ItemStack(Material.GOLDEN_APPLE, 1);
            }
            return new ItemStack(Material.NAME_TAG, 1);
        }
        if (luckLevel == 2) {
            if (roll < 45) {
                return new ItemStack(Material.GOLD_INGOT, rng.nextInt(1, 4));
            }
            if (roll < 75) {
                return new ItemStack(Material.NAME_TAG, 1);
            }
            return new ItemStack(Material.DIAMOND, 1);
        }
        // Luck I
        if (roll < 60) {
            return new ItemStack(Material.IRON_INGOT, rng.nextInt(2, 5));
        }
        return new ItemStack(Material.GOLD_INGOT, rng.nextInt(1, 3));
    }

    /**
     * Shipwrecks and underwater ruins.
     */
    private ItemStack pickOceanReplacement(int luckLevel) {
        ThreadLocalRandom rng = ThreadLocalRandom.current();
        int roll = rng.nextInt(100);
        if (luckLevel >= 3) {
            if (roll < 40) {
                return new ItemStack(Material.DIAMOND, rng.nextInt(1, 3));
            }
            if (roll < 75) {
                return new ItemStack(Material.EMERALD, rng.nextInt(1, 4));
            }
            return new ItemStack(Material.GOLD_INGOT, rng.nextInt(2, 5));
        }
        if (luckLevel == 2) {
            if (roll < 50) {
                return new ItemStack(Material.GOLD_INGOT, rng.nextInt(1, 4));
            }
            return new ItemStack(Material.EMERALD, rng.nextInt(1, 3));
        }
        // Luck I
        return new ItemStack(Material.IRON_INGOT, rng.nextInt(1, 4));
    }

    private int getLuckLevel(Player player) {
        PotionEffect luck = player.getPotionEffect(PotionEffectType.LUCK);
        return luck == null ? 0 : luck.getAmplifier() + 1;
    }

    /**
     * Creates an enchanted book with the given enchantment at the specified level.
     */
    private ItemStack createEnchantedBook(Enchantment enchantment, int level) {
        ItemStack book = new ItemStack(Material.ENCHANTED_BOOK);
        EnchantmentStorageMeta meta = (EnchantmentStorageMeta) book.getItemMeta();
        meta.addStoredEnchant(enchantment, level, true);
        book.setItemMeta(meta);
        return book;
    }

    private static final class NamespacedKeyComparator {
        private final LootTable table;

        NamespacedKeyComparator(LootTable table) {
            this.table = table;
        }

        boolean matches(LootTables... candidates) {
            for (LootTables candidate : candidates) {
                if (candidate.getLootTable().getKey().equals(table.getKey())) {
                    return true;
                }
            }
            return false;
        }

        /**
         * True if the loot table's key path contains the given substring (e.g. "village_").
         */
        boolean matchesSubstring(String substring) {
            return table.getKey().getKey().contains(substring);
        }
    }
}
