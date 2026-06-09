package com.aearost.aranarthcore.abilities.earthbending.lavabending;

import com.aearost.aranarthcore.utils.AranarthBendingUtils;
import com.aearost.aranarthcore.utils.ChatUtils;
import com.projectkorra.projectkorra.ability.AddonAbility;
import com.projectkorra.projectkorra.ability.LavaAbility;
import com.projectkorra.projectkorra.attribute.Attribute;
import com.projectkorra.projectkorra.util.DamageHandler;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

import java.util.*;

public class MagmaGlaives extends LavaAbility implements AddonAbility {

    public enum Phase {CHARGING, READY, FLYING}

    private static final int SOURCE_RANGE = 5;
    private static final long STAGE_1_MS = 500L;
    private static final long STAGE_2_MS = 1000L;
    private static final long CHARGE_DURATION = 1500L;
    private static final long READY_DURATION = 8000L;
    private static final double GLAIVE_RADIUS = 0.60;
    private static final int GLAIVE_ARMS = 3;
    private static final double CURVE_FACTOR = 1.05;
    private static final int POINTS_PER_ARM = 8;
    private static final double IDLE_ROTATION_SPEED = Math.toRadians(8);
    private static final double FLIGHT_ROTATION_SPEED = Math.toRadians(16);
    private static final double GLAIVE_SPEED = 1.2;
    private static final double STEER_FACTOR = 0.40;
    private static final double HIT_RADIUS = 1.8;
    private static final double SIDE_OFFSET = 0.85;
    private static final double HEIGHT_OFFSET = 0.50;
    private static final int PASSIVE_SOUND_INTERVAL = 16;

    // -------------------------------------------------------------------------
    // Lava colour palette — dark red at center, bright orange at tips
    // -------------------------------------------------------------------------

    private static final Particle.DustOptions LAVA_DEEP_RED = new Particle.DustOptions(Color.fromRGB(140, 10, 0), 1.1f);
    private static final Particle.DustOptions LAVA_RED = new Particle.DustOptions(Color.fromRGB(195, 30, 0), 1.0f);
    private static final Particle.DustOptions LAVA_ORANGE_RED = new Particle.DustOptions(Color.fromRGB(220, 65, 10), 0.9f);
    private static final Particle.DustOptions LAVA_ORANGE = new Particle.DustOptions(Color.fromRGB(250, 110, 25), 0.8f);
    private static final Particle.DustOptions[] LAVA_PALETTE = {LAVA_DEEP_RED, LAVA_RED, LAVA_ORANGE_RED, LAVA_ORANGE};

    @Attribute(Attribute.COOLDOWN)
    private long cooldown;
    @Attribute(Attribute.RANGE)
    private double range;
    @Attribute(Attribute.DAMAGE)
    private double damage;
    @Attribute(Attribute.KNOCKBACK)
    private double knockback;

    private Block sourceBlock;
    private BlockData sourceOriginalData;
    private long chargeStartTime;
    private boolean graniteApplied;
    private boolean netherrackApplied;
    private boolean fullyCharged;

    private Phase phase;
    private long readyStartTime;
    private double idleRotation;
    private boolean glaive1Fired;
    private boolean glaive2Fired;
    private FlyingGlaive flyingGlaive1;
    private FlyingGlaive flyingGlaive2;
    private int passiveSoundTick;
    private final Set<UUID> hitEntities = new HashSet<>();
    private static final Map<UUID, MagmaGlaives> activeInstances = new HashMap<>();
    private final Random random = new Random();

    // -------------------------------------------------------------------------
    // Inner class — flying glaive
    // -------------------------------------------------------------------------

    private static class FlyingGlaive {
        Location position;
        Location prevPosition;
        Vector direction;
        double distanceTraveled;
        double rotationAngle;
        /**
         * The player's horizontal right vector at the moment this glaive was thrown.
         * Locked in as the disc plane normal so the glaive stays visually upright
         * regardless of how its trajectory steers.
         */
        final Vector planeNormal;

        FlyingGlaive(Location start, Vector dir, Vector planeNormal) {
            this.position = start.clone();
            this.prevPosition = start.clone();
            this.direction = dir.clone().normalize();
            this.distanceTraveled = 0;
            this.rotationAngle = 0;
            this.planeNormal = planeNormal.clone().normalize();
        }
    }

