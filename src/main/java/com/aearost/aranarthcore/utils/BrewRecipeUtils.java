package com.aearost.aranarthcore.utils;

import com.aearost.aranarthcore.AranarthCore;
import com.aearost.aranarthcore.database.DatabaseManager;
import com.aearost.aranarthcore.items.brew.BrewRecipe;
import com.aearost.aranarthcore.network.NetworkManager;
import com.aearost.aranarthcore.objects.CustomKeys;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;

import java.io.File;
import java.io.IOException;
import java.text.NumberFormat;
import java.util.*;

/**
 * Manages brewery recipe unlocks per player, including file-based persistence.
 */
public class BrewRecipeUtils {

    private static final HashMap<UUID, Set<String>> playerUnlocks = new HashMap<>();
    private static File dataFile;
    private static YamlConfiguration dataConfig;
    private static final Random RANDOM = new Random();

    /**
     * Loads all player recipe unlocks. When MySQL is available the shared {@code player_brew_unlocks}
     * table is used as the source of truth. On the Survival server a one-time migration from the
     * local {@code brew_unlocks.yml} is performed for any players not yet in the database.
     * Falls back to YAML-only mode when the database is not active (e.g. dev environment).
     */
    public static void initialize(Plugin plugin) {
        dataFile = new File(plugin.getDataFolder(), "brew_unlocks.yml");

        if (DatabaseManager.isActive()) {
            // Primary path: load from shared MySQL
            Map<UUID, Set<String>> dbUnlocks = DatabaseManager.getInstance().loadAllBrewUnlocks();
            playerUnlocks.putAll(dbUnlocks);
            Bukkit.getLogger().info("[AC] Loaded brew unlocks from MySQL for " + dbUnlocks.size() + " player(s)");

            // One-time migration: only on Survival, import YAML records not yet in DB
            if (!AranarthCore.isSmpServer() && dataFile.exists()) {
                migrateYamlToDatabase();
            }
        } else {
            // Fallback: load from local YAML (dev / offline mode)
            loadFromYaml();
        }

        loadBreweryRecipes(plugin);
    }

    /** Loads brew unlocks from the local brew_unlocks.yml into the in-memory map. */
    private static void loadFromYaml() {
        if (!dataFile.exists()) {
            try {
                if (dataFile.createNewFile()) {
                    Bukkit.getLogger().info("[AC] A new brew_unlocks.yml file has been generated");
                }
            } catch (IOException e) {
                Bukkit.getLogger().warning("[AC] Failed to create brew_unlocks.yml: " + e.getMessage());
            }
        }
        dataConfig = YamlConfiguration.loadConfiguration(dataFile);
        for (String uuidStr : dataConfig.getKeys(false)) {
            try {
                UUID uuid = UUID.fromString(uuidStr);
                List<String> recipes = dataConfig.getStringList(uuidStr + ".unlocked");
                playerUnlocks.put(uuid, new HashSet<>(recipes));
            } catch (IllegalArgumentException ignored) {}
        }
    }

    /**
     * Imports Survival's brew_unlocks.yml into MySQL for any players who have no DB record yet.
     * Only called on the Survival server during startup so SMP YAML data is never promoted.
     */
    private static void migrateYamlToDatabase() {
        YamlConfiguration yaml = YamlConfiguration.loadConfiguration(dataFile);
        int migrated = 0;
        for (String uuidStr : yaml.getKeys(false)) {
            try {
                UUID uuid = UUID.fromString(uuidStr);
                if (playerUnlocks.containsKey(uuid)) continue; // already in DB
                List<String> recipes = yaml.getStringList(uuidStr + ".unlocked");
                if (recipes.isEmpty()) continue;
                Set<String> recipeSet = new HashSet<>(recipes);
                playerUnlocks.put(uuid, recipeSet);
                DatabaseManager.getInstance().saveBrewUnlocksBulk(uuid, recipeSet);
                migrated++;
            } catch (IllegalArgumentException ignored) {}
        }
        if (migrated > 0) {
            Bukkit.getLogger().info("[AC] Migrated brew unlocks from YAML to MySQL for " + migrated + " player(s)");
        }
    }

