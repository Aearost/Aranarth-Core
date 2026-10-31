package com.aearost.aranarthcore.abilities.waterbending.plantbending;

import com.aearost.aranarthcore.utils.AbilityConfig;
import com.aearost.aranarthcore.utils.AranarthBendingUtils;
import com.aearost.aranarthcore.utils.ChatUtils;
import com.aearost.aranarthcore.utils.DominionUtils;
import com.projectkorra.projectkorra.GeneralMethods;
import com.projectkorra.projectkorra.ability.AddonAbility;
import com.projectkorra.projectkorra.ability.PlantAbility;
import com.projectkorra.projectkorra.attribute.Attribute;
import com.projectkorra.projectkorra.util.DamageHandler;
import com.projectkorra.projectkorra.util.TempBlock;
import org.bukkit.Color;
import org.bukkit.FluidCollisionMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.MultipleFacing;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.RayTraceResult;
import org.bukkit.util.Vector;

import java.util.*;

public class SapStrike extends PlantAbility implements AddonAbility {

    public enum Phase {SOURCING, SOURCED, READY, FIRED}

    @Attribute(Attribute.COOLDOWN)
    private long cooldown;
    @Attribute(Attribute.DAMAGE)
    private double damage;
    @Attribute(Attribute.RANGE)
    private double range;
    @Attribute("SourceRange")
    private double sourceRange;
    @Attribute("SlownessDuration")
    private int slownessDuration;

    private int slownessAmplifier;
    private int dartCount;
    private double speed;
    private double hitRadius;
    private double spreadAngle;
    private long sourceDuration;
    private long sourcedTimeout;
    private long readyDuration;
    private long fireClickGap;

    private Phase phase;
    private long sourceStartTime;
    private long sourcedStartTime;
    private long readyStartTime;
    private long lastClickTime;
    private Block sourceBlock;
    private TempBlock sapTempBlock;
    private final List<Dart> darts = new ArrayList<>();
    private final Set<UUID> hitEntities = new HashSet<>();

    private static final double STEP = 0.25;
    private static final double LINE_PARTICLE_SPACING = 0.3;
    private static final double HELD_RING_RADIUS = 0.18;
    private static final double HELD_DART_LENGTH = 0.6;

    private static final Particle.DustOptions SAP_DUST =
            new Particle.DustOptions(Color.fromRGB(122, 140, 128), 0.8f);
    private static final Particle.DustOptions DART_DUST =
            new Particle.DustOptions(Color.fromRGB(122, 140, 128), 0.6f);

    private static final Map<UUID, SapStrike> activeInstances = new HashMap<>();

    /**
     * A single in-flight dart of sap, travelling in a straight line.
     */
    private static class Dart {
        final Location location;
        final Vector direction;
        double distanceTraveled;

        Dart(Location origin, Vector direction) {
            this.location = origin.clone();
            this.direction = direction;
        }
    }

    public SapStrike(Player player) {
        super(player);

        if (!bPlayer.canBend(this)) {
            return;
        }

        cooldown = AbilityConfig.getLong("SapStrike.Cooldown");
        damage = AbilityConfig.getDouble("SapStrike.Damage");
        range = AbilityConfig.getDouble("SapStrike.Range");
        dartCount = AbilityConfig.getInt("SapStrike.Darts");
        speed = AbilityConfig.getDouble("SapStrike.Speed");
        hitRadius = AbilityConfig.getDouble("SapStrike.HitRadius");
        spreadAngle = AbilityConfig.getDouble("SapStrike.SpreadAngle");
        sourceRange = AbilityConfig.getDouble("SapStrike.SourceRange");
        sourceDuration = AbilityConfig.getLong("SapStrike.SourceDuration");
        sourcedTimeout = AbilityConfig.getLong("SapStrike.SourcedTimeout");
        readyDuration = AbilityConfig.getLong("SapStrike.ReadyDuration");
        fireClickGap = AbilityConfig.getLong("SapStrike.FireClickGap");
        slownessDuration = AbilityConfig.getInt("SapStrike.SlownessDurationTicks");
        slownessAmplifier = AbilityConfig.getInt("SapStrike.SlownessAmplifier");

        RayTraceResult hit = traceTarget();
        if (hit == null || hit.getHitBlock() == null || hit.getHitBlockFace() == null) {
            return;
        }

        Block log = hit.getHitBlock();
        if (!RootSnare.isUnstrippedLog(log.getType()) || !canHoldSap(log.getRelative(hit.getHitBlockFace()))
                || !AranarthBendingUtils.canBendBlock(player, log)
                || GeneralMethods.isRegionProtectedFromBuild(this, log.getLocation())) {
            return;
        }

        sourceBlock = log;
        phase = Phase.SOURCING;
        sourceStartTime = System.currentTimeMillis();
        activeInstances.put(player.getUniqueId(), this);
        start();
    }

