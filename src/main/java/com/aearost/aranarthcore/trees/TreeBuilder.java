package com.aearost.aranarthcore.trees;

import com.aearost.aranarthcore.objects.Dominion;
import com.aearost.aranarthcore.utils.AranarthUtils;
import com.aearost.aranarthcore.utils.DominionUtils;
import com.aearost.aranarthcore.utils.ShopIslandUtils;
import com.gmail.nossr50.mcMMO;
import org.bukkit.Axis;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Tag;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.Orientable;
import org.bukkit.block.data.Waterlogged;
import org.bukkit.block.data.type.Leaves;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Random;
import java.util.Set;
import java.util.UUID;

/**
 * Collects the blocks of a custom tree, then resolves them against the world so that the tree never replaces
 * existing builds, never grows into other protected land, and only keeps leaves that will not decay.
 */
public class TreeBuilder {

    public enum Layer {
        DECOR, LEAF, LOG
    }

    public record Pos(int x, int y, int z) {
        public Pos offset(int dx, int dy, int dz) {
            return new Pos(x + dx, y + dy, z + dz);
        }
    }

    /**
     * A single block of the tree.
     * @param data The block data.
     * @param layer The layer.
     * @param support The block that must remain for this block to survive, or null if it supports itself.
     * @param replaceGround Whether this block may replace the ground, used by roots.
     * @param replaceWater Whether this block may replace water, used by mangrove roots.
     */
    private record Entry(BlockData data, Layer layer, Pos support, boolean replaceGround, boolean replaceWater) {}

    private static final int[][] NEIGHBOURS = {{1, 0, 0}, {-1, 0, 0}, {0, 1, 0}, {0, -1, 0}, {0, 0, 1}, {0, 0, -1}};

    private final World world;
    private final Random random;
    private final Material woodType;
    private LeafPicker leafPicker;
    private final long noiseSeed;
    private double porosityThreshold;
    private double[] noiseSamples;

    /** Chooses the material of the leaf at a position, used for trees with mixed leaves. */
    public interface LeafPicker {
        Material pick(int x, int y, int z);
    }

    private static final Set<Material> SOFT_GROUND = EnumSet.of(
            Material.DIRT, Material.GRASS_BLOCK, Material.COARSE_DIRT, Material.PODZOL, Material.ROOTED_DIRT,
            Material.MYCELIUM, Material.MUD, Material.MUDDY_MANGROVE_ROOTS, Material.MOSS_BLOCK, Material.PALE_MOSS_BLOCK,
            Material.SAND, Material.RED_SAND, Material.GRAVEL, Material.CLAY, Material.DIRT_PATH,
            Material.SOUL_SAND, Material.SOUL_SOIL, Material.SNOW_BLOCK);
    private final Map<Pos, Entry> entries = new LinkedHashMap<>();
    private final Map<String, String> info = new LinkedHashMap<>();

    public TreeBuilder(World world, Random random, Material woodType, Material leafType) {
        this.world = world;
        this.random = random;
        this.woodType = woodType;
        this.leafPicker = (x, y, z) -> leafType;
        this.noiseSeed = random.nextLong();
        setPorosity(0.2);
    }

    /**
     * Records a parameter the design was generated with, so that liked trees can be traced back to their settings.
     */
    public void info(String key, Object value) {
        info.put(key, value instanceof Double d ? String.format("%.2f", d) : String.valueOf(value));
    }

    /**
     * Provides the parameters the design was generated with.
     */
    public Map<String, String> getInfo() {
        return info;
    }

    public World getWorld() {
        return world;
    }

    public Random getRandom() {
        return random;
    }

    /**
     * Sets how the material of each leaf is chosen, used for trees with mixed leaves.
     */
    public void setLeafPicker(LeafPicker leafPicker) {
        this.leafPicker = leafPicker;
    }

    /**
     * Mixes a second type of leaf into the tree in natural looking patches.
     * @param base The main leaf.
     * @param other The leaf mixed in.
     * @param ratio Roughly how much of the canopy is the mixed in leaf.
     */
    public void setLeafMix(Material base, Material other, double ratio) {
        double threshold = quantile(ratio);
        this.leafPicker = (x, y, z) -> noise(x / 3.0, y / 3.0, z / 3.0, 1) < threshold ? other : base;
    }

