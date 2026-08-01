package com.aearost.aranarthcore.utils;

import com.aearost.aranarthcore.enums.WanderingTraderType;
import com.aearost.aranarthcore.items.ChorusDiamond;
import com.aearost.aranarthcore.items.GodAppleFragment;
import com.aearost.aranarthcore.objects.CustomKeys;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.WanderingTrader;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.MerchantRecipe;
import org.bukkit.inventory.meta.EnchantmentStorageMeta;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.potion.PotionType;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.stream.Collectors;

public class WanderingTraderUtils {

    private static final Random RANDOM = new Random();

    private static final Material[] TIER1_STONES = {
        Material.STONE, Material.COBBLESTONE, Material.GRANITE,
        Material.DIORITE, Material.ANDESITE, Material.SANDSTONE, Material.RED_SANDSTONE
    };

    private static final Material[] TIER2_STONES = {
        Material.DEEPSLATE, Material.COBBLED_DEEPSLATE, Material.TUFF, Material.DRIPSTONE_BLOCK
    };

    private static final Material[] TIER3_STONES = {
        Material.BASALT, Material.BLACKSTONE, Material.END_STONE, Material.MUD, Material.CALCITE
    };

    private static final Material[] SPECIAL_STONES = {
        Material.AMETHYST_BLOCK, Material.PRISMARINE, Material.DARK_PRISMARINE, Material.QUARTZ_BLOCK
    };

    private static final Material[] TIER1_LOGS = {
        Material.OAK_LOG, Material.BIRCH_LOG, Material.SPRUCE_LOG,
        Material.ACACIA_LOG, Material.DARK_OAK_LOG, Material.JUNGLE_LOG
    };

    private static final Material[] TIER2_LOGS = {
        Material.CHERRY_LOG, Material.MANGROVE_LOG, Material.PALE_OAK_LOG
    };

    private static final Material[] NETHER_STEMS = {
        Material.CRIMSON_STEM, Material.WARPED_STEM
    };

    private static final Material[] ALL_SAPLINGS = {
        Material.OAK_SAPLING, Material.BIRCH_SAPLING, Material.SPRUCE_SAPLING,
        Material.ACACIA_SAPLING, Material.DARK_OAK_SAPLING, Material.JUNGLE_SAPLING,
        Material.CHERRY_SAPLING, Material.MANGROVE_PROPAGULE, Material.PALE_OAK_SAPLING
    };

    private static final Material[] CORAL_BLOCKS = {
        Material.BRAIN_CORAL_BLOCK, Material.BUBBLE_CORAL_BLOCK, Material.FIRE_CORAL_BLOCK,
        Material.HORN_CORAL_BLOCK, Material.TUBE_CORAL_BLOCK
    };

    private static final Material[] FLOWERS = {
        Material.ALLIUM, Material.OXEYE_DAISY, Material.CORNFLOWER,
        Material.LILAC, Material.AZURE_BLUET, Material.ROSE_BUSH
    };

    private static final Material[] VINES = {
        Material.TWISTING_VINES, Material.WEEPING_VINES
    };

    private static final Material[] SMITHING_TEMPLATES = {
        Material.SENTRY_ARMOR_TRIM_SMITHING_TEMPLATE,
        Material.VEX_ARMOR_TRIM_SMITHING_TEMPLATE,
        Material.WILD_ARMOR_TRIM_SMITHING_TEMPLATE,
        Material.COAST_ARMOR_TRIM_SMITHING_TEMPLATE,
        Material.DUNE_ARMOR_TRIM_SMITHING_TEMPLATE,
        Material.WAYFINDER_ARMOR_TRIM_SMITHING_TEMPLATE,
        Material.RAISER_ARMOR_TRIM_SMITHING_TEMPLATE,
        Material.SHAPER_ARMOR_TRIM_SMITHING_TEMPLATE,
        Material.HOST_ARMOR_TRIM_SMITHING_TEMPLATE,
        Material.WARD_ARMOR_TRIM_SMITHING_TEMPLATE,
        Material.SILENCE_ARMOR_TRIM_SMITHING_TEMPLATE,
        Material.TIDE_ARMOR_TRIM_SMITHING_TEMPLATE,
        Material.SNOUT_ARMOR_TRIM_SMITHING_TEMPLATE,
        Material.RIB_ARMOR_TRIM_SMITHING_TEMPLATE,
        Material.EYE_ARMOR_TRIM_SMITHING_TEMPLATE,
        Material.SPIRE_ARMOR_TRIM_SMITHING_TEMPLATE,
        Material.FLOW_ARMOR_TRIM_SMITHING_TEMPLATE,
        Material.BOLT_ARMOR_TRIM_SMITHING_TEMPLATE
    };