    public MagmaGlaives(Player player) {
        super(player);

        if (!bPlayer.canBend(this)) {
            return;
        }

        cooldown = 9000;
        range = 16.0;
        damage = 6.0;
        knockback = 1.2;

        // Require an earthbendable block in the player's line of sight
        Block target = player.getTargetBlock(null, SOURCE_RANGE);
        if (!isEarthbendable(player, target)) {
            return;
        }

        sourceBlock = target;
        sourceOriginalData = sourceBlock.getBlockData().clone();
        chargeStartTime = System.currentTimeMillis();
        graniteApplied = false;
        netherrackApplied = false;
        fullyCharged = false;
        idleRotation = 0;
        passiveSoundTick = 0;
        glaive1Fired = false;
        glaive2Fired = false;
        phase = Phase.CHARGING;

        // Source block turns to stone immediately on acquisition
        sourceBlock.setType(AranarthBendingUtils.LAVA_WARMUP_SEQUENCE[0]);
        playTransitionSound(0.65f);

        activeInstances.put(player.getUniqueId(), this);
        start();
    }

    @Override
    public void progress() {
        if (!player.isOnline() || player.isDead()) {
            cancelInstantly();
            return;
        }
        switch (phase) {
            case CHARGING -> progressCharging();
            case READY -> progressReady();
            case FLYING -> progressFlying();
        }
    }

    private void progressCharging() {
        if (!player.isSneaking()) {
            if (fullyCharged) {
                // Player released sneak after full charge
                deployGlaives();
            } else {
                // Released sneak before the charge was complete
                cancelInstantly();
            }
            return;
        }

        long elapsed = System.currentTimeMillis() - chargeStartTime;

        if (!graniteApplied && elapsed >= STAGE_1_MS) {
            sourceBlock.setType(AranarthBendingUtils.LAVA_WARMUP_SEQUENCE[1]);
            graniteApplied = true;
            playTransitionSound(0.80f);
        }
        if (!netherrackApplied && elapsed >= STAGE_2_MS) {
            sourceBlock.setType(AranarthBendingUtils.LAVA_WARMUP_SEQUENCE[2]);
            netherrackApplied = true;
            playTransitionSound(0.95f);
        }
        if (!fullyCharged && elapsed >= CHARGE_DURATION) {
            sourceBlock.setType(AranarthBendingUtils.LAVA_WARMUP_SEQUENCE[3]);
            fullyCharged = true;
            playChargeCompleteSound();
        }

        // Rising particles above the transitioning block
        spawnChargeParticles();
    }

    private void playTransitionSound(float pitch) {
        Location loc = sourceBlock.getLocation().add(0.5, 0.5, 0.5);
        loc.getWorld().playSound(loc, Sound.BLOCK_STONE_PLACE, 1.0f, pitch);
    }

    private void playChargeCompleteSound() {
        Location loc = sourceBlock.getLocation().add(0.5, 0.5, 0.5);
        loc.getWorld().playSound(loc, Sound.BLOCK_LAVA_AMBIENT, 1.5f, 1.3f);
        loc.getWorld().playSound(loc, Sound.BLOCK_FIRE_AMBIENT, 0.9f, 0.5f);
        loc.getWorld().playSound(loc, Sound.BLOCK_STONE_BREAK, 0.8f, 0.4f);
    }

    /**
     * Spawns a tight cluster of rising lava ember particles above the source block.
     */
    private void spawnChargeParticles() {
        Location base = sourceBlock.getLocation().add(0.5, 1.05, 0.5);
        for (int i = 0; i < 4; i++) {
            double angle = random.nextDouble() * Math.PI * 2;
            double r = random.nextDouble() * 0.4;
            Location loc = base.clone().add(
                    Math.cos(angle) * r,
                    random.nextDouble() * 0.2,
                    Math.sin(angle) * r
            );
            base.getWorld().spawnParticle(
                    Particle.DUST, loc, 1, 0, 0, 0, 0,
                    LAVA_PALETTE[random.nextInt(LAVA_PALETTE.length)]
            );
        }
    }

    /**
     * Restores the source block to its original state. Safe to call multiple times.
     */
    private void restoreSourceBlock() {
        if (sourceBlock != null && sourceOriginalData != null) {
            sourceBlock.setBlockData(sourceOriginalData);
            sourceOriginalData = null;
        }
    }

    private void deployGlaives() {
        glaive1Fired = false;
        glaive2Fired = false;
        readyStartTime = System.currentTimeMillis();
        phase = Phase.READY;
        // Block becomes lava for the 8-second active window; restored when ability ends.
        if (sourceBlock != null) {
            sourceBlock.setType(Material.LAVA, false);
        }
    }

    private void progressReady() {
        if (System.currentTimeMillis() - readyStartTime >= READY_DURATION) {
            // 8-second window expired without both glaives being fired
            finishAbility();
            return;
        }

        idleRotation += IDLE_ROTATION_SPEED;
        renderSideGlaives();
        tickPassiveSounds();
    }