    /**
     * Sets roughly how much of the inside of leaf clusters is left as air pockets.
     */
    public void setPorosity(double porosity) {
        this.porosityThreshold = quantile(porosity);
    }

    /**
     * Provides the noise value below which roughly the given share of all noise values fall,
     * as the noise is weighted towards the middle rather than spread evenly.
     */
    private double quantile(double share) {
        if (noiseSamples == null) {
            noiseSamples = new double[1024];
            for (int i = 0; i < noiseSamples.length; i++) {
                noiseSamples[i] = noise(random.nextDouble() * 64, random.nextDouble() * 64, random.nextDouble() * 64, 7);
            }
            Arrays.sort(noiseSamples);
        }
        int index = (int) Math.round(Math.max(0, Math.min(1, share)) * (noiseSamples.length - 1));
        return share <= 0 ? -1 : noiseSamples[index];
    }

    /**
     * Smooth value noise between 0 and 1, used to shape air pockets and leaf patches.
     */
    public double noise(double x, double y, double z, int channel) {
        int x0 = (int) Math.floor(x);
        int y0 = (int) Math.floor(y);
        int z0 = (int) Math.floor(z);
        double fx = smooth(x - x0);
        double fy = smooth(y - y0);
        double fz = smooth(z - z0);
        double result = 0;
        for (int dx = 0; dx <= 1; dx++) {
            for (int dy = 0; dy <= 1; dy++) {
                for (int dz = 0; dz <= 1; dz++) {
                    double weight = (dx == 0 ? 1 - fx : fx) * (dy == 0 ? 1 - fy : fy) * (dz == 0 ? 1 - fz : fz);
                    result += weight * hash(x0 + dx, y0 + dy, z0 + dz, channel);
                }
            }
        }
        return result;
    }

    private static double smooth(double t) {
        return t * t * (3 - 2 * t);
    }

    private double hash(int x, int y, int z, int channel) {
        long h = noiseSeed + channel * 0x9E3779B97F4A7C15L;
        h ^= x * 0x632BE59BD9B4E019L;
        h ^= y * 0x85157AF5L;
        h ^= z * 0xC2B2AE3D27D4EB4FL;
        h = (h ^ (h >>> 31)) * 0xBF58476D1CE4E5B9L;
        h ^= h >>> 29;
        return (h >>> 11) * 0x1.0p-53;
    }

    private void put(Pos pos, Entry entry) {
        Entry existing = entries.get(pos);
        if (existing == null || existing.layer().ordinal() < entry.layer().ordinal()) {
            entries.put(pos, entry);
        }
    }

    /**
     * Places a block of the trunk or a branch. Trees are built from wood rather than logs so that bark shows on every side.
     */
    public void log(int x, int y, int z, Axis axis) {
        put(new Pos(x, y, z), new Entry(axisData(woodType, axis), Layer.LOG, null, false, false));
    }

    public void wood(int x, int y, int z) {
        put(new Pos(x, y, z), new Entry(axisData(woodType, Axis.Y), Layer.LOG, null, false, false));
    }

    /**
     * Places a block of the trunk that may also sink into the ground, such as roots.
     */
    public void root(int x, int y, int z, Material material, Axis axis, boolean replaceWater) {
        put(new Pos(x, y, z), new Entry(axisData(material, axis), Layer.LOG, null, true, replaceWater));
    }

    /**
     * Replaces a block of soft ground beneath the tree, such as turning dirt into moss.
     */
    public void soil(int x, int y, int z, Material material) {
        put(new Pos(x, y, z), new Entry(material.createBlockData(), Layer.LOG, null, true, false));
    }

    public void leaf(int x, int y, int z) {
        put(new Pos(x, y, z), new Entry(leafPicker.pick(x, y, z).createBlockData(), Layer.LEAF, null, false, false));
    }

    /**
     * Removes a leaf, such as to thin out a crown. Any other part of the tree at the position is kept.
     */
    public void removeLeaf(int x, int y, int z) {
        Pos pos = new Pos(x, y, z);
        Entry entry = entries.get(pos);
        if (entry != null && entry.layer() == Layer.LEAF) {
            entries.remove(pos);
        }
    }

