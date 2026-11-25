package com.aearost.aranarthcore.utils;

import com.aearost.aranarthcore.AranarthCore;
import com.aearost.aranarthcore.objects.AranarthPlayer;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.Levelled;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.block.BlockFromToEvent;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Handles the geyser particles at the base of waterfalls.
 */
public class WaterfallUtils {

    private static final int RADIUS = 32;
    private static final int RADIUS_SQUARED = RADIUS * RADIUS;
    private static final int CHUNK_RADIUS = 2;
    private static final long RESCAN_MILLIS = 15_000;
    /** How long a chunk can go without a nearby player before its cached spots are discarded. */
    private static final long EVICT_MILLIS = 60_000;
    /** Maximum chunk snapshots taken per refresh, to cap the main thread cost when many players move at once. */
    private static final int SNAPSHOT_BUDGET = 12;
    /** Maximum waterfall spots shown to a single player per particle frame. */
    private static final int MAX_SPOTS_PER_PLAYER = 48;
    private static final int SPOT_SIZE = 4;
    // Sides of the falling water that are exposed to air, where the particles are placed
    private static final int SIDE_NORTH = 1;
    private static final int SIDE_SOUTH = 2;
    private static final int SIDE_WEST = 4;
    private static final int SIDE_EAST = 8;

    private static final Map<UUID, Map<Long, CachedChunk>> cache = new ConcurrentHashMap<>();
    // Reused main thread buffers for choosing the nearest spots, so spawning allocates nothing per frame
    private static long[] rangeKeys = new long[256];
    private static int[] rangeCoords = new int[256 * SPOT_SIZE];

    // Timings logged to console so the performance cost can be confirmed on the live servers
    private static final TaskTimings particleTimings = new TaskTimings();
    private static final TaskTimings refreshTimings = new TaskTimings();
    private static final AtomicLong asyncScans = new AtomicLong();
    private static final AtomicLong asyncScanNanos = new AtomicLong();
    private static long packetsSent = 0;
    private static long snapshotsTaken = 0;
    private static long lastTimingsLog = 0;

    // The geyser particle settings, resolved from config.yml
    private static Particle.GeyserBase particleData = null;
    private static double particleSpread = 0;
    private static long particleFrame = 0;

    /**
     * Main thread time used by one of the repeating waterfall tasks.
     */
    private static class TaskTimings {
        long runs = 0;
        long totalNanos = 0;
        long maxNanos = 0;

        void record(long nanos) {
            runs++;
            totalNanos += nanos;
            maxNanos = Math.max(maxNanos, nanos);
        }

        String average() {
            return runs == 0 ? "-" : formatMillis(totalNanos / runs);
        }

        String max() {
            return formatMillis(maxNanos);
        }

        void reset() {
            runs = 0;
            totalNanos = 0;
            maxNanos = 0;
        }
    }

    /**
     * Cached waterfall landing spots of a single chunk.
     */
    private static class CachedChunk {
        // Only accessed on the main thread, as async scan results are applied back on the main thread
        /** Flattened x, y, z block coordinates and exposed side mask (see SIDE_*) of each landing spot. */
        int[] spots = new int[0];
        long scannedAt = 0;
        long lastNeeded = 0;
        /** Whether water has started falling in the chunk since it was last snapshotted. */
        boolean dirty = false;
        /** Also cleared by the async scan if it fails, so it must be volatile. */
        volatile boolean scanning = false;
    }

    /**
     * Flags the chunk for a re-scan when water starts falling in it, so new waterfalls appear within a second.
     *
     * @param e The event.
     */
    public static void markFlowDirty(BlockFromToEvent e) {
        if (e.getFace() != BlockFace.DOWN || e.getBlock().getType() != Material.WATER) {
            return;
        }
        Block to = e.getToBlock();
        Map<Long, CachedChunk> worldCache = cache.get(to.getWorld().getUID());
        if (worldCache == null) {
            return;
        }
        CachedChunk entry = worldCache.get(getChunkKey(to.getX() >> 4, to.getZ() >> 4));
        if (entry != null) {
            entry.dirty = true;
        }
    }

