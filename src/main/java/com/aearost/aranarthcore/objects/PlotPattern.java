package com.aearost.aranarthcore.objects;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.structure.Mirror;
import org.bukkit.block.structure.StructureRotation;

import java.util.Map;

/**
 * The repeating layout of the Creative plots world.
 */
public class PlotPattern {

    public static final int FEET_Y = 75;
    public static final int SURFACE_Y = FEET_Y - 1;
    public static final int BEDROCK_Y = -64;

    private static final BlockData BEDROCK = Bukkit.createBlockData(Material.BEDROCK);
    private static final BlockData STONE = Bukkit.createBlockData(Material.STONE);
    private static final BlockData DIRT = Bukkit.createBlockData(Material.DIRT);
    private static final BlockData GRASS = Bukkit.createBlockData(Material.GRASS_BLOCK);
    private static final BlockData AIR = Bukkit.createBlockData(Material.AIR);

    /**
     * Provides whether two neighbouring plots are merged.
     */
    public interface MergeLookup {
        boolean isMergedEast(int gridX, int gridZ);

        boolean isMergedSouth(int gridX, int gridZ);
    }

    /**
     * A block of pattern data. Missing entries fall back to the default ground layers.
     */
    public static class PatternGrid {
        private final int width;
        private final int depth;
        private final int minY;
        private final int height;
        private final BlockData[] cells;

        public PatternGrid(int width, int depth, int minY, int maxY) {
            this.width = width;
            this.depth = depth;
            this.minY = minY;
            this.height = maxY - minY + 1;
            this.cells = new BlockData[width * depth * height];
        }

        public void set(int x, int y, int z, BlockData data) {
            if (x < 0 || x >= width || z < 0 || z >= depth || y < minY || y >= minY + height) {
                return;
            }
            cells[(x * depth + z) * height + (y - minY)] = data;
        }

        public BlockData get(int x, int y, int z) {
            if (y < minY || y >= minY + height) {
                return null;
            }
            return cells[(x * depth + z) * height + (y - minY)];
        }

        public int getWidth() {
            return width;
        }

        public int getDepth() {
            return depth;
        }

        public int getMaxY() {
            return minY + height - 1;
        }

        /**
         * Swaps the X and Z axes, rotating directional blocks to match.
         */
        public PatternGrid transpose() {
            PatternGrid result = new PatternGrid(depth, width, minY, getMaxY());
            for (int x = 0; x < width; x++) {
                for (int z = 0; z < depth; z++) {
                    for (int y = minY; y <= getMaxY(); y++) {
                        BlockData data = get(x, y, z);
                        if (data != null) {
                            BlockData rotated = data.clone();
                            // A transpose is a clockwise rotation followed by an east-west mirror
                            rotated.rotate(StructureRotation.CLOCKWISE_90);
                            rotated.mirror(Mirror.FRONT_BACK);
                            result.set(z, y, x, rotated);
                        }
                    }
                }
            }
            return result;
        }

        /**
         * Rotates the grid 90 degrees clockwise when viewed from above, rotating directional blocks to match.
         */
        public PatternGrid rotateClockwise() {
            PatternGrid result = new PatternGrid(depth, width, minY, getMaxY());
            for (int x = 0; x < width; x++) {
                for (int z = 0; z < depth; z++) {
                    for (int y = minY; y <= getMaxY(); y++) {
                        BlockData data = get(x, y, z);
                        if (data != null) {
                            BlockData rotated = data.clone();
                            rotated.rotate(StructureRotation.CLOCKWISE_90);
                            result.set(depth - 1 - z, y, x, rotated);
                        }
                    }
                }
            }
            return result;
        }
    }

    /**
     * The resolved source of one world column.
     */
    public record Column(PatternGrid grid, int localX, int localZ) {
    }

    private static final Column DEFAULT_COLUMN = new Column(null, 0, 0);

    private final int plotSize;
    private final int roadWidth;
    private final int tileSize;
    private final PatternGrid tile;
    private final PatternGrid mergedRoadNorthSouth;
    private final PatternGrid mergedRoadEastWest;
    private final PatternGrid mergedIntersection;
    /** The T-junction rotated for a merged side to the south, west, north and east, or null to build it from the road. */
    private final PatternGrid[] tJunctions;
    /** The corner rotated for the unmerged plot being south-east, south-west, north-west and north-east, or null to build it from the road. */
    private final PatternGrid[] corners;
    private final int maxY;

