package com.aearost.aranarthcore.utils;

import com.aearost.aranarthcore.AranarthCore;
import com.aearost.aranarthcore.objects.Gate;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.type.Fence;
import org.bukkit.block.data.type.Wall;
import org.bukkit.entity.Player;

import java.util.*;

/**
 * Centralizes all helper methods and information pertaining to gates.
 */
public class GateUtils {

    public static final int MAX_GATE_SIZE = 25;

    public static final Set<Material> WOODEN_GATE_MATERIALS = Set.of(
            Material.OAK_FENCE,
            Material.SPRUCE_FENCE,
            Material.BIRCH_FENCE,
            Material.JUNGLE_FENCE,
            Material.ACACIA_FENCE,
            Material.DARK_OAK_FENCE,
            Material.MANGROVE_FENCE,
            Material.CHERRY_FENCE,
            Material.BAMBOO_FENCE,
            Material.PALE_OAK_FENCE
    );

    public static final Set<Material> METAL_GATE_MATERIALS = Set.of(
            Material.IRON_BARS,
            Material.IRON_CHAIN,
            Material.COPPER_BARS,
            Material.EXPOSED_COPPER_BARS,
            Material.WEATHERED_COPPER_BARS,
            Material.OXIDIZED_COPPER_BARS,
            Material.WAXED_COPPER_BARS,
            Material.WAXED_EXPOSED_COPPER_BARS,
            Material.WAXED_WEATHERED_COPPER_BARS,
            Material.WAXED_OXIDIZED_COPPER_BARS
    );

    public static final Set<Material> NETHER_BRICK_FENCE_MATERIALS = Set.of(
            Material.NETHER_BRICK_FENCE
    );

    public static final Set<Material> WALL_MATERIALS = Set.of(
            Material.COBBLESTONE_WALL,
            Material.MOSSY_COBBLESTONE_WALL,
            Material.STONE_BRICK_WALL,
            Material.MOSSY_STONE_BRICK_WALL,
            Material.BRICK_WALL,
            Material.PRISMARINE_WALL,
            Material.RED_SANDSTONE_WALL,
            Material.SANDSTONE_WALL,
            Material.END_STONE_BRICK_WALL,
            Material.DIORITE_WALL,
            Material.ANDESITE_WALL,
            Material.GRANITE_WALL,
            Material.NETHER_BRICK_WALL,
            Material.RED_NETHER_BRICK_WALL,
            Material.BLACKSTONE_WALL,
            Material.POLISHED_BLACKSTONE_WALL,
            Material.POLISHED_BLACKSTONE_BRICK_WALL,
            Material.COBBLED_DEEPSLATE_WALL,
            Material.POLISHED_DEEPSLATE_WALL,
            Material.DEEPSLATE_BRICK_WALL,
            Material.DEEPSLATE_TILE_WALL,
            Material.TUFF_WALL,
            Material.TUFF_BRICK_WALL,
            Material.POLISHED_TUFF_WALL,
            Material.MUD_BRICK_WALL,
            Material.RESIN_BRICK_WALL
    );