    /**
     * Parses the BreweryX recipes.yml and injects display name, ingredients, and color
     * into each BrewRecipe enum value so they don't need to be hardcoded.
     */
    private static void loadBreweryRecipes(Plugin plugin) {
        File recipesFile = new File(plugin.getDataFolder().getParentFile(), "BreweryX/recipes.yml");
        if (!recipesFile.exists()) {
            Bukkit.getLogger().warning("[AC] BreweryX recipes.yml not found at " + recipesFile.getPath()
                    + " - brew displays will fall back to recipe IDs.");
            return;
        }
        YamlConfiguration config = YamlConfiguration.loadConfiguration(recipesFile);
        ConfigurationSection recipesSection = config.getConfigurationSection("recipes");
        if (recipesSection == null) {
            Bukkit.getLogger().warning("[AC] No 'recipes' section found in BreweryX recipes.yml.");
            return;
        }
        int loaded = 0;
        for (BrewRecipe recipe : BrewRecipe.values()) {
            ConfigurationSection section = recipesSection.getConfigurationSection(recipe.getId());
            if (section == null) {
                Bukkit.getLogger().warning("[AC] Recipe '" + recipe.getId() + "' not found in BreweryX recipes.yml.");
                continue;
            }
            // Name
            String nameRaw = section.getString("name", recipe.getId());
            String[] nameParts = nameRaw.split("/");
            String displayName = ChatColor.stripColor(
                    ChatUtils.translateToColor(nameParts[nameParts.length - 1].trim()));

            // Ingredients
            List<String> rawIngredients = section.getStringList("ingredients");
            String[] ingredients = rawIngredients.stream()
                    .map(BrewRecipeUtils::prettifyIngredient)
                    .toArray(String[]::new);

            // Color
            String colorHex = resolveBreweryColor(section.getString("color", "8888FF"));

            // Process fields
            int cookingTime  = section.getInt("cookingtime", 0);
            int distillRuns  = section.getInt("distillruns", 0);
            int age          = section.getInt("age", 0);
            Object woodRaw   = section.get("wood");
            int wood         = resolveWoodInt(woodRaw != null ? woodRaw.toString() : "0");

            recipe.injectRuntimeData(new BrewRecipe.RuntimeData(displayName, ingredients, colorHex, cookingTime, distillRuns, age, wood));
            loaded++;
        }
        Bukkit.getLogger().info("[AC] Loaded BreweryX display data for " + loaded + "/" + BrewRecipe.values().length + " brew recipes.");
    }

    /**
     * Converts a raw BreweryX ingredient string into a human-readable label.
     */
    private static String prettifyIngredient(String raw) {
        String[] parts = raw.split("/");
        String material = parts[0].trim();
        String amount = parts.length > 1 ? parts[1].trim() : "1";
        // Strip plugin prefix
        if (material.contains(":")) {
            material = material.substring(material.indexOf(':') + 1);
        }
        // Split on underscores and hyphens, title-case each word
        String[] words = material.replace('-', '_').split("_");
        StringBuilder sb = new StringBuilder();
        for (String word : words) {
            if (word.isEmpty()) continue;
            if (!sb.isEmpty()) sb.append(' ');
            sb.append(Character.toUpperCase(word.charAt(0)));
            sb.append(word.substring(1).toLowerCase());
        }
        return sb + " x" + amount;
    }

    /**
     * Resolves a BreweryX wood value (number or name string) to its integer ID.
     * 0=Any 1=Birch 2=Oak 3=Jungle 4=Spruce 5=Acacia 6=Dark Oak 7=Crimson 8=Warped
     * 9=Mangrove 10=Cherry 11=Bamboo 12=Cut Copper 13=Pale Oak
     */
    private static int resolveWoodInt(String raw) {
        if (raw == null) return 0;
        String cleaned = raw.trim();
        try {
            return Integer.parseInt(cleaned);
        } catch (NumberFormatException ignored) {}
        return switch (cleaned.replace(' ', '_').toUpperCase()) {
            case "BIRCH"      -> 1;
            case "OAK"        -> 2;
            case "JUNGLE"     -> 3;
            case "SPRUCE"     -> 4;
            case "ACACIA"     -> 5;
            case "DARK_OAK"   -> 6;
            case "CRIMSON"    -> 7;
            case "WARPED"     -> 8;
            case "MANGROVE"   -> 9;
            case "CHERRY"     -> 10;
            case "BAMBOO"     -> 11;
            case "CUT_COPPER" -> 12;
            case "PALE_OAK"   -> 13;
            default           -> 0;
        };
    }

    /** Converts a wood integer ID to a human-readable barrel label. */
    private static String woodName(int wood) {
        return switch (wood) {
            case 1  -> "Birch Barrel";
            case 2  -> "Oak Barrel";
            case 3  -> "Jungle Barrel";
            case 4  -> "Spruce Barrel";
            case 5  -> "Acacia Barrel";
            case 6  -> "Dark Oak Barrel";
            case 7  -> "Crimson Barrel";
            case 8  -> "Warped Barrel";
            case 9  -> "Mangrove Barrel";
            case 10 -> "Cherry Barrel";
            case 11 -> "Bamboo Barrel";
            case 12 -> "Cut Copper Barrel";
            case 13 -> "Pale Oak Barrel";
            default -> "Any";
        };
    }