    /**
     * @param plotSize           The width of a plot's buildable area.
     * @param roadWidth          The width of the roads between plots.
     * @param tile               The plot tile, plotSize + roadWidth wide on both axes. Only its roads are used.
     * @param mergedRoad         The road strip between two east-west neighbours once merged (roadWidth by plotSize), or null for plain ground.
     * @param mergedIntersection The intersection once all four surrounding plots are merged (roadWidth by roadWidth), or null for plain ground.
     * @param tJunction          The intersection when only the two plots on one side of it are merged (roadWidth by roadWidth),
     *                           or null to build T-junctions from the road.
     * @param tJunctionSide      The side of tJunction that the merged plots are on: 0 south, 1 west, 2 north, 3 east.
     * @param corner             The intersection when three of the plots around it are merged in an L shape (roadWidth by roadWidth),
     *                           or null to build corners from the road.
     * @param cornerSide         The corner of the corner schematic that the unmerged plot is in: 0 south-east, 1 south-west, 2 north-west, 3 north-east.
     */
    public PlotPattern(int plotSize, int roadWidth, PatternGrid tile, PatternGrid mergedRoad, PatternGrid mergedIntersection,
                       PatternGrid tJunction, int tJunctionSide, PatternGrid corner, int cornerSide) {
        this.plotSize = plotSize;
        this.roadWidth = roadWidth;
        this.tileSize = plotSize + roadWidth;
        this.tile = tile;
        this.mergedRoadNorthSouth = mergedRoad;
        this.mergedRoadEastWest = mergedRoad == null ? null : mergedRoad.transpose();
        this.mergedIntersection = mergedIntersection;
        // Each clockwise turn moves a T-junction's merged side from the south to the west, north, then east,
        // and a corner's unmerged plot from the south-east to the south-west, north-west, then north-east
        this.tJunctions = getRotations(tJunction, tJunctionSide);
        this.corners = getRotations(corner, cornerSide);

        int highest = SURFACE_Y;
        for (PatternGrid grid : new PatternGrid[]{tile, mergedRoad, mergedIntersection, tJunction, corner}) {
            if (grid != null) {
                highest = Math.max(highest, grid.getMaxY());
            }
        }
        this.maxY = highest;
    }

    /**
     * Provides the grid in all four orientations, indexed so that the given orientation holds the grid unchanged.
     */
    private static PatternGrid[] getRotations(PatternGrid grid, int orientation) {
        if (grid == null) {
            return null;
        }
        PatternGrid[] rotations = new PatternGrid[4];
        PatternGrid rotated = grid;
        for (int i = 0; i < 4; i++) {
            rotations[(orientation + i) % 4] = rotated;
            rotated = rotated.rotateClockwise();
        }
        return rotations;
    }

    /**
     * Creates the built-in pattern to be used if there are missing schematic files.
     */
    public static PlotPattern createDefault(int plotSize, int roadWidth) {
        int tileSize = plotSize + roadWidth;
        BlockData road = Bukkit.createBlockData(Material.STONE_BRICKS);
        BlockData border = Bukkit.createBlockData(Material.SMOOTH_STONE_SLAB);
        PatternGrid tile = new PatternGrid(tileSize, tileSize, SURFACE_Y, SURFACE_Y + 1);

        int farRow = tileSize - 1;
        for (int x = 0; x < tileSize; x++) {
            for (int z = 0; z < tileSize; z++) {
                boolean roadX = x >= plotSize;
                boolean roadZ = z >= plotSize;
                if (!roadX && !roadZ) {
                    continue;
                }
                tile.set(x, SURFACE_Y, z, road);

                boolean isBorder;
                if (roadX && roadZ) {
                    // Intersection - only the corner of each surrounding plot
                    isBorder = (x == plotSize || x == farRow) && (z == plotSize || z == farRow);
                } else if (roadX) {
                    isBorder = x == plotSize || x == farRow;
                } else {
                    isBorder = z == plotSize || z == farRow;
                }
                if (isBorder) {
                    tile.set(x, SURFACE_Y + 1, z, border);
                }
            }
        }
        return new PlotPattern(plotSize, roadWidth, tile, null, null, null, 0, null, 0);
    }

