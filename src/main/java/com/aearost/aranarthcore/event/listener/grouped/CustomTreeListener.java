package com.aearost.aranarthcore.event.listener.grouped;

import com.aearost.aranarthcore.AranarthCore;
import com.aearost.aranarthcore.objects.Dominion;
import com.aearost.aranarthcore.trees.CustomTrees;
import com.aearost.aranarthcore.trees.TreeBuilder;
import com.aearost.aranarthcore.utils.CustomTreeUtils;
import com.aearost.aranarthcore.utils.DominionUtils;
import com.aearost.aranarthcore.utils.ShopIslandUtils;
import com.gmail.nossr50.mcMMO;
import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.GameRules;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.TreeType;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockExplodeEvent;
import org.bukkit.event.block.BlockFromToEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.world.ChunkLoadEvent;
import org.bukkit.event.world.ChunkUnloadEvent;
import org.bukkit.event.world.StructureGrowEvent;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Random;
import java.util.Set;
import java.util.UUID;

/**
 * Grows saplings into their custom tree, unless the player who planted them, or who bone mealed them, has big trees disabled.
 */
public class CustomTreeListener implements Listener {

    private final Set<StructureGrowEvent> customTrees = Collections.newSetFromMap(new IdentityHashMap<>());
    private static final long TIMED_GROWTH_TICKS = 600L;
    private static final double TIMED_GROWTH_CHANCE = 0.03;
    private final Random random = new Random();

    public CustomTreeListener(AranarthCore plugin) {
        Bukkit.getPluginManager().registerEvents(this, plugin);
        for (World world : Bukkit.getWorlds()) {
            for (Chunk chunk : world.getLoadedChunks()) {
                CustomTreeUtils.trackTimedSaplings(chunk);
            }
        }
        Bukkit.getScheduler().runTaskTimer(plugin, this::growTimedSaplings, TIMED_GROWTH_TICKS, TIMED_GROWTH_TICKS);
    }

    /**
     * Grows the sapling straight away through the usual growth event, so that protections and quests still apply.
     * @param block The sapling.
     * @param player The player who caused it to grow, or null if it grew on its own.
     */
    public static void growNow(Block block, Player player) {
        TreeType species = switch (block.getType()) {
            case PALE_OAK_SAPLING -> TreeType.PALE_OAK;
            case DARK_OAK_SAPLING -> TreeType.DARK_OAK;
            case BIRCH_SAPLING -> TreeType.BIRCH;
            case POPLAR_SAPLING -> TreeType.POPLAR;
            case SPRUCE_SAPLING -> TreeType.REDWOOD;
            case JUNGLE_SAPLING -> TreeType.SMALL_JUNGLE;
            case ACACIA_SAPLING -> TreeType.ACACIA;
            case CHERRY_SAPLING -> TreeType.CHERRY;
            case MANGROVE_PROPAGULE -> TreeType.MANGROVE;
            case AZALEA, FLOWERING_AZALEA -> TreeType.AZALEA;
            case BROWN_MUSHROOM -> TreeType.BROWN_MUSHROOM;
            case RED_MUSHROOM -> TreeType.RED_MUSHROOM;
            case CRIMSON_FUNGUS -> TreeType.CRIMSON_FUNGUS;
            case WARPED_FUNGUS -> TreeType.WARPED_FUNGUS;
            default -> TreeType.TREE;
        };
        Bukkit.getPluginManager().callEvent(new StructureGrowEvent(block.getLocation(), species, player != null, player,
                new ArrayList<>()));
    }

