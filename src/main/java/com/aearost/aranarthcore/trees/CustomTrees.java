package com.aearost.aranarthcore.trees;

import com.aearost.aranarthcore.trees.TreeBuilder.Layer;
import com.aearost.aranarthcore.trees.TreeBuilder.Pos;
import org.bukkit.Axis;
import org.bukkit.Material;
import org.bukkit.Tag;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.Ageable;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.Directional;
import org.bukkit.block.data.MultipleFacing;
import org.bukkit.block.data.Segmentable;
import org.bukkit.block.data.type.CaveVines;
import org.bukkit.block.data.type.CaveVinesPlant;
import org.bukkit.block.data.type.Cocoa;
import org.bukkit.block.data.type.FlowerBed;
import org.bukkit.block.data.type.HangingMoss;
import org.bukkit.block.data.type.MangrovePropagule;
import org.bukkit.block.data.type.MossyCarpet;
import org.bukkit.block.data.type.ShelfMushroom;
import org.bukkit.block.data.type.Wall;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Random;
import java.util.function.DoubleUnaryOperator;

/**
 * The designs of the custom trees that saplings grow into, unless the player has big trees disabled.
 * Each sapling has one or more variants, and every tree is randomized in both shape and size.
 */
public class CustomTrees {

    /**
     * Every custom tree design, identified by a number.
     */
    public enum Variant {
        SPREADING_OAK(1, Material.OAK_SAPLING, 1, "Spreading Oak"),
        OLD_GIANT_OAK(2, Material.OAK_SAPLING, 3, "Old Giant Oak"),
        TALL_FOREST_OAK(3, Material.OAK_SAPLING, 2, "Tall Forest Oak"),
        SILVER_BIRCH(4, Material.BIRCH_SAPLING, 1, "Silver Birch"),
        COLUMN_BIRCH(41, Material.BIRCH_SAPLING, 1, "Column Birch"),
        BIRCH_SPIRE(46, Material.BIRCH_SAPLING, 2, "Birch Spire"),
        LEANING_BIRCH(48, Material.BIRCH_SAPLING, 2, "Leaning Birch"),
        POPLAR(5, Material.POPLAR_SAPLING, 1, "Poplar"),
        GREAT_POPLAR(44, Material.POPLAR_SAPLING, 2, "Great Poplar"),
        MOUNTAIN_SPRUCE(6, Material.SPRUCE_SAPLING, 2, "Mountain Spruce"),
        SUBALPINE_SPIRE(7, Material.SPRUCE_SAPLING, 1, "Subalpine Spire"),
        RAGGED_SPRUCE(8, Material.SPRUCE_SAPLING, 1, "Ragged Old Spruce"),
        JUNGLE_EMERGENT(9, Material.JUNGLE_SAPLING, 2, "Jungle Emergent"),
        KAPOK(10, Material.JUNGLE_SAPLING, 5, "Kapok"),
        SLENDER_JUNGLE(21, Material.JUNGLE_SAPLING, 1, "Slender Jungle"),
        SAVANNA_UMBRELLA(12, Material.ACACIA_SAPLING, 1, "Savanna Umbrella"),
        BOTTLE_BAOBAB(22, Material.ACACIA_SAPLING, 3, "Bottle Baobab"),
        TALL_BAOBAB(23, Material.ACACIA_SAPLING, 5, "Tall Baobab"),
        SPLIT_BAOBAB(24, Material.ACACIA_SAPLING, 3, "Split Baobab"),
        YOUNG_DARK_OAK(25, Material.DARK_OAK_SAPLING, 1, "Young Dark Oak"),
        GNARLED_DARK_OAK(26, Material.DARK_OAK_SAPLING, 2, "Gnarled Dark Oak"),
        ANCIENT_DARK_OAK(13, Material.DARK_OAK_SAPLING, 3, "Ancient Dark Oak"),
        YOUNG_PALE_OAK(27, Material.PALE_OAK_SAPLING, 1, "Young Pale Oak"),
        HOLLOW_PALE_OAK(14, Material.PALE_OAK_SAPLING, 2, "Hollow Pale Oak"),
        ANCIENT_PALE_OAK(28, Material.PALE_OAK_SAPLING, 3, "Ancient Pale Oak"),
        BLOSSOM_CHERRY(15, Material.CHERRY_SAPLING, 1, "Blossom Cherry"),
        GRAND_CHERRY(29, Material.CHERRY_SAPLING, 2, "Grand Cherry"),
        GREAT_MANGROVE(16, Material.MANGROVE_PROPAGULE, "Great Mangrove"),
        BANYAN_GROVE(40, Material.MANGROVE_PROPAGULE, 11, "Banyan Grove"),
        AZALEA(17, Material.AZALEA, "Azalea"),
        FLOWERING_AZALEA(18, Material.FLOWERING_AZALEA, "Flowering Azalea"),
        CRIMSON_COLOSSUS(19, Material.CRIMSON_FUNGUS, 2, "Crimson Colossus"),
        CRIMSON_HELIX(30, Material.CRIMSON_FUNGUS, 1, "Crimson Helix"),
        WARPED_CANDELABRA(20, Material.WARPED_FUNGUS, 2, "Warped Candelabra"),
        LITTLE_WARPED_CANDELABRA(31, Material.WARPED_FUNGUS, 1, "Little Warped Candelabra"),
        BUTTON_BROWN_CAP(32, Material.BROWN_MUSHROOM, 1, "Button Brown Cap"),
        RED_TOADSTOOL(34, Material.RED_MUSHROOM, 1, "Red Toadstool"),
        TALL_RED_BELL(35, Material.RED_MUSHROOM, 1, "Tall Red Bell"),
        GREAT_FLAT_BROWN_CAP(36, Material.BROWN_MUSHROOM, 2, "Great Flat Brown Cap"),
        GREAT_RED_TOADSTOOL(38, Material.RED_MUSHROOM, 2, "Great Red Toadstool"),
        GREAT_RED_BELL(39, Material.RED_MUSHROOM, 2, "Great Red Bell");

        private final int id;
        private final Material sapling;
        /** The width of the square of saplings it grows from, 1 for a single sapling, or 0 if it grows from any. */
        private final int size;
        private final String displayName;

        Variant(int id, Material sapling, String displayName) {
            this(id, sapling, 0, displayName);
        }

        Variant(int id, Material sapling, int size, String displayName) {
            this.id = id;
            this.sapling = sapling;
            this.size = size;
            this.displayName = displayName;
        }

        /**
         * Provides the width of the square of saplings it grows from, or 0 if it grows from any.
         */
        public int getSize() {
            return size;
        }

        public int getId() {
            return id;
        }

        public Material getSapling() {
            return sapling;
        }

        public String getDisplayName() {
            return displayName;
        }

        /**
         * Provides every variant that the sapling can grow into.
         */
        public static List<Variant> forSapling(Material sapling) {
            List<Variant> variants = new ArrayList<>();
            for (Variant variant : values()) {
                if (variant.sapling == sapling) {
                    variants.add(variant);
                }
            }
            return variants;
        }

        /**
         * Provides the widths of the squares larger than 1x1 that the sapling grows a different tree from, largest first.
         */
        public static List<Integer> squareSizes(Material sapling) {
            List<Integer> sizes = new ArrayList<>();
            for (Variant variant : forSapling(sapling)) {
                if (variant.size > 1 && !sizes.contains(variant.size)) {
                    sizes.add(variant.size);
                }
            }
            sizes.sort((a, b) -> b - a);
            return sizes;
        }
    }

    /**
     * How a limb splits into smaller limbs.
     * @param wobble How much the limb randomly bends along its length.
     * @param droop How much the limb bends downwards along its length.
     * @param targetPitch The upward angle that smaller limbs tend towards.
     * @param spread How far apart smaller limbs fan out.
     * @param minChildren The fewest smaller limbs a limb splits into.
     * @param maxChildren The most smaller limbs a limb splits into.
     * @param shrinkMin The smallest length of a smaller limb, relative to its parent.
     * @param shrinkMax The largest length of a smaller limb, relative to its parent.
     * @param minLength Limbs shorter than this stop splitting.
     * @param clusterR The horizontal radius of the leaf cluster at the end of each limb.
     * @param clusterRy The vertical radius of the leaf cluster at the end of each limb.
     * @param clusterLift How far above the end of the limb its leaf cluster sits.
     * @param roughness How ragged the edges of leaf clusters are.
     * @param nodeCluster The chance of a smaller leaf cluster where a limb splits, adding volume.
     */
    private record LimbStyle(double wobble, double droop, double targetPitch, double spread, int minChildren, int maxChildren,
                             double shrinkMin, double shrinkMax, double minLength, double clusterR, double clusterRy,
                             double clusterLift, double roughness, double nodeCluster) {}

    /** Natural ground that plants, petals and moss can cover. */
    private static final Set<Material> SOIL = EnumSet.of(Material.GRASS_BLOCK, Material.DIRT, Material.COARSE_DIRT,
            Material.PODZOL, Material.ROOTED_DIRT, Material.MOSS_BLOCK, Material.PALE_MOSS_BLOCK, Material.MYCELIUM,
            Material.MUD, Material.MUDDY_MANGROVE_ROOTS);

    private static boolean isSoil(Material material) {
        return SOIL.contains(material);
    }

    private static final BlockFace[] HORIZONTAL = {BlockFace.NORTH, BlockFace.EAST, BlockFace.SOUTH, BlockFace.WEST};
    private static final double GOLDEN_ANGLE = 2.39996;

    private final World world;
    private final Random r;
    private TreeBuilder t;

    private CustomTrees(World world, Random random) {
        this.world = world;
        this.r = random;
    }

    /**
     * Designs a random custom tree that the sapling can grow into.
     * @param sapling The sapling type.
     * @param size The width of the square of saplings it grows from, or 1 if it was on its own.
     * @param world The world.
     * @param random The random used for the design.
     * @param x The X coordinate of the sapling, or of the north-west sapling of a 2x2.
     * @param y The Y coordinate of the sapling.
     * @param z The Z coordinate of the sapling, or of the north-west sapling of a 2x2.
     * @return The designed tree, or null if the sapling has no custom tree.
     */
    public static TreeBuilder design(Material sapling, int size, World world, Random random, int x, int y, int z) {
        List<Variant> variants = Variant.forSapling(sapling);
        if (variants.isEmpty()) {
            return null;
        }
        // Variants tied to a square of saplings only grow from that square
        List<Variant> sized = new ArrayList<>();
        for (Variant variant : variants) {
            if (variant.getSize() == size) {
                sized.add(variant);
            }
        }
        if (!sized.isEmpty()) {
            variants = sized;
        } else {
            variants.removeIf(variant -> variant.getSize() != 0);
            if (variants.isEmpty()) {
                return null;
            }
        }
        return design(variants.get(random.nextInt(variants.size())), world, random, x, y, z);
    }

    /**
     * Designs a custom tree of a specific variant.
     */
    public static TreeBuilder design(Variant variant, World world, Random random, int x, int y, int z) {
        CustomTrees trees = new CustomTrees(world, random);
        switch (variant) {
            case SPREADING_OAK -> trees.spreadingOak(x, y, z);
            case OLD_GIANT_OAK -> trees.oldGiantOak(x, y, z);
            case TALL_FOREST_OAK -> trees.tallForestOak(x, y, z);
            case SILVER_BIRCH -> trees.silverBirch(x, y, z);
            case COLUMN_BIRCH -> trees.columnBirch(x, y, z);
            case BIRCH_SPIRE -> trees.birchSpire(x, y, z);
            case LEANING_BIRCH -> trees.leaningBirch(x, y, z);
            case POPLAR -> trees.poplar(x, y, z);
            case GREAT_POPLAR -> trees.greatPoplar(x, y, z);
            case MOUNTAIN_SPRUCE -> trees.mountainSpruce(x, y, z);
            case SUBALPINE_SPIRE -> trees.subalpineSpire(x, y, z);
            case RAGGED_SPRUCE -> trees.raggedSpruce(x, y, z);
            case JUNGLE_EMERGENT -> trees.jungleEmergent(x, y, z);
            case KAPOK -> trees.kapok(x, y, z);
            case SLENDER_JUNGLE -> trees.slenderJungle(x, y, z);
            case SAVANNA_UMBRELLA -> trees.acacia(x, y, z);
            case BOTTLE_BAOBAB -> trees.bottleBaobab(x, y, z);
            case TALL_BAOBAB -> trees.tallBaobab(x, y, z);
            case SPLIT_BAOBAB -> trees.splitBaobab(x, y, z);
            case YOUNG_DARK_OAK -> trees.youngOak(x, y, z, false);
            case GNARLED_DARK_OAK -> trees.gnarledOak(x, y, z, false);
            case ANCIENT_DARK_OAK -> trees.ancientOak(x, y, z, false);
            case YOUNG_PALE_OAK -> trees.youngOak(x, y, z, true);
            case HOLLOW_PALE_OAK -> trees.gnarledOak(x, y, z, true);
            case ANCIENT_PALE_OAK -> trees.ancientOak(x, y, z, true);
            case BLOSSOM_CHERRY -> trees.cherry(x, y, z);
            case GRAND_CHERRY -> trees.grandCherry(x, y, z);
            case GREAT_MANGROVE -> trees.mangrove(x, y, z);
            case BANYAN_GROVE -> trees.banyanGrove(x, y, z);
            case AZALEA -> trees.azalea(x, y, z, 0.1);
            case FLOWERING_AZALEA -> trees.azalea(x, y, z, 0.9);
            case CRIMSON_COLOSSUS -> trees.crimson(x, y, z);
            case CRIMSON_HELIX -> trees.crimsonHelix(x, y, z);
            case WARPED_CANDELABRA -> trees.warped(x, y, z, 2);
            case LITTLE_WARPED_CANDELABRA -> trees.warped(x, y, z, 1);
            case BUTTON_BROWN_CAP -> trees.brownCap(x, y, z, 1);
            case RED_TOADSTOOL -> trees.redToadstool(x, y, z, 1);
            case TALL_RED_BELL -> trees.tallRedBell(x, y, z, 1);
            case GREAT_FLAT_BROWN_CAP -> trees.brownCap(x, y, z, 2);
            case GREAT_RED_TOADSTOOL -> trees.redToadstool(x, y, z, 2);
            case GREAT_RED_BELL -> trees.tallRedBell(x, y, z, 2);
        }
        if (trees.t != null && hasLeafLitter(variant)) {
            trees.leafLitter(x, y, z, Math.max(1, variant.getSize()));
        }
        if (trees.t != null) {
            trees.shelfMushrooms(variant, y);
        }
        return trees.t;
    }

    // ------------------------------------------------------------
    // Helpers
    // ------------------------------------------------------------

    private int between(int min, int max) {
        return min + r.nextInt(max - min + 1);
    }

    private double between(double min, double max) {
        return min + r.nextDouble() * (max - min);
    }

    private boolean chance(double chance) {
        return r.nextDouble() < chance;
    }

    private double jitter(double amount) {
        return (r.nextDouble() * 2 - 1) * amount;
    }

    private double randomAngle() {
        return r.nextDouble() * Math.PI * 2;
    }

    /**
     * Grows a limb that keeps splitting into smaller limbs, each ending in a cluster of leaves.
     * @param yaw The horizontal direction of the limb.
     * @param pitch The upward angle of the limb.
     * @param length The length of the limb.
     * @param depth How many more times the limb may split.
     */
    private void limb(double x, double y, double z, double yaw, double pitch, double length, int depth, LimbStyle s) {
        int segments = Math.max(1, (int) Math.round(length / 2.5));
        double segment = length / segments;
        for (int i = 0; i < segments; i++) {
            yaw += jitter(s.wobble());
            pitch = Math.max(-0.35, Math.min(1.5, pitch + jitter(s.wobble() * 0.5) - s.droop()));
            double nx = x + Math.cos(yaw) * Math.cos(pitch) * segment;
            double ny = y + Math.sin(pitch) * segment;
            double nz = z + Math.sin(yaw) * Math.cos(pitch) * segment;
            t.branch(x, y, z, nx, ny, nz);
            x = nx;
            y = ny;
            z = nz;
        }
        if (depth <= 0 || length * s.shrinkMax() < s.minLength()) {
            double size = between(0.85, 1.15);
            t.blob(x, y + s.clusterLift(), z, s.clusterR() * size, s.clusterRy() * size, s.clusterR() * size, s.roughness());
            return;
        }
        if (chance(s.nodeCluster())) {
            t.blob(x, y + s.clusterLift(), z, s.clusterR() * 0.65, s.clusterRy() * 0.7, s.clusterR() * 0.65, s.roughness());
        }
        int children = between(s.minChildren(), s.maxChildren());
        for (int i = 0; i < children; i++) {
            double offset = children == 1 ? jitter(s.spread())
                    : -s.spread() + 2 * s.spread() * i / (children - 1) + jitter(0.25);
            double childPitch = pitch + (s.targetPitch() - pitch) * 0.6 + jitter(0.2);
            limb(x, y, z, yaw + offset, childPitch, length * between(s.shrinkMin(), s.shrinkMax()), depth - 1, s);
        }
    }

    /**
     * Grows a limb that keeps splitting like {@link #limb}, but starts thick and tapers each time it splits, so that only
     * the ends of its smallest branches are a single block wide.
     * @param radius The radius of the limb where it starts.
     */
    private void thickLimb(double x, double y, double z, double yaw, double pitch, double length, int depth, LimbStyle s,
                           double radius) {
        if (radius < 0.5) {
            limb(x, y, z, yaw, pitch, length, depth, s);
            return;
        }
        int segments = Math.max(1, (int) Math.round(length / 2.5));
        double segment = length / segments;
        double endRadius = radius * 0.75;
        for (int i = 0; i < segments; i++) {
            yaw += jitter(s.wobble());
            pitch = Math.max(-0.35, Math.min(1.5, pitch + jitter(s.wobble() * 0.5) - s.droop()));
            double nx = x + Math.cos(yaw) * Math.cos(pitch) * segment;
            double ny = y + Math.sin(pitch) * segment;
            double nz = z + Math.sin(yaw) * Math.cos(pitch) * segment;
            t.thickBranch(x, y, z, nx, ny, nz, radius + (endRadius - radius) * i / segments,
                    radius + (endRadius - radius) * (i + 1) / segments);
            x = nx;
            y = ny;
            z = nz;
        }
        if (depth <= 0 || length * s.shrinkMax() < s.minLength()) {
            double size = between(0.85, 1.15);
            t.blob(x, y + s.clusterLift(), z, s.clusterR() * size, s.clusterRy() * size, s.clusterR() * size, s.roughness());
            return;
        }
        if (chance(s.nodeCluster())) {
            t.blob(x, y + s.clusterLift(), z, s.clusterR() * 0.65, s.clusterRy() * 0.7, s.clusterR() * 0.65, s.roughness());
        }
        int children = between(s.minChildren(), s.maxChildren());
        for (int i = 0; i < children; i++) {
            double offset = children == 1 ? jitter(s.spread())
                    : -s.spread() + 2 * s.spread() * i / (children - 1) + jitter(0.25);
            double childPitch = pitch + (s.targetPitch() - pitch) * 0.6 + jitter(0.2);
            thickLimb(x, y, z, yaw + offset, childPitch, length * between(s.shrinkMin(), s.shrinkMax()), depth - 1, s,
                    endRadius * 0.7);
        }
    }

    /**
     * Draws a branch that bends upwards, made of two segments.
     * @return The end of the branch followed by its middle.
     */
    private double[] curvedBranch(double sx, double sy, double sz, double angle, double length, double rise) {
        double mx = sx + Math.cos(angle) * length * 0.5;
        double my = sy + rise * 0.3;
        double mz = sz + Math.sin(angle) * length * 0.5;
        double ex = sx + Math.cos(angle) * length;
        double ey = sy + rise;
        double ez = sz + Math.sin(angle) * length;
        t.branch(sx, sy, sz, mx, my, mz);
        t.branch(mx, my, mz, ex, ey, ez);
        return new double[] {ex, ey, ez, mx, my, mz};
    }

    /**
     * Grows roots that spread out from the base and dig down into soft ground.
     * @param startRadius How far from the center of the trunk the roots start.
     * @param depth How deep below the surface the roots end.
     */
    private void surfaceRoots(double cx, int y, double cz, int count, double minLength, double maxLength, double startRadius,
                              int depth, Material material) {
        double start = randomAngle();
        for (int i = 0; i < count; i++) {
            double angle = start + i * Math.PI * 2 / count + jitter(0.35);
            double length = between(minLength, maxLength);
            double ox = Math.cos(angle);
            double oz = Math.sin(angle);
            double midX = cx + ox * (startRadius + length * 0.6);
            double midZ = cz + oz * (startRadius + length * 0.6);
            t.rootLine(cx + ox * startRadius, y + 0.5, cz + oz * startRadius, midX, y - 0.5, midZ, material, false);
            t.rootLine(midX, y - 0.5, midZ, cx + ox * (startRadius + length), y - 0.5 - depth, cz + oz * (startRadius + length),
                    material, false);
        }
    }

    /**
     * Provides the first open block above solid ground in the column, or null if there is none nearby.
     */
    private Block findGround(int x, int baseY, int z, int range) {
        for (int y = baseY + 2; y >= baseY - range; y--) {
            Block block = world.getBlockAt(x, y, z);
            if (block.getType().isAir() && block.getRelative(BlockFace.DOWN).getType().isSolid()) {
                return block;
            }
        }
        return null;
    }

