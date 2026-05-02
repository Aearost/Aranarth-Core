package com.aearost.aranarthcore.abilities.waterbending;

import com.aearost.aranarthcore.utils.ChatUtils;
import com.projectkorra.projectkorra.ability.AddonAbility;
import com.projectkorra.projectkorra.ability.PlantAbility;
import com.projectkorra.projectkorra.attribute.Attribute;
import com.projectkorra.projectkorra.util.BlockSource;
import com.projectkorra.projectkorra.util.ClickType;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.type.Leaves;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

public class RazorLeaves extends PlantAbility implements AddonAbility {

    public enum Phase { SOURCED, CASTING }

    @Attribute(Attribute.COOLDOWN)
    private long cooldown;
    @Attribute(Attribute.SELECT_RANGE)
    private int selectRange;
    @Attribute(Attribute.RANGE)
    private int range;
    @Attribute(Attribute.DAMAGE)
    private double damage;
    @Attribute("SourceLeashRange")
    private double sourceLeashRange;

    // Source block tracking
    private Block sourceBlock;
    private BlockData sourceOriginalData;

    private Phase phase;
    private int leavesRemaining;
    private final List<LeafProjectile> projectiles = new ArrayList<>();

    // Static instance registry (one active RazorLeaves per player)
    private static final Map<UUID, RazorLeaves> activeInstances = new HashMap<>();

    private static final double SPEED = 2.5;     // blocks per tick
    private static final double STEP = 0.2;      // sub-step size in blocks
    private static final double HIT_RADIUS = 0.8;

    // Deep forest green
    private static final Particle.DustOptions LEAF_DUST =
            new Particle.DustOptions(Color.fromRGB(0, 90, 10), 0.6f);

    private static class LeafProjectile {
        Location position;
        final Vector direction;
        double distanceTraveled;

        LeafProjectile(Location start, Vector direction) {
            this.position = start.clone();
            this.direction = direction.clone().normalize();
            this.distanceTraveled = 0.0;
        }
    }

    public RazorLeaves(Player player) {
        super(player);

        if (!bPlayer.canBend(this)) {
            return;
        }

        cooldown = 8000;
        selectRange = 6;
        range = 20;
        damage = 2.0;
        sourceLeashRange = 10.0;
        leavesRemaining = 5;
        phase = Phase.SOURCED;

        Block found = BlockSource.getWaterSourceBlock(player, selectRange, ClickType.SHIFT_DOWN, false, false, bPlayer.canPlantbend());
        if (found == null || !isValidPlantSource(found)) {
            return;
        }

        sourceBlock = found;
        activateSourceBlock();
        activeInstances.put(player.getUniqueId(), this);
        start();
    }

    // -------------------------------------------------------------------------
    // PK tick
    // -------------------------------------------------------------------------

    @Override
    public void progress() {
        // Check the player is within leash range of the source
        if (sourceBlock != null) {
            Location sourceCenter = sourceBlock.getLocation().add(0.5, 0.5, 0.5);
            if (player.getLocation().distanceSquared(sourceCenter) > sourceLeashRange * sourceLeashRange) {
                if (phase == Phase.SOURCED) {
                    cancelInstantly();
                } else {
                    finishAbility();
                }
                return;
            }
        }

        // Progress all in-flight projectiles
        projectiles.removeIf(this::progressProjectile);

        // All leaves fired and all projectiles resolved — end the ability
        if (leavesRemaining <= 0 && projectiles.isEmpty() && phase == Phase.CASTING) {
            finishAbility();
        }
    }

    // -------------------------------------------------------------------------
    // Sneak / Re-source
    // -------------------------------------------------------------------------

    /**
     * Called by the listener when the player taps sneak while RazorLeaves is active.
     * In SOURCED phase: tries to swap to a new source block.
     * In CASTING phase: ignored (ability is already mid-use).
     */
    public void onSneak() {
        if (phase == Phase.CASTING) {
            return;
        }

        Block found = BlockSource.getWaterSourceBlock(player, selectRange, ClickType.SHIFT_DOWN, false, false, bPlayer.canPlantbend());
        if (found == null || !isValidPlantSource(found) || found.equals(sourceBlock)) {
            return;
        }

        restoreSourceBlock();
        sourceBlock = found;
        activateSourceBlock();
    }

    // -------------------------------------------------------------------------
    // Left-click / Firing
    // -------------------------------------------------------------------------

    /**
     * Called by the listener when the player left-clicks while RazorLeaves is active.
     * Fires a leaf from the source block toward where the player is currently looking.
     */
    public void onLeftClick() {
        if (leavesRemaining <= 0) {
            return;
        }
        fireLeaf();
    }

    private void fireLeaf() {
        // Project a point along the player's look ray at max range, then calculate
        // the direction from the source block center toward that point.
        Location sourceCenter = sourceBlock.getLocation().add(0.5, 0.5, 0.5);
        Location aimPoint = player.getEyeLocation().add(player.getEyeLocation().getDirection().multiply(range));
        Vector direction = aimPoint.toVector().subtract(sourceCenter.toVector()).normalize();
        projectiles.add(new LeafProjectile(sourceCenter, direction));
        leavesRemaining--;
        phase = Phase.CASTING;
        Location sourceLocation = sourceBlock.getLocation();
        float pitch = 0.8F + ThreadLocalRandom.current().nextFloat() * (1.1F - 0.8F);
        sourceLocation.getWorld().playSound(sourceLocation, Sound.ENTITY_BREEZE_LAND, 2.0f, pitch);
    }