    private record OverLeveledEnchant(Enchantment enchantment, int level, Material appliedMaterial) {}

    // null means "choose randomly from pickaxe or chestplate" (Unbreaking IV)
    private static final List<OverLeveledEnchant> OVER_LEVELED_ENCHANTS = List.of(
        new OverLeveledEnchant(Enchantment.SHARPNESS, 6, Material.DIAMOND_SWORD),
        new OverLeveledEnchant(Enchantment.EFFICIENCY, 6, Material.DIAMOND_PICKAXE),
        new OverLeveledEnchant(Enchantment.FORTUNE, 4, Material.DIAMOND_PICKAXE),
        new OverLeveledEnchant(Enchantment.PROTECTION, 5, Material.DIAMOND_CHESTPLATE),
        new OverLeveledEnchant(Enchantment.FEATHER_FALLING, 5, Material.DIAMOND_BOOTS),
        new OverLeveledEnchant(Enchantment.LOOTING, 4, Material.DIAMOND_SWORD),
        new OverLeveledEnchant(Enchantment.FIRE_ASPECT, 3, Material.DIAMOND_SWORD),
        new OverLeveledEnchant(Enchantment.POWER, 6, Material.BOW),
        new OverLeveledEnchant(Enchantment.UNBREAKING, 4, null),
        new OverLeveledEnchant(Enchantment.DEPTH_STRIDER, 4, Material.DIAMOND_BOOTS),
        new OverLeveledEnchant(Enchantment.RESPIRATION, 4, Material.DIAMOND_HELMET),
        new OverLeveledEnchant(Enchantment.SWIFT_SNEAK, 4, Material.DIAMOND_LEGGINGS),
        new OverLeveledEnchant(Enchantment.SOUL_SPEED, 4, Material.DIAMOND_BOOTS),
        new OverLeveledEnchant(Enchantment.LUCK_OF_THE_SEA, 4, Material.FISHING_ROD),
        new OverLeveledEnchant(Enchantment.LURE, 4, Material.FISHING_ROD),
        new OverLeveledEnchant(Enchantment.FROST_WALKER, 3, Material.DIAMOND_BOOTS),
        new OverLeveledEnchant(Enchantment.KNOCKBACK, 3, Material.DIAMOND_SWORD),
        new OverLeveledEnchant(Enchantment.PUNCH, 3, Material.BOW)
    );

    private record NormalBook(Enchantment enchantment, int level, int price) {}

    private static final List<NormalBook> NORMAL_BOOKS = List.of(
        new NormalBook(Enchantment.MENDING, 1, 24),
        new NormalBook(Enchantment.SILK_TOUCH, 1, 16),
        new NormalBook(Enchantment.UNBREAKING, 3, 12),
        new NormalBook(Enchantment.INFINITY, 1, 12),
        new NormalBook(Enchantment.FLAME, 1, 8),
        new NormalBook(Enchantment.PUNCH, 2, 8),
        new NormalBook(Enchantment.LOYALTY, 3, 10),
        new NormalBook(Enchantment.CHANNELING, 1, 8),
        new NormalBook(Enchantment.MULTISHOT, 1, 10),
        new NormalBook(Enchantment.QUICK_CHARGE, 3, 10),
        new NormalBook(Enchantment.RIPTIDE, 3, 10),
        new NormalBook(Enchantment.THORNS, 3, 10)
    );

    // canBeAmplified: if true, randomly pick extended or amplified II at spawn
    private record PotionOption(
        PotionType baseType,
        PotionEffectType effectType,
        int extendedTicks,
        int amplifiedTicks,
        int cost,
        boolean canBeAmplified
    ) {}