    /**
     * Resolves a BreweryX color value (named color or hex string) to a 6-char uppercase hex string.
     */
    private static String resolveBreweryColor(String raw) {
        if (raw == null) return "8888FF";
        String cleaned = raw.replace("'", "").replace("\"", "").trim();
        if (cleaned.matches("[0-9A-Fa-f]{6}")) return cleaned.toUpperCase();
        return switch (cleaned.toUpperCase()) {
            case "DARK_RED"    -> "8B0000";
            case "RED"         -> "C80000";
            case "BRIGHT_RED"  -> "FF3300";
            case "ORANGE"      -> "FF8C00";
            case "YELLOW"      -> "FFFF00";
            case "PINK"        -> "FF91A4";
            case "PURPLE"      -> "7B2FBE";
            case "BLUE"        -> "2E52A3";
            case "CYAN"        -> "00BFFF";
            case "WATER"       -> "3F76FF";
            case "TEAL"        -> "00827F";
            case "OLIVE"       -> "808000";
            case "GREEN"       -> "008000";
            case "LIME"        -> "00FF00";
            case "BLACK"       -> "1A1A1A";
            case "GREY"        -> "808080";
            case "BRIGHT_GREY" -> "C0C0C0";
            case "WHITE"       -> "F0F0F0";
            default            -> "8888FF";
        };
    }

    public static boolean isUnlocked(UUID uuid, BrewRecipe recipe) {
        return recipe.isDefaultUnlocked() || playerUnlocks.getOrDefault(uuid, Collections.emptySet()).contains(recipe.getId());
    }

    public static boolean isUnlocked(UUID uuid, String recipeId) {
        BrewRecipe recipe = BrewRecipe.fromId(recipeId);
        if (recipe != null && recipe.isDefaultUnlocked()) return true;
        return playerUnlocks.getOrDefault(uuid, Collections.emptySet()).contains(recipeId);
    }

    public static void unlock(UUID uuid, String recipeId) {
        playerUnlocks.computeIfAbsent(uuid, k -> new HashSet<>()).add(recipeId);
        if (DatabaseManager.isActive()) {
            Bukkit.getScheduler().runTaskAsynchronously(AranarthCore.getInstance(),
                    () -> DatabaseManager.getInstance().saveBrewUnlock(uuid, recipeId));
            if (NetworkManager.isActive()) {
                NetworkManager.getInstance().publishBrewUnlock(uuid, recipeId);
            }
        } else {
            saveUnlocks(uuid);
        }
    }

    /**
     * Updates the in-memory unlock map when a brew is unlocked on another server.
     * No DB write is needed here — the originating server already persisted it.
     */
    public static void applyRemoteUnlock(UUID uuid, String recipeId) {
        playerUnlocks.computeIfAbsent(uuid, k -> new HashSet<>()).add(recipeId);
    }

    /** Saves unlocks to YAML. Only used as a fallback when MySQL is not available. */
    private static void saveUnlocks(UUID uuid) {
        if (dataConfig == null) return;
        Set<String> unlocked = playerUnlocks.getOrDefault(uuid, Collections.emptySet());
        dataConfig.set(uuid.toString() + ".unlocked", new ArrayList<>(unlocked));
        try {
            dataConfig.save(dataFile);
        } catch (IOException e) {

        }
    }

    /** Returns all recipes unlocked by this player (including defaults), in enum declaration order. */
    public static List<BrewRecipe> getUnlockedRecipes(UUID uuid) {
        Set<String> unlocked = playerUnlocks.getOrDefault(uuid, Collections.emptySet());
        List<BrewRecipe> result = new ArrayList<>();
        for (BrewRecipe r : BrewRecipe.values()) {
            if (r.isDefaultUnlocked() || unlocked.contains(r.getId())) result.add(r);
        }
        return result;
    }

    /**
     * Returns all COMMON recipes that are purchasable in the shop — excludes default-unlocked
     * recipes, price-0 recipes, and any the player has already bought.
     */
    public static List<BrewRecipe> getLockedCommonRecipes(UUID uuid) {
        Set<String> unlocked = playerUnlocks.getOrDefault(uuid, Collections.emptySet());
        List<BrewRecipe> result = new ArrayList<>();
        for (BrewRecipe r : BrewRecipe.values()) {
            if (r.getTier() == BrewRecipe.Tier.COMMON && r.getPrice() > 0 && !r.isDefaultUnlocked() && !unlocked.contains(r.getId())) {
                result.add(r);
            }
        }
        return result;
    }