    @Override
    public void progress() {
        if (player.isDead() || !player.isOnline() || !player.getWorld().equals(sourceBlock.getWorld())) {
            if (phase == Phase.READY) {
                bPlayer.addCooldown(this);
            }
            remove();
            return;
        }

        switch (phase) {
            case SOURCING -> progressSourcing();
            case SOURCED -> progressSourced();
            case READY -> progressReady();
            case FIRED -> progressFired();
        }
    }

    private void progressSourcing() {
        if (!player.isSneaking() || !bPlayer.canBendIgnoreCooldowns(this)) {
            remove();
            return;
        }

        // The player must keep looking at the log for the whole extraction
        RayTraceResult hit = traceTarget();
        if (hit == null || !sourceBlock.equals(hit.getHitBlock()) || hit.getHitBlockFace() == null) {
            remove();
            return;
        }

        BlockFace face = hit.getHitBlockFace();
        Location faceLocation = sourceBlock.getLocation().add(0.5, 0.5, 0.5).add(face.getDirection().multiply(0.55));
        double fraction = (double) (System.currentTimeMillis() - sourceStartTime) / sourceDuration;
        int particles = 1 + (int) (Math.min(1.0, fraction) * 3);
        sourceBlock.getWorld().spawnParticle(Particle.DUST, faceLocation, particles, 0.2, 0.2, 0.2, 0, SAP_DUST);

        if (fraction >= 1.0) {
            formSap(face);
        }
    }

    /**
     * Displays the extracted sap as glow lichen on the face of the log that the player was looking at.
     */
    private void formSap(BlockFace face) {
        Block sapBlock = sourceBlock.getRelative(face);
        if (!canHoldSap(sapBlock)) {
            remove();
            return;
        }

        MultipleFacing sapData = (MultipleFacing) Material.GLOW_LICHEN.createBlockData();
        sapData.setFace(face.getOppositeFace(), true);
        sapTempBlock = new TempBlock(sapBlock, sapData);

        Location sapLocation = sapBlock.getLocation().add(0.5, 0.5, 0.5);
        sapLocation.getWorld().spawnParticle(Particle.DUST, sapLocation, 12, 0.3, 0.3, 0.3, 0, SAP_DUST);
        sapLocation.getWorld().playSound(sapLocation, Sound.BLOCK_HONEY_BLOCK_PLACE, 1.0f, 0.8f);

        phase = Phase.SOURCED;
        sourcedStartTime = System.currentTimeMillis();
    }

    private void progressSourced() {
        Block sapBlock = sapTempBlock.getBlock();
        if (!bPlayer.canBendIgnoreCooldowns(this)
                || System.currentTimeMillis() - sourcedStartTime >= sourcedTimeout
                || !RootSnare.isUnstrippedLog(sourceBlock.getType())
                || sapBlock.getType() != Material.GLOW_LICHEN || !TempBlock.isTempBlock(sapBlock)) {
            remove();
            return;
        }

        sapBlock.getWorld().spawnParticle(Particle.DUST, sapBlock.getLocation().add(0.5, 0.5, 0.5),
                1, 0.25, 0.25, 0.25, 0, SAP_DUST);
    }

    private void progressReady() {
        if (!bPlayer.canBendIgnoreCooldowns(this)
                || System.currentTimeMillis() - readyStartTime >= readyDuration) {
            loseDarts();
            return;
        }
        drawHeldDarts();
    }

    private void progressFired() {
        darts.removeIf(this::progressDart);
        if (darts.isEmpty()) {
            remove();
        }
    }