    /**
     * Provides how much of a ground cover remains at a distance from the tree, fading out gradually towards the edge
     * instead of ending suddenly.
     * @return 1 in the middle, falling smoothly to 0 at the radius.
     */
    private static double fade(double distance, double radius) {
        double p = Math.max(0, Math.min(1, (radius - distance) / (radius * 0.5)));
        return p * p * (3 - 2 * p);
    }

    /**
     * Scatters a ground cover around the base of the tree, thinning out towards its edge.
     */
    private void groundCover(int x, int y, int z, double radius, double density, Material material, boolean dirtOnly) {
        for (int dx = (int) -radius; dx <= radius; dx++) {
            for (int dz = (int) -radius; dz <= radius; dz++) {
                if (dx * dx + dz * dz > radius * radius || !chance(density * fade(Math.hypot(dx, dz), radius))) {
                    continue;
                }
                Block ground = findGround(x + dx, y, z + dz, 3);
                if (ground == null || t.has(ground.getX(), ground.getY(), ground.getZ())) {
                    continue;
                }
                Material below = ground.getRelative(BlockFace.DOWN).getType();
                if (dirtOnly && !isSoil(below)) {
                    continue;
                }
                BlockData data = material.createBlockData();
                if (data instanceof FlowerBed bed) {
                    bed.setFlowerAmount(between(1, bed.getMaximumFlowerAmount()));
                }
                if (data instanceof Segmentable segments) {
                    segments.setSegmentAmount(between(segments.getMinimumSegmentAmount(), segments.getMaximumSegmentAmount()));
                }
                if (data instanceof Directional directional) {
                    directional.setFacing(HORIZONTAL[r.nextInt(4)]);
                }
                t.decor(ground.getX(), ground.getY(), ground.getZ(), data, 0, -1, 0);
            }
        }
    }

    /**
     * Determines if fallen leaves gather beneath the variant.
     */
    private static boolean hasLeafLitter(Variant variant) {
        return switch (variant) {
            case SPREADING_OAK, OLD_GIANT_OAK, TALL_FOREST_OAK, SILVER_BIRCH, COLUMN_BIRCH, BIRCH_SPIRE, LEANING_BIRCH,
                 POPLAR, GREAT_POPLAR, YOUNG_DARK_OAK, GNARLED_DARK_OAK, ANCIENT_DARK_OAK, AZALEA, FLOWERING_AZALEA -> true;
            default -> false;
        };
    }

    /**
     * Lays occasional patches of moss carpet along the tops of branches.
     * @param minY The lowest height that moss grows at, keeping it off roots and buttresses.
     * @param chance The chance of a patch starting on each exposed branch block.
     */
    private void mossOnBranches(int minY, double chance) {
        mossOnBranches(minY, chance, Material.MOSS_CARPET);
    }

    /**
     * Lays occasional patches of a moss carpet along the tops of branches.
     */
    private void mossOnBranches(int minY, double chance, Material carpet) {
        for (Pos wood : t.getPositions(Layer.LOG)) {
            if (wood.y() < minY || t.has(wood.x(), wood.y() + 1, wood.z()) || !chance(chance)) {
                continue;
            }
            // Spreads along the top of the branch from where it started
            int size = between(2, 6);
            int x = wood.x();
            int z = wood.z();
            for (int i = 0; i < size * 3 && size > 0; i++) {
                if (t.has(x, wood.y(), z) && !t.has(x, wood.y() + 1, z)
                        && t.decor(x, wood.y() + 1, z, carpet.createBlockData(), 0, -1, 0)) {
                    size--;
                }
                BlockFace face = HORIZONTAL[r.nextInt(4)];
                if (t.has(x + face.getModX(), wood.y(), z + face.getModZ())) {
                    x += face.getModX();
                    z += face.getModZ();
                }
            }
        }
    }

    /**
     * Scatters a small patch of sweet berry bushes a few blocks apart from each other, a short way from the tree.
     */
    private void berryPatch(double cx, int y, double cz, double minDistance, double maxDistance) {
        double angle = randomAngle();
        double distance = between(minDistance, maxDistance);
        double px = cx + Math.cos(angle) * distance;
        double pz = cz + Math.sin(angle) * distance;
        int bushes = between(4, 6);
        for (int i = 0; i < bushes; i++) {
            int bx = (int) Math.floor(px + jitter(3.5));
            int bz = (int) Math.floor(pz + jitter(3.5));
            Block ground = findGround(bx, y, bz, 3);
            if (ground == null || t.has(ground.getX(), ground.getY(), ground.getZ())
                    || !isSoil(ground.getRelative(BlockFace.DOWN).getType())) {
                continue;
            }
            Ageable bush = (Ageable) Material.SWEET_BERRY_BUSH.createBlockData();
            bush.setAge(between(1, bush.getMaximumAge()));
            t.decor(ground.getX(), ground.getY(), ground.getZ(), bush, 0, -1, 0);
        }
    }

    /**
     * Scatters patches of fallen leaves under the canopy, spreading as far as its leaves reach.
     * @param size The width of the square of saplings the tree grew from.
     */
    private void leafLitter(int x, int y, int z, int size) {
        double cx = x + size / 2.0;
        double cz = z + size / 2.0;
        double reach = 0;
        for (Pos leaf : t.getPositions(Layer.LEAF)) {
            reach = Math.max(reach, Math.hypot(leaf.x() + 0.5 - cx, leaf.z() + 0.5 - cz));
        }
        if (reach < 1) {
            return;
        }
        groundCover((int) Math.floor(cx), y, (int) Math.floor(cz), Math.min(reach * 1.15, 16), 0.24, Material.LEAF_LITTER, false);
    }

    /**
     * Grows the occasional cluster of shelf mushrooms on the trunks of the trees that they suit.
     * @param y The height of the ground.
     */
    private void shelfMushrooms(Variant variant, int y) {
        switch (variant) {
            case SILVER_BIRCH, COLUMN_BIRCH, BIRCH_SPIRE -> {
                if (chance(0.35)) {
                    shelfMushrooms(between(1, 2), y + 1, y + 7);
                }
            }
            case LEANING_BIRCH -> {
                if (chance(0.8)) {
                    shelfMushrooms(between(2, 3), y + 1, y + 9);
                }
            }
            case OLD_GIANT_OAK, YOUNG_DARK_OAK, GNARLED_DARK_OAK, ANCIENT_DARK_OAK, YOUNG_PALE_OAK, HOLLOW_PALE_OAK,
                 ANCIENT_PALE_OAK -> {
                if (chance(0.4)) {
                    shelfMushrooms(between(1, 3), y + 1, y + 8);
                }
            }
            default -> {
            }
        }
    }

    /**
     * Places shelf mushrooms on the sides of the tree's wood, often stacked in small clusters of up to 3 above each other.
     * @param count How many clusters to place.
     * @param minY The lowest height they grow at.
     * @param maxY The highest height they grow at.
     */
    private void shelfMushrooms(int count, int minY, int maxY) {
        List<Pos> wood = new ArrayList<>(t.getPositions(Layer.LOG));
        wood.removeIf(pos -> pos.y() < minY || pos.y() > maxY || !Tag.LOGS.isTagged(t.typeAt(pos.x(), pos.y(), pos.z())));
        if (wood.isEmpty()) {
            return;
        }
        int placed = 0;
        for (int attempt = 0; attempt < count * 8 && placed < count; attempt++) {
            Pos pos = wood.get(r.nextInt(wood.size()));
            // Faces away from the wood it grows on
            BlockFace face = HORIZONTAL[r.nextInt(4)];
            int px = pos.x() + face.getModX();
            int pz = pos.z() + face.getModZ();
            if (t.has(px, pos.y(), pz)) {
                continue;
            }
            int stack = between(1, 3);
            for (int i = 0; i < stack; i++) {
                int py = pos.y() + i;
                Material behind = t.typeAt(pos.x(), py, pos.z());
                if (behind == null || !Tag.LOGS.isTagged(behind) || t.has(px, py, pz)) {
                    break;
                }
                ShelfMushroom shelf = (ShelfMushroom) Material.SHELF_MUSHROOM.createBlockData();
                shelf.setFacing(face);
                shelf.setAge(between(0, shelf.getMaximumAge()));
                t.decor(px, py, pz, shelf, -face.getModX(), 0, -face.getModZ());
            }
            placed++;
        }
    }

    /**
     * Places cocoa pods on the sides of the tree's wood, spread all over the trunk and its branches.
     */
    private void cocoaPods(int count, int minY) {
        List<Pos> wood = new ArrayList<>(t.getPositions(Layer.LOG));
        wood.removeIf(pos -> pos.y() < minY);
        if (wood.isEmpty()) {
            return;
        }
        int placed = 0;
        for (int attempt = 0; attempt < count * 8 && placed < count; attempt++) {
            Pos pos = wood.get(r.nextInt(wood.size()));
            BlockFace face = HORIZONTAL[r.nextInt(4)];
            int px = pos.x() + face.getModX();
            int pz = pos.z() + face.getModZ();
            if (t.has(px, pos.y(), pz)) {
                continue;
            }
            Cocoa cocoa = (Cocoa) Material.COCOA.createBlockData();
            BlockFace towardsWood = face.getOppositeFace();
            cocoa.setFacing(towardsWood);
            cocoa.setAge(between(0, cocoa.getMaximumAge()));
            if (t.decor(px, pos.y(), pz, cocoa, towardsWood.getModX(), 0, towardsWood.getModZ())) {
                placed++;
            }
        }
    }

    /**
     * Hangs vines from the outer edges of the canopy.
     */
    private void hangingVines(double chance, int maxLength, int minY) {
        for (Pos leaf : t.getPositions(Layer.LEAF)) {
            if (leaf.y() < minY || !chance(chance)) {
                continue;
            }
            BlockFace face = HORIZONTAL[r.nextInt(4)];
            int x = leaf.x() + face.getModX();
            int z = leaf.z() + face.getModZ();
            if (t.has(x, leaf.y(), z)) {
                continue;
            }
            int length = between(1, maxLength);
            for (int i = 0; i < length; i++) {
                int y = leaf.y() - i;
                MultipleFacing vine = (MultipleFacing) Material.VINE.createBlockData();
                vine.setFace(face.getOppositeFace(), true);
                // The first vine clings to the leaf, the rest hang from the vine above
                boolean placed = i == 0
                        ? t.decor(x, y, z, vine, -face.getModX(), 0, -face.getModZ())
                        : t.decor(x, y, z, vine, 0, 1, 0);
                if (!placed) {
                    break;
                }
            }
        }
    }

    /**
     * Places vines clinging to the sides of a square trunk.
     */
    private void trunkVines(int x0, int z0, int size, int y0, int y1, double chance) {
        for (int y = y0; y <= y1; y++) {
            for (int i = 0; i < size; i++) {
                placeTrunkVine(x0 - 1, y, z0 + i, BlockFace.EAST, chance);
                placeTrunkVine(x0 + size, y, z0 + i, BlockFace.WEST, chance);
                placeTrunkVine(x0 + i, y, z0 - 1, BlockFace.SOUTH, chance);
                placeTrunkVine(x0 + i, y, z0 + size, BlockFace.NORTH, chance);
            }
        }
    }

    private void placeTrunkVine(int x, int y, int z, BlockFace towardsTrunk, double chance) {
        if (!chance(chance)) {
            return;
        }
        MultipleFacing vine = (MultipleFacing) Material.VINE.createBlockData();
        vine.setFace(towardsTrunk, true);
        t.decor(x, y, z, vine, towardsTrunk.getModX(), 0, towardsTrunk.getModZ());
    }

    // ------------------------------------------------------------
    // Oak
    // ------------------------------------------------------------

    /**
     * #1 Spreading Oak - a short single trunk that splits into a few limbs, which keep forking into an open,
     * rounded crown made of many separate leaf clusters.
     */
    private void spreadingOak(int x, int y, int z) {
        t = new TreeBuilder(world, r, Material.OAK_WOOD, Material.OAK_LEAVES);
        t.setLeafMix(Material.OAK_LEAVES, Material.DARK_OAK_LEAVES, 0.15);
        t.setPorosity(0.25);
        double scale = between(0.7, 1.4);
        double cx = x + 0.5;
        double cz = z + 0.5;
        int trunkHeight = (int) Math.round(between(3.5, 6.0) * scale);
        t.info("scale", scale);
        t.info("trunk", trunkHeight);
        double topX = cx + jitter(0.7);
        double topZ = cz + jitter(0.7);
        t.branch(cx, y, cz, topX, y + trunkHeight, topZ);

        LimbStyle style = new LimbStyle(0.25, 0.05, 0.35, 0.7, 2, 3, 0.6, 0.75, 1.6, 2.5, 1.8, 0.6, 0.4, 0.3);
        int limbs = between(3, 4);
        double start = randomAngle();
        for (int i = 0; i < limbs; i++) {
            double yaw = start + i * Math.PI * 2 / limbs + jitter(0.4);
            int drop = between(0, 2);
            double progress = (double) (trunkHeight - drop) / trunkHeight;
            limb(cx + (topX - cx) * progress, y + trunkHeight - drop, cz + (topZ - cz) * progress,
                    yaw, between(0.45, 0.85), between(4.5, 6.5) * scale, scale > 1.15 ? 3 : 2, style);
        }
        limb(topX, y + trunkHeight, topZ, randomAngle(), 1.3, between(2.5, 4.0) * scale, 1, style);
        if (scale > 1.0) {
            surfaceRoots(cx, y, cz, between(3, 5), 1.5, 3.0 * scale, 0.6, 1, Material.OAK_WOOD);
        }
    }

    /**
     * #2 Old Giant Oak - grown from a 3x3 of saplings. A short, wide 3x3 base with deep roots, from which single
     * limbs reach out and climb into a huge, open crown.
     */
    private void oldGiantOak(int x, int y, int z) {
        t = new TreeBuilder(world, r, Material.OAK_WOOD, Material.OAK_LEAVES);
        t.setLeafMix(Material.OAK_LEAVES, Material.DARK_OAK_LEAVES, 0.2);
        t.setPorosity(0.28);
        double scale = between(1.0, 1.35);
        double cx = x + 1.5;
        double cz = z + 1.5;
        int baseHeight = between(3, 5);
        t.trunk(cx, cz, y, y + baseHeight, 1.5);
        // The trunk narrows to a 2x2 above the base
        int upperHeight = between(1, 3);
        t.trunk(x + 1, z + 1, y + baseHeight, y + baseHeight + upperHeight, 1.0);
        surfaceRoots(cx, y, cz, between(6, 8), 2.5, 3.5 * scale, 1.8, 2, Material.OAK_WOOD);
        t.info("scale", scale);
        t.info("base", baseHeight);

        LimbStyle style = new LimbStyle(0.25, 0.04, 0.45, 0.7, 2, 3, 0.58, 0.72, 1.6, 2.7, 1.9, 0.7, 0.4, 0.35);
        // Limbs leave from the outer edge of the base, angled more steeply the longer they are
        int[][] edge = {{0, 0}, {1, 0}, {2, 0}, {2, 1}, {2, 2}, {1, 2}, {0, 2}, {0, 1}};
        int limbs = between(5, 6);
        int first = r.nextInt(edge.length);
        for (int i = 0; i < limbs; i++) {
            int[] cell = edge[(first + i * edge.length / limbs) % edge.length];
            double sx = x + cell[0] + 0.5;
            double sz = z + cell[1] + 0.5;
            double yaw = Math.atan2(sz - cz, sx - cx) + jitter(0.4);
            double length = between(4.5, 6.5) * scale;
            double pitch = between(0.35, 0.6) + Math.max(0, length - 6) * 0.06;
            limb(sx, y + baseHeight - between(0, 1), sz, yaw, pitch, length, 2, style);
        }
        limb(x + 1, y + baseHeight + upperHeight, z + 1, randomAngle(), 1.25, between(4.0, 5.5) * scale, 2, style);
    }

    /**
     * #3 Tall Forest Oak - grown from a 2x2 of saplings. A tall 2x2 trunk that narrows near the top, with limbs
     * reaching steeply upwards into a narrow, high crown.
     */
    private void tallForestOak(int x, int y, int z) {
        t = new TreeBuilder(world, r, Material.OAK_WOOD, Material.OAK_LEAVES);
        t.setLeafMix(Material.OAK_LEAVES, Material.BIRCH_LEAVES, 0.12);
        t.setPorosity(0.22);
        double scale = between(0.8, 1.3);
        double cx = x + 1;
        double cz = z + 1;
        int trunkHeight = (int) Math.round(between(9.0, 12.0) * scale);
        int wideHeight = (int) (trunkHeight * between(0.6, 0.75));
        t.trunk(cx, cz, y, y + wideHeight, 1.0);
        // Above the 2x2 the trunk continues from one of its corners
        double upperX = x + r.nextInt(2) + 0.5;
        double upperZ = z + r.nextInt(2) + 0.5;
        double topX = upperX + jitter(0.8);
        double topZ = upperZ + jitter(0.8);
        t.branch(upperX, y + wideHeight, upperZ, topX, y + trunkHeight, topZ);
        t.info("scale", scale);
        t.info("trunk", trunkHeight);

        LimbStyle style = new LimbStyle(0.2, 0.02, 0.9, 0.5, 2, 2, 0.6, 0.72, 1.4, 2.3, 1.8, 0.5, 0.4, 0.4);
        int limbs = between(5, 7);
        double yaw = randomAngle();
        for (int i = 0; i < limbs; i++) {
            double progress = 0.5 + 0.4 * i / Math.max(1, limbs - 1) + jitter(0.04);
            double h = y + trunkHeight * progress;
            double sx;
            double sz;
            if (h <= y + wideHeight) {
                sx = cx;
                sz = cz;
            } else {
                double upper = (h - y - wideHeight) / Math.max(1, trunkHeight - wideHeight);
                sx = upperX + (topX - upperX) * upper;
                sz = upperZ + (topZ - upperZ) * upper;
            }
            yaw += GOLDEN_ANGLE;
            limb(sx, h, sz, yaw, between(0.75, 1.1), between(3.0, 4.8) * scale, 2, style);
        }
        limb(topX, y + trunkHeight, topZ, randomAngle(), 1.4, 3.0 * scale, 1, style);
    }

    // ------------------------------------------------------------
    // Birch
    // ------------------------------------------------------------

    /**
     * #4 Silver Birch - a tall, slender and slightly leaning trunk, with a full but airy crown made of tufts spiralling
     * evenly around the upper trunk.
     */
    private void silverBirch(int x, int y, int z) {
        t = new TreeBuilder(world, r, Material.BIRCH_WOOD, Material.BIRCH_LEAVES);
        t.setLeafMix(Material.BIRCH_LEAVES, Material.OAK_LEAVES, 0.12);
        t.setPorosity(0.15);
        double cx = x + 0.5;
        double cz = z + 0.5;
        int height = between(11, 20);
        double leanAngle = randomAngle();
        double lean = chance(0.65) ? between(0.6, 1.3) : 0;
        double midY = y + height * 0.55;
        double topX = cx + Math.cos(leanAngle) * lean;
        double topZ = cz + Math.sin(leanAngle) * lean;
        t.branch(cx, y, cz, cx, midY, cz);
        t.branch(cx, midY, cz, topX, y + height, topZ);
        t.info("height", height);

        // A slim core so that the tufts read as one crown
        double crownX = (cx + topX) / 2;
        double crownZ = (cz + topZ) / 2;
        t.blob(crownX, y + height * 0.72, crownZ, 1.8, height * 0.3, 1.8, 0.3);

        // Tufts are evenly spaced around the trunk and kept close to it, so the crown looks full but tidy
        LimbStyle tuft = new LimbStyle(0.1, 0, 0.8, 0, 1, 1, 1, 1, 99, 2.1, 1.6, 0.4, 0.3, 0);
        double yaw = randomAngle();
        double h = height * 0.42;
        while (h < height * 0.92) {
            double progress = h / height;
            double sx = progress < 0.55 ? cx : cx + (topX - cx) * (progress - 0.55) / 0.45;
            double sz = progress < 0.55 ? cz : cz + (topZ - cz) * (progress - 0.55) / 0.45;
            yaw += GOLDEN_ANGLE;
            double length = between(1.6, 2.4) * (1 - 0.4 * (progress - 0.42) / 0.5);
            limb(sx, y + h, sz, yaw, between(0.75, 1.0), length, 0, tuft);
            h += between(1.2, 2.0);
        }
        t.blob(topX, y + height + 1, topZ, 1.8, 1.8, 1.8, 0.2);
    }