    // Maps each special gate material to the place-block sound to play on open/close.
    public static final Map<Material, String> MATERIAL_SOUNDS = Map.ofEntries(
            Map.entry(Material.NETHER_BRICK_FENCE, "block.nether_brick.place"),
            Map.entry(Material.COBBLESTONE_WALL, "block.stone.place"),
            Map.entry(Material.MOSSY_COBBLESTONE_WALL, "block.stone.place"),
            Map.entry(Material.STONE_BRICK_WALL, "block.stone.place"),
            Map.entry(Material.MOSSY_STONE_BRICK_WALL, "block.stone.place"),
            Map.entry(Material.BRICK_WALL, "block.stone.place"),
            Map.entry(Material.PRISMARINE_WALL, "block.stone.place"),
            Map.entry(Material.RED_SANDSTONE_WALL, "block.stone.place"),
            Map.entry(Material.SANDSTONE_WALL, "block.stone.place"),
            Map.entry(Material.END_STONE_BRICK_WALL, "block.stone.place"),
            Map.entry(Material.DIORITE_WALL, "block.stone.place"),
            Map.entry(Material.ANDESITE_WALL, "block.stone.place"),
            Map.entry(Material.GRANITE_WALL, "block.stone.place"),
            Map.entry(Material.NETHER_BRICK_WALL, "block.nether_brick.place"),
            Map.entry(Material.RED_NETHER_BRICK_WALL, "block.nether_brick.place"),
            Map.entry(Material.BLACKSTONE_WALL, "block.stone.place"),
            Map.entry(Material.POLISHED_BLACKSTONE_WALL, "block.stone.place"),
            Map.entry(Material.POLISHED_BLACKSTONE_BRICK_WALL, "block.stone.place"),
            Map.entry(Material.COBBLED_DEEPSLATE_WALL, "block.deepslate.place"),
            Map.entry(Material.POLISHED_DEEPSLATE_WALL, "block.deepslate.place"),
            Map.entry(Material.DEEPSLATE_BRICK_WALL, "block.deepslate.place"),
            Map.entry(Material.DEEPSLATE_TILE_WALL, "block.deepslate.place"),
            Map.entry(Material.TUFF_WALL, "block.tuff.place"),
            Map.entry(Material.TUFF_BRICK_WALL, "block.tuff_bricks.place"),
            Map.entry(Material.POLISHED_TUFF_WALL, "block.polished_tuff.place"),
            Map.entry(Material.MUD_BRICK_WALL, "block.mud_bricks.place"),
            Map.entry(Material.RESIN_BRICK_WALL, "block.resin_bricks.place")
    );

    private static final List<BlockFace> WALL_FACES = List.of(
            BlockFace.NORTH, BlockFace.EAST, BlockFace.SOUTH, BlockFace.WEST
    );

    private static final List<Gate> gates = new ArrayList<>();

    // String key "world:x:y:z" -> gate UUID
    private static final HashMap<String, UUID> blockToGateId = new HashMap<>();
    private static final Set<UUID> gatePlacementModeSet = new HashSet<>();
    private static final Set<UUID> modifyingGateSet = new HashSet<>();
    private static final Map<UUID, Integer> modifyTimeoutTaskIds = new HashMap<>();

    // -------------------------------------------------------------------------
    // Public API
    // -------------------------------------------------------------------------

    public static boolean isGateMaterial(Material type) {
        return WOODEN_GATE_MATERIALS.contains(type)
                || METAL_GATE_MATERIALS.contains(type)
                || NETHER_BRICK_FENCE_MATERIALS.contains(type)
                || WALL_MATERIALS.contains(type);
    }

    public static boolean isMetalGate(Material type) {
        return METAL_GATE_MATERIALS.contains(type);
    }

    public static Gate.GateType determineGateType(Material type) {
        if (METAL_GATE_MATERIALS.contains(type)) {
            return Gate.GateType.METAL;
        }
        if (NETHER_BRICK_FENCE_MATERIALS.contains(type)) {
            return Gate.GateType.NETHER_BRICK;
        }
        if (WALL_MATERIALS.contains(type)) {
            return Gate.GateType.WALL;
        }
        return Gate.GateType.WOODEN;
    }

    public static List<Gate> getGates() {
        return gates;
    }

    /**
     * Returns the Gate whose block set includes this location, or null.
     */
    public static Gate getGateAt(Location loc) {
        UUID gateId = blockToGateId.get(key(loc));
        if (gateId == null) {
            return null;
        }
        for (Gate gate : gates) {
            if (gate.getId().equals(gateId)) {
                return gate;
            }
        }
        return null;
    }

    public static boolean isInGatePlacementMode(UUID playerId) {
        return gatePlacementModeSet.contains(playerId);
    }

    /**
     * Toggles gate placement mode for the given player.
     */
    public static boolean toggleGatePlacementMode(UUID playerId) {
        if (gatePlacementModeSet.contains(playerId)) {
            gatePlacementModeSet.remove(playerId);
            return false;
        } else {
            gatePlacementModeSet.add(playerId);
            return true;
        }
    }

    public static void exitGatePlacementMode(UUID playerId) {
        gatePlacementModeSet.remove(playerId);
    }

