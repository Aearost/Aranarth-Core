package com.aearost.aranarthcore.crops;

import com.aearost.aranarthcore.items.produce.ProduceType;
import org.bukkit.Material;

/**
 * Every custom crop, berry bush and vine: what plants it, what it grows on, how it grows and what it gives.
 */
public enum CropType {

    // Berry bushes: a barren sweet berry bush that shows a few small berries, then many, picked by right-clicking
    BLUEBERRY(ProduceType.BLUEBERRY, Soil.DIRT, HarvestMode.REGROW, 4, 25, 1, 0, ProduceType.BLUEBERRY, 5, 10, null, 50, false, Material.SWEET_BERRIES),
    RASPBERRY(ProduceType.RASPBERRY, Soil.DIRT, HarvestMode.REGROW, 4, 25, 1, 0, ProduceType.RASPBERRY, 5, 10, null, 100, false, Material.SWEET_BERRIES),
    BLACKBERRY(ProduceType.BLACKBERRY, Soil.DIRT, HarvestMode.REGROW, 4, 25, 1, 0, ProduceType.BLACKBERRY, 5, 10, null, 200, true, Material.SWEET_BERRIES),
    STRAWBERRY(ProduceType.STRAWBERRY, Soil.DIRT, HarvestMode.REGROW, 4, 20, 1, 0, ProduceType.STRAWBERRY, 5, 10, null, 25, false, Material.SWEET_BERRIES),
    // Vegetables that keep giving: tomatoes and peppers fruit a few times, then stay barren until broken
    TOMATO(ProduceType.TOMATO_SEEDS, Soil.FARMLAND, HarvestMode.REGROW, 3, 30, 1, 2, ProduceType.TOMATO, 2, 4, ProduceType.TOMATO_SEEDS, 0, false, Material.WHEAT_SEEDS),
    BELL_PEPPER(ProduceType.BELL_PEPPER_SEEDS, Soil.FARMLAND, HarvestMode.REGROW, 6, 35, 1, 3, ProduceType.GREEN_BELL_PEPPER, 2, 4, ProduceType.BELL_PEPPER_SEEDS, 0, false, Material.WHEAT_SEEDS),
    // Field crops, harvested by breaking them and planted again
    CORN(ProduceType.CORN_SEEDS, Soil.FARMLAND, HarvestMode.REPLANT, 7, 40, 0, 0, ProduceType.CORN, 2, 4, ProduceType.CORN_SEEDS, 0, false, Material.WHEAT_SEEDS),
    RICE(ProduceType.RICE, Soil.MUD_BANK, HarvestMode.REPLANT, 3, 35, 0, 0, ProduceType.RICE_STALK, 2, 4, null, 0, false, Material.WHEAT_SEEDS),
    ONION(ProduceType.ONION, Soil.FARMLAND, HarvestMode.REPLANT, 3, 30, 0, 0, ProduceType.ONION, 1, 3, null, 0, false, Material.BEETROOT_SEEDS),
    GARLIC(ProduceType.GARLIC, Soil.FARMLAND, HarvestMode.REPLANT, 3, 30, 0, 0, ProduceType.GARLIC, 1, 3, null, 0, false, Material.BEETROOT_SEEDS),
    SWEET_POTATO(ProduceType.SWEET_POTATO, Soil.FARMLAND, HarvestMode.REPLANT, 4, 30, 0, 0, ProduceType.SWEET_POTATO, 1, 4, null, 0, false, Material.POTATO),
    CABBAGE(ProduceType.CABBAGE_SEEDS, Soil.FARMLAND, HarvestMode.REPLANT, 4, 30, 0, 0, ProduceType.CABBAGE, 1, 1, ProduceType.CABBAGE_SEEDS, 0, false, Material.BEETROOT_SEEDS),
    LETTUCE(ProduceType.LETTUCE_SEEDS, Soil.FARMLAND, HarvestMode.REPLANT, 4, 20, 0, 0, ProduceType.LETTUCE, 1, 1, ProduceType.LETTUCE_SEEDS, 0, false, Material.BEETROOT_SEEDS),
    // A stem that grows its fruit beside it, again and again
    CANTALOUPE(ProduceType.CANTALOUPE_SEEDS, Soil.FARMLAND, HarvestMode.REGROW, 5, 30, 3, 0, ProduceType.CANTALOUPE_SLICE, 3, 5, null, 0, false, Material.MELON_SEEDS),
    // Vines planted beside a fence post, one on each side it should grow along, and shrubs
    GREEN_GRAPES(ProduceType.GREEN_GRAPES, Soil.DIRT, HarvestMode.REGROW, 3, 45, 1, 0, ProduceType.GREEN_GRAPES, 1, 24, null, 0, false, Material.SWEET_BERRIES),
    RED_GRAPES(ProduceType.RED_GRAPES, Soil.DIRT, HarvestMode.REGROW, 3, 45, 1, 0, ProduceType.RED_GRAPES, 1, 24, null, 0, false, Material.SWEET_BERRIES),
    KIWI(ProduceType.KIWI_SEEDS, Soil.DIRT, HarvestMode.REGROW, 3, 45, 1, 0, ProduceType.KIWI, 1, 24, null, 0, false, Material.SWEET_BERRIES),
    COFFEE(ProduceType.COFFEE_BEANS, Soil.DIRT, HarvestMode.REGROW, 4, 50, 2, 0, ProduceType.COFFEE_BEANS, 2, 5, null, 0, false, Material.SWEET_BERRIES),
    PINEAPPLE(ProduceType.PINEAPPLE, Soil.DIRT, HarvestMode.REGROW, 6, 90, 2, 0, ProduceType.PINEAPPLE, 1, 1, null, 0, false, Material.SWEET_BERRIES);

