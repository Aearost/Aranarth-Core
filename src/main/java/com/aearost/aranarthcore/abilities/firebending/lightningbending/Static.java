package com.aearost.aranarthcore.abilities.firebending.lightningbending;

import com.aearost.aranarthcore.AranarthCore;
import com.aearost.aranarthcore.utils.AranarthBendingUtils;
import com.aearost.aranarthcore.utils.ChatUtils;
import com.projectkorra.projectkorra.ability.AddonAbility;
import com.projectkorra.projectkorra.ability.LightningAbility;
import com.projectkorra.projectkorra.attribute.Attribute;
import com.projectkorra.projectkorra.util.DamageHandler;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.UUID;

public class Static extends LightningAbility implements AddonAbility {

    private static final double PROJECTILE_SPEED = 1.5;
    private static final double STEP = 0.15;
    private static final double HIT_RADIUS = 0.8;
    private static final long ELECTROCUTION_DURATION_MS = 500L;

    private static final Map<UUID, Static> ACTIVE_INSTANCES = new HashMap<>();

    @Attribute(Attribute.DAMAGE)
    private double damage;
    @Attribute(Attribute.COOLDOWN)
    private long cooldown;
    @Attribute(Attribute.RANGE)
    private double range;
    @Attribute("BounceRadius")
    private double bounceRadius;
    @Attribute("MaxBounces")
    private int maxBounces;

    private Location shotLocation;
    private final Vector direction;
    private double distanceTraveled;
    private int stepIndex;
    private final Random rand = new Random();

    public Static(Player player) {
        super(player);

        direction = player.getEyeLocation().getDirection().normalize();

        if (!bPlayer.canBend(this) || hasActiveInstance(player.getUniqueId())) {
            return;
        }

        damage = 2.0;
        cooldown = 7000L;
        range = 7.0;
        bounceRadius = 5.0;
        maxBounces = 6;
        shotLocation = player.getEyeLocation().clone();
        distanceTraveled = 0.0;
        stepIndex = 0;

        ACTIVE_INSTANCES.put(player.getUniqueId(), this);
        bPlayer.addCooldown(this);
        player.getWorld().playSound(player.getEyeLocation(), Sound.ENTITY_BEE_HURT, 0.8f, 0.3f);
        start();
    }

    @Override
    public void progress() {
        if (player.isDead() || !player.isOnline()) {
            remove();
            return;
        }
        if (!bPlayer.canBendIgnoreCooldowns(this)) {
            remove();
            return;
        }
        progressTraveling();
    }

    private void progressTraveling() {
        double remaining = PROJECTILE_SPEED;

        while (remaining > 0) {
            double step = Math.min(STEP, remaining);

            // Advance along aim direction with small perpendicular jitter
            Vector advance = direction.clone().multiply(step);
            advance.setX(advance.getX() + (rand.nextDouble() - 0.5) * 0.04);
            advance.setY(advance.getY() + (rand.nextDouble() - 0.5) * 0.04);
            advance.setZ(advance.getZ() + (rand.nextDouble() - 0.5) * 0.04);
            shotLocation.add(advance);
            distanceTraveled += step;
            remaining -= step;

            spawnBoltParticle(shotLocation, stepIndex++);

            if (distanceTraveled > range) {
                shotLocation.getWorld().playSound(shotLocation, Sound.ENTITY_BEE_HURT, 0.4f, 0.5f);
                remove();
                return;
            }

            if (!isTransparent(shotLocation.getBlock())) {
                shotLocation.getWorld().playSound(shotLocation, Sound.ENTITY_BEE_HURT, 0.4f, 0.5f);
                remove();
                return;
            }

            LivingEntity hit = findEntityAt(shotLocation);
            if (hit != null) {
                Set<UUID> alreadyHit = new HashSet<>();
                alreadyHit.add(player.getUniqueId());
                alreadyHit.add(hit.getUniqueId());
                strikeEntity(hit);
                chain(hit, alreadyHit, 1);
                remove();
                return;
            }
        }
    }

    /**
     * Schedules the next chain bounce 5 ticks after the previous strike
     *
     * @param from        The entity the bolt last struck.
     * @param alreadyHit  UUIDs of all entities already struck this cast.
     * @param bouncesDone Number of successful strikes so far, including the initial hit.
     */
    private void chain(LivingEntity from, Set<UUID> alreadyHit, int bouncesDone) {
        if (bouncesDone >= maxBounces) {
            return;
        }

        Location fromLoc = from.getLocation().clone();
        Location fromCenter = fromLoc.clone().add(0, from.getHeight() * 0.5, 0);

        new BukkitRunnable() {
            @Override
            public void run() {
                LivingEntity next = findClosestNearby(fromLoc, alreadyHit);
                if (next == null) {
                    return;
                }

                drawChainPath(fromCenter, next.getLocation().clone().add(0, next.getHeight() * 0.5, 0));

                next.getWorld().playSound(next.getLocation(), Sound.ENTITY_BEE_HURT, 0.6f, 0.2f);
                strikeEntity(next);
                alreadyHit.add(next.getUniqueId());
                chain(next, alreadyHit, bouncesDone + 1);
            }
        }.runTaskLater(AranarthCore.getInstance(), 5L);
    }