    /** Returns all recipes of the given tier that the player has NOT yet unlocked. */
    private static List<BrewRecipe> getLockedRecipesByTier(UUID uuid, BrewRecipe.Tier tier) {
        Set<String> unlocked = playerUnlocks.getOrDefault(uuid, Collections.emptySet());
        List<BrewRecipe> result = new ArrayList<>();
        for (BrewRecipe r : BrewRecipe.values()) {
            if (r.getTier() == tier && !r.isDefaultUnlocked() && !unlocked.contains(r.getId())) result.add(r);
        }
        return result;
    }

    /**
     * Returns a random RARE-tier recipe the player hasn't unlocked yet,
     * or null if all are already unlocked.
     */
    public static BrewRecipe getRandomLockedRare(UUID uuid) {
        List<BrewRecipe> locked = getLockedRecipesByTier(uuid, BrewRecipe.Tier.RARE);
        return locked.isEmpty() ? null : locked.get(RANDOM.nextInt(locked.size()));
    }

    /**
     * Returns a random LEGENDARY-tier recipe the player hasn't unlocked yet,
     * or null if all are already unlocked.
     */
    public static BrewRecipe getRandomLockedLegendary(UUID uuid) {
        List<BrewRecipe> locked = getLockedRecipesByTier(uuid, BrewRecipe.Tier.LEGENDARY);
        return locked.isEmpty() ? null : locked.get(RANDOM.nextInt(locked.size()));
    }

    /** Returns true when every LEGENDARY-tier recipe has been unlocked by this player. */
    public static boolean allLegendaryUnlocked(UUID uuid) {
        return getLockedRecipesByTier(uuid, BrewRecipe.Tier.LEGENDARY).isEmpty();
    }

    /**
     * Creates the FILLED_MAP unlock token for the given recipe.
     */
    public static ItemStack createRecipeMapItem(BrewRecipe recipe) {
        ItemStack item = new ItemStack(Material.FILLED_MAP);
        ItemMeta meta = item.getItemMeta();

        String tierColor = switch (recipe.getTier()) {
            case COMMON    -> "&f";
            case RARE      -> "&9";
            case LEGENDARY -> "&6";
        };
        String tierName = switch (recipe.getTier()) {
            case COMMON    -> "Common";
            case RARE      -> "Rare";
            case LEGENDARY -> "Legendary";
        };

        meta.setDisplayName(ChatUtils.translateToColor("&6&l[Recipe] &f" + recipe.getDisplayName()));

        List<String> lore = new ArrayList<>();
        lore.add(ChatUtils.translateToColor("&7This page contains the recipe for:"));
        lore.add(ChatUtils.translateToColor("&f&l" + recipe.getDisplayName()));
        lore.add("");
        lore.add(ChatUtils.translateToColor("&8Tier: " + tierColor + tierName));
        lore.add(ChatUtils.translateToColor("&7&oRight-click to unlock this recipe!"));
        meta.setLore(lore);

        meta.getPersistentDataContainer().set(CustomKeys.BREW_RECIPE, PersistentDataType.STRING, recipe.getId());
        item.setItemMeta(meta);
        return item;
    }