    /**
     * Places a non-log block that forms the body of the tree, such as a wart block or shroomlight.
     */
    public void solid(int x, int y, int z, Material material) {
        put(new Pos(x, y, z), new Entry(material.createBlockData(), Layer.LEAF, null, false, false));
    }

    /**
     * Places a decoration that only survives while the block at the support offset remains.
     */
    public boolean decor(int x, int y, int z, BlockData data, int sx, int sy, int sz) {
        Pos pos = new Pos(x, y, z);
        if (entries.containsKey(pos)) {
            return false;
        }
        entries.put(pos, new Entry(data, Layer.DECOR, pos.offset(sx, sy, sz), false, false));
        return true;
    }

    private BlockData axisData(Material material, Axis axis) {
        BlockData data = material.createBlockData();
        if (data instanceof Orientable orientable && orientable.getAxes().contains(axis)) {
            orientable.setAxis(axis);
        }
        return data;
    }

    public boolean has(int x, int y, int z) {
        return entries.containsKey(new Pos(x, y, z));
    }

    public boolean isLeaf(int x, int y, int z) {
        Entry entry = entries.get(new Pos(x, y, z));
        return entry != null && entry.layer() == Layer.LEAF;
    }

    /**
     * Provides the material of the part of the tree at the position, or null if there is none.
     */
    public Material typeAt(int x, int y, int z) {
        Entry entry = entries.get(new Pos(x, y, z));
        return entry == null ? null : entry.data().getMaterial();
    }

    /**
     * Provides a snapshot of every block of the given layer, used to decorate the tree.
     */
    public List<Pos> getPositions(Layer layer) {
        List<Pos> positions = new ArrayList<>();
        for (Map.Entry<Pos, Entry> entry : entries.entrySet()) {
            if (entry.getValue().layer() == layer) {
                positions.add(entry.getKey());
            }
        }
        return positions;
    }

    /**
     * Fills a vertical trunk of the given radius around a center point.
     */
    public void trunk(double cx, double cz, int y0, int y1, double radius) {
        for (int y = y0; y <= y1; y++) {
            for (int x = (int) Math.floor(cx - radius - 1); x <= cx + radius + 1; x++) {
                for (int z = (int) Math.floor(cz - radius - 1); z <= cz + radius + 1; z++) {
                    double dx = x + 0.5 - cx;
                    double dz = z + 0.5 - cz;
                    if (dx * dx + dz * dz <= radius * radius + 0.01) {
                        log(x, y, z, Axis.Y);
                    }
                }
            }
        }
    }

    /**
     * Draws a thick branch between two points, tapering from one radius to another.
     */
    public void thickBranch(double x0, double y0, double z0, double x1, double y1, double z1, double r0, double r1) {
        branch(x0, y0, z0, x1, y1, z1);
        thickLine(x0, y0, z0, x1, y1, z1, r0, r1, (x, y, z, axis) -> wood(x, y, z));
    }

    /**
     * Draws a thick root between two points, tapering from one radius to another, which may sink into soft ground and water.
     */
    public void thickRoot(double x0, double y0, double z0, double x1, double y1, double z1, double r0, double r1, Material material) {
        rootLine(x0, y0, z0, x1, y1, z1, material, true);
        thickLine(x0, y0, z0, x1, y1, z1, r0, r1, (x, y, z, axis) -> root(x, y, z, material, Axis.Y, true));
    }

    private void thickLine(double x0, double y0, double z0, double x1, double y1, double z1, double r0, double r1,
                           CellConsumer consumer) {
        double length = Math.sqrt((x1 - x0) * (x1 - x0) + (y1 - y0) * (y1 - y0) + (z1 - z0) * (z1 - z0));
        int steps = Math.max(1, (int) Math.ceil(length * 2));
        for (int i = 0; i <= steps; i++) {
            double p = (double) i / steps;
            double px = x0 + (x1 - x0) * p;
            double py = y0 + (y1 - y0) * p;
            double pz = z0 + (z1 - z0) * p;
            double radius = r0 + (r1 - r0) * p;
            int reach = (int) Math.ceil(radius) + 1;
            for (int bx = (int) Math.floor(px) - reach; bx <= px + reach; bx++) {
                for (int by = (int) Math.floor(py) - reach; by <= py + reach; by++) {
                    for (int bz = (int) Math.floor(pz) - reach; bz <= pz + reach; bz++) {
                        double dx = bx + 0.5 - px;
                        double dy = by + 0.5 - py;
                        double dz = bz + 0.5 - pz;
                        if (dx * dx + dy * dy + dz * dz <= radius * radius + 0.25) {
                            consumer.accept(bx, by, bz, Axis.Y);
                        }
                    }
                }
            }
        }
    }