    private void progressFlying() {
        idleRotation += IDLE_ROTATION_SPEED;
        renderSideGlaives();
        tickPassiveSounds();

        boolean g1alive = stepGlaive(flyingGlaive1);
        boolean g2alive = stepGlaive(flyingGlaive2);

        if (!g1alive) {
            flyingGlaive1 = null;
        }
        if (!g2alive) {
            flyingGlaive2 = null;
        }

        // Both glaives have been thrown and both have finished flying
        if (glaive2Fired && flyingGlaive1 == null && flyingGlaive2 == null) {
            remove();
        }
    }

    /**
     * Fires the right glaive on the first click, then the left glaive on the second.
     */
    public void onLeftClick() {
        if (phase != Phase.READY && phase != Phase.FLYING) {
            return;
        }

        Location eyeLoc = player.getEyeLocation();
        Vector dir = eyeLoc.getDirection().normalize();
        Vector right = computeRightVector();

        Location convergencePoint = eyeLoc.clone().add(dir.clone().multiply(4.0));

        if (!glaive1Fired) {
            Location startPos = eyeLoc.clone()
                    .add(right.clone().multiply(SIDE_OFFSET))
                    .add(0, -HEIGHT_OFFSET, 0);
            Vector initialDir = convergencePoint.toVector().subtract(startPos.toVector()).normalize();
            flyingGlaive1 = new FlyingGlaive(startPos, initialDir, right);
            glaive1Fired = true;
            playThrowSound(startPos);
            phase = Phase.FLYING;

        } else if (!glaive2Fired) {
            Location startPos = eyeLoc.clone()
                    .add(right.clone().multiply(-SIDE_OFFSET))
                    .add(0, -HEIGHT_OFFSET, 0);
            Vector initialDir = convergencePoint.toVector().subtract(startPos.toVector()).normalize();
            flyingGlaive2 = new FlyingGlaive(startPos, initialDir, right);
            glaive2Fired = true;
            playThrowSound(startPos);
            // Both glaives are now in the air - start the cooldown immediately
            bPlayer.addCooldown(this);
        }
    }

    /**
     * Advances a flying glaive by one tick.
     */
    private boolean stepGlaive(FlyingGlaive g) {
        if (g == null) {
            return false;
        }

        // Steer toward the player's current look direction each tick
        Vector lookDir = player.getEyeLocation().getDirection().normalize();
        g.direction = g.direction.clone().add(lookDir.multiply(STEER_FACTOR)).normalize();

        g.prevPosition = g.position.clone();
        g.position.add(g.direction.clone().multiply(GLAIVE_SPEED));
        g.distanceTraveled += GLAIVE_SPEED;
        g.rotationAngle += FLIGHT_ROTATION_SPEED;

        if (g.position.getBlock().getType().isSolid()) {
            return false;
        }

        if (g.position.getBlock().getType() == Material.WATER) {
            g.position.getWorld().playSound(g.position, Sound.BLOCK_LAVA_EXTINGUISH, 1.0f, 0.4f);
            return false;
        }

        if (checkGlaiveCollision(g)) {
            return false;
        }

        renderGlaive(g.position, g.planeNormal, g.rotationAngle);

        return g.distanceTraveled < range;
    }

    /**
     * Performs a swept-line collision test between the glaive's movement segment and nearby living entities.
     */
    private boolean checkGlaiveCollision(FlyingGlaive g) {
        Vector p1 = g.prevPosition.toVector();
        Vector sweep = g.position.toVector().subtract(p1);
        double sweepLenSq = sweep.lengthSquared();

        for (LivingEntity entity : g.position.getWorld().getLivingEntities()) {
            if (entity.equals(player)) {
                continue;
            }
            if (hitEntities.contains(entity.getUniqueId())) {
                continue;
            }

            Vector center = entity.getLocation().add(0, entity.getHeight() / 2.0, 0).toVector();
            double t = (sweepLenSq > 0) ? center.clone().subtract(p1).dot(sweep) / sweepLenSq : 0;
            t = Math.max(0, Math.min(1, t));
            Vector closest = p1.clone().add(sweep.clone().multiply(t));
            if (center.distanceSquared(closest) > HIT_RADIUS * HIT_RADIUS) {
                continue;
            }

            hitEntities.add(entity.getUniqueId());
            DamageHandler.damageEntity(entity, damage, this);

            // Knockback with a slight upward pop
            Vector kb = g.direction.clone().normalize().multiply(knockback);
            kb.setY(kb.getY() + 0.28);
            entity.setVelocity(kb);

            // Rock impact sounds
            entity.getWorld().playSound(entity.getLocation(), Sound.BLOCK_STONE_HIT, 1.3f, 0.65f);
            entity.getWorld().playSound(entity.getLocation(), Sound.BLOCK_STONE_BREAK, 0.7f, 0.80f);
            return true;
        }
        return false;
    }

