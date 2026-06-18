package com.aearost.aranarthcore.abilities.earthbending.metalbending;

import com.aearost.aranarthcore.utils.ChatUtils;
import com.projectkorra.projectkorra.GeneralMethods;
import com.projectkorra.projectkorra.ability.AddonAbility;
import com.projectkorra.projectkorra.ability.ElementalAbility;
import com.projectkorra.projectkorra.ability.MetalAbility;
import com.projectkorra.projectkorra.attribute.Attribute;
import com.projectkorra.projectkorra.util.DamageHandler;
import com.projectkorra.projectkorra.util.TempBlock;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public class MetalShred extends MetalAbility implements AddonAbility {

    public enum Phase { SOURCING, SOURCED, FIRING, STAYING }

    private static final int MAX_BLOCKS = 50;
    private static final long STAY_DURATION_MS = 5000L;
    private static final long EXTEND_INTERVAL_MS = 20L;
    private static final double DAMAGE_RADIUS = 2.0;
    private static final BlockFace[] HORIZONTAL_FACES = {
        BlockFace.NORTH, BlockFace.SOUTH, BlockFace.EAST, BlockFace.WEST
    };
    private static final BlockFace[] DIAGONAL_FACES = {
        BlockFace.NORTH_EAST, BlockFace.NORTH_WEST, BlockFace.SOUTH_EAST, BlockFace.SOUTH_WEST
    };

    private static final Map<UUID, MetalShred> ACTIVE_INSTANCES = new HashMap<>();

    @Attribute(Attribute.COOLDOWN)
    private long cooldown;
    @Attribute(Attribute.DAMAGE)
    private double damage;
    @Attribute(Attribute.RANGE)
    private double range;

    private Phase phase;
    private boolean horizontal;
    private Block source;
    private Block lastBlock;
    private boolean sourcingStarted;
    private long lastExtendTime;
    private long stayStartTime;
    private int coilRemaining;
    private Block coilFront;
    private boolean stopCoil;
    private final List<TempBlock> tblocks = new ArrayList<>();
    private final Set<Vector> sourcedVectors = new HashSet<>();

    public MetalShred(final Player player) {
        super(player);

        if (hasAbility(player, MetalShred.class)) {
            getAbility(player, MetalShred.class).remove();
        }
        if (!bPlayer.canBendIgnoreBindsCooldowns(this)) {
            return;
        }
        if (bPlayer.isOnCooldown(this)) {
            return;
        }

        final Block adjacent = findAdjacentMetalBlock();
        if (adjacent == null) {
            return;
        }

        this.cooldown = 9000L;
        this.damage = 6.0;
        this.range = 1.0;
        this.source = adjacent;
        this.horizontal = isHorizontalWall(adjacent);
        this.phase = Phase.SOURCING;
        this.sourcingStarted = false;
        this.stopCoil = false;
        this.lastBlock = null;
        this.coilFront = null;

        sourcedVectors.add(blockToVector(adjacent));
        offsetBlock(adjacent, GeneralMethods.getDirection(player.getLocation(), adjacent.getLocation()));

        ACTIVE_INSTANCES.put(player.getUniqueId(), this);
        this.start();
    }

    @Override
    public void progress() {
        if (player.isDead() || !player.isOnline()) {
            remove();
            return;
        }
        switch (phase) {
            case SOURCING -> progressSourcing();
            case SOURCED -> { }
            case FIRING -> progressFiring();
            case STAYING -> progressStaying();
        }
    }

    private void progressSourcing() {
        if (!player.isSprinting()) {
            if (sourcingStarted) {
                phase = Phase.SOURCED;
            }
            return;
        }
        sourcingStarted = true;
        if (sourcedVectors.size() >= MAX_BLOCKS) {
            phase = Phase.SOURCED;
            return;
        }
        final Block next = findNextWallBlock();
        if (next == null) {
            return;
        }
        sourcedVectors.add(blockToVector(next));
        offsetBlock(next, GeneralMethods.getDirection(player.getLocation(), next.getLocation()));
        this.range = sourcedVectors.size();
        this.lastBlock = next;
    }

    private void progressFiring() {
        if (coilRemaining <= 0 || stopCoil) {
            phase = Phase.STAYING;
            stayStartTime = System.currentTimeMillis();
            return;
        }
        if (System.currentTimeMillis() - lastExtendTime < EXTEND_INTERVAL_MS) {
            return;
        }
        lastExtendTime = System.currentTimeMillis();

        final Block next = nextCoilBlock();
        if (next == null) {
            endWithCooldown();
            return;
        }
        extendCoil(next);

        for (final Entity entity : GeneralMethods.getEntitiesAroundPoint(next.getLocation(), DAMAGE_RADIUS)) {
            if (!(entity instanceof LivingEntity) || entity.getEntityId() == player.getEntityId()) {
                continue;
            }
            DamageHandler.damageEntity(entity, damage, this);
            GeneralMethods.setVelocity(this, entity, entity.getVelocity()
                    .add(player.getLocation().getDirection().setY(0.0).normalize().multiply(0.3).setY(0.1)));
        }

        coilFront = next;
        coilRemaining--;
    }

    private void progressStaying() {
        if (System.currentTimeMillis() - stayStartTime >= STAY_DURATION_MS) {
            endWithCooldown();
        }
    }

    private Block findAdjacentMetalBlock() {
        final Location feet = player.getLocation();
        for (int dist = 1; dist <= 4; dist++) {
            for (final BlockFace face : HORIZONTAL_FACES) {
                for (int dy = -1; dy <= 2; dy++) {
                    final Block b = feet.clone().add(0, dy, 0).getBlock().getRelative(face, dist);
                    if (isMetal(b)) return b;
                }
            }
        }
        return null;
    }

    private Block findNextWallBlock() {
        final Block origin = (lastBlock != null) ? lastBlock : source;

        // Direct 1-step search
        Block found = searchNeighbors(origin);
        if (found != null) return found;

        // 2-hop fallback
        for (final BlockFace linkFace : HORIZONTAL_FACES) {
            final Block intermediate = origin.getRelative(linkFace);
            if (!sourcedVectors.contains(blockToVector(intermediate))) continue;
            found = searchNeighbors(intermediate);
            if (found != null) return found;
        }
        for (final BlockFace linkFace : DIAGONAL_FACES) {
            final Block intermediate = origin.getRelative(linkFace);
            if (!sourcedVectors.contains(blockToVector(intermediate))) continue;
            found = searchNeighbors(intermediate);
            if (found != null) return found;
        }

        return null;
    }

    private Block searchNeighbors(final Block origin) {
        for (final BlockFace face : HORIZONTAL_FACES) {
            final Block b = origin.getRelative(face);
            if (isValidNextBlock(b)) return b;
        }
        for (final BlockFace face : DIAGONAL_FACES) {
            final Block b = origin.getRelative(face);
            if (isValidNextBlock(b)) return b;
        }
        return null;
    }

    private boolean isValidNextBlock(final Block b) {
        return isMetal(b) && !sourcedVectors.contains(blockToVector(b)) && isNearPlayer(b);
    }

    private boolean isNearPlayer(final Block b) {
        final double dx = b.getX() + 0.5 - player.getLocation().getX();
        final double dz = b.getZ() + 0.5 - player.getLocation().getZ();
        return (dx * dx + dz * dz) <= 30.25; // 5.5 blocks
    }

    /**
     * Returns the next block for the coil to extend into.
     */
    private Block nextCoilBlock() {
        final Block origin = coilFront != null ? coilFront : (lastBlock != null ? lastBlock : source);
        final Location target = GeneralMethods.getTargetedLocation(player, (int) range);
        return origin.getRelative(
                GeneralMethods.getCardinalDirection(GeneralMethods.getDirection(origin.getLocation(), target)));
    }

    /**
     * Determines whether the sourced block is part of a horizontal surface such as a floor.
     */
    private boolean isHorizontalWall(final Block b) {
        return ElementalAbility.isAir(b.getRelative(BlockFace.UP).getType())
                && !isMetal(b.getRelative(BlockFace.DOWN));
    }

    private void offsetBlock(final Block b, final Vector d) {
        if (horizontal) {
            raiseBlock(b, d);
        } else {
            shiftBlock(b, d);
        }
    }

    private void raiseBlock(final Block b, final Vector d) {
        final Block up = b.getRelative(BlockFace.UP);
        final Block away = b.getRelative(GeneralMethods.getCardinalDirection(d));
        final Block awayUp = away.getRelative(BlockFace.UP);
        final Block deeperB = b.getRelative(BlockFace.DOWN);
        final Block deeperA = away.getRelative(BlockFace.DOWN);

        clearOffsetBlocks();

        if (!up.getType().isSolid()) {
            tblocks.add(new TempBlock(up, b.getBlockData()));
        }
        if (!awayUp.getType().isSolid()) {
            tblocks.add(new TempBlock(awayUp, away.getBlockData()));
        }
        if (isMetal(b)) {
            tblocks.add(new TempBlock(b, Material.AIR.createBlockData()));
        }
        if (isMetal(away)) {
            tblocks.add(new TempBlock(away, Material.AIR.createBlockData()));
        }
        if (isMetal(deeperB)) {
            tblocks.add(new TempBlock(deeperB, Material.AIR.createBlockData()));
        }
        if (isMetal(deeperA)) {
            tblocks.add(new TempBlock(deeperA, Material.AIR.createBlockData()));
        }

        playMetalbendingSound(b.getLocation());
        b.getWorld().playSound(b.getLocation(), Sound.ENTITY_IRON_GOLEM_HURT, 1.0f, 1.0f);
    }

    private void shiftBlock(final Block b, final Vector d) {
        final Block under = b.getRelative(BlockFace.DOWN);
        final Block above = b.getRelative(BlockFace.UP);
        final BlockFace toward = GeneralMethods.getCardinalDirection(d).getOppositeFace();

        clearOffsetBlocks();

        // Visually shift b, and any adjacent metal above/below it, toward the player
        if (!b.getRelative(toward).getType().isSolid()) {
            tblocks.add(new TempBlock(b.getRelative(toward), b.getBlockData()));
        }
        if (isMetal(under) && !under.getRelative(toward).getType().isSolid()) {
            tblocks.add(new TempBlock(under.getRelative(toward), under.getBlockData()));
        }
        if (isMetal(above) && !above.getRelative(toward).getType().isSolid()) {
            tblocks.add(new TempBlock(above.getRelative(toward), above.getBlockData()));
        }
        if (isMetal(b)) {
            tblocks.add(new TempBlock(b, Material.AIR.createBlockData()));
        }
        if (isMetal(under)) {
            tblocks.add(new TempBlock(under, Material.AIR.createBlockData()));
        }
        if (isMetal(above)) {
            tblocks.add(new TempBlock(above, Material.AIR.createBlockData()));
        }

        playMetalbendingSound(b.getLocation());
        b.getWorld().playSound(b.getLocation(), Sound.ENTITY_IRON_GOLEM_HURT, 1.0f, 1.0f);
    }

    // Clears the currently displayed offset visual so only the most recent segment is visible
    private void clearOffsetBlocks() {
        for (final TempBlock tb : tblocks) {
            if (!ElementalAbility.isAir(tb.getBlock().getType())) {
                tb.setType(Material.AIR);
            }
        }
    }

    private void extendCoil(final Block b) {
        final Block under = b.getRelative(BlockFace.DOWN);
        final Block above = b.getRelative(BlockFace.UP);
        if (!b.getType().isSolid()) {
            tblocks.add(new TempBlock(b, Material.IRON_BLOCK.createBlockData()));
        } else {
            stopCoil = true;
            return;
        }
        // The blocks above and below are optional
        if (!under.getType().isSolid()) {
            tblocks.add(new TempBlock(under, Material.IRON_BLOCK.createBlockData()));
        }
        if (!above.getType().isSolid()) {
            tblocks.add(new TempBlock(above, Material.IRON_BLOCK.createBlockData()));
        }
        playMetalbendingSound(b.getLocation());
    }

    public void onLeftClick() {
        if (phase != Phase.SOURCED) {
            return;
        }
        this.phase = Phase.FIRING;
        this.lastExtendTime = 0L;
        this.coilRemaining = sourcedVectors.size();

        final Block anchor = (lastBlock != null) ? lastBlock : source;
        this.coilFront = anchor.getRelative(
                GeneralMethods.getCardinalDirection(
                        GeneralMethods.getDirection(player.getLocation(), anchor.getLocation())
                ).getOppositeFace());

        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_IRON_GOLEM_ATTACK, 1.0f, 0.8f);
        player.getWorld().playSound(player.getLocation(), Sound.BLOCK_METAL_HIT, 0.9f, 0.6f);
    }

    public void cancelInstantly() {
        remove();
    }

    public void endWithCooldown() {
        bPlayer.addCooldown(this);
        remove();
    }

    private void revertAll() {
        for (final TempBlock tb : tblocks) {
            tb.revertBlock();
        }
        tblocks.clear();
    }

    private static Vector blockToVector(final Block b) {
        return new Vector(b.getX(), b.getY(), b.getZ());
    }

    @Override
    public void remove() {
        revertAll();
        ACTIVE_INSTANCES.remove(player.getUniqueId());
        super.remove();
    }

    @Override
    public void stop() {
        ACTIVE_INSTANCES.clear();
    }

    public static boolean hasActiveInstance(final UUID uuid) {
        return ACTIVE_INSTANCES.containsKey(uuid);
    }

    public static MetalShred getActiveInstance(final UUID uuid) {
        return ACTIVE_INSTANCES.get(uuid);
    }

    public Phase getPhase() {
        return phase;
    }

    @Override
    public boolean isSneakAbility() {
        return true;
    }

    @Override
    public boolean isHarmlessAbility() {
        return false;
    }

    @Override
    public long getCooldown() {
        return this.cooldown;
    }

    @Override
    public Location getLocation() {
        return lastBlock != null ? lastBlock.getLocation() : player.getLocation();
    }

    @Override
    public String getName() {
        return "MetalShred";
    }

    @Override
    public void load() {}

    @Override
    public String getAuthor() {
        return "Aearost";
    }

    @Override
    public String getVersion() {
        return "1.0";
    }

    @Override
    public String getDescription() {
        return "Run alongside a metal wall to shred its surface into a razor coil, "
                + "then unleash it at your enemies. Supports both straight and curved metallic walls alike.\n"
                + ChatUtils.translateToColor("&fUsage: Tap Sneak (starting point) > Sprint alongside wall (source of coil) > Left-click (drag to control)");
    }
}
