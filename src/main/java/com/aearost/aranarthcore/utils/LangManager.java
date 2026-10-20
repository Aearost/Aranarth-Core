package com.aearost.aranarthcore.utils;

import com.aearost.aranarthcore.AranarthCore;
import org.bukkit.Bukkit;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.*;

/**
 * Loads and provides access to all player-facing message strings from lang property files.
 * en_US is the primary locale and is used as the fallback for any missing keys in other locales.
 * Loading strategy: bundled jar is loaded first, then the data-folder file is overlaid on top.
 * The data-folder copy is kept in sync with the bundled defaults on every load (see syncDataFile),
 * while admin customisations in the data folder still take precedence for keys they define.
 * Usage:
 *   chatMessage(Lang.getFor(player, "general.no_permission"))
 *   chatMessage(Lang.getFor(player, "player.not_found", "name", playerName))
 */
public class LangManager {

    /** Maps locale code (e.g. "en_US") to its human-readable display name. */
    public static final Map<String, String> LOCALE_DISPLAY_NAMES;
    static {
        Map<String, String> m = new LinkedHashMap<>();
        m.put("en_US", "English");
        m.put("de_DE", "German");
        m.put("fr_FR", "French");
        m.put("es_ES", "Spanish");
        m.put("pt_BR", "Portuguese");
        LOCALE_DISPLAY_NAMES = Collections.unmodifiableMap(m);
    }

    /**
     * Marks a placeholder value as a lang key to resolve in the recipient's locale. It starts with a control
     * character so player-written text (nicknames, sign or chat input) can never be mistaken for a nested key.
     */
    public static final String NESTED_PREFIX = "\u001Elang:";

    /** Separates a nested key from its placeholder pairs. */
    public static final String NESTED_SEPARATOR = "\u001F";

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

