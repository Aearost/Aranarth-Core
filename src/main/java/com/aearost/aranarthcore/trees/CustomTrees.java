package com.aearost.aranarthcore.trees;

import com.aearost.aranarthcore.enums.Month;
import com.aearost.aranarthcore.trees.TreeBuilder.Layer;
import com.aearost.aranarthcore.trees.TreeBuilder.Pos;
import com.aearost.aranarthcore.utils.AranarthUtils;
import com.aearost.aranarthcore.utils.DateUtils;
import org.bukkit.Axis;
import org.bukkit.Material;
import org.bukkit.Tag;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.Ageable;
import org.bukkit.block.data.Bisected;
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
import java.util.function.Predicate;

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
        ROUND_OAK(86, Material.OAK_SAPLING, 1, "Round Oak"),
        OLD_GIANT_OAK(2, Material.OAK_SAPLING, 3, "Old Giant Oak"),
        TALL_FOREST_OAK(3, Material.OAK_SAPLING, 2, "Tall Forest Oak"),
        SILVER_BIRCH(4, Material.BIRCH_SAPLING, 1, "Silver Birch"),
        COLUMN_BIRCH(41, Material.BIRCH_SAPLING, 1, "Column Birch"),
        BIRCH_SPIRE(46, Material.BIRCH_SAPLING, 2, "Birch Spire"),
        LEANING_BIRCH(48, Material.BIRCH_SAPLING, 2, "Leaning Birch"),
        POPLAR(5, Material.POPLAR_SAPLING, 1, "Poplar"),
        MAPLE(60, Material.POPLAR_SAPLING, 1, "Maple"),
        GREAT_POPLAR(44, Material.POPLAR_SAPLING, 2, "Great Poplar"),
        MOUNTAIN_SPRUCE(6, Material.SPRUCE_SAPLING, 2, "Mountain Spruce"),
        TIERED_SPRUCE(87, Material.SPRUCE_SAPLING, 2, "Tiered Spruce"),
        SMALL_MOUNTAIN_SPRUCE(7, Material.SPRUCE_SAPLING, 1, "Small Mountain Spruce"),
        TALL_PINE(8, Material.SPRUCE_SAPLING, 1, "Tall Pine"),
        JUNGLE_EMERGENT(9, Material.JUNGLE_SAPLING, 2, "Jungle Emergent"),
        KAPOK(10, Material.JUNGLE_SAPLING, 5, "Kapok"),
        SLENDER_JUNGLE(21, Material.JUNGLE_SAPLING, 1, "Slender Jungle"),
        JUNGLE_PARASOL(88, Material.JUNGLE_SAPLING, 1, "Jungle Parasol"),
        SAVANNA_UMBRELLA(12, Material.ACACIA_SAPLING, 1, "Savanna Umbrella"),
        FORKED_ACACIA(89, Material.ACACIA_SAPLING, 1, "Forked Acacia"),
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
        ELDER_CHERRY(90, Material.CHERRY_SAPLING, 3, "Elder Cherry"),
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
        GREAT_RED_BELL(39, Material.RED_MUSHROOM, 2, "Great Red Bell"),
        // Rare trees, which only grow where their conditions are met
        DRAGONBLOOD(49, Material.MANGROVE_PROPAGULE, 2, "Dragonblood Tree", "Desert"),
        COCONUT_PALM(50, Material.JUNGLE_SAPLING, 1, "Coconut Palm", "Beach"),
        DATE_PALM(51, Material.JUNGLE_SAPLING, 1, "Date Palm", "Desert, within 15 blocks of water"),
        RAINBOW_EUCALYPTUS(52, Material.JUNGLE_SAPLING, 2, "Rainbow Eucalyptus", "Any jungle, within 15 blocks of water"),
        STRANGLER_FIG(53, Material.JUNGLE_SAPLING, 3, "Strangler Fig", "Any jungle"),
        GIANT_SEQUOIA(54, Material.SPRUCE_SAPLING, 5, "Giant Sequoia", "Old Growth Pine Taiga"),
        REDWOOD(79, Material.SPRUCE_SAPLING, 3, "Redwood", "Old Growth Spruce Taiga"),
        BALD_CYPRESS(56, Material.SPRUCE_SAPLING, 2, "Bald Cypress", "Swamp, on the bank next to water"),
        WINDSWEPT_HAWTHORN(57, Material.OAK_SAPLING, 1, "Windswept Hawthorn", "Windswept Hills, Gravelly Hills, Forest"),
        JOSHUA_TREE(59, Material.ACACIA_SAPLING, 1, "Joshua Tree", "Any Badlands"),
        SNOW_GUM(61, Material.BIRCH_SAPLING, 1, "Snow Gum", "Snowy Plains, Snowy Taiga, Ice Spikes"),
        PENCIL_CYPRESS(62, Material.SPRUCE_SAPLING, 1, "Pencil Cypress", "Meadow"),
        GREAT_CEDAR(63, Material.SPRUCE_SAPLING, 3, "Great Cedar", "Meadow"),
        MONKEY_PUZZLE(64, Material.SPRUCE_SAPLING, 2, "Monkey Puzzle", "Windswept Forest"),
        DEAD_SNAG(65, Material.SPRUCE_SAPLING, 1, "Dead Snag", "Pale Garden"),
        WITCH_TREE(66, Material.DARK_OAK_SAPLING, 1, "Witch Tree", "Swamp"),
        OLIVE_TREE(67, Material.PALE_OAK_SAPLING, 1, "Olive Tree", "Savanna Plateau"),
        FLAME_TREE(68, Material.ACACIA_SAPLING, 2, "Flame Tree", "Savanna Plateau"),
        TREE_FERN(69, Material.JUNGLE_SAPLING, 1, "Tree Fern", "Lush Caves"),
        GIANT_BONSAI(71, Material.OAK_SAPLING, 1, "Giant Bonsai", "Planted on a moss block"),
        MYCELIAL_OAK(72, Material.OAK_SAPLING, 1, "Mycelial Oak", "Planted on mycelium"),
        ORCHARD_OAK(73, Material.OAK_SAPLING, 1, "Orchard Oak", "Sunflower Plains"),
        FLAME_HELIX(75, Material.CRIMSON_FUNGUS, 1, "Flame Helix", "Nether Wastes"),
        WARPED_BLOOM(76, Material.WARPED_FUNGUS, 2, "Warped Bloom", "Nether Wastes"),
        EMBER_OAK(77, Material.OAK_SAPLING, 1, "Ember Oak", "Nether Wastes"),
        SOUL_PINE(78, Material.SPRUCE_SAPLING, 1, "Soul Pine", "Soul Sand Valley"),
        LANTERN_LILY(80, Material.PALE_OAK_SAPLING, 2, "Lantern Lily", "Any End biome"),
        BONE_SPINE(85, Material.PALE_OAK_SAPLING, 1, "Bone Spine", "Any End biome"),
        BONE_SPRAWL(84, Material.PALE_OAK_SAPLING, 1, "Bone Sprawl", "Any End biome"),
        HALO_TREE(83, Material.POPLAR_SAPLING, 2, "Halo Tree", "Any End biome");

        private final int id;
        private final Material sapling;
        /** The width of the square of saplings it grows from, 1 for a single sapling, or 0 if it grows from any. */
        private final int size;
        private final String displayName;
        /** Where a rare tree grows, or null for a tree that grows anywhere. */
        private final String condition;

        Variant(int id, Material sapling, String displayName) {
            this(id, sapling, 0, displayName);
        }

        Variant(int id, Material sapling, int size, String displayName) {
            this(id, sapling, size, displayName, null);
        }

        Variant(int id, Material sapling, int size, String displayName, String condition) {
            this.id = id;
            this.sapling = sapling;
            this.size = size;
            this.displayName = displayName;
            this.condition = condition;
        }

        /**
         * Determines if the variant is a rare tree, which only grows where its conditions are met.
         */
        public boolean isRare() {
            return condition != null;
        }

        /**
         * Provides where a rare tree grows, or null for a tree that grows anywhere.
         */
        public String getCondition() {
            return condition;
        }

        /**
         * Provides every rare tree.
         */
        public static List<Variant> rare() {
            List<Variant> variants = new ArrayList<>();
            for (Variant variant : values()) {
                if (variant.isRare()) {
                    variants.add(variant);
                }
            }
            return variants;
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
         * Provides every variant that the sapling can grow into anywhere, leaving out rare trees.
         */
        public static List<Variant> forSapling(Material sapling) {
            List<Variant> variants = new ArrayList<>();
            for (Variant variant : values()) {
                if (variant.sapling == sapling && !variant.isRare()) {
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
        return design(sapling, size, world, random, x, y, z, variant -> true);
    }

    /**
     * Designs a random custom tree that the sapling can grow into, out of the ones a player has left turned on.
     * @param enabled Determines if each tree may be picked. If every tree for this size is turned off, there is no
     *                custom tree, and the sapling grows its vanilla tree.
     * @return The designed tree, or null if the sapling has no custom tree it may grow into.
     */
    public static TreeBuilder design(Material sapling, int size, World world, Random random, int x, int y, int z,
                                     Predicate<Variant> enabled) {
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
        if (sized.isEmpty()) {
            sized = new ArrayList<>(variants);
            sized.removeIf(variant -> variant.getSize() != 0);
        }
        // Only the trees the player has left turned on
        sized.removeIf(variant -> !enabled.test(variant));
        if (sized.isEmpty()) {
            return null;
        }
        return design(sized.get(random.nextInt(sized.size())), world, random, x, y, z);
    }

    /**
     * Designs a custom tree of a specific variant.
     */
    public static TreeBuilder design(Variant variant, World world, Random random, int x, int y, int z) {
        return design(variant, world, random, x, y, z, -1);
    }

    /**
     * Designs a custom tree of a specific variant, picking which of its forms to grow for a variant that has several,
     * such as the four sizes of a Flame Tree.
     * @param idea The form to grow, or -1 for a random one.
     */
    public static TreeBuilder design(Variant variant, World world, Random random, int x, int y, int z, int idea) {
        CustomTrees trees = new CustomTrees(world, random);
        // A fixed stage of autumn for each sample in the test world, otherwise taken from the calendar
        int season = idea < 0 ? -1 : idea % 4;
        switch (variant) {
            case SPREADING_OAK -> trees.spreadingOak(x, y, z);
            case ROUND_OAK -> trees.roundOak(x, y, z);
            case OLD_GIANT_OAK -> trees.oldGiantOak(x, y, z);
            case TALL_FOREST_OAK -> trees.tallForestOak(x, y, z);
            case SILVER_BIRCH -> trees.silverBirch(x, y, z);
            case COLUMN_BIRCH -> trees.columnBirch(x, y, z);
            case BIRCH_SPIRE -> trees.birchSpire(x, y, z);
            case LEANING_BIRCH -> trees.leaningBirch(x, y, z);
            case POPLAR -> trees.poplar(x, y, z, season);
            case MAPLE -> trees.maple(x, y, z, season);
            case GREAT_POPLAR -> trees.greatPoplar(x, y, z, season);
            case MOUNTAIN_SPRUCE -> trees.mountainSpruce(x, y, z, false);
            case TIERED_SPRUCE -> trees.tieredSpruce(x, y, z);
            case SMALL_MOUNTAIN_SPRUCE -> trees.mountainSpruce(x, y, z, true);
            case TALL_PINE -> trees.tallPine(x, y, z);
            case JUNGLE_EMERGENT -> trees.jungleEmergent(x, y, z);
            case KAPOK -> trees.kapok(x, y, z);
            case SLENDER_JUNGLE -> trees.slenderJungle(x, y, z);
            case JUNGLE_PARASOL -> trees.jungleIdea(x, y, z, idea < 0 ? random.nextInt(4) : idea % 4);
            case SAVANNA_UMBRELLA -> trees.acacia(x, y, z);
            case FORKED_ACACIA -> trees.forkedAcacia(x, y, z);
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
            case ELDER_CHERRY -> trees.elderCherry(x, y, z);
            case GREAT_MANGROVE -> trees.mangrove(x, y, z);
            case BANYAN_GROVE -> trees.banyanGrove(x, y, z, idea);
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
            case DRAGONBLOOD -> trees.dragonblood(x, y, z);
            case COCONUT_PALM -> trees.coconutPalm(x, y, z);
            case DATE_PALM -> trees.datePalm(x, y, z);
            case RAINBOW_EUCALYPTUS -> trees.rainbowEucalyptus(x, y, z);
            case STRANGLER_FIG -> trees.stranglerFig(x, y, z);
            case GIANT_SEQUOIA -> trees.giantSequoia(x, y, z);
            case REDWOOD -> trees.redwood(x, y, z);
            case BALD_CYPRESS -> trees.baldCypress(x, y, z);
            case WINDSWEPT_HAWTHORN -> trees.windsweptHawthorn(x, y, z);
            case JOSHUA_TREE -> trees.joshuaTree(x, y, z);
            case SNOW_GUM -> trees.snowGum(x, y, z);
            case PENCIL_CYPRESS -> trees.pencilCypress(x, y, z);
            case GREAT_CEDAR -> trees.greatCedar(x, y, z);
            case MONKEY_PUZZLE -> trees.monkeyPuzzle(x, y, z);
            case DEAD_SNAG -> trees.deadSnag(x, y, z);
            case WITCH_TREE -> trees.witchTree(x, y, z);
            case OLIVE_TREE -> trees.oliveTree(x, y, z);
            case FLAME_TREE -> trees.flameTree(x, y, z, idea < 0 ? -1 : idea % 4);
            case TREE_FERN -> trees.treeFern(x, y, z);
            case GIANT_BONSAI -> trees.giantBonsai(x, y, z);
            case MYCELIAL_OAK -> trees.mycelialOak(x, y, z);
            case ORCHARD_OAK -> trees.orchardOak(x, y, z);
            case FLAME_HELIX -> trees.flameHelix(x, y, z);
            case WARPED_BLOOM -> trees.warpedBloom(x, y, z);
            case EMBER_OAK -> trees.emberOak(x, y, z);
            case SOUL_PINE -> trees.soulPine(x, y, z);
            case LANTERN_LILY -> trees.lanternLily(x, y, z);
            case BONE_SPINE -> trees.boneSpine(x, y, z);
            case BONE_SPRAWL -> trees.boneSprawl(x, y, z);
            case HALO_TREE -> trees.haloTree(x, y, z);
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
            case SPREADING_OAK, ROUND_OAK, OLD_GIANT_OAK, TALL_FOREST_OAK, SILVER_BIRCH, COLUMN_BIRCH, BIRCH_SPIRE, LEANING_BIRCH,
                 POPLAR, GREAT_POPLAR, YOUNG_DARK_OAK, GNARLED_DARK_OAK, ANCIENT_DARK_OAK, AZALEA, FLOWERING_AZALEA,
                 WINDSWEPT_HAWTHORN, MAPLE, MYCELIAL_OAK, ORCHARD_OAK, GIANT_BONSAI -> true;
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
            case ANCIENT_DARK_OAK -> shelfMushrooms(between(3, 5), y + 1, y + 12);
            case OLD_GIANT_OAK, YOUNG_DARK_OAK, GNARLED_DARK_OAK, YOUNG_PALE_OAK, HOLLOW_PALE_OAK, ANCIENT_PALE_OAK -> {
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

    /**
     * How a {@link #naturalLimb} grows.
     * @param bend How much the limb's upward angle changes with every block it grows: positive curves it upwards,
     *             negative arches it outwards and over.
     * @param wobble How much the limb randomly wanders as it grows.
     * @param sideChance The chance of a side branch leaving each part of the limb.
     * @param minChildren The fewest limbs it forks into at its end.
     * @param maxChildren The most limbs it forks into at its end.
     * @param shrink The length of each fork, relative to the limb it grows from.
     * @param minLength Limbs shorter than this stop forking.
     * @param clusterR The horizontal radius of the leaf cluster at the end of each limb.
     * @param clusterRy The vertical radius of the leaf cluster at the end of each limb.
     * @param clusterLift How far above the end of the limb its leaf cluster sits.
     * @param roughness How ragged the edges of the leaf clusters are.
     * @param nodeCluster The chance of a smaller leaf cluster where a limb forks, adding volume.
     */
    private record Crown(double bend, double wobble, double sideChance, int minChildren, int maxChildren, double shrink,
                         double minLength, double clusterR, double clusterRy, double clusterLift, double roughness,
                         double nodeCluster) {}

    /**
     * Grows a limb the way a real one grows: thick where it leaves the trunk and tapering towards its tip, curving
     * gradually rather than running straight, with smaller side branches leaving it along the way, alternating sides,
     * and forking at its end. Every tip holds a cluster of leaves.
     * @param yaw The horizontal direction of the limb.
     * @param pitch The upward angle the limb starts at.
     * @param length The length of the limb.
     * @param radius The radius of the limb where it starts, or below 0.55 for a limb a single block wide.
     * @param depth How many more times the limb may branch.
     */
    private void naturalLimb(double x, double y, double z, double yaw, double pitch, double length, double radius,
                             int depth, Crown c) {
        int segments = Math.max(1, (int) Math.round(length / 2.0));
        double segment = length / segments;
        double endRadius = radius * 0.7;
        int side = chance(0.5) ? 1 : -1;
        for (int i = 0; i < segments; i++) {
            yaw += jitter(c.wobble());
            pitch = Math.max(-0.5, Math.min(1.45, pitch + c.bend() * segment + jitter(c.wobble() * 0.4)));
            double nx = x + Math.cos(yaw) * Math.cos(pitch) * segment;
            double ny = y + Math.sin(pitch) * segment;
            double nz = z + Math.sin(yaw) * Math.cos(pitch) * segment;
            double r0 = radius + (endRadius - radius) * i / segments;
            double r1 = radius + (endRadius - radius) * (i + 1) / segments;
            if (r1 >= 0.55) {
                t.thickBranch(x, y, z, nx, ny, nz, r0, r1);
            } else {
                t.branch(x, y, z, nx, ny, nz);
            }
            // Side branches are shorter and thinner than the limb, and lean a little more upwards
            if (depth > 0 && i < segments - 1 && length > c.minLength() * 1.5 && chance(c.sideChance())) {
                naturalLimb(nx, ny, nz, yaw + side * between(0.7, 1.2), pitch + between(0.0, 0.25),
                        length * between(0.35, 0.5), r1 * 0.55, depth - 1, c);
                side = -side;
            }
            x = nx;
            y = ny;
            z = nz;
        }
        if (depth <= 0 || length * c.shrink() < c.minLength()) {
            double size = between(0.85, 1.15);
            t.blob(x, y + c.clusterLift(), z, c.clusterR() * size, c.clusterRy() * size, c.clusterR() * size, c.roughness());
            return;
        }
        if (chance(c.nodeCluster())) {
            t.blob(x, y + c.clusterLift(), z, c.clusterR() * 0.7, c.clusterRy() * 0.75, c.clusterR() * 0.7, c.roughness());
        }
        int children = between(c.minChildren(), c.maxChildren());
        double spread = 0.55;
        for (int k = 0; k < children; k++) {
            double offset = children == 1 ? jitter(0.3) : -spread + 2 * spread * k / (children - 1) + jitter(0.2);
            naturalLimb(x, y, z, yaw + offset, pitch + jitter(0.2), length * c.shrink() * between(0.9, 1.1),
                    endRadius * 0.75, depth - 1, c);
        }
    }

    /**
     * Rounds the radius of a slice of a trunk centred on the middle of a block, so that the slice never has a single
     * block sticking out of the middle of one of its sides.
     */
    private static double smoothRadius(double radius) {
        int n = (int) Math.floor(radius);
        double next = Math.sqrt(n * n + 1);
        return n >= 1 && radius < next ? next + 0.01 : radius;
    }

    /** The leaves of an autumn tree at each stage of the season, from summer green to red. */
    private static final Material[] SEASON_LEAVES = {Material.AZALEA_LEAVES, Material.YELLOW_POPLAR_LEAVES,
            Material.ORANGE_POPLAR_LEAVES, Material.RED_POPLAR_LEAVES};
    private static final String[] SEASON_NAMES = {"green", "yellow", "orange", "red"};

    /**
     * Provides how far autumn has come on the in-game calendar: below 0 from Ignivor to Ardorvor, from 0 to 1 through
     * Solarvor, 1 to 2 through Follivor, 2 to 3 through Strigavor, and above 3 from Faunivor through winter.
     */
    public static double autumnStage() {
        Month month = AranarthUtils.getMonth();
        if (month == null) {
            return -0.6;
        }
        double through = Math.max(0, Math.min(1,
                (AranarthUtils.getDay() - 1) / (double) Math.max(1, DateUtils.getDaysInMonth(month))));
        return switch (month) {
            case SOLARVOR -> through;
            case FOLLIVOR -> 1 + through;
            case STRIGAVOR -> 2 + through;
            case FAUNIVOR, UMBRAVOR, GLACIVOR, FRIGORVOR, OBSCURVOR -> 3.4;
            default -> -0.6;
        };
    }

    /**
     * Picks the colour of an autumn tree's leaves from the calendar: green from Ignivor to Ardorvor, turning yellow
     * through Solarvor, orange through Follivor and red through Strigavor, and staying red until the year ends. Trees grown on
     * the same day are not all the same colour, as some turn sooner than others.
     * @param forced A stage from 0 to 3 to use instead of the calendar, or -1.
     * @return 0 green, 1 yellow, 2 orange or 3 red.
     */
    private int seasonColour(int forced) {
        if (forced >= 0) {
            return Math.min(3, forced);
        }
        return (int) Math.max(0, Math.min(3, Math.round(autumnStage() + r.nextGaussian() * 0.45)));
    }

    // ------------------------------------------------------------
    // Oak
    // ------------------------------------------------------------

    /**
     * #1 Spreading Oak - a small, simple tree: a short single trunk with a few stubby limbs, each ending in a leaf
     * cluster, that together form a modest rounded crown.
     */
    private void spreadingOak(int x, int y, int z) {
        t = new TreeBuilder(world, r, Material.OAK_WOOD, Material.OAK_LEAVES);
        t.setLeafMix(Material.OAK_LEAVES, Material.DARK_OAK_LEAVES, 0.15);
        // No air pockets, as the crown is too small to hide them and they read as missing leaves
        t.setPorosity(0);
        double scale = between(1.0, 1.25);
        double cx = x + 0.5;
        double cz = z + 0.5;
        int trunkHeight = (int) Math.round(between(3.5, 4.5) * scale);
        t.info("scale", scale);
        t.info("trunk", trunkHeight);
        double topX = cx + jitter(0.4);
        double topZ = cz + jitter(0.4);
        t.branch(cx, y, cz, topX, y + trunkHeight, topZ);

        // A dense core around the top of the trunk so the crown reads as one full, bushy mass
        t.blob(topX, y + trunkHeight + 0.5, topZ, 2.5 * scale, 2.0 * scale, 2.5 * scale, 0.2);

        // Limbs do not fork, so each one is just a short arm holding a single leaf cluster
        LimbStyle style = new LimbStyle(0.2, 0.03, 0.6, 0.5, 1, 1, 0.6, 0.7, 99, 2.3, 1.8, 0.5, 0.2, 0);
        int limbs = between(3, 4);
        double start = randomAngle();
        for (int i = 0; i < limbs; i++) {
            double yaw = start + i * Math.PI * 2 / limbs + jitter(0.4);
            int drop = between(0, 1);
            double progress = (double) (trunkHeight - drop) / trunkHeight;
            limb(cx + (topX - cx) * progress, y + trunkHeight - drop, cz + (topZ - cz) * progress,
                    yaw, between(0.6, 0.95), between(1.8, 2.6) * scale, 0, style);
        }
        limb(topX, y + trunkHeight, topZ, randomAngle(), 1.35, between(1.6, 2.3) * scale, 0, style);
    }

    /**
     * Scatters a few small patches of brown and red mushrooms in the shade beneath the tree.
     */
    private void mushroomPatches(double cx, int y, double cz, int count, double minDistance, double maxDistance) {
        for (int i = 0; i < count; i++) {
            double angle = randomAngle();
            double distance = between(minDistance, maxDistance);
            int px = (int) Math.floor(cx + Math.cos(angle) * distance);
            int pz = (int) Math.floor(cz + Math.sin(angle) * distance);
            groundCover(px, y, pz, between(1.5, 2.5), 0.7, chance(0.6) ? Material.BROWN_MUSHROOM : Material.RED_MUSHROOM,
                    true);
        }
    }

    /**
     * #86 Round Oak - a mid-sized oak on a single short trunk, a little taller than the Spreading Oak, whose limbs reach
     * out low and turn gently upwards, all wrapped in a full, rounded crown that starts close to the ground.
     */
    private void roundOak(int x, int y, int z) {
        t = new TreeBuilder(world, r, Material.OAK_WOOD, Material.OAK_LEAVES);
        t.setLeafMix(Material.OAK_LEAVES, Material.DARK_OAK_LEAVES, 0.15);
        t.setPorosity(0.08);
        double cx = x + 0.5;
        double cz = z + 0.5;
        int trunk = between(3, 4);
        double topX = cx + jitter(0.4);
        double topZ = cz + jitter(0.4);
        t.branch(cx, y, cz, topX, y + trunk, topZ);
        surfaceRoots(cx, y, cz, between(2, 3), 1.0, 2.0, 0.6, 1, Material.OAK_WOOD);
        t.info("trunk", trunk);
        // Limbs leave the trunk low down and turn gently upwards towards their ends
        Crown crown = new Crown(0.05, 0.2, 0.3, 2, 2, 0.6, 1.5, 2.4, 1.9, 0.5, 0.3, 0.6);
        int limbs = between(3, 4);
        double start = randomAngle();
        for (int i = 0; i < limbs; i++) {
            naturalLimb(topX, y + trunk - between(1, 2), topZ, start + i * Math.PI * 2 / limbs + jitter(0.35),
                    between(0.25, 0.45), between(3.5, 4.5), 0, 1, crown);
        }
        naturalLimb(topX, y + trunk, topZ, randomAngle(), 1.3, between(3.0, 4.0), 0, 1, crown);
        // A core of leaves that reaches down close to where the limbs start, so the crown is round and full
        t.blob(topX, y + trunk + 2, topZ, 3.4, 2.8, 3.4, 0.3);
        // Every branch in the crown is wrapped in leaves, so no bare wood shows
        coverWood(y + trunk - 1, 1.3);
    }

    /**
     * #2 Old Giant Oak - grown from a 3x3 of saplings. A massive trunk, flared at its base, that tapers as it rises and
     * splits high up into a few huge limbs. They leave the trunk thick, climb, and then arch outwards, branching along the
     * way, under a tall, broad and full crown.
     */
    private void oldGiantOak(int x, int y, int z) {
        t = new TreeBuilder(world, r, Material.OAK_WOOD, Material.OAK_LEAVES);
        t.setLeafMix(Material.OAK_LEAVES, Material.DARK_OAK_LEAVES, 0.2);
        t.setPorosity(0.12);
        double scale = between(1.0, 1.25);
        double cx = x + 1.5;
        double cz = z + 1.5;
        int trunkHeight = (int) Math.round(between(8.0, 10.0) * scale);
        for (int h = 0; h <= trunkHeight; h++) {
            double radius = 1.45 + 0.35 * (1 - (double) h / trunkHeight) + 0.9 * Math.exp(-h / 1.6);
            t.trunk(cx, cz, y + h, y + h, smoothRadius(radius));
        }
        surfaceRoots(cx, y, cz, between(6, 8), 2.0, 3.5 * scale, 2.2, 2, Material.OAK_WOOD);
        t.info("scale", scale);
        t.info("trunk", trunkHeight);

        // Lower limbs reach further and spread wider, higher ones climb more steeply
        Crown crown = new Crown(-0.035, 0.18, 0.5, 2, 3, 0.6, 1.8, 2.9, 2.1, 0.6, 0.35, 0.5);
        int limbs = between(5, 6);
        double start = randomAngle();
        for (int i = 0; i < limbs; i++) {
            double yaw = start + i * Math.PI * 2 / limbs + jitter(0.35);
            double q = r.nextDouble();
            double sy = y + trunkHeight * (0.6 + 0.4 * q);
            naturalLimb(cx + Math.cos(yaw) * 1.2, sy, cz + Math.sin(yaw) * 1.2, yaw, 0.55 + 0.3 * q,
                    (9.0 - 1.5 * q) * scale * between(0.95, 1.05), 1.1, 2, crown);
        }
        naturalLimb(cx, y + trunkHeight, cz, randomAngle(), 1.3, between(6.0, 7.5) * scale, 1.0, 2, crown);
        // Fills the middle of the crown so it reads as one full mass
        t.blob(cx, y + trunkHeight + 6 * scale, cz, 5.0 * scale, 3.2 * scale, 5.0 * scale, 0.4);
    }

    /**
     * #3 Tall Forest Oak - grown from a 2x2 of saplings. An oak grown up among other trees: a tall, straight 2x2 trunk,
     * bare for over half its height, that forks into two or three steep leaders climbing into a tall, oval crown, with a
     * couple of shorter limbs just below the fork.
     */
    private void tallForestOak(int x, int y, int z) {
        t = new TreeBuilder(world, r, Material.OAK_WOOD, Material.OAK_LEAVES);
        t.setLeafMix(Material.OAK_LEAVES, Material.BIRCH_LEAVES, 0.12);
        // Pockets of air through the crown, so it is full but never one solid mass
        t.setPorosity(0.22);
        double scale = between(0.9, 1.15);
        double cx = x + 1;
        double cz = z + 1;
        int height = (int) Math.round(between(16.0, 20.0) * scale);
        int forkY = y + (int) (height * between(0.55, 0.62));
        int crownTop = y + height;
        t.trunk(cx, cz, y, forkY, 1.0);
        surfaceRoots(cx, y, cz, between(4, 5), 1.5, 2.8, 1.2, 1, Material.OAK_WOOD);
        t.info("scale", scale);
        t.info("height", height);

        // Big leaf clusters at every tip and fork, so the crown is full
        Crown crown = new Crown(-0.03, 0.18, 0.55, 2, 2, 0.6, 1.5, 3.1, 2.4, 0.5, 0.35, 0.85);
        // A couple of shorter limbs leave the trunk just below the fork
        int lower = between(2, 3);
        double yaw = randomAngle();
        for (int i = 0; i < lower; i++) {
            yaw += GOLDEN_ANGLE + jitter(0.3);
            naturalLimb(cx + Math.cos(yaw) * 0.9, forkY - between(1, 3), cz + Math.sin(yaw) * 0.9, yaw, between(0.65, 0.85),
                    between(4.0, 5.5) * scale, 0.6, 2, crown);
        }
        // The trunk forks into steep leaders that climb into the crown
        int leaders = between(2, 3);
        double leaderStart = randomAngle();
        for (int i = 0; i < leaders; i++) {
            double angle = leaderStart + i * Math.PI * 2 / leaders + jitter(0.3);
            naturalLimb(cx + Math.cos(angle) * 0.5, forkY, cz + Math.sin(angle) * 0.5, angle, between(1.15, 1.3),
                    (crownTop - forkY) * between(0.85, 1.0), 0.8, 2, crown);
        }
        // A core of leaves fills the middle of the crown
        t.blob(cx, forkY + (crownTop - forkY) * 0.55, cz, 4.0 * scale, (crownTop - forkY) * 0.55, 4.0 * scale, 0.35);
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
        // A 2x2 trunk low down, then a short stretch where it narrows, then a single block wide trunk that follows the
        // curve in short steps, so it bends smoothly instead of in two long straight lines
        for (int h = 0; h < wide; h++) {
            double[] p = birchPath(x, z, h, height, lean, angle, 1.8);
            placeSquare((int) Math.round(p[0]) - 1, y + h, (int) Math.round(p[1]) - 1);
        }
        double[] previous = birchPath(x, z, wide - 1, height, lean, angle, 1.8);
        int previousY = y + wide - 1;
        int step = wide + 1;
        while (true) {
            double[] p = birchPath(x, z, step, height, lean, angle, 1.8);
            if (step <= wide + 2) {
                t.thickBranch(previous[0], previousY, previous[1], p[0], y + step, p[1], 0.8, 0.7);
            } else {
                t.branch(previous[0], previousY, previous[1], p[0], y + step, p[1]);
            }
            previous = p;
            previousY = y + step;
            if (step >= height) {
                break;
            }
            step = Math.min(height, step + 2);
        }
        // Roots brace the trunk on the side away from the lean
        surfaceRoots(x + 1 - Math.cos(angle) * 0.5, y, z + 1 - Math.sin(angle) * 0.5, between(3, 4), 1.5, 2.5, 1.2, 1,
                Material.BIRCH_WOOD);
        // Now and then a small branch grows out of the lower trunk, holding a little tuft of leaves
        int twigs = between(1, 3);
        for (int i = 0; i < twigs; i++) {
            int h = between(4, wide - 1);
            double[] p = birchPath(x, z, h, height, lean, angle, 1.8);
            double yaw = randomAngle();
            double sx = p[0] + Math.cos(yaw);
            double sz = p[1] + Math.sin(yaw);
            double length = between(1.8, 2.8);
            double ex = sx + Math.cos(yaw) * length;
            double ey = y + h + between(1.0, 2.0);
            double ez = sz + Math.sin(yaw) * length;
            t.branch(sx, y + h, sz, ex, ey, ez);
            t.blob(ex, ey + 0.5, ez, between(1.3, 1.7), 1.0, between(1.3, 1.7), 0.3);
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
     * Starts a poplar, with dark poplar wood and leaves of a single colour picked from the season: green in spring and
     * summer, then yellow, orange and red as autumn goes on.
     * @param season A stage of autumn from 0 to 3 to use instead of the calendar, or -1.
     */
    private void startPoplar(int season) {
        int colour = seasonColour(season);
        t = new TreeBuilder(world, r, Material.POPLAR_WOOD, SEASON_LEAVES[colour]);
        if (colour == 0) {
            t.setLeafMix(Material.AZALEA_LEAVES, Material.OAK_LEAVES, 0.3);
        }
        t.setPorosity(0.2);
        t.info("leaves", SEASON_NAMES[colour]);
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
     * #5 Poplar - an autumn tree whose leaves follow the season. A dark trunk, bare for its lower part, that splits into several limbs which keep
     * forking into a big, bushy and rounded crown of leaves in a single colour, with fallen leaves beneath and the odd
     * shelf mushroom on the trunk.
     */
    private void poplar(int x, int y, int z, int season) {
        startPoplar(season);
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
    private void greatPoplar(int x, int y, int z, int season) {
        startPoplar(season);
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

        // Thick limbs leave the top of the trunk steeply and curve gradually outwards as they grow, branching along the
        // way, so none of them runs out along a straight diagonal. Their ends carry big masses of leaves
        Crown crown = new Crown(-0.06, 0.25, 0.45, 2, 3, 0.62, 1.8, 3.8, 3.0, 0.8, 0.35, 0.4);
        int limbs = between(5, 7);
        double start = randomAngle();
        for (int i = 0; i < limbs; i++) {
            double yaw = start + i * Math.PI * 2 / limbs + jitter(0.3);
            double sy = y + trunkHeight - between(0, 2);
            naturalLimb(cx + Math.cos(yaw) * 0.6, sy, cz + Math.sin(yaw) * 0.6, yaw, between(1.0, 1.2),
                    between(8.5, 10.5) * scale, 0.9, 2, crown);
        }
        // A thick leader up the middle and a core of leaves fill in the centre of the crown
        naturalLimb(cx, y + trunkHeight, cz, randomAngle(), 1.4, between(6.0, 7.5) * scale, 0.9, 2, crown);
        t.blob(cx, y + trunkHeight + 6.5 * scale, cz, 6.0 * scale, 4.5 * scale, 6.0 * scale, 0.4);
        // Plenty of volume up top, so the crown is rounded and full rather than thin above the limbs
        t.blob(cx + jitter(1), y + trunkHeight + 11 * scale, cz + jitter(1), 6.5 * scale, 4.0 * scale, 6.5 * scale, 0.4);
        if (chance(0.6)) {
            shelfMushrooms(between(1, 3), y + 1, y + trunkHeight - 1);
        }
    }

    /**
     * #60 Maple - an autumn tree grown from a single poplar sapling. A grey-brown trunk that splits into a few steep limbs
     * under a dense, egg-shaped crown, taller than it is wide, coloured by the season with a touch of the next colour
     * mixed in.
     */
    private void maple(int x, int y, int z, int season) {
        // The colour of the tree follows the season, with the colour next to it mixed in in patches
        int colour = seasonColour(season);
        Material main = SEASON_LEAVES[colour];
        Material accent = switch (colour) {
            case 0 -> Material.OAK_LEAVES;
            case 2 -> chance(0.5) ? Material.RED_POPLAR_LEAVES : Material.YELLOW_POPLAR_LEAVES;
            default -> Material.ORANGE_POPLAR_LEAVES;
        };
        // The bark is compared between the two greyest woods
        Material bark = chance(0.5) ? Material.OAK_WOOD : Material.ACACIA_WOOD;
        t = new TreeBuilder(world, r, bark, main);
        t.setLeafMix(main, accent, 0.18);
        t.setPorosity(0.18);
        t.info("leaves", main.name().toLowerCase().replace("_poplar_leaves", "").replace("_leaves", ""));
        t.info("bark", bark.name().toLowerCase().replace("_wood", ""));
        double scale = between(0.9, 1.2);
        double cx = x + 0.5;
        double cz = z + 0.5;
        int trunkHeight = between(4, 6);
        double topX = cx + jitter(0.5);
        double topZ = cz + jitter(0.5);
        t.branch(cx, y, cz, topX, y + trunkHeight, topZ);
        surfaceRoots(cx, y, cz, 3, 1.0, 2.0, 0.6, 1, bark);
        t.info("scale", scale);

        // Several limbs climb steeply out of the fork and keep forking, each fork ending in its own clump of leaves, so
        // the crown is tall and oval and built from many uneven clumps rather than a few smooth masses
        LimbStyle style = new LimbStyle(0.3, 0.02, 1.0, 0.55, 2, 2, 0.58, 0.72, 1.4, 2.3, 2.0, 0.4, 0.5, 0.6);
        int limbs = between(4, 6);
        double start = randomAngle();
        for (int i = 0; i < limbs; i++) {
            limb(topX, y + trunkHeight - between(0, 2), topZ, start + i * Math.PI * 2 / limbs + jitter(0.4),
                    between(0.8, 1.15), between(4.0, 5.5) * scale, 2, style);
        }
        t.branch(topX, y + trunkHeight, topZ, topX + jitter(0.6), y + trunkHeight + 5 * scale, topZ + jitter(0.6));
        // A smaller core fills the middle so the crown has no hollow, without smoothing its outline
        t.blob(topX, y + trunkHeight + 4.5 * scale, topZ, 2.6 * scale, 3.4 * scale, 2.6 * scale, 0.45);
        coverWood(y + trunkHeight + 1, 1.4);
        // Leaves are thinned at random, breaking up any smooth, rounded edges
        fluff(0.15);
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
     * #6 Mountain Spruce and #7 Small Mountain Spruce - an irregular, uneven spruce whose layers of drooping sprays vary in
     * length, leaning slightly away from the wind. The Mountain Spruce grows from a 2x2 of saplings on a 2x2 trunk, the
     * small one from a single sapling and noticeably shorter. Sizes vary greatly.
     */
    private void mountainSpruce(int x, int y, int z, boolean small) {
        t = new TreeBuilder(world, r, Material.SPRUCE_WOOD, Material.SPRUCE_LEAVES);
        double cx = small ? x + 0.5 : x + 1;
        double cz = small ? z + 0.5 : z + 1;
        int height = small ? between(11, 17) : between(24, 34);
        int wideHeight = small ? height : (int) (height * between(0.65, 0.8));
        double upperX = cx;
        double upperZ = cz;
        if (small) {
            t.trunk(cx, cz, y, y + height, 0.5);
            surfaceRoots(cx, y, cz, between(2, 3), 1.0, 2.0, 0.6, 1, Material.SPRUCE_WOOD);
        } else {
            t.trunk(cx, cz, y, y + wideHeight, 1.0);
            // Above the 2x2 the trunk continues from one of its corners
            upperX = x + r.nextInt(2) + 0.5;
            upperZ = z + r.nextInt(2) + 0.5;
            t.trunk(upperX, upperZ, y + wideHeight, y + height, 0.5);
            surfaceRoots(cx, y, cz, between(4, 6), 1.5, 3.0, 1.2, 2, Material.SPRUCE_WOOD);
        }
        t.info("height", height);

        // The lowest sprays start well above the ground, so the tree never sits on a skirt of needles
        int crownBase = y + (small ? between(3, 5) : between(5, 8));
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
            double sx = h <= y + wideHeight ? cx : upperX;
            double sz = h <= y + wideHeight ? cz : upperZ;
            for (int i = 0; i < sprays; i++) {
                double yaw = start + i * Math.PI * 2 / sprays + jitter(0.45);
                double sprayLength = length * between(0.7, 1.15) * (1 + windBias * Math.cos(yaw - windYaw));
                spray(sx, h, sz, yaw, sprayLength, between(0.25, 0.4));
            }
            t.disc(sx, h + 1, sz, small ? 1.3 : 1.8, 0.3);
            h += between(2, 3);
        }
        spruceTop(upperX + (small ? 0 : jitter(0.5)), y + height, upperZ + (small ? 0 : jitter(0.5)));
        if (!small && chance(0.2)) {
            berryPatch(cx, y, cz, 5, 9);
        }
    }

    /**
     * #87 Tiered Spruce - grown from a 2x2 of saplings. A tall spruce with the same needles and drooping sprays as the
     * Mountain Spruce, so the two blend into one forest, but grown in distinct, uneven tiers with the trunk showing
     * between them, shrinking upwards into a dense, narrow spire.
     */
    private void tieredSpruce(int x, int y, int z) {
        t = new TreeBuilder(world, r, Material.SPRUCE_WOOD, Material.SPRUCE_LEAVES);
        double cx = x + 1;
        double cz = z + 1;
        int height = between(24, 32);
        int wideHeight = (int) (height * between(0.6, 0.7));
        t.trunk(cx, cz, y, y + wideHeight, 1.0);
        double upperX = x + r.nextInt(2) + 0.5;
        double upperZ = z + r.nextInt(2) + 0.5;
        t.trunk(upperX, upperZ, y + wideHeight, y + height, 0.5);
        surfaceRoots(cx, y, cz, between(4, 6), 1.5, 3.0, 1.2, 2, Material.SPRUCE_WOOD);
        t.info("height", height);

        int crownBase = y + between(6, 8);
        int spireStart = y + (int) (height * between(0.72, 0.78));
        double maxLength = height * between(0.16, 0.2) + 1.0;
        int h = crownBase;
        int tiers = 0;
        while (h < spireStart) {
            double progress = (double) (h - crownBase) / (y + height - crownBase);
            double length = maxLength * Math.pow(1 - progress, 0.85) * between(0.85, 1.1) + 0.8;
            boolean wide = h <= y + wideHeight;
            double sx = wide ? cx : upperX;
            double sz = wide ? cz : upperZ;
            int sprays = between(5, 7);
            double start = randomAngle();
            for (int i = 0; i < sprays; i++) {
                double yaw = start + i * Math.PI * 2 / sprays + jitter(0.4);
                spray(sx, h, sz, yaw, length * between(0.75, 1.2), between(0.15, 0.3));
            }
            // A second, shorter layer just above makes each tier a thick, full shelf
            int inner = between(4, 5);
            for (int i = 0; i < inner; i++) {
                spray(sx, h + 1, sz, randomAngle(), length * between(0.5, 0.7), between(0.2, 0.35));
            }
            t.disc(sx, h + 2, sz, 1.6, 0.3);
            tiers++;
            h += between(3, 4);
        }
        // The spire: short sprays packed close together up to the top
        while (h < y + height - 1) {
            double progress = (double) (h - crownBase) / (y + height - crownBase);
            double length = maxLength * Math.pow(1 - progress, 0.85) + 0.8;
            int sprays = between(4, 5);
            double start = randomAngle();
            for (int i = 0; i < sprays; i++) {
                spray(upperX, h, upperZ, start + i * Math.PI * 2 / sprays + jitter(0.4), length * between(0.8, 1.1),
                        between(0.25, 0.4));
            }
            h += between(1, 2);
        }
        spruceTop(upperX, y + height, upperZ);
        t.info("tiers", tiers);
    }

    /**
     * #8 Tall Pine - grown from a single sapling, about the size of a Small Mountain Spruce. A gently swaying trunk, bare
     * low down but for the odd broken stub, under whorls of branches that reach out almost level and turn up a
     * little at their ends, each holding flat pads of needles, so the crown is a loose, rounded dome.
     */
    private void tallPine(int x, int y, int z) {
        t = new TreeBuilder(world, r, Material.SPRUCE_WOOD, Material.SPRUCE_LEAVES);
        t.setPorosity(0.1);
        double cx = x + 0.5;
        double cz = z + 0.5;
        int height = between(12, 18);
        t.info("height", height);
        List<double[]> path = bentTrunk(cx, y, cz, height, 3, 0.4, null);
        surfaceRoots(cx, y, cz, between(3, 4), 1.2, 2.2, 0.6, 1, Material.SPRUCE_WOOD);
        double[] top = path.get(path.size() - 1);
        int crownStart = y + (int) (height * between(0.4, 0.5));
        int stubs = between(0, 2);
        for (int i = 0; i < stubs; i++) {
            double[] at = pathAt(path, y + between(3, Math.max(4, crownStart - y - 2)));
            BlockFace face = HORIZONTAL[r.nextInt(4)];
            t.wood((int) Math.floor(at[0]) + face.getModX(), (int) Math.floor(at[1]), (int) Math.floor(at[2]) + face.getModZ());
        }
        Crown pad = new Crown(0.05, 0.25, 0.25, 1, 2, 0.6, 1.4, 2.3, 1.0, 0.5, 0.3, 0.35);
        int h = crownStart;
        while (h < y + height - 2) {
            double progress = (double) (h - crownStart) / (y + height - crownStart);
            double length = (3.6 - 1.8 * progress) * between(0.85, 1.15);
            int count = between(2, 4);
            double start = randomAngle();
            double[] at = pathAt(path, h);
            for (int i = 0; i < count; i++) {
                naturalLimb(at[0], h, at[2], start + i * Math.PI * 2 / count + jitter(0.5), between(-0.05, 0.2), length, 0,
                        1, pad);
            }
            h += between(2, 3);
        }
        t.blob(top[0], top[1] + 0.5, top[2], 2.4, 1.4, 2.4, 0.3);
    }

    // ------------------------------------------------------------
    // Jungle
    // ------------------------------------------------------------

    /**
     * #9 Jungle Emergent - a towering 2x2 trunk braced by curved buttress roots that climb its base and dig deep into the
     * ground, with side branches along the trunk, a broad umbrella canopy of arching limbs, vines and cocoa.
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

        // Buttress roots climb a few blocks up the trunk, flare out in a curve and dig deep into the ground
        int buttresses = between(4, 5);
        double rootStart = randomAngle();
        for (int i = 0; i < buttresses; i++) {
            double angle = rootStart + i * Math.PI * 2 / buttresses + jitter(0.3);
            double ox = Math.cos(angle);
            double oz = Math.sin(angle);
            double rise = between(3.0, 5.0);
            double length = between(3.5, 5.0);
            double mx = cx + ox * (1.0 + length * 0.45);
            double mz = cz + oz * (1.0 + length * 0.45);
            double my = y + rise * 0.3;
            double ex = cx + ox * (1.0 + length);
            double ez = cz + oz * (1.0 + length);
            t.thickRoot(cx + ox * 0.9, y + rise, cz + oz * 0.9, mx, my, mz, 0.75, 0.55, Material.JUNGLE_WOOD);
            t.thickRoot(mx, my, mz, ex, y - 1, ez, 0.55, 0.35, Material.JUNGLE_WOOD);
            t.rootLine(ex, y - 1, ez, ex + ox, y - between(2.5, 4.0), ez + oz, Material.JUNGLE_WOOD, false);
        }

        // Side branches leave the upper trunk, rising and then levelling out, each with a few leafy forks
        Crown side = new Crown(-0.04, 0.2, 0.35, 2, 2, 0.6, 1.4, 2.5, 1.6, 0.5, 0.35, 0.35);
        int sideBranches = between(3, 5);
        double yaw = randomAngle();
        for (int i = 0; i < sideBranches; i++) {
            yaw += GOLDEN_ANGLE + jitter(0.3);
            double h = y + height * between(0.45, 0.8);
            naturalLimb(cx + Math.cos(yaw), h, cz + Math.sin(yaw), yaw, between(0.6, 0.85), between(3.5, 5.0) * scale, 0,
                    1, side);
        }

        // The canopy: limbs climb out of the top of the trunk and arch outwards into a broad umbrella
        Crown canopy = new Crown(-0.07, 0.22, 0.45, 2, 3, 0.6, 1.6, 3.0, 1.8, 0.5, 0.35, 0.4);
        int limbs = between(5, 6);
        double start = randomAngle();
        for (int i = 0; i < limbs; i++) {
            double angle = start + i * Math.PI * 2 / limbs + jitter(0.3);
            naturalLimb(cx + Math.cos(angle) * 0.6, y + height - between(0, 2), cz + Math.sin(angle) * 0.6, angle,
                    between(0.85, 1.05), between(6.5, 8.5) * scale, 0.75, 2, canopy);
        }
        t.blob(cx, y + height + 2, cz, 4.0, 2.0, 4.0, 0.35);

        trunkVines(x, z, 2, y + 2, y + height - 3, 0.3);
        cocoaPods(between(10, 16), y + 2);
        hangingVines(0.1, 6, y + (int) (height * 0.45));
        mossOnBranches(y + 4, 0.14);
    }

    /**
     * #10 Kapok - grown from a 5x5 of saplings, towering far above every other jungle tree. A massive trunk that swells smoothly out into buttress fins at its base,
     * ending well below the canopy. Thick limbs sweep up and out into a wide canopy that slopes down at its edges, with
     * smaller branches both up in the canopy and lower down the trunk, all draped in vines.
     */
    private void kapok(int x, int y, int z) {
        t = new TreeBuilder(world, r, Material.JUNGLE_WOOD, Material.JUNGLE_LEAVES);
        t.setLeafMix(Material.JUNGLE_LEAVES, Material.OAK_LEAVES, 0.12);
        t.setPorosity(0.22);
        double scale = between(0.95, 1.15);
        // A 5x5 tree, so it stands far larger than the 2x2 Jungle Emergent
        double k = between(1.6, 1.8);
        double cx = x + 2.5;
        double cz = z + 2.5;
        int height = (int) Math.round(between(25.0, 31.0) * scale * k);
        // The trunk is pulled down below the canopy, which stays at the same height
        int drop = (int) Math.round(between(7, 11) * k);
        int trunkTop = y + height - drop;
        t.info("scale", scale);
        t.info("height", height);
        t.info("drop", drop);

        // The trunk narrows smoothly from a wide, uneven base, rather than in sudden steps
        int trunkHeight = trunkTop - y;
        for (int h = 0; h <= trunkHeight; h++) {
            double p = (double) h / trunkHeight;
            double radius = (1.7 + 0.4 * (1 - p) + 1.2 * Math.exp(-h / (4.0 * k))) * 1.35;
            t.trunk(cx, cz, y + h, y + h, smoothRadius(radius));
        }

        // Buttress fins grow out of the trunk well above the ground and sweep down in a smooth curve, thicker where they
        // join the trunk, bending a little as they go and digging into the ground at their ends
        int fins = between(5, 7);
        double start = randomAngle();
        for (int i = 0; i < fins; i++) {
            double angle = start + i * Math.PI * 2 / fins + jitter(0.25);
            double curve = jitter(0.05);
            int finLength = (int) Math.round(between(6, 8) * k);
            int finHeight = (int) Math.round(between(5, 7) * k);
            double finAngle = angle;
            for (double d = 2.5; d <= finLength; d += 0.5) {
                finAngle += curve * 0.5;
                double u = (d - 2.5) / (finLength - 2.5);
                int top = (int) Math.round(finHeight * Math.pow(1 - u, 1.5));
                double fx = cx + Math.cos(finAngle) * d;
                double fz = cz + Math.sin(finAngle) * d;
                for (int fy = y - 1; fy <= y + top - 1; fy++) {
                    t.root((int) Math.floor(fx), fy, (int) Math.floor(fz), Material.JUNGLE_WOOD, Axis.Y, false);
                    if (u < 0.45) {
                        t.root((int) Math.floor(fx - Math.sin(finAngle) * 0.7), fy, (int) Math.floor(fz + Math.cos(finAngle) * 0.7),
                                Material.JUNGLE_WOOD, Axis.Y, false);
                        t.root((int) Math.floor(fx + Math.sin(finAngle) * 0.7), fy, (int) Math.floor(fz - Math.cos(finAngle) * 0.7),
                                Material.JUNGLE_WOOD, Axis.Y, false);
                    }
                }
            }
            double ex = cx + Math.cos(finAngle) * finLength;
            double ez = cz + Math.sin(finAngle) * finLength;
            t.rootLine(ex, y - 1, ez, ex + Math.cos(finAngle) * 2, y - between(3, 5), ez + Math.sin(finAngle) * 2,
                    Material.JUNGLE_WOOD, false);
        }

        // Smaller branches lower down the trunk leave its side climbing, then level out, each about the same modest length
        Crown low = new Crown(-0.05, 0.2, 0.3, 2, 2, 0.6, 1.4, 2.3, 1.4, 0.4, 0.35, 0.3);
        int lowBranches = between(3, 5);
        double lowYaw = randomAngle();
        for (int i = 0; i < lowBranches; i++) {
            lowYaw += GOLDEN_ANGLE + jitter(0.3);
            double h = y + trunkHeight * between(0.5, 0.8);
            naturalLimb(cx + Math.cos(lowYaw) * 2.4, h, cz + Math.sin(lowYaw) * 2.4, lowYaw, between(0.6, 0.85),
                    between(4.0, 5.5) * k * 0.8, 0.75, 2, low);
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
            double reach = (2.0 * k + drop * between(0.4, 0.6)) * (0.55 + 0.45 * level);
            double rise = (y + height - 1 - trunkTop) * level + between(0, 1);
            double thickness = between(1.2, 1.7) * 1.3;
            double armAngle = angle + jitter(0.15);
            double sx = cx + Math.cos(angle) * 1.6;
            double sy = trunkTop - between(1, 3);
            double sz = cz + Math.sin(angle) * 1.6;
            double px = sx;
            double py = sy;
            double pz = sz;
            for (int part = 1; part <= 4; part++) {
                double p = part / 4.0;
                double out = reach * (1 - Math.cos(p * Math.PI / 2));
                double nx = sx + Math.cos(armAngle) * out;
                double ny = sy + rise * Math.sin(p * Math.PI / 2);
                double nz = sz + Math.sin(armAngle) * out;
                t.thickBranch(px, py, pz, nx, ny, nz, thickness * (1 - 0.1 * (part - 1)), thickness * (1 - 0.1 * part));
                px = nx;
                py = ny;
                pz = nz;
            }
            // The limb stays thick for the first part of its spread, then splits into smaller limbs. It splits early
            // and keeps rising slightly, so that no limb runs out flat and straight for a long way
            double length = between(6.5, 8.5) * scale * k * (0.7 + 0.3 * level);
            double pitch = between(0.25, 0.4) + 0.15 * (1 - level);
            double thickLength = length * 0.3;
            double ex = px + Math.cos(angle) * Math.cos(pitch) * thickLength;
            double ey = py + Math.sin(pitch) * thickLength;
            double ez = pz + Math.sin(angle) * Math.cos(pitch) * thickLength;
            t.thickBranch(px, py, pz, ex, ey, ez, thickness * 0.65, 1.0);
            thickLimb(ex, ey, ez, angle + jitter(0.2), pitch, length * 0.55, 3, canopy, 0.8);
            // Smaller branches rising out of the top of the canopy
            for (int j = 0; j < between(2, 3); j++) {
                limb(px, py, pz, angle + jitter(0.8), between(0.6, 0.9), between(3.0, 4.5) * k * 0.8, 1, upper);
            }
        }
        // A stout leader keeps the middle of the canopy filled, rounding off its top
        double leaderX = cx + jitter(0.6);
        double leaderZ = cz + jitter(0.6);
        t.thickBranch(cx, trunkTop, cz, leaderX, y + height, leaderZ, 2.1, 1.1);
        // The top of the canopy is rounded, narrowing gradually rather than ending in a flat layer
        t.blob(leaderX, y + height + 1, leaderZ, 4.5 * k, 3.2 * k * 0.8, 4.5 * k, 0.35);
        t.blob(leaderX + jitter(1.0), y + height + 3.5 * k, leaderZ + jitter(1.0), 2.8 * k, 1.8 * k * 0.8, 2.8 * k, 0.35);

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

    /**
     * #88 Second 1x1 jungle tree - four ideas being compared, one per sample, each meant to blend in among the other
     * jungle trees: 0 Understory Tree, 1 Liana Tree, 2 Buttressed Jungle and 3 Layered Jungle. All are draped in vines and
     * cocoa like the rest of the jungle.
     */
    private void jungleIdea(int x, int y, int z, int idea) {
        t = new TreeBuilder(world, r, Material.JUNGLE_WOOD, Material.JUNGLE_LEAVES);
        t.setLeafMix(Material.JUNGLE_LEAVES, Material.OAK_LEAVES, 0.12);
        t.setPorosity(0.15);
        double cx = x + 0.5;
        double cz = z + 0.5;
        int height = switch (idea) {
            case 0 -> jungleUnderstory(cx, y, cz);
            case 1 -> jungleLiana(cx, y, cz);
            case 2 -> jungleButtressed(cx, y, cz);
            default -> jungleLayered(cx, y, cz);
        };
        String[] names = {"Understory Tree", "Liana Tree", "Buttressed Jungle", "Layered Jungle"};
        t.info("idea", names[idea].replace(' ', '_'));
        t.info("height", height);
        trunkVines(x, z, 1, y + 1, y + height - 2, idea == 1 ? 0.6 : 0.25);
        cocoaPods(between(4, 8), y + 2);
        hangingVines(idea == 2 ? 0.18 : 0.12, idea == 2 ? 7 : 5, y + height / 2);
        mossOnBranches(y + 3, 0.14);
    }

    /**
     * Idea A, Understory Tree. A short tree of the forest floor, 6 to 9 tall, on a slightly leaning trunk under a dense,
     * rounded crown that reaches low, like the smaller trees beneath the jungle canopy.
     * @return The height of the tree.
     */
    private int jungleUnderstory(double cx, int y, double cz) {
        int height = between(6, 9);
        double topX = cx + jitter(0.7);
        double topZ = cz + jitter(0.7);
        t.branch(cx, y, cz, topX, y + height - 2, topZ);
        surfaceRoots(cx, y, cz, between(2, 3), 1.0, 2.0, 0.6, 2, Material.JUNGLE_WOOD);
        Crown crown = new Crown(-0.02, 0.2, 0.3, 2, 2, 0.6, 1.4, 2.4, 1.8, 0.4, 0.35, 0.5);
        int limbs = between(3, 4);
        double start = randomAngle();
        for (int i = 0; i < limbs; i++) {
            naturalLimb(topX, y + height - 2 - between(0, 1), topZ, start + i * Math.PI * 2 / limbs + jitter(0.35),
                    between(0.5, 0.8), between(3.0, 4.0), 0, 1, crown);
        }
        t.blob(topX, y + height, topZ, 3.0, 2.2, 3.0, 0.3);
        return height;
    }

    /**
     * Idea B, Liana Tree. A crooked trunk 10 to 14 tall, wound about with vines, holding irregular clumps of leaves at
     * different heights, like a tree half swallowed by the jungle.
     * @return The height of the tree.
     */
    private int jungleLiana(double cx, int y, double cz) {
        int height = between(10, 14);
        List<double[]> path = bentTrunk(cx, y, cz, height, 4, 1.0, null);
        surfaceRoots(cx, y, cz, between(3, 4), 1.2, 2.2, 0.6, 2, Material.JUNGLE_WOOD);
        double[] top = path.get(path.size() - 1);
        Crown clump = new Crown(-0.02, 0.25, 0.2, 1, 2, 0.6, 1.4, 2.2, 1.5, 0.4, 0.4, 0.4);
        int clumps = between(3, 5);
        double yaw = randomAngle();
        for (int i = 0; i < clumps; i++) {
            yaw += GOLDEN_ANGLE + jitter(0.4);
            double[] at = pathAt(path, y + height * between(0.45, 0.9));
            naturalLimb(at[0], at[1], at[2], yaw, between(0.4, 0.7), between(2.5, 4.0), 0, 1, clump);
        }
        t.blob(top[0], top[1] + 1, top[2], 2.6, 1.8, 2.6, 0.4);
        // Vines climb all over its wood
        woodVines(y + 1, 0.3);
        return height;
    }

    /**
     * Idea C, Buttressed Jungle. A small version of the Jungle Emergent: a straight trunk 12 to 16 tall braced by small
     * buttress roots, bare up to an oval crown of arching limbs, with long vines hanging from it.
     * @return The height of the tree.
     */
    private int jungleButtressed(double cx, int y, double cz) {
        int height = between(12, 16);
        t.trunk(cx, cz, y, y + height, 0.5);
        int roots = between(3, 4);
        double rootStart = randomAngle();
        for (int i = 0; i < roots; i++) {
            double angle = rootStart + i * Math.PI * 2 / roots + jitter(0.3);
            double ox = Math.cos(angle);
            double oz = Math.sin(angle);
            double reach = between(2.0, 3.0);
            t.thickRoot(cx + ox * 0.6, y + between(1.5, 2.5), cz + oz * 0.6, cx + ox * reach, y - 1, cz + oz * reach, 0.5, 0.4,
                    Material.JUNGLE_WOOD);
            t.rootLine(cx + ox * reach, y - 1, cz + oz * reach, cx + ox * (reach + 1), y - between(2.0, 3.0),
                    cz + oz * (reach + 1), Material.JUNGLE_WOOD, false);
        }
        Crown crown = new Crown(-0.05, 0.2, 0.35, 2, 2, 0.6, 1.4, 2.6, 1.6, 0.5, 0.35, 0.4);
        int limbs = between(4, 5);
        double start = randomAngle();
        for (int i = 0; i < limbs; i++) {
            naturalLimb(cx, y + height - between(0, 2), cz, start + i * Math.PI * 2 / limbs + jitter(0.3), between(0.75, 0.95),
                    between(4.0, 5.5), 0, 1, crown);
        }
        t.blob(cx, y + height + 2, cz, 3.0, 1.8, 3.0, 0.35);
        return height;
    }

    /**
     * Idea D, Layered Jungle. A straight trunk 12 to 16 tall carrying two or three flat layers of leaves on near-level
     * branches, each layer smaller than the one below, like the stacked crowns of tall jungle trees seen from below.
     * @return The height of the tree.
     */
    private int jungleLayered(double cx, int y, double cz) {
        int height = between(12, 16);
        t.trunk(cx, cz, y, y + height, 0.5);
        surfaceRoots(cx, y, cz, between(3, 4), 1.2, 2.2, 0.6, 2, Material.JUNGLE_WOOD);
        Crown pad = new Crown(0.03, 0.2, 0.0, 1, 1, 0.6, 99, 2.4, 0.9, 0.4, 0.3, 0);
        int layers = between(2, 3);
        for (int layer = 0; layer < layers; layer++) {
            double q = layers == 1 ? 1 : (double) layer / (layers - 1);
            int ly = y + (int) Math.round(height * (0.5 + 0.4 * q));
            double length = 4.2 - 1.8 * q;
            int branches = between(3, 4);
            double start = randomAngle();
            for (int i = 0; i < branches; i++) {
                naturalLimb(cx, ly, cz, start + i * Math.PI * 2 / branches + jitter(0.35), between(0.1, 0.25), length, 0, 0,
                        pad);
            }
            t.disc(cx, ly + 1, cz, 1.6, 0.3);
        }
        t.blob(cx, y + height + 1, cz, 2.2, 1.2, 2.2, 0.3);
        return height;
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

    /**
     * #89 Forked Acacia - grown from a single sapling. A trunk that forks into two or three slender stems, which climb
     * almost straight up beside each other before leaning apart like a vase, each ending in its own flat pad of leaves at
     * a different height, so the crown is a loose stack of flat layers rather than one umbrella.
     */
    private void forkedAcacia(int x, int y, int z) {
        t = new TreeBuilder(world, r, Material.ACACIA_WOOD, Material.ACACIA_LEAVES);
        t.setPorosity(0.08);
        double cx = x + 0.5;
        double cz = z + 0.5;
        int fork = between(2, 3);
        t.branch(cx, y, cz, cx, y + fork, cz);
        int stems = between(2, 3);
        t.info("stems", stems);
        double start = randomAngle();
        for (int i = 0; i < stems; i++) {
            double yaw = start + i * Math.PI * 2 / stems + jitter(0.25);
            double rise = between(4.5, 7.5);
            double out = between(3.0, 4.5);
            // Each stem first climbs almost straight up beside the others and then leans out, so the stems part
            // gradually instead of all leaving the trunk from one block
            double ax = cx + Math.cos(yaw) * 0.8;
            double ay = y + fork + rise * 0.35;
            double az = cz + Math.sin(yaw) * 0.8;
            double ex = cx + Math.cos(yaw) * out;
            double ey = y + fork + rise;
            double ez = cz + Math.sin(yaw) * out;
            t.branch(cx, y + fork, cz, ax, ay, az);
            t.branch(ax, ay, az, ex, ey, ez);
            acaciaPad(ex, (int) Math.floor(ey), ez, between(2.4, 3.4));
        }
    }

    /**
     * Places a flat pad of acacia leaves resting directly on the end of a stem, in two layers, with no logs beneath it.
     */
    private void acaciaPad(double x, int y, double z, double radius) {
        t.disc(x, y + 1, z, radius, 0.3);
        t.disc(x, y + 2, z, radius * 0.6, 0.25);
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
     * Grows a round, thick trunk whose radius changes along its height, rounded so that no single blocks stick out of its
     * sides. Where it steps in or out between two heights, a block is added in the middle of each side of the narrower
     * slice, so the step is rounded off instead of leaving a flat ledge.
     * @param radius Provides the radius of the trunk at a height, from 0 at the base to 1 at the top.
     */
    private void baobabTrunk(double cx, double cz, int y, int height, DoubleUnaryOperator radius) {
        double[] radii = new double[height + 1];
        for (int h = 0; h <= height; h++) {
            radii[h] = smoothRadius(radius.applyAsDouble((double) h / height));
            t.trunk(cx, cz, y + h, y + h, radii[h]);
        }
        int bx = (int) Math.floor(cx);
        int bz = (int) Math.floor(cz);
        for (int h = 0; h <= height; h++) {
            for (int n = -1; n <= 1; n += 2) {
                int other = h + n;
                if (other < 0 || other > height || radii[other] - radii[h] < 0.7) {
                    continue;
                }
                int reach = (int) Math.floor(radii[other]);
                t.wood(bx + reach, y + h, bz);
                t.wood(bx - reach, y + h, bz);
                t.wood(bx, y + h, bz + reach);
                t.wood(bx, y + h, bz - reach);
            }
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
        int height = between(8, 12);
        // A fatter bottle, so every slice is round rather than a square
        double bulge = between(3.0, 3.4);
        double bulgeAt = between(0.3, 0.5);
        double top = between(1.45, 2.3);
        baobabTrunk(cx, cz, y, height, p -> p < bulgeAt
                ? 2.25 + (bulge - 2.25) * Math.sin(p / bulgeAt * Math.PI / 2)
                : top + (bulge - top) * Math.cos((p - bulgeAt) / (1 - bulgeAt) * Math.PI / 2));
        surfaceRoots(cx, y, cz, between(5, 6), 1.2, 2.5, 2.4, 2, Material.ACACIA_WOOD);
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
        // The trunk swells and narrows a little along its length, so it is never a perfectly straight column
        double phase = randomAngle();
        baobabTrunk(cx, cz, y, height, p -> (p < 0.12
                ? flare + (base - flare) * Math.sin(p / 0.12 * Math.PI / 2)
                : base + (top - base) * Math.pow((p - 0.12) / 0.88, 1.4)) + 0.25 * Math.sin(p * Math.PI * 2 + phase));
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
        // Thick main branches leave the top of the trunk, climb and then arch outwards, forking into stubby twigs
        Crown grand = new Crown(-0.04, 0.2, 0.35, 2, 3, 0.55, 1.3, 2.0, 1.1, 0.5, 0.35, 0.3);
        int branches = between(7, 9);
        double start = randomAngle();
        for (int i = 0; i < branches; i++) {
            double angle = start + i * Math.PI * 2 / branches + jitter(0.3);
            double sx = cx + Math.cos(angle) * top * 0.7;
            double sz = cz + Math.sin(angle) * top * 0.7;
            double sy = y + height - between(0, 2);
            naturalLimb(sx, sy, sz, angle, between(0.6, 0.9), between(7.0, 9.5), 1.1, 2, grand);
        }
        // The trunk narrows into a short leader instead of ending flat
        t.thickBranch(cx, y + height, cz, cx + jitter(0.5), y + height + 3, cz + jitter(0.5), top * 0.7, 0.8);
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
        // A round trunk that flares out at its base, rather than a square column, and rounds off at its shoulders into the
        // stems instead of ending in a flat top
        baobabTrunk(cx, cz, y, splitHeight, p -> 2.3 - 0.05 * p + 0.8 * Math.exp(-p * 6)
                - 0.85 * Math.pow(Math.max(0, (p - 0.7) / 0.3), 2));
        surfaceRoots(cx, y, cz, between(5, 6), 1.2, 2.5, 1.8, 2, Material.ACACIA_WOOD);
        t.info("height", height);
        t.info("split", splitHeight);

        int stems = between(2, 3);
        double start = randomAngle();
        for (int i = 0; i < stems; i++) {
            double angle = start + i * Math.PI * 2 / stems + jitter(0.3);
            double spread = between(1.5, 2.3);
            double stemRadius = between(0.9, 1.15);
            double sx = cx + Math.cos(angle);
            double sz = cz + Math.sin(angle);
            double ex = cx + Math.cos(angle) * (1.0 + spread);
            double ez = cz + Math.sin(angle) * (1.0 + spread);
            int stemTop = y + height - between(0, 2);
            int stemHeight = stemTop - (y + splitHeight);
            // Stems start inside the top of the trunk, so they grow out of its rounded shoulders
            for (int h = -2; h <= stemHeight; h++) {
                double p = (double) Math.max(0, h) / stemHeight;
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
     * #14 Hollow Pale Oak - grown from a 2x2 of saplings. A gnarled, twisting 2x2 trunk with crooked, leafy limbs that sag
     * at their ends. Most stand tall, and one in three is smaller.
     */
    private void gnarledOak(int x, int y, int z, boolean pale) {
        if (!pale) {
            gnarledDarkOak(x, y, z);
            return;
        }
        startOak(true);
        t.setPorosity(0.3);
        double scale = between(0.8, 1.3);
        // Pale oaks are wide and droopy. Most stand tall, and one in three is a smaller, lower tree
        boolean small = chance(1 / 3.0);
        int height = (int) Math.round(between(4.5, 6.5) * scale) + between(3, 5) + (small ? 0 : between(4, 6));
        t.info("small", small);
        // The tall ones carry a bigger, denser crown, so they are not thin for their height
        if (!small) {
            t.setPorosity(0.18);
        }
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
        int limbs = small ? between(6, 7) : between(7, 9);
        double start = randomAngle();
        for (int i = 0; i < limbs; i++) {
            double angle = start + i * Math.PI * 2 / limbs + jitter(0.3);
            double px = topX;
            double py = topY - between(0, 3);
            double pz = topZ;
            for (int segment = 0; segment < 3; segment++) {
                double segmentAngle = angle + jitter(0.7);
                double length = between(1.8, 2.7) * scale * (small ? 1 : 1.15);
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
            t.blob(px, py + 0.5, pz, small ? 3.4 : 3.9, small ? 1.5 : 1.9, small ? 3.4 : 3.9, 0.5);
        }
        if (pale) {
            t.blob(topX, topY + 1.0, topZ, small ? 4.0 : 4.8, small ? 1.6 : 2.2, small ? 4.0 : 4.8, 0.45);
            // A few small side branches lower down the trunk, more on the tall ones
            LimbStyle side = new LimbStyle(0.25, 0.04, 0.3, 0.6, 1, 2, 0.6, 0.7, 1.4, 2.0, 1.2, 0.4, 0.45, 0.5);
            int sideBranches = small ? between(1, 2) : between(2, 4);
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
     * #26 Gnarled Dark Oak - grown from a 2x2 of saplings. Much like a vanilla dark oak, improved and much taller: a 2x2
     * trunk that twists a little as it rises, splitting into three or four main limbs that leave it thick and arch
     * outwards, each with a couple of side branches, under a broad, dense and flat-topped canopy.
     */
    private void gnarledDarkOak(int x, int y, int z) {
        startOak(false);
        t.setPorosity(0.1);
        // About a third smaller than before, so the Ancient Dark Oak stands well above it
        double scale = between(0.65, 0.85);
        int height = (int) Math.round(between(10.0, 13.0) * scale);
        t.info("scale", scale);
        t.info("height", height);
        int ox = 0;
        int oz = 0;
        for (int h = 0; h <= height; h++) {
            // The trunk twists as it rises
            if (h >= 4 && h % 3 == 1 && chance(0.5)) {
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
        for (int[] offset : flare) {
            int rise = chance(0.5) ? between(1, 2) : 0;
            for (int h = 0; h < rise; h++) {
                t.wood(x + offset[0], y + h, z + offset[1]);
            }
        }
        surfaceRoots(x + 1, y, z + 1, between(3, 4), 1.2, 2.4, 1.2, 1, Material.DARK_OAK_WOOD);

        double topX = x + ox + 1;
        double topZ = z + oz + 1;
        int topY = y + height;
        // The main limbs arch outwards and down, so the tree keeps the arches of a dark oak but with a full canopy. They
        // branch all along their length and split at their ends
        Crown crown = new Crown(-0.09, 0.22, 0.8, 2, 3, 0.6, 1.4, 2.8, 1.7, 0.6, 0.35, 0.6);
        int limbs = between(4, 6);
        double start = randomAngle();
        for (int i = 0; i < limbs; i++) {
            double angle = start + i * Math.PI * 2 / limbs + jitter(0.3);
            naturalLimb(topX + Math.cos(angle) * 0.6, topY - between(1, 3), topZ + Math.sin(angle) * 0.6, angle,
                    between(0.8, 1.0), between(6.5, 8.5) * scale, 0.95, 2, crown);
        }
        // A broad, flat-topped canopy over the middle, like a vanilla dark oak's
        t.blob(topX, topY + 2.0, topZ, 5.5 * scale, 2.0, 5.5 * scale, 0.35);
        t.blob(topX, topY + 3.5, topZ, 3.6 * scale, 1.4, 3.6 * scale, 0.35);
        decorateOak(false, x + 1, y, z + 1, 8.0 * scale);
        if (chance(0.5)) {
            mushroomPatches(x + 1, y, z + 1, between(1, 2), 2.5, 5.5);
        }
    }

    /**
     * #13 Ancient Dark Oak and #28 Ancient Pale Oak - grown from a 3x3 of saplings. A flared 3x3 trunk with deep roots
     * and sprawling single limbs, starting no lower than the 4th block, that fork into a broad, full canopy.
     */
    private void ancientOak(int x, int y, int z, boolean pale) {
        startOak(pale);
        t.setPorosity(pale ? 0.16 : 0.1);
        // The dark oak is grown larger, so it is clearly bigger than the Gnarled Dark Oak
        double scale = pale ? between(1.2, 1.6) : between(1.5, 1.85);
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

        // Thick limbs leave the top of the trunk, climb and arch outwards, branching along the way into a dense canopy
        Crown crown = new Crown(-0.06, 0.22, 0.6, 2, 3, 0.6, 1.6, 3.4, 2.2, 0.6, 0.35, 0.7);
        int limbs = between(6, 7);
        double start = randomAngle();
        for (int i = 0; i < limbs; i++) {
            double angle = start + i * Math.PI * 2 / limbs + jitter(0.3);
            // Never lower than the 4th block of the trunk
            double sy = Math.max(y + 3, y + trunkHeight - between(0, 2));
            naturalLimb(cx + Math.cos(angle) * 1.2, sy, cz + Math.sin(angle) * 1.2, angle, between(0.55, 0.8),
                    between(6.5, 8.5) * scale * 0.85, 1.1, 2, crown);
        }
        naturalLimb(cx, y + trunkHeight, cz, randomAngle(), 1.3, 4.0 * scale, 0.9, 1, crown);

        // Fills the canopy, so it is full and dense with no open gaps
        int canopyY = y + trunkHeight + (int) Math.round(3 * scale);
        t.blob(cx, canopyY + 1, cz, 5.5 * scale, 2.6, 5.5 * scale, 0.4);
        int fills = between(6, 8);
        double fillStart = randomAngle();
        for (int i = 0; i < fills; i++) {
            double angle = fillStart + i * Math.PI * 2 / fills + jitter(0.4);
            double distance = between(3.0, 6.0) * scale;
            t.blob(cx + Math.cos(angle) * distance, canopyY + between(0.0, 2.0), cz + Math.sin(angle) * distance,
                    3.2, 2.0, 3.2, 0.4);
        }
        decorateOak(false, cx, y, cz, 8.5 * scale);
        // Old and overgrown: more vines hanging from the canopy and climbing the wood, and mushrooms in its shade
        hangingVines(0.1, 6, y + 4);
        woodVines(y + 1, 0.1);
        if (chance(0.7)) {
            mushroomPatches(cx, y, cz, between(2, 4), 3.0, 7.0 * scale);
        }
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
        // Roots climb two to five blocks up the trunk before curving down into the ground, so the trunk rises out of them
        int buttresses = between(6, 8);
        double rootStart = randomAngle();
        for (int i = 0; i < buttresses; i++) {
            double rootAngle = rootStart + i * Math.PI * 2 / buttresses + jitter(0.3);
            double rise = between(2.0, 5.0);
            double reach = between(3.0, 5.0);
            double ox = Math.cos(rootAngle);
            double oz = Math.sin(rootAngle);
            double mx = cx + ox * (2.0 + reach * 0.4);
            double mz = cz + oz * (2.0 + reach * 0.4);
            double my = y + rise * 0.35;
            t.thickRoot(cx + ox * 1.6, y + rise, cz + oz * 1.6, mx, my, mz, 0.9, 0.6, Material.PALE_OAK_WOOD);
            t.thickRoot(mx, my, mz, cx + ox * (2.0 + reach), y - 1, cz + oz * (2.0 + reach), 0.6, 0.3,
                    Material.PALE_OAK_WOOD);
        }

        // Limbs grow out of the side of the trunk at uneven heights and directions, thick where they leave it, then
        // climb and arch outwards, leafy all along their length
        Crown crown = new Crown(-0.05, 0.2, 0.6, 2, 3, 0.62, 1.6, 3.2, 1.9, 0.5, 0.4, 0.95);
        LimbStyle style = new LimbStyle(0.22, 0.04, 0.3, 0.7, 2, 3, 0.6, 0.74, 1.6, 3.2, 1.9, 0.5, 0.4, 0.95);
        int limbs = between(8, 9);
        double angle = randomAngle();
        for (int i = 0; i < limbs; i++) {
            angle += GOLDEN_ANGLE + jitter(0.35);
            double sy = Math.max(y + 3, y + trunkHeight * between(0.6, 1.0) + between(0, 2));
            naturalLimb(cx + Math.cos(angle) * 1.3, sy, cz + Math.sin(angle) * 1.3, angle, between(0.5, 0.75),
                    between(7.0, 9.0) * scale * 0.8, 1.15, 2, crown);
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
     * #90 Elder Cherry - grown from a 3x3 of saplings. The cherry's answer to the Old Giant Oak: a short, massive trunk
     * flared at its base, whose thick limbs rise steeply and then bend gently outwards, holding huge clouds of blossom
     * that sag a little at their edges, with a wide carpet of petals below.
     */
    private void elderCherry(int x, int y, int z) {
        t = new TreeBuilder(world, r, Material.CHERRY_WOOD, Material.CHERRY_LEAVES);
        t.setPorosity(0.25);
        double scale = between(1.0, 1.2);
        double cx = x + 1.5;
        double cz = z + 1.5;
        int trunkHeight = between(5, 7);
        for (int h = 0; h <= trunkHeight; h++) {
            t.trunk(cx, cz, y + h, y + h, smoothRadius(1.45 + 0.8 * Math.exp(-h / 1.5)));
        }
        surfaceRoots(cx, y, cz, between(6, 7), 2.0, 3.5, 1.8, 1, Material.CHERRY_WOOD);
        t.info("scale", scale);
        t.info("trunk", trunkHeight);
        // Limbs bend outwards only gently, so the blossom sags a little at the edges without reaching the ground
        Crown crown = new Crown(-0.045, 0.2, 0.55, 2, 2, 0.6, 1.8, 4.4, 2.2, 0.8, 0.3, 0.8);
        int limbs = between(5, 6);
        double start = randomAngle();
        for (int i = 0; i < limbs; i++) {
            double angle = start + i * Math.PI * 2 / limbs + jitter(0.3);
            naturalLimb(cx + Math.cos(angle), y + trunkHeight - between(0, 2), cz + Math.sin(angle), angle,
                    between(0.9, 1.05), between(9.0, 11.5) * scale, 1.0, 2, crown);
        }
        t.blob(cx, y + trunkHeight + 7 * scale, cz, 6.5, 3.4, 6.5, 0.3);
        cherryFinish(x + 1, y, z + 1, 11 * scale);
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
            // Every root is shaped differently: some reach far and some stay close, some arch high and some drop
            // steeply, and some bend sideways on their way down
            double reach = between(2.5, 7.0) * scale;
            double sy = baseY + between(-1, 2);
            double bendAngle = angle + (chance(0.35) ? jitter(0.6) : 0);
            double controlX = cx + Math.cos(bendAngle) * reach * between(0.4, 0.8);
            double controlY = sy + between(0.5, 3.0);
            double controlZ = cz + Math.sin(bendAngle) * reach * between(0.4, 0.8);
            double endX = cx + Math.cos(angle) * reach;
            double endY = y - 1;
            double endZ = cz + Math.sin(angle) * reach;
            // Now and then a root runs on along the ground for a block or two before digging in
            if (chance(0.35)) {
                double runAngle = angle + jitter(0.5);
                double run = between(1.0, 2.5);
                t.rootLine(endX, y, endZ, endX + Math.cos(runAngle) * run, y, endZ + Math.sin(runAngle) * run,
                        Material.MANGROVE_ROOTS, true);
            }
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
     * into it. Its canopy is built in four close levels of massive limbs, each reaching a little less far than the one
     * below, and a crown.
     */
    private void banyanGrove(int x, int y, int z, int idea) {
        // Its limbs grow in one of three ways: 0 Arching Limbs, 1 Winding Limbs or 2 Forking Limbs, mostly arching or
        // forking
        double roll = r.nextDouble();
        int mode = idea >= 0 ? idea % 3 : roll < 0.4 ? 0 : roll < 0.8 ? 2 : 1;
        t = new TreeBuilder(world, r, Material.MANGROVE_WOOD, Material.MANGROVE_LEAVES);
        t.info("idea", new String[] {"Arching_Limbs", "Winding_Limbs", "Forking_Limbs"}[mode]);
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

        // The heights of the tiers of limbs, close enough together that no big gaps open up between them
        int tier1 = y + (int) Math.round(height * between(0.38, 0.42));
        int tier2 = y + (int) Math.round(height * between(0.5, 0.54));
        int tier3 = y + (int) Math.round(height * between(0.62, 0.66));
        int tier4 = y + (int) Math.round(height * between(0.76, 0.8));

        // The bulk of the trunk is at least 11 blocks across, swelling out much wider at its base and narrowing above the
        // lowest tier of limbs
        double bulk = Math.max(5.6, 5.2 * Math.sqrt(s));
        DoubleUnaryOperator radius = h -> {
            double base = bulk + 3.5 * Math.exp(-h / (5.0 * s));
            int lower = tier1 - y;
            if (h <= lower) {
                return base;
            }
            double p = Math.min(1, (h - lower) / (double) (tier4 - tier1));
            return base - (base - 2.2) * p;
        };
        int trunkHeight = tier4 - y;
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
        // Each tier reaches a little less far than the one below it, so no long low limb sticks out on its own
        double angle = randomAngle();
        angle = banyanTier(cx, cz, y, tier1, radius, between(9, 10), 28.0 * s, 34.0 * s, 2.4 * s, 3, canopy, angle, s, mode);
        angle = banyanTier(cx, cz, y, tier2, radius, between(8, 9), 22.0 * s, 27.0 * s, 2.0 * s, 3, canopy, angle, s, mode);
        angle = banyanTier(cx, cz, y, tier3, radius, between(7, 8), 16.0 * s, 20.0 * s, 1.6 * s, 2, canopy, angle, s, mode);
        banyanTier(cx, cz, y, tier4, radius, between(6, 7), 10.0 * s, 13.0 * s, 1.2 * s, 2, small, angle, s, mode);

        // A stout leader climbs on up the middle into a rounded crown, as wide as it is tall
        double leaderX = cx + jitter(1.5);
        double leaderZ = cz + jitter(1.5);
        int leaderTop = y + height - (int) Math.round(5 * s);
        t.thickBranch(cx, tier4, cz, leaderX, leaderTop, leaderZ, 2.2, 1.0);
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
     * @param mode How the limbs grow: 0 each rises in a high arch and comes back down far out from the trunk, 1 each
     *             winds from side to side as it goes, 2 each is short and splits into thick forks that do the reaching.
     * @return The direction of the last limb of this tier.
     */
    private double banyanTier(double cx, double cz, int y, int tierY, DoubleUnaryOperator radius, int limbs, double minReach,
                              double maxReach, double thickness, int depth, LimbStyle style, double angle, double s,
                              int mode) {
        double reachScale = switch (mode) {
            case 0 -> 0.85;
            case 2 -> 0.5;
            default -> 0.9;
        };
        for (int i = 0; i < limbs; i++) {
            angle += GOLDEN_ANGLE + jitter(0.25);
            double sy = tierY - between(0, 3);
            double startRadius = radius.applyAsDouble(sy - y) - 1.5;
            double sx = cx + Math.cos(angle) * startRadius;
            double sz = cz + Math.sin(angle) * startRadius;
            double reach = between(minReach, maxReach) * reachScale;
            // Each tier climbs gently towards the next, so the levels of the canopy run into each other without gaps
            double rise = between(2.5, 5.0) * s;
            double arch = reach * between(0.22, 0.3);
            double limbThickness = thickness * between(0.85, 1.15);
            // Long limbs sag slightly towards their tips, so the canopy slopes down at its edges like a cone
            double sag = reach * 0.06;
            double phase = randomAngle();
            double waves = between(1.0, 1.6);
            int segments = Math.max(6, (int) Math.round(reach / 4));
            double px = sx;
            double py = sy;
            double pz = sz;
            for (int k = 1; k <= segments; k++) {
                double u = (double) k / segments;
                double nx;
                double ny;
                double nz;
                if (mode == 1) {
                    // Winds from side to side as it goes, rising and dipping gently
                    double heading = angle + 0.5 * Math.sin(u * Math.PI * 2 * waves + phase);
                    double step = reach / segments * 1.1;
                    nx = px + Math.cos(heading) * step;
                    nz = pz + Math.sin(heading) * step;
                    ny = sy + rise * Math.sin(u * Math.PI / 2) + 1.5 * s * Math.sin(u * Math.PI * 3 + phase) * u;
                } else {
                    double heading = angle + jitter(0.08);
                    nx = sx + Math.cos(heading) * reach * u;
                    nz = sz + Math.sin(heading) * reach * u;
                    // Arching limbs rise high and come back down far out from the trunk
                    ny = mode == 0 ? sy + arch * Math.sin(Math.PI * u * 0.85)
                            : sy + rise * Math.sin(u * Math.PI / 2) - sag * u * u;
                }
                // The limb stays thick for most of its length and only thins out towards its end
                double u0 = (double) (k - 1) / segments;
                t.thickBranch(px, py, pz, nx, ny, nz, limbThickness * (1 - 0.65 * Math.pow(u0, 1.6)),
                        limbThickness * (1 - 0.65 * Math.pow(u, 1.6)));
                // Side branches leave the limb all along its length, alternating sides, so the long limbs stay full
                // They start thick and taper as they split, so only the ends of the smallest branches are 1 block wide
                if (u >= 0.25 && u < 1 && chance(0.85)) {
                    thickLimb(nx, ny, nz, angle + (k % 2 == 0 ? 1 : -1) * between(0.6, 1.1), between(0.15, 0.35),
                            reach * between(0.22, 0.32), depth - 1, style,
                            limbThickness * (1 - 0.65 * Math.pow(u, 1.6)) * 0.55);
                }
                // Branches climbing up from the middle of the limb fill the space between it and the tier above
                if (u >= 0.35 && u <= 0.75 && chance(0.35)) {
                    thickLimb(nx, ny, nz, angle + jitter(0.6), between(0.6, 0.9), reach * between(0.18, 0.25), 1, style,
                            limbThickness * 0.4);
                }
                px = nx;
                py = ny;
                pz = nz;
            }
            if (mode == 2) {
                // A short limb splits at its end into two or three thick forks that spread apart and split again
                int forks = between(2, 3);
                for (int f = 0; f < forks; f++) {
                    double offset = -0.5 + 1.0 * f / (forks - 1) + jitter(0.15);
                    thickLimb(px, py, pz, angle + offset, between(0.1, 0.3), reach * between(0.8, 1.0), depth, style,
                            limbThickness * 0.5);
                }
            } else {
                thickLimb(px, py, pz, angle + jitter(0.3), between(0.1, 0.3), reach * between(0.3, 0.38), depth, style,
                        limbThickness * 0.35);
            }
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

    // ------------------------------------------------------------
    // Rare trees - shared helpers
    // ------------------------------------------------------------

    /** The direction the wind blows towards, so that every windswept tree leans the same way. East. */
    private static final double WIND_YAW = 0;

    /**
     * Picks one of several woods for a block of bark, in streaks that run along the trunk.
     * @param stretch How far each streak stretches vertically compared to sideways.
     */
    private Material streak(int x, int y, int z, Material[] palette, double stretch) {
        double n = t.noise(x / 1.3, y / stretch, z / 1.3, 4);
        return palette[(int) Math.floor(n * palette.length * 3) % palette.length];
    }

    /**
     * Draws a branch whose bark mixes several woods in streaks.
     */
    private void streakLine(double x0, double y0, double z0, double x1, double y1, double z1, Material[] palette,
                            double stretch) {
        t.line(x0, y0, z0, x1, y1, z1, (bx, by, bz, axis) -> t.wood(bx, by, bz, streak(bx, by, bz, palette, stretch)));
    }

    /**
     * Fills a layer of a round trunk whose bark mixes several woods in streaks.
     */
    private void streakDisc(double cx, int y, double cz, double radius, Material[] palette, double stretch) {
        int range = (int) Math.ceil(radius) + 1;
        for (int bx = (int) Math.floor(cx) - range; bx <= cx + range; bx++) {
            for (int bz = (int) Math.floor(cz) - range; bz <= cz + range; bz++) {
                double dx = bx + 0.5 - cx;
                double dz = bz + 0.5 - cz;
                if (dx * dx + dz * dz <= radius * radius + 0.01) {
                    t.wood(bx, y, bz, streak(bx, y, bz, palette, stretch));
                }
            }
        }
    }

    /**
     * Provides the smallest difference between two angles.
     */
    private static double angleDifference(double a, double b) {
        double difference = Math.abs(a - b) % (Math.PI * 2);
        return difference > Math.PI ? Math.PI * 2 - difference : difference;
    }

    /**
     * Grows a palm frond from the crown that arches up and then droops towards its tip, with leaflets along both sides.
     * The first part holds a hidden spine of wood so that the tip stays close enough to wood not to decay.
     * @param lift How steeply the frond rises at first.
     * @param droop How strongly the frond bends down towards its tip.
     */
    private void frond(double cx, double cy, double cz, double yaw, double length, double lift, double droop) {
        double dx = Math.cos(yaw);
        double dz = Math.sin(yaw);
        // Leaflets stick out sideways from the frond, along whichever axis is most across it
        int sideX = Math.abs(dx) > Math.abs(dz) ? 0 : 1;
        int sideZ = 1 - sideX;
        // Diagonal fronds take more steps through their leaves, so the spine runs far enough that the tip stays within reach
        double spine = Math.max(0, length - 3.2);
        double px = cx;
        double py = cy;
        double pz = cz;
        for (double s = 0.5; s <= length + 0.01; s += 0.5) {
            double nx = cx + dx * s;
            double ny = cy + lift * s - droop * s * s / length;
            double nz = cz + dz * s;
            if (s <= spine) {
                t.branch(px, py, pz, nx, ny, nz);
                // The spine is wrapped in leaves on every side, so no wood shows through the frond
                int bx = (int) Math.floor(nx);
                int by = (int) Math.floor(ny);
                int bz = (int) Math.floor(nz);
                t.leaf(bx, by + 1, bz);
                t.leaf(bx, by - 1, bz);
                t.leaf(bx + sideX, by, bz + sideZ);
                t.leaf(bx - sideX, by, bz - sideZ);
            } else {
                t.line(px, py, pz, nx, ny, nz, (bx, by, bz, axis) -> t.leaf(bx, by, bz));
            }
            double p = s / length;
            if (p > 0.2 && p < 0.9) {
                int bx = (int) Math.floor(nx);
                int by = (int) Math.floor(ny);
                int bz = (int) Math.floor(nz);
                t.leaf(bx + sideX, by, bz + sideZ);
                t.leaf(bx - sideX, by, bz - sideZ);
                // The leaflets hang down a little at their ends
                if (chance(0.35)) {
                    t.leaf(bx + sideX, by - 1, bz + sideZ);
                }
                if (chance(0.35)) {
                    t.leaf(bx - sideX, by - 1, bz - sideZ);
                }
            }
            px = nx;
            py = ny;
            pz = nz;
        }
    }

    /**
     * Places resin clumps on the sides and undersides of the tree's wood, often running a short way down it.
     */
    private void resinClumps(int count, int minY, int maxY) {
        resinClumps(count, minY, maxY, 0, 0, Double.MAX_VALUE);
    }

    /**
     * Places resin running down the sides of the tree's wood, only on the wood within a distance of a point, such as the
     * trunk.
     */
    private void resinClumps(int count, int minY, int maxY, double cx, double cz, double maxDistance) {
        List<Pos> wood = new ArrayList<>(t.getPositions(Layer.LOG));
        wood.removeIf(pos -> pos.y() < minY || pos.y() > maxY || !Tag.LOGS.isTagged(t.typeAt(pos.x(), pos.y(), pos.z()))
                || Math.hypot(pos.x() + 0.5 - cx, pos.z() + 0.5 - cz) > maxDistance);
        if (wood.isEmpty()) {
            return;
        }
        boolean trunkOnly = maxDistance < 100;
        BlockFace[] faces = trunkOnly ? HORIZONTAL
                : new BlockFace[] {BlockFace.NORTH, BlockFace.EAST, BlockFace.SOUTH, BlockFace.WEST, BlockFace.DOWN};
        int placed = 0;
        for (int attempt = 0; attempt < count * 8 && placed < count; attempt++) {
            Pos pos = wood.get(r.nextInt(wood.size()));
            BlockFace face = faces[r.nextInt(faces.length)];
            BlockFace towardsWood = face.getOppositeFace();
            // Resin on a trunk leaks a long way down it
            int run = face == BlockFace.DOWN ? 1 : trunkOnly ? between(2, 4) : between(1, 3);
            boolean any = false;
            for (int i = 0; i < run; i++) {
                int px = pos.x() + face.getModX();
                int py = pos.y() + face.getModY() - i;
                int pz = pos.z() + face.getModZ();
                Material behind = t.typeAt(px + towardsWood.getModX(), py + towardsWood.getModY(), pz + towardsWood.getModZ());
                if (behind == null || !Tag.LOGS.isTagged(behind) || t.has(px, py, pz)) {
                    break;
                }
                MultipleFacing resin = (MultipleFacing) Material.RESIN_CLUMP.createBlockData();
                resin.setFace(towardsWood, true);
                if (t.decor(px, py, pz, resin, towardsWood.getModX(), towardsWood.getModY(), towardsWood.getModZ())) {
                    any = true;
                }
            }
            if (any) {
                placed++;
            }
        }
    }

    /**
     * Lets the edges of the foliage sag, hanging a leaf or two beneath leaves that have nothing under them, so clusters
     * look soft and flowing rather than round.
     * @param minY Only leaves at or above this height sag.
     */
    private void sagLeaves(double chance, int minY) {
        for (Pos leaf : new ArrayList<>(t.getPositions(Layer.LEAF))) {
            if (leaf.y() < minY || t.has(leaf.x(), leaf.y() - 1, leaf.z()) || !t.isLeaf(leaf.x(), leaf.y(), leaf.z())
                    || !chance(chance)) {
                continue;
            }
            t.leaf(leaf.x(), leaf.y() - 1, leaf.z());
            if (chance(0.35) && !t.has(leaf.x(), leaf.y() - 2, leaf.z())) {
                t.leaf(leaf.x(), leaf.y() - 2, leaf.z());
            }
        }
    }

    /**
     * Hangs tassels of leaves beneath leaves that have nothing under them, for foliage that droops heavily.
     * @param maxLength The longest a tassel hangs.
     */
    private void droopLeaves(double chance, int minY, int maxLength) {
        for (Pos leaf : new ArrayList<>(t.getPositions(Layer.LEAF))) {
            if (leaf.y() < minY || t.has(leaf.x(), leaf.y() - 1, leaf.z()) || !t.isLeaf(leaf.x(), leaf.y(), leaf.z())
                    || !chance(chance)) {
                continue;
            }
            int length = between(1, maxLength);
            for (int i = 1; i <= length && !t.has(leaf.x(), leaf.y() - i, leaf.z()); i++) {
                t.leaf(leaf.x(), leaf.y() - i, leaf.z());
            }
        }
    }

    /**
     * Removes leaves that touch nothing else of the tree on any side, so no single leaf is left floating.
     */
    private void removeLoneLeaves() {
        for (Pos leaf : new ArrayList<>(t.getPositions(Layer.LEAF))) {
            boolean touching = false;
            for (BlockFace face : new BlockFace[] {BlockFace.NORTH, BlockFace.EAST, BlockFace.SOUTH, BlockFace.WEST,
                    BlockFace.UP, BlockFace.DOWN}) {
                if (t.has(leaf.x() + face.getModX(), leaf.y() + face.getModY(), leaf.z() + face.getModZ())) {
                    touching = true;
                    break;
                }
            }
            if (!touching) {
                t.removeLeaf(leaf.x(), leaf.y(), leaf.z());
            }
        }
    }

    /**
     * Hangs short strands of pale moss from the undersides of the leaves, never long enough to form curtains.
     */
    private void mossStrands(double chance, int maxLength) {
        for (Pos leaf : t.getPositions(Layer.LEAF)) {
            if (t.has(leaf.x(), leaf.y() - 1, leaf.z()) || !chance(chance)) {
                continue;
            }
            hangMoss(leaf.x(), leaf.y() - 1, leaf.z(), between(1, maxLength));
        }
    }

    /**
     * Hangs a strand of pale moss downwards from just below a block.
     */
    private void hangMoss(int x, int topY, int z, int length) {
        for (int i = 0; i < length; i++) {
            HangingMoss moss = (HangingMoss) Material.PALE_HANGING_MOSS.createBlockData();
            moss.setTip(i == length - 1);
            if (!t.decor(x, topY - i, z, moss, 0, 1, 0)) {
                break;
            }
        }
    }

    /**
     * Turns the ground in a circle around the tree into another block, thinning out towards its edge.
     */
    private void groundSoil(double cx, int y, double cz, double radius, double density, Material material) {
        int range = (int) Math.ceil(radius);
        for (int dx = -range; dx <= range; dx++) {
            for (int dz = -range; dz <= range; dz++) {
                int bx = (int) Math.floor(cx) + dx;
                int bz = (int) Math.floor(cz) + dz;
                double distance = Math.hypot(bx + 0.5 - cx, bz + 0.5 - cz);
                if (distance > radius || !chance(density * fade(distance, radius))) {
                    continue;
                }
                Block ground = findGround(bx, y, bz, 3);
                if (ground == null) {
                    continue;
                }
                Block soil = ground.getRelative(BlockFace.DOWN);
                if (!t.has(soil.getX(), soil.getY(), soil.getZ())) {
                    t.soil(soil.getX(), soil.getY(), soil.getZ(), material);
                }
            }
        }
    }

    /**
     * Provides the point a trunk made of straight segments passes through at a height.
     * @param path The points of the trunk from bottom to top, as x, y and z.
     */
    private static double[] pathAt(List<double[]> path, double y) {
        for (int i = 1; i < path.size(); i++) {
            double[] a = path.get(i - 1);
            double[] b = path.get(i);
            if (y <= b[1] || i == path.size() - 1) {
                double p = b[1] == a[1] ? 0 : Math.max(0, Math.min(1, (y - a[1]) / (b[1] - a[1])));
                return new double[] {a[0] + (b[0] - a[0]) * p, y, a[2] + (b[2] - a[2]) * p};
            }
        }
        return path.get(0);
    }

    /**
     * Builds a trunk that bends between a few points, returning its path from bottom to top.
     * @param bend How far each point may stray sideways.
     */
    private List<double[]> bentTrunk(double cx, int y, double cz, int height, int segments, double bend, Material[] palette) {
        List<double[]> path = new ArrayList<>();
        path.add(new double[] {cx, y, cz});
        double px = cx;
        double pz = cz;
        for (int i = 1; i <= segments; i++) {
            px += jitter(bend);
            pz += jitter(bend);
            path.add(new double[] {px, y + (double) height * i / segments, pz});
        }
        for (int i = 1; i < path.size(); i++) {
            double[] a = path.get(i - 1);
            double[] b = path.get(i);
            if (palette == null) {
                t.branch(a[0], a[1], a[2], b[0], b[1], b[2]);
            } else {
                streakLine(a[0], a[1], a[2], b[0], b[1], b[2], palette, 4);
            }
        }
        return path;
    }

    /**
     * Places a large fern a short way from the tree, made of its two halves.
     */
    private void largeFerns(int x, int y, int z, int count, double minDistance, double maxDistance) {
        for (int i = 0; i < count; i++) {
            double angle = randomAngle();
            double distance = between(minDistance, maxDistance);
            Block ground = findGround((int) Math.floor(x + 0.5 + Math.cos(angle) * distance), y,
                    (int) Math.floor(z + 0.5 + Math.sin(angle) * distance), 3);
            if (ground == null || t.has(ground.getX(), ground.getY(), ground.getZ())
                    || t.has(ground.getX(), ground.getY() + 1, ground.getZ())
                    || !isSoil(ground.getRelative(BlockFace.DOWN).getType())) {
                continue;
            }
            Bisected lower = (Bisected) Material.LARGE_FERN.createBlockData();
            lower.setHalf(Bisected.Half.BOTTOM);
            Bisected upper = (Bisected) Material.LARGE_FERN.createBlockData();
            upper.setHalf(Bisected.Half.TOP);
            if (t.decor(ground.getX(), ground.getY(), ground.getZ(), lower, 0, -1, 0)) {
                t.decor(ground.getX(), ground.getY() + 1, ground.getZ(), upper, 0, -1, 0);
            }
        }
    }

    // ------------------------------------------------------------
    // Rare trees - Acacia
    // ------------------------------------------------------------

    /**
     * #49 Dragonblood Tree - grown from a 2x2 of saplings. A stout trunk that forks again and again into upturned branches
     * whose tips all stop at about the same height, holding a dense canopy like an umbrella blown inside out, closed all
     * the way across. Resin leaks down the trunk.
     */
    private void dragonblood(int x, int y, int z) {
        t = new TreeBuilder(world, r, Material.MANGROVE_WOOD, Material.MANGROVE_LEAVES);
        t.setPorosity(0);
        double cx = x + 1;
        double cz = z + 1;
        int trunkHeight = between(4, 6);
        t.trunk(cx, cz, y, y + trunkHeight, 1.0);
        surfaceRoots(cx, y, cz, between(4, 5), 1.2, 2.2, 1.2, 1, Material.MANGROVE_WOOD);
        // The branches only climb a few blocks before they reach the canopy
        int branchHeight = between(4, 5);
        int canopyY = y + trunkHeight + branchHeight;
        t.info("branches", branchHeight);

        List<double[]> tips = new ArrayList<>();
        int stems = between(4, 5);
        double start = randomAngle();
        for (int i = 0; i < stems; i++) {
            double yaw = start + i * Math.PI * 2 / stems + jitter(0.3);
            dragonFork(cx + Math.cos(yaw) * 0.6, y + trunkHeight, cz + Math.sin(yaw) * 0.6, yaw, 2, canopyY, tips);
        }
        // Short diagonal branches cross over the middle and hold the centre of the canopy, so it never has a hole
        int inner = between(2, 3);
        for (int i = 0; i < inner; i++) {
            double yaw = start + (i + 0.5) * Math.PI * 2 / inner + jitter(0.4);
            double sx = cx + Math.cos(yaw) * 0.8;
            double sz = cz + Math.sin(yaw) * 0.8;
            double ex = cx - Math.cos(yaw) * between(0.5, 1.5);
            double ez = cz - Math.sin(yaw) * between(0.5, 1.5);
            t.branch(sx, y + trunkHeight, sz, ex, canopyY, ez);
            t.wood((int) Math.floor(ex), canopyY + 1, (int) Math.floor(ez));
            tips.add(new double[] {ex, canopyY, ez});
        }

        // The canopy is a dense dome over the tips, thickest in the middle and a little ragged underneath its edge
        double reach = 0;
        for (double[] tip : tips) {
            reach = Math.max(reach, Math.hypot(tip[0] - cx, tip[2] - cz));
        }
        double radius = reach + 2.2;
        int range = (int) Math.ceil(radius) + 1;
        for (int bx = (int) Math.floor(cx) - range; bx <= cx + range; bx++) {
            for (int bz = (int) Math.floor(cz) - range; bz <= cz + range; bz++) {
                double distance = Math.hypot(bx + 0.5 - cx, bz + 0.5 - cz);
                if (distance > radius) {
                    continue;
                }
                double p = distance / radius;
                int bottom = canopyY + (p > 0.85 ? 1 : 0);
                int top = canopyY + (p < 0.45 ? 3 : p < 0.8 ? 2 : 1);
                for (int h = bottom; h <= top; h++) {
                    if (h == bottom && p > 0.55 && chance(0.25)) {
                        continue;
                    }
                    t.leaf(bx, h, bz);
                }
            }
        }
        resinClumps(between(3, 4), y + 1, y + trunkHeight, cx, cz, 1.6);
    }

    /**
     * Grows a Dragonblood branch that forks into two, over and over, rising steeply so that every tip reaches the canopy.
     */
    private void dragonFork(double x, double y, double z, double yaw, int depth, int canopyY, List<double[]> tips) {
        if (depth <= 0) {
            double out = between(0.8, 1.4);
            double ex = x + Math.cos(yaw) * out;
            double ez = z + Math.sin(yaw) * out;
            t.branch(x, y, z, ex, canopyY, ez);
            // The tip carries on into the canopy so that its leaves stay close to wood
            t.wood((int) Math.floor(ex), canopyY + 1, (int) Math.floor(ez));
            tips.add(new double[] {ex, canopyY, ez});
            return;
        }
        double rise = (canopyY - y) / (depth + 1.0);
        double out = between(1.3, 2.1);
        double ex = x + Math.cos(yaw) * out;
        double ey = y + rise;
        double ez = z + Math.sin(yaw) * out;
        t.branch(x, y, z, ex, ey, ez);
        double spread = between(0.45, 0.75);
        dragonFork(ex, ey, ez, yaw - spread, depth - 1, canopyY, tips);
        dragonFork(ex, ey, ez, yaw + spread, depth - 1, canopyY, tips);
    }

    /**
     * #59 Joshua Tree - grown from a single sapling. A short trunk that splits into 2 or 3 main stems, each forking into
     * side arms at odd angles, with a spiky rosette of needles bursting out of the end of every arm and no other foliage.
     */
    private void joshuaTree(int x, int y, int z) {
        t = new TreeBuilder(world, r, Material.STRIPPED_OAK_WOOD, Material.ACACIA_LEAVES);
        t.setLeafMix(Material.ACACIA_LEAVES, Material.JUNGLE_LEAVES, 0.3);
        t.setPorosity(0);
        double cx = x + 0.5;
        double cz = z + 0.5;
        int trunkHeight = between(2, 4);
        double tx = cx + jitter(0.4);
        double tz = cz + jitter(0.4);
        t.branch(cx, y, cz, tx, y + trunkHeight, tz);
        surfaceRoots(cx, y, cz, 3, 1.0, 1.8, 0.6, 1, Material.STRIPPED_OAK_WOOD);

        int stems = between(2, 3);
        t.info("stems", stems);
        double start = randomAngle();
        for (int i = 0; i < stems; i++) {
            double yaw = start + i * Math.PI * 2 / stems + jitter(0.5);
            double pitch = between(0.75, 1.15);
            double length = between(2.5, 4.0);
            double ex = tx + Math.cos(yaw) * Math.cos(pitch) * length;
            double ey = y + trunkHeight + Math.sin(pitch) * length;
            double ez = tz + Math.sin(yaw) * Math.cos(pitch) * length;
            t.branch(tx, y + trunkHeight, tz, ex, ey, ez);
            // Side arms leave the end of the stem, and sometimes its middle, at odd angles
            int arms = between(1, 3);
            for (int k = 0; k < arms; k++) {
                joshuaArm(ex, ey, ez, yaw + jitter(1.4), between(0.3, 1.3), between(1.5, 3.0), chance(0.3));
            }
            if (chance(0.4)) {
                double mx = tx + (ex - tx) * 0.55;
                double my = y + trunkHeight + (ey - y - trunkHeight) * 0.55;
                double mz = tz + (ez - tz) * 0.55;
                joshuaArm(mx, my, mz, yaw + (chance(0.5) ? 1 : -1) * between(0.8, 1.6), between(0.2, 0.9),
                        between(1.5, 2.5), false);
            }
        }
    }

    private void joshuaArm(double x, double y, double z, double yaw, double pitch, double length, boolean fork) {
        double ex = x + Math.cos(yaw) * Math.cos(pitch) * length;
        double ey = y + Math.sin(pitch) * length;
        double ez = z + Math.sin(yaw) * Math.cos(pitch) * length;
        t.branch(x, y, z, ex, ey, ez);
        if (fork) {
            for (int i = 0; i < 2; i++) {
                joshuaArm(ex, ey, ez, yaw + (i == 0 ? -1 : 1) * between(0.5, 0.9), between(0.6, 1.3), between(1.2, 2.0),
                        false);
            }
            return;
        }
        rosette((int) Math.floor(ex), (int) Math.floor(ey), (int) Math.floor(ez));
    }

    /**
     * Places a rosette of stiff needles bursting out of the end of a branch, each one a thin spike rather than a ball of
     * leaves.
     */
    private void rosette(int x, int y, int z) {
        t.wood(x, y, z);
        t.leaf(x, y + 1, z);
        int spikes = between(4, 6);
        double start = randomAngle();
        for (int i = 0; i < spikes; i++) {
            double yaw = start + i * Math.PI * 2 / spikes + jitter(0.3);
            // Most spikes point up and out, a few stick straight out sideways or slightly down
            double pitch = between(-0.2, 1.1);
            double length = between(1.2, 2.0);
            double ex = x + 0.5 + Math.cos(yaw) * Math.cos(pitch) * length;
            double ey = y + 0.5 + Math.sin(pitch) * length;
            double ez = z + 0.5 + Math.sin(yaw) * Math.cos(pitch) * length;
            t.line(x + 0.5, y + 0.5, z + 0.5, ex, ey, ez, (bx, by, bz, axis) -> t.leaf(bx, by, bz));
        }
        // A spike straight up the middle
        t.leaf(x, y + 2, z);
    }

    /**
     * #68 Flame Tree - grown from a 2x2 of saplings. Grows in one of four sizes, the way the real tree changes with age:
     * a young tree is a slim trunk under a small parasol, and as it ages its few thin limbs spread almost level, further
     * and further out, until an ancient tree is a short, flared trunk under a flat-topped umbrella of red blossom far
     * wider than it is tall. Its limbs stay thin at every size, only the trunk thickens.
     * @param size 0 Young, 1 Growing, 2 Mature, 3 Ancient, or -1 for a random size, mostly growing or mature.
     */
    private void flameTree(int x, int y, int z, int size) {
        if (size < 0) {
            double roll = r.nextDouble();
            size = roll < 0.2 ? 0 : roll < 0.5 ? 1 : roll < 0.85 ? 2 : 3;
        }
        String[] names = {"Young", "Growing", "Mature", "Ancient"};
        t = new TreeBuilder(world, r, Material.ACACIA_WOOD, Material.RED_POPLAR_LEAVES);
        t.setPorosity(0.08);
        t.info("idea", names[size] + "_Flame_Tree");
        double cx = x + 1;
        double cz = z + 1;
        int trunkHeight = between(new int[] {4, 4, 3, 4}[size], new int[] {5, 5, 4, 5}[size]);
        double baseRadius = new double[] {0.9, 1.0, 1.15, 1.35}[size];
        // How far the limbs reach out, and how far they climb on the way, by size
        double minReach = new double[] {2.5, 4.0, 5.5, 7.0}[size];
        double maxReach = new double[] {3.5, 5.0, 7.0, 9.0}[size];
        int limbs = between(new int[] {3, 3, 4, 5}[size], new int[] {3, 4, 5, 6}[size]);
        double rise = new double[] {2.5, 2.5, 3.0, 3.0}[size] + between(0.0, 1.0);
        double cluster = new double[] {1.8, 2.1, 2.4, 2.7}[size];
        int crownY = y + trunkHeight + (int) Math.round(rise);
        t.info("trunk", trunkHeight);
        // Below the crown's middle its underside is green and orange, above it the red blossom
        t.setLeafPicker((bx, by, bz) -> {
            double n = t.noise(bx / 2.0, by / 2.0, bz / 2.0, 5);
            if (by <= crownY) {
                return n < 0.3 ? Material.ACACIA_LEAVES : n < 0.6 ? Material.ORANGE_POPLAR_LEAVES : Material.RED_POPLAR_LEAVES;
            }
            return n < 0.88 ? Material.RED_POPLAR_LEAVES : Material.ORANGE_POPLAR_LEAVES;
        });
        // The trunk bends gently and tapers as it rises
        double leanYaw = randomAngle();
        double lean = between(0.3, 0.9);
        List<double[]> path = new ArrayList<>();
        for (int h = 0; h <= trunkHeight; h++) {
            double p = (double) h / trunkHeight;
            path.add(new double[] {cx + Math.cos(leanYaw) * lean * Math.sin(p * Math.PI / 2), y + h,
                    cz + Math.sin(leanYaw) * lean * Math.sin(p * Math.PI / 2)});
        }
        for (int i = 1; i < path.size(); i++) {
            double[] a = path.get(i - 1);
            double[] b = path.get(i);
            double radius = baseRadius - (baseRadius - 0.75) * i / trunkHeight;
            t.thickBranch(a[0], a[1], a[2], b[0], b[1], b[2], radius, radius);
        }
        if (size >= 2) {
            surfaceRoots(cx, y, cz, between(4, 6), 1.0, 1.5 + size, 1.2, 1, Material.ACACIA_WOOD);
        }
        if (size == 3) {
            // An ancient tree's base flares out unevenly into its roots
            int[][] flare = {{-1, 0}, {-1, 1}, {2, 0}, {2, 1}, {0, -1}, {1, -1}, {0, 2}, {1, 2}};
            for (int[] offset : flare) {
                int height = between(0, 2);
                for (int h = 0; h < height; h++) {
                    t.wood(x + offset[0], y + h, z + offset[1]);
                }
            }
        }

        // A few thin limbs leave the top of the trunk, climbing as they spread out almost level, the older the tree the
        // lower they split and the further they reach
        double[] top = path.get(path.size() - 1);
        double start = randomAngle();
        for (int i = 0; i < limbs; i++) {
            double angle = start + i * Math.PI * 2 / limbs + jitter(0.3);
            double[] from = pathAt(path, y + trunkHeight * between(size >= 2 ? 0.6 : 0.8, 1.0));
            double reach = between(minReach, maxReach);
            double mx = from[0] + Math.cos(angle) * reach * 0.45;
            double my = from[1] + (crownY - from[1]) * 0.75;
            double mz = from[2] + Math.sin(angle) * reach * 0.45;
            t.branch(from[0], from[1], from[2], mx, my, mz);
            // Bigger trees fork once at the end of each limb, a young one keeps a single tip
            int tips = size >= 2 ? 2 : 1;
            for (int f = 0; f < tips; f++) {
                double fork = tips == 1 ? angle : angle + (f == 0 ? -1 : 1) * between(0.25, 0.4);
                double ex = from[0] + Math.cos(fork) * reach;
                double ey = crownY - between(0.0, 0.8);
                double ez = from[2] + Math.sin(fork) * reach;
                t.branch(mx, my, mz, ex, ey, ez);
                t.blob(ex, ey + 1.0, ez, cluster * between(0.9, 1.15), 1.2, cluster * between(0.9, 1.15), 0.3);
            }
            // Leaves along the middle of each limb, so the umbrella is one sheet rather than separate tufts
            t.blob(mx, my + 1.0, mz, cluster * 0.9, 1.0, cluster * 0.9, 0.3);
        }
        // A low dome over the trunk, flat on top, joining the limbs' clusters
        t.branch(top[0], top[1], top[2], top[0], crownY, top[2]);
        t.blob(top[0], crownY + 1.0, top[2], minReach * 0.6 + 1, 1.3, minReach * 0.6 + 1, 0.3);
    }

    // ------------------------------------------------------------
    // Rare trees - Jungle
    // ------------------------------------------------------------

    /**
     * #50 Coconut Palm - grown from a single sapling. A slender, ringed trunk that curves out to one side, topped by a crown
     * of long arching fronds that droop at their tips, with coconuts clustered beneath.
     */
    private void coconutPalm(int x, int y, int z) {
        t = new TreeBuilder(world, r, Material.STRIPPED_JUNGLE_WOOD, Material.JUNGLE_LEAVES);
        t.setPorosity(0);
        double cx = x + 0.5;
        double cz = z + 0.5;
        // Most palms are of an ordinary height, but some old ones grow up to two thirds taller
        int height = chance(0.6) ? between(9, 14) : between(15, 23);
        double yaw = randomAngle();
        double lean = between(2.5, 4.5) * Math.max(1, height / 14.0);
        t.info("height", height);
        double px = cx;
        double pz = cz;
        double py = y;
        for (int h = 1; h <= height; h++) {
            double p = (double) h / height;
            double nx = cx + Math.cos(yaw) * lean * p * p;
            double nz = cz + Math.sin(yaw) * lean * p * p;
            t.line(px, py, pz, nx, y + h, nz, (bx, by, bz, axis) -> t.wood(bx, by, bz));
            px = nx;
            pz = nz;
            py = y + h;
        }
        int top = y + height;
        t.blob(px, top + 0.5, pz, 1.4, 0.8, 1.4, 0.1);
        int fronds = between(7, 9);
        double start = randomAngle();
        for (int i = 0; i < fronds; i++) {
            frond(px, top + 0.5, pz, start + i * Math.PI * 2 / fronds + jitter(0.25), between(5.0, 6.5),
                    between(0.5, 0.8), between(1.4, 1.9));
        }
        // A couple of young fronds still pointing upwards
        for (int i = 0; i < between(1, 2); i++) {
            frond(px, top + 1, pz, randomAngle(), between(2.5, 3.5), 1.5, 0.6);
        }
        cocoaPods(between(3, 6), top - 2);
    }

    /**
     * #51 Date Palm - grown from a single sapling. A very tall, dead straight trunk, a crown of stiff fronds pointing
     * upwards and outwards, a few thin dried fronds hanging beneath it, bunches of dates hanging from the top of the
     * trunk, and a ring of grass and ferns at its base.
     */
    private void datePalm(int x, int y, int z) {
        t = new TreeBuilder(world, r, Material.STRIPPED_JUNGLE_WOOD, Material.JUNGLE_LEAVES);
        t.setPorosity(0);
        double cx = x + 0.5;
        double cz = z + 0.5;
        int height = between(13, 18);
        t.info("height", height);
        for (int h = 0; h <= height; h++) {
            t.wood(x, y + h, z);
        }
        int top = y + height;
        t.blob(cx, top + 0.5, cz, 1.3, 0.8, 1.3, 0.1);
        int fronds = between(11, 14);
        double start = randomAngle();
        for (int i = 0; i < fronds; i++) {
            frond(cx, top + 0.5, cz, start + i * Math.PI * 2 / fronds + jitter(0.25), between(4.5, 6.0),
                    between(0.9, 1.3), between(0.6, 0.9));
        }
        // Dates hang from two sides of the top of the trunk, and dried fronds hang all around the rest of it, reaching 1 to
        // 2 blocks out from the trunk
        List<BlockFace> dateSides = new ArrayList<>(List.of(HORIZONTAL));
        Collections.shuffle(dateSides, r);
        dateSides = dateSides.subList(0, 2);
        t.setLeafPicker((bx, by, bz) -> t.noise(bx, by, bz, 5) < 0.5 ? Material.ORANGE_POPLAR_LEAVES
                : Material.YELLOW_POPLAR_LEAVES);
        for (int dx = -2; dx <= 2; dx++) {
            for (int dz = -2; dz <= 2; dz++) {
                int ring = Math.max(Math.abs(dx), Math.abs(dz));
                if (ring == 0 || (ring == 2 && (Math.abs(dx) == 2 && Math.abs(dz) == 2 || chance(0.5)))) {
                    continue;
                }
                boolean dateSpot = false;
                for (BlockFace face : dateSides) {
                    if (dx == face.getModX() && dz == face.getModZ()) {
                        dateSpot = true;
                        break;
                    }
                }
                if (dateSpot) {
                    continue;
                }
                int length = ring == 1 ? between(2, 3) : between(1, 2);
                for (int h = 0; h < length; h++) {
                    t.leaf(x + dx, top - 1 - h, z + dz);
                }
            }
        }
        for (BlockFace face : dateSides) {
            for (int h = 1; h <= 3; h++) {
                if (chance(0.55) && !t.has(x + face.getModX(), top - h, z + face.getModZ())) {
                    Cocoa date = (Cocoa) Material.COCOA.createBlockData();
                    date.setFacing(face.getOppositeFace());
                    date.setAge(between(1, date.getMaximumAge()));
                    t.decor(x + face.getModX(), top - h, z + face.getModZ(), date, -face.getModX(), 0, -face.getModZ());
                }
            }
        }
        groundCover(x, y, z, 4.5, 0.35, Material.SHORT_GRASS, true);
        groundCover(x, y, z, 3.5, 0.15, Material.FERN, true);
    }

    /**
     * #52 Rainbow Eucalyptus - grown from a 2x2 of saplings. A tall, smooth trunk with no branches for its lower half,
     * its peeling bark in long vertical strips of green, orange, red, teal and purple, that forks into two or three
     * leaders under a full, rounded crown.
     */
    private void rainbowEucalyptus(int x, int y, int z) {
        t = new TreeBuilder(world, r, Material.STRIPPED_JUNGLE_WOOD, Material.JUNGLE_LEAVES);
        t.setLeafMix(Material.JUNGLE_LEAVES, Material.OAK_LEAVES, 0.2);
        t.setPorosity(0.12);
        Material[] bark = {Material.STRIPPED_JUNGLE_WOOD, Material.STRIPPED_ACACIA_WOOD, Material.STRIPPED_WARPED_HYPHAE,
                Material.STRIPPED_CRIMSON_HYPHAE};
        double cx = x + 1;
        double cz = z + 1;
        int height = between(26, 36);
        int forkHeight = (int) (height * between(0.5, 0.6));
        for (int h = 0; h <= forkHeight; h++) {
            rainbowDisc(cx, y + h, cz, 1.0, bark);
        }
        // The base swells out into buttresses that climb several blocks up the trunk before they blend into it
        int flareHeight = between(5, 7);
        double flare = between(1.6, 2.2);
        int lobes = between(4, 5);
        double phase = randomAngle();
        for (int h = 0; h < flareHeight; h++) {
            double q = 1 - (double) h / flareHeight;
            int range = (int) Math.ceil(1 + flare) + 1;
            for (int bx = (int) Math.floor(cx) - range; bx <= cx + range; bx++) {
                for (int bz = (int) Math.floor(cz) - range; bz <= cz + range; bz++) {
                    double dx = bx + 0.5 - cx;
                    double dz = bz + 0.5 - cz;
                    double angle = Math.atan2(dz, dx);
                    double lobe = Math.max(0, Math.cos((angle - phase) * lobes));
                    double radius = 1.0 + flare * q * q * (0.3 + 0.7 * lobe * lobe)
                            + (t.noise(bx / 2.0, h / 3.0, bz / 2.0, 6) - 0.5) * 0.6 * q;
                    if (dx * dx + dz * dz <= radius * radius) {
                        t.wood(bx, y + h, bz, rainbowBark(bx, y + h, bz, bark));
                    }
                }
            }
        }
        surfaceRoots(cx, y, cz, between(5, 6), 2.0, 4.0, 1.0 + flare * 0.8, 2, Material.STRIPPED_JUNGLE_WOOD);
        t.info("height", height);

        // The trunk forks into leaders that lean apart, each tapering from 2 blocks wide to 1
        int leaders = between(2, 3);
        double start = randomAngle();
        LimbStyle crown = new LimbStyle(0.25, 0.02, 0.6, 0.6, 2, 2, 0.55, 0.7, 1.5, 3.0, 2.0, 0.5, 0.3, 0.5);
        for (int i = 0; i < leaders; i++) {
            double yaw = start + i * Math.PI * 2 / leaders + jitter(0.4);
            double lean = between(2.0, 4.0);
            int top = height - between(0, 3);
            double[] from = {cx, y + forkHeight, cz};
            double[] to = {cx + Math.cos(yaw) * lean, y + top, cz + Math.sin(yaw) * lean};
            for (int h = forkHeight; h <= top; h++) {
                double p = (double) (h - forkHeight) / (top - forkHeight);
                double px = from[0] + (to[0] - from[0]) * p * p;
                double pz = from[2] + (to[2] - from[2]) * p * p;
                rainbowDisc(px, y + h, pz, p < 0.35 ? 0.8 : 0.5, bark);
            }
            // Limbs climb out of the upper part of each leader, alternating around it
            double limbYaw = yaw;
            for (int h = forkHeight + 3; h < top - 1; h += between(2, 3)) {
                double p = (double) (h - forkHeight) / (top - forkHeight);
                limbYaw += GOLDEN_ANGLE + jitter(0.3);
                limb(from[0] + (to[0] - from[0]) * p * p, y + h, from[2] + (to[2] - from[2]) * p * p, limbYaw,
                        between(0.45, 0.8), between(4.0, 6.5), 1, crown);
            }
            t.blob(to[0], y + top + 1.5, to[2], 3.4, 2.4, 3.4, 0.3);
        }
    }

    /**
     * Fills a layer of a Rainbow Eucalyptus trunk, whose bark peels off in narrow strips that run a long way up it.
     */
    private void rainbowDisc(double cx, int y, double cz, double radius, Material[] palette) {
        int range = (int) Math.ceil(radius) + 1;
        for (int bx = (int) Math.floor(cx) - range; bx <= cx + range; bx++) {
            for (int bz = (int) Math.floor(cz) - range; bz <= cz + range; bz++) {
                double dx = bx + 0.5 - cx;
                double dz = bz + 0.5 - cz;
                if (dx * dx + dz * dz <= radius * radius + 0.01) {
                    t.wood(bx, y, bz, rainbowBark(bx, y, bz, palette));
                }
            }
        }
    }

    /**
     * Picks the bark colour of a Rainbow Eucalyptus block. Each column of the trunk keeps one colour for a long run
     * before it changes, so the bark reads as long vertical strips, with the plain base colour taking up the most room.
     */
    private Material rainbowBark(int bx, int y, int bz, Material[] palette) {
        double n = t.noise(bx * 3.1, y / 9.0, bz * 3.1, 4);
        int band = (int) Math.floor(n * (palette.length + 2) * 1.6) % (palette.length + 2);
        return palette[Math.max(0, band - 2)];
    }

    /**
     * #53 Strangler Fig - grown from a 3x3 of saplings. The tree it grew around rotted away long ago, leaving a hollow
     * of roots that wander up around it, merging and splitting, with gaps between them that can be climbed inside. Limbs
     * leave the upper part of the hollow at different heights and climb into a full, broad crown, with aerial roots
     * hanging back down from it.
     */
    private void stranglerFig(int x, int y, int z) {
        t = new TreeBuilder(world, r, Material.JUNGLE_WOOD, Material.JUNGLE_LEAVES);
        t.setLeafMix(Material.JUNGLE_LEAVES, Material.OAK_LEAVES, 0.15);
        t.setPorosity(0.12);
        double cx = x + 1.5;
        double cz = z + 1.5;
        int height = between(16, 24);
        double radius = between(2.4, 3.0);
        double entrance = randomAngle();
        t.info("height", height);

        // Each root wanders around the hollow as it climbs, and many stop part of the way up where they merge into another
        int strands = between(11, 14);
        List<double[]> tops = new ArrayList<>();
        double spread = randomAngle();
        for (int k = 0; k < strands; k++) {
            // Strands start spread evenly around the hollow, so no side is left bare
            double angle = spread + k * Math.PI * 2 / strands + jitter(0.25);
            double turn = jitter(0.12);
            double strandRadius = radius + jitter(0.35);
            int end = k < 5 ? height : (int) (height * between(0.6, 1.0));
            boolean thick = chance(0.6);
            double px = cx + Math.cos(angle) * strandRadius;
            double pz = cz + Math.sin(angle) * strandRadius;
            for (int h = 1; h <= end; h++) {
                turn = Math.max(-0.2, Math.min(0.2, turn + jitter(0.05)));
                angle += turn;
                double nr = strandRadius + 0.3 * Math.sin(h * 0.4 + k);
                double nx = cx + Math.cos(angle) * nr;
                double nz = cz + Math.sin(angle) * nr;
                // The way in at the bottom stays open
                boolean doorway = h < 4 && angleDifference(angle, entrance) < 0.5;
                if (!doorway) {
                    t.line(px, y + h - 1, pz, nx, y + h, nz, (bx, by, bz, axis) -> figRoot(bx, by, bz));
                    if (thick || h < 3) {
                        // Thick roots and the flared base are two blocks wide
                        int ox = (int) Math.floor(nx + Math.cos(angle) * 0.9);
                        int oz = (int) Math.floor(nz + Math.sin(angle) * 0.9);
                        if (h < 3 || chance(0.7)) {
                            figRoot(ox, y + h, oz);
                        }
                    }
                }
                px = nx;
                pz = nz;
            }
            if (end == height) {
                tops.add(new double[] {px, pz, angle});
            }
        }
        surfaceRoots(cx, y, cz, between(7, 9), 1.5, 3.5, radius, 2, Material.JUNGLE_WOOD);

        // Limbs leave the strands across the upper third of the hollow, each at its own height
        int top = y + height;
        LimbStyle crown = new LimbStyle(0.3, 0.02, 0.55, 0.6, 2, 2, 0.55, 0.7, 1.6, 3.2, 2.1, 0.5, 0.3, 0.45);
        int limbs = between(6, 9);
        double start = randomAngle();
        for (int i = 0; i < limbs; i++) {
            double angle = start + i * Math.PI * 2 / limbs + jitter(0.3);
            int h = top - between(0, (int) (height * 0.35));
            // Limbs start thick where they leave the hollow and only thin out towards their ends
            thickLimb(cx + Math.cos(angle) * radius, h, cz + Math.sin(angle) * radius, angle, between(0.35, 0.75),
                    between(6.0, 9.0), 2, crown, 0.9);
        }
        for (double[] strand : tops) {
            thickLimb(strand[0], top, strand[1], strand[2] + jitter(0.5), between(0.6, 1.0), between(4.0, 6.0), 1, crown,
                    0.7);
        }
        // The crown closes over the top of the hollow
        t.blob(cx, top + 3, cz, radius + 3.5, 2.6, radius + 3.5, 0.3);
        // Aerial roots hang down from the limbs, some all the way to the ground
        List<Pos> wood = new ArrayList<>(t.getPositions(Layer.LOG));
        wood.removeIf(pos -> pos.y() < top - 2 || Math.hypot(pos.x() + 0.5 - cx, pos.z() + 0.5 - cz) < radius + 2);
        Collections.shuffle(wood, r);
        int aerial = between(3, 6);
        for (Pos pos : wood) {
            if (aerial-- <= 0) {
                break;
            }
            int length = between(4, 10);
            for (int h = 1; h <= length; h++) {
                if (t.has(pos.x(), pos.y() - h, pos.z()) && h > 1) {
                    break;
                }
                t.wood(pos.x(), pos.y() - h, pos.z(), Material.MANGROVE_ROOTS);
            }
        }
        groundCover((int) Math.floor(cx), y, (int) Math.floor(cz), radius - 0.6, 0.6, Material.MOSS_CARPET, false);
        woodVines(y + 1, 0.1);
        hangingVines(0.08, 4, top);
        mossOnBranches(top - 4, 0.1);
    }

    /**
     * Places a block of a Strangler Fig root, mostly jungle wood with patches of mangrove roots.
     */
    private void figRoot(int x, int y, int z) {
        t.wood(x, y, z, t.noise(x / 1.5, y / 2.0, z / 1.5, 6) < 0.3 ? Material.MANGROVE_ROOTS : Material.JUNGLE_WOOD);
    }

    /**
     * #69 Tree Fern - grown from a single sapling. A thin, fuzzy trunk topped by a ring of long arching fronds like a small
     * prehistoric palm, with large ferns and moss around its base.
     */
    private void treeFern(int x, int y, int z) {
        t = new TreeBuilder(world, r, Material.JUNGLE_WOOD, Material.JUNGLE_LEAVES);
        t.setLeafMix(Material.JUNGLE_LEAVES, Material.AZALEA_LEAVES, 0.3);
        t.setPorosity(0);
        Material[] bark = {Material.JUNGLE_WOOD, Material.JUNGLE_WOOD, Material.MANGROVE_ROOTS};
        double cx = x + 0.5;
        double cz = z + 0.5;
        int height = between(4, 7);
        double tx = cx + jitter(0.8);
        double tz = cz + jitter(0.8);
        streakLine(cx, y, cz, tx, y + height, tz, bark, 1.5);
        // The top block is always wood, so that the fronds stay close enough to it not to decay
        t.wood((int) Math.floor(tx), y + height, (int) Math.floor(tz), Material.JUNGLE_WOOD);
        int fronds = between(8, 11);
        double start = randomAngle();
        for (int i = 0; i < fronds; i++) {
            frond(tx, y + height + 0.5, tz, start + i * Math.PI * 2 / fronds + jitter(0.25), between(3.5, 5.0), 0.8, 1.6);
        }
        for (int i = 0; i < between(2, 3); i++) {
            frond(tx, y + height + 1, tz, randomAngle(), 2.5, 1.6, 0.4);
        }
        largeFerns(x, y, z, between(3, 5), 1.5, 3.5);
        groundCover(x, y, z, 3.0, 0.4, Material.FERN, true);
        groundCover(x, y, z, 2.5, 0.4, Material.MOSS_CARPET, false);
    }

    // ------------------------------------------------------------
    // Rare trees - Spruce
    // ------------------------------------------------------------

    /**
     * #54 Giant Sequoia - grown from a 5x5 of saplings. An enormous reddish trunk with a flared base and wide surface
     * roots, bare for most of its height, with a small rounded crown at the very top. Now and then a fire has burnt a
     * walk-in hollow into its base.
     */
    private void giantSequoia(int x, int y, int z) {
        t = new TreeBuilder(world, r, Material.STRIPPED_MANGROVE_WOOD, Material.SPRUCE_LEAVES);
        t.setPorosity(0.08);
        Material[] bark = {Material.STRIPPED_MANGROVE_WOOD, Material.STRIPPED_MANGROVE_WOOD,
                Material.STRIPPED_MANGROVE_WOOD, Material.STRIPPED_MANGROVE_WOOD, Material.MANGROVE_WOOD};
        double cx = x + 2.5;
        double cz = z + 2.5;
        int height = between(70, 95);
        double flare = between(4.3, 4.7);
        double base = between(3.3, 3.6);
        double top = between(1.4, 1.7);
        boolean scar = chance(0.125);
        double scarYaw = randomAngle();
        int scarHeight = between(6, 9);
        t.info("height", height);
        t.info("scar", scar);
        int range = (int) Math.ceil(flare) + 1;
        for (int h = 0; h <= height; h++) {
            double p = (double) h / height;
            double radius = p < 0.1 ? flare + (base - flare) * Math.sin(p / 0.1 * Math.PI / 2)
                    : base + (top - base) * Math.pow((p - 0.1) / 0.9, 0.8);
            for (int bx = (int) Math.floor(cx) - range; bx <= cx + range; bx++) {
                for (int bz = (int) Math.floor(cz) - range; bz <= cz + range; bz++) {
                    double distance = Math.hypot(bx + 0.5 - cx, bz + 0.5 - cz);
                    if (distance > radius + 0.01) {
                        continue;
                    }
                    Material wood = streak(bx, y + h, bz, bark, 9);
                    if (scar && h < scarHeight) {
                        double angle = Math.atan2(bz + 0.5 - cz, bx + 0.5 - cx);
                        // The opening narrows towards its top, like an arch
                        double width = 0.5 * (1 - Math.pow((double) h / scarHeight, 2));
                        boolean opening = angleDifference(angle, scarYaw) < width && distance > 1.0;
                        boolean cavity = distance < radius - 1.2 && h < scarHeight - 1;
                        if (opening || cavity) {
                            continue;
                        }
                        // The walls of the hollow are charred
                        if (distance < radius - 0.2 || angleDifference(angle, scarYaw) < width + 0.3) {
                            wood = Material.DARK_OAK_WOOD;
                        }
                    }
                    t.wood(bx, y + h, bz, wood);
                }
            }
        }
        surfaceRoots(cx, y, cz, between(10, 13), 3.0, 7.0, 3.6, 1, Material.STRIPPED_MANGROVE_WOOD);
        // Burls bulging from the trunk
        for (int i = 0; i < between(2, 4); i++) {
            int h = between(10, (int) (height * 0.55));
            double angle = randomAngle();
            double p = (double) h / height;
            double radius = base + (top - base) * Math.pow((p - 0.1) / 0.9, 0.8);
            double bx = cx + Math.cos(angle) * radius;
            double bz = cz + Math.sin(angle) * radius;
            t.thickBranch(bx, y + h, bz, bx, y + h + 1, bz, 1.2, 1.0);
        }

        // A big, dense crown of stout limbs over the top third, rounded rather than pointed
        // Wide, flat sprays rather than round balls, the limbs bending down a little under their weight
        LimbStyle crown = new LimbStyle(0.3, 0.07, 0.2, 0.7, 2, 3, 0.55, 0.7, 1.6, 3.6, 1.5, 0.2, 0.45, 0.8);
        t.setPorosity(0);
        double yaw = randomAngle();
        int crownStart = y + (int) (height * 0.58);
        for (int h = crownStart; h < y + height; h += between(1, 2)) {
            double p = (double) (h - crownStart) / (y + height - crownStart);
            double radius = top + 0.8 * (1 - p);
            yaw += GOLDEN_ANGLE + jitter(0.3);
            // The lowest limbs are the thickest, thinning out towards the top of the crown
            thickLimb(cx + Math.cos(yaw) * radius, h, cz + Math.sin(yaw) * radius, yaw, between(0.05, 0.35),
                    5.0 + 6.0 * Math.sin((0.3 + p * 0.7) * Math.PI) + jitter(1.0), 2, crown, 1.4 - 0.7 * p);
        }
        t.blob(cx, y + height + 1, cz, 4.6, 2.6, 4.6, 0.45);
        t.blob(cx + jitter(1.5), y + height - 4, cz + jitter(1.5), 6.0, 2.2, 6.0, 0.45);
        sagLeaves(0.3, crownStart);
    }

    /**
     * #79 Redwood - grown from a 3x3 of saplings. Taller and slimmer than a sequoia, with dark, deeply furrowed red bark,
     * a bare trunk for its lower half with a few broken stubs, and a narrow, pointed crown of short drooping sprays.
     */
    private void redwood(int x, int y, int z) {
        t = new TreeBuilder(world, r, Material.STRIPPED_SPRUCE_WOOD, Material.SPRUCE_LEAVES);
        t.setLeafMix(Material.SPRUCE_LEAVES, Material.DARK_OAK_LEAVES, 0.2);
        t.setPorosity(0);
        // The furrows of the bark run straight up the trunk in long, narrow strips
        Material[] bark = {Material.STRIPPED_SPRUCE_WOOD, Material.STRIPPED_SPRUCE_WOOD, Material.STRIPPED_SPRUCE_WOOD,
                Material.MANGROVE_WOOD};
        double cx = x + 1.5;
        double cz = z + 1.5;
        int height = between(60, 85);
        double flare = between(2.6, 2.9);
        double base = between(1.8, 2.0);
        double top = 0.6;
        t.info("height", height);
        for (int h = 0; h <= height; h++) {
            double p = (double) h / height;
            double radius = p < 0.06 ? flare + (base - flare) * Math.sin(p / 0.06 * Math.PI / 2)
                    : base + (top - base) * Math.pow((p - 0.06) / 0.94, 1.1);
            int range = (int) Math.ceil(radius) + 1;
            for (int bx = (int) Math.floor(cx) - range; bx <= cx + range; bx++) {
                for (int bz = (int) Math.floor(cz) - range; bz <= cz + range; bz++) {
                    double dx = bx + 0.5 - cx;
                    double dz = bz + 0.5 - cz;
                    if (dx * dx + dz * dz <= radius * radius + 0.01) {
                        double n = t.noise(bx * 2.7, (y + h) / 8.0, bz * 2.7, 4);
                        t.wood(bx, y + h, bz, bark[(int) Math.floor(n * bark.length * 3) % bark.length]);
                    }
                }
            }
        }
        surfaceRoots(cx, y, cz, between(7, 9), 2.0, 4.5, 2.4, 1, Material.STRIPPED_SPRUCE_WOOD);
        // Broken stubs where lower branches fell off long ago
        for (int i = 0; i < between(3, 6); i++) {
            int h = between(8, (int) (height * 0.45));
            double angle = randomAngle();
            double radius = base + (top - base) * Math.pow((double) h / height, 1.1);
            t.branch(cx + Math.cos(angle) * radius, y + h, cz + Math.sin(angle) * radius,
                    cx + Math.cos(angle) * (radius + between(1.0, 2.5)), y + h - between(0.0, 1.0),
                    cz + Math.sin(angle) * (radius + between(1.0, 2.5)));
        }

        // A narrow, pointed crown of short sprays that droop at their tips, shortest towards the top
        int crownStart = y + (int) (height * between(0.45, 0.55));
        // A third of them keep their leaves further down the trunk
        if (chance(1.0 / 3)) {
            crownStart -= between(5, 10);
            t.info("low", true);
        }
        for (int h = crownStart; h < y + height - 1; h++) {
            if (chance(0.3)) {
                continue;
            }
            double p = (double) (h - crownStart) / (y + height - crownStart);
            double radius = base + (top - base) * Math.pow((double) (h - y) / height, 1.1);
            int sprays = between(2, 4);
            double start = randomAngle();
            for (int i = 0; i < sprays; i++) {
                double yaw = start + i * Math.PI * 2 / sprays + jitter(0.4);
                double length = radius + (4.5 * Math.sin((0.25 + p * 0.75) * Math.PI) + 1.5) * between(0.7, 1.1);
                spray(cx, h, cz, yaw, length, between(0.4, 0.8));
            }
        }
        spruceTop(cx, y + height, cz);
    }

    /**
     * #56 Bald Cypress - grown from a 2x2 of saplings. A wide, bell-shaped base of deep vertical flutes that spreads down
     * into the water, narrowing quickly into a tall, straight, slim trunk. Short, slender branches stick out sideways near
     * the top, holding a narrow, feathery crown, and long curtains of moss hang from them, with knees poking up out of
     * the water around it.
     */
    private void baldCypress(int x, int y, int z) {
        t = new TreeBuilder(world, r, Material.SPRUCE_WOOD, Material.SPRUCE_LEAVES);
        t.setLeafMix(Material.SPRUCE_LEAVES, Material.MANGROVE_LEAVES, 0.35);
        t.setPorosity(0.08);
        Material[] bark = {Material.SPRUCE_WOOD, Material.SPRUCE_WOOD, Material.STRIPPED_SPRUCE_WOOD};
        double cx = x + 1;
        double cz = z + 1;
        int height = between(20, 28);
        int flareHeight = between(5, 8);
        double flare = between(2.8, 3.4);
        int flutes = between(6, 8);
        double flutePhase = randomAngle();
        t.info("height", height);
        t.info("flare", flareHeight);

        // The bell-shaped base: wide at the water, narrowing fast, with deep grooves between its flutes
        for (int h = -1; h <= flareHeight; h++) {
            double p = Math.max(0, (double) h / flareHeight);
            double radius = 1.0 + (flare - 1.0) * (1 - p) * (1 - p);
            int range = (int) Math.ceil(radius) + 1;
            for (int bx = (int) Math.floor(cx) - range; bx <= cx + range; bx++) {
                for (int bz = (int) Math.floor(cz) - range; bz <= cz + range; bz++) {
                    double dx = bx + 0.5 - cx;
                    double dz = bz + 0.5 - cz;
                    double distance = Math.hypot(dx, dz);
                    double angle = Math.atan2(dz, dx);
                    // The grooves cut deepest near the water and fade out as the trunk narrows
                    double groove = 0.5 + 0.5 * Math.cos(flutes * angle + flutePhase);
                    double reach = 1.0 + (radius - 1.0) * (0.45 + 0.55 * groove);
                    if (distance > reach + 0.01) {
                        continue;
                    }
                    Material wood = bark[(int) Math.floor(t.noise(bx * 2.3, (y + h) / 4.0, bz * 2.3, 4) * 3 * bark.length)
                            % bark.length];
                    if (h < 1) {
                        t.root(bx, y + h, bz, wood, Axis.Y, true);
                    } else {
                        t.wood(bx, y + h, bz, wood);
                    }
                }
            }
        }
        // A straight, slim trunk above the base, narrowing from 2x2 to a single block near the top
        double upperX = x + r.nextInt(2) + 0.5;
        double upperZ = z + r.nextInt(2) + 0.5;
        int wideHeight = (int) (height * between(0.55, 0.7));
        for (int h = flareHeight + 1; h <= height; h++) {
            if (h <= wideHeight) {
                t.trunk(cx, cz, y + h, y + h, 1.0);
            } else {
                t.trunk(upperX, upperZ, y + h, y + h, 0.5);
            }
        }
        // Knees poking up out of the water around the tree
        int knees = between(6, 10);
        for (int i = 0; i < knees; i++) {
            double angle = randomAngle();
            double distance = between(3.5, 7.0);
            int kx = (int) Math.floor(cx + Math.cos(angle) * distance);
            int kz = (int) Math.floor(cz + Math.sin(angle) * distance);
            int kneeHeight = between(1, 3);
            for (int h = -1; h < kneeHeight; h++) {
                t.root(kx, y + h, kz, h == kneeHeight - 1 ? Material.STRIPPED_SPRUCE_WOOD : Material.SPRUCE_WOOD, Axis.Y, true);
            }
        }

        // Branches over the top third rise out of the trunk, then arch over and dip at their tips, each splitting into
        // feathery sprays so the crown is full rather than a few bare sticks
        LimbStyle feathery = new LimbStyle(0.4, 0.14, 0.15, 0.75, 2, 3, 0.6, 0.72, 1.4, 2.8, 1.5, 0.1, 0.45, 0.9);
        int crownBase = y + (int) (height * between(0.58, 0.66));
        double yaw = randomAngle();
        for (int h = crownBase; h < y + height - 1; h++) {
            yaw += GOLDEN_ANGLE + jitter(0.3);
            double p = (double) (h - crownBase) / (y + height - crownBase);
            double sx = h <= y + wideHeight ? cx : upperX;
            double sz = h <= y + wideHeight ? cz : upperZ;
            limb(sx, h, sz, yaw, between(0.35, 0.65), between(4.0, 6.5) * (1.1 - 0.45 * p), 2, feathery);
        }
        t.blob(upperX, y + height + 0.5, upperZ, 3.0, 2.2, 3.0, 0.4);
        // Leaves along the branches themselves, not only at their tips
        coverWood(crownBase + 1, 1.2);
        sagLeaves(0.3, crownBase);
        // Long curtains of moss hang from the leaves and from the undersides of the branches
        mossStrands(0.3, 7);
        for (Pos pos : t.getPositions(Layer.LOG)) {
            if (pos.y() >= crownBase && !t.has(pos.x(), pos.y() - 1, pos.z()) && chance(0.35)) {
                hangMoss(pos.x(), pos.y() - 1, pos.z(), between(2, 6));
            }
        }
    }

    /**
     * #62 Pencil Cypress - grown from a single sapling. A tall, narrow, dense column of green that tapers to a soft point,
     * with its wood hidden inside.
     */
    private void pencilCypress(int x, int y, int z) {
        t = new TreeBuilder(world, r, Material.SPRUCE_WOOD, Material.SPRUCE_LEAVES);
        t.setLeafMix(Material.SPRUCE_LEAVES, Material.DARK_OAK_LEAVES, 0.3);
        t.setPorosity(0);
        double cx = x + 0.5;
        double cz = z + 0.5;
        int height = between(12, 20);
        double width = between(1.4, 1.7);
        t.trunk(cx, cz, y, y + height - 3, 0.5);
        t.info("height", height);
        for (int h = 1; h <= height; h++) {
            double p = (double) h / height;
            double radius = p < 0.65 ? width * (0.85 + 0.15 * Math.sin(p / 0.65 * Math.PI / 2))
                    : width * Math.cos((p - 0.65) / 0.35 * Math.PI / 2) + 0.3;
            if (h == 1) {
                radius = width * 0.7;
            }
            t.disc(cx, y + h, cz, radius, 0.15);
        }
        t.leaf(x, y + height + 1, z);
    }

    /**
     * #63 Great Cedar - grown from a 3x3 of saplings. A massive trunk that splits into a few huge limbs spreading far out
     * to the sides from its lower third to its top, the lowest reaching furthest, each limb carrying its own flat shelves
     * of foliage at its own heights, so that the shelves are staggered up a tall crown rather than stacked in even layers.
     */
    private void greatCedar(int x, int y, int z) {
        t = new TreeBuilder(world, r, Material.STRIPPED_DARK_OAK_WOOD, Material.SPRUCE_LEAVES);
        t.setLeafMix(Material.SPRUCE_LEAVES, Material.DARK_OAK_LEAVES, 0.25);
        t.setPorosity(0);
        double cx = x + 1.5;
        double cz = z + 1.5;
        // One in four is an old giant, 6 blocks taller and a little wider
        boolean giant = chance(0.25);
        int trunkHeight = between(11, 15) + (giant ? 6 : 0);
        for (int h = 0; h <= trunkHeight; h++) {
            // Thick at the base, narrowing a little towards the crown
            t.trunk(cx, cz, y + h, y + h, 1.5 - 0.3 * h / trunkHeight);
        }
        surfaceRoots(cx, y, cz, between(7, 9), 2.0, 4.5, 1.6, 2, Material.STRIPPED_DARK_OAK_WOOD);
        double scale = between(0.9, 1.15) * (giant ? 1.2 : 1);
        t.info("scale", scale);
        t.info("giant", giant);

        // Limbs leave the trunk all the way from its lower third to its top, the lowest reaching furthest, so the crown is
        // a tall stack of staggered shelves that narrows upwards rather than a cap on top of a bare trunk
        int limbs = between(8, 11);
        double yaw = randomAngle();
        for (int i = 0; i < limbs; i++) {
            double q = (double) i / (limbs - 1);
            yaw += GOLDEN_ANGLE + jitter(0.35);
            double sy = y + trunkHeight * (0.3 + 0.65 * q) + jitter(0.8);
            double pitch = between(0.12, 0.35);
            double length = (11.0 - 5.5 * q) * between(0.85, 1.1) * scale;
            double ex = cx + Math.cos(yaw) * Math.cos(pitch) * length;
            double ey = sy + Math.sin(pitch) * length;
            double ez = cz + Math.sin(yaw) * Math.cos(pitch) * length;
            t.thickBranch(cx, sy, cz, ex, ey, ez, 1.1 - 0.4 * q, 0.5);
            // Shelves sit along the limb wherever it has reached, so each one is at its own height
            int shelves = q < 0.6 ? between(2, 3) : between(1, 2);
            for (int k = 0; k < shelves; k++) {
                double p = shelves == 1 ? 0.85 : 0.45 + 0.55 * k / (shelves - 1) + jitter(0.06);
                double px = cx + (ex - cx) * p;
                double pz = cz + (ez - cz) * p;
                int py = (int) Math.floor(sy + (ey - sy) * p) + between(0, 1);
                cedarShelf(px, py, pz, between(2.8, 4.0) * scale * (1.0 - 0.35 * q) * (0.8 + 0.3 * p),
                        yaw + jitter(0.6));
            }
        }
        // Two smaller shelves over the top of the trunk, so the crown is flat-topped with no leader sticking out
        int topShelf = y + trunkHeight + between(2, 3);
        t.branch(cx, y + trunkHeight, cz, cx, topShelf, cz);
        cedarShelf(cx, topShelf, cz, between(2.6, 3.3) * scale, randomAngle());
        cedarShelf(cx, y + trunkHeight - 1, cz, between(3.4, 4.3) * scale, randomAngle());
        removeLoneLeaves();
    }

    /**
     * Places a flat shelf of cedar foliage, a little longer along the limb it sits on, with hidden spokes beneath it that
     * keep its leaves close enough to wood.
     */
    private void cedarShelf(double x, int y, double z, double radius, double yaw) {
        int spokes = 5;
        for (int s = 0; s < spokes; s++) {
            double sa = yaw + s * Math.PI * 2 / spokes;
            t.branch(x, y, z, x + Math.cos(sa) * radius * 0.65, y, z + Math.sin(sa) * radius * 0.65);
        }
        double along = radius * 1.15;
        double across = radius * 0.9;
        double ca = Math.abs(Math.cos(yaw));
        double sa = Math.abs(Math.sin(yaw));
        double rx = ca * along + sa * across;
        double rz = sa * along + ca * across;
        // Two solid layers, the upper one smaller, with softly uneven edges so the shelf is rounded but never has holes
        for (int layer = 0; layer < 2; layer++) {
            double scale = layer == 0 ? 1.0 : 0.62;
            int range = (int) Math.ceil(Math.max(rx, rz)) + 1;
            for (int bx = (int) Math.floor(x) - range; bx <= x + range; bx++) {
                for (int bz = (int) Math.floor(z) - range; bz <= z + range; bz++) {
                    double nx = (bx + 0.5 - x) / (rx * scale);
                    double nz = (bz + 0.5 - z) / (rz * scale);
                    double edge = nx * nx + nz * nz + (t.noise(bx / 2.0, y + layer, bz / 2.0, 7) - 0.5) * 0.4;
                    if (edge <= 1) {
                        t.leaf(bx, y + 1 + layer, bz);
                    }
                }
            }
        }
    }

    /**
     * #64 Monkey Puzzle - grown from a 2x2 of saplings. A straight trunk with whorls of long, rope-like branches, each
     * wrapped in leaves along its whole length like a furry tail and curving upwards at the tip, under a flattened dome.
     */
    private void monkeyPuzzle(int x, int y, int z) {
        t = new TreeBuilder(world, r, Material.SPRUCE_WOOD, Material.SPRUCE_LEAVES);
        t.setLeafMix(Material.SPRUCE_LEAVES, Material.JUNGLE_LEAVES, 0.4);
        t.setPorosity(0);
        double cx = x + 1;
        double cz = z + 1;
        int height = between(18, 26);
        int wideHeight = (int) (height * 0.5);
        t.trunk(cx, cz, y, y + wideHeight, 1.0);
        double upperX = x + r.nextInt(2) + 0.5;
        double upperZ = z + r.nextInt(2) + 0.5;
        t.trunk(upperX, upperZ, y + wideHeight, y + height, 0.5);
        surfaceRoots(cx, y, cz, between(4, 5), 1.2, 2.5, 1.2, 2, Material.SPRUCE_WOOD);
        t.info("height", height);

        double start = randomAngle();
        for (int h = y + (int) (height * 0.35); h < y + height - 2; h += 3) {
            double p = (double) (h - y) / height;
            int branches = between(5, 6);
            start += 0.5;
            for (int i = 0; i < branches; i++) {
                double yaw = start + i * Math.PI * 2 / branches + jitter(0.2);
                double length = between(4.0, 6.5) * (1.15 - 0.6 * p);
                double sx = h <= y + wideHeight ? cx : upperX;
                double sz = h <= y + wideHeight ? cz : upperZ;
                double mx = sx + Math.cos(yaw) * length * 0.7;
                double mz = sz + Math.sin(yaw) * length * 0.7;
                double ex = sx + Math.cos(yaw) * length;
                double ez = sz + Math.sin(yaw) * length;
                tail(sx, h, sz, mx, h - 0.5, mz);
                tail(mx, h - 0.5, mz, ex, h + 1.5, ez);
            }
        }
        t.blob(upperX, y + height, upperZ, 2.8, 1.6, 2.8, 0.2);
        for (int i = 0; i < 4; i++) {
            double yaw = start + i * Math.PI / 2 + jitter(0.3);
            tail(upperX, y + height - 1, upperZ, upperX + Math.cos(yaw) * 2.5, y + height + 1, upperZ + Math.sin(yaw) * 2.5);
        }
    }

    /**
     * Draws a branch wrapped tightly in leaves along its whole length, like a furry tail.
     */
    private void tail(double x0, double y0, double z0, double x1, double y1, double z1) {
        t.line(x0, y0, z0, x1, y1, z1, (bx, by, bz, axis) -> {
            t.log(bx, by, bz, axis);
            for (BlockFace face : new BlockFace[] {BlockFace.UP, BlockFace.DOWN, BlockFace.NORTH, BlockFace.EAST,
                    BlockFace.SOUTH, BlockFace.WEST}) {
                t.leaf(bx + face.getModX(), by + face.getModY(), bz + face.getModZ());
            }
        });
    }

    /**
     * #65 Dead Snag - grown from a single sapling. A dead, leafless conifer with a snapped-off top, short broken stubs of
     * branches and its bark long gone, with pale moss hanging from the stubs.
     */
    private void deadSnag(int x, int y, int z) {
        t = new TreeBuilder(world, r, Material.STRIPPED_DARK_OAK_WOOD, Material.SPRUCE_LEAVES);
        int height = between(6, 11);
        for (int h = 0; h <= height; h++) {
            t.wood(x, y + h, z);
        }
        // The broken top is jagged
        if (chance(0.7)) {
            BlockFace face = HORIZONTAL[r.nextInt(4)];
            t.wood(x + face.getModX(), y + height, z + face.getModZ());
            if (chance(0.5)) {
                t.wood(x + face.getModX(), y + height + 1, z + face.getModZ());
            }
        }
        surfaceRoots(x + 0.5, y, z + 0.5, between(3, 4), 1.0, 2.0, 0.6, 2, Material.STRIPPED_DARK_OAK_WOOD);
        t.info("height", height);

        int stubs = between(4, 7);
        for (int i = 0; i < stubs; i++) {
            int h = between(2, height - 1);
            double yaw = randomAngle();
            double length = between(1.0, 2.5);
            double ex = x + 0.5 + Math.cos(yaw) * length;
            double ey = y + h + between(-0.6, 0.4);
            double ez = z + 0.5 + Math.sin(yaw) * length;
            t.branch(x + 0.5, y + h, z + 0.5, ex, ey, ez);
            int bx = (int) Math.floor(ex);
            int by = (int) Math.floor(ey);
            int bz = (int) Math.floor(ez);
            if (t.has(bx, by, bz) && chance(0.6)) {
                hangMoss(bx, by - 1, bz, between(1, 3));
            }
        }
        shelfMushrooms(between(1, 3), y + 1, y + height);
    }

    /**
     * #78 Soul Pine - grown from a single sapling in the Nether. A thin, pale, crooked conifer with ragged tiers of pale
     * needles, like the ghost of a pine, with soul soil spreading around its roots.
     */
    private void soulPine(int x, int y, int z) {
        t = new TreeBuilder(world, r, Material.STRIPPED_POPLAR_WOOD, Material.PALE_OAK_LEAVES);
        t.setPorosity(0);
        double cx = x + 0.5;
        double cz = z + 0.5;
        int height = between(10, 15);
        List<double[]> path = bentTrunk(cx, y, cz, height, 4, 1.3, null);
        t.info("height", height);
        for (int h = y + 3; h < y + height - 1; h += between(2, 3)) {
            double p = (double) (h - y) / height;
            double[] point = pathAt(path, h);
            int sprays = between(2, 4);
            double start = randomAngle();
            for (int i = 0; i < sprays; i++) {
                spray(point[0], h, point[2], start + i * Math.PI * 2 / sprays + jitter(0.6),
                        (3.0 * (1 - p) + 0.8) * between(0.6, 1.2), 0.3);
            }
        }
        double[] topPoint = path.get(path.size() - 1);
        spruceTop(topPoint[0], y + height, topPoint[2]);
        groundSoil(cx, y, cz, 3.0, 0.8, Material.SOUL_SOIL);
    }

    // ------------------------------------------------------------
    // Rare trees - Oak, Birch, Poplar and Cherry
    // ------------------------------------------------------------

    /**
     * #57 Windswept Hawthorn - grown from a single sapling. A trunk bent hard over by the wind with its crown combed into
     * a flat wedge pointing downwind. They all lean roughly east, like a natural compass, each a little differently.
     */
    private void windsweptHawthorn(int x, int y, int z) {
        t = new TreeBuilder(world, r, Material.OAK_WOOD, Material.OAK_LEAVES);
        t.setLeafMix(Material.OAK_LEAVES, Material.FLOWERING_AZALEA_LEAVES, 0.12);
        t.setPorosity(0.12);
        double wind = WIND_YAW + jitter(0.35);
        t.info("lean", Math.toDegrees(wind));
        double wx = Math.cos(wind);
        double wz = Math.sin(wind);
        double cx = x + 0.5;
        double cz = z + 0.5;
        int height = between(4, 6);
        double bend = between(2.5, 4.0);
        double mx = cx + wx * bend * 0.3;
        double mz = cz + wz * bend * 0.3;
        double my = y + height * 0.5;
        double tx = cx + wx * bend;
        double tz = cz + wz * bend;
        double ty = y + height;
        t.branch(cx, y, cz, mx, my, mz);
        t.branch(mx, my, mz, tx, ty, tz);
        // A low limb reaching downwind
        t.branch(mx, my, mz, mx + wx * 3 + wz * jitter(1), my + 1, mz + wz * 3 + wx * jitter(1));
        surfaceRoots(cx, y, cz, 3, 1.0, 2.0, 0.6, 2, Material.OAK_WOOD);

        for (int i = 0; i <= 4; i++) {
            double p = i / 4.0;
            double bx = tx + wx * (p * 5 - 1);
            double bz = tz + wz * (p * 5 - 1);
            double along = 2.4 * (1 - 0.3 * p);
            double across = 2.6 * (1 - 0.4 * p);
            t.blob(bx, ty + 0.5 - p * 0.8, bz, Math.abs(wx) * along + Math.abs(wz) * across, 1.4 * (1 - 0.45 * p),
                    Math.abs(wz) * along + Math.abs(wx) * across, 0.3);
        }
        // The wind strips the upwind side and flattens the top
        for (Pos leaf : t.getPositions(Layer.LEAF)) {
            double along = (leaf.x() + 0.5 - tx) * wx + (leaf.z() + 0.5 - tz) * wz;
            if (along < -1.5 || leaf.y() > ty + 2) {
                t.removeLeaf(leaf.x(), leaf.y(), leaf.z());
            }
        }
    }

    /**
     * #71 Giant Bonsai - grown from a single sapling. A miniature ancient tree, only a few blocks tall, with a thick,
     * twisting trunk and flat cloud pads of leaves with clear air beneath each one.
     */
    private void giantBonsai(int x, int y, int z) {
        t = new TreeBuilder(world, r, Material.OAK_WOOD, Material.OAK_LEAVES);
        t.setLeafMix(Material.OAK_LEAVES, Material.AZALEA_LEAVES, 0.3);
        t.setPorosity(0);
        Material[] bark = {Material.OAK_WOOD, Material.OAK_WOOD, Material.DARK_OAK_WOOD};
        double cx = x + 0.5;
        double cz = z + 0.5;
        int height = between(4, 6);
        double phase = randomAngle();
        double[] top = {cx, y + height, cz};
        for (int h = 0; h <= height; h++) {
            double p = (double) h / height;
            double ox = 0.8 * Math.sin(p * Math.PI * 1.3 + phase);
            double oz = 0.8 * Math.cos(p * Math.PI * 1.1 + phase);
            streakDisc(cx + ox, y + h, cz + oz, 1.05 - 0.55 * p, bark, 2);
            top = new double[] {cx + ox, y + h, cz + oz};
        }
        surfaceRoots(cx, y, cz, between(3, 4), 1.0, 2.0, 0.8, 1, Material.OAK_WOOD);

        int pads = between(2, 4);
        double start = randomAngle();
        for (int i = 0; i < pads; i++) {
            double yaw = start + i * Math.PI * 2 / pads + jitter(0.4);
            int h = (int) Math.round(height * between(0.5, 0.9));
            double p = (double) h / height;
            double sx = cx + 0.8 * Math.sin(p * Math.PI * 1.3 + phase);
            double sz = cz + 0.8 * Math.cos(p * Math.PI * 1.1 + phase);
            double length = between(2.0, 3.5);
            double ex = sx + Math.cos(yaw) * length;
            double ez = sz + Math.sin(yaw) * length;
            streakLine(sx, y + h, sz, ex, y + h + 0.5, ez, bark, 2);
            double radius = between(2.0, 2.6);
            t.blob(ex, y + h + 1.5, ez, radius, 0.8, radius, 0.15);
        }
        t.blob(top[0], top[1] + 1.5, top[2], 2.8, 0.9, 2.8, 0.15);
    }

    /**
     * #72 Mycelial Oak - grown from a single sapling. A Spreading Oak taken over by fungus, with shelf mushrooms climbing
     * its trunk, mushroom caps growing from its branches and mycelium spreading around its roots.
     */
    private void mycelialOak(int x, int y, int z) {
        spreadingOak(x, y, z);
        double cx = x + 0.5;
        double cz = z + 0.5;
        int trunkHeight = 3;
        // Small mushroom caps growing out of the tops of branches
        List<Pos> wood = new ArrayList<>(t.getPositions(Layer.LOG));
        wood.removeIf(pos -> pos.y() < y + trunkHeight);
        Collections.shuffle(wood, r);
        int caps = between(1, 2);
        for (Pos pos : wood) {
            if (caps <= 0) {
                break;
            }
            // Caps push their way out through the leaves, but never through wood
            boolean blocked = false;
            for (int dx = -1; dx <= 1 && !blocked; dx++) {
                for (int dy = 1; dy <= 3 && !blocked; dy++) {
                    for (int dz = -1; dz <= 1; dz++) {
                        if (t.has(pos.x() + dx, pos.y() + dy, pos.z() + dz)
                                && !t.isLeaf(pos.x() + dx, pos.y() + dy, pos.z() + dz)) {
                            blocked = true;
                            break;
                        }
                    }
                }
            }
            if (blocked) {
                continue;
            }
            for (int dx = -1; dx <= 1; dx++) {
                for (int dy = 1; dy <= 3; dy++) {
                    for (int dz = -1; dz <= 1; dz++) {
                        t.removeLeaf(pos.x() + dx, pos.y() + dy, pos.z() + dz);
                    }
                }
            }
            boolean red = chance(0.5);
            t.solid(pos.x(), pos.y() + 1, pos.z(), Material.MUSHROOM_STEM);
            Material cap = red ? Material.RED_MUSHROOM_BLOCK : Material.BROWN_MUSHROOM_BLOCK;
            for (int dx = -1; dx <= 1; dx++) {
                for (int dz = -1; dz <= 1; dz++) {
                    // Red caps are rounded, brown caps are flat squares
                    if (!red || Math.abs(dx) + Math.abs(dz) <= 1) {
                        t.solid(pos.x() + dx, pos.y() + 2, pos.z() + dz, cap);
                    }
                }
            }
            if (red) {
                t.solid(pos.x(), pos.y() + 3, pos.z(), cap);
            }
            caps--;
        }
        shelfMushrooms(between(3, 5), y + 1, y + trunkHeight + 2);
        groundSoil(cx, y, cz, between(3.5, 4.5), 0.85, Material.MYCELIUM);
        groundCover(x, y, z, 4.0, 0.06, Material.RED_MUSHROOM, true);
        groundCover(x, y, z, 4.0, 0.08, Material.BROWN_MUSHROOM, true);
    }

    /**
     * #73 Orchard Oak - grown from a single sapling. A short, tidy fruit tree whose clear trunk splits into a few low
     * limbs under a neat rounded crown, with spots of blossom and a few petals on the ground.
     */
    private void orchardOak(int x, int y, int z) {
        t = new TreeBuilder(world, r, Material.OAK_WOOD, Material.OAK_LEAVES);
        // Blossom is sprinkled evenly over the crown, a block here and there, rather than in patches
        t.setLeafPicker((bx, by, bz) -> t.noise(bx * 5.7, by * 5.7, bz * 5.7, 3) < 0.36
                ? Material.FLOWERING_AZALEA_LEAVES : Material.OAK_LEAVES);
        t.setPorosity(0.12);
        double cx = x + 0.5;
        double cz = z + 0.5;
        int trunkHeight = between(2, 3);
        t.branch(cx, y, cz, cx, y + trunkHeight, cz);
        LimbStyle style = new LimbStyle(0.15, 0.02, 0.6, 0.5, 1, 1, 0.6, 0.7, 99, 2.2, 1.8, 0.4, 0.12, 0);
        int limbs = between(3, 4);
        double start = randomAngle();
        for (int i = 0; i < limbs; i++) {
            limb(cx, y + trunkHeight, cz, start + i * Math.PI * 2 / limbs + jitter(0.3), between(0.55, 0.8),
                    between(2.5, 3.5), 0, style);
        }
        t.blob(cx, y + trunkHeight + 2.2, cz, 3.0, 2.3, 3.0, 0.12);
        groundCover(x, y, z, 3.5, 0.2, Material.PINK_PETALS, true);
    }

    /**
     * #77 Ember Oak - grown from a single sapling in the Nether. A pale-barked oak with a big, airy crown of red, orange
     * and yellow leaves like embers, on a trunk that flares out into its roots.
     */
    private void emberOak(int x, int y, int z) {
        t = new TreeBuilder(world, r, Material.STRIPPED_POPLAR_WOOD, Material.RED_POPLAR_LEAVES);
        t.setPorosity(0);
        // 40% red, 15% orange and 10% yellow, with the remaining 35% left as air further down
        t.setLeafPicker((bx, by, bz) -> {
            double n = r.nextDouble() * 0.65;
            return n < 0.40 ? Material.RED_POPLAR_LEAVES : n < 0.55 ? Material.ORANGE_POPLAR_LEAVES
                    : Material.YELLOW_POPLAR_LEAVES;
        });
        double scale = between(1.2, 1.5);
        double cx = x + 0.5;
        double cz = z + 0.5;
        int height = (int) Math.round(between(4.0, 5.0) * scale);
        double tx = cx + jitter(0.5);
        double tz = cz + jitter(0.5);
        t.branch(cx, y, cz, tx, y + height, tz);
        t.info("scale", scale);
        // The base flares out into the roots, higher on some sides than others
        for (BlockFace face : HORIZONTAL) {
            int rise = between(1, 3);
            for (int h = 0; h < rise; h++) {
                t.wood(x + face.getModX(), y + h, z + face.getModZ());
            }
            // Some sides spread a second block out at ground level
            if (chance(0.5)) {
                t.wood(x + face.getModX() * 2, y, z + face.getModZ() * 2);
            }
        }
        for (int dx = -1; dx <= 1; dx += 2) {
            for (int dz = -1; dz <= 1; dz += 2) {
                if (chance(0.5)) {
                    t.wood(x + dx, y, z + dz);
                }
            }
        }
        surfaceRoots(cx, y, cz, between(5, 7), 1.5, 3.0, 1.2, 1, Material.STRIPPED_POPLAR_WOOD);

        // Built like a spreading oak, only bigger
        t.blob(tx, y + height + 0.5, tz, 2.6 * scale, 2.1 * scale, 2.6 * scale, 0.2);
        LimbStyle style = new LimbStyle(0.2, 0.03, 0.6, 0.5, 1, 2, 0.6, 0.7, 2.2, 2.5, 2.0, 0.5, 0.2, 0.3);
        int limbs = between(4, 5);
        double start = randomAngle();
        for (int i = 0; i < limbs; i++) {
            limb(tx, y + height - between(0, 1), tz, start + i * Math.PI * 2 / limbs + jitter(0.4), between(0.5, 0.9),
                    between(2.0, 2.8) * scale, 1, style);
        }
        limb(tx, y + height, tz, randomAngle(), 1.35, between(1.6, 2.3) * scale, 0, style);
        // The crown is airy, with about a third of it left open, though leaves touching the wood stay to hide it
        for (Pos pos : t.getPositions(Layer.LEAF)) {
            if (chance(0.35) && !touchesWood(pos)) {
                t.removeLeaf(pos.x(), pos.y(), pos.z());
            }
        }
    }

    /**
     * #61 Snow Gum - grown from a single sapling. A single crooked trunk that zig-zags upwards, its bark streaked cream,
     * grey, orange and red, with sparse, drooping foliage.
     */
    private void snowGum(int x, int y, int z) {
        t = new TreeBuilder(world, r, Material.BIRCH_WOOD, Material.BIRCH_LEAVES);
        t.setPorosity(0.1);
        // Mostly smooth cream bark, with some grey and the odd streak of orange where old bark is peeling
        Material[] bark = {Material.STRIPPED_BIRCH_WOOD, Material.STRIPPED_BIRCH_WOOD, Material.STRIPPED_BIRCH_WOOD,
                Material.BIRCH_WOOD, Material.STRIPPED_BIRCH_WOOD, Material.STRIPPED_BIRCH_WOOD,
                Material.STRIPPED_ACACIA_WOOD};
        double px = x + 0.5;
        double pz = z + 0.5;
        double py = y;
        double yaw = randomAngle();
        int segments = between(3, 4);
        for (int i = 0; i < segments; i++) {
            double dir = yaw + (i % 2 == 0 ? 0 : Math.PI) + jitter(0.5);
            double out = between(1.0, 2.0);
            double nx = px + Math.cos(dir) * out;
            double nz = pz + Math.sin(dir) * out;
            double ny = py + between(2.0, 3.0);
            streakLine(px, py, pz, nx, ny, nz, bark, 6);
            if (i > 0) {
                // A short side branch at each bend, ending in its own clump of leaves
                double sx = nx + Math.cos(dir) * 1.8;
                double sz = nz + Math.sin(dir) * 1.8;
                t.branch(nx, ny, nz, sx, ny + 0.8, sz);
                t.blob(sx, ny + 1.2, sz, 2.4, 1.4, 2.4, 0.3);
            }
            px = nx;
            pz = nz;
            py = ny;
        }
        LimbStyle droopy = new LimbStyle(0.3, 0.08, 0.3, 0.6, 2, 2, 0.6, 0.7, 1.5, 2.6, 1.5, 0.3, 0.3, 0.5);
        for (int i = 0; i < between(3, 4); i++) {
            limb(px, py, pz, randomAngle(), between(0.4, 0.8), between(3.0, 4.0), 1, droopy);
        }
        t.blob(px, py + 1.5, pz, 2.8, 1.8, 2.8, 0.3);
        // The foliage droops at its edges
        for (Pos leaf : t.getPositions(Layer.LEAF)) {
            if (!t.has(leaf.x(), leaf.y() - 1, leaf.z()) && chance(0.2)) {
                t.leaf(leaf.x(), leaf.y() - 1, leaf.z());
            }
        }
    }

    // ------------------------------------------------------------
    // Rare trees - Dark Oak and Pale Oak
    // ------------------------------------------------------------

    /**
     * #66 Witch Tree - grown from a single sapling. A crooked, leaning trunk with long hooked branches reaching out like
     * fingers and only a few small clumps of leaves, its roots gripping the ground, with mushrooms and cobwebs in its forks.
     */
    private void witchTree(int x, int y, int z) {
        startOak(false);
        t.setPorosity(0.3);
        Material[] bark = {Material.DARK_OAK_WOOD, Material.DARK_OAK_WOOD, Material.STRIPPED_DARK_OAK_WOOD};
        double cx = x + 0.5;
        double cz = z + 0.5;
        int height = between(7, 10);
        List<double[]> path = bentTrunk(cx, y, cz, height, 3, 1.4, bark);
        surfaceRoots(cx, y, cz, between(4, 6), 1.5, 3.0, 0.6, 2, Material.DARK_OAK_WOOD);
        t.info("height", height);

        List<double[]> forks = new ArrayList<>();
        int branches = between(4, 6);
        double start = randomAngle();
        int clumps = between(2, 4);
        for (int i = 0; i < branches; i++) {
            double yaw = start + i * Math.PI * 2 / branches + jitter(0.4);
            double[] from = pathAt(path, y + height * between(0.45, 1.0));
            // Out, up into a hook, then curling back in like a finger
            double ax = from[0] + Math.cos(yaw) * between(2.5, 4.0);
            double ay = from[1] + between(0.5, 1.5);
            double az = from[2] + Math.sin(yaw) * between(2.5, 4.0);
            double hook = yaw + jitter(0.5);
            double bx = ax + Math.cos(hook) * between(1.5, 2.5);
            double by = ay + between(1.5, 2.5);
            double bz = az + Math.sin(hook) * between(1.5, 2.5);
            double ex = bx - Math.cos(yaw);
            double ey = by - 1;
            double ez = bz - Math.sin(yaw);
            streakLine(from[0], from[1], from[2], ax, ay, az, bark, 4);
            streakLine(ax, ay, az, bx, by, bz, bark, 4);
            streakLine(bx, by, bz, ex, ey, ez, bark, 4);
            forks.add(new double[] {ax, ay, az});
            if (clumps > 0 && chance(0.6)) {
                t.blob(bx, by + 0.5, bz, 1.3, 1.0, 1.3, 0.4);
                clumps--;
            }
        }
        // Mushrooms on top of the forks and cobwebs strung beside them
        int mushrooms = between(2, 3);
        int cobwebs = between(1, 2);
        for (double[] fork : forks) {
            int fx = (int) Math.floor(fork[0]);
            int fy = (int) Math.floor(fork[1]);
            int fz = (int) Math.floor(fork[2]);
            if (!t.has(fx, fy, fz)) {
                continue;
            }
            if (mushrooms > 0 && !t.has(fx, fy + 1, fz)) {
                t.decor(fx, fy + 1, fz, Material.BROWN_MUSHROOM.createBlockData(), 0, -1, 0);
                mushrooms--;
            } else if (cobwebs > 0) {
                BlockFace face = HORIZONTAL[r.nextInt(4)];
                if (!t.has(fx + face.getModX(), fy - 1, fz + face.getModZ())) {
                    t.decor(fx + face.getModX(), fy - 1, fz + face.getModZ(), Material.COBWEB.createBlockData(),
                            -face.getModX(), 1, -face.getModZ());
                    cobwebs--;
                }
            }
        }
    }

    /**
     * #67 Olive Tree - grown from a single pale oak sapling. A short, wide, twisted trunk with deep hollows and gaps as if
     * it were centuries old, under a loose, rounded crown of silvery leaves.
     */
    private void oliveTree(int x, int y, int z) {
        t = new TreeBuilder(world, r, Material.STRIPPED_DARK_OAK_WOOD, Material.PALE_OAK_LEAVES);
        t.setPorosity(0.3);
        double cx = x + 0.5;
        double cz = z + 0.5;
        int height = between(3, 5);
        double phase = randomAngle();
        double[] top = {cx, y + height, cz};
        for (int h = 0; h <= height; h++) {
            double ox = 0.5 * Math.cos(h * 0.8 + phase);
            double oz = 0.5 * Math.sin(h * 0.8 + phase);
            double radius = 1.5 - 0.15 * h;
            double tcx = cx + ox;
            double tcz = cz + oz;
            List<int[]> ring = new ArrayList<>();
            List<int[]> gaps = new ArrayList<>();
            for (int bx = (int) Math.floor(tcx) - 2; bx <= tcx + 2; bx++) {
                for (int bz = (int) Math.floor(tcz) - 2; bz <= tcz + 2; bz++) {
                    double distance = Math.hypot(bx + 0.5 - tcx, bz + 0.5 - tcz);
                    if (distance > radius) {
                        continue;
                    }
                    // Hollow in the middle with gaps through the sides, except at the very top where the limbs start
                    if (h < height && distance < radius - 0.9) {
                        continue;
                    }
                    boolean gap = h > 0 && h < height && t.noise(bx / 1.5, h / 1.5, bz / 1.5, 6) > 0.62;
                    (gap ? gaps : ring).add(new int[] {bx, bz});
                }
            }
            // Gaps never take more than a third of a layer, so the trunk always holds together
            while (ring.size() < (ring.size() + gaps.size()) * 2 / 3 && !gaps.isEmpty()) {
                ring.add(gaps.remove(r.nextInt(gaps.size())));
            }
            for (int[] block : ring) {
                t.wood(block[0], y + h, block[1]);
            }
            top = new double[] {tcx, y + h, tcz};
        }
        // Limbs climb steeply out of the trunk under a tall, rounded crown
        LimbStyle style = new LimbStyle(0.3, 0.03, 0.55, 0.6, 1, 2, 0.6, 0.7, 1.8, 2.4, 1.6, 0.4, 0.35, 0.4);
        int limbs = between(4, 6);
        double start = randomAngle();
        for (int i = 0; i < limbs; i++) {
            limb(top[0], top[1], top[2], start + i * Math.PI * 2 / limbs + jitter(0.4), between(0.5, 0.9),
                    between(2.5, 4.0), 1, style);
        }
        t.blob(top[0], top[1] + 2.5, top[2], 3.6, 2.2, 3.6, 0.35);
        // Every branch in the crown is wrapped in leaves, so none show bare
        coverWood(y + height + 1, 1.3);
    }

    // ------------------------------------------------------------
    // Rare trees - Nether fungi
    // ------------------------------------------------------------

    /**
     * #75 Flame Helix - grown from a single crimson fungus. The corkscrew Crimson Helix that a lone crimson fungus grows
     * into, its spikes made of leaves coloured like a flame, dark at the bottom and yellow at the top.
     */
    private void flameHelix(int x, int y, int z) {
        t = new TreeBuilder(world, r, Material.CRIMSON_HYPHAE, Material.RED_POPLAR_LEAVES);
        t.setPorosity(0);
        double cx = x + 0.5;
        double cz = z + 0.5;
        int height = between(10, 16);
        int bottom = y + 2;
        int topY = y + height + 2;
        t.setLeafPicker((bx, by, bz) -> {
            double p = (by - bottom) / (double) Math.max(1, topY - bottom) + (r.nextDouble() - 0.5) * 0.25;
            return p < 0.2 ? Material.NETHER_WART_BLOCK : p < 0.5 ? Material.RED_POPLAR_LEAVES
                    : p < 0.8 ? Material.ORANGE_POPLAR_LEAVES : Material.YELLOW_POPLAR_LEAVES;
        });
        double twist = between(0.45, 0.65) * (chance(0.5) ? 1 : -1);
        double angle = randomAngle();
        double coil = between(1.0, 1.6);
        double px = cx;
        double pz = cz;
        t.info("height", height);
        double[] last = {cx, y, cz};
        for (int h = 1; h <= height; h++) {
            angle += twist;
            // The coil tightens towards the top
            double radius = coil * (1 - 0.6 * h / height) * Math.min(1, h / 3.0);
            double nx = cx + Math.cos(angle) * radius;
            double nz = cz + Math.sin(angle) * radius;
            t.branch(px, y + h - 1, pz, nx, y + h, nz);
            last = new double[] {nx, y + h, nz};
            if (h >= 3 && h % 2 == 0 && chance(0.75)) {
                double spikeAngle = angle + jitter(0.4);
                double length = Math.min(2.6, between(1.5, 3.0) * (1.1 - 0.5 * h / height)) + 0.5;
                double ex = nx + Math.cos(spikeAngle) * length;
                double ey = y + h + between(1.2, 2.0);
                double ez = nz + Math.sin(spikeAngle) * length;
                List<int[]> spike = new ArrayList<>();
                // Each spike of leaves is carried by a hidden core of stem
                t.line(nx, y + h, nz, ex, ey, ez, (bx, by, bz, axis) -> {
                    t.wood(bx, by, bz);
                    for (BlockFace face : HORIZONTAL) {
                        t.leaf(bx + face.getModX(), by, bz + face.getModZ());
                    }
                    t.leaf(bx, by + 1, bz);
                    spike.add(new int[] {bx, by, bz});
                });
                int[] tip = spike.get(spike.size() - 1);
                t.leaf(tip[0], tip[1] + 2, tip[2]);
                // The shroomlight hangs beneath the tip, with vines trailing from it
                if (!t.has(tip[0], tip[1] - 1, tip[2])) {
                    t.solid(tip[0], tip[1] - 1, tip[2], Material.SHROOMLIGHT);
                    weepingVines(tip[0], tip[1] - 2, tip[2], between(1, 4));
                }
                for (int[] block : spike) {
                    if (chance(0.3)) {
                        weepingVines(block[0], block[1] - 1, block[2], between(1, 3));
                    }
                }
            }
            px = nx;
            pz = nz;
        }
        t.leaf((int) Math.floor(last[0]), y + height + 1, (int) Math.floor(last[2]));
        t.leaf((int) Math.floor(last[0]), y + height + 2, (int) Math.floor(last[2]));
    }

    /**
     * #76 Warped Bloom - grown from a 2x2 of warped fungi. A stem that rises out of a ring of gnarled roots and curves
     * gently to one side, then opens at the top into a giant, ragged flower: uneven petals of wart lined with warped
     * nylium, held up from beneath by struts of stripped hyphae, around a cluster of glowing stamens.
     */
    private void warpedBloom(int x, int y, int z) {
        t = new TreeBuilder(world, r, Material.WARPED_HYPHAE, Material.WARPED_WART_BLOCK);
        double cx = x + 1;
        double cz = z + 1;
        int height = between(8, 12);
        double bendYaw = randomAngle();
        double bend = between(2.0, 3.5);
        t.info("height", height);
        // The stem curves smoothly to one side, thick at the base and thinner towards the flower
        double px = cx;
        double pz = cz;
        double py = y;
        for (int h = 1; h <= height; h++) {
            double p = (double) h / height;
            double nx = cx + Math.cos(bendYaw) * bend * p * p;
            double nz = cz + Math.sin(bendYaw) * bend * p * p;
            double radius = 1.1 - 0.4 * p;
            t.thickBranch(px, py, pz, nx, y + h, nz, radius + 0.05, radius);
            px = nx;
            pz = nz;
            py = y + h;
        }
        // A ring of gnarled roots around the base, rising unevenly up the stem
        int roots = between(5, 7);
        double rootStart = randomAngle();
        for (int i = 0; i < roots; i++) {
            double angle = rootStart + i * Math.PI * 2 / roots + jitter(0.4);
            double rise = between(1.0, 3.0);
            t.thickRoot(cx + Math.cos(angle) * 0.8, y + rise, cz + Math.sin(angle) * 0.8,
                    cx + Math.cos(angle) * between(2.5, 3.8), y - 1, cz + Math.sin(angle) * between(2.5, 3.8),
                    0.7, 0.3, Material.WARPED_HYPHAE);
        }
        double fx = px;
        double fz = pz;
        int capBase = y + height;

        // Ragged petals of uneven length and width, each curving up from the centre
        int petals = between(6, 8);
        double phase = randomAngle();
        double[] petalAngle = new double[petals];
        double[] petalLength = new double[petals];
        double[] petalWidth = new double[petals];
        double[] petalLift = new double[petals];
        for (int k = 0; k < petals; k++) {
            petalAngle[k] = phase + k * Math.PI * 2 / petals + jitter(0.25);
            petalLength[k] = between(4.5, 7.5);
            petalWidth[k] = Math.PI / petals * between(0.6, 0.95);
            petalLift[k] = between(2.5, 4.5);
        }
        double maxLength = 7.5;
        int range = (int) Math.ceil(maxLength) + 1;
        // The lowest and highest block of every column of the flower, to fill any gaps left between them afterwards
        Map<Long, int[]> columns = new HashMap<>();
        for (int bx = (int) Math.floor(fx) - range; bx <= fx + range; bx++) {
            for (int bz = (int) Math.floor(fz) - range; bz <= fz + range; bz++) {
                double distance = Math.hypot(bx + 0.5 - fx, bz + 0.5 - fz);
                if (distance > maxLength) {
                    continue;
                }
                if (distance <= 1.8) {
                    // The base of the flower, a plate of wart under the stamens
                    t.solid(bx, capBase, bz, Material.WARPED_WART_BLOCK);
                    columns.put(columnKey(bx, bz), new int[] {capBase, capBase});
                    continue;
                }
                double angle = Math.atan2(bz + 0.5 - fz, bx + 0.5 - fx);
                int petal = -1;
                double nearest = Double.MAX_VALUE;
                for (int k = 0; k < petals; k++) {
                    double difference = angleDifference(angle, petalAngle[k]);
                    if (difference < nearest) {
                        nearest = difference;
                        petal = k;
                    }
                }
                double p = (distance - 1.5) / (petalLength[petal] - 1.5);
                if (p > 1) {
                    continue;
                }
                // Each petal widens from the centre to its middle and then closes in a rounded end, like the end of an
                // ellipse, so its outer edge curves smoothly instead of being cut square
                double width = p < 0.5 ? petalWidth[petal] * (0.55 + 0.45 * Math.sin(Math.PI * p))
                        : petalWidth[petal] * Math.sqrt(Math.max(0, 1 - Math.pow((p - 0.5) / 0.5, 2)));
                // Near the middle the petals overlap, so no slits open between them around the stamens
                if (p < 0.35) {
                    width = Math.max(width, Math.PI / petals * 1.1);
                }
                if (nearest > width) {
                    continue;
                }
                int top = capBase + (int) Math.round(Math.pow(p, 1.7) * petalLift[petal]);
                int below = capBase + (int) Math.round(Math.pow(Math.max(0, p - 0.25), 1.7) * petalLift[petal]);
                // The underside of the inner part of each petal has a rib of stripped hyphae running along its middle,
                // flush with the petal, like a support holding it up
                int bottom = Math.min(below, top);
                boolean rib = nearest < width * 0.4 && p < 0.65;
                for (int h = bottom; h <= top; h++) {
                    if (h == bottom && rib && h < top) {
                        t.wood(bx, h, bz, Material.STRIPPED_WARPED_HYPHAE);
                    } else {
                        t.solid(bx, h, bz, Material.WARPED_WART_BLOCK);
                    }
                }
                if (rib && bottom == top) {
                    t.wood(bx, bottom - 1, bz, Material.STRIPPED_WARPED_HYPHAE);
                }
                columns.put(columnKey(bx, bz), new int[] {bottom, top});
            }
        }
        // Any empty column surrounded by the flower on four sides is a gap, and so is one on three sides that is well
        // inside a petal's length, so it is filled to match them. Spots near a petal's end are left, keeping its end round
        for (int pass = 0; pass < 2; pass++) {
            Map<Long, int[]> filled = new HashMap<>();
            for (int bx = (int) Math.floor(fx) - range; bx <= fx + range; bx++) {
                for (int bz = (int) Math.floor(fz) - range; bz <= fz + range; bz++) {
                    if (columns.containsKey(columnKey(bx, bz))) {
                        continue;
                    }
                    int count = 0;
                    int low = Integer.MAX_VALUE;
                    int high = Integer.MIN_VALUE;
                    for (BlockFace face : HORIZONTAL) {
                        int[] column = columns.get(columnKey(bx + face.getModX(), bz + face.getModZ()));
                        if (column != null) {
                            count++;
                            low = Math.min(low, column[1]);
                            high = Math.max(high, column[1]);
                        }
                    }
                    boolean inside = count == 4;
                    if (count == 3) {
                        double distance = Math.hypot(bx + 0.5 - fx, bz + 0.5 - fz);
                        double angle = Math.atan2(bz + 0.5 - fz, bx + 0.5 - fx);
                        int petal = 0;
                        for (int k = 1; k < petals; k++) {
                            if (angleDifference(angle, petalAngle[k]) < angleDifference(angle, petalAngle[petal])) {
                                petal = k;
                            }
                        }
                        inside = distance <= 1.5 + (petalLength[petal] - 1.5) * 0.7;
                    }
                    if (inside) {
                        int top = Math.min(high, low + 1);
                        for (int h = low; h <= top; h++) {
                            t.solid(bx, h, bz, Material.WARPED_WART_BLOCK);
                        }
                        filled.put(columnKey(bx, bz), new int[] {low, top});
                    }
                }
            }
            columns.putAll(filled);
        }
        // Stamens of stripped hyphae rise out of the centre, each tipped with a glowing shroomlight
        int stamens = between(3, 5);
        for (int i = 0; i < stamens; i++) {
            double angle = randomAngle();
            double out = between(0.0, 1.3);
            int sx = (int) Math.floor(fx + Math.cos(angle) * out);
            int sz = (int) Math.floor(fz + Math.sin(angle) * out);
            int stamenHeight = between(1, 3);
            for (int h = 1; h <= stamenHeight; h++) {
                t.wood(sx, capBase + h, sz, Material.STRIPPED_WARPED_HYPHAE);
            }
            t.solid(sx, capBase + stamenHeight + 1, sz, Material.SHROOMLIGHT);
        }
        // Twisting vines climb from the inner faces of a few petals
        for (Pos pos : t.getPositions(Layer.LEAF)) {
            if (t.typeAt(pos.x(), pos.y(), pos.z()) == Material.WARPED_WART_BLOCK && pos.y() > capBase
                    && !t.has(pos.x(), pos.y() + 1, pos.z()) && chance(0.05)) {
                int length = between(1, 3);
                for (int h = 1; h <= length; h++) {
                    Material vine = h == length ? Material.TWISTING_VINES : Material.TWISTING_VINES_PLANT;
                    if (!t.decor(pos.x(), pos.y() + h, pos.z(), vine.createBlockData(), 0, -1, 0)) {
                        break;
                    }
                }
            }
        }
    }

    // ------------------------------------------------------------
    // Rare trees - The End
    // ------------------------------------------------------------

    /**
     * #80 Lantern Lily - grown from a 2x2 of pale oak saplings. A giant bundle of lily of the valley: two to five pale
     * stalks rise from one base between two broad leaves, each arching over at the top, with a big white bell hanging
     * from its tip and sometimes a smaller one from its side. Every bell is open underneath, a pearlescent froglight
     * glowing at its heart where it can be seen from below.
     */
    private void lanternLily(int x, int y, int z) {
        t = new TreeBuilder(world, r, Material.PALE_OAK_WOOD, Material.PALE_OAK_LEAVES);
        t.setPorosity(0);
        double cx = x + 1;
        double cz = z + 1;
        t.trunk(cx, cz, y, y + 1, 1.0);
        surfaceRoots(cx, y, cz, between(3, 4), 1.0, 2.0, 1.2, 1, Material.PALE_OAK_WOOD);
        int stalks = between(2, 5);
        int tallest = between(14, 18);
        double start = randomAngle();
        t.info("stalks", stalks);
        t.info("height", tallest);
        for (int i = 0; i < stalks; i++) {
            // Stalks are spread evenly around the base, so their bells hang well apart from each other
            double yaw = start + i * Math.PI * 2 / stalks + jitter(0.25);
            int height = i == 0 ? tallest : tallest - between(2, 6);
            double reach = between(5.0, 6.5);
            // The stalk rises from the base leaning a little outwards, then arches over and down at the top
            double sx = cx + Math.cos(yaw) * 0.6;
            double sz = cz + Math.sin(yaw) * 0.6;
            double[] p0 = {sx, y + 1, sz};
            double[] p1 = {sx + Math.cos(yaw) * reach * 0.3, y + height + 3, sz + Math.sin(yaw) * reach * 0.3};
            double[] p2 = {sx + Math.cos(yaw) * reach, y + height - 1, sz + Math.sin(yaw) * reach};
            List<double[]> arch = new ArrayList<>();
            double[] previous = p0;
            int steps = height + 4;
            for (int k = 1; k <= steps; k++) {
                double u = (double) k / steps;
                double[] point = new double[3];
                for (int a = 0; a < 3; a++) {
                    point[a] = (1 - u) * (1 - u) * p0[a] + 2 * u * (1 - u) * p1[a] + u * u * p2[a];
                }
                t.branch(previous[0], previous[1], previous[2], point[0], point[1], point[2]);
                arch.add(point);
                previous = point;
            }
            lilyBell(p2[0], p2[1], p2[2], between(2.0, 2.3));
            // A smaller bell on a short side stem from the arch
            if (chance(0.6)) {
                double[] at = arch.get((int) (arch.size() * between(0.6, 0.68)));
                double side = yaw + (chance(0.5) ? 1 : -1) * Math.PI / 2;
                double bx = at[0] + Math.cos(side) * 2.8;
                double by = at[1] - 1.5;
                double bz = at[2] + Math.sin(side) * 2.8;
                t.branch(at[0], at[1], at[2], bx, by, bz);
                lilyBell(bx, by, bz, between(1.4, 1.7));
            }
        }
        // Two broad leaves curl up from the base, as a lily of the valley's do
        double leafStart = randomAngle();
        for (int k = 0; k < 2; k++) {
            double yaw = leafStart + k * Math.PI + jitter(0.4);
            double side = yaw + Math.PI / 2;
            double length = between(4.0, 6.0);
            t.line(cx, y + 1, cz, cx + Math.cos(yaw) * 2.2, y + 1 + length, cz + Math.sin(yaw) * 2.2,
                    (bx, by, bz, axis) -> {
                        double p = (by - y - 1) / length;
                        int width = p < 0.15 || p > 0.85 ? 0 : 1;
                        for (int w = -width; w <= width; w++) {
                            t.leaf(bx + (int) Math.round(Math.cos(side) * w), by, bz + (int) Math.round(Math.sin(side) * w));
                        }
                    });
        }
    }

    /**
     * Hangs a bell of a Lantern Lily beneath the tip of its stem: a rounded, solid top, then walls open at the bottom
     * that flare out a little at the mouth, with a pearlescent froglight hanging at its heart. A hidden core of stem runs
     * down into the top so every leaf of the bell stays close enough to wood.
     * @param y The height of the stem's tip, which the bell hangs beneath.
     */
    private void lilyBell(double x, double y, double z, double radius) {
        int centreX = (int) Math.floor(x);
        int centreZ = (int) Math.floor(z);
        int top = (int) Math.floor(y) - 1;
        // The top two rows are solid and round, the two below them are hollow walls that open at the bottom
        double[] rowRadius = {radius * 0.6, radius * 0.92, radius, radius + 0.5};
        int range = (int) Math.ceil(radius) + 2;
        for (int row = 0; row < rowRadius.length; row++) {
            int by = top - row;
            for (int bx = centreX - range; bx <= centreX + range; bx++) {
                for (int bz = centreZ - range; bz <= centreZ + range; bz++) {
                    double d = Math.hypot(bx + 0.5 - x, bz + 0.5 - z);
                    boolean wall = row >= 2;
                    if (d <= rowRadius[row] && (!wall || d > rowRadius[row] - 1.1)) {
                        t.leaf(bx, by, bz);
                    }
                }
            }
        }
        t.branch(x, y, z, centreX + 0.5, top - 1, centreZ + 0.5);
        t.wood(centreX, top, centreZ);
        t.wood(centreX, top - 1, centreZ);
        t.solid(centreX, top - 2, centreZ, Material.PEARLESCENT_FROGLIGHT);
    }

    /**
     * #85 Bone Spine - grown from a single pale oak sapling. A dead, twisted white trunk with no leaves at all, bristling
     * with short, sharp twigs all the way up and splitting into a few crooked prongs at the top, a few wisps of pale moss
     * hanging from it.
     */
    private void boneSpine(int x, int y, int z) {
        t = new TreeBuilder(world, r, Material.STRIPPED_PALE_OAK_WOOD, Material.PALE_OAK_LEAVES);
        double cx = x + 0.5;
        double cz = z + 0.5;
        int height = between(10, 14);
        List<double[]> path = bentTrunk(cx, y, cz, height, 4, 1.0, null);
        surfaceRoots(cx, y, cz, between(3, 4), 1.0, 2.5, 0.6, 1, Material.STRIPPED_PALE_OAK_WOOD);
        for (int h = 2; h < height; h++) {
            if (!chance(0.6)) {
                continue;
            }
            double[] from = pathAt(path, y + h);
            double yaw = randomAngle();
            double pitch = between(0.3, 0.9);
            double length = between(1.5, 3.0) * (1.1 - 0.4 * h / height);
            double ex = from[0] + Math.cos(yaw) * Math.cos(pitch) * length;
            double ey = from[1] + Math.sin(pitch) * length;
            double ez = from[2] + Math.sin(yaw) * Math.cos(pitch) * length;
            t.branch(from[0], from[1], from[2], ex, ey, ez);
            if (chance(0.15)) {
                hangMoss((int) Math.floor(ex), (int) Math.floor(ey) - 1, (int) Math.floor(ez), between(1, 3));
            }
        }
        double[] top = path.get(path.size() - 1);
        int prongs = between(2, 3);
        double start = randomAngle();
        for (int i = 0; i < prongs; i++) {
            double yaw = start + i * Math.PI * 2 / prongs + jitter(0.4);
            double mx = top[0] + Math.cos(yaw) * 1.5;
            double mz = top[2] + Math.sin(yaw) * 1.5;
            double my = top[1] + between(1.5, 2.5);
            t.branch(top[0], top[1], top[2], mx, my, mz);
            t.branch(mx, my, mz, mx + Math.cos(yaw + 0.6) * 1.2, my + between(1.5, 2.5), mz + Math.sin(yaw + 0.6) * 1.2);
        }
    }

    /**
     * #84 Bone Sprawl - grown from a single pale oak sapling. The Bone Spine's low, spreading sister: a short, thin, dead
     * white trunk that throws out three or four slender, crooked limbs almost level over the ground, a few sharp twigs
     * along them and curling up at their ends, with wisps of pale moss hanging beneath them. No leaves at all.
     */
    private void boneSprawl(int x, int y, int z) {
        t = new TreeBuilder(world, r, Material.STRIPPED_PALE_OAK_WOOD, Material.PALE_OAK_LEAVES);
        double cx = x + 0.5;
        double cz = z + 0.5;
        int height = between(3, 4);
        t.info("height", height);
        List<double[]> path = bentTrunk(cx, y, cz, height, 2, 0.6, null);
        surfaceRoots(cx, y, cz, between(2, 3), 1.0, 2.0, 0.6, 1, Material.STRIPPED_PALE_OAK_WOOD);
        double[] top = path.get(path.size() - 1);
        int limbs = between(3, 4);
        double start = randomAngle();
        for (int i = 0; i < limbs; i++) {
            double yaw = start + i * Math.PI * 2 / limbs + jitter(0.35);
            double[] from = i == 0 ? top : pathAt(path, y + height * between(0.5, 1.0));
            double length = between(4.0, 6.0);
            int segments = 3;
            double px = from[0];
            double py = from[1];
            double pz = from[2];
            // Each limb zigzags outwards in a few crooked segments, nearly level, then curls up at its end
            for (int k = 1; k <= segments; k++) {
                double heading = yaw + jitter(0.45);
                double step = length / segments;
                double nx = px + Math.cos(heading) * step;
                double ny = py + (k == segments ? between(1.5, 2.5) : between(-0.3, 0.8));
                double nz = pz + Math.sin(heading) * step;
                t.branch(px, py, pz, nx, ny, nz);
                // A short, sharp twig bristles from most segments
                int twigs = chance(0.6) ? 1 : 0;
                for (int w = 0; w < twigs; w++) {
                    double f = between(0.3, 1.0);
                    double tx = px + (nx - px) * f;
                    double ty = py + (ny - py) * f;
                    double tz = pz + (nz - pz) * f;
                    double twigYaw = heading + (chance(0.5) ? 1 : -1) * between(0.6, 1.3);
                    double twigPitch = between(0.2, 0.9);
                    double twigLength = between(1.0, 1.8);
                    t.branch(tx, ty, tz, tx + Math.cos(twigYaw) * Math.cos(twigPitch) * twigLength,
                            ty + Math.sin(twigPitch) * twigLength, tz + Math.sin(twigYaw) * Math.cos(twigPitch) * twigLength);
                }
                if (k < segments && chance(0.3)) {
                    hangMoss((int) Math.floor(nx), (int) Math.floor(ny) - 1, (int) Math.floor(nz), between(1, 3));
                }
                px = nx;
                py = ny;
                pz = nz;
            }
        }
    }

    /**
     * #83 Halo Tree - grown from a 2x2 of poplar saplings. A tall, pale trunk that sways gently as it rises out of roots
     * climbing high up its base, its arms leaving the trunk well below the crown and bending gradually outwards to hold
     * a wide, level ring of drooping foliage around a small rounded crown, like a halo. Upside-down end rods hang
     * beneath the ring and glow like icicles of light.
     */
    private void haloTree(int x, int y, int z) {
        t = new TreeBuilder(world, r, Material.STRIPPED_POPLAR_WOOD, Material.PALE_OAK_LEAVES);
        t.setPorosity(0.05);
        double cx = x + 1;
        double cz = z + 1;
        int height = between(13, 18);
        int ringY = y + height + between(2, 3);
        double ringRadius = between(5.5, 7.0);
        t.info("height", height);
        // The trunk tapers and bows gently to one side and back, rather than standing as a straight column
        double leanYaw = randomAngle();
        double lean = between(0.5, 1.2);
        List<double[]> path = new ArrayList<>();
        for (int h = 0; h <= height; h++) {
            double p = (double) h / height;
            double sway = lean * Math.sin(p * Math.PI);
            path.add(new double[] {cx + Math.cos(leanYaw) * sway, y + h, cz + Math.sin(leanYaw) * sway});
        }
        for (int i = 1; i < path.size(); i++) {
            double[] a = path.get(i - 1);
            double[] b = path.get(i);
            double radius = 1.25 - 0.5 * i / height;
            t.thickBranch(a[0], a[1], a[2], b[0], b[1], b[2], radius, radius);
        }
        // Roots climb two to four blocks up the trunk before curving down into the ground
        int roots = between(5, 6);
        double rootStart = randomAngle();
        for (int i = 0; i < roots; i++) {
            double angle = rootStart + i * Math.PI * 2 / roots + jitter(0.35);
            double rise = between(2.0, 4.0);
            double reach = between(2.5, 4.0);
            double mx = cx + Math.cos(angle) * (1.0 + reach * 0.4);
            double mz = cz + Math.sin(angle) * (1.0 + reach * 0.4);
            double my = y + rise * 0.35;
            t.thickRoot(cx + Math.cos(angle) * 0.9, y + rise, cz + Math.sin(angle) * 0.9, mx, my, mz, 0.75, 0.5,
                    Material.STRIPPED_POPLAR_WOOD);
            t.thickRoot(mx, my, mz, cx + Math.cos(angle) * (1.0 + reach), y - 1, cz + Math.sin(angle) * (1.0 + reach),
                    0.5, 0.3, Material.STRIPPED_POPLAR_WOOD);
        }
        // Arms leave the trunk well below the crown at a steep angle and bend gradually outwards to the ring, with small
        // sprays of leaves where they bend, so the crown builds up slowly instead of starting all at once
        int arms = between(4, 6);
        double start = randomAngle();
        for (int i = 0; i < arms; i++) {
            double angle = start + i * Math.PI * 2 / arms + jitter(0.2);
            double[] from = pathAt(path, y + height * between(0.55, 0.85));
            double rx = cx + Math.cos(angle) * ringRadius;
            double rz = cz + Math.sin(angle) * ringRadius;
            double mx = from[0] + (rx - from[0]) * 0.25;
            double my = from[1] + (ringY - from[1]) * 0.6;
            double mz = from[2] + (rz - from[2]) * 0.25;
            double nx = from[0] + (rx - from[0]) * 0.65;
            double ny = ringY - 0.5;
            double nz = from[2] + (rz - from[2]) * 0.65;
            t.branch(from[0], from[1], from[2], mx, my, mz);
            t.branch(mx, my, mz, nx, ny, nz);
            t.branch(nx, ny, nz, rx, ringY, rz);
            if (chance(0.6)) {
                t.blob(mx, my + 0.5, mz, 1.3, 0.9, 1.3, 0.4);
            }
        }
        // A small rounded crown in the middle, well inside the ring so the ring stays clear around it
        double[] top = path.get(path.size() - 1);
        t.branch(top[0], top[1], top[2], cx, ringY + 1, cz);
        t.blob(cx, ringY + 1.5, cz, 2.4, 2.2, 2.4, 0.3);
        // The ring itself, a band of wood wrapped in a thick roll of leaves all the way around
        int steps = (int) Math.ceil(ringRadius * Math.PI * 2);
        for (int i = 0; i < steps; i++) {
            double a0 = i * Math.PI * 2 / steps;
            double a1 = (i + 1) * Math.PI * 2 / steps;
            t.branch(cx + Math.cos(a0) * ringRadius, ringY, cz + Math.sin(a0) * ringRadius,
                    cx + Math.cos(a1) * ringRadius, ringY, cz + Math.sin(a1) * ringRadius);
            if (i % 2 == 0) {
                t.blob(cx + Math.cos(a0) * ringRadius, ringY + 0.5, cz + Math.sin(a0) * ringRadius, 2.0, 1.3, 2.0, 0.3);
            }
        }
        // The ring droops in long tassels
        droopLeaves(0.5, ringY - 2, 4);
        // Upside-down end rods hang beneath the ring and its tassels
        for (Pos pos : new ArrayList<>(t.getPositions(Layer.LEAF))) {
            if (pos.y() > ringY || t.has(pos.x(), pos.y() - 1, pos.z()) || !chance(0.12)) {
                continue;
            }
            Directional rod = (Directional) Material.END_ROD.createBlockData();
            rod.setFacing(BlockFace.DOWN);
            t.decor(pos.x(), pos.y() - 1, pos.z(), rod, 0, 1, 0);
        }
    }

}
