package com.aearost.aranarthcore.trees;

import com.aearost.aranarthcore.AranarthCore;
import com.aearost.aranarthcore.crops.CropType;
import com.aearost.aranarthcore.crops.Crops;
import com.aearost.aranarthcore.objects.Dominion;
import com.aearost.aranarthcore.objects.Outpost;
import com.aearost.aranarthcore.trees.CustomTrees.Variant;
import com.aearost.aranarthcore.utils.AranarthUtils;
import com.aearost.aranarthcore.utils.ChatUtils;
import com.aearost.aranarthcore.utils.CustomTreeUtils;
import com.aearost.aranarthcore.utils.DominionUtils;
import com.aearost.aranarthcore.utils.FruitUtils;
import com.aearost.aranarthcore.utils.Lang;
import com.aearost.aranarthcore.utils.OutpostUtils;
import com.gmail.nossr50.mcMMO;
import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.HeightMap;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Tag;
import org.bukkit.World;
import org.bukkit.block.Biome;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.BlockState;
import org.bukkit.block.data.Bisected;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.scheduler.BukkitTask;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Deque;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.function.Predicate;

/**
 * Grows fruit trees, berry bushes and crops in the wild, a little at a time.
 */
public class WildGrowth {

    /** The furthest from the centre plants may grow in blocks, so that a world without a border is not grown forever. */
    private static final int MAX_RADIUS = 25_000;
    private static final int CLAIM_BUFFER = 4;
    private static final int CHANCE = 20;
    private static final int ROLLS_PER_TICK = 512;
    private static final int WORK_PER_TICK = 8;
    private static final long CLAIMS_REFRESH_MS = 60_000L;
    private static final int SAVE_TICKS = 20 * 30;
    private static final int EDGE_MARGIN = 16;
    private static final double RIPE_CHANCE = 0.55;

    private static final NamespacedKey DONE_KEY = new NamespacedKey("aranarthcore", "wild_growth");
    private static final String FILE_NAME = "wild_growth.yml";
    private static final String LOG_NAME = "wild_growth_plants.csv";
    private static final Map<String, Run> RUNS = new HashMap<>();
    private static BukkitTask saveTask;

    private static final Set<Material> SOIL = EnumSet.of(Material.GRASS_BLOCK, Material.DIRT, Material.COARSE_DIRT,
            Material.PODZOL, Material.ROOTED_DIRT, Material.MOSS_BLOCK);
    private static final Set<Material> SANDY = EnumSet.of(Material.SAND, Material.GRASS_BLOCK, Material.DIRT,
            Material.COARSE_DIRT);

    /**
     * How the plants of a grouping are laid out.
     */
    private enum Shape {
        /** Fruit trees in loose rows: small 3-5 (30%), medium 9-15 (50%) or large 20-35 (20%). */
        ORCHARD,
        /** A few trees close together. */
        CLUSTER,
        /** Plants scattered over an area. */
        FIELD,
        /** A row of bushes. */
        HEDGE,
        /** Plants close together in a small patch. */
        PATCH,
        /** Plants along the edge of water. */
        BANK
    }

    /**
     * A kind of grouping that grows wild.
     * @param shape How its plants are laid out.
     * @param min The fewest plants, unused by orchards.
     * @param max The most plants, unused by orchards.
     * @param biomes Where it grows.
     * @param condition Anything else its centre must meet, or null for nothing.
     * @param ground What its plants grow on.
     * @param trees The trees it may be made of, one kind per grouping.
     * @param crops The crops it may be made of, one kind per grouping.
     */
    private record Group(Shape shape, int min, int max, Set<Biome> biomes, Predicate<Block> condition,
                         Set<Material> ground, List<Variant> trees, List<CropType> crops) {

        boolean isTree() {
            return !trees.isEmpty();
        }
    }