    /**
     * Draws a face-connected line of logs between two points, oriented along the line.
     */
    public void branch(double x0, double y0, double z0, double x1, double y1, double z1) {
        line(x0, y0, z0, x1, y1, z1, (x, y, z, axis) -> log(x, y, z, axis));
    }

    /**
     * Draws a face-connected line of roots between two points, which may sink into the ground.
     */
    public void rootLine(double x0, double y0, double z0, double x1, double y1, double z1, Material material, boolean replaceWater) {
        line(x0, y0, z0, x1, y1, z1, (x, y, z, axis) -> root(x, y, z, material, axis, replaceWater));
    }

    public interface CellConsumer {
        void accept(int x, int y, int z, Axis axis);
    }

    public void line(double x0, double y0, double z0, double x1, double y1, double z1, CellConsumer consumer) {
        double dx = x1 - x0;
        double dy = y1 - y0;
        double dz = z1 - z0;
        Axis axis;
        if (Math.abs(dy) >= Math.abs(dx) && Math.abs(dy) >= Math.abs(dz)) {
            axis = Axis.Y;
        } else if (Math.abs(dx) >= Math.abs(dz)) {
            axis = Axis.X;
        } else {
            axis = Axis.Z;
        }
        int steps = (int) Math.ceil(Math.max(Math.abs(dx), Math.max(Math.abs(dy), Math.abs(dz))) * 2) + 1;
        int px = (int) Math.floor(x0);
        int py = (int) Math.floor(y0);
        int pz = (int) Math.floor(z0);
        consumer.accept(px, py, pz, axis);
        for (int i = 1; i <= steps; i++) {
            double t = (double) i / steps;
            int nx = (int) Math.floor(x0 + dx * t);
            int ny = (int) Math.floor(y0 + dy * t);
            int nz = (int) Math.floor(z0 + dz * t);
            // Moves one axis at a time so the line stays connected by faces rather than by corners
            if (nx != px) {
                px = nx;
                consumer.accept(px, py, pz, axis);
            }
            if (nz != pz) {
                pz = nz;
                consumer.accept(px, py, pz, axis);
            }
            if (ny != py) {
                py = ny;
                consumer.accept(px, py, pz, axis);
            }
        }
    }

    /**
     * Fills an ellipsoid of leaves with a ragged, natural edge.
     * @param roughness How ragged the edge is, from 0 (smooth) to 1 (very patchy).
     */
    public void blob(double cx, double cy, double cz, double rx, double ry, double rz, double roughness) {
        for (int x = (int) Math.floor(cx - rx); x <= cx + rx; x++) {
            for (int y = (int) Math.floor(cy - ry); y <= cy + ry; y++) {
                for (int z = (int) Math.floor(cz - rz); z <= cz + rz; z++) {
                    double nx = (x + 0.5 - cx) / rx;
                    double ny = (y + 0.5 - cy) / ry;
                    double nz = (z + 0.5 - cz) / rz;
                    double d = nx * nx + ny * ny + nz * nz;
                    if (d > 1) {
                        continue;
                    }
                    double edge = 1 - roughness;
                    if (d > edge && random.nextDouble() < (d - edge) / (1 - edge + 0.0001) * 0.85) {
                        continue;
                    }
                    // Air pockets break up the canopy so it does not look like a solid mass
                    if (d > 0.12 && noise(x / 2.2, y / 2.2, z / 2.2, 2) < porosityThreshold) {
                        continue;
                    }
                    leaf(x, y, z);
                }
            }
        }
    }

    /**
     * Fills a flat disc of leaves with a ragged edge.
     */
    public void disc(double cx, int y, double cz, double radius, double roughness) {
        blob(cx, y + 0.5, cz, radius, 0.5, radius, roughness);
    }