    /**
     * Resolves which pattern data a world column uses, taking merged plots into account.
     */
    public Column resolve(int x, int z, MergeLookup lookup) {
        int gx = Math.floorDiv(x, tileSize);
        int gz = Math.floorDiv(z, tileSize);
        int lx = Math.floorMod(x, tileSize);
        int lz = Math.floorMod(z, tileSize);
        boolean roadX = lx >= plotSize;
        boolean roadZ = lz >= plotSize;

        // Plot interior - always the default ground, the schematic only provides the roads
        if (!roadX && !roadZ) {
            return DEFAULT_COLUMN;
        }

        // North-south road between (gx, gz) and (gx + 1, gz)
        if (roadX && !roadZ) {
            if (lookup.isMergedEast(gx, gz)) {
                return mergedRoadNorthSouth == null ? DEFAULT_COLUMN : new Column(mergedRoadNorthSouth, lx - plotSize, lz);
            }
            // The ends of the road can belong to a custom intersection at either end
            Column end = getIntersectionArm(x, z, gx, gz - 1, lz, lookup);
            if (end == null) {
                end = getIntersectionArm(x, z, gx, gz, plotSize - 1 - lz, lookup);
            }
            return end != null ? end : new Column(tile, lx, lz);
        }

        // East-west road between (gx, gz) and (gx, gz + 1)
        if (!roadX) {
            if (lookup.isMergedSouth(gx, gz)) {
                return mergedRoadEastWest == null ? DEFAULT_COLUMN : new Column(mergedRoadEastWest, lx, lz - plotSize);
            }
            Column end = getIntersectionArm(x, z, gx - 1, gz, lx, lookup);
            if (end == null) {
                end = getIntersectionArm(x, z, gx, gz, plotSize - 1 - lx, lookup);
            }
            return end != null ? end : new Column(tile, lx, lz);
        }

        // Intersection surrounded by (gx, gz), (gx + 1, gz), (gx, gz + 1), (gx + 1, gz + 1)
        boolean north = lookup.isMergedEast(gx, gz);
        boolean south = lookup.isMergedEast(gx, gz + 1);
        boolean west = lookup.isMergedSouth(gx, gz);
        boolean east = lookup.isMergedSouth(gx + 1, gz);

        if (north && south && west && east) {
            return mergedIntersection == null ? DEFAULT_COLUMN : new Column(mergedIntersection, lx - plotSize, lz - plotSize);
        }
        if (!north && !south && !west && !east) {
            return new Column(tile, lx, lz);
        }

        int ix = lx - plotSize;
        int iz = lz - plotSize;
        PatternGrid custom = getCustomIntersection(gx, gz, ix, iz, lookup);
        if (custom != null) {
            int margin = getMargin(custom);
            return new Column(custom, ix + margin, iz + margin);
        }

        // Each merged side closes off the half of the intersection nearest to it, using the
        // road profile that runs parallel to that side. This forms T-junctions, corners and dead ends.
        double half = roadWidth / 2.0;
        int distanceHorizontal = Integer.MAX_VALUE;
        if (north && iz < half) {
            distanceHorizontal = iz;
        }
        if (south && roadWidth - 1 - iz < half) {
            distanceHorizontal = Math.min(distanceHorizontal, roadWidth - 1 - iz);
        }
        int distanceVertical = Integer.MAX_VALUE;
        if (west && ix < half) {
            distanceVertical = ix;
        }
        if (east && roadWidth - 1 - ix < half) {
            distanceVertical = Math.min(distanceVertical, roadWidth - 1 - ix);
        }

        if (distanceHorizontal == Integer.MAX_VALUE && distanceVertical == Integer.MAX_VALUE) {
            return new Column(tile, lx, lz);
        }
        if (distanceHorizontal <= distanceVertical) {
            // Same row of the east-west road just west of this intersection
            return new Column(tile, plotSize - 1, lz);
        }
        // Same column of the north-south road just north of this intersection
        return new Column(tile, lx, plotSize - 1);
    }

