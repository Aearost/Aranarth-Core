package com.aearost.aranarthcore.abilities.firebending.combustion;

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

import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

public class CombustionStrike extends CombustionAbility implements AddonAbility {

    public enum Phase {CHARGING, CHARGED, TRAVELING}

    @Attribute(Attribute.COOLDOWN)
    private long cooldown;
    @Attribute(Attribute.RANGE)
    private double range;
    @Attribute(Attribute.DAMAGE)
    private double damage;
    @Attribute("ChargeDuration")
    private long chargeDuration;
    @Attribute("MisfireDamage")
    private double misfireDamage;
    @Attribute("ExplosionRadius")
    private double explosionRadius;

    private Phase phase;
    private long chargeStartTime;
    private double chargeRingAngle;
    private double healthAtCharge;

    // Traveling projectile state
    private Location strikePosition;
    private Vector strikeDirection;
    private double distanceTraveled;
    private double nextSoundDistance;

    // 100 blocks / 2 seconds / 20 ticks per second = 2.5 blocks per tick
    private static final double SPEED = 2.5;
    private static final double STEP = 0.4;
    private static final double HIT_RADIUS = 0.8;
    private static final double RING_ROTATION_PER_TICK = 2.0 * Math.PI / 50.0;
    private static final double RING_RADIUS = 1.5;
    // Radius within which nearby entities take damage from the impact explosion
    private static final double AOE_ENTITY_RADIUS = 3.0;

    private static final Map<UUID, CombustionStrike> activeInstances = new HashMap<>();

    public CombustionStrike(Player player) {
        super(player);

        if (!bPlayer.canBend(this)) {
            return;
        }

        cooldown = 14000;
        range = 100.0;
        damage = 20.0;        // 10 hearts
        chargeDuration = 5000;
        misfireDamage = 8.0;  // 4 hearts
        explosionRadius = 3.5;

        phase = Phase.CHARGING;
        chargeStartTime = System.currentTimeMillis();
        chargeRingAngle = 0.0;
        healthAtCharge = player.getHealth();

        activeInstances.put(player.getUniqueId(), this);
        start();
    }

    @Override
    public void progress() {
        if (player.isDead() || !player.isOnline()) {
            cleanup();
            remove();
            return;
        }

        switch (phase) {
            case CHARGING -> progressCharging();
            case CHARGED -> progressCharged();
            case TRAVELING -> progressTraveling();
        }
    }

    private void progressCharging() {
        if (!player.isSneaking()) {
            // Released sneak before fully charged
            cleanup();
            remove();
            return;
        }

        // Punish damage taken while building up the charge
        if (player.getHealth() + 0.5 < healthAtCharge) {
            triggerMisfire();
            return;
        }

        chargeRingAngle += RING_ROTATION_PER_TICK;
        spawnChargeRing();

        // Transition once the full charge duration has elapsed
        if (System.currentTimeMillis() - chargeStartTime >= chargeDuration) {
            phase = Phase.CHARGED;
        }
    }

    private void progressCharged() {
        // Punish damage taken while holding a full charge
        if (player.getHealth() + 0.5 < healthAtCharge) {
            triggerMisfire();
            return;
        }

        // Continue the ring animation and overlay smoke in the player's view
        chargeRingAngle += RING_ROTATION_PER_TICK;
        spawnChargeRing();
        spawnReadyVisionSmoke();

        if (!player.isSneaking()) {
            launchStrike();
        }
    }

    private void progressTraveling() {
        double targetDistance = distanceTraveled + SPEED;

        while (distanceTraveled < targetDistance) {
            double step = Math.min(STEP, targetDistance - distanceTraveled);
            strikePosition.add(strikeDirection.clone().multiply(step));
            distanceTraveled += step;

            // Range exceeded without hitting anything
            if (distanceTraveled > range) {
                finishAbility();
                return;
            }

            Block block = strikePosition.getBlock();

            // Solid block or liquid
            if (block.getType().isSolid() || block.isLiquid()) {
                createExplosion(strikePosition.clone());
                finishAbility();
                return;
            }

            // Center the explosion on the entity's body
            LivingEntity hit = findNearbyLivingEntity(strikePosition, HIT_RADIUS);
            if (hit != null) {
                createExplosion(hit.getLocation().add(0, hit.getHeight() / 2.0, 0));
                finishAbility();
                return;
            }

            // Trail particles every other step
            if (((int) (distanceTraveled / STEP)) % 2 == 0) {
                strikePosition.getWorld().spawnParticle(Particle.LARGE_SMOKE, strikePosition.clone(), 1, 0, 0, 0, 0.04, null, true);
                strikePosition.getWorld().spawnParticle(Particle.FIREWORK, strikePosition.clone(), 1, 0, 0, 0, 0.04, null, true);
            }

            // Firework burst ring + sound every 20 blocks
            if (distanceTraveled >= nextSoundDistance) {
                strikePosition.getWorld().playSound(strikePosition, Sound.ENTITY_FIREWORK_ROCKET_BLAST, 1.5f, 0.01f);
                nextSoundDistance = distanceTraveled + 20.0;
                spawnBurstRing(strikePosition.clone(), strikeDirection);
            }
        }
    }

    private void launchStrike() {
        Location origin = player.getEyeLocation();
        strikePosition = origin.clone();
        strikeDirection = origin.getDirection().normalize();
        distanceTraveled = 0.0;
        nextSoundDistance = 20.0;

        origin.getWorld().playSound(origin, Sound.ENTITY_FIREWORK_ROCKET_BLAST, 2.5f, 0.01f);
        origin.getWorld().playSound(origin, Sound.ENTITY_WITHER_SHOOT, 1.5f, 1.8f);
        spawnBurstRing(origin.clone(), strikeDirection);

        phase = Phase.TRAVELING;
    }