    /**
     * Determines whether the block should be tracked as part of a gate:
     * - Placement mode and NOT adjacent to any gate will start a new gate.
     * - Placement mode and adjacent to existing gate will exit gate mode, places normally.
     * - Sneaking and adjacent to existing gate of same type will connect to that gate.
     * - Otherwise it is an ordinary fence/bar, no gate tracking is needed.
     *
     * @return The error message
     */
    public static String handleBlockPlaced(Block block, Player player) {
        Location loc = normalized(block.getLocation());
        UUID playerId = player.getUniqueId();
        boolean inPlacementMode = isInGatePlacementMode(playerId);
        Gate adjacentGate = findAdjacentGateWithRoom(loc, block.getType());

        if (adjacentGate != null) {
            // Always exit placement mode when adjacent to an existing gate.
            if (inPlacementMode) {
                exitGatePlacementMode(playerId);
            }

            if (player.isSneaking()) {
                String axisError = getAxisError(loc, adjacentGate);
                if (axisError != null) {
                    return axisError;
                }

                // Determine or update axis now that a new block is being added.
                if (adjacentGate.getAxis() == null) {
                    Gate.Axis newAxis = determineAxisFromGate(adjacentGate, loc);
                    if (newAxis != null) {
                        adjacentGate.setAxis(newAxis);
                    }
                }

                adjacentGate.getBlockMaterials().put(loc, block.getType());
                blockToGateId.put(key(loc), adjacentGate.getId());
                notifyGateModification(player);
            }
        } else {
            if (inPlacementMode) {
                Gate gate = new Gate(UUID.randomUUID(), playerId, determineGateType(block.getType()), loc, block.getType());
                gates.add(gate);
                blockToGateId.put(key(loc), gate.getId());
                exitGatePlacementMode(playerId);
                player.sendMessage(ChatUtils.chatMessage("&7A new gate has been created"));
            }
        }

        return null;
    }

    /**
     * Removes a block from gate tracking when it is destroyed.
     * If the gate was open, all non-removed blocks are restored before removal.
     * Splits the gate into disconnected components if needed.
     */
    public static void handleBlockRemoved(Location loc) {
        Gate gate = getGateAt(loc);
        if (gate == null) {
            return;
        }

        if (gate.isOpen()) {
            Location removedNormalized = normalized(loc);
            for (Location blockLoc : gate.getBlocks()) {
                if (!sameBlock(blockLoc, removedNormalized)) {
                    // Resets block data and reconnects faces (close the gate)
                    blockLoc.getBlock().setType(gate.getMaterialAt(blockLoc), true);
                }
            }
            gate.setOpen(false);
        }

        Location normalizedLoc = normalized(loc);
        gate.getBlocks().remove(normalizedLoc);
        blockToGateId.remove(key(normalizedLoc));

        if (gate.getBlocks().isEmpty()) {
            gates.remove(gate);
            return;
        }

        if (gate.getBlocks().size() == 1) {
            gate.setAxis(null);
        }

        splitGateIfDisconnected(gate);
    }

    /**
     * Toggles a gate between open and closed states.
     *
     * @return An error message if the gate cannot be toggled, or null on success.
     */
    public static String toggleGate(Gate gate) {
        Gate.Axis axis = gate.getAxis();
        if (axis == null) {
            return "You must add blocks along an X or Z axis first";
        }
        int minVal = getMinAxisVal(gate);
        int maxVal = getMaxAxisVal(gate);
        if (maxVal - minVal < 2) {
            return "This gate needs to be at least 3 blocks wide to open";
        }
        if (gate.isOpen()) {
            closeGate(gate);
        } else {
            openGate(gate);
        }
        return null;
    }

    /**
     * Returns all endpoint blocks of the gate.
     */
    public static Set<Location> getEndpoints(Gate gate) {
        Gate.Axis axis = gate.getAxis();
        if (axis == null) {
            return new HashSet<>(gate.getBlocks());
        }

        int minVal = getMinAxisVal(gate);
        int maxVal = getMaxAxisVal(gate);

        Set<Location> endpoints = new HashSet<>();
        for (Location loc : gate.getBlocks()) {
            int val = (axis == Gate.Axis.X) ? loc.getBlockX() : loc.getBlockZ();
            if (val == minVal || val == maxVal) {
                endpoints.add(loc);
            }
        }
        return endpoints;
    }