    /**
     * #41 Column Birch - a very tall, narrow column of birch leaves that starts low on the trunk and tapers to a point,
     * held up by short branches sweeping steeply upwards that always stay hidden inside the leaves.
     */
    private void columnBirch(int x, int y, int z) {
        t = new TreeBuilder(world, r, Material.BIRCH_WOOD, Material.BIRCH_LEAVES);
        // The column is too thin for air pockets, which would cut lines of missing leaves through it
        t.setPorosity(0);
        double cx = x + 0.5;
        double cz = z + 0.5;
        int height = between(14, 26);
        int crownStart = between(3, 4);
        int top = y + height + 2;
        // Taller columns are wider, so that none of them look too thin
        double width = between(2.3, 2.8) + (height - 14) * 0.03;
        t.info("height", height);

        // The trunk stops a few blocks below the tip of the column so its top stays covered
        t.trunk(cx, cz, y, y + height - 1, 0.5);
        for (int h = y + crownStart; h <= top; h++) {
            t.disc(cx, h, cz, columnRadius(h, y + crownStart, top, width), 0.12);
        }

        // Each branch is kept at least a block and a half inside the leaves at the height where it ends
        for (int h = y + crownStart + 1; h < y + height - 4; h += between(1, 2)) {
            double rise = between(1.0, 1.6);
            double room = columnRadius((int) Math.round(h + rise), y + crownStart, top, width) - 1.5;
            double length = Math.min(between(0.6, 1.1), room);
            if (length < 0.5) {
                continue;
            }
            double angle = randomAngle();
            t.branch(cx, h, cz, cx + Math.cos(angle) * length, h + rise, cz + Math.sin(angle) * length);
        }
        fluff(0.2);
    }

    /**
     * Turns a share of the leaves into air, scattered one by one rather than in pockets, so the leaves look fluffy.
     * Leaves touching the wood are kept, so the wood stays hidden.
     * @param share The share of the leaves to remove.
     */
    private void fluff(double share) {
        for (Pos pos : t.getPositions(Layer.LEAF)) {
            if (chance(share) && !touchesWood(pos)) {
                t.removeLeaf(pos.x(), pos.y(), pos.z());
            }
        }
    }