    /**
     * Provides the custom T-junction or corner schematic used by the intersection to the south-east of plot (gx, gz),
     * or null if it has none.
     *
     * @param ix The column's X relative to the intersection's north-west corner, used to pick the half of a straight road.
     * @param iz The column's Z relative to the intersection's north-west corner, used to pick the half of a straight road.
     */
    private PatternGrid getCustomIntersection(int gx, int gz, int ix, int iz, MergeLookup lookup) {
        if (tJunctions == null && corners == null) {
            return null;
        }
        boolean north = lookup.isMergedEast(gx, gz);
        boolean south = lookup.isMergedEast(gx, gz + 1);
        boolean west = lookup.isMergedSouth(gx, gz);
        boolean east = lookup.isMergedSouth(gx + 1, gz);
        int mergedSides = (north ? 1 : 0) + (south ? 1 : 0) + (west ? 1 : 0) + (east ? 1 : 0);
        if (mergedSides == 1 && tJunctions != null) {
            return tJunctions[south ? 0 : west ? 1 : north ? 2 : 3];
        }
        // Two merged sides that touch form an L, and the road turns around the one unmerged plot
        if (mergedSides == 2 && north != south && corners != null) {
            if (north) {
                return corners[west ? 0 : 1];
            }
            return corners[east ? 2 : 3];
        }
        // Two opposite merged sides leave a straight road, built from the closed halves of the two matching T-junctions
        if (mergedSides == 2 && north == south && tJunctions != null) {
            double half = roadWidth / 2.0;
            if (north) {
                return tJunctions[iz < half ? 2 : 0];
            }
            return tJunctions[ix < half ? 1 : 3];
        }
        return null;
    }

    /**
     * Provides the column of a custom intersection's schematic for a road block leading into that intersection,
     * or null if the intersection has no custom schematic or the block is past the part it covers.
     *
     * @param distance How many blocks the road block is from the intersection, 0 being right next to it.
     */
    private Column getIntersectionArm(int x, int z, int gx, int gz, int distance, MergeLookup lookup) {
        int ix = x - (gx * tileSize + plotSize);
        int iz = z - (gz * tileSize + plotSize);
        PatternGrid custom = getCustomIntersection(gx, gz, ix, iz, lookup);
        if (custom == null) {
            return null;
        }
        int margin = getMargin(custom);
        if (distance >= margin) {
            return null;
        }
        return new Column(custom, ix + margin, iz + margin);
    }

    /**
     * Provides how far an intersection schematic reaches into the roads around it.
     */
    private int getMargin(PatternGrid grid) {
        return (grid.getWidth() - roadWidth) / 2;
    }

    /**
     * Provides the furthest that any custom intersection reaches into the roads around it.
     */
    public int getIntersectionMargin() {
        int margin = 0;
        if (tJunctions != null) {
            margin = getMargin(tJunctions[0]);
        }
        if (corners != null) {
            margin = Math.max(margin, getMargin(corners[0]));
        }
        return margin;
    }

    /**
     * Provides the block that belongs at the given height of a resolved column.
     */
    public BlockData getBlock(Column column, int y) {
        if (y <= BEDROCK_Y) {
            return BEDROCK;
        }
        if (column.grid() != null) {
            BlockData data = column.grid().get(column.localX(), y, column.localZ());
            if (data != null) {
                return data;
            }
        }
        return getDefaultBlock(y);
    }

    /**
     * Provides the default ground layers: bedrock, stone, 3 layers of dirt, and grass.
     */
    public static BlockData getDefaultBlock(int y) {
        if (y <= BEDROCK_Y) {
            return BEDROCK;
        } else if (y < SURFACE_Y - 3) {
            return STONE;
        } else if (y < SURFACE_Y) {
            return DIRT;
        } else if (y == SURFACE_Y) {
            return GRASS;
        }
        return AIR;
    }

    /**
     * Provides the highest Y level that contains any pattern data.
     */
    public int getMaxY() {
        return maxY;
    }

    public int getPlotSize() {
        return plotSize;
    }

    public int getRoadWidth() {
        return roadWidth;
    }

    public int getTileSize() {
        return tileSize;
    }

    /**
     * Builds a PatternGrid from blocks gathered relative to the copy origin.
     *
     * @param blocks Map of {x, y, z} relative positions to block data.
     */
    public static PatternGrid buildGrid(int width, int depth, Map<int[], BlockData> blocks) {
        int minY = Integer.MAX_VALUE;
        int maxY = Integer.MIN_VALUE;
        for (int[] pos : blocks.keySet()) {
            minY = Math.min(minY, pos[1]);
            maxY = Math.max(maxY, pos[1]);
        }
        if (blocks.isEmpty()) {
            minY = SURFACE_Y;
            maxY = SURFACE_Y;
        }
        // Never replace the bedrock floor
        minY = Math.max(minY, BEDROCK_Y + 1);
        PatternGrid grid = new PatternGrid(width, depth, minY, Math.max(minY, maxY));
        for (Map.Entry<int[], BlockData> entry : blocks.entrySet()) {
            int[] pos = entry.getKey();
            grid.set(pos[0], pos[1], pos[2], entry.getValue());
        }
        return grid;
    }
}