    /**
     * Registers a fully built Gate object loaded from persistence.
     */
    public static void addGate(Gate gate) {
        gates.add(gate);
        for (Location loc : gate.getBlocks()) {
            blockToGateId.put(key(loc), gate.getId());
        }
    }

    /**
     * Clears all gate data; used when reloading from persistence.
     */
    public static void clearGates() {
        gates.clear();
        blockToGateId.clear();
    }

    /**
     * Handles the functionality to open a gate.
     */
    private static void openGate(Gate gate) {
        gate.setOpen(true);
        Gate.Axis axis = gate.getAxis();
        int minVal = getMinAxisVal(gate);
        int maxVal = getMaxAxisVal(gate);

        for (Location blockLoc : gate.getBlocks()) {
            int val = (axis == Gate.Axis.X) ? blockLoc.getBlockX() : blockLoc.getBlockZ();
            if (val == minVal) {
                setEndpointInwardFace(blockLoc, axis, true, false);
            } else if (val == maxVal) {
                setEndpointInwardFace(blockLoc, axis, false, false);
            } else {
                blockLoc.getBlock().setType(Material.AIR, false);
            }
        }

        playGateSounds(gate, sortedByAxis(gate), true);
    }

    /**
     * Handles the functionality to close a gate.
     */
    private static void closeGate(Gate gate) {
        gate.setOpen(false);
        Gate.Axis axis = gate.getAxis();
        int minVal = getMinAxisVal(gate);
        int maxVal = getMaxAxisVal(gate);

        // Restore interior blocks with explicitly determined face connections
        for (Location blockLoc : gate.getBlocks()) {
            int val = (axis == Gate.Axis.X) ? blockLoc.getBlockX() : blockLoc.getBlockZ();
            if (val != minVal && val != maxVal) {
                restoreInteriorBlock(blockLoc, gate.getMaterialAt(blockLoc));
            }
        }

        // Explicitly reconnect endpoint inward faces
        for (Location blockLoc : gate.getBlocks()) {
            int val = (axis == Gate.Axis.X) ? blockLoc.getBlockX() : blockLoc.getBlockZ();
            if (val == minVal) {
                setEndpointInwardFace(blockLoc, axis, true, true);
            } else if (val == maxVal) {
                setEndpointInwardFace(blockLoc, axis, false, true);
            }
        }

        playGateSounds(gate, sortedByAxis(gate), false);
    }

    /**
     * Restores the temporarily hidden gate blocks.
     */
    private static void restoreInteriorBlock(Location loc, Material type) {
        Block block = loc.getBlock();
        block.setType(type, false); // Place with default data first

        BlockData bd = block.getBlockData();
        if (bd instanceof Fence fence) {
            for (BlockFace face : fence.getAllowedFaces()) {
                fence.setFace(face, fenceConnectsTo(block.getRelative(face)));
            }
            block.setBlockData(fence, true);
        } else if (bd instanceof Wall wall) {
            for (BlockFace face : WALL_FACES) {
                wall.setHeight(face, fenceConnectsTo(block.getRelative(face)) ? Wall.Height.LOW : Wall.Height.NONE);
            }
            block.setBlockData(wall, true);
        } else {
            // Non-Fence/Wall material will fall back to physics-based restore
            block.setType(type, true);
        }
    }

    /**
     * Returns true if a fence or bar block should connect its face toward the input block.
     */
    private static boolean fenceConnectsTo(Block neighbor) {
        Material mat = neighbor.getType();
        if (mat == Material.AIR || mat == Material.CAVE_AIR || mat == Material.VOID_AIR) {
            return false;
        }
        if (isGateMaterial(mat)) {
            return true;
        }
        // Fences connect to solid, fully-opaque blocks (e.g. stone, planks).
        return mat.isSolid() && mat.isOccluding();
    }