    private void renderSideGlaives() {
        if (glaive1Fired && glaive2Fired) {
            return;
        }

        Vector right = computeRightVector();
        Location eye = player.getEyeLocation();

        if (!glaive1Fired) {
            Location pos = eye.clone()
                    .add(right.clone().multiply(SIDE_OFFSET))
                    .add(0, -HEIGHT_OFFSET, 0);
            renderGlaive(pos, right, idleRotation);
        }
        if (!glaive2Fired) {
            Location pos = eye.clone()
                    .add(right.clone().multiply(-SIDE_OFFSET))
                    .add(0, -HEIGHT_OFFSET, 0);
            // Stagger by one third of a full turn so the two glaives don't look identical
            renderGlaive(pos, right, idleRotation + (Math.PI * 2.0 / 3.0));
        }
    }

    private void renderGlaive(Location center, Vector planeNormal, double rotation) {
        Vector axisA = new Vector(0, 1, 0);
        Vector axisB = new Vector(planeNormal.getZ(), 0, -planeNormal.getX());
        World world = center.getWorld();

        for (int arm = 0; arm < GLAIVE_ARMS; arm++) {
            double baseAngle = rotation + arm * (Math.PI * 2.0 / GLAIVE_ARMS);

            for (int p = 0; p < POINTS_PER_ARM; p++) {
                double t = (double) p / (POINTS_PER_ARM - 1); // 0 (center) → 1 (tip)
                double r = GLAIVE_RADIUS * t;
                double angle = baseAngle + CURVE_FACTOR * t;

                Location loc = center.clone().add(
                        axisA.clone().multiply(r * Math.cos(angle))
                                .add(axisB.clone().multiply(r * Math.sin(angle)))
                );

                // Colour gradient: dark red at center, bright orange at tip
                int colourIdx = (int) (t * (LAVA_PALETTE.length - 1));
                world.spawnParticle(Particle.DUST, loc, 1, 0, 0, 0, 0, LAVA_PALETTE[colourIdx]);
            }
        }
    }

    private void playThrowSound(Location loc) {
        World world = loc.getWorld();
        world.playSound(loc, Sound.ENTITY_IRON_GOLEM_ATTACK, 1.3f, 0.65f);
        world.playSound(loc, Sound.BLOCK_LAVA_AMBIENT, 0.9f, 1.5f);
    }

    private void tickPassiveSounds() {
        passiveSoundTick++;
        if (passiveSoundTick < PASSIVE_SOUND_INTERVAL) {
            return;
        }
        passiveSoundTick = 0;

        Location loc = player.getLocation();
        World world = loc.getWorld();
        if (!glaive1Fired) {
            world.playSound(loc, Sound.BLOCK_LAVA_AMBIENT, 0.45f, 1.35f + (float) (random.nextDouble() * 0.25));
        }
        if (!glaive2Fired) {
            world.playSound(loc, Sound.BLOCK_LAVA_AMBIENT, 0.45f, 1.35f + (float) (random.nextDouble() * 0.25));
        }
    }

    private void finishAbility() {
        restoreSourceBlock();
        bPlayer.addCooldown(this);
        remove();
    }

    public void cancelInstantly() {
        restoreSourceBlock();
        remove();
    }

    public void endWithCooldown() {
        restoreSourceBlock();
        bPlayer.addCooldown(this);
        remove();
    }

    @Override
    public void remove() {
        restoreSourceBlock();
        super.remove();
        activeInstances.remove(player.getUniqueId());
    }

    /**
     * Returns the player's current horizontal right vector, derived from their look direction.
     */
    private Vector computeRightVector() {
        Vector forward = player.getEyeLocation().getDirection();
        Vector flat = new Vector(forward.getX(), 0, forward.getZ());
        if (flat.lengthSquared() < 0.001) {
            flat = new Vector(0, 0, 1);
        }
        flat.normalize();
        return new Vector(-flat.getZ(), 0, flat.getX());
    }

    public static boolean hasActiveInstance(UUID uuid) {
        return activeInstances.containsKey(uuid);
    }

    public static MagmaGlaives getActiveInstance(UUID uuid) {
        return activeInstances.get(uuid);
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
        return cooldown;
    }

    @Override
    public Location getLocation() {
        return player.getLocation();
    }

    @Override
    public String getName() {
        return "MagmaGlaives";
    }

    @Override
    public void load() {
    }

    @Override
    public void stop() {
        restoreSourceBlock();
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
        return "Forge two spinning glaives of molten rock by channeling your lavabending, " +
                "generated by forming a block into magma. Once ready, the glaives are held at your sides.\n" +
                ChatUtils.translateToColor("&fUsage: Sneak (hold) > Sneak (release) > Left-click");
    }
}
