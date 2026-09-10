package com.aearost.aranarthcore.utils;

import com.aearost.aranarthcore.AranarthCore;
import com.aearost.aranarthcore.objects.HeadEntry;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.plugin.Plugin;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Fetches and caches custom head entries from the minecraft-heads.com API.
 * Only heads that match a Bukkit Material are included in the exchangeable list.
 */
public class HeadsDatabaseManager {

    private static final String API_BASE = "https://minecraft-heads.com/scripts/api.php?tags=true&cat=";

    private static final List<String> MOB_CATEGORIES = List.of("minecraftmobs", "animals", "monsters");
    private static final List<HeadEntry> exchangeableHeads = new ArrayList<>();
    private static final Map<String, String> mobHeadTextures = new HashMap<>();
    private static volatile boolean loaded = false;

    public static void initialize(Plugin plugin) {
        org.bukkit.configuration.file.FileConfiguration config = plugin.getConfig();

        List<String> categories = config.getStringList("head-exchange.categories");

        // Build manual override map
        Map<String, String> overrides = new HashMap<>();
        ConfigurationSection overridesSection = config.getConfigurationSection("head-exchange.material-overrides");
        if (overridesSection != null) {
            for (String key : overridesSection.getKeys(false)) {
                String val = overridesSection.getString(key, "");
                if (!val.isEmpty()) {
                    overrides.put(key, val);
                }
            }
        }

        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            HttpClient client = HttpClient.newBuilder()
                    .connectTimeout(Duration.ofSeconds(10))
                    .build();

            List<HeadEntry> unmatched = new ArrayList<>();

            for (String cat : categories) {
                try {
                    HttpRequest request = HttpRequest.newBuilder()
                            .uri(URI.create(API_BASE + cat))
                            .timeout(Duration.ofSeconds(15))
                            .build();
                    HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

                    JsonArray array = JsonParser.parseString(response.body()).getAsJsonArray();
                    for (JsonElement el : array) {
                        JsonObject obj = el.getAsJsonObject();
                        String name = obj.get("name").getAsString();
                        String texture = obj.get("value").getAsString();

                        Material material = resolveMaterial(name, overrides);
                        HeadEntry entry = new HeadEntry(name, texture, material, cat);

                        if (material != null) {
                            exchangeableHeads.add(entry);
                        } else {
                            unmatched.add(entry);
                        }
                    }
                } catch (Exception ex) {
                    Bukkit.getLogger().warning(AranarthCore.LOG_PREFIX
                            + "Failed to fetch heads for category '" + cat + "': " + ex.getMessage());
                }
            }

            // Fetch mob head texture categories (animals + hostile) for drop system
            for (String category : MOB_CATEGORIES) {
                try {
                    HttpRequest req = HttpRequest.newBuilder()
                            .uri(URI.create(API_BASE + category))
                            .timeout(Duration.ofSeconds(15))
                            .build();
                    HttpResponse<String> res = client.send(req, HttpResponse.BodyHandlers.ofString());
                    JsonArray array = JsonParser.parseString(res.body()).getAsJsonArray();
                    for (JsonElement el : array) {
                        JsonObject obj = el.getAsJsonObject();
                        String name = obj.get("name").getAsString().toLowerCase();
                        String texture = obj.get("value").getAsString();
                        mobHeadTextures.putIfAbsent(name, texture);
                    }
                } catch (Exception ex) {
                    Bukkit.getLogger().warning(AranarthCore.LOG_PREFIX
                            + "Failed to fetch mob head textures for category '" + category + "': " + ex.getMessage());
                }
            }

            loaded = true;
            Bukkit.getLogger().info(AranarthCore.LOG_PREFIX + "Head Exchange: loaded "
                    + exchangeableHeads.size() + " exchangeable heads across " + categories.size() + " category/categories. "
                    + "Mob texture DB: " + mobHeadTextures.size() + " entries.");

            if (!unmatched.isEmpty()) {
                Bukkit.getLogger().info(AranarthCore.LOG_PREFIX + "Head Exchange: " + unmatched.size()
                        + " heads had no vanilla material match and were skipped (use material-overrides in config.yml to include them).");
            }
        });
    }

    /**
     * Returns read-only list of all exchangeable heads. Empty until API load completes.
     */
    public static List<HeadEntry> getExchangeableHeads() {
        return Collections.unmodifiableList(exchangeableHeads);
    }

    /**
     * Returns the texture base64 for a mob head by display name (case-insensitive).
     * Tries exact match, then prefix match, then contains match.
     * Returns null if not found or not yet loaded.
     */
    public static String getMobHeadTexture(String name) {
        String key = name.toLowerCase();
        // 1. Exact match
        String exact = mobHeadTextures.get(key);
        if (exact != null) {
            return exact;
        }
        // 2. Prefix match i.e "sheep" -> "sheep (white)"
        String prefix = null;
        String contains = null;
        for (Map.Entry<String, String> entry : mobHeadTextures.entrySet()) {
            String entryKey = entry.getKey();
            if (prefix == null && entryKey.startsWith(key)) {
                prefix = entry.getValue();
            }
            if (contains == null && entryKey.contains(key)) {
                contains = entry.getValue();
            }
            if (prefix != null && contains != null) {
                break;
            }
        }
        if (prefix != null) {
            return prefix;
        }
        // 3. Contains match i.e "sheep" -> "brown sheep"
        return contains;
    }

    public static boolean isLoaded() {
        return loaded;
    }

    /**
     * Attempts to find a Material matching the given head display name.
     * Checks manual overrides first, then tries several normalizations.
     */
    private static Material resolveMaterial(String headName, Map<String, String> overrides) {
        // Manual override check
        if (overrides.containsKey(headName)) {
            return Material.getMaterial(overrides.get(headName));
        }

        // Strip parenthetical variant descriptions, e.g. "Acacia Log (rounded)" -> "Acacia Log"
        String stripped = headName.replaceAll("\\s*\\(.*?\\)", "").trim();

        String base = stripped.toUpperCase()
                .replace(' ', '_')
                .replace('-', '_')
                .replaceAll("[^A-Z_0-9]", "");

        Material m = tryResolve(base);
        if (m != null) {
            return m;
        }

        // If the stripped name differs from the original, also try the full name (without parens stripped)
        String baseFull = headName.toUpperCase()
                .replace(' ', '_')
                .replace('-', '_')
                .replaceAll("[^A-Z_0-9]", "");
        if (!baseFull.equals(base)) {
            m = tryResolve(baseFull);
            if (m != null) {
                return m;
            }
        }

        return null;
    }

    /**
     * Attempts all normalization strategies for a single base string.
     */
    private static Material tryResolve(String base) {
        // Direct match
        Material m = Material.getMaterial(base);
        if (m != null) {
            return m;
        }

        // Remove trailing _BLOCK
        if (base.endsWith("_BLOCK")) {
            m = Material.getMaterial(base.substring(0, base.length() - 6));
            if (m != null) {
                return m;
            }
        }

        // Add _BLOCK suffix
        m = Material.getMaterial(base + "_BLOCK");
        if (m != null) {
            return m;
        }

        // Remove trailing _ITEM
        if (base.endsWith("_ITEM")) {
            m = Material.getMaterial(base.substring(0, base.length() - 5));
            if (m != null) {
                return m;
            }
        }

        // Strip trailing S for plurals
        if (base.endsWith("S") && base.length() > 2) {
            String singular = base.substring(0, base.length() - 1);
            m = Material.getMaterial(singular);
            if (m != null) {
                return m;
            }
            // Also try _BLOCK on the singular
            m = Material.getMaterial(singular + "_BLOCK");
            if (m != null) {
                return m;
            }
        }

        // Strip trailing ES for plurals
        if (base.endsWith("ES") && base.length() > 3) {
            m = Material.getMaterial(base.substring(0, base.length() - 2));
            if (m != null) {
                return m;
            }
        }

        // Prepend RAW_
        m = Material.getMaterial("RAW_" + base);
        if (m != null) {
            return m;
        }

        return null;
    }
}