    private static final List<PotionOption> POTION_POOL = List.of(
        new PotionOption(PotionType.NIGHT_VISION,    PotionEffectType.NIGHT_VISION,    9600, 9600, 1, false),
        new PotionOption(PotionType.INVISIBILITY,    PotionEffectType.INVISIBILITY,    9600, 9600, 1, false),
        new PotionOption(PotionType.FIRE_RESISTANCE, PotionEffectType.FIRE_RESISTANCE, 9600, 9600, 1, false),
        new PotionOption(PotionType.WATER_BREATHING, PotionEffectType.WATER_BREATHING, 9600, 9600, 1, false),
        new PotionOption(PotionType.REGENERATION,    PotionEffectType.REGENERATION,    2100,  900, 1, true),
        new PotionOption(PotionType.STRENGTH,        PotionEffectType.STRENGTH,        9600, 1800, 1, true),
        new PotionOption(PotionType.WATER,           PotionEffectType.HASTE,           9600, 1800, 1, true),
        new PotionOption(PotionType.LUCK,            PotionEffectType.LUCK,            6000, 6000, 1, false),
        new PotionOption(PotionType.WATER,           PotionEffectType.WITHER,          1800,  440, 2, true),
        new PotionOption(PotionType.WATER,           PotionEffectType.ABSORPTION,      2100,  900, 2, true)
    );

    /**
     * Selects a trader type.
     * @return the selected WanderingTraderType
     */
    public static WanderingTraderType applyTrader(WanderingTrader wt) {
        WanderingTraderType[] types = WanderingTraderType.values();
        WanderingTraderType type = types[RANDOM.nextInt(types.length)];

        wt.getPersistentDataContainer().set(
            CustomKeys.WANDERING_TRADER_TYPE,
            PersistentDataType.STRING,
            type.name()
        );

        if (type == WanderingTraderType.VANILLA) {
            return type;
        }

        wt.setRecipes(buildTrades(type, wt));

        return type;
    }

    private static List<MerchantRecipe> buildTrades(WanderingTraderType type, WanderingTrader wt) {
        return switch (type) {
            case MASON      -> buildMasonTrades();
            case LUMBERJACK -> buildLumberjackTrades();
            case FARMER     -> buildFarmerTrades();
            case SMITH      -> buildSmithTrades();
            case ALCHEMIST  -> buildAlchemistTrades();
            case TRAVELER   -> buildTravelerTrades();
            case ENCHANTER  -> buildEnchanterTrades(wt);
            default         -> new ArrayList<>();
        };
    }

    private static List<MerchantRecipe> buildMasonTrades() {
        List<MerchantRecipe> trades = new ArrayList<>();

        // Tier 1 - pick 3-4 unique stones (7 available)
        List<Material> t1 = shuffledList(TIER1_STONES);
        int t1Count = 3 + RANDOM.nextInt(2); // 3 or 4
        for (int i = 0; i < t1Count; i++) {
            trades.add(buy(new ItemStack(t1.get(i), 64), 32, 1));
        }

        // Tier 2 - pick 2 unique stones (4 available)
        List<Material> t2 = shuffledList(TIER2_STONES);
        for (int i = 0; i < 2; i++) {
            trades.add(buy(new ItemStack(t2.get(i), 64), 32, 2));
        }

        // Tier 3 - pick 2 unique stones (5 available)
        List<Material> t3 = shuffledList(TIER3_STONES);
        for (int i = 0; i < 2; i++) {
            trades.add(buy(new ItemStack(t3.get(i), 64), 32, 3));
        }

        // Special - pick 1-4 unique blocks (4 available)
        int soFar = trades.size(); // 7, 8, or 9 depending on t1Count
        List<Material> sp = shuffledList(SPECIAL_STONES);
        int specialMax = Math.min(12 - soFar, sp.size()); // Cap at available pool size
        int specialCount = 1 + RANDOM.nextInt(specialMax); // At least 1, up to specialMax
        for (int i = 0; i < specialCount; i++) {
            trades.add(buy(new ItemStack(sp.get(i), 16), 32, 3));
        }

        return trades;
    }