    private static final List<Group> GROUPS = List.of(
            trees(Shape.ORCHARD, 0, 0, Set.of(Biome.PLAINS, Biome.FOREST, Biome.FLOWER_FOREST, Biome.MEADOW), null, SOIL,
                    Variant.APPLE_TREE, Variant.GREEN_APPLE_TREE, Variant.PEAR_TREE),
            trees(Shape.ORCHARD, 0, 0, Set.of(Biome.MEADOW, Biome.SUNFLOWER_PLAINS, Biome.CHERRY_GROVE), null, SOIL,
                    Variant.PLUM_TREE, Variant.PEACH_TREE, Variant.APRICOT_TREE),
            // Citrus grows in savannas, and on plains only near a river or lake
            trees(Shape.ORCHARD, 0, 0, Set.of(Biome.SAVANNA, Biome.PLAINS),
                    block -> block.getWorld().getBiome(block.getX(), block.getY(), block.getZ()) != Biome.PLAINS
                            || nearWater(block, 15), SOIL,
                    Variant.LEMON_TREE, Variant.ORANGE_TREE, Variant.LIME_TREE),
            trees(Shape.CLUSTER, 2, 4, Set.of(Biome.JUNGLE, Biome.SPARSE_JUNGLE, Biome.BAMBOO_JUNGLE), null, SOIL,
                    Variant.MANGO_TREE, Variant.BANANA_PLANT, Variant.AVOCADO_TREE, Variant.LYCHEE_TREE),
            trees(Shape.CLUSTER, 2, 4, Set.of(Biome.SAVANNA, Biome.SAVANNA_PLATEAU, Biome.WOODED_BADLANDS), null, SOIL,
                    Variant.FIG_TREE, Variant.POMEGRANATE_TREE),
            trees(Shape.CLUSTER, 2, 2, Set.of(Biome.DARK_FOREST, Biome.OLD_GROWTH_BIRCH_FOREST), null, SOIL,
                    Variant.PERSIMMON_TREE),
            // The rare fruit trees keep to the same places their saplings need
            trees(Shape.CLUSTER, 2, 5, Set.of(Biome.BEACH), null, SANDY, Variant.COCONUT_PALM),
            trees(Shape.CLUSTER, 2, 5, Set.of(Biome.DESERT), block -> nearWater(block, 15), SANDY, Variant.DATE_PALM),
            trees(Shape.CLUSTER, 2, 5, Set.of(Biome.SAVANNA_PLATEAU), null, SOIL, Variant.OLIVE_TREE),
            crops(Shape.FIELD, 6, 12, Set.of(Biome.TAIGA, Biome.OLD_GROWTH_PINE_TAIGA, Biome.OLD_GROWTH_SPRUCE_TAIGA,
                    Biome.SNOWY_TAIGA), null, SOIL, CropType.BLUEBERRY),
            crops(Shape.HEDGE, 4, 8, Set.of(Biome.FOREST, Biome.BIRCH_FOREST, Biome.WINDSWEPT_FOREST), null, SOIL,
                    CropType.RASPBERRY, CropType.BLACKBERRY),
            crops(Shape.PATCH, 5, 10, Set.of(Biome.MEADOW, Biome.FLOWER_FOREST, Biome.PLAINS), null, SOIL,
                    CropType.STRAWBERRY),
            crops(Shape.PATCH, 6, 12, Set.of(Biome.PLAINS, Biome.SUNFLOWER_PLAINS), null, SOIL, CropType.CORN),
            crops(Shape.BANK, 4, 10, Set.of(Biome.SWAMP, Biome.RIVER), block -> nearWater(block, 10),
                    EnumSet.of(Material.GRASS_BLOCK, Material.DIRT, Material.MUD, Material.COARSE_DIRT), CropType.RICE),
            crops(Shape.PATCH, 3, 6, Set.of(Biome.SPARSE_JUNGLE, Biome.JUNGLE, Biome.SAVANNA), null, SOIL,
                    CropType.PINEAPPLE, CropType.COFFEE, CropType.KIWI),
            crops(Shape.PATCH, 2, 4, Set.of(Biome.MEADOW, Biome.PLAINS, Biome.SAVANNA), null, SOIL,
                    CropType.GREEN_GRAPES, CropType.RED_GRAPES, CropType.TOMATO, CropType.BELL_PEPPER),
            crops(Shape.PATCH, 3, 6, Set.of(Biome.PLAINS, Biome.MEADOW, Biome.FOREST), null, SOIL,
                    CropType.CABBAGE, CropType.LETTUCE, CropType.ONION, CropType.GARLIC, CropType.SWEET_POTATO,
                    CropType.CANTALOUPE)
    );

    /**
     * Provides every crop that grows wild in the biome, such as the crops that grass found there may drop.
     */
    public static List<CropType> cropsFor(Biome biome) {
        List<CropType> crops = new ArrayList<>();
        for (Group group : GROUPS) {
            if (!group.isTree() && group.biomes().contains(biome)) {
                for (CropType crop : group.crops()) {
                    if (!crops.contains(crop)) {
                        crops.add(crop);
                    }
                }
            }
        }
        return crops;
    }

    private static Group trees(Shape shape, int min, int max, Set<Biome> biomes, Predicate<Block> condition,
                               Set<Material> ground, Variant... trees) {
        return new Group(shape, min, max, biomes, condition, ground, List.of(trees), List.of());
    }

    private static Group crops(Shape shape, int min, int max, Set<Biome> biomes, Predicate<Block> condition,
                               Set<Material> ground, CropType... crops) {
        return new Group(shape, min, max, biomes, condition, ground, List.of(), List.of(crops));
    }

    /**
     * A run across one world.
     */
    private static class Run {
        final World world;
        /** The radius chosen when it started, or 0 to fit the world border. */
        final int radius;
        /** The chunks plants may grow in, being those wholly inside the area. */
        final int minCx, maxCx, minCz, maxCz;
        List<int[]> regions;
        int regionIndex;
        int chunkIndex;
        int groups;
        /** Who started it, told when it finishes, or null for the console. */
        UUID starter;
        /** Whether it is waiting for chunks to load. */
        boolean busy;
        /** The plants of the grouping being grown, placed a few per tick. */
        final Deque<Placement> queue = new ArrayDeque<>();
        /** The chunks held loaded for the grouping being grown. */
        final List<Chunk> held = new ArrayList<>();
        Set<Long> claimed = new HashSet<>();
        long claimedAt;
        BukkitTask task;
        /** The plant the grouping being placed is made of, as its produce id. */
        String plant;
        /** How many plants of the grouping have grown so far. */
        int placed;
        /** Where the first plant of the grouping grew. */
        Block first;

        Run(World world, int radius) {
            this.world = world;
            this.radius = radius;
            // A chosen radius is a square around 0,0, otherwise the square inside the world border
            double centerX = 0;
            double centerZ = 0;
            double half = radius;
            if (radius <= 0) {
                centerX = world.getWorldBorder().getCenter().getX();
                centerZ = world.getWorldBorder().getCenter().getZ();
                half = world.getWorldBorder().getSize() / 2;
            }
            half = Math.min(half, MAX_RADIUS);
            minCx = Math.ceilDiv((int) Math.ceil(centerX - half), 16);
            maxCx = Math.floorDiv((int) Math.floor(centerX + half), 16) - 1;
            minCz = Math.ceilDiv((int) Math.ceil(centerZ - half), 16);
            maxCz = Math.floorDiv((int) Math.floor(centerZ + half), 16) - 1;
        }
    }

