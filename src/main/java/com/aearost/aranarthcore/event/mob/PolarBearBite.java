package com.aearost.aranarthcore.event.mob;

import com.aearost.aranarthcore.utils.MountUtils;
import org.bukkit.Bukkit;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.PolarBear;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import java.util.*;

/**
 * Bite ability for the Polar Bear Dog (Water) mount.
 *
 * <p><b>On land/air:</b> the bear lunges in the rider's look direction. Max range scales
 * with the upward angle — 12 blocks horizontal, down to 5 blocks straight up. The lunge
 * stops on first entity contact, dealing high damage plus reduced splash damage to up to
 * {@value MAX_AOE_TARGETS} targets within {@value AOE_RADIUS} blocks.
 *
 * <p><b>In water:</b> no dash occurs. The bear bites any living entity within
 * {@value HIT_RANGE} blocks immediately, with all the same damage and effects.
 */
public class PolarBearBite extends BukkitRunnable {

    static final Set<UUID> LUNGING_MOUNTS = new HashSet<>();
    private static final double LUNGE_SPEED = 1.5;           // blocks/tick (constant)
    private static final double MAX_RANGE_HORIZONTAL = 12.0;  // max distance when looking level
    private static final double MAX_RANGE_VERTICAL = 5.0;     // max distance when looking straight up
    private static final double HIT_RANGE = 2.0;              // contact radius to trigger bite
    private static final double AOE_RADIUS = 2.0;             // splash radius around primary hit
    private static final int MAX_AOE_TARGETS = 5;             // max total entities hit (incl. primary)
    private static final int LUNGE_SAFETY_TICKS = 12;         // hard timeout fallback
    private static final double MIN_DAMAGE = 5.0;             // half-hearts floor
    private static final double AOE_DAMAGE_FRACTION = 0.65;   // AOE targets take 65% of primary damage

    static final int BASE_XP = 4;
    static final int XP_PER_HIT = 2;

    private final UUID bearUUID;
    private final UUID riderUUID;
    private final Map<UUID, PolarBearBite> activeLunges;
    private final double maxDamage;
    /** Full 3D lunge velocity vector (direction * LUNGE_SPEED). Null when in water. */
    private final Vector lungeDir;
    /** Maximum 3D distance this lunge may travel, derived from look angle. */
    private final double maxDistance;
    /** True when activated while the bear is in water — skips all dash logic. */
    private final boolean inWater;
    private final Random random = new Random();
    private int tick = 0;
    private double distanceTraveled = 0;

    public PolarBearBite(PolarBear bear, Player rider,
                         Map<UUID, PolarBearBite> activeLunges, double maxDamage) {
        this.bearUUID = bear.getUniqueId();
        this.riderUUID = rider.getUniqueId();
        this.activeLunges = activeLunges;
        this.maxDamage = maxDamage;
        this.inWater = MountUtils.isMountInWater(bear.getUniqueId());

        if (inWater) {
            // No lunge — these fields are unused in the water path
            this.lungeDir = null;
            this.maxDistance = 0;
        } else {
            // Full 3D look direction — getDirection() returns a unit vector
            Vector look = rider.getEyeLocation().getDirection();
            this.lungeDir = look.clone().multiply(LUNGE_SPEED);

            // The more the player looks upward, the shorter the lunge range.
            // Only upward angles are penalised (looking down is treated as horizontal).
            double upFraction = Math.max(0.0, look.getY()); // 0 = horizontal, 1 = straight up
            this.maxDistance = MAX_RANGE_HORIZONTAL
                    + (MAX_RANGE_VERTICAL - MAX_RANGE_HORIZONTAL) * upFraction;

            // Only claim velocity control when actually lunging
            LUNGING_MOUNTS.add(bear.getUniqueId());
        }
    }