    /**
     * Gives every planted lone Dark Oak and Pale Oak sapling a chance to grow, as vanilla never grows them on their own.
     */
    private void growTimedSaplings() {
        for (Location loc : new ArrayList<>(CustomTreeUtils.getTimedSaplings())) {
            World world = loc.getWorld();
            if (world == null || !world.isChunkLoaded(loc.getBlockX() >> 4, loc.getBlockZ() >> 4)) {
                continue;
            }
            Block block = loc.getBlock();
            UUID planter = CustomTreeUtils.getPlanter(block);
            if (planter == null || !CustomTreeUtils.growsOnTimer(block.getType())) {
                CustomTreeUtils.getTimedSaplings().remove(loc);
                continue;
            }
            // Saplings in a 2x2 are grown by vanilla, and lone ones only grow for players with big trees enabled
            if (CustomTreeUtils.findSquareCorner(block, 2) != null || !CustomTreeUtils.isBigTreeEnabled(planter)) {
                continue;
            }
            Integer tickSpeed = world.getGameRuleValue(GameRules.RANDOM_TICK_SPEED);
            if (tickSpeed == null || tickSpeed <= 0 || block.getLightLevel() < 9
                    || random.nextDouble() >= TIMED_GROWTH_CHANCE * tickSpeed / 3.0) {
                continue;
            }
            growNow(block, null);
        }
    }

    @EventHandler
    public void onChunkLoad(ChunkLoadEvent e) {
        CustomTreeUtils.trackTimedSaplings(e.getChunk());
    }

    @EventHandler
    public void onChunkUnload(ChunkUnloadEvent e) {
        CustomTreeUtils.untrackTimedSaplings(e.getChunk());
    }

    /**
     * Replaces the vanilla tree with the custom tree.
     */
    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onStructureGrow(StructureGrowEvent e) {
        Block origin = e.getLocation().getBlock();
        Material type = origin.getType();
        if (!CustomTreeUtils.isGrowable(type)) {
            return;
        }

        // The largest square the sapling is part of decides which trees it can grow into
        Block corner = null;
        int size = 1;
        for (int squareSize : CustomTrees.Variant.squareSizes(type)) {
            corner = CustomTreeUtils.findSquareCorner(origin, squareSize);
            if (corner != null) {
                size = squareSize;
                break;
            }
        }
        Set<Block> saplings = new HashSet<>();
        if (corner != null) {
            for (int dx = 0; dx < size; dx++) {
                for (int dz = 0; dz < size; dz++) {
                    saplings.add(corner.getRelative(dx, 0, dz));
                }
            }
        } else {
            saplings.add(origin);
        }
        // Saplings that vanilla consumed as part of this growth are removed too
        for (BlockState state : e.getBlocks()) {
            Block block = state.getBlock();
            if (block.getType() == type) {
                saplings.add(block);
            }
        }

        // A player who bone meals the sapling decides, otherwise any planter who disabled big trees keeps it vanilla
        boolean canGrowLarge = false;
        if (e.getPlayer() != null) {
            if (!CustomTreeUtils.isBigTreeEnabled(e.getPlayer().getUniqueId())) {
                return;
            }
            canGrowLarge = CustomTreeUtils.canGrowLargeTrees(e.getPlayer().getUniqueId());
        } else {
            for (Block sapling : saplings) {
                UUID planter = CustomTreeUtils.getPlanter(sapling);
                if (planter != null && !CustomTreeUtils.isBigTreeEnabled(planter)) {
                    return;
                }
                canGrowLarge |= CustomTreeUtils.canGrowLargeTrees(planter);
            }
        }
        // The largest trees only grow for Dukes and above, so the world is not filled with them, otherwise it grows vanilla
        if (size >= CustomTreeUtils.LARGE_TREE_SIZE && !canGrowLarge) {
            return;
        }

        Block base = corner != null ? corner : origin;
        TreeBuilder tree = CustomTrees.design(type, size, base.getWorld(), new Random(), base.getX(), base.getY(), base.getZ());
        if (tree == null) {
            return;
        }
        List<BlockState> states = tree.resolve(origin.getLocation(), saplings);
        // It would grow into something a player built, into the land of another Dominion, or onto another shop island
        if (states == null || entersOtherDominion(origin, states) || entersOtherIsland(origin, states)) {
            return;
        }
        for (Block sapling : saplings) {
            CustomTreeUtils.clearPlanter(sapling);
        }
        e.getBlocks().clear();
        e.getBlocks().addAll(states);
        customTrees.add(e);
    }