    /**
     * A plant waiting to be placed.
     * @param cost How much of a tick's work it takes.
     */
    private record Placement(int cost, Runnable action) {}

    /**
     * Starts growing wild plants in the world, or carries on from where it stopped.
     * @param starter Who started it, or null for the console.
     * @param radius How far from 0,0 plants may grow in every direction, 0 to fit the world border, or null to keep
     *               the radius it was last started with.
     * @return Whether it started, false if it was already running.
     */
    public static boolean start(World world, UUID starter, Integer radius) {
        if (RUNS.containsKey(world.getName())) {
            return false;
        }
        ConfigurationSection saved = load().getConfigurationSection("worlds." + world.getName());
        int chosen = radius != null ? radius : saved == null ? 0 : saved.getInt("radius", 0);
        Run run = new Run(world, chosen);
        run.regions = findRegions(run);
        run.starter = starter;
        if (saved != null) {
            String region = saved.getString("region", "");
            for (int i = 0; i < run.regions.size(); i++) {
                int[] r = run.regions.get(i);
                if ((r[0] + "," + r[1]).equals(region)) {
                    run.regionIndex = i;
                    run.chunkIndex = saved.getInt("chunk", 0);
                    break;
                }
            }
            run.groups = saved.getInt("groups", 0);
        }
        RUNS.put(world.getName(), run);
        Bukkit.getLogger().info(AranarthCore.LOG_PREFIX + "Growing wild plants in " + world.getName() + " (chunks "
                + run.minCx + " to " + run.maxCx + " by " + run.minCz + " to " + run.maxCz + "): region "
                + Math.min(run.regionIndex + 1, run.regions.size()) + "/" + run.regions.size() + ", "
                + run.groups + " groupings so far");
        run.task = Bukkit.getScheduler().runTaskTimer(AranarthCore.getInstance(), () -> tick(run), 1L, 1L);
        if (saveTask == null) {
            saveTask = Bukkit.getScheduler().runTaskTimer(AranarthCore.getInstance(), WildGrowth::saveAll, SAVE_TICKS,
                    SAVE_TICKS);
        }
        saveAll();
        return true;
    }

    /**
     * Stops growing wild plants in the world, remembering where it got to.
     * @return Whether it was running.
     */
    public static boolean stop(String worldName) {
        Run run = RUNS.get(worldName);
        if (run == null) {
            return false;
        }
        saveAll();
        end(run);
        YamlConfiguration config = load();
        config.set("worlds." + worldName + ".running", false);
        save(config);
        return true;
    }

    /**
     * Provides the progress of the run in the world, or null if it is not running.
     * @return The region it is on, how many regions there are, and how many groupings it has grown.
     */
    public static int[] getProgress(String worldName) {
        Run run = RUNS.get(worldName);
        return run == null ? null : new int[] {Math.min(run.regionIndex + 1, run.regions.size()), run.regions.size(), run.groups};
    }

    /**
     * Carries on every run that was going when the server stopped.
     */
    public static void resumeAll() {
        ConfigurationSection worlds = load().getConfigurationSection("worlds");
        if (worlds == null) {
            return;
        }
        for (String name : worlds.getKeys(false)) {
            World world = Bukkit.getWorld(name);
            if (world != null && worlds.getBoolean(name + ".running")) {
                String starter = worlds.getString(name + ".starter");
                start(world, starter == null || starter.isEmpty() ? null : UUID.fromString(starter), null);
                Bukkit.getLogger().info(AranarthCore.LOG_PREFIX + "Resumed growing wild plants in " + name);
            }
        }
    }

    /**
     * Stops every run, remembering where each got to so that it carries on when the server starts again.
     */
    public static void shutdown() {
        saveAll();
        for (Run run : new ArrayList<>(RUNS.values())) {
            end(run);
        }
    }

    private static void end(Run run) {
        if (run.task != null) {
            run.task.cancel();
        }
        run.queue.clear();
        release(run);
        RUNS.remove(run.world.getName());
    }

    private static void finish(Run run) {
        end(run);
        YamlConfiguration config = load();
        config.set("worlds." + run.world.getName() + ".running", false);
        config.set("worlds." + run.world.getName() + ".finished", true);
        save(config);
        String groups = String.valueOf(run.groups);
        Bukkit.getLogger().info(AranarthCore.LOG_PREFIX + "Finished growing wild plants in " + run.world.getName() + ": "
                + groups + " groupings");
        Player player = run.starter == null ? null : Bukkit.getPlayer(run.starter);
        if (player != null) {
            player.sendMessage(ChatUtils.chatMessage(Lang.getFor(player, "trees.regen_done", "world", run.world.getName(),
                    "groups", groups)));
        }
    }