    /**
     * Re-validates the cached spots of nearby chunks, takes snapshots of nearby chunks that are new, and scans them asynchronously.
     */
    public static void refreshCache() {
        if (!isEnabled()) {
            cache.clear();
            return;
        }
        long start = System.nanoTime();
        long now = System.currentTimeMillis();
        List<CachedChunk> neverScanned = new ArrayList<>();
        List<CachedChunk> dirty = new ArrayList<>();
        List<CachedChunk> stale = new ArrayList<>();
        Set<CachedChunk> visited = new HashSet<>();
        Map<CachedChunk, long[]> coords = new HashMap<>();
        Map<CachedChunk, World> worlds = new HashMap<>();
        int players = 0;

        for (Player player : Bukkit.getOnlinePlayers()) {
            if (!isEligible(player)) {
                continue;
            }
            World world = player.getWorld();
            Map<Long, CachedChunk> worldCache = cache.computeIfAbsent(world.getUID(), k -> new ConcurrentHashMap<>());
            players++;
            int pcx = player.getLocation().getBlockX() >> 4;
            int pcz = player.getLocation().getBlockZ() >> 4;

            for (int cx = pcx - CHUNK_RADIUS; cx <= pcx + CHUNK_RADIUS; cx++) {
                for (int cz = pcz - CHUNK_RADIUS; cz <= pcz + CHUNK_RADIUS; cz++) {
                    CachedChunk entry = worldCache.computeIfAbsent(getChunkKey(cx, cz), k -> new CachedChunk());
                    entry.lastNeeded = now;
                    if (!visited.add(entry) || entry.scanning || !world.isChunkLoaded(cx, cz)) {
                        continue;
                    }
                    // Drops spots whose waterfall was removed and updates which sides are exposed to air
                    entry.spots = validateSpots(entry.spots, world);

                    List<CachedChunk> queue;
                    if (entry.scannedAt == 0) {
                        queue = neverScanned;
                    } else if (entry.dirty) {
                        queue = dirty;
                    } else if (now - entry.scannedAt >= RESCAN_MILLIS) {
                        queue = stale;
                    } else {
                        continue;
                    }
                    queue.add(entry);
                    coords.put(entry, new long[]{cx, cz});
                    worlds.put(entry, world);
                }
            }
        }

        // Unscanned chunks first, then chunks where water started falling, then the periodic re-scans
        List<CachedChunk> toScan = new ArrayList<>(neverScanned);
        toScan.addAll(dirty);
        toScan.addAll(stale);
        int taken = 0;
        for (CachedChunk entry : toScan) {
            if (taken >= SNAPSHOT_BUDGET) {
                break;
            }
            World world = worlds.get(entry);
            long[] xz = coords.get(entry);
            ChunkSnapshot snapshot = world.getChunkAt((int) xz[0], (int) xz[1]).getChunkSnapshot(false, false, false);
            int minY = world.getMinHeight();
            int maxY = world.getMaxHeight();
            entry.scanning = true;
            // Cleared at snapshot time so any flow after this point flags the chunk again
            entry.dirty = false;
            taken++;
            int chunkX = (int) xz[0];
            int chunkZ = (int) xz[1];
            Bukkit.getScheduler().runTaskAsynchronously(AranarthCore.getInstance(), () -> {
                long scanStart = System.nanoTime();
                int[] found;
                try {
                    found = scanSnapshot(snapshot, minY, maxY);
                } catch (RuntimeException ex) {
                    entry.scanning = false;
                    throw ex;
                } finally {
                    asyncScanNanos.addAndGet(System.nanoTime() - scanStart);
                    asyncScans.incrementAndGet();
                }
                // Exposed sides can cross into neighbouring chunks, so they are resolved on the main thread
                Bukkit.getScheduler().runTask(AranarthCore.getInstance(), () -> {
                    try {
                        entry.spots = world.isChunkLoaded(chunkX, chunkZ) ? validateSpots(found, world) : new int[0];
                        entry.scannedAt = System.currentTimeMillis();
                    } finally {
                        entry.scanning = false;
                    }
                });
            });
        }

        // Discard chunks no longer near any player
        for (Map<Long, CachedChunk> worldCache : cache.values()) {
            worldCache.values().removeIf(entry -> now - entry.lastNeeded > EVICT_MILLIS);
        }

        refreshTimings.record(System.nanoTime() - start);
        snapshotsTaken += taken;
        logTimingsIfDue(now, players);
    }