    private static List<MerchantRecipe> buildLumberjackTrades() {
        List<MerchantRecipe> trades = new ArrayList<>();

        // Always 4 unique wood types, tiered pricing preserved
        Set<Material> tier1Set = new HashSet<>(Arrays.asList(TIER1_LOGS));
        List<Material> allLogs = new ArrayList<>();
        allLogs.addAll(Arrays.asList(TIER1_LOGS));
        allLogs.addAll(Arrays.asList(TIER2_LOGS));
        Collections.shuffle(allLogs, RANDOM);
        for (int i = 0; i < 4; i++) {
            Material log = allLogs.get(i);
            int price = tier1Set.contains(log) ? 1 : 2;
            trades.add(buy(new ItemStack(log, 64), 32, price));
        }

        // Nether stem or bamboo block, equal chance
        Material stemOrBamboo = RANDOM.nextBoolean() ? pickRandom(NETHER_STEMS) : Material.BAMBOO_BLOCK;
        trades.add(buy(new ItemStack(stemOrBamboo, 64), 32, 3));

        // 2-3 different saplings
        List<Material> saplingPool = new ArrayList<>(Arrays.asList(ALL_SAPLINGS));
        Collections.shuffle(saplingPool, RANDOM);
        int saplingCount = 2 + RANDOM.nextInt(2); // 2 or 3
        for (int i = 0; i < saplingCount; i++) {
            trades.add(buy(new ItemStack(saplingPool.get(i), 1), 16, 2));
        }

        trades.add(buy(new ItemStack(Material.CHORUS_FRUIT, 32), 24, 1));
        trades.add(buy(new ItemStack(Material.APPLE, 8), 24, 1));

        if (RANDOM.nextBoolean()) {
            trades.add(sell(3, 24, new GodAppleFragment().getItem()));
        }

        return trades;
    }

    private static List<MerchantRecipe> buildFarmerTrades() {
        List<MerchantRecipe> trades = new ArrayList<>();

        // Sell trades (player gives crops, receives emeralds)
        Material cropAB = RANDOM.nextBoolean() ? Material.WHEAT : Material.BEETROOT;
        trades.add(sell(1, 64, new ItemStack(cropAB, 4)));

        Material cropAmt8 = pickRandom(new Material[]{Material.POTATO, Material.CARROT, Material.COCOA_BEANS});
        trades.add(sell(1, 64, new ItemStack(cropAmt8, 8)));

        Material berries = RANDOM.nextBoolean() ? Material.SWEET_BERRIES : Material.GLOW_BERRIES;
        trades.add(sell(4, 64, new ItemStack(berries, 16)));

        // Buy trades (player gives emeralds, receives items)
        Material seeds = pickRandom(new Material[]{
            Material.WHEAT_SEEDS, Material.MELON_SEEDS,
            Material.PUMPKIN_SEEDS, Material.TORCHFLOWER_SEEDS
        });
        trades.add(buy(new ItemStack(seeds, 16), 24, 1));
        trades.add(buy(new ItemStack(Material.BROWN_MUSHROOM_BLOCK, 16), 16, 1));
        trades.add(buy(new ItemStack(Material.RED_MUSHROOM_BLOCK, 16), 16, 1));
        trades.add(buy(new ItemStack(Material.MUSHROOM_STEM, 16), 16, 1));
        trades.add(buy(new ItemStack(Material.HONEY_BLOCK, 2), 24, 1));

        return trades;
    }

    private static List<MerchantRecipe> buildSmithTrades() {
        List<MerchantRecipe> trades = new ArrayList<>();

        // Sell trades
        trades.add(sell(2, 32, new ItemStack(Material.RAW_COPPER, 8)));
        trades.add(sell(3, 32, new ItemStack(Material.RAW_IRON, 6)));
        trades.add(sell(4, 32, new ItemStack(Material.RAW_GOLD, 4)));

        // Buy trade
        trades.add(buy(new ItemStack(Material.DIAMOND, 1), 16, 1));

        // One random smithing template
        trades.add(buy(new ItemStack(pickRandom(SMITHING_TEMPLATES), 1), 2, 32));

        // Diamond gear loadout
        if (RANDOM.nextBoolean()) {
            trades.add(buy(new ItemStack(Material.DIAMOND_SWORD, 1), 3, 3));
            trades.add(buy(new ItemStack(Material.DIAMOND_PICKAXE, 1), 3, 3));
            if (RANDOM.nextBoolean()) {
                Material extraTool = pickRandom(new Material[]{
                    Material.DIAMOND_AXE, Material.DIAMOND_SHOVEL, Material.DIAMOND_HOE
                });
                trades.add(buy(new ItemStack(extraTool, 1), 3, 3));
            }
        } else {
            List<Material> armorPool = new ArrayList<>(Arrays.asList(
                Material.DIAMOND_HELMET, Material.DIAMOND_CHESTPLATE,
                Material.DIAMOND_LEGGINGS, Material.DIAMOND_BOOTS
            ));
            Collections.shuffle(armorPool, RANDOM);
            int count = 2 + RANDOM.nextInt(2); // 2 or 3
            for (int i = 0; i < count; i++) {
                trades.add(buy(new ItemStack(armorPool.get(i), 1), 3, 5));
            }
        }

        return trades;
    }