    /**
     * Places the waiting plants of the current grouping, or looks for the next one.
     */
    private static void tick(Run run) {
        if (!run.queue.isEmpty()) {
            int work = 0;
            while (work < WORK_PER_TICK && !run.queue.isEmpty()) {
                Placement placement = run.queue.poll();
                work += placement.cost();
                try {
                    placement.action().run();
                } catch (RuntimeException e) {
                    Bukkit.getLogger().warning(AranarthCore.LOG_PREFIX + "A wild plant failed to grow: " + e.getMessage());
                }
            }
            if (run.queue.isEmpty()) {
                release(run);
                // Only groupings that grew at least one plant count, and each is written down so it can be found
                if (run.placed > 0) {
                    run.groups++;
                    record(run);
                }
                run.busy = false;
            }
            return;
        }
        if (run.busy) {
            return;
        }
        if (System.currentTimeMillis() - run.claimedAt > CLAIMS_REFRESH_MS) {
            run.claimed = findClaimed(run.world);
            run.claimedAt = System.currentTimeMillis();
        }
        for (int rolls = 0; rolls < ROLLS_PER_TICK; rolls++) {
            if (run.regionIndex >= run.regions.size()) {
                finish(run);
                return;
            }
            int[] region = run.regions.get(run.regionIndex);
            int cx = (region[0] << 5) + (run.chunkIndex & 31);
            int cz = (region[1] << 5) + (run.chunkIndex >> 5);
            if (++run.chunkIndex >= 1024) {
                run.chunkIndex = 0;
                run.regionIndex++;
            }
            if (!isUsable(run, cx, cz) || roll(run.world, cx, cz).nextInt(CHANCE) != 0
                    || !run.world.isChunkGenerated(cx, cz)) {
                continue;
            }
            startGroup(run, cx, cz);
            return;
        }
    }

    /**
     * Determines if plants may grow in the chunk: wholly inside the area, and away from claimed land.
     */
    private static boolean isUsable(Run run, int cx, int cz) {
        return cx >= run.minCx && cx <= run.maxCx && cz >= run.minCz && cz <= run.maxCz
                && !run.claimed.contains(key(cx, cz));
    }

    /**
     * Provides the random that decides a chunk, the same every time, so that a run picked up again decides the same way.
     */
    private static Random roll(World world, int cx, int cz) {
        long seed = world.getSeed() ^ (cx * 0x9E3779B97F4A7C15L) ^ (cz * 0xC2B2AE3D27D4EB4FL) ^ 0x5DEECE66DL;
        return new Random(seed);
    }

    /**
     * Loads the chunk and grows a grouping there if its biome suits one.
     */
    private static void startGroup(Run run, int cx, int cz) {
        run.busy = true;
        World world = run.world;
        world.getChunkAtAsync(cx, cz, false).thenAccept(chunk -> Bukkit.getScheduler().runTask(AranarthCore.getInstance(), () -> {
            if (RUNS.get(world.getName()) != run || chunk == null) {
                run.busy = false;
                return;
            }
            // A chunk is only ever grown once, even if the run is started again
            if (chunk.getPersistentDataContainer().has(DONE_KEY, PersistentDataType.BYTE)) {
                run.busy = false;
                return;
            }
            chunk.getPersistentDataContainer().set(DONE_KEY, PersistentDataType.BYTE, (byte) 1);

            Random random = roll(world, cx, cz);
            random.nextInt(CHANCE);
            int x = (cx << 4) + 2 + random.nextInt(12);
            int z = (cz << 4) + 2 + random.nextInt(12);
            Block ground = world.getHighestBlockAt(x, z, HeightMap.MOTION_BLOCKING_NO_LEAVES);
            Block above = ground.getRelative(BlockFace.UP);
            if (AranarthUtils.isProtectedSpawnLocation(above.getLocation())) {
                run.busy = false;
                return;
            }
            Biome biome = world.getBiome(x, above.getY(), z);
            List<Group> suited = new ArrayList<>();
            for (Group group : GROUPS) {
                if (group.biomes().contains(biome) && (group.condition() == null || group.condition().test(above))) {
                    suited.add(group);
                }
            }
            if (suited.isEmpty()) {
                run.busy = false;
                return;
            }
            Group group = suited.get(random.nextInt(suited.size()));
            int count = count(group, random);
            int reach = reach(group, count) + EDGE_MARGIN;
            loadArea(run, x, z, reach).thenRun(() -> Bukkit.getScheduler().runTask(AranarthCore.getInstance(), () -> {
                if (RUNS.get(world.getName()) != run) {
                    release(run);
                    return;
                }
                plan(run, group, count, x, z, random);
                if (run.queue.isEmpty()) {
                    release(run);
                    run.busy = false;
                }
            }));
        }));
    }

    /**
     * Provides how many plants a grouping grows.
     */
    private static int count(Group group, Random random) {
        if (group.shape() == Shape.ORCHARD) {
            double size = random.nextDouble();
            if (size < 0.3) {
                return 3 + random.nextInt(3);
            }
            return size < 0.8 ? 9 + random.nextInt(7) : 20 + random.nextInt(16);
        }
        return group.min() + random.nextInt(group.max() - group.min() + 1);
    }

    /**
     * Provides how far from its centre a grouping reaches, in blocks.
     */
    private static int reach(Group group, int count) {
        return switch (group.shape()) {
            case ORCHARD -> (int) Math.ceil(Math.sqrt(count)) * 4 + 2;
            case CLUSTER -> 6 + count * 2;
            case FIELD -> 8;
            case HEDGE -> count;
            case PATCH -> 3 + count / 2;
            case BANK -> 10;
        };
    }

