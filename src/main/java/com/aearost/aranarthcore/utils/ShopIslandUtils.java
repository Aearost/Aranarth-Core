package com.aearost.aranarthcore.utils;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Biome;
import org.bukkit.block.Block;

import java.util.UUID;

/**
 * Provides utility methods for generating, deleting, and querying floating shop islands
 * in the "shops" void world.
 */
public class ShopIslandUtils {

    public static final String SHOPS_WORLD = "shops";

    // Grid layout: each island occupies a 250x250 cell; islands are centered within their cell
    public static final int GRID_SIZE = 250;
    public static final int BUILD_HALF = 25;   // players may build ±25 blocks from center (50x50 area)
    public static final int PLOT_HALF = 125;   // teleport-back boundary: ±125 from center (250x250)

    // Island shape constants
    private static final int MAX_ISLAND_RADIUS = 22;
    // Loop is wider than the radius so that outward noise bulges can be generated
    private static final int LOOP_RADIUS = MAX_ISLAND_RADIUS + 5;
    public static final int ISLAND_TOP_Y = 100;

    // The island has a hemispherical base: deepest at the centre, rounding out smoothly
    // to zero depth at the perimeter edge. Total island height is roughly 15–20 blocks.
    private static final int MAX_ISLAND_DEPTH = 16;

    // Large number of columns per row before wrapping (effectively unlimited for any normal use)
    private static final int GRID_ROW_WIDTH = 1000;

    // -----------------------------------------------------------------------
    // Grid positioning
    // -----------------------------------------------------------------------

    /**
     * Returns the [centerX, centerZ] coordinates in the shops world for the given island index.
     * Islands are laid out in a row-major grid, each GRID_SIZE apart.
     *
     * @param index The zero-based island index.
     * @return An int array {centerX, centerZ}.
     */
    public static int[] getIslandCenter(int index) {
        int col = index % GRID_ROW_WIDTH;
        int row = index / GRID_ROW_WIDTH;
        int centerX = col * GRID_SIZE + GRID_SIZE / 2;
        int centerZ = row * GRID_SIZE + GRID_SIZE / 2;
        return new int[]{centerX, centerZ};
    }

    // -----------------------------------------------------------------------
    // Island generation
    // -----------------------------------------------------------------------

    /**
     * Generates a natural-looking floating shop island centered at the given coordinates.
     * Shape characteristics:
     *   - Irregular perimeter: overlapping sine waves + fractal noise make each viewing angle different.
     *   - Organic surface: fractal noise raises/lowers the surface by a few blocks; center is kept high.
     *   - Rounded base: hemispherical bowl — deepest at centre (~MAX_ISLAND_DEPTH blocks), rounding
     *     out smoothly to zero at the perimeter; fractal noise roughens the underside organically.
     *   - Natural layering: the dirt/stone boundary is driven by 3-D-like noise, so dirt "bleeds"
     *     irregularly into stone rather than sitting on a flat horizontal plane.
     *
     * @param world   The shops world.
     * @param centerX The X coordinate of the island center.
     * @param centerZ The Z coordinate of the island center.
     */
    public static void generateShopIsland(World world, int centerX, int centerZ) {
        // Unique seed per island so every island looks different from its neighbours
        long seed = (long) centerX * 1234567L ^ (long) centerZ * 7654321L;

        for (int dx = -LOOP_RADIUS; dx <= LOOP_RADIUS; dx++) {
            for (int dz = -LOOP_RADIUS; dz <= LOOP_RADIUS; dz++) {
                double d = Math.sqrt(dx * dx + dz * dz);
                if (d > LOOP_RADIUS) {
                    continue;
                }

                double angle = Math.atan2(dz, dx);

                // ---- Perimeter shape ----
                // Gentle sine waves keep the outline mostly circular with subtle organic variation.
                // Dropped the ×11 term and trimmed amplitudes slightly for a rounder silhouette.
                double angleMod = 0.02 * Math.sin(angle * 3 + 0.70)
                                + 0.013 * Math.sin(angle * 5 + 1.30)
                                + 0.010 * Math.sin(angle * 7 + 2.40);

                // Very light fractal noise — just enough to break up the perfect circle
                float perimNoise = fractalNoise(dx * 0.20f, dz * 0.20f, seed, 3);
                double effectiveD = d * (1.0 - angleMod - perimNoise * 0.025);

                if (effectiveD > MAX_ISLAND_RADIUS) {
                    continue;
                }

                double normalizedD = effectiveD / MAX_ISLAND_RADIUS; // 0..1

                // ---- Surface height ----
                // Very slight dome: edges barely dip below the interior for a nearly flat top.
                double domeEffect = -normalizedD * normalizedD * 1.5;
                // Low-frequency noise (0.07) with 2 octaves produces one broad rise instead of
                // several small clusters, while still feeling organic.
                float surfNoise = fractalNoise(dx * 0.07f, dz * 0.07f, seed + 1000L, 2);
                double surfaceVar = surfNoise * 1.0 * (1.0 - normalizedD * 0.6);
                int surfaceY = (int) Math.round(ISLAND_TOP_Y + domeEffect + surfaceVar);
                // Clamp: surface can poke up at most 1 block, and drop at most 2 at the edges
                surfaceY = Math.max(ISLAND_TOP_Y - 2, Math.min(ISLAND_TOP_Y + 1, surfaceY));
                // Keep the very centre reliably flat so the player's home spot is solid
                if (d < 3.0) {
                    surfaceY = Math.max(surfaceY, ISLAND_TOP_Y);
                }

                // ---- Rounded base ----
                // Hemispherical bowl: sqrt curve gives a smooth round underside rather than a cone.
                // Deepest at the centre (MAX_ISLAND_DEPTH blocks below the surface), shallowing to
                // zero at the perimeter so the edge is just a single grass block.
                double bowlDepth = MAX_ISLAND_DEPTH * Math.sqrt(1.0 - normalizedD * normalizedD);
                // Small fractal noise roughens the underside so it looks natural, not moulded
                float bottomNoise = fractalNoise(dx * 0.25f, dz * 0.25f, seed + 2000L, 3);
                int bottomY = (int) Math.round(surfaceY - bowlDepth + bottomNoise * 2.0);
                bottomY = Math.max(surfaceY - MAX_ISLAND_DEPTH - 3, Math.min(bottomY, surfaceY));

                // ---- Dirt depth ----
                // The number of dirt blocks under the grass surface varies per column (1–5 blocks).
                float dirtNoise = fractalNoise(dx * 0.28f, dz * 0.28f, seed + 3000L, 2);
                int dirtDepth = Math.round(2.5f + dirtNoise * 1.8f);
                dirtDepth = Math.max(1, Math.min(5, dirtDepth));

                // ---- Place blocks ----
                for (int y = bottomY; y <= surfaceY; y++) {
                    Block block = world.getBlockAt(centerX + dx, y, centerZ + dz);
                    if (y == surfaceY) {
                        block.setType(Material.GRASS_BLOCK);
                    } else {
                        int depthFromSurface = surfaceY - y;
                        // Use Y as a secondary noise axis so the dirt/stone boundary undulates
                        // in three dimensions rather than sitting on a flat horizontal plane.
                        float transNoise = fractalNoise(dx * 0.32f, dz * 0.32f + y * 0.11f, seed + 4000L, 2);
                        int localDirt = dirtDepth + Math.round(transNoise * 1.5f);
                        localDirt = Math.max(0, localDirt);
                        block.setType(depthFromSurface <= localDirt ? Material.DIRT : Material.STONE);
                    }
                }
            }
        }
    }