    /**
     * Creates a display POTION item representing the given unlocked recipe,
     * with ingredient lore. For avatar brews of Rare tier or above, the secret
     * ingredient's name is obfuscated while its quantity remains visible.
     * Non-avatar brews never have any ingredient obfuscated.
     */
    public static ItemStack createPotionDisplay(BrewRecipe recipe) {
        ItemStack item = new ItemStack(Material.POTION);
        PotionMeta meta = (PotionMeta) item.getItemMeta();

        meta.setColor(recipe.getPotionColor());
        meta.setDisplayName(ChatUtils.translateToColor("&f&l" + recipe.getDisplayName()));

        List<String> lore = new ArrayList<>();
        lore.add(ChatUtils.translateToColor("&8Ingredients:"));

        String[] ingredients = recipe.getIngredients();
        int secret = recipe.getSecretIngredientIndex();
        boolean obfuscateSecrets = secret >= 0 && recipe.getTier() == BrewRecipe.Tier.LEGENDARY;
        for (int i = 0; i < ingredients.length; i++) {
            if (i == secret && obfuscateSecrets) {
                // Show the quantity but obfuscate the ingredient name
                String ing = ingredients[i];
                int lastX = ing.lastIndexOf(" x");
                String amount = lastX >= 0 ? ing.substring(lastX) : "";
                lore.add(ChatUtils.translateToColor("&7&o  &7&k????????????????&r&7&o" + amount));
            } else {
                lore.add(ChatUtils.translateToColor("&7&o  " + ingredients[i]));
            }
        }

        // Process section
        lore.add("");
        lore.add(ChatUtils.translateToColor("&8Process:"));

        int cookingTime = recipe.getCookingTime();
        lore.add(ChatUtils.translateToColor("&7&o  Ferment: " + (cookingTime > 0 ? cookingTime + " min" : "None")));

        int distillRuns = recipe.getDistillRuns();
        lore.add(ChatUtils.translateToColor("&7&o  Distill: " + (distillRuns > 0 ? distillRuns + " run" + (distillRuns > 1 ? "s" : "") : "None")));

        int age = recipe.getAge();
        lore.add(ChatUtils.translateToColor("&7&o  Age: " + (age > 0 ? age + " year" + (age > 1 ? "s" : "") : "None")));

        if (age > 0) {
            lore.add(ChatUtils.translateToColor("&7&o  Barrel: " + woodName(recipe.getWood())));
        }

        String tierColor = switch (recipe.getTier()) {
            case COMMON    -> "&f";
            case RARE      -> "&9";
            case LEGENDARY -> "&6";
        };
        String tierName = switch (recipe.getTier()) {
            case COMMON    -> "Common";
            case RARE      -> "Rare";
            case LEGENDARY -> "Legendary";
        };
        lore.add("");
        lore.add(ChatUtils.translateToColor("&8Tier: " + tierColor + tierName));
        meta.setLore(lore);

        item.setItemMeta(meta);
        return item;
    }

    /**
     * Creates the shop display item for a locked COMMON-tier recipe (shows purchase price).
     */
    public static ItemStack createShopPotionDisplay(BrewRecipe recipe) {
        ItemStack item = new ItemStack(Material.POTION);
        PotionMeta meta = (PotionMeta) item.getItemMeta();

        meta.setColor(recipe.getPotionColor());
        meta.setDisplayName(ChatUtils.translateToColor("&f&l" + recipe.getDisplayName()));

        String tierColor = switch (recipe.getTier()) {
            case COMMON    -> "&f";
            case RARE      -> "&9";
            case LEGENDARY -> "&6";
        };
        String tierName = switch (recipe.getTier()) {
            case COMMON    -> "Common";
            case RARE      -> "Rare";
            case LEGENDARY -> "Legendary";
        };

        NumberFormat fmt = NumberFormat.getInstance();
        List<String> lore = new ArrayList<>();
        lore.add(ChatUtils.translateToColor("&8&oIngredients revealed on purchase"));
        lore.add("");
        lore.add(ChatUtils.translateToColor("&8Tier: " + tierColor + tierName));
        lore.add(ChatUtils.translateToColor("&8Price: &6$" + fmt.format(recipe.getPrice())));
        lore.add(ChatUtils.translateToColor("&7&oClick to purchase!"));
        meta.setLore(lore);

        item.setItemMeta(meta);
        return item;
    }

    /**
     * Creates a cycling recipe map display for the vote crate GUI preview.
     * Uses the Nth RARE-tier recipe (wraps around).
     */
    public static ItemStack createCyclingRecipeMapDisplay(int index) {
        List<BrewRecipe> rareRecipes = new ArrayList<>();
        for (BrewRecipe r : BrewRecipe.values()) {
            if (r.getTier() == BrewRecipe.Tier.RARE) rareRecipes.add(r);
        }
        if (rareRecipes.isEmpty()) return new ItemStack(Material.FILLED_MAP);
        BrewRecipe recipe = rareRecipes.get(index % rareRecipes.size());

        ItemStack item = new ItemStack(Material.FILLED_MAP);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(ChatUtils.translateToColor("&f" + recipe.getDisplayName() + " Recipe"));
        List<String> lore = new ArrayList<>();
        lore.add(ChatUtils.translateToColor("&8Tier: &9Rare"));
        lore.add(ChatUtils.translateToColor("&c5% Chance"));
        meta.setLore(lore);
        item.setItemMeta(meta);
        return item;
    }

    /** Returns the total number of RARE-tier recipes (used for cycling bounds). */
    public static int getRareRecipeCount() {
        int count = 0;
        for (BrewRecipe r : BrewRecipe.values()) {
            if (r.getTier() == BrewRecipe.Tier.RARE) count++;
        }
        return count;
    }
}