        for (String locale : new String[]{"de_DE", "fr_FR", "es_ES", "pt_BR"}) {
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

        // Step 2: create or update the data-folder copy (allows server admins to customise)
        File langFile = new File(langDir, locale + ".properties");
        syncDataFile(plugin, langDir, locale);

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
     * Brings the data-folder copy of a lang file up to date with the bundled defaults.
     * A snapshot of the last bundled defaults is kept in lang/defaults/, so any value that still matches
     * the previous default is replaced with the new one and missing keys are added, while values an admin
     * changed are kept. Without a snapshot (the first run of this logic) every value is reset to the bundled default.
     */
    private void syncDataFile(AranarthCore plugin, File langDir, String locale) {
        String bundled;
        try (InputStream in = plugin.getResource("lang/" + locale + ".properties")) {
            if (in == null) {
                return;
            }
            bundled = new String(in.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            Bukkit.getLogger().warning("[AC] Failed to read bundled lang/" + locale + ".properties: " + e.getMessage());
            return;
        }

        File langFile = new File(langDir, locale + ".properties");
        File snapshotFile = new File(new File(langDir, "defaults"), locale + ".properties");
        try {
            Map<String, String> current = langFile.exists() ? readRawValues(readFile(langFile)) : Map.of();
            Map<String, String> previous = snapshotFile.exists() ? readRawValues(readFile(snapshotFile)) : null;

            // A value is customised if it differs from the default that was shipped when it was last synced
            Map<String, String> customised = new LinkedHashMap<>();
            if (previous != null) {
                for (Map.Entry<String, String> entry : current.entrySet()) {
                    String previousDefault = previous.get(entry.getKey());
                    if (previousDefault == null || !previousDefault.strip().equals(entry.getValue().strip())) {
                        customised.put(entry.getKey(), entry.getValue());
                    }
                }
            }

            // Rebuild from the bundled file so comments, ordering and new keys follow the defaults
            StringBuilder output = new StringBuilder();
            Set<String> bundledKeys = new HashSet<>();
            for (String line : bundled.split("\n", -1)) {
                String key = getKey(line);
                if (key != null) {
                    bundledKeys.add(key);
                    if (customised.containsKey(key)) {
                        line = line.substring(0, line.indexOf('=') + 1) + customised.get(key);
                    }
                }
                output.append(line).append('\n');
            }
            output.setLength(output.length() - 1);
            boolean hasExtraKeys = false;
            for (Map.Entry<String, String> entry : customised.entrySet()) {
                if (!bundledKeys.contains(entry.getKey())) {
                    if (!hasExtraKeys) {
                        output.append("\n# Custom keys that are not part of the bundled defaults\n");
                        hasExtraKeys = true;
                    }
                    output.append(entry.getKey()).append('=').append(entry.getValue()).append('\n');
                }
            }

            String updated = output.toString();
            if (!langFile.exists() || !updated.equals(readFile(langFile))) {
                int changed = 0;
                Map<String, String> updatedValues = readRawValues(updated);
                for (Map.Entry<String, String> entry : updatedValues.entrySet()) {
                    if (!entry.getValue().equals(current.get(entry.getKey()))) {
                        changed++;
                    }
                }
                writeFile(langFile, updated);
                Bukkit.getLogger().info("[AC] Updated " + changed + " entries in lang/" + locale + ".properties ("
                        + customised.size() + " customised entries kept).");
            }
            if (!snapshotFile.exists() || !bundled.equals(readFile(snapshotFile))) {
                writeFile(snapshotFile, bundled);
            }
        } catch (IOException e) {
            Bukkit.getLogger().warning("[AC] Failed to update lang/" + locale + ".properties: " + e.getMessage());
        }
    }

    /**
     * Reads the raw (unescaped) value of every key in a lang file, keyed in file order.
     */
    private static Map<String, String> readRawValues(String content) {
        Map<String, String> values = new LinkedHashMap<>();
        for (String line : content.split("\n")) {
            String key = getKey(line);
            if (key != null) {
                values.put(key, line.substring(line.indexOf('=') + 1).replace("\r", ""));
            }
        }
        return values;
    }

    /**
     * The key of a key=value line, or null for comments and blank lines.
     */
    private static String getKey(String line) {
        String trimmed = line.strip();
        if (trimmed.isEmpty() || trimmed.startsWith("#") || trimmed.startsWith("!") || !line.contains("=")) {
            return null;
        }
        return line.substring(0, line.indexOf('=')).strip();
    }

    private static String readFile(File file) throws IOException {
        return Files.readString(file.toPath());
    }

    private static void writeFile(File file, String content) throws IOException {
        File parent = file.getParentFile();
        if (!parent.exists() && !parent.mkdirs()) {
            throw new IOException("Could not create " + parent.getPath());
        }
        Files.writeString(file.toPath(), content);
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
        return replacePlaceholders("en_US", value, args);
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
        return replacePlaceholders(locale, value, args);
    }

    /** Returns the display name for a locale code, or null if not recognised. */
    public String getDisplayName(String locale) {
        return LOCALE_DISPLAY_NAMES.get(locale);
    }

    /** Returns an unmodifiable view of all loaded locale codes. */
    public Set<String> getSupportedLocales() {
        return Collections.unmodifiableSet(localeMessages.keySet());
    }

    /**
     * Resolves a value built by Lang.key() in the given locale, or returns it unchanged if it is plain text.
     */
    public String resolveNested(String locale, String value) {
        if (value == null || !value.startsWith(NESTED_PREFIX)) {
            return value;
        }
        String[] parts = value.substring(NESTED_PREFIX.length()).split(NESTED_SEPARATOR, -1);
        Object[] args = new Object[parts.length - 1];
        System.arraycopy(parts, 1, args, 0, args.length);
        return get(locale, parts[0], args);
    }

    private String replacePlaceholders(String locale, String value, Object[] args) {
        if (args.length >= 2) {
            for (int i = 0; i + 1 < args.length; i += 2) {
                Object arg = args[i + 1];
                // A nested key (e.g. a rank or job name) is resolved in the same locale as the message
                if (arg instanceof String nested && nested.startsWith(NESTED_PREFIX)) {
                    arg = resolveNested(locale, nested);
                }
                value = value.replace("{" + args[i] + "}", String.valueOf(arg));
            }
        }
        return ChatUtils.translateToColor(value);
    }
}
