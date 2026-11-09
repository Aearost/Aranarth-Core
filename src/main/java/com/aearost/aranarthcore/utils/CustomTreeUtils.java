package com.aearost.aranarthcore.utils;

import com.aearost.aranarthcore.objects.AranarthPlayer;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.block.Block;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

import java.util.EnumSet;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * Handles the saplings that grow into custom trees.
 */
public class CustomTreeUtils {

    private static final Set<Material> GROWABLE = EnumSet.of(
            Material.OAK_SAPLING, Material.BIRCH_SAPLING, Material.POPLAR_SAPLING, Material.SPRUCE_SAPLING,
            Material.JUNGLE_SAPLING, Material.ACACIA_SAPLING, Material.DARK_OAK_SAPLING, Material.PALE_OAK_SAPLING,
            Material.CHERRY_SAPLING,
            Material.MANGROVE_PROPAGULE, Material.AZALEA, Material.FLOWERING_AZALEA,
            Material.CRIMSON_FUNGUS, Material.WARPED_FUNGUS, Material.BROWN_MUSHROOM, Material.RED_MUSHROOM);

    private static final Set<Material> GROWS_ON_TIMER = EnumSet.of(Material.DARK_OAK_SAPLING, Material.PALE_OAK_SAPLING);
    private static final Set<Location> TIMED_SAPLINGS = new HashSet<>();
    private static final String KEY_PREFIX = "tree_planter_";
    public static final int LARGE_TREE_SIZE = 5;
    public static final int LARGE_TREE_RANK = 5;

    /**
     * Determines if the material is a sapling or fungus that can grow into a custom tree.
     */
    public static boolean isGrowable(Material material) {
        return GROWABLE.contains(material);
    }

    /**
     * Determines if the material is a sapling that vanilla never grows on its own, so is grown on a timer instead.
     */
    public static boolean growsOnTimer(Material material) {
        return GROWS_ON_TIMER.contains(material);
    }

    /**
     * Determines if a player wants their saplings to grow into custom trees.
     *
     * @param uuid The UUID of the player, or null if there is no player.
     * @return Confirmation of whether big trees are enabled.
     */
    public static boolean isBigTreeEnabled(UUID uuid) {
        if (uuid == null) {
            return true;
        }
        AranarthPlayer aranarthPlayer = AranarthUtils.getPlayer(uuid);
        return aranarthPlayer == null || !aranarthPlayer.isBigTreeDisabled();
    }

    /**
     * Determines if a player may grow the largest scale of trees.
     *
     * @param uuid The UUID of the player, or null if there is no player.
     * @return Confirmation of whether the player is Duke or above.
     */
    public static boolean canGrowLargeTrees(UUID uuid) {
        if (uuid == null) {
            return false;
        }
        AranarthPlayer aranarthPlayer = AranarthUtils.getPlayer(uuid);
        return aranarthPlayer != null && aranarthPlayer.getRank() >= LARGE_TREE_RANK;
    }

    /**
     * Provides the lone saplings in loaded chunks that vanilla never grows, so are grown on a timer instead.
     */
    public static Set<Location> getTimedSaplings() {
        return TIMED_SAPLINGS;
    }

    /**
     * Remembers every planted sapling in the chunk that is grown on a timer.
     */
    public static void trackTimedSaplings(Chunk chunk) {
        PersistentDataContainer pdc = chunk.getPersistentDataContainer();
        for (NamespacedKey key : pdc.getKeys()) {
            if (!key.getNamespace().equals("aranarthcore") || !key.getKey().startsWith(KEY_PREFIX)) {
                continue;
            }
            String value = pdc.get(key, PersistentDataType.STRING);
            if (value == null) {
                continue;
            }
            Material material = Material.matchMaterial(value.split(";")[0]);
            if (material == null || !GROWS_ON_TIMER.contains(material)) {
                continue;
            }
            String[] parts = key.getKey().substring(KEY_PREFIX.length()).split("_");
            int x = (chunk.getX() << 4) + Integer.parseInt(parts[0]);
            int y = Integer.parseInt(parts[1]);
            int z = (chunk.getZ() << 4) + Integer.parseInt(parts[2]);
            TIMED_SAPLINGS.add(new Location(chunk.getWorld(), x, y, z));
        }
    }

    /**
     * Forgets the timed saplings of a chunk that is no longer loaded.
     */
    public static void untrackTimedSaplings(Chunk chunk) {
        TIMED_SAPLINGS.removeIf(loc -> loc.getWorld().equals(chunk.getWorld())
                && loc.getBlockX() >> 4 == chunk.getX() && loc.getBlockZ() >> 4 == chunk.getZ());
    }

    private static NamespacedKey getKey(Block block) {
        return new NamespacedKey("aranarthcore", KEY_PREFIX + (block.getX() & 15) + "_" + block.getY() + "_" + (block.getZ() & 15));
    }

    /**
     * Remembers who planted the sapling. A lone sapling that vanilla never grows also starts its growth timer.
     *
     * @param block   The sapling.
     * @param planter The UUID of the player who planted it.
     */
    public static void setPlanter(Block block, UUID planter) {
        block.getChunk().getPersistentDataContainer().set(getKey(block), PersistentDataType.STRING,
                block.getType().name() + ";" + planter);
        if (GROWS_ON_TIMER.contains(block.getType())) {
            TIMED_SAPLINGS.add(block.getLocation());
        }
    }

    /**
     * Provides the player who planted the sapling.
     *
     * @param block The block.
     * @return The UUID of the planter, or null if it is not known.
     */
    public static UUID getPlanter(Block block) {
        PersistentDataContainer pdc = block.getChunk().getPersistentDataContainer();
        NamespacedKey key = getKey(block);
        String value = pdc.get(key, PersistentDataType.STRING);
        if (value == null) {
            return null;
        }
        String[] parts = value.split(";");
        if (parts.length < 2 || block.getType() != Material.matchMaterial(parts[0])) {
            pdc.remove(key);
            return null;
        }
        try {
            return UUID.fromString(parts[1]);
        } catch (IllegalArgumentException e) {
            pdc.remove(key);
            return null;
        }
    }

    /**
     * Forgets who planted the block.
     */
    public static void clearPlanter(Block block) {
        PersistentDataContainer pdc = block.getChunk().getPersistentDataContainer();
        NamespacedKey key = getKey(block);
        // Only changes the chunk when there is something to forget, as this runs for every block placed and broken
        if (pdc.has(key, PersistentDataType.STRING)) {
            pdc.remove(key);
        }
    }

    /**
     * Forgets who planted the block, as well as the sapling above it, which would pop off without the block.
     *
     * @param block The block that was removed.
     */
    public static void clearRemoved(Block block) {
        clearPlanter(block);
        clearPlanter(block.getRelative(0, 1, 0));
    }

    /**
     * Provides the north-west sapling of a square of the same sapling type that the block is part of.
     *
     * @param block The sapling.
     * @param size  The width of the square, such as 3 for a 3x3.
     * @return The north-west sapling, or null if the block is not part of a square of that size.
     */
    public static Block findSquareCorner(Block block, int size) {
        Material type = block.getType();
        for (int cx = -(size - 1); cx <= 0; cx++) {
            for (int cz = -(size - 1); cz <= 0; cz++) {
                Block corner = block.getRelative(cx, 0, cz);
                boolean complete = true;
                for (int dx = 0; dx < size && complete; dx++) {
                    for (int dz = 0; dz < size && complete; dz++) {
                        complete = corner.getRelative(dx, 0, dz).getType() == type;
                    }
                }
                if (complete) {
                    return corner;
                }
            }
        }
        return null;
    }
}