    /**
     * Loads every usable chunk around the centre, holding each one loaded until the grouping is placed.
     */
    private static CompletableFuture<Void> loadArea(Run run, int x, int z, int reach) {
        List<CompletableFuture<Chunk>> futures = new ArrayList<>();
        for (int cx = (x - reach) >> 4; cx <= (x + reach) >> 4; cx++) {
            for (int cz = (z - reach) >> 4; cz <= (z + reach) >> 4; cz++) {
                if (isUsable(run, cx, cz) && run.world.isChunkGenerated(cx, cz)) {
                    futures.add(run.world.getChunkAtAsync(cx, cz, false));
                }
            }
        }
        return CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).handle((ignored, error) -> {
            Bukkit.getScheduler().runTask(AranarthCore.getInstance(), () -> {
                for (CompletableFuture<Chunk> future : futures) {
                    Chunk chunk = future.getNow(null);
                    if (chunk != null && chunk.addPluginChunkTicket(AranarthCore.getInstance())) {
                        run.held.add(chunk);
                    }
                }
            });
            return null;
        }).thenCompose(ignored -> {
            // Waits a tick so that the chunks are held before anything is placed
            CompletableFuture<Void> next = new CompletableFuture<>();
            Bukkit.getScheduler().runTaskLater(AranarthCore.getInstance(), () -> next.complete(null), 1L);
            return next;
        });
    }

    private static void release(Run run) {
        for (Chunk chunk : run.held) {
            chunk.removePluginChunkTicket(AranarthCore.getInstance());
        }
        run.held.clear();
    }

    /**
     * Determines if everything within the margin of the spot is in a chunk that is loaded and usable.
     */
    private static boolean isReady(Run run, int x, int z, int margin) {
        for (int cx = (x - margin) >> 4; cx <= (x + margin) >> 4; cx++) {
            for (int cz = (z - margin) >> 4; cz <= (z + margin) >> 4; cz++) {
                if (!isUsable(run, cx, cz) || !run.world.isChunkLoaded(cx, cz)) {
                    return false;
                }
            }
        }
        return true;
    }

    /**
     * Works out where each plant of the grouping goes and queues it to be placed.
     */
    private static void plan(Run run, Group group, int count, int x, int z, Random random) {
        run.placed = 0;
        run.first = null;
        List<int[]> spots = switch (group.shape()) {
            case ORCHARD -> orchardSpots(x, z, count, random);
            case CLUSTER -> scatter(x, z, count, reach(group, count) - 2, 6, random);
            case FIELD -> scatter(x, z, count, 7, 2, random);
            case HEDGE -> hedge(x, z, count, random);
            case PATCH -> scatter(x, z, count, reach(group, count), 2, random);
            case BANK -> List.of();
        };
        if (group.isTree()) {
            Variant variant = group.trees().get(random.nextInt(group.trees().size()));
            run.plant = variant.getFruitId();
            for (int[] spot : spots) {
                long seed = random.nextLong();
                run.queue.add(new Placement(4, () -> growTree(run, group, variant, spot[0], spot[1], new Random(seed))));
            }
            return;
        }
        CropType type = group.crops().get(random.nextInt(group.crops().size()));
        run.plant = type.getYield().getId();
        if (group.shape() == Shape.BANK) {
            for (Block soil : bankSpots(run, x, z, count, random)) {
                long seed = random.nextLong();
                run.queue.add(new Placement(1, () -> growCrop(run, type, soil, new Random(seed))));
            }
            return;
        }
        for (int[] spot : spots) {
            long seed = random.nextLong();
            run.queue.add(new Placement(1, () -> {
                Block ground = findGround(run, group, spot[0], spot[1], 2);
                if (ground != null) {
                    growCrop(run, type, ground, new Random(seed));
                }
            }));
        }
    }

    /**
     * Lays out an orchard in loose rows about 7 blocks apart.
     */
    private static List<int[]> orchardSpots(int x, int z, int count, Random random) {
        int side = (int) Math.ceil(Math.sqrt(count));
        double angle = random.nextDouble() * Math.PI / 2;
        double cos = Math.cos(angle);
        double sin = Math.sin(angle);
        List<int[]> spots = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            double gx = (i % side - (side - 1) / 2.0) * 7 + random.nextInt(3) - 1;
            double gz = (i / side - (side - 1) / 2.0) * 7 + random.nextInt(3) - 1;
            spots.add(new int[] {x + (int) Math.round(gx * cos - gz * sin), z + (int) Math.round(gx * sin + gz * cos)});
        }
        return spots;
    }

    /**
     * Scatters spots around the centre, each at least the gap from the others.
     */
    private static List<int[]> scatter(int x, int z, int count, int radius, int gap, Random random) {
        List<int[]> spots = new ArrayList<>();
        for (int tries = 0; tries < count * 12 && spots.size() < count; tries++) {
            int sx = x + random.nextInt(radius * 2 + 1) - radius;
            int sz = z + random.nextInt(radius * 2 + 1) - radius;
            boolean clear = true;
            for (int[] other : spots) {
                if (Math.abs(other[0] - sx) < gap && Math.abs(other[1] - sz) < gap) {
                    clear = false;
                    break;
                }
            }
            if (clear) {
                spots.add(new int[] {sx, sz});
            }
        }
        return spots;
    }

    /**
     * Lays out a row of bushes, running north to south or east to west.
     */
    private static List<int[]> hedge(int x, int z, int count, Random random) {
        boolean alongX = random.nextBoolean();
        List<int[]> spots = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            int offset = i - count / 2;
            spots.add(alongX ? new int[] {x + offset, z} : new int[] {x, z + offset});
        }
        return spots;
    }

    /**
     * Finds natural ground at the spot with room above it, clearing any grass or flowers growing there.
     * @param margin How far around the spot must be loaded and usable.
     * @return The ground, or null if nothing may grow there.
     */
    private static Block findGround(Run run, Group group, int x, int z, int margin) {
        if (!isReady(run, x, z, margin)) {
            return null;
        }
        World world = run.world;
        Block ground = world.getHighestBlockAt(x, z, HeightMap.MOTION_BLOCKING_NO_LEAVES);
        if (!group.ground().contains(ground.getType()) || mcMMO.getChunkManager().isIneligible(ground)) {
            return null;
        }
        Block above = ground.getRelative(BlockFace.UP);
        if (!group.biomes().contains(world.getBiome(x, above.getY(), z))
                || AranarthUtils.isProtectedSpawnLocation(above.getLocation())) {
            return null;
        }
        return clearPlant(above) ? ground : null;
    }

    /**
     * Clears grass, ferns, flowers or snow from the block so that something can grow there.
     * @return Whether the block is now empty.
     */
    private static boolean clearPlant(Block block) {
        Material type = block.getType();
        if (type.isAir()) {
            return true;
        }
        if (!Tag.REPLACEABLE.isTagged(type) && !Tag.SMALL_FLOWERS.isTagged(type) || type == Material.WATER
                || type == Material.LAVA || Crops.isCropBlock(block) || mcMMO.getChunkManager().isIneligible(block)) {
            return false;
        }
        // A tall plant loses its top half too
        if (block.getBlockData() instanceof Bisected) {
            Block top = block.getRelative(BlockFace.UP);
            if (top.getType() == type) {
                top.setType(Material.AIR, false);
            }
        }
        block.setType(Material.AIR, false);
        return true;
    }

    /**
     * Grows a fruit tree at the spot, already bearing fruit, part of which is still growing back.
     */
    private static void growTree(Run run, Group group, Variant variant, int x, int z, Random random) {
        Block ground = findGround(run, group, x, z, EDGE_MARGIN);
        if (ground == null) {
            return;
        }
        Block base = ground.getRelative(BlockFace.UP);
        // Not right up against the trunk of another tree
        for (int dx = -2; dx <= 2; dx++) {
            for (int dz = -2; dz <= 2; dz++) {
                for (int dy = 0; dy <= 3; dy++) {
                    if (Tag.LOGS.isTagged(base.getRelative(dx, dy, dz).getType())) {
                        return;
                    }
                }
            }
        }
        if (variant.isRare() && !RareTreeConditions.isMetWild(variant, base)) {
            return;
        }
        TreeBuilder tree = CustomTrees.design(variant, run.world, random, base.getX(), base.getY(), base.getZ(), -1, true);
        if (tree == null) {
            return;
        }
        List<BlockState> states = tree.resolve(base.getLocation(), new HashSet<>());
        if (states == null || states.isEmpty()) {
            return;
        }
        for (BlockState state : states) {
            if (!isReady(run, state.getX(), state.getZ(), 0)) {
                return;
            }
        }
        for (BlockState state : states) {
            Block block = state.getBlock();
            block.setBlockData(state.getBlockData(), false);
            // Wild trees are natural, so they are never treated as player-placed
            mcMMO.getChunkManager().setEligible(block);
            CustomTreeUtils.markNoDrop(block);
        }
        tree.applyHeadTextures();
        grew(run, base);
        // Some of the fruit has been picked by nature a while ago, so the tree looks part way through the season
        for (TreeBuilder.Pos pos : tree.getFruitSpots().keySet()) {
            if (random.nextDouble() >= RIPE_CHANCE) {
                FruitUtils.unripen(run.world.getBlockAt(pos.x(), pos.y(), pos.z()), random.nextDouble());
            }
        }
    }

    /**
     * Grows a crop on the ground, at a random stage, ripe more often than not.
     */
    private static void growCrop(Run run, CropType type, Block ground, Random random) {
        Block base = ground.getRelative(BlockFace.UP);
        if (!base.getType().isAir()) {
            return;
        }
        int stage = random.nextDouble() < RIPE_CHANCE ? type.getRipeStage() : random.nextInt(type.getStages());
        if (type.isVine()) {
            growVine(run, type, base, stage, random);
            return;
        }
        if (Crops.place(base, type, stage, false)) {
            grew(run, base);
        }
    }

    /**
     * Grows a vine with a weathered fence post of the local wood for it to climb.
     */
    private static void growVine(Run run, CropType type, Block base, int stage, Random random) {
        BlockFace[] sides = {BlockFace.NORTH, BlockFace.EAST, BlockFace.SOUTH, BlockFace.WEST};
        BlockFace side = sides[random.nextInt(4)];
        Block post = base.getRelative(side);
        Block postGround = post.getRelative(BlockFace.DOWN);
        if (!SOIL.contains(postGround.getType()) || !isReady(run, post.getX(), post.getZ(), 1)) {
            return;
        }
        int height = 2 + random.nextInt(2);
        for (int h = 0; h < height; h++) {
            if (!clearPlant(post.getRelative(0, h, 0))) {
                return;
            }
        }
        Biome biome = run.world.getBiome(post.getX(), post.getY(), post.getZ());
        Material fence = biome == Biome.SAVANNA || biome == Biome.SAVANNA_PLATEAU ? Material.ACACIA_FENCE
                : biome == Biome.JUNGLE || biome == Biome.SPARSE_JUNGLE || biome == Biome.BAMBOO_JUNGLE
                ? Material.JUNGLE_FENCE : Material.OAK_FENCE;
        for (int h = 0; h < height; h++) {
            post.getRelative(0, h, 0).setType(fence, false);
        }
        if (!Crops.place(base, type, stage, false)) {
            for (int h = 0; h < height; h++) {
                post.getRelative(0, h, 0).setType(Material.AIR, false);
            }
            return;
        }
        grew(run, base);
    }

    /**
     * Finds spots on the bank of water around the centre, turning the ground there to mud for rice.
     */
    private static List<Block> bankSpots(Run run, int x, int z, int count, Random random) {
        List<Block> found = new ArrayList<>();
        int radius = 10;
        List<int[]> columns = new ArrayList<>();
        for (int dx = -radius; dx <= radius; dx++) {
            for (int dz = -radius; dz <= radius; dz++) {
                columns.add(new int[] {x + dx, z + dz});
            }
        }
        Collections.shuffle(columns, random);
        Group bank = null;
        for (Group group : GROUPS) {
            if (group.shape() == Shape.BANK) {
                bank = group;
            }
        }
        for (int[] column : columns) {
            if (found.size() >= count || bank == null) {
                break;
            }
            if (!isReady(run, column[0], column[1], 1)) {
                continue;
            }
            Block ground = run.world.getHighestBlockAt(column[0], column[1], HeightMap.MOTION_BLOCKING_NO_LEAVES);
            if (!bank.ground().contains(ground.getType()) || mcMMO.getChunkManager().isIneligible(ground)
                    || !nextToWater(ground)) {
                continue;
            }
            Block above = ground.getRelative(BlockFace.UP);
            if (!above.getType().isAir() && !Tag.REPLACEABLE.isTagged(above.getType())
                    || AranarthUtils.isProtectedSpawnLocation(above.getLocation())) {
                continue;
            }
            found.add(ground);
        }
        // The ground only becomes mud once the plant is about to grow on it
        List<Block> soils = new ArrayList<>();
        for (Block ground : found) {
            if (clearPlant(ground.getRelative(BlockFace.UP))) {
                if (ground.getType() != Material.MUD) {
                    ground.setType(Material.MUD, false);
                }
                soils.add(ground);
            }
        }
        return soils;
    }

    private static boolean nextToWater(Block block) {
        for (BlockFace face : new BlockFace[] {BlockFace.NORTH, BlockFace.EAST, BlockFace.SOUTH, BlockFace.WEST}) {
            if (block.getRelative(face).getType() == Material.WATER) {
                return true;
            }
        }
        return false;
    }

    /**
     * Determines if there is water within the range of the block, measured sideways, from a little below it to just above.
     */
    private static boolean nearWater(Block block, int range) {
        for (int dy = -3; dy <= 1; dy++) {
            for (int dx = -range; dx <= range; dx += 2) {
                for (int dz = -range; dz <= range; dz += 2) {
                    if (dx * dx + dz * dz <= range * range && block.getRelative(dx, dy, dz).getType() == Material.WATER) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    /**
     * Provides every chunk of the world that is claimed by a Dominion or outpost, along with the chunks around it.
     */
    private static Set<Long> findClaimed(World world) {
        Set<Long> claimed = new HashSet<>();
        List<Chunk> chunks = new ArrayList<>();
        for (Dominion dominion : DominionUtils.getDominions()) {
            chunks.addAll(dominion.getChunks());
            for (Outpost outpost : OutpostUtils.getDominionOutposts(dominion.getId())) {
                chunks.addAll(outpost.getChunks());
            }
        }
        for (Chunk chunk : chunks) {
            if (!chunk.getWorld().equals(world)) {
                continue;
            }
            for (int dx = -CLAIM_BUFFER; dx <= CLAIM_BUFFER; dx++) {
                for (int dz = -CLAIM_BUFFER; dz <= CLAIM_BUFFER; dz++) {
                    claimed.add(key(chunk.getX() + dx, chunk.getZ() + dz));
                }
            }
        }
        return claimed;
    }

    private static long key(int cx, int cz) {
        return ((long) cx << 32) | (cz & 0xFFFFFFFFL);
    }

    /**
     * Provides every region file of the world that overlaps the area of the run, nearest to its centre first.
     * @return The X and Z of each region.
     */
    private static List<int[]> findRegions(Run run) {
        World world = run.world;
        // Newer worlds keep each dimension under dimensions/minecraft, older ones keep the overworld's regions at the top
        String dimension = switch (world.getEnvironment()) {
            case NETHER -> "the_nether";
            case THE_END -> "the_end";
            default -> "overworld";
        };
        String legacy = switch (world.getEnvironment()) {
            case NETHER -> "DIM-1/region";
            case THE_END -> "DIM1/region";
            default -> "region";
        };
        List<int[]> regions = new ArrayList<>();
        File[] files = new File(world.getWorldFolder(), "dimensions/minecraft/" + dimension + "/region").listFiles();
        if (files == null || files.length == 0) {
            files = new File(world.getWorldFolder(), legacy).listFiles();
        }
        if (files == null) {
            return regions;
        }
        for (File file : files) {
            String[] parts = file.getName().split("\\.");
            if (parts.length != 4 || !parts[0].equals("r") || !parts[3].equals("mca") || file.length() == 0) {
                continue;
            }
            try {
                int rx = Integer.parseInt(parts[1]);
                int rz = Integer.parseInt(parts[2]);
                if (rx >= run.minCx >> 5 && rx <= run.maxCx >> 5 && rz >= run.minCz >> 5 && rz <= run.maxCz >> 5) {
                    regions.add(new int[] {rx, rz});
                }
            } catch (NumberFormatException ignored) {
                // Not a region file
            }
        }
        // Twice the centre region, so that a centre between two regions sorts evenly
        long midX = (long) (run.minCx >> 5) + (run.maxCx >> 5);
        long midZ = (long) (run.minCz >> 5) + (run.maxCz >> 5);
        regions.sort(Comparator.<int[]>comparingLong(r -> (2L * r[0] - midX) * (2L * r[0] - midX)
                        + (2L * r[1] - midZ) * (2L * r[1] - midZ))
                .thenComparingInt(r -> r[0]).thenComparingInt(r -> r[1]));
        return regions;
    }

    /**
     * Counts a plant of the grouping that has grown.
     */
    private static void grew(Run run, Block base) {
        if (run.placed++ == 0) {
            run.first = base;
        }
    }

    /**
     * A grouping that grew in the wild.
     * @param plant The produce id of what it is made of.
     */
    public record Grown(String world, int x, int y, int z, String plant, int count) {}

    private static File getLogFile() {
        return new File(AranarthCore.getInstance().getDataFolder(), LOG_NAME);
    }

    /**
     * Writes down the grouping that just grew, so that it can be found again.
     */
    private static void record(Run run) {
        Block first = run.first;
        String line = run.world.getName() + "," + first.getX() + "," + first.getY() + "," + first.getZ() + "," + run.plant
                + "," + run.placed + System.lineSeparator();
        try {
            Files.writeString(getLogFile().toPath(), line, StandardCharsets.UTF_8, StandardOpenOption.CREATE,
                    StandardOpenOption.APPEND);
        } catch (IOException e) {
            Bukkit.getLogger().warning(AranarthCore.LOG_PREFIX + "Failed to write down a wild grouping: " + e.getMessage());
        }
    }

    /**
     * Provides the groupings that grew in the world nearest to the spot, nearest first.
     * @param limit How many to provide at most.
     */
    public static List<Grown> findNearest(String world, int x, int z, int limit) {
        List<Grown> grown = new ArrayList<>();
        File file = getLogFile();
        if (!file.exists()) {
            return grown;
        }
        try {
            for (String line : Files.readAllLines(file.toPath(), StandardCharsets.UTF_8)) {
                String[] p = line.split(",");
                if (p.length == 6 && p[0].equals(world)) {
                    grown.add(new Grown(p[0], Integer.parseInt(p[1]), Integer.parseInt(p[2]), Integer.parseInt(p[3]), p[4],
                            Integer.parseInt(p[5])));
                }
            }
        } catch (IOException | NumberFormatException e) {
            Bukkit.getLogger().warning(AranarthCore.LOG_PREFIX + "Failed to read the wild groupings: " + e.getMessage());
        }
        grown.sort(Comparator.comparingLong(g -> (long) (g.x() - x) * (g.x() - x) + (long) (g.z() - z) * (g.z() - z)));
        return grown.subList(0, Math.min(limit, grown.size()));
    }

    private static File getFile() {
        return new File(AranarthCore.getInstance().getDataFolder(), FILE_NAME);
    }

    private static YamlConfiguration load() {
        return YamlConfiguration.loadConfiguration(getFile());
    }

    private static void save(YamlConfiguration config) {
        try {
            config.save(getFile());
        } catch (IOException e) {
            Bukkit.getLogger().warning(AranarthCore.LOG_PREFIX + "Failed to save wild growth progress: " + e.getMessage());
        }
    }

    /**
     * Remembers where every run got to.
     */
    private static void saveAll() {
        if (RUNS.isEmpty()) {
            return;
        }
        YamlConfiguration config = load();
        for (Run run : RUNS.values()) {
            String path = "worlds." + run.world.getName();
            // The grouping being placed is left out, so a restart only skips it rather than growing it twice
            int[] region = run.regionIndex < run.regions.size() ? run.regions.get(run.regionIndex) : null;
            config.set(path + ".region", region == null ? "" : region[0] + "," + region[1]);
            config.set(path + ".chunk", run.chunkIndex);
            config.set(path + ".groups", run.groups);
            config.set(path + ".running", true);
            config.set(path + ".starter", run.starter == null ? "" : run.starter.toString());
            config.set(path + ".radius", run.radius);
        }
        save(config);
    }

    /**
     * Forgets everything grown in the world, for when it is deleted and made again, stopping it if it is running.
     */
    public static void forget(String worldName) {
        Run run = RUNS.get(worldName);
        if (run != null) {
            end(run);
        }
        YamlConfiguration config = load();
        config.set("worlds." + worldName, null);
        save(config);
        File file = getLogFile();
        if (!file.exists()) {
            return;
        }
        try {
            List<String> kept = new ArrayList<>();
            for (String line : Files.readAllLines(file.toPath(), StandardCharsets.UTF_8)) {
                if (!line.startsWith(worldName + ",")) {
                    kept.add(line);
                }
            }
            Files.write(file.toPath(), kept, StandardCharsets.UTF_8);
        } catch (IOException e) {
            Bukkit.getLogger().warning(AranarthCore.LOG_PREFIX + "Failed to forget the wild groupings of " + worldName
                    + ": " + e.getMessage());
        }
    }
}
