package com.aearost.aranarthcore.items.produce;

import org.bukkit.Material;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Every fruit, berry, crop, seed and nut grown by Aranarth's fruit trees, berry bushes and crops.
 */
public enum ProduceType {

    // Tree fruit (the vanilla Apple is the Red Apple)
    GREEN_APPLE("green_apple", Kind.FRUIT, true),
    PEAR("pear", Kind.FRUIT, true),
    CHERRY("cherry", Kind.FRUIT, true),
    DATE("date", Kind.FRUIT, false),
    COCONUT("coconut", Kind.FRUIT, false),
    BANANA("banana", Kind.FRUIT, true),
    LEMON("lemon", Kind.FRUIT, true),
    ORANGE("orange", Kind.FRUIT, true),
    LIME("lime", Kind.FRUIT, true),
    MANGO("mango", Kind.FRUIT, true),
    OLIVE("olive", Kind.FRUIT, false),
    PLUM("plum", Kind.FRUIT, true),
    PEACH("peach", Kind.FRUIT, true),
    POMEGRANATE("pomegranate", Kind.FRUIT, true),
    APRICOT("apricot", Kind.FRUIT, true),
    FIG("fig", Kind.FRUIT, true),
    AVOCADO("avocado", Kind.FRUIT, true),
    PERSIMMON("persimmon", Kind.FRUIT, true),
    LYCHEE("lychee", Kind.FRUIT, true),

    // Berries
    BLUEBERRY("blueberry", Kind.BERRY, false),
    RASPBERRY("raspberry", Kind.BERRY, false),
    BLACKBERRY("blackberry", Kind.BERRY, false),
    STRAWBERRY("strawberry", Kind.BERRY, false),

    // Crops
    CORN("corn", Kind.CROP, false),
    TOMATO("tomato", Kind.CROP, false),
    RICE("rice", Kind.CROP, false),
    RICE_STALK("rice_stalk", Kind.CROP, false),
    GREEN_GRAPES("green_grapes", Kind.CROP, false),
    RED_GRAPES("red_grapes", Kind.CROP, false),
    PINEAPPLE("pineapple", Kind.CROP, false),
    GREEN_BELL_PEPPER("green_bell_pepper", Kind.CROP, false),
    YELLOW_BELL_PEPPER("yellow_bell_pepper", Kind.CROP, false),
    ORANGE_BELL_PEPPER("orange_bell_pepper", Kind.CROP, false),
    RED_BELL_PEPPER("red_bell_pepper", Kind.CROP, false),
    CABBAGE("cabbage", Kind.CROP, false),
    LETTUCE("lettuce", Kind.CROP, false),
    ONION("onion", Kind.CROP, false),
    GARLIC("garlic", Kind.CROP, false),
    SWEET_POTATO("sweet_potato", Kind.CROP, false),
    CANTALOUPE_SLICE("cantaloupe_slice", Kind.CROP, false),
    KIWI("kiwi", Kind.CROP, false),
    COFFEE_BEANS("coffee_beans", Kind.CROP, false),

    // Seeds
    CORN_SEEDS("corn_seeds", Kind.SEED, false),
    TOMATO_SEEDS("tomato_seeds", Kind.SEED, false),
    BELL_PEPPER_SEEDS("bell_pepper_seeds", Kind.SEED, false),
    LETTUCE_SEEDS("lettuce_seeds", Kind.SEED, false),
    CABBAGE_SEEDS("cabbage_seeds", Kind.SEED, false),
    CANTALOUPE_SEEDS("cantaloupe_seeds", Kind.SEED, false),
    KIWI_SEEDS("kiwi_seeds", Kind.SEED, false),

    // Nuts
    ACORN("acorn", Kind.NUT, false),
    ROASTED_ACORN("roasted_acorn", Kind.NUT, false);

    /**
     * What kind of produce it is.
     */
    public enum Kind {
        FRUIT, BERRY, CROP, SEED, NUT
    }

    private final String id;
    private final Kind kind;
    private final boolean inVoteCrate;

    ProduceType(String id, Kind kind, boolean inVoteCrate) {
        this.id = id;
        this.kind = kind;
        this.inVoteCrate = inVoteCrate;
    }

    public String getId() {
        return id;
    }

    public Kind getKind() {
        return kind;
    }

    /**
     * @return Whether the produce can be rolled from a Vote Crate. Fruit of the rare trees (Date, Coconut, Olive) cannot.
     */
    public boolean isInVoteCrate() {
        return inVoteCrate;
    }

    /**
     * The hunger and saturation a food restores, and how long it takes to eat.
     * @param nutrition The hunger points restored, where 20 is a full bar.
     * @param saturation The saturation restored.
     * @param seconds How long it takes to eat.
     */
    public record Food(int nutrition, float saturation, float seconds) {}