    @Override
    public void run() {
        tick++;

        Entity entity = Bukkit.getEntity(bearUUID);
        if (!(entity instanceof PolarBear bear) || bear.isDead()) {
            finish();
            return;
        }
        if (bear.getPassengers().isEmpty()) {
            finish();
            return;
        }

        // ── Water path: immediate melee bite, no movement ────────────────────
        if (inWater) {
            for (Entity nearby : bear.getNearbyEntities(HIT_RANGE, HIT_RANGE, HIT_RANGE)) {
                if (!(nearby instanceof LivingEntity target)) continue;
                if (target.getUniqueId().equals(bearUUID)) continue;
                if (target.getUniqueId().equals(riderUUID)) continue;
                bite(bear, target);
                break;
            }
            finish();
            return;
        }

        // ── Land/air lunge path ───────────────────────────────────────────────
        if (tick > LUNGE_SAFETY_TICKS) {
            finish();
            return;
        }

        // Stop before the next step would exceed the range cap
        if (distanceTraveled + LUNGE_SPEED > maxDistance) {
            finish();
            return;
        }

        // Drive the bear in the full 3D look direction at constant speed
        bear.setVelocity(lungeDir);
        distanceTraveled += LUNGE_SPEED;

        // Check for a contact hit at the bear's current position
        for (Entity nearby : bear.getNearbyEntities(HIT_RANGE, HIT_RANGE, HIT_RANGE)) {
            if (!(nearby instanceof LivingEntity target)) continue;
            if (target.getUniqueId().equals(bearUUID)) continue;
            if (target.getUniqueId().equals(riderUUID)) continue;
            bite(bear, target);
            finish();
            return;
        }
    }

    private void bite(PolarBear bear, LivingEntity primaryTarget) {
        double damage = MIN_DAMAGE + random.nextDouble() * (maxDamage - MIN_DAMAGE);

        // Play evoker jaw sound at the bear's location
        bear.getWorld().playSound(bear.getLocation(), Sound.ENTITY_EVOKER_FANGS_ATTACK, 1.5f, 0.8f);

        // Build the list of targets: primary first, then AOE neighbours
        List<LivingEntity> targets = new ArrayList<>();
        targets.add(primaryTarget);

        for (Entity nearby : primaryTarget.getNearbyEntities(AOE_RADIUS, AOE_RADIUS, AOE_RADIUS)) {
            if (targets.size() >= MAX_AOE_TARGETS) break;
            if (!(nearby instanceof LivingEntity le)) continue;
            if (le.getUniqueId().equals(bearUUID)) continue;
            if (le.getUniqueId().equals(riderUUID)) continue;
            targets.add(le);
        }

        // Deal damage — full to primary, reduced to AOE targets
        for (LivingEntity target : targets) {
            double d = target.equals(primaryTarget) ? damage : damage * AOE_DAMAGE_FRACTION;
            target.damage(d, bear);
        }

        if (!inWater) {
            // Stop the bear dead after a land/air impact
            bear.setVelocity(new Vector(0, 0, 0));
        }

        // Visual feedback
        bear.getWorld().spawnParticle(Particle.SWEEP_ATTACK,
                primaryTarget.getLocation().add(0, 1.0, 0), 4, 0.4, 0.3, 0.4, 0);
        bear.getWorld().spawnParticle(Particle.CRIT,
                primaryTarget.getLocation().add(0, 1.0, 0), 8, 0.3, 0.2, 0.3, 0.05);

        // Award bite XP
        MountUtils.addBiteXp(bearUUID, BASE_XP + targets.size() * XP_PER_HIT);
    }

    private void finish() {
        if (!inWater) {
            // Zero residual velocity so the bear doesn't coast beyond the range cap.
            // When called after a land/air bite(), velocity is already zero — harmless double-zero.
            Entity e = Bukkit.getEntity(bearUUID);
            if (e instanceof PolarBear bear && !bear.isDead()) {
                bear.setVelocity(new Vector(0, 0, 0));
            }
        }
        LUNGING_MOUNTS.remove(bearUUID); // safe even if never added (water path)
        activeLunges.remove(bearUUID);
        this.cancel();
    }
}
