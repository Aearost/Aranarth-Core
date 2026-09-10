package com.aearost.aranarthcore.utils;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * In-memory cache for Reaper Inventory data.
 */
public class ReaperManager {

    private static final Map<UUID, String[]> cache = new HashMap<>();

    /**
     * @param dropsB64   Base64-encoded item array
     * @param deathTime  Timestamp of death in milliseconds
     * @param deathWorld World name where the player died
     * @param deathX     X coordinate of death location
     * @param deathY     Y coordinate of death location
     * @param deathZ     Z coordinate of death location
     */
    public static void put(UUID uuid, String dropsB64, long deathTime, String deathWorld, double deathX, double deathY, double deathZ) {
        cache.put(uuid, new String[]{dropsB64, String.valueOf(deathTime), deathWorld, String.valueOf(deathX), String.valueOf(deathY), String.valueOf(deathZ)});
    }

    /**
     * Returns {dropsB64, deathTimeString, deathWorld, deathXStr, deathYStr, deathZStr} or null if not present.
     */
    public static String[] get(UUID uuid) {
        return cache.get(uuid);
    }

    public static void remove(UUID uuid) {
        cache.remove(uuid);
    }
}