    /**
     * Plays staggered sounds sweeping along the gate axis.
     */
    private static void playGateSounds(Gate gate, List<Location> sortedBlocks, boolean opening) {
        // Sounds that have a reliable Bukkit Sound enum entry use it directly to avoid
        // Named Sound Effect packet issues with vanilla sound lookup in Paper 1.21+.
        Sound enumSound = null;
        String stringSound = null;
        float baseVolume;
        float basePitch;
        float pitchIncrement;
        float pitchRandom;

        switch (gate.getGateType()) {
            case METAL -> {
                stringSound = opening ? "block.copper_chest.open" : "block.copper_chest.close";
                baseVolume = 0.6f;
                basePitch = 0.70f;
                pitchIncrement = 0.025f;
                pitchRandom = 0.05f;
            }
            case NETHER_BRICK -> {
                enumSound = Sound.BLOCK_NETHER_BRICKS_PLACE;
                baseVolume = 0.7f;
                basePitch = 0.85f;
                pitchIncrement = 0.020f;
                pitchRandom = 0.10f;
            }
            case WALL -> {
                Material anchorMat = gate.getBlockMaterials().values().iterator().next();
                stringSound = MATERIAL_SOUNDS.getOrDefault(anchorMat, "block.stone.place");
                baseVolume = 0.7f;
                basePitch = 0.85f;
                pitchIncrement = 0.020f;
                pitchRandom = 0.10f;
            }
            default -> { // WOODEN
                stringSound = opening ? "block.fence_gate.open" : "block.fence_gate.close";
                baseVolume = 0.7f;
                basePitch = 0.85f;
                pitchIncrement = 0.020f;
                pitchRandom = 0.10f;
            }
        }

        World world = sortedBlocks.get(0).getWorld();
        Random random = new Random();
        final Sound finalEnumSound = enumSound;
        final String finalStringSound = stringSound;

        // 1 tick per block sound
        for (int i = 0; i < sortedBlocks.size(); i++) {
            final Location blockLoc = sortedBlocks.get(i);
            final float pitch = basePitch + (i * pitchIncrement) + random.nextFloat() * pitchRandom;
            final float finalVolume = baseVolume;

            Runnable playSoundAction = finalEnumSound != null
                    ? () -> world.playSound(blockLoc, finalEnumSound, SoundCategory.BLOCKS, finalVolume, pitch)
                    : () -> world.playSound(blockLoc, finalStringSound, finalVolume, pitch);

            if (i == 0) {
                playSoundAction.run();
            } else {
                Bukkit.getScheduler().scheduleSyncDelayedTask(
                        AranarthCore.getInstance(),
                        playSoundAction,
                        i
                );
            }
        }
    }

    /**
     * Returns the minimum X or Z coordinate (depending on axis) across all gate blocks.
     */
    private static int getMinAxisVal(Gate gate) {
        Gate.Axis axis = gate.getAxis();
        int min = Integer.MAX_VALUE;
        for (Location loc : gate.getBlocks()) {
            int val = (axis == Gate.Axis.X) ? loc.getBlockX() : loc.getBlockZ();
            if (val < min) {
                min = val;
            }
        }
        return min;
    }

    /**
     * Returns the maximum X or Z coordinate (depending on axis) across all gate blocks.
     */
    private static int getMaxAxisVal(Gate gate) {
        Gate.Axis axis = gate.getAxis();
        int max = Integer.MIN_VALUE;
        for (Location loc : gate.getBlocks()) {
            int val = (axis == Gate.Axis.X) ? loc.getBlockX() : loc.getBlockZ();
            if (val > max) {
                max = val;
            }
        }
        return max;
    }

    /**
     * Sets or clears the inward-facing BlockData connection on an endpoint block.
     *
     * @param endpointLoc The endpoint block's location.
     * @param axis        The gate's axis.
     * @param isMin       True if this is the min-coordinate endpoint column, false if max.
     * @param connected   True to connect the inward face, false to clear it.
     */
    private static void setEndpointInwardFace(Location endpointLoc, Gate.Axis axis, boolean isMin, boolean connected) {
        Block block = endpointLoc.getBlock();
        BlockData bd = block.getBlockData();
        BlockFace inwardFace = getInwardFace(axis, isMin);

        if (bd instanceof Fence fence) {
            if (fence.getAllowedFaces().contains(inwardFace)) {
                // When closing (connected=true), only connect if the inward neighbor actually qualifies
                // Prevents phantom connections into empty space
                // When opening (connected=false), always disconnect
                fence.setFace(inwardFace, connected && fenceConnectsTo(block.getRelative(inwardFace)));
                block.setBlockData(fence, false);
            }
        } else if (bd instanceof Wall wall) {
            Wall.Height height = (connected && fenceConnectsTo(block.getRelative(inwardFace)))
                    ? Wall.Height.LOW : Wall.Height.NONE;
            wall.setHeight(inwardFace, height);
            block.setBlockData(wall, false);
        }
    }