    /**
     * Advances a dart by one tick's worth of movement.
     *
     * @return Whether the dart should be removed.
     */
    private boolean progressDart(Dart dart) {
        World world = dart.location.getWorld();
        double remaining = speed;
        while (remaining > 0) {
            double step = Math.min(STEP, remaining);
            dart.location.add(dart.direction.clone().multiply(step));
            dart.distanceTraveled += step;
            remaining -= step;

            Block block = dart.location.getBlock();
            if (dart.distanceTraveled > range || (block.getType().isSolid() && !block.isPassable())) {
                world.spawnParticle(Particle.DUST, dart.location, 4, 0.1, 0.1, 0.1, 0, DART_DUST);
                return true;
            }

            world.spawnParticle(Particle.DUST, dart.location, 1, 0, 0, 0, 0, DART_DUST);

            for (Entity entity : GeneralMethods.getEntitiesAroundPoint(dart.location, hitRadius)) {
                if (!(entity instanceof LivingEntity living) || entity instanceof ArmorStand
                        || entity.getUniqueId().equals(player.getUniqueId())) {
                    continue;
                }
                if (living instanceof Player targetPlayer && !DominionUtils.canAttackPlayer(player, targetPlayer)) {
                    continue;
                }
                // The volley counts as one shot, so a target struck by several darts is only hit once
                if (hitEntities.add(living.getUniqueId())) {
                    DamageHandler.damageEntity(living, damage, this);
                    living.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, slownessDuration,
                            slownessAmplifier, false, true, true));
                    world.spawnParticle(Particle.DUST, dart.location, 8, 0.2, 0.2, 0.2, 0, DART_DUST);
                    world.playSound(dart.location, Sound.BLOCK_GLASS_BREAK, 0.8f, 1.6f);
                }
                return true;
            }
        }
        return false;
    }

    /**
     * Pulls the sap out of the log on the first left-click, and fires the darts on the second.
     */
    public void onLeftClick() {
        long now = System.currentTimeMillis();
        long timeSinceLastClick = now - lastClickTime;
        lastClickTime = now;

        if (phase == Phase.SOURCED) {
            extractSap();
        } else if (phase == Phase.READY && timeSinceLastClick >= fireClickGap) {
            fireDarts();
        }
    }

    /**
     * Draws the sap from the log to the player's hand, hardening it into darts.
     */
    private void extractSap() {
        Block sapBlock = sapTempBlock.getBlock();
        RayTraceResult hit = traceTarget();
        if (hit == null || (!sapBlock.equals(hit.getHitBlock()) && !sourceBlock.equals(hit.getHitBlock()))) {
            return;
        }

        Location sapLocation = sapBlock.getLocation().add(0.5, 0.5, 0.5);
        revertSap();

        Vector toHand = getHandLocation().toVector().subtract(sapLocation.toVector());
        double length = toHand.length();
        if (length > 0) {
            Vector dir = toHand.normalize();
            for (double d = 0; d < length; d += LINE_PARTICLE_SPACING) {
                sapLocation.getWorld().spawnParticle(Particle.DUST, sapLocation.clone().add(dir.clone().multiply(d)),
                        1, 0, 0, 0, 0, SAP_DUST);
            }
        }
        player.getWorld().playSound(sapLocation, Sound.BLOCK_HONEY_BLOCK_SLIDE, 1.0f, 1.2f);
        player.getWorld().playSound(player.getLocation(), Sound.BLOCK_GLASS_PLACE, 1.0f, 1.4f);

        phase = Phase.READY;
        readyStartTime = System.currentTimeMillis();
    }

    /**
     * Fires all the darts at once in a tight spread around the player's look direction.
     */
    private void fireDarts() {
        Location hand = getHandLocation();
        Vector lookDir = player.getEyeLocation().getDirection().normalize();
        double spread = Math.tan(Math.toRadians(spreadAngle));

        for (int i = 0; i < dartCount; i++) {
            Vector direction = lookDir.clone().add(getRingOffset(lookDir, i, spread)).normalize();
            darts.add(new Dart(hand.clone().add(getRingOffset(lookDir, i, HELD_RING_RADIUS)), direction));
        }

        phase = Phase.FIRED;
        bPlayer.addCooldown(this);
        player.getWorld().playSound(hand, Sound.ENTITY_ARROW_SHOOT, 1.0f, 1.4f);
        player.getWorld().playSound(hand, Sound.BLOCK_GLASS_HIT, 1.0f, 1.6f);
    }

    /**
     * Draws the hardened darts in a ring around the player's hand, pointing where they are looking.
     */
    private void drawHeldDarts() {
        Location hand = getHandLocation();
        Vector lookDir = player.getEyeLocation().getDirection().normalize();
        for (int i = 0; i < dartCount; i++) {
            Location base = hand.clone().add(getRingOffset(lookDir, i, HELD_RING_RADIUS));
            for (double d = 0; d < HELD_DART_LENGTH; d += STEP) {
                player.getWorld().spawnParticle(Particle.DUST, base.clone().add(lookDir.clone().multiply(d)),
                        1, 0, 0, 0, 0, DART_DUST);
            }
        }
    }

    /**
     * Provides the offset of a dart on a ring perpendicular to the given direction.
     *
     * @param direction The normalized direction the ring faces.
     * @param index     The index of the dart.
     * @param radius    The radius of the ring.
     * @return The offset from the center of the ring.
     */
    private Vector getRingOffset(Vector direction, int index, double radius) {
        Vector right = direction.clone().crossProduct(new Vector(0, 1, 0));
        right = right.lengthSquared() > 1e-6 ? right.normalize() : new Vector(1, 0, 0);
        Vector up = right.clone().crossProduct(direction);

        double angle = index * Math.PI * 2 / dartCount;
        return right.multiply(radius * Math.cos(angle)).add(up.multiply(radius * Math.sin(angle)));
    }

    /**
     * Cancels the ability while the sap is not yet extracted, otherwise loses the darts. Darts in flight will finish.
     */
    public void cancelFromSlotChange() {
        if (phase == Phase.SOURCING || phase == Phase.SOURCED) {
            remove();
        } else if (phase == Phase.READY) {
            loseDarts();
        }
    }

    /**
     * Loses the extracted sap and starts the cooldown.
     */
    private void loseDarts() {
        bPlayer.addCooldown(this);
        remove();
    }

    private void revertSap() {
        // A block placed in the sap's spot removes the TempBlock, and must not be overwritten by the air it replaced
        if (sapTempBlock != null && !sapTempBlock.isReverted() && TempBlock.isTempBlock(sapTempBlock.getBlock())) {
            sapTempBlock.revertBlock();
        }
    }

    /**
     * Finds the block the player is looking at within the source range.
     *
     * @return The result of the trace, or null if no block is within range.
     */
    private RayTraceResult traceTarget() {
        Location eye = player.getEyeLocation();
        return player.getWorld().rayTraceBlocks(eye, eye.getDirection(), sourceRange, FluidCollisionMode.NEVER, false);
    }

    private boolean canHoldSap(Block block) {
        return block.getType().isAir() && !TempBlock.isTempBlock(block);
    }

    private Location getHandLocation() {
        return GeneralMethods.getRightSide(player.getLocation(), 0.55)
                .add(0, player.getEyeHeight() - 0.5, 0);
    }

    public static boolean hasActiveInstance(UUID uuid) {
        return activeInstances.containsKey(uuid);
    }

    public static SapStrike getActiveInstance(UUID uuid) {
        return activeInstances.get(uuid);
    }

    @Override
    public void remove() {
        super.remove();
        revertSap();
        darts.clear();
        activeInstances.remove(player.getUniqueId());
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
        return cooldown;
    }

    @Override
    public String getName() {
        return "SapStrike";
    }

    @Override
    public Location getLocation() {
        return player.getLocation();
    }

    @Override
    public void load() {
    }

    @Override
    public void stop() {
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
        return "Draw the sap out of a tree and harden it into a volley of sharp darts, " +
                "striking your target for heavy damage and slowing them down.\n" +
                ChatUtils.translateToColor("&fUsage: Hold Sneak (on a log until sap forms) > Left-click (the sap) > Left-click (to fire)");
    }
}
