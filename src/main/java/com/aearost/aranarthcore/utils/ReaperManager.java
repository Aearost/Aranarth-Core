package com.aearost.aranarthcore.utils;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * In-memory cache for Reaper Inventory data.
 */
public class ReaperManager {

    private static final Map<UUID, String[]> cache = new HashMap<>();

    public static void put(UUID uuid, String dropsB64, long deathTime) {
        cache.put(uuid, new String[]{dropsB64, String.valueOf(deathTime)});
    }

    /**
     * Returns {dropsB64, deathTimeString} or null if not present.
     */
    public static String[] get(UUID uuid) {
        return cache.get(uuid);
    }

    public static void remove(UUID uuid) {
        cache.remove(uuid);
    }
}