    /**
     * Returns the face of an endpoint block pointing toward the gate interior.
     */
    private static BlockFace getInwardFace(Gate.Axis axis, boolean isMin) {
        if (axis == Gate.Axis.X) {
            return isMin ? BlockFace.EAST : BlockFace.WEST;
        } else {
            return isMin ? BlockFace.SOUTH : BlockFace.NORTH;
        }
    }

    /**
     * Searches all 6 face-adjacent blocks (including above/below for multi-row gates)
     * for one that belongs to a gate with room to grow.
     */
    private static Gate findAdjacentGateWithRoom(Location loc, Material type) {
        for (Location neighbor : allNeighbors(loc)) {
            if (isGateMaterial(neighbor.getBlock().getType())) {
                Gate neighborGate = getGateAt(neighbor);
                if (neighborGate != null && neighborGate.getBlocks().size() < MAX_GATE_SIZE) {
                    return neighborGate;
                }
            }
        }
        return null;
    }

    /**
     * Validates whether placing a block at the input location is compatible with the existing gate's axis constraint.
     *
     * @return An error message if invalid, or null if valid.
     */
    private static String getAxisError(Location loc, Gate adjacentGate) {
        Gate.Axis axis = adjacentGate.getAxis();

        if (axis == null) {
            boolean allSameX = true, allSameZ = true;
            for (Location existing : adjacentGate.getBlocks()) {
                if (existing.getBlockX() != loc.getBlockX()) {
                    allSameX = false;
                }
                if (existing.getBlockZ() != loc.getBlockZ()) {
                    allSameZ = false;
                }
            }

            if (!allSameX && !allSameZ) {
                return "Gates must be connected along the same X or Z axis!";
            }
            return null;
        }

        // Axis already fixed so only check the perpendicular horizontal coordinate
        Location any = adjacentGate.getBlocks().iterator().next();
        if (axis == Gate.Axis.X) {
            if (loc.getBlockZ() != any.getBlockZ()) {
                return "Connected gate blocks must share the same Z coordinate!";
            }
        } else {
            if (loc.getBlockX() != any.getBlockX()) {
                return "Connected gate blocks must share the same X coordinate!";
            }
        }
        return null;
    }

    /**
     * Determines what horizontal axis the gate should have given its existing blocks plus a new block at the input location.
     */
    private static Gate.Axis determineAxisFromGate(Gate gate, Location newLoc) {
        boolean allSameX = true, allSameZ = true;
        for (Location existing : gate.getBlocks()) {
            if (existing.getBlockX() != newLoc.getBlockX()) {
                allSameX = false;
            }
            if (existing.getBlockZ() != newLoc.getBlockZ()) {
                allSameZ = false;
            }
        }
        if (allSameX && allSameZ) {
            return null; // Pure vertical, no axis yet
        }
        if (allSameX) {
            return Gate.Axis.Z;
        }
        if (allSameZ) {
            return Gate.Axis.X;
        }
        return null;
    }

    /**
     * Sends a "gate modification started" message on the first sneak-add for this player.
     */
    private static void notifyGateModification(Player player) {
        UUID id = player.getUniqueId();

        if (!modifyingGateSet.contains(id)) {
            modifyingGateSet.add(id);
            player.sendMessage(ChatUtils.chatMessage("&7You are now modifying a gate"));
        }

        Integer existing = modifyTimeoutTaskIds.remove(id);
        if (existing != null) {
            Bukkit.getScheduler().cancelTask(existing);
        }

        int taskId = Bukkit.getScheduler().scheduleSyncDelayedTask(
                AranarthCore.getInstance(),
                () -> {
                    modifyingGateSet.remove(id);
                    modifyTimeoutTaskIds.remove(id);
                    Player online = Bukkit.getPlayer(id);
                    if (online != null) {
                        online.sendMessage(ChatUtils.chatMessage("&7The gate modification has ended"));
                    }
                },
                60L
        );
        modifyTimeoutTaskIds.put(id, taskId);
    }

