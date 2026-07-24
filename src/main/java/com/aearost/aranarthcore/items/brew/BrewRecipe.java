package com.aearost.aranarthcore.items.brew;

import org.bukkit.Color;

public enum BrewRecipe {

    // ─── COMMON tier ──────────────────────────────────────────────────────────
    // Difficulty 1–4. Unlocked through the brew shop.
    POTATO_SOUP( "potato_soup",  Tier.COMMON, 0,     -1, true),   // diff 1, INSTANT_HEALTH/1
    HOT_CHOC(    "hot_choc",     Tier.COMMON, 0,     -1, true),   // diff 2, HASTE+REGEN
    WHEAT_BEER(  "wheatbeer",    Tier.COMMON, 0,     -1, true),   // diff 1, age 2 Birch
    BEER(        "beer",         Tier.COMMON, 7000,  -1, false),  // diff 1, age 3
    PALE_ALE(    "pale_ale",     Tier.COMMON, 8000,  -1, false),  // diff 2, age 4 Pale Oak
    COFFEE(      "coffee",       Tier.COMMON, 9000,  -1, false),  // diff 3, REGEN+SPEED
    PUMPKIN_BEER("pumpkin_beer", Tier.COMMON, 10000, -1, false),  // diff 3, age 5 Oak
    MEAD(        "mead",         Tier.COMMON, 11000, -1, false),  // diff 2, age 4 Oak
    ICED_COFFEE( "iced_coffee",  Tier.COMMON, 12000, -1, false),  // diff 4, REGEN+SPEED
    HONEY_MEAD(  "honey_mead",   Tier.COMMON, 14000, -1, false),  // diff 3, age 6 Oak, ABSORPTION
    CIDRE(       "cidre",        Tier.COMMON, 15000, -1, false),  // diff 4, age 3
    DARK_BEER(   "darkbeer",     Tier.COMMON, 0,     -1, false),  // diff 2, age 8 Dark Oak
    APPLE_MEAD(  "ap_mead",      Tier.COMMON, 0,     -1, false),  // diff 4, age 4 Oak, WATER_BREATHING
    WINE(        "wine",         Tier.COMMON, 0,     -1, false),  // diff 4, age 20
    VODKA(       "vodka",        Tier.COMMON, 0,     -1, false),  // diff 4, distillruns 3, WEAKNESS+POISON
    WHITE_JADE_TEA(  "atla4",  Tier.COMMON, 0, 1, false),  // POISON+NAUSEA (lore-significant)
    ONION_BANANA(    "atla16", Tier.COMMON, 0, 2, false),  // NAUSEA+REGEN (lore-significant)
    CACTUS_JUICE(    "atla1",  Tier.COMMON, 0, 1, false),  // NAUSEA+BLINDNESS+SPEED
    JASMINE_TEA(     "atla3",  Tier.COMMON, 0, 2, false),  // REGEN+ABSORPTION/120s
    SKY_BISON_CREAM( "atla7",  Tier.COMMON, 0, 2, false),  // JUMP_BOOST/2/120s+SLOW_FALLING
    CABBAGE_WINE(    "atla11", Tier.COMMON, 0, 1, false),  // SATURATION
    GINSENG_TEA(     "atla12", Tier.COMMON, 0, 1, false),  // SPEED+HASTE
    GREEN_TEA(       "atla14", Tier.COMMON, 0, 1, false),  // SPEED+SATURATION
    OOLONG_TEA(      "atla15", Tier.COMMON, 0, 1, false),  // HASTE+RESISTANCE