    /**
     * Applies damage and electrocution to a struck entity.
     *
     * @param target The entity to strike.
     */
    private void strikeEntity(LivingEntity target) {
        double actualDamage = (target instanceof Player) ? damage : damage * 3.0;
        DamageHandler.damageEntity(target, actualDamage, this);
        AranarthBendingUtils.applyElectrocution(target, ELECTROCUTION_DURATION_MS, 0.1);

        for (int k = 0; k < 6; k++) {
            Particle.DustOptions dust = (k % 2 == 0)
                    ? AranarthBendingUtils.LIGHTNING_DUST_BRIGHT
                    : AranarthBendingUtils.LIGHTNING_DUST_BLUE;
            target.getLocation().getWorld().spawnParticle(Particle.DUST, target.getLocation(), 1,
                    rand.nextDouble() * 0.4, rand.nextDouble() * target.getHeight() * 0.5,
                    rand.nextDouble() * 0.4, 0, dust);
        }
    }

    /**
     * Draws a jagged lightning path between two locations using alternating DUST
     * particles.
     *
     * @param from Start location (typically the struck entity's center).
     * @param to   End location (typically the next target's center).
     */
    private void drawChainPath(Location from, Location to) {
        Vector path = to.toVector().subtract(from.toVector());
        if (path.length() < 0.001) {
            return;
        }

        Vector stepVec = path.normalize().multiply(STEP);
        Location current = from.clone();
        int i = 0;

        while (current.distanceSquared(to) > STEP * STEP) {
            Location drawPos = current.clone().add(
                    (rand.nextDouble() - 0.5) * 0.05,
                    (rand.nextDouble() - 0.5) * 0.05,
                    (rand.nextDouble() - 0.5) * 0.05);

            Particle.DustOptions dust = (i % 2 == 0)
                    ? AranarthBendingUtils.LIGHTNING_DUST_BRIGHT
                    : AranarthBendingUtils.LIGHTNING_DUST_BLUE;
            drawPos.getWorld().spawnParticle(Particle.DUST, drawPos, 1, 0.01, 0.01, 0.01, 0, dust);
            current.add(stepVec);
            i++;
        }
    }

    /**
     * Spawns a lightning trail particle at the given position.
     *
     * @param pos       World position of the particle.
     * @param stepIndex Current step index, used to alternate dust colours.
     */
    private void spawnBoltParticle(Location pos, int stepIndex) {
        Particle.DustOptions dust = (stepIndex % 2 == 0)
                ? AranarthBendingUtils.LIGHTNING_DUST_BRIGHT
                : AranarthBendingUtils.LIGHTNING_DUST_BLUE;
        pos.getWorld().spawnParticle(Particle.DUST, pos, 1, 0.02, 0.02, 0.02, 0, dust);
    }

    /**
     * Returns the first living entity within HIT_RADIUS of the given location,
     * excluding the caster.
     *
     * @param loc The location to check.
     * @return The first living entity found, or null.
     */
    private LivingEntity findEntityAt(Location loc) {
        for (Entity entity : loc.getWorld().getNearbyEntities(loc, HIT_RADIUS, HIT_RADIUS, HIT_RADIUS)) {
            if (!(entity instanceof LivingEntity living) || entity.equals(player)) {
                continue;
            }
            return living;
        }
        return null;
    }

    /**
     * Finds the closest living entity within bounceRadius of the given location,
     * excluding UUIDs in the provided set.
     *
     * @param loc        Centre of the search area.
     * @param alreadyHit UUIDs of entities to skip.
     * @return The closest eligible entity, or null if none exist.
     */
    private LivingEntity findClosestNearby(Location loc, Set<UUID> alreadyHit) {
        LivingEntity closest = null;
        double closestDistSq = Double.MAX_VALUE;

        for (Entity entity : loc.getWorld().getNearbyEntities(loc, bounceRadius, bounceRadius, bounceRadius)) {
            if (!(entity instanceof LivingEntity living)) {
                continue;
            }
            if (alreadyHit.contains(entity.getUniqueId())) {
                continue;
            }
            double distSq = entity.getLocation().distanceSquared(loc);
            if (distSq < closestDistSq) {
                closestDistSq = distSq;
                closest = living;
            }
        }
        return closest;
    }

    public static boolean hasActiveInstance(UUID uuid) {
        return ACTIVE_INSTANCES.containsKey(uuid);
    }

    public static Static getActiveInstance(UUID uuid) {
        return ACTIVE_INSTANCES.get(uuid);
    }

    @Override
    public void remove() {
        ACTIVE_INSTANCES.remove(player.getUniqueId());
        super.remove();
    }

    @Override
    public void stop() {
        if (player != null) {
            ACTIVE_INSTANCES.remove(player.getUniqueId());
        }
    }

    @Override
    public long getCooldown() {
        return cooldown;
    }

    @Override
    public Location getLocation() {
        return shotLocation != null ? shotLocation : player.getLocation();
    }

    @Override
    public boolean isSneakAbility() {
        return false;
    }

    @Override
    public boolean isHarmlessAbility() {
        return false;
    }

    @Override
    public String getName() {
        return "Static";
    }

    @Override
    public void load() {
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
        return "Fire a jagged bolt of static electricity that chains between up to 6 targets within 3 blocks of each other. " +
                "Each struck entity is electrocuted and stunned; non-player entities take triple damage per hit.\n" +
                ChatUtils.translateToColor("&fUsage: Left-Click");
    }
}