    /**
     * Removes all non-air blocks in and around the island's footprint.
     * Uses the wider loop radius to capture any edge blocks generated by outward noise bulges,
     * and a generous Y range to catch any blocks the player may have placed above.
     *
     * @param world   The shops world.
     * @param centerX The X coordinate of the island center.
     * @param centerZ The Z coordinate of the island center.
     */
    public static void deleteShopIsland(World world, int centerX, int centerZ) {
        for (int dx = -LOOP_RADIUS; dx <= LOOP_RADIUS; dx++) {
            for (int dz = -LOOP_RADIUS; dz <= LOOP_RADIUS; dz++) {
                // Start from MAX_ISLAND_DEPTH+3 below ISLAND_TOP_Y (covers noise overshoot on the base);
                // ISLAND_TOP_Y+35 covers any tall player builds above the surface.
                for (int y = ISLAND_TOP_Y - MAX_ISLAND_DEPTH - 3; y <= ISLAND_TOP_Y + 35; y++) {
                    Block block = world.getBlockAt(centerX + dx, y, centerZ + dz);
                    if (block.getType() != Material.AIR) {
                        block.setType(Material.AIR);
                    }
                }
            }
        }
    }

    // -----------------------------------------------------------------------
    // Boundary checks
    // -----------------------------------------------------------------------

    /**
     * Returns whether the given location is within the build boundary (±25 blocks from island center).
     *
     * @param location The location to check.
     * @param centerX  The island center X.
     * @param centerZ  The island center Z.
     * @return True if within the 50x50 build area.
     */
    public static boolean isWithinBuildBoundary(Location location, int centerX, int centerZ) {
        int dx = Math.abs(location.getBlockX() - centerX);
        int dz = Math.abs(location.getBlockZ() - centerZ);
        return dx <= BUILD_HALF && dz <= BUILD_HALF;
    }

    /**
     * Returns whether the given location is within the plot boundary (±125 blocks from island center).
     * Players outside this range or below Y=50 are teleported back to the shop home.
     *
     * @param location The location to check.
     * @param centerX  The island center X.
     * @param centerZ  The island center Z.
     * @return True if within the 250x250 plot area.
     */
    public static boolean isWithinPlotBoundary(Location location, int centerX, int centerZ) {
        int dx = Math.abs(location.getBlockX() - centerX);
        int dz = Math.abs(location.getBlockZ() - centerZ);
        return dx <= PLOT_HALF && dz <= PLOT_HALF;
    }

