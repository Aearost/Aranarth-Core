package com.aearost.aranarthcore.trees;

import com.aearost.aranarthcore.trees.CustomTrees.Variant;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Biome;
import org.bukkit.block.Block;

import java.util.EnumMap;
import java.util.Map;
import java.util.Set;

/**
 * Decides where each rare tree grows.
 */
public class RareTreeConditions {

    /** How far a sapling may be from water for the trees that grow near it. */
    private static final int WATER_RANGE = 15;
    /** How far a sapling may be from water to be on its bank. */
    private static final int BANK_RANGE = 2;

    private static final Set<Biome> DESERT = Set.of(Biome.DESERT);
    private static final Set<Biome> BEACH = Set.of(Biome.BEACH);
    private static final Set<Biome> JUNGLE = Set.of(Biome.JUNGLE, Biome.SPARSE_JUNGLE, Biome.BAMBOO_JUNGLE);
    private static final Set<Biome> SWAMP = Set.of(Biome.SWAMP);
    private static final Set<Biome> BADLANDS = Set.of(Biome.BADLANDS, Biome.ERODED_BADLANDS, Biome.WOODED_BADLANDS);
    private static final Set<Biome> MEADOW = Set.of(Biome.MEADOW);
    private static final Set<Biome> SAVANNA_PLATEAU = Set.of(Biome.SAVANNA_PLATEAU);
    private static final Set<Biome> NETHER_WASTES = Set.of(Biome.NETHER_WASTES);

    /**
     * Where a rare tree is planted.
     * @param world The world.
     * @param corner The north-west sapling of the square, or the sapling itself.
     * @param size The width of the square of saplings.
     */
    private record Spot(World world, Block corner, int size) {}

    private interface Condition {
        boolean isMet(Spot spot);
    }

    private static final Map<Variant, Condition> CONDITIONS = new EnumMap<>(Variant.class);

    static {
        CONDITIONS.put(Variant.DRAGONBLOOD, spot -> inBiome(spot, DESERT));
        CONDITIONS.put(Variant.COCONUT_PALM, spot -> inBiome(spot, BEACH));
        CONDITIONS.put(Variant.DATE_PALM, spot -> inBiome(spot, DESERT) && nearWater(spot, WATER_RANGE));
        CONDITIONS.put(Variant.RAINBOW_EUCALYPTUS, spot -> inBiome(spot, JUNGLE) && nearWater(spot, WATER_RANGE));
        CONDITIONS.put(Variant.STRANGLER_FIG, spot -> inBiome(spot, JUNGLE));
        CONDITIONS.put(Variant.GIANT_SEQUOIA, spot -> inBiome(spot, Set.of(Biome.OLD_GROWTH_PINE_TAIGA)));
        CONDITIONS.put(Variant.REDWOOD, spot -> inBiome(spot, Set.of(Biome.OLD_GROWTH_SPRUCE_TAIGA)));
        CONDITIONS.put(Variant.BALD_CYPRESS, spot -> inBiome(spot, SWAMP) && nearWater(spot, BANK_RANGE));
        CONDITIONS.put(Variant.WINDSWEPT_HAWTHORN, spot -> inBiome(spot,
                Set.of(Biome.WINDSWEPT_HILLS, Biome.WINDSWEPT_GRAVELLY_HILLS, Biome.FOREST)));
        CONDITIONS.put(Variant.JOSHUA_TREE, spot -> inBiome(spot, BADLANDS));
        CONDITIONS.put(Variant.SNOW_GUM, spot -> inBiome(spot,
                Set.of(Biome.SNOWY_PLAINS, Biome.SNOWY_TAIGA, Biome.ICE_SPIKES)));
        CONDITIONS.put(Variant.PENCIL_CYPRESS, spot -> inBiome(spot, MEADOW));
        CONDITIONS.put(Variant.GREAT_CEDAR, spot -> inBiome(spot, MEADOW));
        CONDITIONS.put(Variant.MONKEY_PUZZLE, spot -> inBiome(spot, Set.of(Biome.WINDSWEPT_FOREST)));
        CONDITIONS.put(Variant.DEAD_SNAG, spot -> inBiome(spot, Set.of(Biome.PALE_GARDEN)));
        CONDITIONS.put(Variant.WITCH_TREE, spot -> inBiome(spot, SWAMP));
        CONDITIONS.put(Variant.OLIVE_TREE, spot -> inBiome(spot, SAVANNA_PLATEAU));
        CONDITIONS.put(Variant.FLAME_TREE, spot -> inBiome(spot, SAVANNA_PLATEAU));
        CONDITIONS.put(Variant.TREE_FERN, spot -> inBiome(spot, Set.of(Biome.LUSH_CAVES)));
        CONDITIONS.put(Variant.GIANT_BONSAI, spot -> plantedOn(spot, Material.MOSS_BLOCK));
        CONDITIONS.put(Variant.MYCELIAL_OAK, spot -> plantedOn(spot, Material.MYCELIUM));
        CONDITIONS.put(Variant.ORCHARD_OAK, spot -> inBiome(spot, Set.of(Biome.SUNFLOWER_PLAINS)));
        CONDITIONS.put(Variant.FLAME_HELIX, spot -> inBiome(spot, NETHER_WASTES));
        CONDITIONS.put(Variant.WARPED_BLOOM, spot -> inBiome(spot, NETHER_WASTES));
        CONDITIONS.put(Variant.EMBER_OAK, spot -> inBiome(spot, NETHER_WASTES));
        CONDITIONS.put(Variant.SOUL_PINE, spot -> inBiome(spot, Set.of(Biome.SOUL_SAND_VALLEY)));
        // End trees grow anywhere in the End, whatever the biome
        CONDITIONS.put(Variant.LANTERN_LILY, RareTreeConditions::inEnd);
        CONDITIONS.put(Variant.BONE_SPINE, RareTreeConditions::inEnd);
        CONDITIONS.put(Variant.BONE_SPRAWL, RareTreeConditions::inEnd);
        CONDITIONS.put(Variant.HALO_TREE, RareTreeConditions::inEnd);
    }

