package com.aearost.aranarthcore.items.brew;

import org.bukkit.Color;

public enum BrewRecipe {

    // ─── BASIC tier ───────────────────────────────────────────────────────────
    // Simple brews - no distilling, short/no aging, mild or no effects
    POTATO_SOUP("potato_soup", Tier.BASIC, 0,     -1, true),   // diff 1, INSTANT_HEALTH/1
    HOT_CHOC(   "hot_choc",   Tier.BASIC, 0,     -1, true),   // diff 2, HASTE/40
    WHEAT_BEER( "wheatbeer",  Tier.BASIC, 0,     -1, true),   // diff 1, no effects
    BEER(       "beer",       Tier.BASIC, 7000,  -1, false),  // diff 1, no effects
    COFFEE(     "coffee",     Tier.BASIC, 9000,  -1, false),  // diff 3, mild REGEN+SPEED
    MEAD(       "mead",       Tier.BASIC, 11000, -1, false),  // diff 2, age 4, no effects
    ICED_COFFEE("iced_coffee",Tier.BASIC, 12000, -1, false),  // diff 4, no distill/age, mild REGEN+SPEED
    EGGNOG(     "eggnog",     Tier.BASIC, 13000, -1, false),  // diff 4, age 3, no effects
    CIDRE(      "cidre",      Tier.BASIC, 15000, -1, false),  // diff 4, age 3, no effects

    // ─── MIDDLE tier ──────────────────────────────────────────────────────────
    // Moderate complexity - specific barrel wood, distillruns 1–3, longer aging, or notable single effect.
    DARK_BEER(   "darkbeer",     Tier.MIDDLE, 0, -1, false),  // diff 2 but age 8 Dark Oak specifically
    APPLE_MEAD(  "ap_mead",     Tier.MIDDLE, 0, -1, false),  // diff 4, WATER_BREATHING/2/150s
    WINE(        "wine",        Tier.MIDDLE, 0, -1, false),  // diff 4, age 20 — very long
    VODKA(       "vodka",       Tier.MIDDLE, 0, -1, false),  // diff 4, distillruns 3, WEAKNESS+POISON
    APPLE_LIQUOR("apple_liquor",Tier.MIDDLE, 0, -1, false),  // diff 5, distillruns 3, age 6 Acacia
    TEQUILA(     "tequila",     Tier.MIDDLE, 0, -1, false),  // diff 5, distillruns 2, age 12 Birch
    GIN(         "gin",         Tier.MIDDLE, 0, -1, false),  // diff 6, distillruns 2
    RUM(         "rum",         Tier.MIDDLE, 0, -1, false),  // diff 6, distillruns 2, age 14, FIRE_RESISTANCE+POISON
    GOLDEN_VODKA("g_vodka",     Tier.MIDDLE, 0, -1, false),  // diff 6, distillruns 3, WEAKNESS+POISON

    // ─── HIGHER tier ──────────────────────────────────────────────────────────
    // High complexity - distillruns 4+, age 16+ OR powerful multi-effects OR Avatar theme.
    WHISKEY(       "whiskey",     Tier.HIGHER, 0, -1, false),  // diff 7, distillruns 2, age 18 Spruce
    FIRE_WHISKEY(  "fire_whiskey",Tier.HIGHER, 0, -1, false),  // diff 7, distillruns 3, age 18 Spruce
    SHROOM_VODKA(  "shroom_vodka",Tier.HIGHER, 0, -1, false),  // diff 7, distillruns 5, WEAKNESS/80+NAUSEA+NIGHT_VISION
    ABSINTHE(      "absinthe",    Tier.HIGHER, 0, -1, false),  // diff 8, distillruns 6, POISON/25
    GREEN_ABSINTHE("gr_absinthe", Tier.HIGHER, 0, -1, false),  // diff 9, distillruns 6, POISON/40+INSTANT_DAMAGE

