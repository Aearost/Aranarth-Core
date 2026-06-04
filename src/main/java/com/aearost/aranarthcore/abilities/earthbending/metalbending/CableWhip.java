package com.aearost.aranarthcore.abilities.earthbending.metalbending;

import com.aearost.aranarthcore.utils.AranarthBendingUtils;
import com.aearost.aranarthcore.utils.ChatUtils;
import com.projectkorra.projectkorra.GeneralMethods;
import com.projectkorra.projectkorra.ability.AddonAbility;
import com.projectkorra.projectkorra.ability.MetalAbility;
import com.projectkorra.projectkorra.attribute.Attribute;
import com.projectkorra.projectkorra.util.DamageHandler;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.util.BoundingBox;
import org.bukkit.util.Vector;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public class CableWhip extends MetalAbility implements AddonAbility {

    public enum Phase { CHARGING, READY, WHIPPING }
    private static final long CHARGE_DURATION_MS = 500L;
    private static final long READY_DURATION_MS = 3000L;
    private static final int MAX_WHIPS = 3;
    private static final int WHIP_TICKS = 20;
    private static final int IDLE_TICKS = 4;
    private static final int POST_HIT_LINGER_TICKS = 4;
    private static final double WHIP_HIT_RADIUS = 0.8;
    private static final double HIT_DELAY_FRACTION = 0.3;
    private static final int HANG_STEPS = 8;
    private static final double HANG_STEP_SIZE = 0.25;
    private static final double HAND_OFFSET = 0.3;
    private static final Particle.DustOptions CABLE_DUST =
            new Particle.DustOptions(Color.fromRGB(55, 55, 58), 0.5f);
    private static final Map<UUID, CableWhip> ACTIVE_INSTANCES = new HashMap<>();

    @Attribute(Attribute.COOLDOWN)
    private long cooldown;
    @Attribute(Attribute.DAMAGE)
    private double damage;
    @Attribute(Attribute.RANGE)
    private double range;
    private Phase phase;
    private long chargeStartTime;
    private long readyStartTime;
    private Vector whipDirection;
    private Vector prevWhipDirection;
    private double prevYawDelta;
    private int idleTicks;
    private int whipTick;
    private final Set<UUID> whipHitEntities = new HashSet<>();
    private int whipsDone;
    private boolean hasHit;
    private int ticksSinceHit;

    public CableWhip(final Player player) {
        super(player);

        if (!bPlayer.canBendIgnoreBindsCooldowns(this)) {
            return;
        }
        if (bPlayer.isOnCooldown(this)) {
            return;
        }
        if (!AranarthBendingUtils.hasMetalRequirement(player)) {
            return;
        }

        this.cooldown        = 6000L;
        this.damage          = 5.0;
        this.range           = 6.0;
        this.phase           = Phase.CHARGING;
        this.chargeStartTime = System.currentTimeMillis();
        this.whipsDone       = 0;

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
            case CHARGING -> progressCharging();
            case READY    -> progressReady();
            case WHIPPING -> progressWhipping();
        }
    }

    private void progressCharging() {
        if (!player.isSneaking()) {
            // Released before the charge completed - no cooldown.
            remove();
            return;
        }

        spawnHangingCables();

        if (System.currentTimeMillis() - chargeStartTime >= CHARGE_DURATION_MS) {
            readyStartTime = System.currentTimeMillis();
            phase = Phase.READY;
            // Light metallic chime signals that the cables are ready to crack.
            player.getWorld().playSound(player.getLocation(), Sound.BLOCK_CHAIN_STEP, 0.55f, 1.2f);
        }
    }

    private void progressReady() {
        if (!player.isSneaking()) {
            // Sneak released after charge
            if (whipsDone > 0) {
                finishWithCooldown();
            } else {
                remove();
            }
            return;
        }

        // 3-second ready window elapsed
        if (System.currentTimeMillis() - readyStartTime >= READY_DURATION_MS) {
            finishWithCooldown();
            return;
        }

        // Idle hanging-cable animation, thinned to every other tick
        if ((System.currentTimeMillis() / 50L) % 2L == 0L) {
            spawnHangingCables();
        }
    }

    private void progressWhipping() {
        whipTick++;

        final Vector curDirection = player.getEyeLocation().getDirection().normalize();

        // Reversal and idle detection
        final double yawDelta = prevWhipDirection.getX() * curDirection.getZ()
                              - prevWhipDirection.getZ() * curDirection.getX();
        if (Math.abs(yawDelta) > 0.01) {
            // Intentional drag this tick
            if (prevYawDelta != 0.0 && Math.signum(yawDelta) != Math.signum(prevYawDelta)) {
                // Direction reversed
                consumeCrack();
                return;
            }
            prevYawDelta = yawDelta;
            idleTicks = 0;
        } else if (prevYawDelta != 0.0) {
            // Mouse has stopped after at least one drag tick
            idleTicks++;
            if (idleTicks >= IDLE_TICKS) {
                consumeCrack();
                return;
            }
        }

        // End the crack shortly after landing a hit
        if (hasHit) {
            ticksSinceHit++;
            if (ticksSinceHit >= POST_HIT_LINGER_TICKS) {
                consumeCrack();
                return;
            }
        }

        final Location hand = GeneralMethods.getMainHandLocation(player);
        final double tipFraction = (double) whipTick / WHIP_TICKS;

        final int VISUAL_STEPS = 4;
        for (int s = 1; s <= VISUAL_STEPS; s++) {
            final double t = (double) s / VISUAL_STEPS;
            whipDirection = prevWhipDirection.clone()
                    .add(curDirection.clone().subtract(prevWhipDirection).multiply(t))
                    .normalize();
            drawWhipLine(hand);
        }

        if (tipFraction >= HIT_DELAY_FRACTION) {
            final int HIT_STEPS = 10;
            for (int s = 1; s <= HIT_STEPS; s++) {
                final double t = (double) s / HIT_STEPS;
                whipDirection = prevWhipDirection.clone()
                        .add(curDirection.clone().subtract(prevWhipDirection).multiply(t))
                        .normalize();
                checkLineHits(hand);
            }
        }

        whipDirection     = curDirection;
        prevWhipDirection = curDirection;

        if (whipTick >= WHIP_TICKS) {
            consumeCrack();
        }
    }

    /**
     * Finishes one whip crack.
     */
    private void consumeCrack() {
        whipsDone++;
        if (whipsDone >= MAX_WHIPS) {
            finishWithCooldown();
        } else {
            whipHitEntities.clear();
            hasHit        = false;
            ticksSinceHit = 0;
            phase = Phase.READY;
        }
    }

    private void drawWhipLine(final Location hand) {
        final int drawSteps = Math.max(2, (int) (range / 0.25));
        for (int i = 0; i <= drawSteps; i++) {
            final double s   = (double) i / drawSteps;
            final Location loc = hand.clone().add(whipDirection.clone().multiply(s * range));
            loc.getWorld().spawnParticle(Particle.DUST, loc, 1, 0, 0, 0, 0, CABLE_DUST);
        }
    }

    private void checkLineHits(final Location hand) {
        final Location end = hand.clone().add(whipDirection.clone().multiply(range));
        final double r = WHIP_HIT_RADIUS;
        final BoundingBox lineBounds = new BoundingBox(
                Math.min(hand.getX(), end.getX()) - r,
                Math.min(hand.getY(), end.getY()) - r,
                Math.min(hand.getZ(), end.getZ()) - r,
                Math.max(hand.getX(), end.getX()) + r,
                Math.max(hand.getY(), end.getY()) + r,
                Math.max(hand.getZ(), end.getZ()) + r);

        final Vector origin = hand.toVector();

        for (final Entity entity : hand.getWorld().getNearbyEntities(lineBounds)) {
            if (entity.equals(player)) continue;
            if (!(entity instanceof LivingEntity)) continue;
            if (whipHitEntities.contains(entity.getUniqueId())) continue;

            // Expand the entity's actual hitbox by the cable thickness and do a true ray test
            if (entity.getBoundingBox().expand(r).rayTrace(origin, whipDirection, range) == null) continue;

            whipHitEntities.add(entity.getUniqueId());
            DamageHandler.damageEntity(entity, this.damage, this);
            hasHit = true;

            entity.getWorld().playSound(entity.getLocation(), Sound.ENTITY_IRON_GOLEM_ATTACK, 1.0f, 1.9f);
            entity.getWorld().playSound(entity.getLocation(), Sound.BLOCK_METAL_HIT,          0.6f, 2.0f);
        }
    }

    public void onLeftClick() {
        if (phase == Phase.WHIPPING) {
            // Left-clicking during an active crack consumes it immediately.
            consumeCrack();
            return;
        }
        if (phase != Phase.READY) {
            return;
        }
        whipDirection     = player.getEyeLocation().getDirection().normalize();
        prevWhipDirection = whipDirection.clone();
        prevYawDelta      = 0.0;
        idleTicks         = 0;
        whipTick          = 0;
        hasHit            = false;
        ticksSinceHit     = 0;
        whipHitEntities.clear();
        phase = Phase.WHIPPING;

        // Cable-swish + metallic crack at the moment of release.
        final Location loc = player.getLocation();
        player.getWorld().playSound(loc, Sound.BLOCK_CHAIN_STEP,         0.7f,  1.6f);
        player.getWorld().playSound(loc, Sound.ENTITY_IRON_GOLEM_ATTACK, 0.85f, 2.0f);
    }

    public void cancelInstantly() {
        remove();
    }


    public void endWithCooldown() {
        finishWithCooldown();
    }

    private void spawnHangingCables() {
        // Build a right-vector perpendicular to the player's facing direction on the XZ plane
        final Vector forward = player.getLocation().getDirection().setY(0).normalize();
        final Vector right   = forward.crossProduct(new Vector(0, 1, 0)).normalize()
                                       .multiply(HAND_OFFSET);

        // Approximate hand height at ~1.1 blocks above foot level
        final Location center   = player.getLocation().add(0, 1.1, 0);
        final Location mainHand = center.clone().add(right);
        final Location offHand  = center.clone().subtract(right);

        for (int i = 0; i < HANG_STEPS; i++) {
            final double drop = i * HANG_STEP_SIZE;
            mainHand.getWorld().spawnParticle(
                    Particle.DUST, mainHand.clone().add(0, -drop, 0),
                    1, 0, 0, 0, 0, CABLE_DUST);
            offHand.getWorld().spawnParticle(
                    Particle.DUST, offHand.clone().add(0, -drop, 0),
                    1, 0, 0, 0, 0, CABLE_DUST);
        }
    }

    private void finishWithCooldown() {
        bPlayer.addCooldown(this);
        remove();
    }

    @Override
    public void remove() {
        ACTIVE_INSTANCES.remove(player.getUniqueId());
        super.remove();
    }

    public static boolean hasActiveInstance(final UUID uuid) {
        return ACTIVE_INSTANCES.containsKey(uuid);
    }

    public static CableWhip getActiveInstance(final UUID uuid) {
        return ACTIVE_INSTANCES.get(uuid);
    }

    public Phase getPhase() {
        return phase;
    }

    public int getWhipsDone() {
        return whipsDone;
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
        return player.getLocation();
    }

    @Override
    public String getName() {
        return "CableWhip";
    }

    @Override
    public void load() {}

    @Override
    public void stop() {}

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
        return "Extend your metal cables downwards and crack them like a whip to lash nearby enemies up close. "
                + "You may lash up to three times before the cables retract.\n"
                + ChatUtils.translateToColor("&fUsage: Hold Sneak (charge) > Left-click (drag) up to 3x");
    }
}