    /**
     * Determines if the rare tree may grow from the square of saplings.
     * @param variant The rare tree.
     * @param corner The north-west sapling of the square, or the sapling itself.
     * @param size The width of the square of saplings.
     * @return Confirmation of whether its conditions are met. A tree with no conditions set never grows.
     */
    public static boolean isMet(Variant variant, Block corner, int size) {
        Condition condition = CONDITIONS.get(variant);
        return condition != null && variant.getSize() == size && corner.getType() == variant.getSapling()
                && condition.isMet(new Spot(corner.getWorld(), corner, size));
    }

    /**
     * Determines if a rare tree may grow wild at the spot, as if a single sapling were planted there.
     * @param variant The rare tree.
     * @param spot The block the tree would grow from, just above the ground.
     * @return Confirmation of whether its conditions are met. A tree with no conditions set never grows.
     */
    public static boolean isMetWild(Variant variant, Block spot) {
        Condition condition = CONDITIONS.get(variant);
        return condition != null && condition.isMet(new Spot(spot.getWorld(), spot, 1));
    }

    /**
     * Determines if every sapling of the square is in one of the biomes.
     */
    private static boolean inBiome(Spot spot, Set<Biome> biomes) {
        for (int dx = 0; dx < spot.size(); dx++) {
            for (int dz = 0; dz < spot.size(); dz++) {
                Block sapling = spot.corner().getRelative(dx, 0, dz);
                if (!biomes.contains(spot.world().getBiome(sapling.getX(), sapling.getY(), sapling.getZ()))) {
                    return false;
                }
            }
        }
        return true;
    }

    private static boolean inEnd(Spot spot) {
        return spot.world().getEnvironment() == World.Environment.THE_END;
    }

    /**
     * Determines if every sapling of the square is planted on the block.
     */
    private static boolean plantedOn(Spot spot, Material ground) {
        for (int dx = 0; dx < spot.size(); dx++) {
            for (int dz = 0; dz < spot.size(); dz++) {
                if (spot.corner().getRelative(dx, -1, dz).getType() != ground) {
                    return false;
                }
            }
        }
        return true;
    }

    /**
     * Determines if there is water within the range of the square, measured sideways, from a little below the saplings to
     * just above them.
     */
    private static boolean nearWater(Spot spot, int range) {
        Block corner = spot.corner();
        int size = spot.size();
        for (int dy = -3; dy <= 1; dy++) {
            for (int dx = -range; dx < size + range; dx++) {
                for (int dz = -range; dz < size + range; dz++) {
                    // Distance to the nearest sapling of the square
                    int ox = dx < 0 ? -dx : Math.max(0, dx - (size - 1));
                    int oz = dz < 0 ? -dz : Math.max(0, dz - (size - 1));
                    if (ox * ox + oz * oz > range * range) {
                        continue;
                    }
                    if (corner.getRelative(dx, dy, dz).getType() == Material.WATER) {
                        return true;
                    }
                }
            }
        }
        return false;
    }
}