    // Avatar brews - all HIGHER by theme regardless of mechanical complexity
    CACTUS_JUICE(     "atla1",  Tier.HIGHER, 0, 1,  false),  // NAUSEA+BLINDNESS+SPEED
    FIRE_WHISKEY_ATLA("atla2",  Tier.HIGHER, 0, 2,  false),  // diff 7, FIRE_RESISTANCE+STRENGTH
    JASMINE_TEA(      "atla3",  Tier.HIGHER, 0, 2,  false),  // REGEN+ABSORPTION/120s
    WHITE_JADE_TEA(   "atla4",  Tier.HIGHER, 0, 1,  false),  // POISON+NAUSEA (lore-significant)
    SEAWEED_GROG(     "atla5",  Tier.HIGHER, 0, 1,  false),  // WATER_BREATHING/180s+RESISTANCE
    RICE_WINE(        "atla6",  Tier.HIGHER, 0, 1,  false),  // diff 6, age 8, RESISTANCE+HASTE
    SKY_BISON_CREAM(  "atla7",  Tier.HIGHER, 0, 2,  false),  // JUMP_BOOST/2/120s+SLOW_FALLING
    SWAMP_BREW(       "atla8",  Tier.HIGHER, 0, 2,  false),  // NIGHT_VISION/180s
    OASIS_WATER(      "atla9",  Tier.HIGHER, 0, 2,  false),  // diff 8, REGEN/2+ABSORPTION/2+INSTANT_HEALTH
    COMET_BRANDY(     "atla10", Tier.HIGHER, 0, 2,  false),  // diff 9, STRENGTH/2+SPEED/2+FIRE_RESISTANCE
    CABBAGE_WINE(     "atla11", Tier.HIGHER, 0, 1,  false),  // SATURATION
    GINSENG_TEA(      "atla12", Tier.HIGHER, 0, 1,  false),  // SPEED+HASTE
    WHITE_DRAGON_TEA( "atla13", Tier.HIGHER, 0, 1,  false),  // REGEN+LUCK/160s
    GREEN_TEA(        "atla14", Tier.HIGHER, 0, 1,  false),  // SPEED+SATURATION
    OOLONG_TEA(       "atla15", Tier.HIGHER, 0, 1,  false),  // HASTE+RESISTANCE
    ONION_BANANA(     "atla16", Tier.HIGHER, 0, 2,  false),  // NAUSEA+REGEN (lore-significant)
    SPIRIT_TONIC(       "atla17", Tier.HIGHER, 0, 1,  false),  // diff 8, SLOW_FALLING+NIGHT_VISION+INVISIBILITY
    AVATAR_ELIXIR(      "atla18", Tier.HIGHER, 0, 3,  false),  // diff 10, RESISTANCE/2+STRENGTH/2+SPEED
    BADGERMOLE_BREW(    "atla19", Tier.HIGHER, 0, -1, false),  // diff 10, HASTE/4-5+NIGHT_VISION, Mangrove age 16
    PAI_SHO_CORDIAL(    "atla20", Tier.HIGHER, 0, -1, false),  // diff 9, LUCK/2+ABSORPTION/2+RESISTANCE, Pale Oak age 15
    FULL_MOON_VINTAGE(  "atla21", Tier.HIGHER, 0, -1, false),  // diff 9, STRENGTH/2+NIGHT_VISION, Crimson age 14
    SUN_WARRIOR_MEAD(   "atla22", Tier.HIGHER, 0, -1, false),  // diff 9, FIRE_RESISTANCE+STRENGTH/2+REGEN, Acacia age 12
    NEUTRAL_JING_JULEP( "atla23", Tier.HIGHER, 0, -1, false),  // diff 8, JUMP_BOOST/3+HASTE/2, Bamboo age 8
    FORBIDDEN_INK(      "atla24", Tier.HIGHER, 0, -1, false);  // diff 9, NIGHT_VISION+LUCK+SLOW_FALLING, Dark Oak age 12

    public enum Tier {
        BASIC, MIDDLE, HIGHER
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
