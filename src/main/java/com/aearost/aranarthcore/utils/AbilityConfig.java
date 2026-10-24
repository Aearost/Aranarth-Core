package com.aearost.aranarthcore.utils;

import com.aearost.aranarthcore.AranarthCore;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.Plugin;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.Set;

/**
 * Loads abilities.yml, which holds the tunable values (damage, cooldowns, ranges, durations, etc.) of every AranarthCore ability.
 */
public class AbilityConfig {

    private static final String FILE_NAME = "abilities.yml";

    private static YamlConfiguration config = new YamlConfiguration();
    private static final Set<String> warnedPaths = new HashSet<>();

    /**
     * Loads abilities.yml from the plugin data folder, creating it from the bundled copy if it does not exist yet.
     */
    public static void load(Plugin plugin) {
        File file = new File(plugin.getDataFolder(), FILE_NAME);
        if (!file.exists()) {
            plugin.saveResource(FILE_NAME, false);
        }

        YamlConfiguration loaded = YamlConfiguration.loadConfiguration(file);
        InputStream stream = plugin.getResource(FILE_NAME);
        if (stream != null) {
            YamlConfiguration defaults = YamlConfiguration.loadConfiguration(new InputStreamReader(stream, StandardCharsets.UTF_8));
            loaded.setDefaults(defaults);

            // Adds the values of newly added abilities to the server's copy so they can be edited
            boolean hasMissingKeys = false;
            for (String key : defaults.getKeys(true)) {
                if (!defaults.isConfigurationSection(key) && !loaded.isSet(key)) {
                    hasMissingKeys = true;
                    break;
                }
            }
            if (hasMissingKeys) {
                loaded.options().copyDefaults(true);
                try {
                    loaded.save(file);
                } catch (IOException e) {
                    Bukkit.getLogger().warning(AranarthCore.LOG_PREFIX + "Failed to add missing values to " + FILE_NAME + ": " + e.getMessage());
                }
            }
        }

        config = loaded;
        warnedPaths.clear();
    }

    public static double getDouble(String path) {
        checkPath(path);
        return config.getDouble(path);
    }

    public static long getLong(String path) {
        checkPath(path);
        return config.getLong(path);
    }

    public static int getInt(String path) {
        checkPath(path);
        return config.getInt(path);
    }

    public static boolean getBoolean(String path) {
        checkPath(path);
        return config.getBoolean(path);
    }

    /**
     * Logs a single warning for a path that exists in neither the server's abilities.yml nor the bundled copy.
     */
    private static void checkPath(String path) {
        if (!config.contains(path) && warnedPaths.add(path)) {
            Bukkit.getLogger().warning(AranarthCore.LOG_PREFIX + "Missing value '" + path + "' in " + FILE_NAME + " - defaulting to 0");
        }
    }
}