    private boolean touchesWood(Pos pos) {
        for (BlockFace face : new BlockFace[] {BlockFace.NORTH, BlockFace.EAST, BlockFace.SOUTH, BlockFace.WEST,
                BlockFace.UP, BlockFace.DOWN}) {
            Material type = t.typeAt(pos.x() + face.getModX(), pos.y() + face.getModY(), pos.z() + face.getModZ());
            if (type != null && Tag.LOGS.isTagged(type)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Wraps every block of wood at or above a height in a small cluster of leaves, so that no bare branches show in the
     * crown.
     */
    private void coverWood(int minY, double radius) {
        for (Pos pos : t.getPositions(Layer.LOG)) {
            if (pos.y() >= minY) {
                t.blob(pos.x() + 0.5, pos.y() + 0.5, pos.z() + 0.5, radius, radius * 0.85, radius, 0.3);
            }
        }
    }

    /**
     * The radius of the column of leaves of a {@link #columnBirch} at a height. It is widest a third of the way up, then
     * tapers to a point above the trunk.
     */
    private double columnRadius(int h, int start, int top, double width) {
        double progress = Math.max(0, Math.min(1, (double) (h - start) / (top - start)));
        return progress < 0.3
                ? 1.9 + progress / 0.3 * (width - 1.9)
                : width * Math.pow(1 - (progress - 0.3) / 0.7, 0.7) + 0.5;
    }

    /**
     * The point a 2x2 birch trunk passes through at a height above its base, bending away from straight up.
     * @param lean How far the top of the trunk is from above its base.
     * @param bend How the lean is spread up the trunk: 1 leans evenly, higher values stay upright longer and then curve.
     */
    private double[] birchPath(int x, int z, double h, int height, double lean, double angle, double bend) {
        double offset = lean * Math.pow(Math.max(0, h) / height, bend);
        return new double[] {x + 1 + Math.cos(angle) * offset, z + 1 + Math.sin(angle) * offset};
    }

    /**
     * Builds the trunk of a birch grown from a 2x2 of saplings. It is a 2x2 of wood up to a height, then narrows to a
     * single block up to its top, following {@link #birchPath}.
     * @param wide How many blocks of the trunk are 2x2.
     */
    private void birchTrunk(int x, int y, int z, int height, int wide, double lean, double angle, double bend) {
        for (int h = 0; h < wide; h++) {
            double[] p = birchPath(x, z, h, height, lean, angle, bend);
            placeSquare((int) Math.round(p[0]) - 1, y + h, (int) Math.round(p[1]) - 1);
        }
        // The single block wide part starts from the block of the last 2x2 on the side the trunk leans towards
        double[] last = birchPath(x, z, wide - 1, height, lean, angle, bend);
        int squareX = (int) Math.round(last[0]) - 1;
        int squareZ = (int) Math.round(last[1]) - 1;
        double fromX = squareX + (Math.cos(angle) >= 0 ? 1.5 : 0.5);
        double fromZ = squareZ + (Math.sin(angle) >= 0 ? 1.5 : 0.5);
        double[] mid = birchPath(x, z, (wide + height) / 2.0, height, lean, angle, bend);
        double[] top = birchPath(x, z, height, height, lean, angle, bend);
        t.branch(fromX, y + wide - 1, fromZ, mid[0], y + (wide + height) / 2.0, mid[1]);
        t.branch(mid[0], y + (wide + height) / 2.0, mid[1], top[0], y + height, top[1]);
    }

    /**
     * Grows tufts of birch leaves spiralling evenly up a 2x2 birch trunk, shorter towards the top, like the crown of #4.
     * @param from The share of the height that the tufts start at.
     * @param to The share of the height that the tufts stop at.
     */
    private void birchTufts(int x, int y, int z, int height, double lean, double angle, double bend, double from,
                            double to, double minLength, double maxLength, LimbStyle tuft) {
        double yaw = randomAngle();
        double h = height * from;
        while (h < height * to) {
            double progress = (h / height - from) / (to - from);
            double[] p = birchPath(x, z, h, height, lean, angle, bend);
            yaw += GOLDEN_ANGLE;
            double length = between(minLength, maxLength) * (1 - 0.45 * progress);
            limb(p[0], y + h, p[1], yaw, between(0.7, 1.0), length, 0, tuft);
            h += between(1.2, 1.9);
        }
    }

    /**
     * #46 Birch Spire - grown from a 2x2 of saplings. A giant Column Birch: a tall, fluffy column of birch leaves around
     * a 2x2 trunk, widest a third of the way up and tapering to a point high above the forest.
     */
    private void birchSpire(int x, int y, int z) {
        t = new TreeBuilder(world, r, Material.BIRCH_WOOD, Material.BIRCH_LEAVES);
        // Like #41, no air pockets, so no lines of missing leaves cut through the column
        t.setPorosity(0);
        double cx = x + 1;
        double cz = z + 1;
        int height = between(26, 36);
        int crownStart = between(4, 6);
        int top = y + height + 3;
        double width = between(3.4, 4.0) + (height - 26) * 0.04;
        t.info("height", height);
        birchTrunk(x, y, z, height - 2, (int) Math.round(height * 0.5), 0, 0, 1);
        for (int h = y + crownStart; h <= top; h++) {
            double progress = Math.max(0, Math.min(1, (double) (h - y - crownStart) / (top - y - crownStart)));
            double radius = progress < 0.3
                    ? 2.6 + progress / 0.3 * (width - 2.6)
                    : width * Math.pow(1 - (progress - 0.3) / 0.7, 0.7) + 0.6;
            t.disc(cx, h, cz, radius, 0.12);
        }
        // Short branches keep the wide middle of the column close enough to the wood, and stay hidden inside it
        for (int h = y + crownStart + 1; h < y + height - 6; h += between(1, 2)) {
            double angle = randomAngle();
            double length = between(1.0, 1.8);
            t.branch(cx, h, cz, cx + Math.cos(angle) * length, h + between(1.0, 1.6), cz + Math.sin(angle) * length);
        }
        fluff(0.2);
    }

    /**
     * #48 Leaning Birch - grown from a 2x2 of saplings. An old birch whose thick 2x2 trunk grows upright and then curves
     * well over to one side, with its crown hanging out beyond its base, a few broken stubs, and more shelf mushrooms
     * than other birches.
     */
    private void leaningBirch(int x, int y, int z) {
        t = new TreeBuilder(world, r, Material.BIRCH_WOOD, Material.BIRCH_LEAVES);
        t.setPorosity(0.15);
        int height = between(18, 24);
        int wide = (int) Math.round(height * between(0.5, 0.6));
        double angle = randomAngle();
        double lean = between(3.5, 5.5);
        t.info("height", height);
        birchTrunk(x, y, z, height, wide, lean, angle, 1.8);
        // Roots brace the trunk on the side away from the lean
        surfaceRoots(x + 1 - Math.cos(angle) * 0.5, y, z + 1 - Math.sin(angle) * 0.5, between(3, 4), 1.5, 2.5, 1.2, 1,
                Material.BIRCH_WOOD);
        // Broken stubs where lower branches fell off
        int stubs = between(2, 4);
        for (int i = 0; i < stubs; i++) {
            int h = between(3, wide - 1);
            double[] p = birchPath(x, z, h, height, lean, angle, 1.8);
            BlockFace face = HORIZONTAL[r.nextInt(4)];
            int bx = (int) Math.round(p[0]) - 1 + (face.getModX() > 0 ? 2 : face.getModX() < 0 ? -1 : r.nextInt(2));
            int bz = (int) Math.round(p[1]) - 1 + (face.getModZ() > 0 ? 2 : face.getModZ() < 0 ? -1 : r.nextInt(2));
            t.wood(bx, y + h, bz);
        }

        double[] core = birchPath(x, z, height * 0.75, height, lean, angle, 1.8);
        t.blob(core[0], y + height * 0.75, core[1], 2.3, height * 0.22, 2.3, 0.3);
        LimbStyle tuft = new LimbStyle(0.1, 0, 0.8, 0, 1, 1, 1, 1, 99, 2.3, 1.8, 0.4, 0.3, 0);
        birchTufts(x, y, z, height, lean, angle, 1.8, 0.5, 0.93, 2.2, 3.2, tuft);
        double[] top = birchPath(x, z, height, height, lean, angle, 1.8);
        t.blob(top[0], y + height + 1, top[1], 2.1, 2.0, 2.1, 0.2);
    }

    // ------------------------------------------------------------
    // Poplar
    // ------------------------------------------------------------

    /**
     * Starts a poplar, with dark poplar wood and leaves of a single colour, red, orange or yellow, picked for the tree.
     */
    private void startPoplar() {
        Material[] colours = {Material.RED_POPLAR_LEAVES, Material.ORANGE_POPLAR_LEAVES, Material.YELLOW_POPLAR_LEAVES};
        Material leaves = colours[r.nextInt(colours.length)];
        t = new TreeBuilder(world, r, Material.POPLAR_WOOD, leaves);
        t.setPorosity(0.2);
        t.info("leaves", leaves.name().toLowerCase().replace("_poplar_leaves", ""));
    }

    /**
     * Places short, single block stubs sticking out of the side of a 1 wide poplar trunk, where old branches broke off.
     */
    private void poplarStubs(int x, int z, int fromY, int toY, int count) {
        for (int i = 0; i < count; i++) {
            BlockFace face = HORIZONTAL[r.nextInt(4)];
            int h = between(fromY, toY);
            t.wood(x + face.getModX(), h, z + face.getModZ());
        }
    }

    /**
     * #5 Poplar - an autumn tree. A dark trunk, bare for its lower part, that splits into several limbs which keep
     * forking into a big, bushy and rounded crown of leaves in a single colour, with fallen leaves beneath and the odd
     * shelf mushroom on the trunk.
     */
    private void poplar(int x, int y, int z) {
        startPoplar();
        double scale = between(0.9, 1.15);
        double cx = x + 0.5;
        double cz = z + 0.5;
        int trunkHeight = between(4, 6);
        double topX = cx + jitter(0.6);
        double topZ = cz + jitter(0.6);
        t.branch(cx, y, cz, topX, y + trunkHeight, topZ);
        t.info("trunk", trunkHeight);
        t.info("scale", scale);
        poplarStubs(x, z, y + 2, y + trunkHeight - 1, between(1, 2));

        // The trunk splits only 2 or 3 ways, into limbs rising at about 45 degrees that fork once at most. They end in
        // big round clusters that overlap, so the crown reads as one compact, bushy mass
        LimbStyle style = new LimbStyle(0.2, 0.02, 0.8, 0.45, 1, 2, 0.6, 0.75, 1.5, 3.7, 3.0, 0.6, 0.35, 1.0);
        int limbs = between(2, 3);
        double start = randomAngle();
        for (int i = 0; i < limbs; i++) {
            double yaw = start + i * Math.PI * 2 / limbs + jitter(0.35);
            limb(topX, y + trunkHeight, topZ, yaw, between(0.7, 0.9), between(4.0, 5.0) * scale, 1, style);
        }
        // The trunk carries on a little way up the middle, under a core of leaves that fills in the centre of the crown
        t.branch(topX, y + trunkHeight, topZ, topX + jitter(0.5), y + trunkHeight + 3 * scale, topZ + jitter(0.5));
        t.blob(topX, y + trunkHeight + 4.5 * scale, topZ, 4.2 * scale, 3.5 * scale, 4.2 * scale, 0.4);
        // Leaves wrap every branch in the crown, so no bare wood shows through
        coverWood(y + trunkHeight + 1, 1.8);
        if (chance(0.45)) {
            shelfMushrooms(between(1, 2), y + 1, y + trunkHeight - 1);
        }
    }

    /**
     * #44 Great Poplar - grown from a 2x2 of saplings. A huge autumn tree, about 1.7 times as wide as #5, on a thick trunk
     * with a wide flared base and roots. Thick limbs fork out into a vast, bushy and rounded crown of leaves in a single
     * colour, like a tree in the middle of an old autumn forest.
     */
    private void greatPoplar(int x, int y, int z) {
        startPoplar();
        double scale = between(0.95, 1.15);
        double cx = x + 1;
        double cz = z + 1;
        int trunkHeight = between(6, 8);
        t.trunk(cx, cz, y, y + trunkHeight, 1.0);
        t.info("trunk", trunkHeight);
        t.info("scale", scale);

        // A wide base that flares out unevenly, rising higher on some sides, with roots spreading out from it
        int[][] flare = {{-1, 0}, {-1, 1}, {2, 0}, {2, 1}, {0, -1}, {1, -1}, {0, 2}, {1, 2}};
        for (int[] offset : flare) {
            int rise = between(1, 3);
            for (int h = 0; h < rise; h++) {
                t.wood(x + offset[0], y + h, z + offset[1]);
            }
        }
        int[][] corners = {{-1, -1}, {2, -1}, {-1, 2}, {2, 2}};
        for (int[] corner : corners) {
            if (chance(0.5)) {
                t.wood(x + corner[0], y, z + corner[1]);
            }
        }
        surfaceRoots(cx, y, cz, between(5, 7), 2.0, 3.5, 1.6, 1, Material.POPLAR_WOOD);

        // Thick limbs leave the top of the trunk and fork out into the crown
        // Limbs rise at about 45 degrees, halfway between spreading flat and climbing straight up
        // The ends of the limbs carry big masses of leaves, while their lower parts are left partly bare
        LimbStyle style = new LimbStyle(0.32, 0.02, 0.8, 0.6, 2, 3, 0.6, 0.74, 1.6, 4.1, 3.2, 0.8, 0.35, 0.3);
        int limbs = between(5, 7);
        double start = randomAngle();
        for (int i = 0; i < limbs; i++) {
            double yaw = start + i * Math.PI * 2 / limbs + jitter(0.3);
            double sy = y + trunkHeight - between(0, 2);
            // Limbs alternate between climbing steeply and spreading wider, and wander as they grow, so they never all
            // run out along the same straight diagonal
            boolean steep = i % 2 == 0;
            double pitch = steep ? between(0.95, 1.15) : between(0.55, 0.7);
            double length = (steep ? between(8.0, 9.5) : between(8.5, 10.5)) * scale;
            thickLimb(cx + Math.cos(yaw) * 0.5, sy, cz + Math.sin(yaw) * 0.5, yaw, pitch, length, 2, style, 0.9);
        }
        // A thick leader up the middle and a core of leaves fill in the centre of the crown
        thickLimb(cx, y + trunkHeight, cz, randomAngle(), 1.35, between(6.0, 7.5) * scale, 2, style, 0.9);
        t.blob(cx, y + trunkHeight + 6.5 * scale, cz, 6.0 * scale, 4.5 * scale, 6.0 * scale, 0.4);
        // Plenty of volume up top, so the crown is rounded and full rather than thin above the limbs
        t.blob(cx + jitter(1), y + trunkHeight + 11 * scale, cz + jitter(1), 6.5 * scale, 4.0 * scale, 6.5 * scale, 0.4);
        if (chance(0.6)) {
            shelfMushrooms(between(1, 3), y + 1, y + trunkHeight - 1);
        }
    }

    // ------------------------------------------------------------
    // Spruce
    // ------------------------------------------------------------

    /**
     * Grows a flat spray of needles out from the trunk that curves downwards towards its tip.
     * The wood inside it always stops short of the tip, so it is covered by needles.
     * @param droop How strongly the spray curves downwards.
     */
    private void spray(double cx, int y, double cz, double yaw, double length, double droop) {
        double dx = Math.cos(yaw);
        double dz = Math.sin(yaw);
        double woodLength = length - 2.0;
        if (woodLength >= 1) {
            t.branch(cx, y, cz, cx + dx * woodLength, y - droop * woodLength * woodLength / length, cz + dz * woodLength);
        }
        for (double s = 0.5; s <= length; s += 0.5) {
            double px = cx + dx * s;
            double py = y - droop * s * s / length;
            double pz = cz + dz * s;
            double width = 1.4 * (1 - 0.45 * s / length);
            int by = (int) Math.floor(py);
            for (int ox = -2; ox <= 2; ox++) {
                for (int oz = -2; oz <= 2; oz++) {
                    int bx = (int) Math.floor(px) + ox;
                    int bz = (int) Math.floor(pz) + oz;
                    double ddx = bx + 0.5 - px;
                    double ddz = bz + 0.5 - pz;
                    if (ddx * ddx + ddz * ddz <= width * width) {
                        t.leaf(bx, by, bz);
                    }
                }
            }
            // Needles cover the top of the wood, and the tips droop
            if (s <= woodLength + 0.5 || (s < length * 0.6 && chance(0.6))) {
                t.leaf((int) Math.floor(px), by + 1, (int) Math.floor(pz));
            }
            if (s > length - 1 && chance(0.5)) {
                t.leaf((int) Math.floor(px), by - 1, (int) Math.floor(pz));
            }
        }
    }

    /**
     * Places a small pointed leader of needles at the top of a spruce.
     */
    private void spruceTop(double x, int y, double z) {
        int bx = (int) Math.floor(x);
        int bz = (int) Math.floor(z);
        t.disc(x, y, z, 1.2, 0.2);
        int leaderHeight = between(2, 3);
        for (int i = 1; i <= leaderHeight; i++) {
            t.leaf(bx, y + i, bz);
        }
    }

    /**
     * #6 Mountain Spruce - grown from a 2x2 of saplings. An irregular, uneven spruce on a 2x2 trunk whose layers of
     * drooping sprays vary in length, leaning slightly away from the wind. Sizes vary greatly.
     */
    private void mountainSpruce(int x, int y, int z) {
        t = new TreeBuilder(world, r, Material.SPRUCE_WOOD, Material.SPRUCE_LEAVES);
        double cx = x + 1;
        double cz = z + 1;
        int height = between(20, 34);
        int wideHeight = (int) (height * between(0.65, 0.8));
        t.trunk(cx, cz, y, y + wideHeight, 1.0);
        // Above the 2x2 the trunk continues from one of its corners
        double upperX = x + r.nextInt(2) + 0.5;
        double upperZ = z + r.nextInt(2) + 0.5;
        t.trunk(upperX, upperZ, y + wideHeight, y + height, 0.5);
        surfaceRoots(cx, y, cz, between(4, 6), 1.5, 3.0, 1.2, 2, Material.SPRUCE_WOOD);
        t.info("height", height);

        int crownBase = y + between(3, 6);
        double maxLength = height * between(0.17, 0.24) + 0.7;
        double exponent = between(0.75, 1.15);
        double windYaw = randomAngle();
        double windBias = between(0.0, 0.2);
        int h = crownBase;
        while (h < y + height - 1) {
            double progress = (double) (h - crownBase) / (y + height - crownBase);
            double length = maxLength * Math.pow(1 - progress, exponent) + 0.8;
            int sprays = length > 2.5 ? between(5, 7) : between(3, 5);
            double start = randomAngle();
            for (int i = 0; i < sprays; i++) {
                double yaw = start + i * Math.PI * 2 / sprays + jitter(0.45);
                double sprayLength = length * between(0.7, 1.15) * (1 + windBias * Math.cos(yaw - windYaw));
                double sx = h <= y + wideHeight ? cx : upperX;
                double sz = h <= y + wideHeight ? cz : upperZ;
                spray(sx, h, sz, yaw, sprayLength, between(0.25, 0.4));
            }
            t.disc(h <= y + wideHeight ? cx : upperX, h + 1, h <= y + wideHeight ? cz : upperZ, 1.8, 0.3);
            h += between(2, 3);
        }
        spruceTop(upperX + jitter(0.5), y + height, upperZ + jitter(0.5));
        if (chance(0.2)) {
            berryPatch(cx, y, cz, 5, 9);
        }
    }

    /**
     * #7 Subalpine Spire - a tall, narrow column of short, heavily drooping sprays that only tapers near the top.
     */
    private void subalpineSpire(int x, int y, int z) {
        t = new TreeBuilder(world, r, Material.SPRUCE_WOOD, Material.SPRUCE_LEAVES);
        double cx = x + 0.5;
        double cz = z + 0.5;
        // Narrowed towards the samples that were liked, avoiding the shortest and stubbiest spires
        int height = between(18, 28);
        t.trunk(cx, cz, y, y + height, 0.5);
        t.info("height", height);

        int crownBase = y + between(1, 3);
        double maxLength = between(2.4, 3.1);
        t.info("length", maxLength);
        int h = crownBase;
        while (h < y + height - 1) {
            double progress = (double) (h - crownBase) / (y + height - crownBase);
            double length = progress < 0.7
                    ? maxLength * (1 - 0.25 * progress)
                    : maxLength * 0.82 * (1 - (progress - 0.7) / 0.3) + 0.6;
            int sprays = between(4, 6);
            double start = randomAngle();
            for (int i = 0; i < sprays; i++) {
                double yaw = start + i * Math.PI * 2 / sprays + jitter(0.4);
                spray(cx, h, cz, yaw, length * between(0.8, 1.1), between(0.5, 0.8));
            }
            h += between(1, 2);
        }
        spruceTop(cx, y + height, cz);
    }

    /**
     * #8 Ragged Old Spruce - a tall, weathered spruce with a high crown of uneven, gappy layers of sprays,
     * a few tufts on the bare lower trunk, and often a second leader splitting off near the top.
     */
    private void raggedSpruce(int x, int y, int z) {
        t = new TreeBuilder(world, r, Material.SPRUCE_WOOD, Material.SPRUCE_LEAVES);
        double cx = x + 0.5;
        double cz = z + 0.5;
        int height = between(20, 32);
        t.trunk(cx, cz, y, y + height, 0.5);
        t.info("height", height);

        int crownBase = y + (int) (height * between(0.3, 0.45));
        // Small tufts growing out of the bare lower trunk
        for (int i = 0; i < between(1, 3); i++) {
            int tuftY = y + between(3, Math.max(4, crownBase - y - 1));
            BlockFace face = HORIZONTAL[r.nextInt(4)];
            t.blob(cx + face.getModX(), tuftY + 0.5, cz + face.getModZ(), 1.2, 0.8, 1.2, 0.3);
        }

        double maxLength = height * between(0.16, 0.22);
        int h = crownBase;
        while (h < y + height - 1) {
            double progress = (double) (h - crownBase) / (y + height - crownBase);
            double length = maxLength * Math.pow(1 - progress, 0.9) + 0.8;
            if (!chance(0.3)) {
                int sprays = between(2, 5);
                double start = randomAngle();
                for (int i = 0; i < sprays; i++) {
                    double yaw = start + i * Math.PI * 2 / sprays + jitter(0.6);
                    spray(cx, h, cz, yaw, length * between(0.45, 1.3), between(0.2, 0.5));
                }
            }
            h += between(2, 4);
        }
        spruceTop(cx, y + height, cz);

        // A second leader splitting off near the top, a single step from the trunk and wrapped in needles
        if (chance(0.35)) {
            int splitY = y + (int) (height * 0.85);
            BlockFace face = HORIZONTAL[r.nextInt(4)];
            double leaderX = cx + face.getModX();
            double leaderZ = cz + face.getModZ();
            t.trunk(leaderX, leaderZ, splitY + 1, y + height - 1, 0.5);
            for (int ly = splitY + 1; ly <= y + height - 1; ly++) {
                t.disc(leaderX, ly, leaderZ, 1.3, 0.2);
            }
            spruceTop(leaderX, y + height - 1, leaderZ);
        }
    }

    // ------------------------------------------------------------
    // Jungle
    // ------------------------------------------------------------

    /**
     * #9 Jungle Emergent - a towering 2x2 trunk braced by buttress roots that dig deep into the ground,
     * with side branches along the trunk, a broad umbrella canopy, vines and cocoa.
     */
    private void jungleEmergent(int x, int y, int z) {
        t = new TreeBuilder(world, r, Material.JUNGLE_WOOD, Material.JUNGLE_LEAVES);
        t.setLeafMix(Material.JUNGLE_LEAVES, Material.OAK_LEAVES, 0.15);
        t.setPorosity(0.2);
        double scale = between(0.8, 1.25);
        double cx = x + 1;
        double cz = z + 1;
        int height = (int) Math.round(between(20.0, 28.0) * scale) - between(5, 10);
        t.trunk(cx, cz, y, y + height, 1.0);

        // Buttress roots that continue deep into the ground
        double rootStart = randomAngle();
        for (int i = 0; i < 4; i++) {
            double angle = rootStart + i * Math.PI / 2 + jitter(0.3);
            double length = between(4.0, 5.5);
            double ox = Math.cos(angle);
            double oz = Math.sin(angle);
            t.rootLine(cx + ox, y + 4.5, cz + oz, cx + ox * length, y - 0.5, cz + oz * length, Material.JUNGLE_WOOD, false);
            t.rootLine(cx + ox, y + 2.5, cz + oz, cx + ox * (length - 1), y - 0.5, cz + oz * (length - 1), Material.JUNGLE_WOOD, false);
            t.rootLine(cx + ox * length, y - 0.5, cz + oz * length, cx + ox * (length + 1.5), y - between(2.5, 4.5),
                    cz + oz * (length + 1.5), Material.JUNGLE_WOOD, false);
        }

        // Side branches along the upper trunk, always rising rather than running flat
        LimbStyle side = new LimbStyle(0.2, 0.01, 0.6, 0.6, 2, 2, 0.6, 0.7, 1.5, 2.6, 1.7, 0.5, 0.35, 0.3);
        int sideBranches = between(3, 5);
        double yaw = randomAngle();
        for (int i = 0; i < sideBranches; i++) {
            yaw += GOLDEN_ANGLE;
            double h = y + height * between(0.45, 0.8);
            limb(cx, h, cz, yaw, between(0.55, 0.85), between(3.0, 5.0) * scale, 1, side);
        }

        // Umbrella canopy
        LimbStyle canopy = new LimbStyle(0.25, 0.015, 0.4, 0.6, 2, 3, 0.58, 0.72, 1.6, 3.0, 1.8, 0.5, 0.35, 0.35);
        int limbs = 6;
        double start = randomAngle();
        for (int i = 0; i < limbs; i++) {
            double angle = start + i * Math.PI * 2 / limbs + jitter(0.3);
            limb(cx, y + height - 1, cz, angle, between(0.45, 0.7), between(5.0, 7.0) * scale, 2, canopy);
        }
        t.blob(cx, y + height + 2, cz, 4.0, 2.0, 4.0, 0.35);

        trunkVines(x, z, 2, y + 2, y + height - 3, 0.3);
        cocoaPods(between(10, 16), y + 2);
        hangingVines(0.1, 6, y + (int) (height * 0.45));
        mossOnBranches(y + 4, 0.14);
    }

    /**
     * #10 Kapok - grown from a 5x5 of saplings. A massive trunk that swells smoothly out into buttress fins at its base,
     * ending well below the canopy. Thick limbs sweep up and out into a wide canopy that slopes down at its edges, with
     * smaller branches both up in the canopy and lower down the trunk, all draped in vines.
     */
    private void kapok(int x, int y, int z) {
        t = new TreeBuilder(world, r, Material.JUNGLE_WOOD, Material.JUNGLE_LEAVES);
        t.setLeafMix(Material.JUNGLE_LEAVES, Material.OAK_LEAVES, 0.12);
        t.setPorosity(0.22);
        double scale = between(0.95, 1.15);
        double cx = x + 2.5;
        double cz = z + 2.5;
        int height = (int) Math.round(between(25.0, 31.0) * scale);
        // The trunk is pulled down below the canopy, which stays at the same height
        int drop = between(7, 11);
        int trunkTop = y + height - drop;
        t.info("scale", scale);
        t.info("height", height);
        t.info("drop", drop);

        // The trunk narrows smoothly from a wide, uneven base, rather than in sudden steps
        int trunkHeight = trunkTop - y;
        for (int h = 0; h <= trunkHeight; h++) {
            double p = (double) h / trunkHeight;
            double radius = 1.6 + 0.5 * (1 - p) + 1.5 * Math.exp(-h / 2.5) + jitter(0.2);
            t.trunk(cx + jitter(0.15), cz + jitter(0.15), y + h, y + h, radius);
        }

        // Buttress fins that taper away from the trunk and dig into the ground at their ends
        int fins = between(5, 7);
        double start = randomAngle();
        for (int i = 0; i < fins; i++) {
            double angle = start + i * Math.PI * 2 / fins + jitter(0.25);
            double ox = Math.cos(angle);
            double oz = Math.sin(angle);
            int finLength = between(6, 8);
            int finHeight = between(4, 6);
            for (int d = 3; d <= finLength; d++) {
                int fx = (int) Math.floor(cx + ox * d);
                int fz = (int) Math.floor(cz + oz * d);
                int top = (int) Math.round(finHeight * (1 - (double) (d - 2) / finLength));
                for (int fy = y - 1; fy <= y + top - 1; fy++) {
                    t.root(fx, fy, fz, Material.JUNGLE_WOOD, Axis.Y, false);
                }
            }
            t.rootLine(cx + ox * finLength, y - 1, cz + oz * finLength, cx + ox * (finLength + 2), y - between(3, 5),
                    cz + oz * (finLength + 2), Material.JUNGLE_WOOD, false);
        }

        // Smaller branches lower down the trunk
        LimbStyle small = new LimbStyle(0.25, 0.04, 0.3, 0.6, 2, 2, 0.55, 0.7, 1.4, 2.3, 1.4, 0.4, 0.35, 0.3);
        int lowBranches = between(3, 5);
        double lowYaw = randomAngle();
        for (int i = 0; i < lowBranches; i++) {
            lowYaw += GOLDEN_ANGLE + jitter(0.3);
            double h = y + trunkHeight * between(0.45, 0.8);
            double sx = cx + Math.cos(lowYaw) * 1.5;
            double sz = cz + Math.sin(lowYaw) * 1.5;
            double ex = sx + Math.cos(lowYaw) * 2.5;
            double ez = sz + Math.sin(lowYaw) * 2.5;
            t.thickBranch(sx, h, sz, ex, h + 1.2, ez, 0.7, 0.5);
            // Angled upwards so they never stick straight out a long way
            limb(ex, h + 1.2, ez, lowYaw, between(0.45, 0.75), between(2.5, 4.0), 1, small);
        }

        // Limbs droop towards their tips so the canopy slopes down gently at its edges instead of staying flat
        LimbStyle canopy = new LimbStyle(0.25, 0.09, -0.05, 0.65, 2, 2, 0.55, 0.7, 1.8, 2.6, 1.5, 0.5, 0.4, 0.3);
        LimbStyle upper = new LimbStyle(0.25, 0.02, 0.6, 0.6, 2, 2, 0.55, 0.7, 1.4, 2.4, 1.5, 0.4, 0.35, 0.3);
        int limbs = between(6, 8);
        double limbStart = randomAngle();
        for (int i = 0; i < limbs; i++) {
            double angle = limbStart + i * Math.PI * 2 / limbs + jitter(0.3);
            // Each thick limb sweeps smoothly up and out from the trunk, some ending lower and closer in than others, so
            // that the canopy widens gradually from the trunk instead of all at once at a single flat height
            double level = i % 2 == 0 ? between(0.85, 1.0) : between(0.5, 0.75);
            double reach = (2.0 + drop * between(0.4, 0.6)) * (0.55 + 0.45 * level);
            double rise = (y + height - 1 - trunkTop) * level + between(0, 1);
            double thickness = between(1.2, 1.7);
            double armAngle = angle + jitter(0.15);
            double sx = cx + Math.cos(angle) * 1.2;
            double sy = trunkTop - between(1, 3);
            double sz = cz + Math.sin(angle) * 1.2;
            double px = sx;
            double py = sy;
            double pz = sz;
            for (int k = 1; k <= 4; k++) {
                double p = k / 4.0;
                double out = reach * (1 - Math.cos(p * Math.PI / 2));
                double nx = sx + Math.cos(armAngle) * out;
                double ny = sy + rise * Math.sin(p * Math.PI / 2);
                double nz = sz + Math.sin(armAngle) * out;
                t.thickBranch(px, py, pz, nx, ny, nz, thickness * (1 - 0.1 * (k - 1)), thickness * (1 - 0.1 * k));
                px = nx;
                py = ny;
                pz = nz;
            }
            // The limb stays thick for the first part of its spread, then splits into smaller limbs. It splits early
            // and keeps rising slightly, so that no limb runs out flat and straight for a long way
            double length = between(6.5, 8.5) * scale * (0.7 + 0.3 * level);
            double pitch = between(0.25, 0.4) + 0.15 * (1 - level);
            double thickLength = length * 0.3;
            double ex = px + Math.cos(angle) * Math.cos(pitch) * thickLength;
            double ey = py + Math.sin(pitch) * thickLength;
            double ez = pz + Math.sin(angle) * Math.cos(pitch) * thickLength;
            t.thickBranch(px, py, pz, ex, ey, ez, thickness * 0.65, 0.9);
            limb(ex, ey, ez, angle + jitter(0.2), pitch, length * 0.55, 3, canopy);
            // Smaller branches rising out of the top of the canopy
            for (int j = 0; j < between(1, 2); j++) {
                limb(px, py, pz, angle + jitter(0.8), between(0.6, 0.9), between(3.0, 4.5), 1, upper);
            }
        }
        // A stout leader keeps the middle of the canopy filled, rounding off its top
        double leaderX = cx + jitter(0.6);
        double leaderZ = cz + jitter(0.6);
        t.thickBranch(cx, trunkTop, cz, leaderX, y + height, leaderZ, 1.6, 0.9);
        // The top of the canopy is rounded, narrowing gradually rather than ending in a flat layer
        t.blob(leaderX, y + height + 1, leaderZ, 4.5, 3.2, 4.5, 0.35);
        t.blob(leaderX + jitter(1.0), y + height + 3.5, leaderZ + jitter(1.0), 2.8, 1.8, 2.8, 0.35);

        // Draped in vines, both hanging from the canopy and climbing the wood
        hangingVines(0.2, 8, y + 5);
        woodVines(y + 1, 0.12);
        mossOnBranches(y + 6, 0.14);
    }

    /**
     * Places vines clinging to the sides of the tree's wood.
     * @param minY The lowest height that vines grow at.
     * @param chance The chance of a vine on each block of wood.
     */
    private void woodVines(int minY, double chance) {
        for (Pos wood : t.getPositions(Layer.LOG)) {
            if (wood.y() < minY || !chance(chance)) {
                continue;
            }
            BlockFace face = HORIZONTAL[r.nextInt(4)];
            int vx = wood.x() + face.getModX();
            int vz = wood.z() + face.getModZ();
            if (t.has(vx, wood.y(), vz)) {
                continue;
            }
            BlockFace towardsWood = face.getOppositeFace();
            int length = between(1, 4);
            for (int i = 0; i < length; i++) {
                MultipleFacing vine = (MultipleFacing) Material.VINE.createBlockData();
                vine.setFace(towardsWood, true);
                boolean placed = i == 0
                        ? t.decor(vx, wood.y(), vz, vine, towardsWood.getModX(), 0, towardsWood.getModZ())
                        : t.decor(vx, wood.y() - i, vz, vine, 0, 1, 0);
                if (!placed) {
                    break;
                }
            }
        }
    }

    /**
     * #21 Slender Jungle - grown from a single sapling. A tall, thin trunk with short rising branches all the way up,
     * each holding a small pad of leaves, topped by a small crown and draped in vines and cocoa.
     */
    private void slenderJungle(int x, int y, int z) {
        t = new TreeBuilder(world, r, Material.JUNGLE_WOOD, Material.JUNGLE_LEAVES);
        t.setLeafMix(Material.JUNGLE_LEAVES, Material.OAK_LEAVES, 0.12);
        t.setPorosity(0.2);
        double cx = x + 0.5;
        double cz = z + 0.5;
        int height = between(14, 24);
        double topX = cx + jitter(1.0);
        double topZ = cz + jitter(1.0);
        int bendY = y + (int) (height * between(0.4, 0.6));
        t.branch(cx, y, cz, cx, bendY, cz);
        t.branch(cx, bendY, cz, topX, y + height, topZ);
        surfaceRoots(cx, y, cz, between(3, 4), 1.5, 2.5, 0.6, 3, Material.JUNGLE_WOOD);
        t.info("height", height);

        LimbStyle pad = new LimbStyle(0.2, 0.01, 0.6, 0.5, 1, 2, 0.6, 0.75, 1.5, 2.1, 1.1, 0.4, 0.35, 0.2);
        double yaw = randomAngle();
        for (int h = y + (int) (height * between(0.3, 0.4)); h < y + height - 2; h += between(2, 3)) {
            double progress = (double) (h - bendY) / Math.max(1, y + height - bendY);
            double sx = h <= bendY ? cx : cx + (topX - cx) * progress;
            double sz = h <= bendY ? cz : cz + (topZ - cz) * progress;
            yaw += GOLDEN_ANGLE + jitter(0.3);
            double length = between(2.0, 3.5) * (1.1 - 0.4 * (h - y) / height);
            limb(sx, h, sz, yaw, between(0.4, 0.7), length, 1, pad);
        }
        LimbStyle crown = new LimbStyle(0.25, 0.02, 0.45, 0.6, 2, 2, 0.6, 0.7, 1.4, 2.4, 1.4, 0.4, 0.35, 0.3);
        for (int i = 0; i < between(3, 4); i++) {
            limb(topX, y + height, topZ, randomAngle(), between(0.4, 0.8), between(2.5, 3.5), 1, crown);
        }
        t.blob(topX, y + height + 1, topZ, 2.2, 1.3, 2.2, 0.3);

        trunkVines(x, z, 1, y + 1, y + height - 3, 0.25);
        cocoaPods(between(5, 9), y + 2);
        hangingVines(0.12, 5, y + (int) (height * 0.4));
        mossOnBranches(y + 3, 0.14);
    }

    // ------------------------------------------------------------
    // Acacia
    // ------------------------------------------------------------

    /**
     * #12 Savanna Umbrella - a short trunk splitting into angled limbs, each holding a wide, flat canopy.
     */
    private void acacia(int x, int y, int z) {
        t = new TreeBuilder(world, r, Material.ACACIA_WOOD, Material.ACACIA_LEAVES);
        t.setPorosity(0.08);
        double cx = x + 0.5;
        double cz = z + 0.5;
        int trunkHeight = between(3, 6);
        t.branch(cx, y, cz, cx, y + trunkHeight, cz);

        int limbs = between(2, 3);
        double start = randomAngle();
        for (int i = 0; i < limbs; i++) {
            double angle = start + i * Math.PI * 2 / limbs + jitter(0.4);
            double length = between(3.5, 7.0);
            double ex = cx + Math.cos(angle) * length;
            double ey = y + trunkHeight + between(3.0, 5.5);
            double ez = cz + Math.sin(angle) * length;
            t.branch(cx, y + trunkHeight, cz, ex, ey, ez);
            canopyPad(ex, (int) Math.floor(ey), ez, between(3.2, 5.0));

            // A smaller pad off the side of the limb
            if (chance(0.5)) {
                double sideAngle = angle + (chance(0.5) ? 1.0 : -1.0);
                double mx = cx + Math.cos(angle) * length * 0.5;
                double my = y + trunkHeight + 2;
                double mz = cz + Math.sin(angle) * length * 0.5;
                double sx = mx + Math.cos(sideAngle) * 2.5;
                double sy = my + 1.5;
                double sz = mz + Math.sin(sideAngle) * 2.5;
                t.branch(mx, my, mz, sx, sy, sz);
                canopyPad(sx, (int) Math.floor(sy), sz, between(2.4, 3.2));
            }
        }
    }

    private void canopyPad(double x, int y, double z, double radius) {
        int bx = (int) Math.floor(x);
        int bz = (int) Math.floor(z);
        // Supporting cross beneath the pad
        t.log(bx + 1, y, bz, Axis.X);
        t.log(bx - 1, y, bz, Axis.X);
        t.log(bx, y, bz + 1, Axis.Z);
        t.log(bx, y, bz - 1, Axis.Z);
        t.disc(x, y + 1, z, radius, 0.3);
        t.disc(x, y + 2, z, radius * 0.6, 0.25);
    }

    /**
     * Starts a baobab with acacia wood, whose grey bark suits it, and sparse acacia leaves.
     */
    private void startBaobab() {
        t = new TreeBuilder(world, r, Material.ACACIA_WOOD, Material.ACACIA_LEAVES);
        t.setPorosity(0.18);
    }

    /**
     * Grows a round, thick trunk whose radius changes along its height.
     * @param radius Provides the radius of the trunk at a height, from 0 at the base to 1 at the top.
     */
    private void baobabTrunk(double cx, double cz, int y, int height, DoubleUnaryOperator radius) {
        for (int h = 0; h <= height; h++) {
            t.trunk(cx, cz, y + h, y + h, radius.applyAsDouble((double) h / height));
        }
    }

    /**
     * Grows the short, stubby branches of a baobab crown, each forking once and ending in a small tuft of leaves.
     */
    private void baobabCrown(double cx, int topY, double cz, double crownRadius, int branches, double minPitch, double maxPitch,
                             double minLength, double maxLength) {
        LimbStyle stub = new LimbStyle(0.25, 0.02, 0.5, 0.7, 2, 3, 0.45, 0.6, 1.2, 1.8, 1.0, 0.5, 0.35, 0.25);
        double start = randomAngle();
        for (int i = 0; i < branches; i++) {
            double angle = start + i * Math.PI * 2 / branches + jitter(0.3);
            double sx = cx + Math.cos(angle) * crownRadius * between(0.3, 0.8);
            double sz = cz + Math.sin(angle) * crownRadius * between(0.3, 0.8);
            limb(sx, topY - between(0, 1), sz, angle, between(minPitch, maxPitch), between(minLength, maxLength), 1, stub);
        }
    }

    /**
     * Grows a short, thick stem rising from the top of a baobab trunk, leaning outwards, with its own crown.
     */
    private void baobabFork(double cx, int topY, double cz, double angle, double radius, int rise, double lean, int branches,
                            double minLength, double maxLength) {
        double ex = cx + Math.cos(angle) * lean;
        double ez = cz + Math.sin(angle) * lean;
        for (int h = 0; h <= rise; h++) {
            double p = Math.sin((double) h / rise * Math.PI / 2);
            t.trunk(cx + (ex - cx) * p, cz + (ez - cz) * p, topY + h, topY + h, radius);
        }
        baobabCrown(ex, topY + rise, ez, radius, branches, 0.35, 0.8, minLength, maxLength);
    }

    /**
     * #22 Bottle Baobab - grown from a 3x3 of saplings. A fat trunk that bulges in the middle like a bottle,
     * with a small crown of short stubby branches holding tufts of leaves, sometimes forking into two at the top.
     */
    private void bottleBaobab(int x, int y, int z) {
        startBaobab();
        double cx = x + 1.5;
        double cz = z + 1.5;
        int height = between(7, 11);
        double bulge = between(2.0, 2.4);
        double bulgeAt = between(0.3, 0.5);
        double top = between(1.0, 1.3);
        baobabTrunk(cx, cz, y, height, p -> p < bulgeAt
                ? 1.6 + (bulge - 1.6) * Math.sin(p / bulgeAt * Math.PI / 2)
                : top + (bulge - top) * Math.cos((p - bulgeAt) / (1 - bulgeAt) * Math.PI / 2));
        surfaceRoots(cx, y, cz, between(5, 6), 1.2, 2.5, 1.8, 2, Material.ACACIA_WOOD);
        t.info("height", height);
        t.info("bulge", bulge);
        if (chance(0.3)) {
            t.info("fork", true);
            double angle = randomAngle();
            baobabFork(cx, y + height, cz, angle, 0.8, between(2, 3), 1.6, 3, 2.0, 3.0);
            baobabFork(cx, y + height, cz, angle + Math.PI + jitter(0.4), 0.8, between(2, 3), 1.6, 3, 2.0, 3.0);
        } else {
            baobabCrown(cx, y + height, cz, top, between(4, 6), 0.45, 0.9, 2.5, 3.8);
        }
    }

    /**
     * #23 Tall Baobab - grown from a 5x5 of saplings. A tall and grand trunk, 7 blocks across, that flares out at its base
     * and narrows gently upwards, carrying a wide crown of thick, rising branches. Now and then the crown splits into two
     * leaders.
     */
    private void tallBaobab(int x, int y, int z) {
        startBaobab();
        double cx = x + 2.5;
        double cz = z + 2.5;
        int height = between(26, 34);
        double flare = between(4.2, 4.7);
        double base = between(3.4, 3.6);
        double top = between(2.4, 2.8);
        baobabTrunk(cx, cz, y, height, p -> p < 0.12
                ? flare + (base - flare) * Math.sin(p / 0.12 * Math.PI / 2)
                : base + (top - base) * Math.pow((p - 0.12) / 0.88, 1.4));
        surfaceRoots(cx, y, cz, between(8, 10), 2.0, 4.5, 4.0, 3, Material.ACACIA_WOOD);
        t.info("height", height);
        t.info("flare", flare);

        if (chance(0.25)) {
            t.info("fork", true);
            double angle = randomAngle();
            baobabFork(cx, y + height, cz, angle, 1.8, between(4, 6), 3.0, between(5, 6), 4.0, 6.5);
            baobabFork(cx, y + height, cz, angle + Math.PI + jitter(0.4), 1.8, between(4, 6), 3.0, between(5, 6), 4.0, 6.5);
            return;
        }
        // Thick main branches that rise and fork into stubby twigs
        LimbStyle grand = new LimbStyle(0.2, 0.02, 0.45, 0.7, 2, 3, 0.5, 0.65, 1.4, 2.0, 1.1, 0.5, 0.35, 0.3);
        int branches = between(7, 9);
        double start = randomAngle();
        for (int i = 0; i < branches; i++) {
            double angle = start + i * Math.PI * 2 / branches + jitter(0.3);
            double sx = cx + Math.cos(angle) * top * 0.6;
            double sz = cz + Math.sin(angle) * top * 0.6;
            int sy = y + height - between(0, 2);
            double pitch = between(0.3, 0.6);
            double length = between(6.0, 9.5);
            double thick = length * 0.45;
            double ex = sx + Math.cos(angle) * Math.cos(pitch) * thick;
            double ey = sy + Math.sin(pitch) * thick;
            double ez = sz + Math.sin(angle) * Math.cos(pitch) * thick;
            t.thickBranch(sx, sy, sz, ex, ey, ez, 1.1, 0.6);
            limb(ex, ey, ez, angle, pitch, length * 0.6, 2, grand);
        }
        t.trunk(cx, cz, y + height, y + height + 1, top * 0.7);
    }

    /**
     * #24 Split Baobab - grown from a 3x3 of saplings, as an alternative to the Bottle Baobab. A thick trunk that splits
     * into two or three stems partway up, each ending in its own stubby crown.
     */
    private void splitBaobab(int x, int y, int z) {
        startBaobab();
        double cx = x + 1.5;
        double cz = z + 1.5;
        int height = between(9, 13);
        int splitHeight = (int) (height * between(0.4, 0.55));
        baobabTrunk(cx, cz, y, splitHeight, p -> 1.8 - 0.3 * p);
        surfaceRoots(cx, y, cz, between(5, 6), 1.2, 2.5, 1.8, 2, Material.ACACIA_WOOD);
        t.info("height", height);
        t.info("split", splitHeight);

        int stems = between(2, 3);
        double start = randomAngle();
        for (int i = 0; i < stems; i++) {
            double angle = start + i * Math.PI * 2 / stems + jitter(0.3);
            double spread = between(1.5, 2.3);
            double stemRadius = between(0.8, 1.0);
            double sx = cx + Math.cos(angle) * 0.8;
            double sz = cz + Math.sin(angle) * 0.8;
            double ex = cx + Math.cos(angle) * (0.8 + spread);
            double ez = cz + Math.sin(angle) * (0.8 + spread);
            int stemTop = y + height - between(0, 2);
            int stemHeight = stemTop - (y + splitHeight);
            for (int h = 0; h <= stemHeight; h++) {
                double p = (double) h / stemHeight;
                // Leans out quickly, then rises straight
                double lean = Math.sin(Math.min(1, p * 1.6) * Math.PI / 2);
                t.trunk(sx + (ex - sx) * lean, sz + (ez - sz) * lean, y + splitHeight + h, y + splitHeight + h, stemRadius);
            }
            baobabCrown(ex, stemTop, ez, stemRadius, 3, 0.35, 0.8, 2.0, 3.2);
        }
    }

    // ------------------------------------------------------------
    // Dark Oak and Pale Oak
    // ------------------------------------------------------------

    /**
     * Starts a dark oak or a pale oak.
     */
    private void startOak(boolean pale) {
        if (pale) {
            t = new TreeBuilder(world, r, Material.PALE_OAK_WOOD, Material.PALE_OAK_LEAVES);
        } else {
            t = new TreeBuilder(world, r, Material.DARK_OAK_WOOD, Material.DARK_OAK_LEAVES);
            t.setLeafMix(Material.DARK_OAK_LEAVES, Material.OAK_LEAVES, 0.15);
        }
    }

    /**
     * Decorates a dark oak with vines and moss, or a pale oak with pale moss hanging from its leaves, lying on its
     * branches and covering the ground beneath it.
     * @param radius How far the moss on the ground spreads from the trunk.
     */
    private void decorateOak(boolean pale, double cx, int y, double cz, double radius) {
        decorateOak(pale, cx, y, cz, radius, 0.45);
    }

    /**
     * Decorates a dark oak or a pale oak.
     * @param dripChance The chance of pale moss hanging from beneath each leaf of a pale oak.
     */
    private void decorateOak(boolean pale, double cx, int y, double cz, double radius, double dripChance) {
        if (pale) {
            paleMossDrip(dripChance);
            mossOnBranches(y + 3, 0.06, Material.PALE_MOSS_CARPET);
            paleMossFloor(cx, y, cz, radius);
        } else {
            hangingVines(0.05, 4, y + 3);
            mossOnBranches(y + 3, 0.04, Material.MOSS_CARPET);
            groundCover((int) Math.floor(cx), y, (int) Math.floor(cz), radius * 0.85, 0.3, Material.MOSS_CARPET, false);
        }
    }

    /**
     * Hangs pale moss from the undersides of the leaves.
     */
    private void paleMossDrip(double chance) {
        for (Pos leaf : t.getPositions(Layer.LEAF)) {
            if (t.has(leaf.x(), leaf.y() - 1, leaf.z()) || !chance(chance)) {
                continue;
            }
            // Long curtains of moss, like a weeping willow
            int length = between(2, 6);
            for (int i = 1; i <= length; i++) {
                HangingMoss moss = (HangingMoss) Material.PALE_HANGING_MOSS.createBlockData();
                moss.setTip(i == length);
                if (!t.decor(leaf.x(), leaf.y() - i, leaf.z(), moss, 0, 1, 0)) {
                    break;
                }
            }
        }
    }

    /**
     * Turns the ground beneath a pale oak into pale moss, with pale moss carpets on top that climb up any block
     * beside them, like in a pale garden.
     */
    private void paleMossFloor(double cx, int y, double cz, double radius) {
        List<Block> carpets = new ArrayList<>();
        for (int dx = (int) -radius - 1; dx <= radius + 1; dx++) {
            for (int dz = (int) -radius - 1; dz <= radius + 1; dz++) {
                int x = (int) Math.floor(cx) + dx;
                int z = (int) Math.floor(cz) + dz;
                double distance = Math.hypot(x + 0.5 - cx, z + 0.5 - cz);
                // Dense near the trunk, fading out gradually towards the edge
                if (distance > radius || !chance(0.97 * fade(distance, radius))) {
                    continue;
                }
                Block ground = findGround(x, y, z, 3);
                if (ground == null) {
                    continue;
                }
                Block soil = ground.getRelative(BlockFace.DOWN);
                if (!isSoil(soil.getType()) || t.has(soil.getX(), soil.getY(), soil.getZ())) {
                    continue;
                }
                t.soil(soil.getX(), soil.getY(), soil.getZ(), Material.PALE_MOSS_BLOCK);
                if (chance(0.88) && !t.has(ground.getX(), ground.getY(), ground.getZ())) {
                    carpets.add(ground);
                }
            }
        }
        for (Block ground : carpets) {
            MossyCarpet carpet = (MossyCarpet) Material.PALE_MOSS_CARPET.createBlockData();
            carpet.setBottom(true);
            MossyCarpet upper = (MossyCarpet) Material.PALE_MOSS_CARPET.createBlockData();
            upper.setBottom(false);
            boolean climbs = false;
            for (BlockFace face : HORIZONTAL) {
                if (isWall(ground.getRelative(face))) {
                    // Creeps up the side, filling both walls of a corner, and continues into the block above
                    carpet.setHeight(face, Wall.Height.TALL);
                    climbs = true;
                    if (isWall(ground.getRelative(face).getRelative(BlockFace.UP))) {
                        upper.setHeight(face, chance(0.5) ? Wall.Height.TALL : Wall.Height.LOW);
                    }
                }
            }
            if (!t.decor(ground.getX(), ground.getY(), ground.getZ(), carpet, 0, -1, 0) || !climbs) {
                continue;
            }
            boolean hasUpperSide = false;
            for (BlockFace face : HORIZONTAL) {
                hasUpperSide |= upper.getHeight(face) != Wall.Height.NONE;
            }
            if (hasUpperSide) {
                t.decor(ground.getX(), ground.getY() + 1, ground.getZ(), upper, 0, -1, 0);
            }
        }
    }

    /**
     * Determines if pale moss can creep up the side of the block, which it only does against pale oak wood.
     */
    private boolean isWall(Block block) {
        Material type = t.typeAt(block.getX(), block.getY(), block.getZ());
        if (type == null) {
            type = block.getType();
        }
        return type == Material.PALE_OAK_WOOD || type == Material.PALE_OAK_LOG;
    }

    /**
     * #25 Young Dark Oak and #27 Young Pale Oak - grown from a single sapling. A single, crooked trunk with a low,
     * wide and dense crown.
     */
    private void youngOak(int x, int y, int z, boolean pale) {
        startOak(pale);
        t.setPorosity(0.15);
        double scale = between(0.65, 0.95);
        double cx = x + 0.5;
        double cz = z + 0.5;
        int height = (int) Math.round(between(4.5, 6.5) * scale);
        t.info("scale", scale);
        t.info("height", height);
        // The trunk kinks once or twice on its way up
        double px = cx;
        double pz = cz;
        int py = y;
        int kinks = between(1, 2);
        for (int i = 1; i <= kinks + 1; i++) {
            int ny = y + height * i / (kinks + 1);
            double nx = i == kinks + 1 ? px : px + jitter(1.2);
            double nz = i == kinks + 1 ? pz : pz + jitter(1.2);
            t.branch(px, py, pz, nx, ny, nz);
            px = nx;
            pz = nz;
            py = ny;
        }
        surfaceRoots(cx, y, cz, between(3, 4), 1.2, 2.2, 0.6, 1, pale ? Material.PALE_OAK_WOOD : Material.DARK_OAK_WOOD);

        LimbStyle style = new LimbStyle(0.25, 0.04, 0.25, 0.7, 2, 2, 0.6, 0.72, 1.4, 2.5, 1.7, 0.5, 0.35, 0.5);
        int limbs = between(4, 5);
        double start = randomAngle();
        for (int i = 0; i < limbs; i++) {
            double angle = start + i * Math.PI * 2 / limbs + jitter(0.35);
            limb(px, py - between(0, 1), pz, angle, between(0.2, 0.5), between(2.8, 4.0) * scale, 1, style);
        }
        t.blob(px, py + 1, pz, 2.6 * scale, 1.6, 2.6 * scale, 0.35);
        // Fewer curtains of moss than the larger pale oaks, but a floor of moss that reaches well past the trunk
        decorateOak(pale, cx, y, cz, Math.max(5.5, 6.0 * scale), 0.15);
    }

    /**
     * #26 Gnarled Dark Oak and #14 Hollow Pale Oak - grown from a 2x2 of saplings. A gnarled, twisting 2x2 trunk with
     * crooked limbs and a patchy canopy.
     */
    private void gnarledOak(int x, int y, int z, boolean pale) {
        startOak(pale);
        t.setPorosity(0.3);
        double scale = between(0.8, 1.3);
        // Pale oaks are low, wide and droopy rather than tall
        int height = pale ? (int) Math.round(between(4.5, 6.5) * scale) + between(3, 5)
                : (int) Math.round(between(7.0, 11.0) * scale);
        t.info("scale", scale);
        t.info("height", height);
        int ox = 0;
        int oz = 0;
        for (int h = 0; h <= height; h++) {
            // The trunk twists as it rises
            if (h >= 4 && h % 3 == 1 && chance(0.65)) {
                placeSquare(x + ox, y + h, z + oz);
                if (chance(0.5)) {
                    ox = Math.max(-1, Math.min(1, ox + (chance(0.5) ? 1 : -1)));
                } else {
                    oz = Math.max(-1, Math.min(1, oz + (chance(0.5) ? 1 : -1)));
                }
            }
            placeSquare(x + ox, y + h, z + oz);
        }
        int[][] flare = {{-1, 0}, {-1, 1}, {2, 0}, {2, 1}, {0, -1}, {1, -1}, {0, 2}, {1, 2}};
        if (pale) {
            // A sturdy base that rises unevenly up the sides of the trunk before narrowing into it: a couple of sides are
            // left bare, one stays low, most rise 2, and a few climb 3 or 4
            List<Integer> rises = new ArrayList<>(List.of(0, 0, 1, 2, 2, 2, 3, between(3, 4)));
            Collections.shuffle(rises, r);
            for (int i = 0; i < flare.length; i++) {
                int[] offset = flare[i];
                int rise = rises.get(i);
                for (int h = 0; h < rise; h++) {
                    t.wood(x + offset[0], y + h, z + offset[1]);
                }
            }
            int[][] corners = {{-1, -1}, {2, -1}, {-1, 2}, {2, 2}};
            for (int[] corner : corners) {
                if (chance(0.4)) {
                    t.wood(x + corner[0], y, z + corner[1]);
                }
            }
            // A flat spread of roots, a single block tall, like the young pale oak's but reaching a little further
            surfaceRoots(x + 1, y, z + 1, between(4, 6), 1.8, 3.2, 1.2, 1, Material.PALE_OAK_WOOD);
        } else {
            for (int[] offset : flare) {
                if (chance(0.45)) {
                    t.wood(x + offset[0], y, z + offset[1]);
                }
            }
        }

        double topX = x + ox + 1;
        double topZ = z + oz + 1;
        int topY = y + height;

        // Crooked limbs that change direction along the way
        int limbs = pale ? between(6, 7) : 5;
        double start = randomAngle();
        for (int i = 0; i < limbs; i++) {
            double angle = start + i * Math.PI * 2 / limbs + jitter(0.3);
            double px = topX;
            double py = topY - between(0, 3);
            double pz = topZ;
            for (int segment = 0; segment < 3; segment++) {
                double segmentAngle = angle + jitter(0.7);
                double length = (pale ? between(1.8, 2.7) : between(1.8, 3.2)) * scale;
                double nx = px + Math.cos(segmentAngle) * length;
                // Pale oak limbs curve upwards as they leave the trunk, then reach outwards and sag at their ends
                double ny = py + (pale ? switch (segment) {
                    case 0 -> between(1.5, 2.5);
                    case 1 -> between(0.5, 1.5);
                    default -> between(-0.6, 0.4);
                } : between(0.0, 2.0));
                double nz = pz + Math.sin(segmentAngle) * length;
                t.branch(px, py, pz, nx, ny, nz);
                // Pale oak limbs are leafy all along their length, not just at their ends
                if (pale && segment < 2) {
                    t.blob(nx, ny + 0.5, nz, 2.3, 1.3, 2.3, 0.5);
                }
                px = nx;
                py = ny;
                pz = nz;
            }
            if (pale) {
                t.blob(px, py + 0.5, pz, 3.4, 1.5, 3.4, 0.5);
            } else {
                t.blob(px, py + 0.5, pz, 3.0, 2.0, 3.0, 0.5);
            }
        }
        if (pale) {
            t.blob(topX, topY + 1.0, topZ, 4.0, 1.6, 4.0, 0.45);
            // One or two small side branches lower down the trunk
            LimbStyle side = new LimbStyle(0.25, 0.04, 0.3, 0.6, 1, 2, 0.6, 0.7, 1.4, 2.0, 1.2, 0.4, 0.45, 0.5);
            int sideBranches = between(1, 2);
            double sideAngle = randomAngle();
            for (int i = 0; i < sideBranches; i++) {
                sideAngle += Math.PI + jitter(0.8);
                double h = y + height * between(0.4, 0.65);
                limb(x + 1 + Math.cos(sideAngle), h, z + 1 + Math.sin(sideAngle), sideAngle, between(0.35, 0.6),
                        between(2.5, 3.5) * scale, 1, side);
            }
        } else {
            t.blob(topX, topY + 1.5, topZ, 3.5, 2.0, 3.5, 0.45);
        }
        decorateOak(pale, x + 1, y, z + 1, 7.0 * scale);
    }

    /**
     * #13 Ancient Dark Oak and #28 Ancient Pale Oak - grown from a 3x3 of saplings. A flared 3x3 trunk with deep roots
     * and sprawling single limbs, starting no lower than the 4th block, that fork into a broad, full canopy.
     */
    private void ancientOak(int x, int y, int z, boolean pale) {
        startOak(pale);
        t.setPorosity(0.16);
        double scale = between(1.2, 1.6);
        double cx = x + 1.5;
        double cz = z + 1.5;
        // Pale oaks stand taller, to look more grand
        int trunkHeight = (int) Math.round(between(5.0, 7.5) * scale) + (pale ? between(3, 5) : 0);
        t.info("scale", scale);
        t.info("trunk", trunkHeight);
        if (pale) {
            ancientPaleOak(x, y, z, scale, trunkHeight);
            return;
        }
        t.trunk(cx, cz, y, y + trunkHeight, 1.5);
        Material wood = Material.DARK_OAK_WOOD;

        // Flared base around the 3x3
        for (int i = 0; i < 3; i++) {
            int[][] flare = {{-1, i}, {3, i}, {i, -1}, {i, 3}};
            for (int[] offset : flare) {
                if (chance(0.6)) {
                    t.wood(x + offset[0], y, z + offset[1]);
                    if (chance(0.35)) {
                        t.wood(x + offset[0], y + 1, z + offset[1]);
                    }
                }
            }
        }
        surfaceRoots(cx, y, cz, between(7, 9), 2.0, 3.5 * scale, 2.2, 3, wood);

        LimbStyle style = new LimbStyle(0.25, 0.04, 0.3, 0.7, 2, 3, 0.58, 0.72, 1.6, 3.0, 2.1, 0.6, 0.35, 0.7);
        int[][] edge = {{0, 0}, {1, 0}, {2, 0}, {2, 1}, {2, 2}, {1, 2}, {0, 2}, {0, 1}};
        int limbs = between(6, 7);
        int first = r.nextInt(edge.length);
        for (int i = 0; i < limbs; i++) {
            int[] cell = edge[(first + i * edge.length / limbs) % edge.length];
            double sx = x + cell[0] + 0.5;
            double sz = z + cell[1] + 0.5;
            double angle = Math.atan2(sz - cz, sx - cx) + jitter(0.35);
            // Never lower than the 4th block of the trunk
            int sy = Math.max(y + 3, y + trunkHeight - between(0, 2));
            limb(sx, sy, sz, angle, between(0.3, 0.55), between(5.5, 7.5) * scale, 2, style);
        }
        limb(cx, y + trunkHeight, cz, randomAngle(), 1.3, 3.5 * scale, 1, style);

        // Fills the open gaps in the top of the canopy
        int canopyY = y + trunkHeight + (int) Math.round(3 * scale);
        t.blob(cx, canopyY + 1, cz, 4.5 * scale, 2.2, 4.5 * scale, 0.4);
        int fills = between(5, 7);
        double fillStart = randomAngle();
        for (int i = 0; i < fills; i++) {
            double angle = fillStart + i * Math.PI * 2 / fills + jitter(0.4);
            double distance = between(3.5, 6.5) * scale;
            t.blob(cx + Math.cos(angle) * distance, canopyY + between(0.0, 2.0), cz + Math.sin(angle) * distance,
                    2.6, 1.6, 2.6, 0.4);
        }
        decorateOak(false, cx, y, cz, 8.5 * scale);
    }

    /**
     * #28 Ancient Pale Oak - a 3x3 trunk that flares smoothly out at its base and narrows at its top, with thick limbs
     * leaving it at different heights and in uneven directions, each leafy along its whole length.
     */
    private void ancientPaleOak(int x, int y, int z, double scale, int trunkHeight) {
        double cx = x + 1.5;
        double cz = z + 1.5;
        // Flares out smoothly at the base, rather than ending in a ring of single blocks
        for (int h = 0; h <= trunkHeight; h++) {
            double radius = 1.5 + Math.exp(-h / 1.8);
            t.trunk(cx + jitter(0.15), cz + jitter(0.15), y + h, y + h, radius);
        }
        // Narrows at the top instead of stopping flat
        double topX = cx + jitter(0.5);
        double topZ = cz + jitter(0.5);
        int topY = y + trunkHeight + between(2, 3);
        t.thickBranch(cx, y + trunkHeight, cz, topX, topY, topZ, 1.4, 0.8);
        surfaceRoots(cx, y, cz, between(7, 9), 2.0, 3.5 * scale, 2.6, 3, Material.PALE_OAK_WOOD);

        LimbStyle style = new LimbStyle(0.22, 0.04, 0.3, 0.7, 2, 3, 0.6, 0.74, 1.6, 3.2, 1.9, 0.5, 0.4, 0.95);
        int limbs = between(8, 9);
        double angle = randomAngle();
        for (int i = 0; i < limbs; i++) {
            // Uneven directions and heights, so the limbs never look like the trunk splitting evenly apart
            angle += GOLDEN_ANGLE + jitter(0.35);
            double sy = Math.max(y + 3, y + trunkHeight * between(0.6, 1.0) + between(0, 2));
            double sx = cx + Math.cos(angle) * 0.6;
            double sz = cz + Math.sin(angle) * 0.6;
            // A thick first part grows out of the side of the trunk before the limb tapers and splits
            double length = between(3.2, 4.2) * scale;
            double pitch = between(0.3, 0.6);
            double ex = sx + Math.cos(angle) * Math.cos(pitch) * length;
            double ey = sy + Math.sin(pitch) * length;
            double ez = sz + Math.sin(angle) * Math.cos(pitch) * length;
            t.thickBranch(sx, sy, sz, ex, ey, ez, 1.4, 0.9);
            t.blob(ex, ey + 0.8, ez, 2.6, 1.5, 2.6, 0.5);
            thickLimb(ex, ey, ez, angle + jitter(0.25), pitch * 0.6, between(4.0, 5.5) * scale, 2, style, 0.8);
        }
        // An upper ring of limbs fills the top of the canopy, so it has no large open areas
        int upperLimbs = between(4, 5);
        double upperAngle = randomAngle();
        for (int i = 0; i < upperLimbs; i++) {
            upperAngle += GOLDEN_ANGLE + jitter(0.3);
            double p = between(0.3, 1.0);
            double sy = y + trunkHeight + (topY - y - trunkHeight) * p;
            thickLimb(cx + (topX - cx) * p, sy, cz + (topZ - cz) * p, upperAngle, between(0.5, 0.8),
                    between(4.0, 5.5) * scale, 2, style, 0.8);
        }
        limb(topX, topY, topZ, randomAngle(), 1.3, 3.0 * scale, 1, style);

        // A few small branches lower down the trunk, kept short so the tree still looks grand
        LimbStyle side = new LimbStyle(0.25, 0.04, 0.3, 0.6, 1, 2, 0.6, 0.7, 1.4, 2.0, 1.2, 0.4, 0.45, 0.5);
        int sideBranches = between(3, 4);
        double sideAngle = randomAngle();
        for (int i = 0; i < sideBranches; i++) {
            sideAngle += GOLDEN_ANGLE + jitter(0.3);
            double h = y + Math.max(3, trunkHeight * between(0.35, 0.6));
            limb(cx + Math.cos(sideAngle) * 1.6, h, cz + Math.sin(sideAngle) * 1.6, sideAngle, between(0.35, 0.6),
                    between(2.5, 3.5), 1, side);
        }

        // Fills the open gaps in the top of the canopy
        int canopyY = y + trunkHeight + (int) Math.round(3 * scale);
        t.blob(cx, canopyY + 1, cz, 4.5 * scale, 2.2, 4.5 * scale, 0.4);
        int fills = between(5, 7);
        double fillStart = randomAngle();
        for (int i = 0; i < fills; i++) {
            double fillAngle = fillStart + i * Math.PI * 2 / fills + jitter(0.4);
            double distance = between(3.5, 6.5) * scale;
            t.blob(cx + Math.cos(fillAngle) * distance, canopyY + between(0.0, 2.0), cz + Math.sin(fillAngle) * distance,
                    2.6, 1.6, 2.6, 0.4);
        }
        decorateOak(true, cx, y, cz, 8.5 * scale);
    }

    private void placeSquare(int x, int y, int z) {
        t.log(x, y, z, Axis.Y);
        t.log(x + 1, y, z, Axis.Y);
        t.log(x, y, z + 1, Axis.Y);
        t.log(x + 1, y, z + 1, Axis.Y);
    }

    // ------------------------------------------------------------
    // Cherry, Mangrove and Azalea
    // ------------------------------------------------------------

    /**
     * Grows a cherry limb that rises steeply before arching outwards, ending in a fluffy cluster of blossoms.
     * @param thickness The radius of the lower part of the limb, or 0 for a thin limb.
     */
    private void cherryLimb(double tx, double ty, double tz, double angle, double scale, double thickness) {
        double mx = tx + Math.cos(angle) * 2.5 * scale;
        double my = ty + between(2.5, 3.5) * scale;
        double mz = tz + Math.sin(angle) * 2.5 * scale;
        double length = between(6.0, 7.5) * scale;
        double ex = tx + Math.cos(angle) * length;
        double ey = ty + between(4.0, 5.5) * scale;
        double ez = tz + Math.sin(angle) * length;
        if (thickness > 0) {
            t.thickBranch(tx, ty, tz, mx, my, mz, thickness, thickness * 0.6);
        } else {
            t.branch(tx, ty, tz, mx, my, mz);
        }
        t.branch(mx, my, mz, ex, ey, ez);
        t.blob(ex, ey + 0.8, ez, 4.4 * scale, 2.4, 4.4 * scale, 0.3);
        t.blob(mx, my + 1, mz, 3.2, 2.1, 3.2, 0.35);
    }

    /**
     * Lets blossoms droop over the edges of the canopy, and scatters petals beneath it.
     * @param radius How far the petals spread from the trunk.
     */
    private void cherryFinish(int x, int y, int z, double radius) {
        for (Pos leaf : t.getPositions(Layer.LEAF)) {
            if (!t.has(leaf.x(), leaf.y() - 1, leaf.z()) && chance(0.15)) {
                t.leaf(leaf.x(), leaf.y() - 1, leaf.z());
            }
        }
        groundCover(x, y, z, radius * 1.2, 0.45, Material.PINK_PETALS, true);
    }

    /**
     * #15 Blossom Cherry - grown from a single sapling. A short trunk that splits into wide, arching limbs with a fluffy,
     * airy canopy of drooping blossoms and patches of petals below.
     */
    private void cherry(int x, int y, int z) {
        t = new TreeBuilder(world, r, Material.CHERRY_WOOD, Material.CHERRY_LEAVES);
        // A larger canopy with more air in it, for a fluffier look
        t.setPorosity(0.3);
        double scale = between(0.55, 0.85);
        double cx = x + 0.5;
        double cz = z + 0.5;
        int trunkHeight = (int) Math.round(between(3.0, 4.0) * scale);
        double tx = cx + jitter(0.6);
        double tz = cz + jitter(0.6);
        double ty = y + trunkHeight;
        t.branch(cx, y, cz, tx, ty, tz);
        t.info("scale", scale);

        int limbs = between(3, 4);
        double start = randomAngle();
        for (int i = 0; i < limbs; i++) {
            cherryLimb(tx, ty, tz, start + i * Math.PI * 2 / limbs + jitter(0.35), scale, 0);
        }
        t.blob(tx, ty + 5 * scale, tz, 3.7, 2.5, 3.7, 0.3);
        cherryFinish(x, y, z, 7.5 * scale);
    }

    /**
     * #29 Grand Cherry - grown from a 2x2 of saplings. A grander cherry on a 2x2 trunk whose thick limbs arch out into
     * a wide, fluffy canopy, with a large spread of petals below.
     */
    private void grandCherry(int x, int y, int z) {
        t = new TreeBuilder(world, r, Material.CHERRY_WOOD, Material.CHERRY_LEAVES);
        t.setPorosity(0.3);
        double scale = between(1.25, 1.55);
        double cx = x + 1;
        double cz = z + 1;
        int trunkHeight = (int) Math.round(between(3.0, 4.0) * scale);
        t.trunk(cx, cz, y, y + trunkHeight, 1.0);
        surfaceRoots(cx, y, cz, between(4, 5), 1.5, 3.0, 1.2, 1, Material.CHERRY_WOOD);
        t.info("scale", scale);

        int limbs = between(4, 5);
        double start = randomAngle();
        for (int i = 0; i < limbs; i++) {
            double angle = start + i * Math.PI * 2 / limbs + jitter(0.3);
            cherryLimb(cx + Math.cos(angle) * 0.5, y + trunkHeight - between(0, 1), cz + Math.sin(angle) * 0.5, angle,
                    scale, between(0.7, 1.0));
        }
        t.blob(cx, y + trunkHeight + 5 * scale, cz, 4.5, 2.8, 4.5, 0.3);
        cherryFinish(x, y, z, 8.5 * scale);
    }

    /**
     * #16 Great Mangrove - a trunk raised on arching prop roots with a broad canopy and hanging propagules. The wood of the
     * trunk carries on down into the roots, turning into mangrove roots a few blocks above the water or ground. The trunk
     * ends below the canopy and its branches bend up into it, so tall trees never look like a pillar.
     */
    private void mangrove(int x, int y, int z) {
        t = new TreeBuilder(world, r, Material.MANGROVE_WOOD, Material.MANGROVE_LEAVES);
        t.setPorosity(0.2);
        double scale = between(0.8, 1.3);
        double cx = x + 0.5;
        double cz = z + 0.5;
        // Measured from the surface of the water the propagule grew in, or from the ground
        int surfaceY = y;
        while (surfaceY < y + 8 && world.getBlockAt(x, surfaceY, z).getType() == Material.WATER) {
            surfaceY++;
        }
        int baseY = surfaceY + between(2, 5);
        // Below this height the wood turns into mangrove roots
        int rootsBelow = surfaceY + between(2, 4);
        int height = (int) Math.round(between(7.0, 11.0) * scale);
        int canopyY = baseY + height;
        int trunkTop = canopyY - between(2, 3) - Math.max(0, height - 8) / 2;
        double topX = cx + jitter(0.6);
        double topZ = cz + jitter(0.6);
        t.branch(cx, baseY, cz, topX, trunkTop, topZ);
        for (int h = y; h < baseY; h++) {
            t.root(x, h, z, h >= rootsBelow ? Material.MANGROVE_WOOD : Material.MANGROVE_ROOTS, Axis.Y, true);
        }
        t.info("scale", scale);
        t.info("height", height);

        // Arching prop roots, wood at the top that turns into mangrove roots near the water or ground
        int roots = between(7, 9);
        double start = randomAngle();
        for (int i = 0; i < roots; i++) {
            double angle = start + i * Math.PI * 2 / roots + jitter(0.3);
            double reach = between(3.0, 6.0) * scale;
            double sy = baseY + between(0, 2);
            double controlX = cx + Math.cos(angle) * reach * 0.6;
            double controlY = sy + 1.5;
            double controlZ = cz + Math.sin(angle) * reach * 0.6;
            double endX = cx + Math.cos(angle) * reach;
            double endY = y - 1;
            double endZ = cz + Math.sin(angle) * reach;
            double px = cx;
            double py = sy;
            double pz = cz;
            for (int step = 1; step <= 12; step++) {
                double s = step / 12.0;
                double nx = (1 - s) * (1 - s) * cx + 2 * (1 - s) * s * controlX + s * s * endX;
                double ny = (1 - s) * (1 - s) * sy + 2 * (1 - s) * s * controlY + s * s * endY;
                double nz = (1 - s) * (1 - s) * cz + 2 * (1 - s) * s * controlZ + s * s * endZ;
                Material material = ny >= rootsBelow ? Material.MANGROVE_WOOD : Material.MANGROVE_ROOTS;
                t.rootLine(px, py, pz, nx, ny, nz, material, true);
                px = nx;
                py = ny;
                pz = nz;
            }
        }

        // Branches that bend up from the top of the trunk into the canopy
        int branches = between(4, 5);
        double branchStart = randomAngle();
        for (int i = 0; i < branches; i++) {
            double angle = branchStart + i * Math.PI * 2 / branches + jitter(0.4);
            double branchY = trunkTop - between(0, 2);
            double[] end = curvedBranch(topX, branchY, topZ, angle, between(4.0, 5.5) * scale, canopyY - 1 - branchY);
            t.blob(end[0], end[1] + 0.5, end[2], 3.4, 2.2, 3.4, 0.35);
        }
        t.branch(topX, trunkTop, topZ, topX + jitter(0.5), canopyY, topZ + jitter(0.5));
        t.blob(topX, canopyY + 1.5, topZ, 4.0, 2.4, 4.0, 0.3);

        // Hanging propagules and a few vines
        for (Pos leaf : t.getPositions(Layer.LEAF)) {
            if (!t.has(leaf.x(), leaf.y() - 1, leaf.z()) && chance(0.08)) {
                MangrovePropagule propagule = (MangrovePropagule) Material.MANGROVE_PROPAGULE.createBlockData();
                propagule.setHanging(true);
                propagule.setAge(between(0, propagule.getMaximumAge()));
                t.decor(leaf.x(), leaf.y() - 1, leaf.z(), propagule, 0, 1, 0);
            }
        }
        hangingVines(0.05, 3, baseY);
        // Moss carpets on the roots and wood above the water, like vanilla mangroves
        mossOnBranches(surfaceY, 0.12);
    }

    /**
     * #40 Banyan Grove - grown from an 11x11 of propagules, the largest tree of all, inspired by the great banyan-grove tree
     * of the Foggy Swamp. A colossal trunk at least 11 blocks across, made of thick strands twisting around each other,
     * that swells out at its base and flows smoothly into huge roots snaking out across the ground in arches before diving
     * into it. Its canopy is built in levels: two broad tiers of massive limbs, a smaller tier above them, and a crown.
     */
    private void banyanGrove(int x, int y, int z) {
        t = new TreeBuilder(world, r, Material.MANGROVE_WOOD, Material.MANGROVE_LEAVES);
        t.setLeafMix(Material.MANGROVE_LEAVES, Material.JUNGLE_LEAVES, 0.12);
        t.setPorosity(0.24);
        double cx = x + 5.5;
        double cz = z + 5.5;
        // Its spread is set by a size usually around 50, anywhere from 40 to 75, and it stands about 10 blocks taller than
        // that so it is never too wide for its height
        int size = (int) Math.round(Math.max(40, Math.min(75, 50 + r.nextGaussian() * 8)));
        double s = size / 50.0;
        int height = size + between(8, 12);
        t.info("height", height);
        t.info("scale", s);

        // Measured from the surface of the water the propagules grew in, or from the ground
        int surfaceY = y;
        while (surfaceY < y + 8 && world.getBlockAt((int) cx, surfaceY, (int) cz).getType() == Material.WATER) {
            surfaceY++;
        }
        // Thin roots turn into mangrove roots below this height
        int rootsBelow = surfaceY + between(2, 4);

        // The heights of the tiers of limbs
        int tier1 = y + (int) Math.round(height * between(0.4, 0.46));
        int tier2 = y + (int) Math.round(height * between(0.6, 0.66));
        int tier3 = y + (int) Math.round(height * between(0.78, 0.83));

        // The bulk of the trunk is at least 11 blocks across, swelling out much wider at its base and narrowing above the
        // lowest tier of limbs
        double bulk = Math.max(5.6, 5.2 * Math.sqrt(s));
        DoubleUnaryOperator radius = h -> {
            double base = bulk + 3.5 * Math.exp(-h / (5.0 * s));
            int lower = tier1 - y;
            if (h <= lower) {
                return base;
            }
            double p = Math.min(1, (h - lower) / (double) (tier3 - tier1));
            return base - (base - 2.2) * p;
        };
        int trunkHeight = tier3 - y;
        for (int h = 0; h <= trunkHeight; h++) {
            // The core reaches right out to the strands, so there are no holes between them
            t.trunk(cx, cz, y + h, y + h, radius.applyAsDouble(h) - 0.5);
        }
        // Thick strands twisting around each other in both directions around the core
        int strands = between(10, 13);
        double strandStart = randomAngle();
        for (int i = 0; i < strands; i++) {
            double startAngle = strandStart + i * Math.PI * 2 / strands + jitter(0.2);
            double twist = between(0.5, 1.1) * (i % 2 == 0 ? 1 : -1);
            double thickness = between(1.3, 1.8) * Math.sqrt(s);
            double phase = randomAngle();
            double px = cx + Math.cos(startAngle) * (radius.applyAsDouble(0) - thickness * 0.6);
            double pz = cz + Math.sin(startAngle) * (radius.applyAsDouble(0) - thickness * 0.6);
            double py = y - 1;
            for (int h = 2; h <= trunkHeight + 2; h += 2) {
                double u = (double) h / trunkHeight;
                double angle = startAngle + twist * u * Math.PI;
                // Strands bulge in and out as they climb, so that the trunk looks gnarled
                double distance = radius.applyAsDouble(h) - thickness * 0.6 + 0.5 * Math.sin(phase + u * 6);
                double nx = cx + Math.cos(angle) * distance;
                double nz = cz + Math.sin(angle) * distance;
                t.thickBranch(px, py, pz, nx, y + h, nz, thickness, thickness);
                px = nx;
                py = y + h;
                pz = nz;
            }
        }

        // Huge roots that grow out of the swollen base of the trunk, so that it flows smoothly into them
        int roots = between(10, 14);
        double rootStart = randomAngle();
        for (int i = 0; i < roots; i++) {
            double angle = rootStart + i * Math.PI * 2 / roots + jitter(0.2);
            double startHeight = between(5.0, 10.0) * s;
            double startRadius = radius.applyAsDouble(startHeight) - 2.0;
            banyanRoot(cx + Math.cos(angle) * startRadius, y, cz + Math.sin(angle) * startRadius, angle, startHeight,
                    between(16.0, 26.0) * s, between(2.4, 3.0) * s, rootsBelow, true);
        }

        // The canopy is built in levels that narrow towards the top, so from afar the tree is a huge, wide cone about 2.5
        // times as wide as it is tall: two broad tiers of massive limbs, a smaller tier above them, and a rounded crown
        LimbStyle canopy = new LimbStyle(0.25, 0.05, 0.15, 0.7, 2, 3, 0.6, 0.72, 1.8, 3.6, 2.2, 0.6, 0.4, 0.45);
        LimbStyle small = new LimbStyle(0.25, 0.05, 0.15, 0.7, 2, 2, 0.6, 0.72, 1.6, 3.0, 1.9, 0.5, 0.4, 0.45);
        double angle = randomAngle();
        angle = banyanTier(cx, cz, y, tier1, radius, between(9, 11), 32.0 * s, 40.0 * s, 1.9 * s, 3, canopy, angle, s);
        angle = banyanTier(cx, cz, y, tier2, radius, between(8, 9), 20.0 * s, 26.0 * s, 1.6 * s, 3, canopy, angle, s);
        banyanTier(cx, cz, y, tier3, radius, between(6, 7), 10.0 * s, 14.0 * s, 1.1 * s, 2, small, angle, s);

        // A stout leader climbs on up the middle into a rounded crown, as wide as it is tall
        double leaderX = cx + jitter(1.5);
        double leaderZ = cz + jitter(1.5);
        int leaderTop = y + height - (int) Math.round(5 * s);
        t.thickBranch(cx, tier3, cz, leaderX, leaderTop, leaderZ, 2.2, 1.0);
        int crownLimbs = between(5, 6);
        double crownAngle = randomAngle();
        for (int i = 0; i < crownLimbs; i++) {
            crownAngle += GOLDEN_ANGLE;
            thickLimb(leaderX, leaderTop - between(2, 5), leaderZ, crownAngle, between(0.3, 0.6), between(6.0, 8.0) * s, 2,
                    small, 0.9);
        }
        t.blob(leaderX, leaderTop + 1, leaderZ, 7.0 * s, 4.0 * s, 7.0 * s, 0.4);
        t.blob(leaderX, leaderTop + 4.0 * s, leaderZ, 4.5 * s, 2.5 * s, 4.5 * s, 0.4);

        // Draped in vines and moss, with propagules hanging beneath the leaves
        for (Pos leaf : t.getPositions(Layer.LEAF)) {
            if (!t.has(leaf.x(), leaf.y() - 1, leaf.z()) && chance(0.04)) {
                MangrovePropagule propagule = (MangrovePropagule) Material.MANGROVE_PROPAGULE.createBlockData();
                propagule.setHanging(true);
                propagule.setAge(between(0, propagule.getMaximumAge()));
                t.decor(leaf.x(), leaf.y() - 1, leaf.z(), propagule, 0, 1, 0);
            }
        }
        hangingVines(0.1, 10, y + 8);
        woodVines(y + 2, 0.05);
        mossOnBranches(surfaceY, 0.08);
        groundCover((int) Math.floor(cx), y, (int) Math.floor(cz), 24 * s, 0.3, Material.MOSS_CARPET, false);
    }

    /**
     * Grows one tier of the massive limbs of a banyan grove, reaching out from the trunk and rising gently before
     * splitting into the canopy.
     * @param tierY The height that the limbs of this tier leave the trunk at.
     * @param radius The radius of the trunk at each height above its base.
     * @param minReach The shortest distance that a limb reaches out from the trunk.
     * @param maxReach The longest distance that a limb reaches out from the trunk.
     * @param thickness The radius of each limb where it leaves the trunk.
     * @param depth How many times the end of each limb splits.
     * @param angle The direction of the previous limb, so that the limbs of each tier sit between those of the last.
     * @return The direction of the last limb of this tier.
     */
    private double banyanTier(double cx, double cz, int y, int tierY, DoubleUnaryOperator radius, int limbs, double minReach,
                              double maxReach, double thickness, int depth, LimbStyle style, double angle, double s) {
        for (int i = 0; i < limbs; i++) {
            angle += GOLDEN_ANGLE + jitter(0.25);
            double sy = tierY - between(0, 3);
            double startRadius = radius.applyAsDouble(sy - y) - 1.5;
            double sx = cx + Math.cos(angle) * startRadius;
            double sz = cz + Math.sin(angle) * startRadius;
            double reach = between(minReach, maxReach);
            // Each tier stays fairly flat, so that the levels of the canopy stand apart
            double rise = between(1.5, 3.5) * s;
            double limbThickness = thickness * between(0.85, 1.15);
            // Long limbs sag slightly towards their tips, so the canopy slopes down at its edges like a cone
            double sag = reach * 0.06;
            int segments = Math.max(6, (int) Math.round(reach / 5));
            double px = sx;
            double py = sy;
            double pz = sz;
            for (int k = 1; k <= segments; k++) {
                double u = (double) k / segments;
                double heading = angle + jitter(0.08);
                double nx = sx + Math.cos(heading) * reach * u;
                double ny = sy + rise * Math.sin(u * Math.PI / 2) - sag * u * u;
                double nz = sz + Math.sin(heading) * reach * u;
                t.thickBranch(px, py, pz, nx, ny, nz, limbThickness * (1 - 0.5 * (k - 1) / segments),
                        limbThickness * (1 - 0.5 * k / segments));
                // Side branches leave the limb all along its length, alternating sides, so the long limbs stay full
                // They start thick and taper as they split, so only the ends of the smallest branches are 1 block wide
                if (u >= 0.25 && u < 1 && chance(0.85)) {
                    thickLimb(nx, ny, nz, angle + (k % 2 == 0 ? 1 : -1) * between(0.6, 1.1), between(0.15, 0.35),
                            reach * between(0.22, 0.32), depth - 1, style, limbThickness * (1 - 0.5 * u) * 0.55);
                }
                px = nx;
                py = ny;
                pz = nz;
            }
            thickLimb(px, py, pz, angle + jitter(0.3), between(0.1, 0.3), reach * between(0.3, 0.38), depth, style,
                    limbThickness * 0.5);
        }
        return angle;
    }

    /**
     * Grows one huge root of a banyan grove out of the swollen base of the trunk, snaking out across the ground in arches
     * before diving into it, and sometimes forking into a smaller root along the way.
     * @param sx The X coordinate where the root leaves the trunk.
     * @param y The height of the ground.
     * @param sz The Z coordinate where the root leaves the trunk.
     * @param angle The direction the root heads in.
     * @param startHeight How high above the ground the root leaves the trunk.
     * @param rootsBelow Below this height, the thin parts of the root turn into mangrove roots.
     * @param canFork Whether the root may fork into a smaller one.
     */
    private void banyanRoot(double sx, int y, double sz, double angle, double startHeight, double length, double thickness,
                            int rootsBelow, boolean canFork) {
        int humps = between(1, 2);
        double hump = between(1.5, 3.5);
        double phase = randomAngle();
        int steps = 16;
        double px = sx;
        double py = y + startHeight;
        double pz = sz;
        for (int i = 1; i <= steps; i++) {
            double u = (double) i / steps;
            // Snakes from side to side as it goes
            double heading = angle + Math.sin(u * Math.PI * 2 + phase) * 0.25;
            double step = length / steps;
            double nx = px + Math.cos(heading) * step;
            double nz = pz + Math.sin(heading) * step;
            // The arches only begin once the root has left the trunk
            double ny = y + startHeight * Math.pow(1 - u, 1.6) + hump * Math.sin(Math.PI * humps * u) * u;
            // The end of the root dives into the ground
            if (u > 0.85) {
                ny = Math.min(ny, y - 3 * (u - 0.85) / 0.15);
            }
            double r0 = thickness * (1 - 0.8 * (u - 1.0 / steps)) + 0.4;
            double r1 = thickness * (1 - 0.8 * u) + 0.4;
            Material material = r1 > 1.0 || ny >= rootsBelow ? Material.MANGROVE_WOOD : Material.MANGROVE_ROOTS;
            t.thickRoot(px, py, pz, nx, ny, nz, r0, r1, material);
            if (canFork && i == steps / 2 && chance(0.5)) {
                double forkAngle = heading + (chance(0.5) ? 1 : -1) * between(0.4, 0.7);
                banyanRoot(nx, y, nz, forkAngle, ny - y, length * 0.5, thickness * 0.4, rootsBelow, false);
            }
            px = nx;
            py = ny;
            pz = nz;
        }
    }

    /**
     * Hangs cave vines beneath the leaves, now and then bearing glow berries.
     */
    private void caveVines(double chance, int maxLength, double berryChance) {
        for (Pos leaf : t.getPositions(Layer.LEAF)) {
            if (t.has(leaf.x(), leaf.y() - 1, leaf.z()) || !chance(chance)) {
                continue;
            }
            int length = between(1, maxLength);
            for (int i = 1; i <= length; i++) {
                BlockData data;
                if (i == length) {
                    CaveVines tip = (CaveVines) Material.CAVE_VINES.createBlockData();
                    tip.setBerries(chance(berryChance));
                    tip.setAge(between(0, 25));
                    data = tip;
                } else {
                    CaveVinesPlant body = (CaveVinesPlant) Material.CAVE_VINES_PLANT.createBlockData();
                    body.setBerries(chance(berryChance));
                    data = body;
                }
                if (!t.decor(leaf.x(), leaf.y() - i, leaf.z(), data, 0, 1, 0)) {
                    break;
                }
            }
        }
    }

    /**
     * #17 Azalea and #18 Flowering Azalea - a short, bent trunk rising from rooted dirt with a wide, rounded canopy.
     * @param flowerRatio How much of the canopy is flowering.
     */
    private void azalea(int x, int y, int z, double flowerRatio) {
        t = new TreeBuilder(world, r, Material.OAK_WOOD, Material.AZALEA_LEAVES);
        t.setLeafMix(Material.AZALEA_LEAVES, Material.FLOWERING_AZALEA_LEAVES, flowerRatio);
        t.setPorosity(0.15);
        double scale = between(0.8, 1.3);
        double cx = x + 0.5;
        double cz = z + 0.5;
        int height = (int) Math.round(between(4.5, 6.0) * scale);
        double bend = randomAngle();
        double mx = cx + Math.cos(bend);
        double mz = cz + Math.sin(bend);
        double topX = cx + Math.cos(bend) * 0.5 + jitter(0.5);
        double topZ = cz + Math.sin(bend) * 0.5 + jitter(0.5);
        t.branch(cx, y, cz, mx, y + height * 0.5, mz);
        t.branch(mx, y + height * 0.5, mz, topX, y + height, topZ);
        t.root(x, y - 1, z, Material.ROOTED_DIRT, Axis.Y, false);

        int branches = 3;
        double start = randomAngle();
        for (int i = 0; i < branches; i++) {
            double angle = start + i * Math.PI * 2 / branches + jitter(0.4);
            double length = between(2.5, 3.5) * scale;
            double ex = topX + Math.cos(angle) * length;
            double ey = y + height + between(0.5, 1.5);
            double ez = topZ + Math.sin(angle) * length;
            t.branch(topX, y + height - 1, topZ, ex, ey, ez);
            t.blob(ex, ey + 0.5, ez, 3.0, 2.0, 3.0, 0.35);
        }
        t.blob(topX, y + height + 1, topZ, 4.6 * scale, 2.6, 4.6 * scale, 0.3);
        caveVines(0.12, 4, 0.2);
        groundCover(x, y, z, 4.2 * scale, 0.42, Material.MOSS_CARPET, false);
    }

    // ------------------------------------------------------------
    // Nether fungi
    // ------------------------------------------------------------

    /**
     * #19 Crimson Colossus - grown from a 2x2 of fungi. A thick 2x2 stem that swells out and blends into a great domed cap
     * with a drooping skirt, glowing shroomlights beneath, and weeping vines hanging inside the dome.
     */
    private void crimson(int x, int y, int z) {
        t = new TreeBuilder(world, r, Material.CRIMSON_HYPHAE, Material.NETHER_WART_BLOCK);
        double cx = x + 1;
        double cz = z + 1;
        int height = between(10, 18);
        double radius = 4.5 + height * 0.22 + jitter(0.6);
        int capHeight = Math.max(3, (int) Math.round(radius * 0.65));
        int capBase = y + height;
        // The stem reaches all the way up into the cap, so there is never a gap above it
        t.trunk(cx, cz, y, capBase + capHeight - 1, 1.0);
        flareStem(cx, cz, y, 1.0, (int) (height * 0.4));
        t.info("height", height);

        // Near the cap the stem swells outwards, with wart creeping down it, so that it blends into the dome
        int swell = Math.min(5, height - 2);
        // Wart only creeps down the top 1 or 2 blocks of the stem
        int wartFrom = capBase - between(1, 2);
        for (int i = 0; i <= swell + capHeight; i++) {
            int h = capBase - swell + i;
            double p = Math.min(1, (double) i / swell);
            double stemRadius = 1.0 + 1.8 * p * p;
            for (int dx = -5; dx <= 5; dx++) {
                for (int dz = -5; dz <= 5; dz++) {
                    int bx = (int) Math.floor(cx) + dx;
                    int bz = (int) Math.floor(cz) + dz;
                    double distance = Math.hypot(bx + 0.5 - cx, bz + 0.5 - cz);
                    if (distance > stemRadius + 0.01) {
                        continue;
                    }
                    // The outside of the swelling turns to wart further up
                    boolean wart = h >= wartFrom && distance > 1.2 && chance(0.6);
                    if (wart) {
                        t.solid(bx, h, bz, Material.NETHER_WART_BLOCK);
                    } else {
                        t.wood(bx, h, bz);
                    }
                }
            }
        }

        int range = (int) Math.ceil(radius) + 1;
        for (int dx = -range; dx <= range; dx++) {
            for (int dz = -range; dz <= range; dz++) {
                int bx = (int) Math.floor(cx) + dx;
                int bz = (int) Math.floor(cz) + dz;
                double distance = Math.hypot(bx + 0.5 - cx, bz + 0.5 - cz);
                if (distance > radius) {
                    continue;
                }
                int top = capBase + domeHeight(distance, radius, capHeight);
                // Reaches down to the height of the slope just outside it, so the dome has no holes seen from inside
                int bottom = Math.min(top - 1, capBase + domeHeight(distance + 1.2, radius, capHeight));
                boolean skirt = distance >= radius - 1.2;
                // The edge of the cap droops down into a skirt
                if (skirt) {
                    bottom = top - between(2, 3);
                }
                for (int h = bottom; h <= top; h++) {
                    t.solid(bx, h, bz, chance(0.05) ? Material.SHROOMLIGHT : Material.NETHER_WART_BLOCK);
                }
                if (!skirt && distance > 3.0 && chance(0.17)) {
                    t.solid(bx, bottom - 1, bz, Material.SHROOMLIGHT);
                }
                // Long weeping vines hang inside the dome, with only short ones on the outside of the skirt
                if (skirt) {
                    if (chance(0.25)) {
                        weepingVines(bx, bottom - 1, bz, between(1, 2));
                    }
                } else if (distance > 3.0 && chance(0.3)) {
                    weepingVines(bx, bottom - 1, bz, between(2, 7));
                }
            }
        }
    }

    /**
     * Provides the height of the top of a rounded cap above its base at a distance from its middle.
     */
    private static int domeHeight(double distance, double radius, int capHeight) {
        if (distance >= radius) {
            return 0;
        }
        return (int) Math.round(capHeight * (1 - Math.pow(distance / radius, 2)));
    }

    /**
     * #30 Crimson Helix - grown from a single fungus. A thin stem that corkscrews upwards, with spikes of wart jutting out
     * along it, each with a shroomlight tucked beneath its tip and trailing weeping vines.
     */
    private void crimsonHelix(int x, int y, int z) {
        t = new TreeBuilder(world, r, Material.CRIMSON_HYPHAE, Material.NETHER_WART_BLOCK);
        double cx = x + 0.5;
        double cz = z + 0.5;
        int height = between(10, 16);
        double twist = between(0.45, 0.65) * (chance(0.5) ? 1 : -1);
        double angle = randomAngle();
        double coil = between(1.0, 1.6);
        double px = cx;
        double pz = cz;
        t.info("height", height);
        for (int h = 1; h <= height; h++) {
            angle += twist;
            // The coil tightens towards the top
            double radius = coil * (1 - 0.6 * h / height) * Math.min(1, h / 3.0);
            double nx = cx + Math.cos(angle) * radius;
            double nz = cz + Math.sin(angle) * radius;
            t.branch(px, y + h - 1, pz, nx, y + h, nz);
            // Spikes jutting outwards from the coil
            if (h >= 3 && h % 2 == 0 && chance(0.75)) {
                double spikeAngle = angle + jitter(0.4);
                // Short and always angled upwards, so a spike never runs out flat like a separate shoot
                double length = Math.min(2.2, between(1.5, 3.0) * (1.1 - 0.5 * h / height));
                double ex = nx + Math.cos(spikeAngle) * length;
                double ey = y + h + between(1.2, 2.0);
                double ez = nz + Math.sin(spikeAngle) * length;
                List<int[]> spike = new ArrayList<>();
                t.line(nx, y + h, nz, ex, ey, ez, (bx, by, bz, axis) -> {
                    t.solid(bx, by, bz, Material.NETHER_WART_BLOCK);
                    spike.add(new int[] {bx, by, bz});
                });
                // The shroomlight hangs beneath the tip, with vines trailing from it
                int[] tip = spike.get(spike.size() - 1);
                if (!t.has(tip[0], tip[1] - 1, tip[2])) {
                    t.solid(tip[0], tip[1] - 1, tip[2], Material.SHROOMLIGHT);
                    weepingVines(tip[0], tip[1] - 2, tip[2], between(1, 5));
                }
                // More vines hang from along the underside of the spike
                for (int[] block : spike) {
                    if (chance(0.35)) {
                        weepingVines(block[0], block[1] - 1, block[2], between(1, 3));
                    }
                }
            }
            px = nx;
            pz = nz;
        }
        // A small crown of wart at the top, with a shroomlight glowing beneath it
        t.blob(px, y + height + 1, pz, 1.6, 1.4, 1.6, 0.3);
        int crownX = (int) Math.floor(px + (chance(0.5) ? 1 : -1));
        int crownZ = (int) Math.floor(pz);
        if (!t.has(crownX, y + height - 1, crownZ)) {
            t.solid(crownX, y + height - 1, crownZ, Material.SHROOMLIGHT);
            weepingVines(crownX, y + height - 2, crownZ, between(1, 3));
        }
    }

    /**
     * Hangs weeping vines downwards from just below a block.
     */
    private void weepingVines(int x, int topY, int z, int length) {
        for (int i = 0; i < length; i++) {
            Material vine = i == length - 1 ? Material.WEEPING_VINES : Material.WEEPING_VINES_PLANT;
            if (!t.decor(x, topY - i, z, vine.createBlockData(), 0, 1, 0)) {
                break;
            }
        }
    }

    /**
     * #20 Warped Candelabra and #31 Little Warped Candelabra - a stem that splits into several arms curving out and up like
     * a candelabra or a branch of coral, each arm ending in a small rounded cap of warped wart with a shroomlight glowing
     * beneath it, and twisting vines growing up from the ground around its base.
     * @param size The width of the square of fungi it grew from, a 2x2 growing a larger one with more arms.
     */
    private void warped(int x, int y, int z, int size) {
        t = new TreeBuilder(world, r, Material.WARPED_HYPHAE, Material.WARPED_WART_BLOCK);
        boolean large = size >= 2;
        double cx = x + size / 2.0;
        double cz = z + size / 2.0;
        int stemHeight = large ? between(6, 9) : between(3, 5);
        int arms = large ? between(4, 6) : between(2, 3);
        double stemRadius = large ? 1.0 : 0.5;
        t.info("arms", arms);

        // The stem leans slightly as it rises
        double lean = randomAngle();
        double topX = cx + Math.cos(lean) * between(0.0, large ? 1.0 : 0.6);
        double topZ = cz + Math.sin(lean) * between(0.0, large ? 1.0 : 0.6);
        int topY = y + stemHeight;
        for (int h = 0; h <= stemHeight; h++) {
            double p = (double) h / stemHeight;
            t.trunk(cx + (topX - cx) * p, cz + (topZ - cz) * p, y + h, y + h, stemRadius);
        }
        flareStem(cx, cz, y, stemRadius, large ? 3 : 1);

        double start = randomAngle();
        for (int i = 0; i < arms; i++) {
            double angle = start + i * Math.PI * 2 / arms + jitter(0.4);
            // Arms leave the stem at different heights, the lowest ones reaching furthest
            int armY = topY - (large ? between(0, 3) : between(0, 1));
            double reach = (large ? between(4.5, 7.0) : between(2.0, 3.2)) * (1.0 + 0.1 * (topY - armY));
            double rise = large ? between(5.0, 9.0) : between(3.0, 5.0);
            double sx = topX + Math.cos(angle) * stemRadius * 0.5;
            double sz = topZ + Math.sin(angle) * stemRadius * 0.5;
            // Some of the arms of a large one stay 2 blocks thick all the way to their caps
            boolean thick = large && chance(0.5);
            // Bends outwards first, then turns to climb upwards
            double px = sx;
            double py = armY;
            double pz = sz;
            for (int k = 1; k <= 4; k++) {
                double p = k / 4.0;
                double out = reach * Math.sin(p * Math.PI / 2);
                double nx = sx + Math.cos(angle) * out;
                double ny = armY + rise * (1 - Math.cos(p * Math.PI / 2));
                double nz = sz + Math.sin(angle) * out;
                if (thick) {
                    t.thickBranch(px, py, pz, nx, ny, nz, 0.9, 0.8);
                } else if (large && k <= 2) {
                    t.thickBranch(px, py, pz, nx, ny, nz, 0.8, 0.6);
                } else {
                    t.branch(px, py, pz, nx, ny, nz);
                }
                px = nx;
                py = ny;
                pz = nz;
            }
            warpedCap(px, (int) Math.floor(py) + 1, pz, large ? between(3.0, 4.0) : between(1.4, 2.0));
        }
        // The middle of the stem carries on up a little to a cap of its own
        if (large || chance(0.5)) {
            int crownY = topY + between(2, 4);
            t.branch(topX, topY, topZ, topX, crownY, topZ);
            warpedCap(topX, crownY + 1, topZ, large ? between(2.6, 3.4) : 1.4);
        }
        // Little shelves of wart sticking out of the side of the stem, like an oyster mushroom
        int shelves = large ? between(2, 4) : between(1, 2);
        double shelfAngle = randomAngle();
        for (int i = 0; i < shelves; i++) {
            shelfAngle += GOLDEN_ANGLE + jitter(0.4);
            int h = between(1, Math.max(1, stemHeight - 1));
            double p = (double) h / stemHeight;
            double sx = cx + (topX - cx) * p + Math.cos(shelfAngle) * (stemRadius + 0.5);
            double sz = cz + (topZ - cz) * p + Math.sin(shelfAngle) * (stemRadius + 0.5);
            warpedShelf(sx, y + h, sz, shelfAngle, large ? between(1.6, 2.4) : between(1.2, 1.7));
        }
        // Twisting vines sprout from the tops of the stem and arms, as well as from the ground around the base
        for (Pos wood : t.getPositions(Layer.LOG)) {
            if (wood.y() > y + 1 && !t.has(wood.x(), wood.y() + 1, wood.z()) && chance(0.06)) {
                int length = between(1, 3);
                for (int i = 1; i <= length; i++) {
                    Material vine = i == length ? Material.TWISTING_VINES : Material.TWISTING_VINES_PLANT;
                    if (!t.decor(wood.x(), wood.y() + i, wood.z(), vine.createBlockData(), 0, -1, 0)) {
                        break;
                    }
                }
            }
        }
        baseVines(cx, y, cz, stemRadius + 0.5, large ? 4.5 : 3.0);
    }

    /**
     * Places a little half-round shelf of warped wart sticking out of the side of a stem, like an oyster mushroom, often
     * with a shroomlight glowing in it.
     * @param angle The direction the shelf faces away from the stem.
     */
    private void warpedShelf(double sx, int y, double sz, double angle, double radius) {
        double ox = Math.cos(angle);
        double oz = Math.sin(angle);
        int range = (int) Math.ceil(radius) + 1;
        List<int[]> blocks = new ArrayList<>();
        for (int dx = -range; dx <= range; dx++) {
            for (int dz = -range; dz <= range; dz++) {
                int bx = (int) Math.floor(sx) + dx;
                int bz = (int) Math.floor(sz) + dz;
                double rx = bx + 0.5 - sx;
                double rz = bz + 0.5 - sz;
                // Only the half facing away from the stem
                if (Math.hypot(rx, rz) > radius || rx * ox + rz * oz < -0.3 || t.has(bx, y, bz)) {
                    continue;
                }
                blocks.add(new int[] {bx, bz});
            }
        }
        int light = !blocks.isEmpty() && chance(0.7) ? r.nextInt(blocks.size()) : -1;
        for (int i = 0; i < blocks.size(); i++) {
            t.solid(blocks.get(i)[0], y, blocks.get(i)[1], i == light ? Material.SHROOMLIGHT : Material.WARPED_WART_BLOCK);
        }
    }

    /**
     * Grows twisting vines up from the ground around the base of a warped fungus, thinning out away from the stem.
     * @param inner How far from the middle of the stem the vines start.
     * @param outer How far from the middle of the stem the vines reach.
     */
    private void baseVines(double cx, int y, double cz, double inner, double outer) {
        int range = (int) Math.ceil(outer);
        for (int dx = -range; dx <= range; dx++) {
            for (int dz = -range; dz <= range; dz++) {
                int bx = (int) Math.floor(cx) + dx;
                int bz = (int) Math.floor(cz) + dz;
                double distance = Math.hypot(bx + 0.5 - cx, bz + 0.5 - cz);
                if (distance < inner || distance > outer || !chance(0.35 * fade(distance - inner, outer - inner))) {
                    continue;
                }
                Block ground = findGround(bx, y, bz, 3);
                if (ground == null || t.has(ground.getX(), ground.getY() - 1, ground.getZ())) {
                    continue;
                }
                // Taller near the stem
                int length = Math.max(1, (int) Math.round(between(1, 4) * fade(distance - inner, outer - inner)));
                for (int i = 0; i < length; i++) {
                    Material vine = i == length - 1 ? Material.TWISTING_VINES : Material.TWISTING_VINES_PLANT;
                    if (!t.decor(ground.getX(), ground.getY() + i, ground.getZ(), vine.createBlockData(), 0, -1, 0)) {
                        break;
                    }
                }
            }
        }
    }

    /**
     * Builds a small rounded cap of warped wart at the end of an arm, with a shroomlight glowing beneath it and twisting
     * vines sprouting up from its top.
     * @param baseY The height of the bottom of the cap.
     */
    private void warpedCap(double cx, int baseY, double cz, double radius) {
        int capHeight = Math.max(2, (int) Math.round(radius * 0.9));
        int range = (int) Math.ceil(radius) + 1;
        for (int dx = -range; dx <= range; dx++) {
            for (int dz = -range; dz <= range; dz++) {
                int bx = (int) Math.floor(cx) + dx;
                int bz = (int) Math.floor(cz) + dz;
                double distance = Math.hypot(bx + 0.5 - cx, bz + 0.5 - cz);
                if (distance > radius) {
                    continue;
                }
                int top = baseY + domeHeight(distance, radius, capHeight);
                // The edge hangs down a block, like the rim of a cap
                int bottom = distance > radius - 1 ? baseY - 1 : baseY;
                for (int h = bottom; h <= top; h++) {
                    t.solid(bx, h, bz, chance(0.04) ? Material.SHROOMLIGHT : Material.WARPED_WART_BLOCK);
                }
            }
        }
        // The shroomlight sits beneath the cap, where the arm meets it
        int lx = (int) Math.floor(cx) + (chance(0.5) ? 1 : -1);
        int lz = (int) Math.floor(cz);
        if (!t.has(lx, baseY - 1, lz)) {
            t.solid(lx, baseY - 1, lz, Material.SHROOMLIGHT);
        }
    }

    /**
     * Widens the base of a fungus stem, thinning out as it rises.
     * @param radius The radius of the stem.
     */
    private void flareStem(double cx, double cz, int y, double radius, int height) {
        for (int h = 0; h <= height; h++) {
            double chance = h == 0 ? 0.9 : 0.9 - 0.8 * h / (height + 1);
            for (int dx = -3; dx <= 3; dx++) {
                for (int dz = -3; dz <= 3; dz++) {
                    int bx = (int) Math.floor(cx) + dx;
                    int bz = (int) Math.floor(cz) + dz;
                    double distance = Math.hypot(bx + 0.5 - cx, bz + 0.5 - cz);
                    if (distance > radius + 0.01 && distance <= radius + 1.0 && chance(chance)) {
                        t.wood(bx, y + h, bz);
                    }
                }
            }
        }
    }

    // ------------------------------------------------------------
    // Giant mushrooms
    // ------------------------------------------------------------

    /**
     * Starts a giant mushroom with a stem of mushroom stem blocks.
     * @param size The width of the square of mushrooms it grew from.
     * @return The scale of the mushroom, larger when grown from a 2x2.
     */
    private double startMushroom(Material cap, int size) {
        t = new TreeBuilder(world, r, Material.MUSHROOM_STEM, cap);
        double scale = size >= 2 ? between(1.5, 1.8) : between(0.9, 1.15);
        t.info("scale", scale);
        return scale;
    }

    /**
     * Grows the stem of a giant mushroom, 1 wide or 2x2, which may curve to one side.
     * @param lean How far the top of the stem is offset from its base.
     * @return The center of the top of the stem.
     */
    private double[] mushroomStem(int x, int y, int z, int size, int height, double lean) {
        double cx = x + size / 2.0;
        double cz = z + size / 2.0;
        double angle = randomAngle();
        double topX = cx + Math.cos(angle) * lean;
        double topZ = cz + Math.sin(angle) * lean;
        if (size < 2) {
            // A thin stem always ends in the middle of a block, so the cap is centered on it
            topX = Math.floor(topX) + 0.5;
            topZ = Math.floor(topZ) + 0.5;
        }
        double px = cx;
        double pz = cz;
        for (int h = 0; h <= height; h++) {
            double p = (double) h / height;
            double bend = p * p;
            double nx = cx + (topX - cx) * bend;
            double nz = cz + (topZ - cz) * bend;
            if (size >= 2) {
                // Always a full 2x2, even where the curve passes between blocks, so the stem never has gaps
                placeSquare((int) Math.round(nx) - 1, y + h, (int) Math.round(nz) - 1);
            } else {
                // Always places a block, even where the curve passes between blocks
                t.wood((int) Math.floor(nx), y + h, (int) Math.floor(nz));
            }
            // Keeps a thin stem connected where it curves sideways
            if (h > 0) {
                t.branch(px, y + h - 1, pz, nx, y + h - 1, nz);
            }
            px = nx;
            pz = nz;
        }
        return new double[] {topX, topZ};
    }

    /**
     * Carries the top of a mushroom stem on upwards until it meets the cap above it, so that there is never a gap.
     * @param fromY The first height above the top of the stem.
     */
    private void stemUpToCap(double cx, double cz, int fromY, int size) {
        List<int[]> columns = new ArrayList<>();
        if (size >= 2) {
            for (int dx = -1; dx <= 0; dx++) {
                for (int dz = -1; dz <= 0; dz++) {
                    columns.add(new int[] {(int) Math.round(cx) + dx, (int) Math.round(cz) + dz});
                }
            }
        } else {
            columns.add(new int[] {(int) Math.floor(cx), (int) Math.floor(cz)});
        }
        for (int[] column : columns) {
            for (int h = fromY; h < fromY + 5 && !t.has(column[0], h, column[1]); h++) {
                t.wood(column[0], h, column[1]);
            }
        }
    }

    /**
     * Places a low mushroom cap, a single layer thick, that may be slightly rounded on top and dipped at its edges.
     * @param bump How far the middle of the cap rises above its base.
     * @param droop How far the edge of the cap dips below its base.
     */
    private void lowCap(double cx, int y, double cz, double radius, int bump, int droop, Material cap, boolean smooth) {
        int range = (int) Math.ceil(radius) + 1;
        Map<Long, Integer> bottoms = new HashMap<>();
        for (int dx = -range; dx <= range; dx++) {
            for (int dz = -range; dz <= range; dz++) {
                int bx = (int) Math.floor(cx) + dx;
                int bz = (int) Math.floor(cz) + dz;
                double distance = Math.hypot(bx + 0.5 - cx, bz + 0.5 - cz);
                if (distance > radius) {
                    continue;
                }
                int top = y + capSurface(distance, radius, bump, droop, smooth);
                // Fills down to the next ring outwards, so that the cap stays closed where it slopes
                int bottom = Math.min(top, y + capSurface(distance + 1, radius, bump, droop, smooth));
                for (int h = bottom; h <= top; h++) {
                    t.solid(bx, h, bz, cap);
                }
                bottoms.put(columnKey(bx, bz), bottom);
            }
        }
        if (!smooth) {
            return;
        }
        // Fills the inside corners of the steps on the underside, so that it curves smoothly instead of in harsh cuts
        List<int[]> fills = new ArrayList<>();
        for (Map.Entry<Long, Integer> column : bottoms.entrySet()) {
            int bx = (int) (column.getKey() >> 32);
            int bz = (int) (long) column.getKey();
            int bottom = column.getValue();
            boolean[] lower = new boolean[4];
            for (int i = 0; i < 4; i++) {
                Integer neighbour = bottoms.get(columnKey(bx + HORIZONTAL[i].getModX(), bz + HORIZONTAL[i].getModZ()));
                lower[i] = neighbour != null && neighbour < bottom;
            }
            // Two lower neighbours side by side make an inside corner
            for (int i = 0; i < 4; i++) {
                if (lower[i] && lower[(i + 1) % 4]) {
                    fills.add(new int[] {bx, bottom - 1, bz});
                    break;
                }
            }
        }
        for (int[] fill : fills) {
            t.solid(fill[0], fill[1], fill[2], cap);
        }
    }

    private static long columnKey(int x, int z) {
        return ((long) x << 32) | (z & 0xFFFFFFFFL);
    }

    /**
     * Provides the height of the top of a low cap above its base at a distance from its middle.
     * @param graded Whether the edge dips down gradually over several rings, rather than suddenly at the outermost one.
     */
    private static int capSurface(double distance, double radius, int bump, int droop, boolean graded) {
        if (graded) {
            double d = Math.min(distance, radius);
            double rise = bump * (1 - Math.pow(d / radius, 2));
            double band = droop + 1.0;
            double edge = Math.max(0, Math.min(1, (d - (radius - band)) / band));
            return (int) Math.round(rise - droop * edge * edge * (3 - 2 * edge));
        }
        if (distance > radius) {
            return -droop;
        }
        int height = (int) Math.round(bump * (1 - Math.pow(distance / radius, 2)));
        return distance > radius - 1 ? height - droop : height;
    }

    /**
     * Places a domed mushroom cap as a hollow shell, with a skirt hanging from its edge.
     * @param height The height of the dome above its base.
     * @param skirt How far the edge hangs down below the base.
     * @return The height of the inside of the top of the dome, just beneath its shell.
     */
    private int domeCap(double cx, int baseY, double cz, double radius, double height, int skirt, Material cap) {
        int range = (int) Math.ceil(radius) + 1;
        for (int dx = -range; dx <= range; dx++) {
            for (int dz = -range; dz <= range; dz++) {
                int bx = (int) Math.floor(cx) + dx;
                int bz = (int) Math.floor(cz) + dz;
                double distance = Math.hypot(bx + 0.5 - cx, bz + 0.5 - cz);
                if (distance > radius) {
                    continue;
                }
                double surface = height * Math.sqrt(Math.max(0, 1 - Math.pow(distance / radius, 2)));
                int top = baseY + (int) Math.round(surface);
                // Only the shell, at least as thick as the slope so it has no holes
                double inner = radius - 1.2;
                int innerTop = distance < inner
                        ? baseY + (int) Math.round(height * Math.sqrt(Math.max(0, 1 - Math.pow(distance / inner, 2)))) - 1
                        : baseY - 1;
                for (int h = Math.max(baseY, innerTop + 1); h <= top; h++) {
                    t.solid(bx, h, bz, cap);
                }
                if (distance > radius - 1.2) {
                    for (int i = 1; i <= skirt; i++) {
                        t.solid(bx, baseY - i, bz, cap);
                    }
                }
            }
        }
        return baseY + (int) Math.round(height) - 1;
    }

    /**
     * Carries the stem of a red mushroom on up through the hollow of its dome to the top, swelling out where it meets the
     * cap so that it blends into it.
     * @param fromY The height of the top of the curved part of the stem.
     * @param toY The height of the inside of the top of the dome.
     * @param capRadius The radius of the cap, as the stem never reaches its outermost ring so it can't be seen outside.
     */
    private void stemToCap(double cx, double cz, int fromY, int toY, int size, double capRadius) {
        double radius = size >= 2 ? 1.0 : 0.5;
        if (size >= 2) {
            for (int h = fromY; h <= toY; h++) {
                placeSquare((int) Math.round(cx) - 1, h, (int) Math.round(cz) - 1);
            }
        } else {
            for (int h = fromY; h <= toY; h++) {
                t.wood((int) Math.floor(cx), h, (int) Math.floor(cz));
            }
        }
        for (int h = Math.max(fromY, toY - 1); h <= toY; h++) {
            double swell = radius + (h == toY ? 1.3 : 0.8);
            int range = (int) Math.ceil(swell) + 1;
            for (int dx = -range; dx <= range; dx++) {
                for (int dz = -range; dz <= range; dz++) {
                    int bx = (int) Math.floor(cx) + dx;
                    int bz = (int) Math.floor(cz) + dz;
                    double distance = Math.hypot(bx + 0.5 - cx, bz + 0.5 - cz);
                    // Stays out of the shell of the cap and its outermost ring, so no stem shows from outside
                    if (distance > swell || distance > capRadius - 1 || t.has(bx, h, bz)) {
                        continue;
                    }
                    // The outer edge of the swelling is the red of the cap creeping down the stem
                    if (distance > swell - 0.8 && chance(0.6)) {
                        t.solid(bx, h, bz, Material.RED_MUSHROOM_BLOCK);
                    } else {
                        t.wood(bx, h, bz);
                    }
                }
            }
        }
    }

    /**
     * Now and then turns some of the soil around the stem of a mushroom into mycelium, thinning out away from it.
     */
    private void myceliumPatch(int x, int y, int z, int size) {
        if (!chance(0.3)) {
            return;
        }
        double cx = x + size / 2.0;
        double cz = z + size / 2.0;
        double radius = between(2.5, 4.0) + size;
        int range = (int) Math.ceil(radius);
        for (int dx = -range; dx <= range; dx++) {
            for (int dz = -range; dz <= range; dz++) {
                int bx = (int) Math.floor(cx) + dx;
                int bz = (int) Math.floor(cz) + dz;
                double distance = Math.hypot(bx + 0.5 - cx, bz + 0.5 - cz);
                if (distance > radius || !chance(0.8 * fade(distance, radius))) {
                    continue;
                }
                Block ground = findGround(bx, y, bz, 3);
                if (ground == null) {
                    continue;
                }
                Block soil = ground.getRelative(BlockFace.DOWN);
                Material type = soil.getType();
                if ((type == Material.GRASS_BLOCK || type == Material.DIRT || type == Material.PODZOL)
                        && !t.has(soil.getX(), soil.getY(), soil.getZ())) {
                    t.soil(soil.getX(), soil.getY(), soil.getZ(), Material.MYCELIUM);
                }
            }
        }
    }

    /**
     * #32 Button Brown Cap and #36 Great Flat Brown Cap - a tall stem, often curving to one side, holding a wide, low
     * brown cap like a button mushroom. It is either flat with its edges dipped down, or slightly rounded on top. The great
     * one is always rounded on top and droops further at its edges.
     */
    private void brownCap(int x, int y, int z, int size) {
        double scale = startMushroom(Material.BROWN_MUSHROOM_BLOCK, size);
        boolean large = size >= 2;
        int height = (int) Math.round(between(5.0, 7.5) * scale);
        double lean = chance(0.5) ? between(1.5, 3.0) * scale : between(0.0, 0.5);
        double[] top = mushroomStem(x, y, z, size, height, Math.min(lean, height * 0.5));
        // The cap is never more than 2 blocks tall, or 4 for a great one: a small one is either rounded on top or dipped
        // at its edges
        int bump;
        int droop;
        if (large) {
            bump = between(1, 2);
            droop = Math.min(between(1, 2), 3 - bump);
        } else {
            bump = chance(0.5) ? 1 : 0;
            droop = 1 - bump;
        }
        double radius = (large ? between(4.0, 4.8) : between(3.6, 4.4)) * scale;
        lowCap(top[0], y + height + 1, top[1], radius, bump, droop, Material.BROWN_MUSHROOM_BLOCK, large);
        stemUpToCap(top[0], top[1], y + height + 1, size);
        t.info("bump", bump);
    }

    /**
     * #34 Red Toadstool and #38 Great Red Toadstool - a thick, curving stem under a round, bulbous red dome with a skirt,
     * hollow inside like a little home, with the stem reaching up through it to the top.
     */
    private void redToadstool(int x, int y, int z, int size) {
        double scale = startMushroom(Material.RED_MUSHROOM_BLOCK, size);
        int height = (int) Math.round(between(4.0, 5.5) * scale);
        double[] top = mushroomStem(x, y, z, size, height, redLean(size, height, Math.min(between(2.0, 5.0), height * 0.7)));
        double radius = between(3.6, 4.4) * scale;
        int baseY = y + height - 1;
        int inside = domeCap(top[0], baseY, top[1], radius, radius * between(0.85, 1.05), between(1, 2),
                Material.RED_MUSHROOM_BLOCK);
        stemToCap(top[0], top[1], y + height, inside, size, radius);
        myceliumPatch(x, y, z, size);
    }

    /**
     * #35 Tall Red Bell and #39 Great Red Bell - a tall, curving stem holding a smaller bell-shaped red cap, with the stem
     * reaching up through it to the top.
     */
    private void tallRedBell(int x, int y, int z, int size) {
        double scale = startMushroom(Material.RED_MUSHROOM_BLOCK, size);
        int height = (int) Math.round(between(8.0, 11.0) * scale);
        double[] top = mushroomStem(x, y, z, size, height, redLean(size, height, between(2.0, 5.0)));
        double radius = between(2.6, 3.2) * scale;
        int baseY = y + height - 2;
        int inside = domeCap(top[0], baseY, top[1], radius, radius * between(1.1, 1.4), between(2, 3),
                Material.RED_MUSHROOM_BLOCK);
        stemToCap(top[0], top[1], y + height, inside, size, radius);
        myceliumPatch(x, y, z, size);
    }

    /**
     * Provides how far the stem of a red mushroom curves to one side. A single mushroom only curves by a block, and only
     * when its stem is tall enough for the curve to look natural.
     * @param greatLean How far the stem of a great mushroom grown from a 2x2 curves.
     */
    private double redLean(int size, int height, double greatLean) {
        if (size >= 2) {
            return greatLean;
        }
        return height >= 6 ? 1.0 : 0;
    }
}
