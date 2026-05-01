package com.aearost.aranarthcore.abilities.firebending;

import com.aearost.aranarthcore.utils.ChatUtils;
import com.projectkorra.projectkorra.ability.AddonAbility;
import com.projectkorra.projectkorra.ability.CombustionAbility;
import com.projectkorra.projectkorra.attribute.Attribute;
import com.projectkorra.projectkorra.util.DamageHandler;
import com.projectkorra.projectkorra.util.TempBlock;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

public class Barrage extends CombustionAbility implements AddonAbility {

    public enum Phase { CASTING, TRAVELING }

    @Attribute(Attribute.COOLDOWN)
    private long cooldown;
    @Attribute(Attribute.RANGE)
    private double range;
    @Attribute(Attribute.DAMAGE)
    private double burstDamage;
    @Attribute("MisfireDamage")
    private double misfireDamage;
    @Attribute("AoeRadius")
    private double aoeRadius;

    private Phase phase;
    private boolean misfired;
    private boolean selfBlasted;
    private int burstsQueued;
    private long lastBurstTime;
    private double initialHealth;

    private final List<CombustionBurst> bursts = new ArrayList<>();

    private static final int TOTAL_BURSTS = 3;
    private static final long BURST_INTERVAL_MS = 500;
    private static final double SPEED = 2.5; // blocks per tick
    private static final double STEP = 0.4;
    private static final double HIT_RADIUS = 0.8;

    private static final Map<UUID, Barrage> activeInstances = new HashMap<>();

    private static class CombustionBurst {
        Location position;
        final Vector direction;
        double distanceTraveled;
        double nextSoundDistance;

        CombustionBurst(Location start, Vector direction) {
            this.position = start.clone();
            this.direction = direction.clone().normalize();
            this.distanceTraveled = 0;
            this.nextSoundDistance = 7 + ThreadLocalRandom.current().nextDouble() * 3;
        }
    }

    public Barrage(Player player) {
        super(player);

        if (!bPlayer.canBend(this)) {
            return;
        }

        cooldown = 7000;
        range = 25;
        burstDamage = 8.0;   // 4 hearts
        misfireDamage = 12.0; // 6 hearts
        aoeRadius = 2.0;

        phase = Phase.CASTING;
        misfired = false;
        selfBlasted = false;
        burstsQueued = 0;
        lastBurstTime = System.currentTimeMillis();
        initialHealth = player.getHealth();

        activeInstances.put(player.getUniqueId(), this);
        fireBurst();
        start();
    }

    // -------------------------------------------------------------------------
    // PK tick
    // -------------------------------------------------------------------------

    @Override
    public void progress() {
        if (player.isDead() || !player.isOnline()) {
            cleanup();
            remove();
            return;
        }

        // If the player took a hit during the casting window
        if (phase == Phase.CASTING && !misfired && player.getHealth() + 0.5 < initialHealth) {
            triggerMisfire();
            return;
        }

        // Fire subsequent bursts every 0.5s intervals while still casting
        if (phase == Phase.CASTING && burstsQueued < TOTAL_BURSTS) {
            long now = System.currentTimeMillis();
            if (now - lastBurstTime >= BURST_INTERVAL_MS) {
                fireBurst();
                lastBurstTime = now;
            }
        }

        // Transition to traveling once all bursts are queued
        if (phase == Phase.CASTING && burstsQueued >= TOTAL_BURSTS) {
            phase = Phase.TRAVELING;
        }

        // Advance all in-flight bursts, remove those that have resolved
        bursts.removeIf(this::progressBurst);

        // A burst caught the caster in its blast should end like a misfire
        if (selfBlasted) {
            finishAbility();
            return;
        }

        // End when all bursts have finished traveling
        if (phase == Phase.TRAVELING && bursts.isEmpty()) {
            finishAbility();
        }
    }

    // -------------------------------------------------------------------------
    // Burst firing
    // -------------------------------------------------------------------------

    private void fireBurst() {
        Location origin = player.getEyeLocation();
        Vector direction = origin.getDirection().normalize();
        bursts.add(new CombustionBurst(origin, direction));
        burstsQueued++;
        origin.getWorld().playSound(origin, Sound.ENTITY_FIREWORK_ROCKET_BLAST, 1.5f, 0.01f);
    }