    // ─── RARE tier ────────────────────────────────────────────────────────────
    // Difficulty 5–6. Unlocked through vote crates.
    CHERRY_WINE(     "cherry_wine",      Tier.RARE, 0, -1, false),  // diff 5, age 16 Cherry
    GLOW_WINE(       "glow_wine",        Tier.RARE, 0, -1, false),  // diff 5, age 8 Pale Oak, GLOWING+NIGHT_VISION
    MELON_VODKA(     "melon_vodka",      Tier.RARE, 0, -1, false),  // diff 5, distillruns 3
    BEETROOT_SPIRIT( "beetroot_spirit",  Tier.RARE, 0, -1, false),  // diff 5, distillruns 3
    SAKE(            "sake",             Tier.RARE, 0, -1, false),  // diff 5, distillruns 1, age 3 Bamboo
    CACHACA(         "cachaca",          Tier.RARE, 0, -1, false),  // diff 5, distillruns 2, age 4 Jungle
    APPLE_LIQUOR(    "apple_liquor",     Tier.RARE, 0, -1, false),  // diff 5, distillruns 3, age 6 Acacia
    DANDELION_LIQUOR("dandelion_liquor", Tier.RARE, 0, -1, false),  // diff 5, distillruns 2, age 4 Birch
    TEQUILA(         "tequila",          Tier.RARE, 0, -1, false),  // diff 5, distillruns 2, age 12 Birch
    GIN(             "gin",              Tier.RARE, 0, -1, false),  // diff 6, distillruns 2
    CHAMPAGNE(       "champagne",        Tier.RARE, 0, -1, false),  // diff 6, distillruns 1, age 4
    RUM(             "rum",              Tier.RARE, 0, -1, false),  // diff 6, distillruns 2, age 14, FIRE_RESISTANCE+POISON
    MANGROVE_RUM(    "mangrove_rum",     Tier.RARE, 0, -1, false),  // diff 6, distillruns 2, age 12 Mangrove
    BRANDY(          "brandy",           Tier.RARE, 0, -1, false),  // diff 6, distillruns 2, age 12 Oak
    POPPY_LIQUOR(    "poppy_liquor",     Tier.RARE, 0, -1, false),  // diff 6, distillruns 2, age 6 Acacia
    GOLDEN_VODKA(    "g_vodka",          Tier.RARE, 0, -1, false),  // diff 6, distillruns 3, WEAKNESS+POISON
    WARPED_ALE(      "warped_ale",       Tier.RARE, 0, -1, false),  // diff 6, age 6 Warped, NIGHT_VISION
    MOONSHINE(       "moonshine",        Tier.RARE, 0, -1, false),  // diff 6, distillruns 4, age 2 Cut Copper, BLINDNESS+POISON
    SEAWEED_GROG(    "atla5",            Tier.RARE, 0, 1,  false),  // WATER_BREATHING/180s+RESISTANCE
    RICE_WINE(       "atla6",            Tier.RARE, 0, 1,  false),  // diff 6, age 8, RESISTANCE+HASTE
    WHITE_DRAGON_TEA("atla13",           Tier.RARE, 0, 1,  false),  // REGEN+LUCK/160s

    // ─── LEGENDARY tier ───────────────────────────────────────────────────────
    // Difficulty 7–10. Unlocked through weekly quests (rank 5+, 33% chance).
    EGGNOG(            "eggnog",       Tier.LEGENDARY, 0, -1, false),  // diff 4→end-tier Advocaat
    WHISKEY(           "whiskey",      Tier.LEGENDARY, 0, -1, false),  // diff 7, distillruns 2, age 18 Spruce
    FIRE_WHISKEY(      "fire_whiskey", Tier.LEGENDARY, 0, -1, false),  // diff 7, distillruns 3, age 18 Spruce
    BOURBON(           "bourbon",      Tier.LEGENDARY, 0, -1, false),  // diff 7, distillruns 2, age 22 Oak
    BAMBOO_LIQUOR(     "bamboo_liquor",Tier.LEGENDARY, 0, -1, false),  // diff 7, distillruns 2, age 6 Bamboo
    SHROOM_VODKA(      "shroom_vodka", Tier.LEGENDARY, 0, -1, false),  // diff 7, distillruns 5, WEAKNESS+NAUSEA+NIGHT_VISION
    ABSINTHE(          "absinthe",     Tier.LEGENDARY, 0, -1, false),  // diff 8, distillruns 6, POISON/25
    NETHER_GROG(       "nether_grog",  Tier.LEGENDARY, 0, -1, false),  // diff 8, distillruns 2, age 10 Crimson, FIRE_RESISTANCE
    GREEN_ABSINTHE(    "gr_absinthe",  Tier.LEGENDARY, 0, -1, false),  // diff 9, distillruns 6, POISON/40+INSTANT_DAMAGE
    FIRE_WHISKEY_ATLA( "atla2",        Tier.LEGENDARY, 0, 2,  false),  // diff 7, FIRE_RESISTANCE+STRENGTH
    SWAMP_BREW(        "atla8",        Tier.LEGENDARY, 0, 2,  false),  // NIGHT_VISION/180s
    OASIS_WATER(       "atla9",        Tier.LEGENDARY, 0, 2,  false),  // diff 8, REGEN/2+ABSORPTION/2+INSTANT_HEALTH
    COMET_BRANDY(      "atla10",       Tier.LEGENDARY, 0, 2,  false),  // diff 9, STRENGTH/2+SPEED/2+FIRE_RESISTANCE
    SPIRIT_TONIC(      "atla17",       Tier.LEGENDARY, 0, 1,  false),  // diff 8, SLOW_FALLING+NIGHT_VISION+INVISIBILITY
    AVATAR_ELIXIR(     "atla18",       Tier.LEGENDARY, 0, 3,  false),  // diff 10, RESISTANCE/2+STRENGTH/2+SPEED
    BADGERMOLE_BREW(   "atla19",       Tier.LEGENDARY, 0, -1, false),  // diff 10, HASTE/4-5+NIGHT_VISION, Mangrove age 16
    PAI_SHO_CORDIAL(   "atla20",       Tier.LEGENDARY, 0, -1, false),  // diff 9, LUCK/2+ABSORPTION/2+RESISTANCE, Pale Oak age 15
    FULL_MOON_VINTAGE( "atla21",       Tier.LEGENDARY, 0, -1, false),  // diff 9, STRENGTH/2+NIGHT_VISION, Crimson age 14
    SUN_WARRIOR_MEAD(  "atla22",       Tier.LEGENDARY, 0, -1, false),  // diff 9, FIRE_RESISTANCE+STRENGTH/2+REGEN, Acacia age 12
    NEUTRAL_JING_JULEP("atla23",       Tier.LEGENDARY, 0, -1, false),  // diff 8, JUMP_BOOST/3+HASTE/2, Bamboo age 8
    FORBIDDEN_INK(     "atla24",       Tier.LEGENDARY, 0, -1, false);  // diff 9, NIGHT_VISION+LUCK+SLOW_FALLING, Dark Oak age 12