    private void spawnChargeRing() {
        Location center = player.getLocation().add(0, 1.0, 0);
        double x = RING_RADIUS * Math.cos(chargeRingAngle);
        double z = RING_RADIUS * Math.sin(chargeRingAngle);
        Location head = center.clone().add(x, 0, z);

        center.getWorld().spawnParticle(Particle.FLAME, head, 3, 0.04, 0.04, 0.04, 0.01, null, true);
        center.getWorld().spawnParticle(Particle.FIREWORK, head, 1, 0, 0, 0, 0.02, null, true);
    }

    private void spawnReadyVisionSmoke() {
        Location eye = player.getEyeLocation();
        Vector forward = eye.getDirection().normalize();
        Location loc = eye.clone().add(forward.multiply(0.6));
        eye.getWorld().spawnParticle(Particle.SMOKE, loc, 3, 0.15, 0.10, 0.15, 0.01, null, true);
    }

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

            Vector offset = perp1.clone().multiply(cos * 0.2).add(perp2.clone().multiply(sin * 0.2));
            Vector vel = perp1.clone().multiply(cos * radius * 0.12).add(perp2.clone().multiply(sin * radius * 0.12));

            Location spawnLoc = center.clone().add(offset);
            center.getWorld().spawnParticle(Particle.FIREWORK, spawnLoc, 0, vel.getX(), vel.getY(), vel.getZ(), 0.12, null, true);
        }
    }

    private void createExplosion(Location center) {
        center.getWorld().spawnParticle(Particle.FLAME, center, 16, 1.0, 1.0, 1.0, 0.4, null, true);
        center.getWorld().spawnParticle(Particle.LARGE_SMOKE, center, 12, 1.0, 1.0, 1.0, 0.3, null, true);
        center.getWorld().spawnParticle(Particle.FIREWORK, center, 16, 1.0, 1.0, 1.0, 0.4, null, true);
        center.getWorld().spawnParticle(Particle.EXPLOSION, center, 4, 0.5, 0.5, 0.5, 0.0, null, true);

        center.getWorld().playSound(center, Sound.ENTITY_GENERIC_EXPLODE, 2.0f, 0.9f);
        scatterBlockDamage(center);

        for (Entity entity : center.getWorld().getNearbyEntities(center, AOE_ENTITY_RADIUS, AOE_ENTITY_RADIUS, AOE_ENTITY_RADIUS)) {
            if (!(entity instanceof LivingEntity living)) {
                continue;
            }
            if (entity.equals(player)) {
                // Caster caught in their own blast takes full damage
                player.damage(misfireDamage);
            } else {
                DamageHandler.damageEntity(living, damage, this);
            }
        }
    }

    private void scatterBlockDamage(Location center) {
        final long revertMs = 10_000;
        int r = (int) Math.ceil(explosionRadius);

        for (int x = -r; x <= r; x++) {
            for (int y = -r; y <= r; y++) {
                for (int z = -r; z <= r; z++) {
                    if (x * x + y * y + z * z > explosionRadius * explosionRadius) {
                        continue;
                    }
                    Block block = center.getBlock().getRelative(x, y, z);
                    if (block.getType().isAir() || block.isLiquid()) {
                        continue;
                    }
                    if (block.getType().getHardness() < 0) {
                        continue;
                    }
                    new TempBlock(block, Material.AIR.createBlockData(), revertMs + ThreadLocalRandom.current().nextInt(500));
                }
            }
        }

        // Place fire on the bottom surface of the cleared sphere
        for (int x = -r; x <= r; x++) {
            for (int z = -r; z <= r; z++) {
                for (int y = -r; y <= r; y++) {
                    if (x * x + y * y + z * z > explosionRadius * explosionRadius) {
                        continue;
                    }
                    // This is the lowest y inside the sphere for this (x, z) column
                    Block below = center.getBlock().getRelative(x, y - 1, z);
                    if (below.getType().isSolid()) {
                        if (new Random().nextInt(3) == 0) {
                            Block firePos = center.getBlock().getRelative(x, y, z);
                            new TempBlock(firePos, Material.FIRE.createBlockData(), revertMs);
                        }
                    }
                    break;
                }
            }
        }
    }

    private void triggerMisfire() {
        player.damage(misfireDamage);
        finishAbility();
    }

    private void finishAbility() {
        cleanup();
        bPlayer.addCooldown(this);
        remove();
    }

    private void cleanup() {
        activeInstances.remove(player.getUniqueId());
    }

    public void cancelInstantly() {
        cleanup();
        remove();
    }

    private LivingEntity findNearbyLivingEntity(Location loc, double radius) {
        for (Entity entity : loc.getWorld().getNearbyEntities(loc, radius, radius, radius)) {
            if (entity instanceof LivingEntity target && !target.equals(player)) {
                return target;
            }
        }
        return null;
    }

    public static boolean hasActiveInstance(UUID uuid) {
        return activeInstances.containsKey(uuid);
    }

    public static CombustionStrike getActiveInstance(UUID uuid) {
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
    public String getName() {
        return "CombustionStrike";
    }

    @Override
    public Location getLocation() {
        if (strikePosition != null) {
            return strikePosition.clone();
        }
        return player.getLocation();
    }

    @Override
    public void load() {
    }

    @Override
    public void stop() {
        if (player != null) {
            cleanup();
        }
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
        return "Channel your combustion into a single devastating strike. Hold sneak to charge, " +
                "and release to fire a straight bolt that detonates in a massive explosion on impact.\n" +
                ChatUtils.translateToColor("&fUsage: Sneak (Hold until smoke) > Sneak (Release)");
    }
}
