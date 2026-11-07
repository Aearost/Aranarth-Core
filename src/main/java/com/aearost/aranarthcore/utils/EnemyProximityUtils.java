package com.aearost.aranarthcore.utils;

import com.aearost.aranarthcore.objects.AranarthPlayer;
import com.aearost.aranarthcore.objects.Dominion;
import com.aearost.aranarthcore.objects.Outpost;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Tracks which enemied dominion members are near each dominion and outpost.
 */
public class EnemyProximityUtils {

    public static final int PROXIMITY_CHUNKS = 4;
    private static final Map<UUID, Set<UUID>> areaToNearbyEnemies = new HashMap<>();
    private static final Map<UUID, Set<UUID>> playerToNearbyAreas = new HashMap<>();

    /**
     * Returns whether any enemy is currently near the given dominion's main land.
     *
     * @param dominion The dominion.
     * @return Whether its bending flag is currently bypassed.
     */
    public static boolean isEnemyNear(Dominion dominion) {
        Set<UUID> enemies = areaToNearbyEnemies.get(dominion.getId());
        return enemies != null && !enemies.isEmpty();
    }

    /**
     * Returns whether any enemy is currently near the given outpost's land.
     *
     * @param outpost The outpost.
     * @return Whether its bending flag is currently bypassed.
     */
    public static boolean isEnemyNear(Outpost outpost) {
        Set<UUID> enemies = areaToNearbyEnemies.get(outpost.getId());
        return enemies != null && !enemies.isEmpty();
    }

    /**
     * Recalculates which areas the player is near as an enemy, based on their current location.
     *
     * @param player The player.
     */
    public static void update(Player player) {
        update(player, player.getLocation());
    }

    /**
     * Recalculates which areas the player is near as an enemy, based on the given location.
     *
     * @param player   The player.
     * @param location The location to evaluate from.
     */
    public static void update(Player player, Location location) {
        setNearbyAreas(player.getUniqueId(), calculateNearbyAreas(player, location));
    }

    /**
     * Removes the player from all proximity tracking, such as when they leave the server.
     *
     * @param uuid The player's UUID.
     */
    public static void remove(UUID uuid) {
        setNearbyAreas(uuid, new HashSet<>());
    }

    /**
     * Determines the IDs of every enemied dominion and outpost within range of the location.
     */
    private static Set<UUID> calculateNearbyAreas(Player player, Location location) {
        Set<UUID> areas = new HashSet<>();
        if (location == null || location.getWorld() == null || !player.isOnline() || player.isDead()) {
            return areas;
        }
        // Staff who are hidden or not actually playing should never unlock bending
        if (player.getGameMode() == GameMode.SPECTATOR) {
            return areas;
        }
        AranarthPlayer aranarthPlayer = AranarthUtils.getPlayer(player.getUniqueId());
        if (aranarthPlayer == null || aranarthPlayer.isVanished() || aranarthPlayer.isInAdminMode()) {
            return areas;
        }
        Dominion playerDominion = DominionUtils.getPlayerDominion(player.getUniqueId());
        if (playerDominion == null) {
            return areas;
        }

        String worldName = location.getWorld().getName();
        int chunkX = location.getBlockX() >> 4;
        int chunkZ = location.getBlockZ() >> 4;
        for (int x = chunkX - PROXIMITY_CHUNKS; x <= chunkX + PROXIMITY_CHUNKS; x++) {
            for (int z = chunkZ - PROXIMITY_CHUNKS; z <= chunkZ + PROXIMITY_CHUNKS; z++) {
                String chunkKey = worldName + ":" + x + ":" + z;
                Dominion dominion = DominionUtils.getDominionOfChunkKey(chunkKey);
                if (dominion != null) {
                    if (playerDominion.isEnemied(dominion)) {
                        areas.add(dominion.getId());
                    }
                    continue;
                }
                Outpost outpost = OutpostUtils.getOutpostOfChunkKey(chunkKey);
                if (outpost != null) {
                    Dominion outpostDominion = DominionUtils.getDominionById(outpost.getDominionId());
                    if (outpostDominion != null && playerDominion.isEnemied(outpostDominion)) {
                        areas.add(outpost.getId());
                    }
                }
            }
        }
        return areas;
    }

    /**
     * Replaces the player's tracked areas, updating the reverse lookup accordingly.
     */
    private static void setNearbyAreas(UUID uuid, Set<UUID> newAreas) {
        Set<UUID> oldAreas = playerToNearbyAreas.getOrDefault(uuid, Set.of());
        for (UUID areaId : oldAreas) {
            if (!newAreas.contains(areaId)) {
                Set<UUID> enemies = areaToNearbyEnemies.get(areaId);
                if (enemies != null) {
                    enemies.remove(uuid);
                    if (enemies.isEmpty()) {
                        areaToNearbyEnemies.remove(areaId);
                    }
                }
            }
        }
        for (UUID areaId : newAreas) {
            areaToNearbyEnemies.computeIfAbsent(areaId, k -> new HashSet<>()).add(uuid);
        }
        if (newAreas.isEmpty()) {
            playerToNearbyAreas.remove(uuid);
        } else {
            playerToNearbyAreas.put(uuid, newAreas);
        }
    }
}