    /**
     * Determines if any block of the tree is in land claimed by a different Dominion than the sapling it grows from.
     */
    private boolean entersOtherDominion(Block origin, List<BlockState> states) {
        Dominion home = DominionUtils.getDominionOfChunkAnywhere(origin.getChunk());
        UUID homeId = home == null ? null : home.getId();
        Map<Long, Boolean> checked = new HashMap<>();
        for (BlockState state : states) {
            int chunkX = state.getX() >> 4;
            int chunkZ = state.getZ() >> 4;
            long chunkKey = ((long) chunkX << 32) | (chunkZ & 0xFFFFFFFFL);
            Boolean other = checked.get(chunkKey);
            if (other == null) {
                Dominion dominion = DominionUtils.getDominionOfChunkAnywhere(origin.getWorld().getChunkAt(chunkX, chunkZ));
                other = dominion != null && !Objects.equals(dominion.getId(), homeId);
                checked.put(chunkKey, other);
            }
            if (other) {
                return true;
            }
        }
        return false;
    }

    /**
     * Determines if any block of a tree in the shops world is outside the shop island of the sapling it grows from.
     */
    private boolean entersOtherIsland(Block origin, List<BlockState> states) {
        if (!origin.getWorld().getName().equals(ShopIslandUtils.SHOPS_WORLD)) {
            return false;
        }
        UUID home = ShopIslandUtils.getIslandOwnerAtLocation(origin.getLocation());
        Map<Long, Boolean> checked = new HashMap<>();
        for (BlockState state : states) {
            // Checked once per grid cell of the shops world, as each cell holds at most one island
            long cellKey = ((long) Math.floorDiv(state.getX(), ShopIslandUtils.GRID_SIZE) << 32)
                    | (Math.floorDiv(state.getZ(), ShopIslandUtils.GRID_SIZE) & 0xFFFFFFFFL);
            Boolean other = checked.get(cellKey);
            if (other == null) {
                other = home == null || !home.equals(ShopIslandUtils.getIslandOwnerAtLocation(state.getLocation()));
                checked.put(cellKey, other);
            }
            if (other) {
                return true;
            }
        }
        return false;
    }

    /**
     * Places the custom tree once every other listener has had the chance to trim it.
     */
    @EventHandler(priority = EventPriority.MONITOR)
    public void onStructureGrowPlace(StructureGrowEvent e) {
        if (!customTrees.remove(e) || e.isCancelled()) {
            return;
        }
        List<BlockState> states = new ArrayList<>(e.getBlocks());
        e.getBlocks().clear();
        for (BlockState state : states) {
            Block block = state.getBlock();
            block.setBlockData(state.getBlockData(), false);
            // Grown trees are natural, so they are never treated as player-placed
            mcMMO.getChunkManager().setEligible(block);
        }
    }

    /**
     * Remembers who planted each sapling, so that the tree follows their big tree toggle when it grows.
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlace(BlockPlaceEvent e) {
        Block block = e.getBlock();
        if (CustomTreeUtils.isGrowable(block.getType())) {
            CustomTreeUtils.setPlanter(block, e.getPlayer().getUniqueId());
        } else {
            CustomTreeUtils.clearPlanter(block);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBreak(BlockBreakEvent e) {
        CustomTreeUtils.clearRemoved(e.getBlock());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onEntityExplode(EntityExplodeEvent e) {
        for (Block block : e.blockList()) {
            CustomTreeUtils.clearRemoved(block);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBlockExplode(BlockExplodeEvent e) {
        for (Block block : e.blockList()) {
            CustomTreeUtils.clearRemoved(block);
        }
    }

    /**
     * Saplings washed away by water or lava forget their planter.
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onFlow(BlockFromToEvent e) {
        if (CustomTreeUtils.isGrowable(e.getToBlock().getType())) {
            CustomTreeUtils.clearPlanter(e.getToBlock());
        }
    }
}