    private static List<MerchantRecipe> buildAlchemistTrades() {
        List<MerchantRecipe> trades = new ArrayList<>();

        // Ingredient trades
        trades.add(buy(new ItemStack(Material.NETHER_WART, 16),   32, 1));
        trades.add(buy(new ItemStack(Material.BLAZE_POWDER, 8),    32, 1));
        trades.add(buy(new ItemStack(Material.GHAST_TEAR, 2),      16, 1));
        trades.add(buy(new ItemStack(Material.DRAGON_BREATH, 8),   16, 1));
        trades.add(buy(new ItemStack(Material.WITHER_ROSE, 8),     24, 1));
        trades.add(buy(new ChorusDiamond().getItem(),              8, 2));

        // 4–5 randomly selected potions
        List<PotionOption> shuffled = new ArrayList<>(POTION_POOL);
        Collections.shuffle(shuffled, RANDOM);
        int count = 4 + RANDOM.nextInt(2); // 4 or 5
        for (int i = 0; i < count; i++) {
            PotionOption opt = shuffled.get(i);
            trades.add(buy(buildPotion(opt), 8, opt.cost()));
        }

        return trades;
    }

    private static List<MerchantRecipe> buildTravelerTrades() {
        List<MerchantRecipe> trades = new ArrayList<>();

        // Vines (one random type)
        trades.add(buy(new ItemStack(pickRandom(VINES), 16), 24, 1));

        // Flowers (one random type)
        trades.add(buy(new ItemStack(pickRandom(FLOWERS), 16), 24, 1));

        trades.add(buy(new ItemStack(Material.FIREFLY_BUSH, 16), 16, 1));
        trades.add(buy(new ItemStack(Material.SPORE_BLOSSOM, 8), 16, 1));

        // Pitcher Pod OR Torchflower Seeds (one per spawn)
        Material plant = RANDOM.nextBoolean() ? Material.PITCHER_POD : Material.TORCHFLOWER_SEEDS;
        trades.add(buy(new ItemStack(plant, 4), 16, 1));

        // Up to 3 different coral block types
        List<Material> coralPool = new ArrayList<>(Arrays.asList(CORAL_BLOCKS));
        Collections.shuffle(coralPool, RANDOM);
        for (int i = 0; i < 3; i++) {
            trades.add(buy(new ItemStack(coralPool.get(i), 16), 16, 1));
        }

        trades.add(buy(new ItemStack(Material.CRYING_OBSIDIAN, 8),  16, 3));
        trades.add(buy(new ItemStack(Material.SULFUR, 64),          32, 2));
        trades.add(buy(new ItemStack(Material.CINNABAR, 64),        32, 2));
        trades.add(buy(new ItemStack(Material.POTENT_SULFUR, 4),     8, 1));
        trades.add(buy(new ItemStack(Material.TURTLE_EGG, 1),        8, 4));
        trades.add(buy(new ItemStack(Material.SNIFFER_EGG, 1),       2, 12));
        trades.add(buy(new ItemStack(Material.DRIED_GHAST, 2),       8, 3));
        trades.add(buy(new ItemStack(Material.ANCIENT_DEBRIS, 1),    8, 6));
        trades.add(buy(new ChorusDiamond().getItem(),                8, 12));

        return trades;
    }