    public enum Tier {
        COMMON, RARE, LEGENDARY
    }

    /** Config-sourced data injected at startup by BrewRecipeUtils.initialize(). */
    public static class RuntimeData {
        public final String displayName;
        public final String[] ingredients;
        public final String colorHex;
        public final int cookingTime;
        public final int distillRuns;
        public final int age;
        public final int wood;

        public RuntimeData(String displayName, String[] ingredients, String colorHex,
                           int cookingTime, int distillRuns, int age, int wood) {
            this.displayName = displayName;
            this.ingredients = ingredients;
            this.colorHex = colorHex;
            this.cookingTime = cookingTime;
            this.distillRuns = distillRuns;
            this.age = age;
            this.wood = wood;
        }
    }

    private final String id;
    private final Tier tier;
    private final int price;
    /** Index into ingredients[] that is secret (obfuscated in lore), or -1 for none. */
    private final int secretIngredientIndex;
    /** True if this recipe is unlocked for all players by default and excluded from the shop. */
    private final boolean defaultUnlocked;

    private RuntimeData runtimeData;

    BrewRecipe(String id, Tier tier, int price, int secretIngredientIndex, boolean defaultUnlocked) {
        this.id = id;
        this.tier = tier;
        this.price = price;
        this.secretIngredientIndex = secretIngredientIndex;
        this.defaultUnlocked = defaultUnlocked;
    }

    public String getId() { return id; }
    public Tier getTier() { return tier; }
    public int getPrice() { return price; }
    public int getSecretIngredientIndex() { return secretIngredientIndex; }
    public boolean isDefaultUnlocked() { return defaultUnlocked; }

    public String getDisplayName() {
        return runtimeData != null ? runtimeData.displayName : id;
    }

    public String[] getIngredients() {
        return runtimeData != null ? runtimeData.ingredients : new String[0];
    }

    public int getCookingTime() { return runtimeData != null ? runtimeData.cookingTime : 0; }
    public int getDistillRuns() { return runtimeData != null ? runtimeData.distillRuns : 0; }
    public int getAge()         { return runtimeData != null ? runtimeData.age : 0; }
    public int getWood()        { return runtimeData != null ? runtimeData.wood : 0; }

    public Color getPotionColor() {
        if (runtimeData == null) return Color.fromRGB(136, 136, 255);
        try {
            String hex = runtimeData.colorHex.length() == 6 ? runtimeData.colorHex : "8888FF";
            int r = Integer.parseInt(hex.substring(0, 2), 16);
            int g = Integer.parseInt(hex.substring(2, 4), 16);
            int b = Integer.parseInt(hex.substring(4, 6), 16);
            return Color.fromRGB(r, g, b);
        } catch (Exception ignored) {
            return Color.fromRGB(136, 136, 255);
        }
    }

    /** Called by BrewRecipeUtils.initialize() to inject config-sourced display data. */
    public void injectRuntimeData(RuntimeData data) {
        this.runtimeData = data;
    }

    /** Looks up a BrewRecipe by its BreweryX config key. Returns null if not found. */
    public static BrewRecipe fromId(String id) {
        for (BrewRecipe r : values()) {
            if (r.id.equals(id)) return r;
        }
        return null;
    }
}
