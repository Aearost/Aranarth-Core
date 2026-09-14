package com.aearost.aranarthcore.utils;

import com.aearost.aranarthcore.AranarthCore;
import org.bukkit.Bukkit;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

/**
 * Loads and provides access to all player-facing message strings from lang property files.
 * en_US is the primary locale and is used as the fallback for any missing keys in other locales.
 * Loading strategy: bundled jar is loaded first, then the data-folder file is overlaid on top.
 * This ensures new keys added in the jar are always present even when the server's copy is stale,
 * while admin customisations in the data folder still take precedence for keys they define.
 * Usage:
 *   chatMessage(Lang.get("general.no_permission"))
 *   chatMessage(Lang.get("player.not_found", "name", playerName))
 *   chatMessage(Lang.getFor(player, "general.no_permission"))
 */
public class LangManager {

    /** Maps locale code (e.g. "en_US") to its human-readable display name. */
    public static final Map<String, String> LOCALE_DISPLAY_NAMES;
    static {
        Map<String, String> m = new LinkedHashMap<>();
        m.put("en_US", "English");
        m.put("de_DE", "German");
        m.put("fr_FR", "French");
        LOCALE_DISPLAY_NAMES = Collections.unmodifiableMap(m);
    }

    private static LangManager instance;

    /** Primary en_US messages - also used as the fallback for missing keys in other locales. */
    private final Map<String, String> messages = new HashMap<>();

    /** Non-English locales keyed by locale code. */
    private final Map<String, Map<String, String>> localeMessages = new HashMap<>();

    private LangManager() {}

    public static LangManager getInstance() {
        if (instance == null) {
            instance = new LangManager();
        }
        return instance;
    }

    /**
     * Loads (or reloads) all messages from all supported lang files.
     */
    public void load() {
        messages.clear();
        localeMessages.clear();

        Map<String, String> enUs = loadLocaleFile("en_US");
        messages.putAll(enUs);
        localeMessages.put("en_US", new HashMap<>(enUs));

        for (String locale : new String[]{"de_DE", "fr_FR"}) {
            Map<String, String> localeMap = loadLocaleFile(locale);
            if (!localeMap.isEmpty()) {
                localeMessages.put(locale, localeMap);
            }
        }

        Bukkit.getLogger().info("[AC] Loaded " + messages.size() + " lang entries ("
                + localeMessages.size() + " locale(s)).");
    }

    private Map<String, String> loadLocaleFile(String locale) {
        AranarthCore plugin = AranarthCore.getInstance();
        File langDir = new File(plugin.getDataFolder(), "lang");
        if (!langDir.exists()) {
            boolean result = langDir.mkdirs();
        }

        Properties props = new Properties();

        // Step 1: load bundled jar resource first so new keys are always present
        try (InputStream in = plugin.getResource("lang/" + locale + ".properties")) {
            if (in != null) {
                props.load(new InputStreamReader(in, StandardCharsets.UTF_8));
            } else if (locale.equals("en_US")) {
                Bukkit.getLogger().severe("[AC] Bundled lang/en_US.properties not found in jar!");
            }
        } catch (IOException e) {
            Bukkit.getLogger().warning("[AC] Failed to read bundled lang/" + locale
                    + ".properties: " + e.getMessage());
        }

        // Step 2: copy to data folder if absent (allows server admins to customise)
        File langFile = new File(langDir, locale + ".properties");
        if (!langFile.exists()) {
            plugin.saveResource("lang/" + locale + ".properties", false);
        }

        // Step 3: overlay data-folder file on top - admin edits take precedence
        if (langFile.exists()) {
            try (InputStreamReader reader = new InputStreamReader(
                    new FileInputStream(langFile), StandardCharsets.UTF_8)) {
                Properties override = new Properties();
                override.load(reader);
                for (String key : override.stringPropertyNames()) {
                    props.setProperty(key, override.getProperty(key));
                }
            } catch (IOException e) {
                Bukkit.getLogger().warning("[AC] Failed to load lang/" + locale
                        + ".properties from data folder: " + e.getMessage());
            }
        }

        Map<String, String> result = new HashMap<>();
        for (String key : props.stringPropertyNames()) {
            result.put(key, props.getProperty(key));
        }
        return result;
    }

    /**
     * Returns the message for the given key in the default (en_US) locale,
     * replacing any {placeholder} tokens with the provided pairs (name, value, ...).
     */
    public String get(String key, Object... args) {
        String value = messages.get(key);
        if (value == null) {
            Bukkit.getLogger().warning("[AC] Missing lang key: " + key);
            return key;
        }
        return replacePlaceholders(value, args);
    }

    /**
     * Returns the message for the given key in the specified locale.
     * Falls back to en_US if the locale is unknown or the key is missing in that locale.
     */
    public String get(String locale, String key, Object... args) {
        if (locale == null || locale.equals("en_US")) {
            return get(key, args);
        }
        // Migrate players who had old dialect codes stored before the rename
        if (locale.equals("fr_CA")) locale = "fr_FR";
        if (locale.equals("de_AT")) locale = "de_DE";
        Map<String, String> map = localeMessages.get(locale);
        if (map == null) {
            return get(key, args);
        }
        String value = map.getOrDefault(key, messages.get(key));
        if (value == null) {
            Bukkit.getLogger().warning("[AC] Missing lang key: " + key);
            return key;
        }
        return replacePlaceholders(value, args);
    }

    /** Returns the display name for a locale code, or null if not recognised. */
    public String getDisplayName(String locale) {
        return LOCALE_DISPLAY_NAMES.get(locale);
    }

    /** Returns an unmodifiable view of all loaded locale codes. */
    public Set<String> getSupportedLocales() {
        return Collections.unmodifiableSet(localeMessages.keySet());
    }

    private String replacePlaceholders(String value, Object[] args) {
        if (args.length >= 2) {
            for (int i = 0; i + 1 < args.length; i += 2) {
                value = value.replace("{" + args[i] + "}", String.valueOf(args[i + 1]));
            }
        }
        return ChatUtils.translateToColor(value);
    }
}