    // -------------------------------------------------------------------------
    // Projectile tick
    // -------------------------------------------------------------------------

    /**
     * Advances a leaf projectile by one tick's worth of movement.
     * Returns true when the projectile should be removed.
     */
    private boolean progressProjectile(LeafProjectile proj) {
        double targetDistance = proj.distanceTraveled + SPEED;
        Location sourceCenter = sourceBlock.getLocation().add(0.5, 0.5, 0.5);

        while (proj.distanceTraveled < targetDistance) {
            double step = Math.min(STEP, targetDistance - proj.distanceTraveled);
            proj.position.add(proj.direction.clone().multiply(step));
            proj.distanceTraveled += step;

            // Range check from source block
            if (!proj.position.getWorld().equals(sourceCenter.getWorld())
                    || proj.position.distanceSquared(sourceCenter) > (double) range * range) {
                return true;
            }

            // Solid block collision — skip the source block so the leaf can exit it
            Block currentBlock = proj.position.getBlock();
            if (!currentBlock.equals(sourceBlock) && currentBlock.getType().isSolid()) {
                return true;
            }

            // Entity hit
            LivingEntity hit = findNearbyLivingEntity(proj.position, HIT_RADIUS);
            if (hit != null) {
                hit.damage(damage, player);
                return true;
            }

            // Particle trail
            proj.position.getWorld().spawnParticle(
                    Particle.DUST,
                    proj.position.clone(),
                    1, 0.05, 0.05, 0.05, 0,
                    LEAF_DUST
            );
        }

        return false;
    }

    // -------------------------------------------------------------------------
    // Source block helpers
    // -------------------------------------------------------------------------

    private void activateSourceBlock() {
        sourceOriginalData = sourceBlock.getBlockData().clone();
        Leaves leafData = (Leaves) Material.OAK_LEAVES.createBlockData();
        leafData.setPersistent(true);
        sourceBlock.setBlockData(leafData);

        sourceBlock.getWorld().spawnParticle(
                Particle.DUST,
                sourceBlock.getLocation().add(0.5, 1.2, 0.5),
                6, 0.3, 0.3, 0.3, 0,
                LEAF_DUST
        );
    }

    private void restoreSourceBlock() {
        if (sourceBlock != null && sourceOriginalData != null) {
            sourceBlock.setBlockData(sourceOriginalData);
            sourceOriginalData = null;
            sourceBlock = null;
        }
    }

    // -------------------------------------------------------------------------
    // Cleanup / end
    // -------------------------------------------------------------------------

    /**
     * Ends the ability and applies the cooldown. Called when all leaves are consumed,
     * the player changes slots, or the player walked too far from the source while casting.
     */
    private void finishAbility() {
        restoreSourceBlock();
        activeInstances.remove(player.getUniqueId());
        bPlayer.addCooldown(this);
        remove();
    }

    /**
     * Called by the listener to end the ability with cooldown (e.g., slot change).
     */
    public void endWithCooldown() {
        finishAbility();
    }

    /**
     * Immediately ends the ability without applying a cooldown. Used when no source
     * was found or the player walked away before firing any leaves.
     */
    public void cancelInstantly() {
        restoreSourceBlock();
        activeInstances.remove(player.getUniqueId());
        remove();
    }

    // -------------------------------------------------------------------------
    // Utility
    // -------------------------------------------------------------------------

    private boolean isValidPlantSource(Block block) {
        Material mat = block.getType();
        return mat != Material.WATER
                && mat != Material.ICE
                && mat != Material.PACKED_ICE
                && mat != Material.BLUE_ICE
                && mat != Material.FROSTED_ICE
                && mat != Material.SNOW
                && mat != Material.SNOW_BLOCK;
    }

    private LivingEntity findNearbyLivingEntity(Location loc, double radius) {
        for (Entity entity : loc.getWorld().getNearbyEntities(loc, radius, radius, radius)) {
            if (entity instanceof LivingEntity target && !target.equals(player)) {
                return target;
            }
        }
        return null;
    }

    // -------------------------------------------------------------------------
    // Static registry
    // -------------------------------------------------------------------------

    public static boolean hasActiveInstance(UUID uuid) {
        return activeInstances.containsKey(uuid);
    }

    public static RazorLeaves getActiveInstance(UUID uuid) {
        return activeInstances.get(uuid);
    }

    public Phase getPhase() {
        return phase;
    }

    // -------------------------------------------------------------------------
    // PK ability interface
    // -------------------------------------------------------------------------

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
        return cooldown;
    }

    @Override
    public String getName() {
        return "RazorLeaves";
    }

    @Override
    public Location getLocation() {
        if (!projectiles.isEmpty()) return projectiles.get(0).position;
        if (sourceBlock != null) return sourceBlock.getLocation();
        return player.getLocation();
    }

    @Override
    public void load() {}

    @Override
    public void stop() {
        restoreSourceBlock();
        if (player != null) activeInstances.remove(player.getUniqueId());
    }

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
        return "Source from a nearby plant and launch a volley of razor-sharp leaves at your foes. " +
                "Each leaf travels in a straight line and strikes hard on impact. " +
                "Stay close to your source — stray too far and the leaves will be lost.\n" +
                ChatUtils.translateToColor("&fUsage: Tap Sneak (plant source) > Left-click (up to 5 times)");
    }
}