    // -------------------------------------------------------------------------
    // Burst tick
    // -------------------------------------------------------------------------

    /**
     * Advances a burst by one tick's worth of movement.
     * Returns true when the burst should be removed.
     */
    private boolean progressBurst(CombustionBurst burst) {
        double targetDistance = burst.distanceTraveled + SPEED;

        while (burst.distanceTraveled < targetDistance) {
            double step = Math.min(STEP, targetDistance - burst.distanceTraveled);
            burst.position.add(burst.direction.clone().multiply(step));
            burst.distanceTraveled += step;

            // Range check
            if (burst.distanceTraveled > range) {
                return true;
            }

            Block block = burst.position.getBlock();

            // Solid block or liquid collision
            if (block.getType().isSolid() || block.isLiquid()) {
                createExplosion(burst.position.clone());
                return true;
            }

            // Entity hit
            LivingEntity hit = findNearbyLivingEntity(burst.position, HIT_RADIUS);
            if (hit != null) {
                createExplosion(hit.getLocation().add(0, hit.getHeight() / 2.0, 0));
                return true;
            }

            // Trail particles every other step (~every 0.8 blocks)
            if (((int) (burst.distanceTraveled / STEP)) % 2 == 0) {
                burst.position.getWorld().spawnParticle(Particle.LARGE_SMOKE, burst.position.clone(), 1, 0, 0, 0, 0.04);
                burst.position.getWorld().spawnParticle(Particle.FIREWORK, burst.position.clone(), 1, 0, 0, 0, 0.04);
            }

            // Periodic ring burst + sound
            if (burst.distanceTraveled >= burst.nextSoundDistance) {
                burst.position.getWorld().playSound(burst.position, Sound.ENTITY_FIREWORK_ROCKET_BLAST, 1.0f, 0.01f);
                burst.nextSoundDistance = burst.distanceTraveled + 7 + ThreadLocalRandom.current().nextDouble() * 3;
                spawnBurstRing(burst.position.clone(), burst.direction);
            }
        }

        return false;
    }

    // -------------------------------------------------------------------------
    // Ring particle effect
    // -------------------------------------------------------------------------

    private void spawnBurstRing(Location center, Vector direction) {
        Vector up = new Vector(0, 1, 0);
        Vector perp1;
        if (Math.abs(direction.dot(up)) > 0.99) {
            perp1 = direction.clone().crossProduct(new Vector(1, 0, 0)).normalize();
        } else {
            perp1 = direction.clone().crossProduct(up).normalize();
        }
        Vector perp2 = direction.clone().crossProduct(perp1).normalize();

        double radius = ThreadLocalRandom.current().nextDouble(0.5, 1.2);
        for (int angle = 0; angle <= 360; angle += 20) {
            double radians = Math.toRadians(angle);
            double cos = Math.cos(radians);
            double sin = Math.sin(radians);

            // Slight inward offset so the ring starts close to the projectile center
            Vector offset = perp1.clone().multiply(cos * 0.2).add(perp2.clone().multiply(sin * 0.2));
            // Velocity radiating outward
            Vector vel = perp1.clone().multiply(cos * radius * 0.12).add(perp2.clone().multiply(sin * radius * 0.12));

            Location spawnLoc = center.clone().add(offset);
            center.getWorld().spawnParticle(Particle.FIREWORK, spawnLoc, 0, vel.getX(), vel.getY(), vel.getZ(), 0.12);
        }
    }

    // -------------------------------------------------------------------------
    // Explosion
    // -------------------------------------------------------------------------

