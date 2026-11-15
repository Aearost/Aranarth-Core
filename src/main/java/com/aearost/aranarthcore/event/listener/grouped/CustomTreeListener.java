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
import org.bukkit.GameMode;
import org.bukkit.GameRules;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.TreeType;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.block.data.Hangable;
import org.bukkit.block.data.type.Sapling;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockExplodeEvent;
import org.bukkit.event.block.BlockFromToEvent;
import org.bukkit.event.block.BlockPistonExtendEvent;
import org.bukkit.event.block.BlockPistonRetractEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
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
 * Bone meal and planted saplings grow through this plugin rather than vanilla, as vanilla only grows a sapling when its own
 * tree would fit, which would stop custom trees from growing next to other trees.
 */
public class CustomTreeListener implements Listener {

    private final Set<StructureGrowEvent> customTrees = Collections.newSetFromMap(new IdentityHashMap<>());
    private static boolean growingNow = false;
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
        growingNow = true;
        try {
            Bukkit.getPluginManager().callEvent(new StructureGrowEvent(block.getLocation(), species, player != null, player,
                    new ArrayList<>()));
        } finally {
            growingNow = false;
        }
    }

    /**
     * Gives every planted sapling a chance to grow into its custom tree. Vanilla only grows a sapling when its own tree
     * would fit, so the custom tree would otherwise never grow where the vanilla one does not.
     */
    private void growTimedSaplings() {
        for (Location loc : new ArrayList<>(CustomTreeUtils.getTimedSaplings())) {
            World world = loc.getWorld();
            if (world == null || !world.isChunkLoaded(loc.getBlockX() >> 4, loc.getBlockZ() >> 4)) {
                continue;
            }
            Block block = loc.getBlock();
            if (!CustomTreeUtils.growsOnTimer(block.getType()) || CustomTreeUtils.getPlanter(block) == null) {
                CustomTreeUtils.getTimedSaplings().remove(loc);
                continue;
            }
            Integer tickSpeed = world.getGameRuleValue(GameRules.RANDOM_TICK_SPEED);
            if (tickSpeed == null || tickSpeed <= 0 || block.getLightLevel() < 9
                    || random.nextDouble() >= TIMED_GROWTH_CHANCE * tickSpeed / 3.0) {
                continue;
            }
            // Hanging propagules only age, and saplings that grow a vanilla tree are left to vanilla
            if ((block.getBlockData() instanceof Hangable hangable && hangable.isHanging()) || !growsCustomTree(block, null)) {
                continue;
            }
            growNow(block, null);
        }
    }

    /**
     * Lets bone meal grow a sapling into its custom tree, even where its vanilla tree would not fit.
     */
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBoneMealSapling(PlayerInteractEvent e) {
        ItemStack item = e.getItem();
        Block block = e.getClickedBlock();
        if (e.getAction() != Action.RIGHT_CLICK_BLOCK || block == null || item == null || item.getType() != Material.BONE_MEAL
                || e.getHand() == null || !CustomTreeUtils.growsFromBoneMeal(block.getType())
                || (block.getBlockData() instanceof Hangable hangable && hangable.isHanging())) {
            return;
        }
        Player player = e.getPlayer();
        // Saplings that grow a vanilla tree are left to vanilla bone meal
        if (!growsCustomTree(block, player)) {
            return;
        }
        e.setCancelled(true);
        if (player.getGameMode() != GameMode.CREATIVE) {
            item.setAmount(item.getAmount() - 1);
        }
        player.swingHand(e.getHand());
        Location centre = block.getLocation().add(0.5, 0.5, 0.5);
        block.getWorld().spawnParticle(Particle.HAPPY_VILLAGER, centre, 15, 0.3, 0.3, 0.3);
        block.getWorld().playSound(centre, Sound.ITEM_BONE_MEAL_USE, 1F, 1F);
        if (random.nextDouble() >= 0.45) {
            return;
        }
        if (block.getBlockData() instanceof Sapling sapling && sapling.getStage() < sapling.getMaximumStage()) {
            sapling.setStage(sapling.getStage() + 1);
            block.setBlockData(sapling, false);
            return;
        }
        growNow(block, player);
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

        Square square = findSquare(origin);
        Set<Block> saplings = square.saplings();
        // Saplings that vanilla consumed as part of this growth are removed too
        for (BlockState state : e.getBlocks()) {
            Block block = state.getBlock();
            if (block.getType() == type) {
                saplings.add(block);
            }
        }
        if (!growsCustomTree(origin, square, e.getPlayer())) {
            return;
        }
        // Planted saplings grow on the timer instead, so that they grow at the same pace wherever the vanilla tree fits
        if (!growingNow && e.getPlayer() == null && !e.isFromBonemeal() && CustomTreeUtils.growsOnTimer(type)
                && getChooser(origin, saplings, null) != null) {
            e.setCancelled(true);
            return;
        }

        final UUID chooser = getChooser(origin, saplings, e.getPlayer());
        Block base = square.corner();
        TreeBuilder tree = CustomTrees.design(type, square.size(), base.getWorld(), new Random(), base.getX(), base.getY(),
                base.getZ(), variant -> CustomTreeUtils.isTreeEnabled(chooser, variant));
        if (tree == null) {
            return;
        }
        List<BlockState> states = tree.resolve(origin.getLocation(), saplings);
        // It would grow into something a player built, into the land of another Dominion, or onto another shop island, so
        // the sapling waits instead of falling back to a vanilla tree
        if (states == null || entersOtherDominion(origin, states) || entersOtherIsland(origin, states)) {
            e.setCancelled(true);
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
     * The saplings a tree grows from.
     * @param corner The north-west sapling, or the sapling itself if it is not part of a square.
     * @param size The width of the square, or 1 if it is not part of a square.
     * @param saplings Every sapling of the square.
     */
    private record Square(Block corner, int size, Set<Block> saplings) {}

    /**
     * Provides the largest square the sapling is part of, which decides which trees it can grow into. A square only grown
     * by rare trees counts only where one of their conditions is met, otherwise a smaller square is used.
     */
    private static Square findSquare(Block origin) {
        Material type = origin.getType();
        List<Integer> normalSizes = CustomTrees.Variant.squareSizes(type);
        List<Integer> sizes = new ArrayList<>(normalSizes);
        for (int size : CustomTrees.Variant.rareSquareSizes(type)) {
            if (!sizes.contains(size)) {
                sizes.add(size);
            }
        }
        sizes.sort((a, b) -> b - a);
        for (int size : sizes) {
            Block corner = CustomTreeUtils.findSquareCorner(origin, size);
            if (corner != null && (normalSizes.contains(size) || CustomTrees.hasRare(type, size, corner))) {
                Set<Block> saplings = new HashSet<>();
                for (int dx = 0; dx < size; dx++) {
                    for (int dz = 0; dz < size; dz++) {
                        saplings.add(corner.getRelative(dx, 0, dz));
                    }
                }
                return new Square(corner, size, saplings);
            }
        }
        Set<Block> saplings = new HashSet<>();
        saplings.add(origin);
        return new Square(origin, 1, saplings);
    }

    /**
     * Determines if the sapling grows into a custom tree rather than its vanilla tree.
     * @param block The sapling.
     * @param player The player who causes it to grow, or null if it grows on its own.
     */
    public static boolean growsCustomTree(Block block, Player player) {
        if (!CustomTreeUtils.isGrowable(block.getType())) {
            return false;
        }
        Square square = findSquare(block);
        return growsCustomTree(block, square, player);
    }

    private static boolean growsCustomTree(Block origin, Square square, Player player) {
        int size = square.size();
        Set<Block> saplings = square.saplings();
        Material type = origin.getType();
        // A player who bone meals the sapling decides, otherwise any planter who disabled big trees keeps it vanilla
        boolean canGrowLarge = false;
        if (player != null) {
            if (!CustomTreeUtils.isBigTreeEnabled(player.getUniqueId())) {
                return false;
            }
            canGrowLarge = CustomTreeUtils.canGrowLargeTrees(player.getUniqueId());
        } else {
            for (Block sapling : saplings) {
                UUID planter = CustomTreeUtils.getPlanter(sapling);
                if (planter != null && !CustomTreeUtils.isBigTreeEnabled(planter)) {
                    return false;
                }
                canGrowLarge |= CustomTreeUtils.canGrowLargeTrees(planter);
            }
        }
        // The largest trees only grow for Dukes and above, so the world is not filled with them, otherwise it grows vanilla
        if (size >= CustomTreeUtils.LARGE_TREE_SIZE && !canGrowLarge) {
            return false;
        }
        // With every tree for it turned off in /trees, it grows vanilla
        UUID chooser = getChooser(origin, saplings, player);
        return CustomTrees.hasDesign(type, size, variant -> CustomTreeUtils.isTreeEnabled(chooser, variant), square.corner());
    }

    /**
     * Provides whose /trees choices decide the tree: the player bone mealing it, otherwise whoever planted it.
     * @return The UUID of the player, or null if nobody is known.
     */
    private static UUID getChooser(Block origin, Set<Block> saplings, Player player) {
        if (player != null) {
            return player.getUniqueId();
        }
        UUID planter = CustomTreeUtils.getPlanter(origin);
        if (planter != null) {
            return planter;
        }
        for (Block sapling : saplings) {
            planter = CustomTreeUtils.getPlanter(sapling);
            if (planter != null) {
                return planter;
            }
        }
        return null;
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
            CustomTreeUtils.markNoDrop(block);
        }
    }

    /**
     * Blocks grown by custom trees that could otherwise be farmed with bone meal drop nothing.
     */
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBreakNoDrop(BlockBreakEvent e) {
        if (CustomTreeUtils.isNoDrop(e.getBlock())) {
            e.setDropItems(false);
            e.setExpToDrop(0);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onEntityExplodeNoDrop(EntityExplodeEvent e) {
        removeNoDrop(e.blockList());
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onBlockExplodeNoDrop(BlockExplodeEvent e) {
        removeNoDrop(e.blockList());
    }

    /**
     * Destroys the blocks that drop nothing straight away, so that the explosion does not drop them.
     */
    private void removeNoDrop(List<Block> blocks) {
        blocks.removeIf(block -> {
            if (!CustomTreeUtils.isNoDrop(block)) {
                return false;
            }
            CustomTreeUtils.clearNoDrop(block);
            block.setType(Material.AIR, false);
            return true;
        });
    }

    /**
     * Blocks that drop nothing cannot be moved by pistons, so that they cannot be carried away from where they grew.
     */
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onPistonExtend(BlockPistonExtendEvent e) {
        if (e.getBlocks().stream().anyMatch(CustomTreeUtils::isNoDrop)) {
            e.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onPistonRetract(BlockPistonRetractEvent e) {
        if (e.getBlocks().stream().anyMatch(CustomTreeUtils::isNoDrop)) {
            e.setCancelled(true);
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
        CustomTreeUtils.clearNoDrop(block);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBreak(BlockBreakEvent e) {
        CustomTreeUtils.clearRemoved(e.getBlock());
        CustomTreeUtils.clearNoDrop(e.getBlock());
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
