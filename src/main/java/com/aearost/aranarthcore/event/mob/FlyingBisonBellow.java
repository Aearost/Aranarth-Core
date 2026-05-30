package com.aearost.aranarthcore.event.mob;

import com.aearost.aranarthcore.AranarthCore;
import com.aearost.aranarthcore.utils.MountUtils;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.*;
import org.bukkit.util.Vector;

import java.util.Random;
import java.util.UUID;

/**
 * Ability to deal low damage and high knockback for the Flying Bison mount.
 */
public class FlyingBisonBellow {

    public static final double AOE_RADIUS = 8.0;
    private static final double MIN_DAMAGE = 4.0;
    private static final double KNOCKBACK_H = 1.8;
    private static final double KNOCKBACK_V = 0.45;
    static final int BASE_XP = 5;
    static final int XP_PER_HIT = 2;

    /**
     * Executes the bellow ability.
     *
     * @param ghast           The Happy Ghast mount.
     * @param rider           The controlling player.
     * @param maxDamage       Max half-heart damage (scaled from Bellow Power skill level).
     * @param mountEntityUUID UUID of the mount entity (for XP tracking).
     */
    public static void trigger(HappyGhast ghast, Player rider, double maxDamage, UUID mountEntityUUID) {
        // Centre of the shockwave is the flying bison's body's mid-point
        Location center = ghast.getLocation().add(0, ghast.getHeight() / 2.0, 0);
        World world = center.getWorld();
        Random random = new Random();

        world.playSound(center, Sound.ENTITY_GHAST_SCREAM, 1.5f, 0.50f);
        world.playSound(center, Sound.ENTITY_GHAST_SHOOT, 0.8f, 0.30f);

        // Apply effects to every living entity in the AOE
        int hits = 0;
        for (Entity nearby : ghast.getNearbyEntities(AOE_RADIUS, AOE_RADIUS, AOE_RADIUS)) {
            if (!(nearby instanceof LivingEntity target)) {
                continue;
            }
            if (target.getUniqueId().equals(ghast.getUniqueId())) {
                continue;
            }
            if (target.getUniqueId().equals(rider.getUniqueId())) {
                continue;
            }

            double dist = nearby.getLocation().distance(center);
            if (dist > AOE_RADIUS) {
                continue;
            }

            // 100% effect at centre but only 50 % at full radius
            double falloff = 1.0 - (dist / AOE_RADIUS) * 0.5;
            target.setVelocity(radialKnockback(
                    target.getLocation(), center, KNOCKBACK_H * falloff, KNOCKBACK_V));

            // Low damage scaled by skill level and falloff
            double damage = MIN_DAMAGE + random.nextDouble() * (maxDamage - MIN_DAMAGE);
            double healthBefore = target.getHealth();
            double absorptionBefore = target.getAbsorptionAmount();
            target.damage(damage * falloff, ghast);
            if (target.isDead() || target.getHealth() < healthBefore || target.getAbsorptionAmount() < absorptionBefore) {
                hits++;
            }
        }

        if (hits > 0) {
            // Base XP per activation + bonus XP per entity hit
            MountUtils.addBellowXp(mountEntityUUID, BASE_XP + hits * XP_PER_HIT);
        }

        spawnExpandingWave(center);
    }

    private static Vector radialKnockback(Location targetLoc, Location origin,
                                          double horizontal, double vertical) {
        Vector dir = targetLoc.toVector().subtract(origin.toVector()).setY(0);
        if (dir.lengthSquared() < 0.001) {
            dir = new Vector(1, 0, 0);
        } else {
            dir.normalize();
        }
        return dir.multiply(horizontal).setY(vertical);
    }

    /**
     * Schedules three particle shells that expand outward like a shockwave.
     */
    private static void spawnExpandingWave(Location center) {
        Bukkit.getScheduler().runTaskLater(AranarthCore.getInstance(),
                () -> spawnShell(center, 2.5), 0L);
        Bukkit.getScheduler().runTaskLater(AranarthCore.getInstance(),
                () -> spawnShell(center, 5.0), 4L);
        Bukkit.getScheduler().runTaskLater(AranarthCore.getInstance(),
                () -> spawnShell(center, 8.0), 8L);
    }

    /**
     * Spawns a single spherical shell of light-gray CLOUD and END_ROD particles at the
     * given radius, using five latitude rings to form a rounded sphere shape.
     */
    private static void spawnShell(Location center, double radius) {
        World world = center.getWorld();
        double[] pitchDegs = {-60, -30, 0, 30, 60};
        for (double pitchDeg : pitchDegs) {
            double pitchRad = Math.toRadians(pitchDeg);
            double y = radius * Math.sin(pitchRad);
            double ringR = radius * Math.cos(pitchRad);
            int points = Math.max(8, (int) (ringR * 7));
            for (int i = 0; i < points; i++) {
                double angle = 2 * Math.PI * i / points;
                double x = ringR * Math.cos(angle);
                double z = ringR * Math.sin(angle);
                Location pl = center.clone().add(x, y, z);
                world.spawnParticle(Particle.CLOUD, pl, 1, 0.1, 0.05, 0.1, 0.008);
                world.spawnParticle(Particle.END_ROD, pl, 1, 0.05, 0.02, 0.05, 0.002);
            }
        }
        // Dense burst at the very centre on the first shell
        if (radius < 3.0) {
            world.spawnParticle(Particle.CLOUD, center, 20, 0.5, 0.4, 0.5, 0.06);
            world.spawnParticle(Particle.END_ROD, center, 8, 0.3, 0.2, 0.3, 0.02);
        }
    }
}