    private void createExplosion(Location center) {
        center.getWorld().spawnParticle(Particle.FLAME, center, 8, 0.6, 0.6, 0.6, 0.3);
        center.getWorld().spawnParticle(Particle.LARGE_SMOKE, center, 6, 0.6, 0.6, 0.6, 0.2);
        center.getWorld().spawnParticle(Particle.FIREWORK, center, 8, 0.6, 0.6, 0.6, 0.3);
        center.getWorld().spawnParticle(Particle.EXPLOSION, center, 2, 0.3, 0.3, 0.3, 0);

        center.getWorld().playSound(center, Sound.ENTITY_GENERIC_EXPLODE, 1.5f, 1.0f);
        scatterBlockDamage(center);

        for (Entity entity : center.getWorld().getNearbyEntities(center, aoeRadius, aoeRadius, aoeRadius)) {
            if (!(entity instanceof LivingEntity living)) continue;
            if (entity.equals(player)) {
                if (!selfBlasted) {
                    selfBlasted = true;
                    player.damage(misfireDamage);
                }
            } else {
                DamageHandler.damageEntity(living, burstDamage, this);
            }
        }
    }

    /**
     * Turns blocks within a small radius of an explosion into temporary air,
     * reverting them after 10 seconds. Skips air, liquids, and indestructible blocks.
     */
    private void scatterBlockDamage(Location center) {
        final double blastRadius = 1.5;
        final long revertMs = 10_000;

        int r = (int) Math.ceil(blastRadius);
        for (int x = -r; x <= r; x++) {
            for (int y = -r; y <= r; y++) {
                for (int z = -r; z <= r; z++) {
                    if (x * x + y * y + z * z > blastRadius * blastRadius) continue;
                    Block block = center.getBlock().getRelative(x, y, z);
                    if (block.getType().isAir() || block.isLiquid()) continue;
                    if (block.getType().getHardness() < 0) continue; // indestructible (bedrock etc.)
                    new TempBlock(block, Material.AIR.createBlockData(), revertMs + ThreadLocalRandom.current().nextInt(500));
                }
            }
        }
    }

    // -------------------------------------------------------------------------
    // Misfire
    // -------------------------------------------------------------------------

    private void triggerMisfire() {
        misfired = true;
        bursts.clear();
        burstsQueued = TOTAL_BURSTS;

        Location misfireCenter = player.getEyeLocation();

        misfireCenter.getWorld().spawnParticle(Particle.FLAME, misfireCenter, 10, 0.8, 0.8, 0.8, 0.3);
        misfireCenter.getWorld().spawnParticle(Particle.LARGE_SMOKE, misfireCenter, 8, 0.8, 0.8, 0.8, 0.2);
        misfireCenter.getWorld().spawnParticle(Particle.FIREWORK, misfireCenter, 10, 0.8, 0.8, 0.8, 0.3);
        misfireCenter.getWorld().spawnParticle(Particle.EXPLOSION, misfireCenter, 3, 0.5, 0.5, 0.5, 0);

        misfireCenter.getWorld().playSound(misfireCenter, Sound.ENTITY_GENERIC_EXPLODE, 1.5f, 0.8f);

        // Damage nearby entities
        for (Entity entity : misfireCenter.getWorld().getNearbyEntities(misfireCenter, aoeRadius, aoeRadius, aoeRadius)) {
            if (entity instanceof LivingEntity living && !entity.equals(player)) {
                DamageHandler.damageEntity(living, misfireDamage, this);
            }
        }

        // Damage the caster (guaranteed, using direct damage to bypass any PK protections)
        player.damage(misfireDamage);

        finishAbility();
    }

    // -------------------------------------------------------------------------
    // Cleanup / end
    // -------------------------------------------------------------------------

    private void finishAbility() {
        cleanup();
        bPlayer.addCooldown(this);
        remove();
    }

    private void cleanup() {
        activeInstances.remove(player.getUniqueId());
        bursts.clear();
    }

    // -------------------------------------------------------------------------
    // Utility
    // -------------------------------------------------------------------------

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

    public static Barrage getActiveInstance(UUID uuid) {
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
        return "Barrage";
    }

    @Override
    public Location getLocation() {
        if (!bursts.isEmpty()) return bursts.get(0).position.clone();
        return player.getLocation();
    }

    @Override
    public void load() {}

    @Override
    public void stop() {
        cleanup();
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
        return "Channel your chi into three consecutive combustive bursts, each detonating on impact.\n" +
                ChatUtils.translateToColor("&fUsage: Tap Sneak");
    }
}