    /**
     * Resolves the tree against the world.
     * @param origin The sapling the tree grows from, used for protections.
     * @param saplings The saplings that are consumed by the tree.
     * @return The block states to place, in order, or null if the tree would grow into something a player placed.
     */
    public List<BlockState> resolve(Location origin, Set<Block> saplings) {
        // Trees never grow into anything built by players, so the sapling waits instead
        for (Map.Entry<Pos, Entry> mapEntry : entries.entrySet()) {
            Pos pos = mapEntry.getKey();
            if (mapEntry.getValue().layer() == Layer.DECOR || pos.y() < world.getMinHeight() || pos.y() >= world.getMaxHeight()) {
                continue;
            }
            Block block = world.getBlockAt(pos.x(), pos.y(), pos.z());
            if (!block.getType().isAir() && !saplings.contains(block) && isPlayerPlaced(block)) {
                return null;
            }
        }

        Map<Pos, Entry> placed = new LinkedHashMap<>();
        Map<Long, Boolean> chunkAllowed = new HashMap<>();
        Dominion originDominion = DominionUtils.getDominionOfChunkAnywhere(origin.getChunk());
        boolean originAtSpawn = AranarthUtils.isProtectedSpawnLocation(origin);
        boolean isShopsWorld = world.getName().equals(ShopIslandUtils.SHOPS_WORLD);
        UUID originIsland = isShopsWorld ? ShopIslandUtils.getIslandOwnerAtLocation(origin) : null;

        for (Map.Entry<Pos, Entry> mapEntry : entries.entrySet()) {
            Pos pos = mapEntry.getKey();
            Entry entry = mapEntry.getValue();
            if (pos.y() < world.getMinHeight() || pos.y() >= world.getMaxHeight()) {
                continue;
            }
            Block block = world.getBlockAt(pos.x(), pos.y(), pos.z());
            if (!canReplace(block, entry, saplings)) {
                continue;
            }
            // The tree may not grow into land of another Dominion
            long chunkKey = ((long) (pos.x() >> 4) << 32) | ((pos.z() >> 4) & 0xFFFFFFFFL);
            boolean allowed = chunkAllowed.computeIfAbsent(chunkKey, key -> {
                Chunk chunk = block.getChunk();
                Dominion dominion = DominionUtils.getDominionOfChunkAnywhere(chunk);
                return dominion == null || (originDominion != null && originDominion.isSameDominion(dominion));
            });
            if (!allowed) {
                continue;
            }
            Location loc = block.getLocation();
            if (!originAtSpawn && AranarthUtils.isProtectedSpawnLocation(loc)) {
                continue;
            }
            if (isShopsWorld && !Objects.equals(originIsland, ShopIslandUtils.getIslandOwnerAtLocation(loc))) {
                continue;
            }
            placed.put(pos, entry);
        }

        Map<Pos, Integer> distances = computeLeafDistances(placed);
        // Leaves hidden inside the canopy that are too far from a log become a log, so that the canopy has no holes
        boolean converted = false;
        for (Map.Entry<Pos, Entry> mapEntry : placed.entrySet()) {
            Pos pos = mapEntry.getKey();
            if (!(mapEntry.getValue().data() instanceof Leaves) || distances.containsKey(pos)) {
                continue;
            }
            boolean enclosed = true;
            for (int[] n : NEIGHBOURS) {
                Entry neighbour = placed.get(pos.offset(n[0], n[1], n[2]));
                if (neighbour == null || neighbour.layer() == Layer.DECOR) {
                    enclosed = false;
                    break;
                }
            }
            if (enclosed) {
                mapEntry.setValue(new Entry(axisData(woodType, Axis.Y), Layer.LOG, null, false, false));
                converted = true;
            }
        }
        if (converted) {
            distances = computeLeafDistances(placed);
        }
        // Remaining leaves too far from a log would decay, so they are not placed
        final Map<Pos, Integer> finalDistances = distances;
        placed.entrySet().removeIf(e -> e.getValue().data() instanceof Leaves && !finalDistances.containsKey(e.getKey()));

        // Decorations are only kept if what holds them up is kept, checked in order so that hanging chains are validated from the top
        Set<Pos> removed = new HashSet<>();
        for (Map.Entry<Pos, Entry> mapEntry : placed.entrySet()) {
            Entry entry = mapEntry.getValue();
            if (entry.support() == null) {
                continue;
            }
            Pos support = entry.support();
            boolean supported = placed.containsKey(support) && !removed.contains(support);
            if (!supported && !placed.containsKey(support)) {
                Block supportBlock = world.getBlockAt(support.x(), support.y(), support.z());
                supported = supportBlock.getType().isSolid() && !saplings.contains(supportBlock);
            }
            if (!supported) {
                removed.add(mapEntry.getKey());
            }
        }
        removed.forEach(placed::remove);

        List<BlockState> states = new ArrayList<>();
        // The saplings are always removed, even where the tree does not replace them
        for (Block sapling : saplings) {
            if (!placed.containsKey(new Pos(sapling.getX(), sapling.getY(), sapling.getZ()))) {
                BlockState state = sapling.getState();
                state.setType(Material.AIR);
                states.add(state);
            }
        }
        for (Layer layer : new Layer[] {Layer.LOG, Layer.LEAF, Layer.DECOR}) {
            for (Map.Entry<Pos, Entry> mapEntry : placed.entrySet()) {
                Entry entry = mapEntry.getValue();
                if (entry.layer() != layer) {
                    continue;
                }
                Pos pos = mapEntry.getKey();
                Block block = world.getBlockAt(pos.x(), pos.y(), pos.z());
                BlockData data = entry.data().clone();
                if (data instanceof Leaves leaves) {
                    leaves.setPersistent(false);
                    leaves.setDistance(distances.get(pos));
                }
                if (data instanceof Waterlogged waterlogged) {
                    waterlogged.setWaterlogged(block.getType() == Material.WATER);
                }
                BlockState state = block.getState();
                state.setBlockData(data);
                states.add(state);
            }
        }
        return states;
    }

