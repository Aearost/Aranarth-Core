package com.aearost.aranarthcore.utils;

import com.aearost.aranarthcore.objects.AranarthPlayer;
import com.aearost.aranarthcore.trees.CustomTrees.Variant;
import com.aearost.aranarthcore.trees.RareTreeConditions;
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

    private static final Set<Material> GROWS_ON_TIMER = EnumSet.of(
            Material.OAK_SAPLING, Material.BIRCH_SAPLING, Material.POPLAR_SAPLING, Material.SPRUCE_SAPLING,
            Material.JUNGLE_SAPLING, Material.ACACIA_SAPLING, Material.DARK_OAK_SAPLING, Material.PALE_OAK_SAPLING,
            Material.CHERRY_SAPLING, Material.MANGROVE_PROPAGULE);
    private static final Set<Material> GROWS_FROM_BONE_MEAL = EnumSet.of(
            Material.OAK_SAPLING, Material.BIRCH_SAPLING, Material.POPLAR_SAPLING, Material.SPRUCE_SAPLING,
            Material.JUNGLE_SAPLING, Material.ACACIA_SAPLING, Material.DARK_OAK_SAPLING, Material.PALE_OAK_SAPLING,
            Material.CHERRY_SAPLING, Material.MANGROVE_PROPAGULE, Material.AZALEA, Material.FLOWERING_AZALEA);
    private static final Set<Location> TIMED_SAPLINGS = new HashSet<>();
    private static final String KEY_PREFIX = "tree_planter_";
    private static final String NO_DROP_PREFIX = "tree_nodrop_";
    /** Blocks grown by custom trees that drop nothing, so that trees cannot be farmed for them with bone meal. */
    private static final Set<Material> NO_DROP = EnumSet.of(Material.COBWEB, Material.PEARLESCENT_FROGLIGHT, Material.END_ROD);
    public static final int LARGE_TREE_SIZE = 5;
    public static final int LARGE_TREE_RANK = 5;

    /**
     * Determines if the material is a sapling or fungus that can grow into a custom tree.
     */
    public static boolean isGrowable(Material material) {
        return GROWABLE.contains(material);
    }

    /**
     * Determines if the material is a sapling that grows on its own. Planted ones are grown on a timer instead of by
     * vanilla, as vanilla only grows them when its own tree would fit.
     */
    public static boolean growsOnTimer(Material material) {
        return GROWS_ON_TIMER.contains(material);
    }

    /**
     * Determines if the material is a sapling that bone meal grows straight into its custom tree, as vanilla only grows
     * it when its own tree would fit.
     */
    public static boolean growsFromBoneMeal(Material material) {
        return GROWS_FROM_BONE_MEAL.contains(material);
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
     * Determines if a player lets their saplings grow into a custom tree, as they can turn any of them off in /trees.
     *
     * @param uuid The UUID of the player, or null if there is no player.
     * @param variant The custom tree.
     * @return Confirmation of whether the tree may grow.
     */
    public static boolean isTreeEnabled(UUID uuid, Variant variant) {
        if (uuid == null) {
            return true;
        }
        AranarthPlayer aranarthPlayer = AranarthUtils.getPlayer(uuid);
        return aranarthPlayer == null || !aranarthPlayer.isTreeDisabled(variant.name());
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
     * Provides the planted saplings in loaded chunks, which are grown on a timer instead of by vanilla.
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
     * Remembers who planted the sapling, and starts its growth timer.
     *
     * @param block   The sapling.
     * @param planter The UUID of the player who planted it.
     */
    public static void setPlanter(Block block, UUID planter) {
        setPlanter(block, planter, null);
    }

    /**
     * Remembers who planted the sapling and the fruit tree it grows into, and starts its growth timer.
     *
     * @param block   The sapling.
     * @param planter The UUID of the player who planted it.
     * @param fruit   The fruit tree it always grows into, or null for a plain sapling.
     */
    public static void setPlanter(Block block, UUID planter, Variant fruit) {
        block.getChunk().getPersistentDataContainer().set(getKey(block), PersistentDataType.STRING,
                block.getType().name() + ";" + planter + (fruit == null ? "" : ";" + fruit.name()));
        if (GROWS_ON_TIMER.contains(block.getType())) {
            TIMED_SAPLINGS.add(block.getLocation());
        }
    }

    /**
     * Provides the fruit tree that a planted fruit sapling grows into.
     *
     * @param block The block.
     * @return The fruit tree, or null if the block is not a planted fruit sapling.
     */
    public static Variant getFruit(Block block) {
        if (!GROWABLE.contains(block.getType())) {
            return null;
        }
        String value = block.getChunk().getPersistentDataContainer().get(getKey(block), PersistentDataType.STRING);
        if (value == null) {
            return null;
        }
        String[] parts = value.split(";");
        if (parts.length < 3 || block.getType() != Material.matchMaterial(parts[0])) {
            return null;
        }
        try {
            Variant variant = Variant.valueOf(parts[2]);
            return variant.getSapling() == block.getType() ? variant : null;
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    /**
     * Determines if a fruit tree can grow from the sapling where it is planted.
     *
     * @param fruit The fruit tree.
     * @param block The fruit sapling.
     * @return Confirmation of whether it can grow there.
     */
    public static boolean canFruitGrow(Variant fruit, Block block) {
        return !fruit.isRare() || RareTreeConditions.isMet(fruit, block, 1);
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

    private static NamespacedKey getNoDropKey(Block block) {
        return new NamespacedKey("aranarthcore", NO_DROP_PREFIX + (block.getX() & 15) + "_" + block.getY() + "_" + (block.getZ() & 15));
    }

    /**
     * Remembers that a block grown by a custom tree drops nothing, if it is one that could otherwise be farmed.
     */
    public static void markNoDrop(Block block) {
        if (NO_DROP.contains(block.getType())) {
            block.getChunk().getPersistentDataContainer().set(getNoDropKey(block), PersistentDataType.STRING,
                    block.getType().name());
        }
    }

    /**
     * Determines if the block was grown by a custom tree and drops nothing.
     */
    public static boolean isNoDrop(Block block) {
        if (!NO_DROP.contains(block.getType())) {
            return false;
        }
        String value = block.getChunk().getPersistentDataContainer().get(getNoDropKey(block), PersistentDataType.STRING);
        return value != null && value.equals(block.getType().name());
    }

    /**
     * Forgets that the block drops nothing.
     */
    public static void clearNoDrop(Block block) {
        PersistentDataContainer pdc = block.getChunk().getPersistentDataContainer();
        NamespacedKey key = getNoDropKey(block);
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