    /**
     * Provides what the produce restores when eaten, or null if it is not eaten on its own.
     */
    public Food getFood() {
        return switch (this) {
            case GREEN_APPLE -> new Food(4, 2.4F, 1.6F);
            case PEAR -> new Food(4, 2.8F, 1.6F);
            case CHERRY -> new Food(1, 0.4F, 0.8F);
            case DATE -> new Food(2, 1.6F, 0.8F);
            case BANANA -> new Food(4, 3.0F, 1.6F);
            case ORANGE, LEMON, LIME -> new Food(3, 2.4F, 1.6F);
            case MANGO -> new Food(5, 3.6F, 1.6F);
            case OLIVE -> new Food(1, 0.6F, 0.8F);
            case PLUM -> new Food(3, 1.8F, 1.6F);
            case PEACH -> new Food(4, 2.4F, 1.6F);
            case POMEGRANATE -> new Food(3, 2.4F, 1.6F);
            case APRICOT -> new Food(2, 1.2F, 1.2F);
            case FIG -> new Food(2, 1.6F, 1.2F);
            case AVOCADO -> new Food(4, 4.8F, 1.6F);
            case PERSIMMON -> new Food(4, 2.4F, 1.6F);
            case LYCHEE -> new Food(1, 0.6F, 0.8F);
            case BLUEBERRY, RASPBERRY -> new Food(2, 0.4F, 0.8F);
            case BLACKBERRY -> new Food(2, 0.6F, 0.8F);
            case STRAWBERRY -> new Food(2, 1.2F, 0.8F);
            case CORN -> new Food(3, 2.4F, 1.6F);
            case TOMATO -> new Food(3, 1.8F, 1.6F);
            case GREEN_GRAPES, RED_GRAPES -> new Food(2, 0.6F, 0.8F);
            case PINEAPPLE -> new Food(4, 3.2F, 1.6F);
            case GREEN_BELL_PEPPER, YELLOW_BELL_PEPPER, ORANGE_BELL_PEPPER, RED_BELL_PEPPER -> new Food(3, 1.8F, 1.6F);
            case CABBAGE -> new Food(3, 2.4F, 1.6F);
            case LETTUCE -> new Food(2, 1.2F, 1.2F);
            case CANTALOUPE_SLICE -> new Food(2, 1.2F, 1.6F);
            case KIWI -> new Food(3, 1.8F, 1.2F);
            case SWEET_POTATO -> new Food(1, 0.6F, 1.6F);
            case ROASTED_ACORN -> new Food(2, 1.6F, 0.8F);
            default -> null;
        };
    }

    /**
     * Provides the vanilla item the produce is made from.
     */
    public Material getBaseMaterial() {
        return switch (this) {
            case BLUEBERRY, RASPBERRY, BLACKBERRY, STRAWBERRY, GREEN_GRAPES, RED_GRAPES -> Material.SWEET_BERRIES;
            case CANTALOUPE_SLICE -> Material.MELON_SLICE;
            case TOMATO, GREEN_BELL_PEPPER, YELLOW_BELL_PEPPER, ORANGE_BELL_PEPPER, RED_BELL_PEPPER, CABBAGE, LETTUCE,
                 ONION, GARLIC -> Material.BEETROOT;
            case SWEET_POTATO -> Material.POTATO;
            case CORN -> Material.CARROT;
            case RICE_STALK -> Material.WHEAT;
            case RICE, CORN_SEEDS, TOMATO_SEEDS, BELL_PEPPER_SEEDS, LETTUCE_SEEDS, CABBAGE_SEEDS, KIWI_SEEDS -> Material.WHEAT_SEEDS;
            case CANTALOUPE_SEEDS -> Material.MELON_SEEDS;
            case COFFEE_BEANS -> Material.COCOA_BEANS;
            // Acorns are roasted by matching the exact item, so they keep a base no vanilla recipe uses
            case ACORN, ROASTED_ACORN -> Material.FIREWORK_STAR;
            // Tree fruit, Kiwi and Pineapple
            default -> Material.APPLE;
        };
    }

    /**
     * Determines if the item shows how the season changes its growth and yield.
     */
    public boolean hasSeasonalLore() {
        return kind != Kind.NUT;
    }

    /**
     * @return The lang key of the produce's name.
     */
    public String getLangKey() {
        return "produce." + id;
    }

    /**
     * @return The name used by /ac give, such as GreenApple.
     */
    public String getGiveName() {
        // Bell peppers are named by colour last, so they sort together with Bell Pepper Seeds
        switch (this) {
            case GREEN_BELL_PEPPER -> {
                return "BellPepperGreen";
            }
            case YELLOW_BELL_PEPPER -> {
                return "BellPepperYellow";
            }
            case ORANGE_BELL_PEPPER -> {
                return "BellPepperOrange";
            }
            case RED_BELL_PEPPER -> {
                return "BellPepperRed";
            }
            default -> {
            }
        }
        StringBuilder name = new StringBuilder();
        for (String part : id.split("_")) {
            name.append(Character.toUpperCase(part.charAt(0))).append(part.substring(1));
        }
        return name.toString();
    }

    /**
     * Provides the produce type from its persistent data id.
     * @param id The id.
     * @return The produce type, or null if there is none.
     */
    public static ProduceType fromId(String id) {
        for (ProduceType type : values()) {
            if (type.id.equals(id)) {
                return type;
            }
        }
        return null;
    }

    /**
     * Provides the produce type from its /ac give name.
     * @param giveName The give name, such as GreenApple.
     * @return The produce type, or null if there is none.
     */
    public static ProduceType fromGiveName(String giveName) {
        for (ProduceType type : values()) {
            if (type.getGiveName().equals(giveName)) {
                return type;
            }
        }
        return null;
    }

    /**
     * @return The names of every produce type used by /ac give.
     */
    public static List<String> getGiveNames() {
        List<String> names = new ArrayList<>();
        for (ProduceType type : values()) {
            names.add(type.getGiveName());
        }
        return Collections.unmodifiableList(names);
    }

    /**
     * @return Every produce type that can be rolled from a Vote Crate.
     */
    public static List<ProduceType> getVoteCrateFruit() {
        List<ProduceType> fruit = new ArrayList<>();
        for (ProduceType type : values()) {
            if (type.inVoteCrate) {
                fruit.add(type);
            }
        }
        return fruit;
    }
}