    /**
     * Calculates the vanilla leaf distance of every leaf, measured through leaves from the nearest log.
     */
    private Map<Pos, Integer> computeLeafDistances(Map<Pos, Entry> placed) {
        Map<Pos, Integer> distances = new HashMap<>();
        ArrayDeque<Pos> queue = new ArrayDeque<>();
        for (Map.Entry<Pos, Entry> mapEntry : placed.entrySet()) {
            if (Tag.LOGS.isTagged(mapEntry.getValue().data().getMaterial())) {
                distances.put(mapEntry.getKey(), 0);
                queue.add(mapEntry.getKey());
            }
        }
        while (!queue.isEmpty()) {
            Pos pos = queue.poll();
            int distance = distances.get(pos);
            if (distance >= 6) {
                continue;
            }
            for (int[] n : NEIGHBOURS) {
                Pos next = pos.offset(n[0], n[1], n[2]);
                Entry entry = placed.get(next);
                if (entry == null || !(entry.data() instanceof Leaves) || distances.containsKey(next)) {
                    continue;
                }
                distances.put(next, distance + 1);
                queue.add(next);
            }
        }
        // Only leaves are returned
        distances.entrySet().removeIf(e -> e.getValue() == 0);
        return distances;
    }

    /**
     * Determines if the block was placed by a player, either as tracked by mcMMO, or as leaves that never decay.
     */
    private boolean isPlayerPlaced(Block block) {
        if (block.getBlockData() instanceof Leaves leaves && leaves.isPersistent()) {
            return true;
        }
        return mcMMO.getChunkManager().isIneligible(block);
    }

    /**
     * Determines if the part of the tree may replace the existing block, so that the tree never destroys builds.
     */
    private boolean canReplace(Block block, Entry entry, Set<Block> saplings) {
        Material type = block.getType();
        if (type.isAir() || saplings.contains(block)) {
            return true;
        }
        if (type == Material.WATER) {
            return entry.replaceWater();
        }
        if (entry.layer() == Layer.DECOR) {
            return false;
        }
        if (Tag.REPLACEABLE.isTagged(type) || Tag.REPLACEABLE_BY_TREES.isTagged(type)) {
            // Leaves of other trees are only ever replaced by the trunk and branches
            return entry.layer() == Layer.LOG || !Tag.LEAVES.isTagged(type);
        }
        if (entry.layer() == Layer.LOG) {
            if (Tag.LEAVES.isTagged(type) || Tag.SMALL_FLOWERS.isTagged(type) || Tag.SAPLINGS.isTagged(type)) {
                return true;
            }
            if (entry.replaceGround()) {
                return SOFT_GROUND.contains(type);
            }
        }
        return false;
    }
}