    /**
     * Spawns the geyser particles at every cached waterfall spot near each player.
     */
    public static void spawnParticles() {
        if (!isEnabled()) {
            return;
        }
        // The task runs every 2 ticks, so only every Nth run sends particles for longer configured intervals
        int intervalTicks = Math.max(2, AranarthCore.getInstance().getConfig().getInt("waterfalls.particle-interval-ticks", 2));
        if (++particleFrame % Math.max(1, intervalTicks / 2) != 0) {
            return;
        }
        long start = System.nanoTime();
        resolveParticle();
        ThreadLocalRandom random = ThreadLocalRandom.current();
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (!isEligible(player)) {
                continue;
            }
            AranarthPlayer aranarthPlayer = AranarthUtils.getPlayer(player.getUniqueId());
            // Skips the frame based on the player's particle setting
            if (random.nextInt(100) >= aranarthPlayer.getParticleNum()) {
                continue;
            }
            Map<Long, CachedChunk> worldCache = cache.get(player.getWorld().getUID());
            if (worldCache == null) {
                continue;
            }
            Location loc = player.getLocation();
            packetsSent += spawnForPlayer(player, worldCache, loc.getX(), loc.getY(), loc.getZ());
        }
        particleTimings.record(System.nanoTime() - start);
    }

    /**
     * Sends the geyser particles of every cached waterfall spot within range of the player.
     *
     * @param player The player to send the particles to.
     * @param worldCache The cached chunks of the player's world.
     * @param px The x coordinate of the player.
     * @param py The y coordinate of the player.
     * @param pz The z coordinate of the player.
     * @return The number of particle packets sent.
     */
    private static int spawnForPlayer(Player player, Map<Long, CachedChunk> worldCache, double px, double py, double pz) {
        int pcx = (int) Math.floor(px) >> 4;
        int pcz = (int) Math.floor(pz) >> 4;
        int inRange = 0;

        // Collects every spot in range, keyed by distance so the nearest come first when over the cap
        for (int cx = pcx - CHUNK_RADIUS; cx <= pcx + CHUNK_RADIUS; cx++) {
            for (int cz = pcz - CHUNK_RADIUS; cz <= pcz + CHUNK_RADIUS; cz++) {
                CachedChunk entry = worldCache.get(getChunkKey(cx, cz));
                if (entry == null) {
                    continue;
                }
                int[] spots = entry.spots;
                for (int i = 0; i < spots.length; i += SPOT_SIZE) {
                    // Spots fully surrounded by water, such as the middle of a wide waterfall, have no particles
                    if (spots[i + 3] == 0) {
                        continue;
                    }
                    double dx = spots[i] + 0.5 - px;
                    double dy = spots[i + 1] + 1.2 - py;
                    double dz = spots[i + 2] + 0.5 - pz;
                    double distanceSquared = dx * dx + dy * dy + dz * dz;
                    if (distanceSquared > RADIUS_SQUARED) {
                        continue;
                    }
                    if (inRange == rangeKeys.length) {
                        rangeKeys = Arrays.copyOf(rangeKeys, inRange * 2);
                        rangeCoords = Arrays.copyOf(rangeCoords, inRange * 2 * SPOT_SIZE);
                    }
                    // Distance in the high bits, index in the low bits, so sorting the keys sorts by distance
                    rangeKeys[inRange] = ((long) distanceSquared << 20) | inRange;
                    System.arraycopy(spots, i, rangeCoords, inRange * SPOT_SIZE, SPOT_SIZE);
                    inRange++;
                }
            }
        }

        int spotsShown = Math.min(inRange, MAX_SPOTS_PER_PLAYER);
        if (inRange > MAX_SPOTS_PER_PLAYER) {
            Arrays.sort(rangeKeys, 0, inRange);
        }
        int packets = 0;
        for (int i = 0; i < spotsShown; i++) {
            int index = (inRange > MAX_SPOTS_PER_PLAYER ? (int) (rangeKeys[i] & 0xFFFFF) : i) * SPOT_SIZE;
            int x = rangeCoords[index];
            // Geysers start at the water surface, which is the top of the landing source block
            double y = rangeCoords[index + 1] + 1.0;
            int z = rangeCoords[index + 2];
            int sides = rangeCoords[index + 3];
            // One particle on the middle of each edge exposed to air, spread only along that edge
            if ((sides & SIDE_NORTH) != 0) {
                spawnEdgeParticle(player, x + 0.5, y, z, particleSpread, 0);
                packets++;
            }
            if ((sides & SIDE_SOUTH) != 0) {
                spawnEdgeParticle(player, x + 0.5, y, z + 1, particleSpread, 0);
                packets++;
            }
            if ((sides & SIDE_WEST) != 0) {
                spawnEdgeParticle(player, x, y, z + 0.5, 0, particleSpread);
                packets++;
            }
            if ((sides & SIDE_EAST) != 0) {
                spawnEdgeParticle(player, x + 1, y, z + 0.5, 0, particleSpread);
                packets++;
            }
        }
        return packets;
    }

    /**
     * Sends the configured particle to the player at one edge of a waterfall.
     *
     * @param player The player to send the particle to.
     * @param x The x coordinate of the edge.
     * @param y The y coordinate of the edge.
     * @param z The z coordinate of the edge.
     * @param spreadX The random spread along the x axis.
     * @param spreadZ The random spread along the z axis.
     */
    private static void spawnEdgeParticle(Player player, double x, double y, double z, double spreadX, double spreadZ) {
        player.spawnParticle(Particle.GEYSER_BASE, x, y, z, 1, spreadX, 0, spreadZ, 0, particleData);
    }

    /**
     * Logs how much main thread time the waterfall tasks used since the last log, if timings are enabled.
     *
     * @param now The current time in milliseconds.
     * @param players The number of players near waterfalls that were processed this refresh.
     */
    private static void logTimingsIfDue(long now, int players) {
        long intervalMillis = Math.max(10, AranarthCore.getInstance().getConfig().getInt("waterfalls.timings-interval-seconds", 300)) * 1000L;
        if (lastTimingsLog == 0) {
            lastTimingsLog = now;
            return;
        }
        if (now - lastTimingsLog < intervalMillis) {
            return;
        }
        if (AranarthCore.getInstance().getConfig().getBoolean("waterfalls.timings", true) && players > 0) {
            int cachedChunks = 0;
            int spots = 0;
            for (Map<Long, CachedChunk> worldCache : cache.values()) {
                cachedChunks += worldCache.size();
                for (CachedChunk entry : worldCache.values()) {
                    spots += entry.spots.length / SPOT_SIZE;
                }
            }
            long scans = asyncScans.getAndSet(0);
            long scanNanos = asyncScanNanos.getAndSet(0);
            String asyncAvg = scans == 0 ? "-" : formatMillis(scanNanos / scans);
            Bukkit.getLogger().info("[AC] Waterfall timings (last " + (now - lastTimingsLog) / 1000 + "s): "
                    + "particles avg " + particleTimings.average() + " max " + particleTimings.max()
                    + " (" + packetsSent + " packets) | refresh avg " + refreshTimings.average() + " max " + refreshTimings.max()
                    + " (" + snapshotsTaken + " snapshots, async scan avg " + asyncAvg + ") | "
                    + players + " players, " + cachedChunks + " cached chunks, " + spots + " waterfall spots");
        } else {
            asyncScans.set(0);
            asyncScanNanos.set(0);
        }
        particleTimings.reset();
        refreshTimings.reset();
        packetsSent = 0;
        snapshotsTaken = 0;
        lastTimingsLog = now;
    }

    /**
     * Updates the geyser particle settings from config.yml, only rebuilding the particle data when they change.
     */
    private static void resolveParticle() {
        FileConfiguration config = AranarthCore.getInstance().getConfig();
        int waterBlocks = Math.max(1, config.getInt("waterfalls.geyser-water-blocks", 2));
        float burstImpulse = (float) config.getDouble("waterfalls.geyser-burst-impulse", 1.0);
        particleSpread = Math.max(0, config.getDouble("waterfalls.particle-spread", 0));
        if (particleData == null || particleData.getWaterBlocks() != waterBlocks || particleData.getBurstImpulse() != burstImpulse) {
            particleData = new Particle.GeyserBase(waterBlocks, burstImpulse);
        }
    }

    /**
     * Provides whether the waterfall particles are enabled in config.yml.
     *
     * @return Confirmation whether the waterfall particles are enabled.
     */
    private static boolean isEnabled() {
        return AranarthCore.getInstance().getConfig().getBoolean("waterfalls.enabled", true);
    }

    /**
     * Formats nanoseconds as milliseconds with 3 decimals.
     *
     * @param nanos The duration in nanoseconds.
     * @return The formatted duration.
     */
    private static String formatMillis(long nanos) {
        return String.format("%.3fms", nanos / 1_000_000.0);
    }

    /**
     * Finds every waterfall landing spot in the chunk snapshot.
     * @param snapshot The snapshot of the chunk.
     * @param minY The minimum height of the world.
     * @param maxY The maximum height of the world.
     * @return The flattened x, y, z block coordinates and empty side mask of each landing spot.
     */
    private static int[] scanSnapshot(ChunkSnapshot snapshot, int minY, int maxY) {
        int baseX = snapshot.getX() << 4;
        int baseZ = snapshot.getZ() << 4;
        List<Integer> found = new ArrayList<>();

        for (int x = 0; x < 16; x++) {
            for (int z = 0; z < 16; z++) {
                for (int y = minY; y < maxY - 2; y++) {
                    // Cheap type checks first so the BlockData is only read for water columns
                    if (snapshot.getBlockType(x, y, z) != Material.WATER
                            || snapshot.getBlockType(x, y + 1, z) != Material.WATER
                            || snapshot.getBlockType(x, y + 2, z) != Material.WATER) {
                        continue;
                    }
                    if (getLevel(snapshot.getBlockData(x, y, z)) != 0) {
                        continue;
                    }
                    // Falling water has a level of 8 or higher
                    if (getLevel(snapshot.getBlockData(x, y + 1, z)) < 8) {
                        continue;
                    }
                    if (getLevel(snapshot.getBlockData(x, y + 2, z)) == 0) {
                        continue;
                    }
                    found.add(baseX + x);
                    found.add(y);
                    found.add(baseZ + z);
                    found.add(0);
                }
            }
        }

        int[] spots = new int[found.size()];
        for (int i = 0; i < spots.length; i++) {
            spots[i] = found.get(i);
        }
        return spots;
    }

    /**
     * Removes any spots that are no longer waterfall landings, and updates which sides of each spot's
     * falling water are exposed to air.
     *
     * @param spots The flattened spots.
     * @param world The world of the spots.
     * @return The flattened spots that are still waterfall landings, with their exposed sides.
     */
    private static int[] validateSpots(int[] spots, World world) {
        if (spots.length == 0) {
            return spots;
        }
        int[] kept = new int[spots.length];
        int size = 0;
        for (int i = 0; i < spots.length; i += SPOT_SIZE) {
            Block landing = world.getBlockAt(spots[i], spots[i + 1], spots[i + 2]);
            if (landing.getType() != Material.WATER || getLevel(landing.getBlockData()) != 0) {
                continue;
            }
            Block falling = landing.getRelative(BlockFace.UP);
            if (falling.getType() != Material.WATER || getLevel(falling.getBlockData()) < 8) {
                continue;
            }
            int sides = 0;
            if (isExposed(world, falling, 0, -1)) {
                sides |= SIDE_NORTH;
            }
            if (isExposed(world, falling, 0, 1)) {
                sides |= SIDE_SOUTH;
            }
            if (isExposed(world, falling, -1, 0)) {
                sides |= SIDE_WEST;
            }
            if (isExposed(world, falling, 1, 0)) {
                sides |= SIDE_EAST;
            }
            kept[size++] = spots[i];
            kept[size++] = spots[i + 1];
            kept[size++] = spots[i + 2];
            kept[size++] = sides;
        }
        return size == spots.length ? kept : Arrays.copyOf(kept, size);
    }

    /**
     * Provides whether the block beside the falling water is air.
     *
     * @param world The world.
     * @param falling The falling water block.
     * @param dx The x direction of the side.
     * @param dz The z direction of the side.
     * @return Confirmation whether that side is exposed to air.
     */
    private static boolean isExposed(World world, Block falling, int dx, int dz) {
        int x = falling.getX() + dx;
        int z = falling.getZ() + dz;
        if ((x >> 4 != falling.getX() >> 4 || z >> 4 != falling.getZ() >> 4) && !world.isChunkLoaded(x >> 4, z >> 4)) {
            return false;
        }
        return world.getBlockAt(x, falling.getY(), z).getType().isAir();
    }

    /**
     * Provides the level of the water block data.
     *
     * @param data The block data.
     * @return The level, or 0 if the data is not levelled.
     */
    private static int getLevel(BlockData data) {
        return data instanceof Levelled levelled ? levelled.getLevel() : 0;
    }

    /**
     * Provides whether the player should see waterfall particles.
     *
     * @param player The player.
     * @return Confirmation whether the player should see waterfall particles.
     */
    private static boolean isEligible(Player player) {
        if (player.getWorld().getEnvironment() != World.Environment.NORMAL) {
            return false;
        }
        AranarthPlayer aranarthPlayer = AranarthUtils.getPlayer(player.getUniqueId());
        return aranarthPlayer != null && aranarthPlayer.getParticleNum() > 0;
    }

    /**
     * Provides a unique key for the chunk coordinates.
     *
     * @param cx The chunk x coordinate.
     * @param cz The chunk z coordinate.
     * @return The chunk key.
     */
    private static long getChunkKey(int cx, int cz) {
        return ((long) cz << 32) | (cx & 0xFFFFFFFFL);
    }
}