    // -----------------------------------------------------------------------
    // Island ownership lookup
    // -----------------------------------------------------------------------

    /**
     * Finds the UUID of the shop island owner whose grid cell contains the given location.
     * Returns null if no island exists at that cell.
     *
     * @param location The location to check (must be in the shops world).
     * @return The owner's UUID, or null.
     */
    public static UUID getIslandOwnerAtLocation(Location location) {
        if (location.getWorld() == null || !location.getWorld().getName().equals(SHOPS_WORLD)) {
            return null;
        }

        int x = location.getBlockX();
        int z = location.getBlockZ();

        // Negative coordinates have no islands — islands start at positive (125, 125)
        if (x < 0 || z < 0) {
            return null;
        }

        int col = x / GRID_SIZE;
        int row = z / GRID_SIZE;
        int cellCenterX = col * GRID_SIZE + GRID_SIZE / 2;
        int cellCenterZ = row * GRID_SIZE + GRID_SIZE / 2;

        for (UUID uuid : AranarthUtils.getShopIslandCenters().keySet()) {
            int[] center = AranarthUtils.getShopIslandCenters().get(uuid);
            if (center[0] == cellCenterX && center[1] == cellCenterZ) {
                return uuid;
            }
        }
        return null;
    }

    // -----------------------------------------------------------------------
    // Biome setting
    // -----------------------------------------------------------------------

    /**
     * Sets the biome for the full 250x250 grid cell belonging to the given island center.
     * Iterates in 4-block steps (the biome section resolution) across the entire X/Z cell
     * and the world's full Y range.
     *
     * @param world   The shops world.
     * @param centerX The island center X.
     * @param centerZ The island center Z.
     * @param biome   The biome to apply.
     */
    public static void setIslandBiome(World world, int centerX, int centerZ, Biome biome) {
        int minX = centerX - PLOT_HALF;
        int minZ = centerZ - PLOT_HALF;
        int minY = world.getMinHeight();
        int maxY = world.getMaxHeight();

        for (int x = minX; x < minX + GRID_SIZE; x += 4) {
            for (int z = minZ; z < minZ + GRID_SIZE; z += 4) {
                for (int y = minY; y < maxY; y += 4) {
                    world.setBiome(x, y, z, biome);
                }
            }
        }
    }

    // -----------------------------------------------------------------------
    // Noise helpers  (hash-based value noise with smooth interpolation)
    // -----------------------------------------------------------------------

    /**
     * Returns a pseudo-random float in [-1, 1] for a given integer grid cell and seed.
     * Uses bit-mixing rather than a lookup table to keep memory usage minimal.
     */
    private static float hashNoise(int xi, int zi, long seed) {
        long h = xi * 1619L + zi * 31337L + seed;
        h = (h ^ (h >>> 13)) * 1274126177L + h;
        h = h ^ (h >>> 16);
        return (h & 0x7FFFFFFFL) / (float) 0x7FFFFFFF - 1.0f;
    }

    /** Hermite smoothstep for noise interpolation. */
    private static float fade(float t) {
        return t * t * (3.0f - 2.0f * t);
    }

    private static float lerp(float a, float b, float t) {
        return a + t * (b - a);
    }

    /**
     * Smooth 2-D value noise using bilinear interpolation of hashed corner values.
     * Returns a value in approximately [-1, 1].
     */
    private static float smoothNoise(float x, float z, long seed) {
        int xi = (int) Math.floor(x);
        int zi = (int) Math.floor(z);
        float fx = x - xi;
        float fz = z - zi;
        float ux = fade(fx);
        float uz = fade(fz);

        float v00 = hashNoise(xi,     zi,     seed);
        float v10 = hashNoise(xi + 1, zi,     seed);
        float v01 = hashNoise(xi,     zi + 1, seed);
        float v11 = hashNoise(xi + 1, zi + 1, seed);

        return lerp(lerp(v00, v10, ux), lerp(v01, v11, ux), uz);
    }

    /**
     * Multi-octave fractal noise (fBm) for organic, layered variation.
     * Each octave doubles the frequency and halves the amplitude.
     *
     * @param x       Noise x-coordinate.
     * @param z       Noise z-coordinate.
     * @param seed    Unique seed; offset per octave to de-correlate layers.
     * @param octaves Number of noise layers (more = finer detail).
     * @return A value in approximately [-1, 1].
     */
    private static float fractalNoise(float x, float z, long seed, int octaves) {
        float result = 0.0f;
        float amplitude = 1.0f;
        float frequency = 1.0f;
        float maxValue = 0.0f;
        for (int i = 0; i < octaves; i++) {
            result    += smoothNoise(x * frequency, z * frequency, seed + i * 997L) * amplitude;
            maxValue  += amplitude;
            amplitude *= 0.5f;
            frequency *= 2.0f;
        }
        return result / maxValue;
    }
}