    public enum Soil {
        /** Grass, dirt, podzol and the like. */
        DIRT,
        /** Farmland. */
        FARMLAND,
        /** Mud or farmland right next to water, like a muddy river bank. */
        MUD_BANK
    }

    /**
     * How a crop is harvested.
     */
    public enum HarvestMode {
        /** Broken to harvest, then planted again. */
        REPLANT,
        /** Picked by right-clicking, or by breaking its fruit, then grows its fruit back. */
        REGROW
    }

    private final ProduceType plant;
    private final Soil soil;
    private final HarvestMode mode;
    private final int stages;
    private final int growMinutes;
    private final int regrowStage;
    private final int maxHarvests;
    private final ProduceType yield;
    private final int yieldMin;
    private final int yieldMax;
    private final ProduceType seeds;
    private final int herbalismLevel;
    private final boolean thorns;
    private final Material season;

    /**
     * @param stages How many stages it grows through, the last being ripe.
     * @param growMinutes How many real minutes it takes to grow from planted to ripe in a month of normal growth.
     * @param regrowStage The stage a regrowing crop goes back to once picked.
     * @param maxHarvests How many times it can be picked before it stays barren, or 0 for no limit.
     * @param seeds The seeds it drops when harvested or broken, or null if it is planted from what it gives.
     * @param herbalismLevel The Herbalism level needed to pick it, or 0 for none.
     * @param thorns Whether it hurts to walk through, like a sweet berry bush.
     * @param season The vanilla crop whose seasons it follows.
     */
    CropType(ProduceType plant, Soil soil, HarvestMode mode, int stages, int growMinutes, int regrowStage, int maxHarvests,
             ProduceType yield, int yieldMin, int yieldMax, ProduceType seeds, int herbalismLevel, boolean thorns,
             Material season) {
        this.plant = plant;
        this.soil = soil;
        this.mode = mode;
        this.stages = stages;
        this.growMinutes = growMinutes;
        this.regrowStage = regrowStage;
        this.maxHarvests = maxHarvests;
        this.yield = yield;
        this.yieldMin = yieldMin;
        this.yieldMax = yieldMax;
        this.seeds = seeds;
        this.herbalismLevel = herbalismLevel;
        this.thorns = thorns;
        this.season = season;
    }

    public ProduceType getPlant() {
        return plant;
    }

    public Soil getSoil() {
        return soil;
    }

    public HarvestMode getMode() {
        return mode;
    }

    public int getStages() {
        return stages;
    }

    public int getRipeStage() {
        return stages - 1;
    }

    /**
     * Provides the first stage it can be picked at.
     */
    public int getHarvestStage() {
        return this == BELL_PEPPER || isBerryBush() ? 2 : stages - 1;
    }

    /**
     * Determines if it is a berry bush, which shows its berries as tiny heads on the outside of a sweet berry bush.
     */
    public boolean isBerryBush() {
        return this == BLUEBERRY || this == RASPBERRY || this == BLACKBERRY || this == STRAWBERRY;
    }

    /**
     * Determines if it is a vine, planted beside a fence post and growing over the trellis that post is part of.
     */
    public boolean isVine() {
        return this == GREEN_GRAPES || this == RED_GRAPES || this == KIWI;
    }

    /**
     * Determines if each piece of its fruit grows, is picked and grows back on its own, like each bell pepper or coffee bean.
     */
    public boolean hasPieces() {
        return this == BELL_PEPPER || this == COFFEE;
    }

    /**
     * Provides the stage the plant itself grows to before its pieces of fruit grow on it.
     */
    public int getPlantStage() {
        return this == BELL_PEPPER ? 1 : getRipeStage();
    }

    /**
     * Determines if it is harvested by right-clicking it as well as by breaking it.
     */
    public boolean isPickedByClicking() {
        return this == CABBAGE || this == LETTUCE || this == GARLIC;
    }

    /**
     * Provides how long it takes to grow from planted to ripe in a month of normal growth.
     */
    public long getGrowMillis() {
        return growMinutes * 60_000L;
    }

    public long getStageMillis() {
        return growMinutes * 60_000L / Math.max(1, stages - 1);
    }

    public int getRegrowStage() {
        return regrowStage;
    }

    public int getMaxHarvests() {
        return maxHarvests;
    }

    public ProduceType getYield() {
        return yield;
    }

    public int getYieldMin() {
        return yieldMin;
    }

    public int getYieldMax() {
        return yieldMax;
    }

    public ProduceType getSeeds() {
        return seeds;
    }

    public int getHerbalismLevel() {
        return herbalismLevel;
    }

    public boolean hasThorns() {
        return thorns;
    }

    public Material getSeason() {
        return season;
    }

    public String getId() {
        return name().toLowerCase();
    }

    /**
     * Provides the crop that the item plants, or null if it plants none.
     */
    public static CropType fromPlant(ProduceType produce) {
        for (CropType type : values()) {
            if (type.plant == produce) {
                return type;
            }
        }
        return null;
    }

    /**
     * Provides the crop from its saved id, or null if there is none.
     */
    public static CropType fromId(String id) {
        for (CropType type : values()) {
            if (type.getId().equals(id)) {
                return type;
            }
        }
        return null;
    }

    /**
     * Provides the vanilla crop whose seasons a produce item follows: the crop it plants, or the crop it comes from.
     */
    public static Material seasonFor(ProduceType produce) {
        for (CropType type : values()) {
            if (type.plant == produce || type.yield == produce || type.seeds == produce) {
                return type.season;
            }
        }
        return Material.SWEET_BERRIES;
    }
}