    /**
     * After a block is removed, checks whether the remaining blocks are still fully connected.
     * Disconnected components each become their own Gate, inheriting the original gate's axis.
     */
    private static void splitGateIfDisconnected(Gate gate) {
        if (gate.getBlocks().size() <= 1) {
            return;
        }

        Set<Location> allBlocks = new HashSet<>(gate.getBlocks());
        List<Set<Location>> components = new ArrayList<>();
        Set<String> visited = new HashSet<>();

        for (Location start : allBlocks) {
            if (visited.contains(key(start))) {
                continue;
            }

            Set<Location> component = new HashSet<>();
            Queue<Location> queue = new LinkedList<>();
            queue.add(start);

            while (!queue.isEmpty()) {
                Location current = queue.poll();
                if (visited.contains(key(current))) {
                    continue;
                }
                visited.add(key(current));
                component.add(current);

                for (Location neighbor : allNeighbors(current)) {
                    if (!visited.contains(key(neighbor)) && allBlocks.contains(neighbor)) {
                        queue.add(neighbor);
                    }
                }
            }
            components.add(component);
        }

        if (components.size() <= 1) {
            return;
        }

        Map<Location, Material> origMaterials = gate.getBlockMaterials();

        Set<Location> first = components.get(0);
        Map<Location, Material> firstMaterials = new HashMap<>();
        for (Location l : first) firstMaterials.put(l, origMaterials.get(l));
        gate.setBlockMaterials(firstMaterials);
        if (first.size() == 1) {
            gate.setAxis(null);
        }

        for (int i = 1; i < components.size(); i++) {
            Set<Location> component = components.get(i);
            Map<Location, Material> compMaterials = new HashMap<>();
            for (Location l : component) compMaterials.put(l, origMaterials.get(l));
            Location newAnchor = component.iterator().next();
            Gate newGate = new Gate(UUID.randomUUID(), gate.getOwner(), gate.getGateType(), newAnchor, compMaterials.get(newAnchor));
            newGate.setBlockMaterials(compMaterials);
            newGate.setAxis(component.size() > 1 ? gate.getAxis() : null);
            gates.add(newGate);
            for (Location blockLoc : component) {
                blockToGateId.put(key(blockLoc), newGate.getId());
            }
        }
    }


    /**
     * Returns all 6 face-adjacent block locations (including above and below).
     */
    private static List<Location> allNeighbors(Location loc) {
        int x = loc.getBlockX(), y = loc.getBlockY(), z = loc.getBlockZ();
        World world = loc.getWorld();
        return Arrays.asList(
                new Location(world, x + 1, y, z),
                new Location(world, x - 1, y, z),
                new Location(world, x, y + 1, z),
                new Location(world, x, y - 1, z),
                new Location(world, x, y, z + 1),
                new Location(world, x, y, z - 1)
        );
    }

    /**
     * Creates a block-aligned Location (no yaw/pitch) for consistent map keys.
     */
    public static Location normalized(Location loc) {
        return new Location(loc.getWorld(), loc.getBlockX(), loc.getBlockY(), loc.getBlockZ());
    }

    /**
     * String key used for the blockToGateId map.
     */
    public static String key(Location loc) {
        return loc.getWorld().getName() + ":" + loc.getBlockX() + ":" + loc.getBlockY() + ":" + loc.getBlockZ();
    }

    /**
     * Compares two locations by block coordinates and world, ignoring yaw/pitch.
     */
    public static boolean sameBlock(Location a, Location b) {
        return a.getBlockX() == b.getBlockX()
                && a.getBlockY() == b.getBlockY()
                && a.getBlockZ() == b.getBlockZ()
                && a.getWorld().equals(b.getWorld());
    }

    /**
     * Returns gate blocks sorted ascending along the gate's axis, for a consistent sound sweep.\
     */
    private static List<Location> sortedByAxis(Gate gate) {
        List<Location> list = new ArrayList<>(gate.getBlocks());
        if (gate.getAxis() == Gate.Axis.X) {
            list.sort(Comparator.comparingInt(Location::getBlockX));
        } else if (gate.getAxis() == Gate.Axis.Z) {
            list.sort(Comparator.comparingInt(Location::getBlockZ));
        } else {
            list.sort(Comparator.comparingInt(Location::getBlockY));
        }
        return list;
    }
}