    private static List<MerchantRecipe> buildEnchanterTrades(WanderingTrader wt) {
        List<MerchantRecipe> trades = new ArrayList<>();

        // Supply trades
        trades.add(buy(new ItemStack(Material.LAPIS_LAZULI, 32),    16, 1));
        trades.add(buy(new ItemStack(Material.EXPERIENCE_BOTTLE, 32), 32, 1));

        // 2 normal enchant books (shuffled, 4 max uses each)
        List<NormalBook> shuffledBooks = new ArrayList<>(NORMAL_BOOKS);
        Collections.shuffle(shuffledBooks, RANDOM);
        for (int i = 0; i < 2; i++) {
            NormalBook book = shuffledBooks.get(i);
            trades.add(buy(buildEnchantedBook(book.enchantment(), book.level()), 4, book.price()));
        }

        // 2 over-leveled trades (1 max use each, all lock on first purchase)
        List<OverLeveledEnchant> shuffledOver = new ArrayList<>(OVER_LEVELED_ENCHANTS);
        Collections.shuffle(shuffledOver, RANDOM);
        int overCount = 2;

        int overLeveledStartIndex = trades.size(); // Always 4
        List<Integer> overLeveledIndices = new ArrayList<>();

        for (int i = 0; i < overCount && i < shuffledOver.size(); i++) {
            OverLeveledEnchant oe = shuffledOver.get(i);
            int bookPrice = 32 + RANDOM.nextInt(17); // 32–48
            int itemPrice = 48 + RANDOM.nextInt(17); // 48–64

            ItemStack tradeItem;
            int price;
            if (RANDOM.nextBoolean()) {
                tradeItem = buildEnchantedBook(oe.enchantment(), oe.level());
                price = bookPrice;
            } else {
                Material mat = oe.appliedMaterial();
                if (mat == null) {
                    mat = RANDOM.nextBoolean() ? Material.DIAMOND_PICKAXE : Material.DIAMOND_CHESTPLATE;
                }
                tradeItem = buildEnchantedItem(mat, oe.enchantment(), oe.level());
                price = itemPrice;
            }

            trades.add(buy(tradeItem, 1, price));
            overLeveledIndices.add(overLeveledStartIndex + i);
        }

        // Store over-leveled indices in PDC for lockout listener
        String indicesStr = overLeveledIndices.stream()
            .map(String::valueOf)
            .collect(Collectors.joining(","));
        wt.getPersistentDataContainer().set(
            CustomKeys.ENCHANTER_LOCKOUT,
            PersistentDataType.STRING,
            indicesStr
        );

        return trades;
    }

    private static ItemStack buildPotion(PotionOption opt) {
        boolean amplified = opt.canBeAmplified() && RANDOM.nextBoolean();
        int ticks = amplified ? opt.amplifiedTicks() : opt.extendedTicks();
        int amplifier = amplified ? 1 : 0;

        ItemStack potion = new ItemStack(Material.POTION);
        PotionMeta meta = (PotionMeta) potion.getItemMeta();
        meta.setBasePotionType(opt.baseType());
        meta.addCustomEffect(new PotionEffect(opt.effectType(), ticks, amplifier, false, true, true), true);
        potion.setItemMeta(meta);
        return potion;
    }

    private static ItemStack buildEnchantedBook(Enchantment enchantment, int level) {
        ItemStack book = new ItemStack(Material.ENCHANTED_BOOK);
        EnchantmentStorageMeta meta = (EnchantmentStorageMeta) book.getItemMeta();
        meta.addStoredEnchant(enchantment, level, true);
        book.setItemMeta(meta);
        return book;
    }

    private static ItemStack buildEnchantedItem(Material material, Enchantment enchantment, int level) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        meta.addEnchant(enchantment, level, true);
        item.setItemMeta(meta);
        return item;
    }

    /**
     * Buy trade - player gives emeralds, receives result.
     */
    private static MerchantRecipe buy(ItemStack result, int maxUses, int emeraldCost) {
        MerchantRecipe recipe = new MerchantRecipe(result, maxUses);
        recipe.addIngredient(new ItemStack(Material.EMERALD, emeraldCost));
        return recipe;
    }

    /**
     * Sell trade - player gives input item, receives emeralds.
     */
    private static MerchantRecipe sell(int emeraldPayment, int maxUses, ItemStack input) {
        MerchantRecipe recipe = new MerchantRecipe(new ItemStack(Material.EMERALD, emeraldPayment), maxUses);
        recipe.addIngredient(input);
        return recipe;
    }

    private static <T> T pickRandom(T[] array) {
        return array[RANDOM.nextInt(array.length)];
    }

    private static <T> List<T> shuffledList(T[] array) {
        List<T> list = new ArrayList<>(Arrays.asList(array));
        Collections.shuffle(list, RANDOM);
        return list;
    }
}
