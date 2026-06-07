package com.aearost.aranarthcore.abilities.earthbending.metalbending;

import com.aearost.aranarthcore.utils.AranarthBendingUtils;
import com.aearost.aranarthcore.utils.ChatUtils;
import com.projectkorra.projectkorra.ability.AddonAbility;
import com.projectkorra.projectkorra.ability.MetalAbility;
import com.projectkorra.projectkorra.attribute.Attribute;
import com.projectkorra.projectkorra.util.DamageHandler;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.UUID;

/**
 * Hurls two metal cables outward and thrashes them vigorously.
 */
public class CableThrash extends MetalAbility implements AddonAbility {

    private static final long CAST_DURATION_MS = 1500L;
    private static final long DAMAGE_INTERVAL_MS = 500L;
    private static final long SOUND_INTERVAL_MS = 450L;
    private static final int CABLE_SEGMENTS = 26;

    private static final Particle.DustOptions CABLE_DUST =
            new Particle.DustOptions(Color.fromRGB(55, 55, 58), 1.0f);
    private static final Particle.DustOptions CABLE_DUST_TIP =
            new Particle.DustOptions(Color.fromRGB(115, 120, 130), 0.85f);
    private static final Map<UUID, CableThrash> ACTIVE_INSTANCES = new HashMap<>();

    @Attribute(Attribute.COOLDOWN)
    private long cooldown;
    @Attribute(Attribute.DAMAGE)
    private double damage;
    @Attribute("Radius")
    private double radius;

    private long castStartTime;
    private long lastDamageTime;
    private long lastSoundTime;
    private long tick;
    private final Map<UUID, LivingEntity> entityInDomeCache = new HashMap<>();
    private final Random random = new Random();

    public CableThrash(final Player player) {
        super(player);

        if (!bPlayer.canBend(this)) {
            return;
        }
        if (!AranarthBendingUtils.hasMetalRequirement(player)) {
            return;
        }
        if (bPlayer.isOnCooldown(this)) {
            return;
        }

        this.cooldown = 7000L;
        this.damage = 2.0;
        this.radius = 4.0;
        this.castStartTime = System.currentTimeMillis();
        this.lastDamageTime = System.currentTimeMillis();
        this.lastSoundTime = System.currentTimeMillis();
        this.tick = 0;

        ACTIVE_INSTANCES.put(player.getUniqueId(), this);
        this.start();

        final Location loc = player.getLocation();
        player.getWorld().playSound(loc, Sound.BLOCK_CHAIN_STEP, 0.9f, 0.75f);
        player.getWorld().playSound(loc, Sound.ENTITY_IRON_GOLEM_ATTACK, 0.6f, 1.3f);
    }

    @Override
    public void progress() {
        if (player.isDead() || !player.isOnline()) {
            remove();
            return;
        }
        if (!player.isSneaking()) {
            endWithCooldown();
            return;
        }

        final long now = System.currentTimeMillis();

        if (now - castStartTime >= CAST_DURATION_MS) {
            endWithCooldown();
            return;
        }

        tick++;

        spawnCableParticles();
        refreshEntityTracking();

        if (now - lastDamageTime >= DAMAGE_INTERVAL_MS) {
            damageEntities();
            lastDamageTime = now;
        }

        if (now - lastSoundTime >= SOUND_INTERVAL_MS) {
            playCableThrashSounds();
            lastSoundTime = now;
        }
    }

    /**
     * Spawns two thrashing cable lines extending outward from the player's position
     */
    private void spawnCableParticles() {
        final double t = tick * 0.17;
        final Location center = player.getLocation().clone().add(0, 1.05, 0);
        final World world = center.getWorld();

        // Cable 1
        final double x1 = Math.sin(t) * 0.65 + Math.sin(t * 1.618) * 0.28 + Math.sin(t * 2.414) * 0.11;
        final double z1 = Math.cos(t * 0.873) * 0.65 + Math.cos(t * 1.732) * 0.28 + Math.cos(t * 2.236) * 0.11;
        final double y1 = Math.sin(t * 1.414 + 0.5) * 0.52 + Math.sin(t * 0.618) * 0.28;
        final Vector dir1 = new Vector(x1, y1, z1).normalize();

        // Cable 2
        final double x2 = Math.sin(t * 0.927 + 2.1) * 0.65 + Math.sin(t * 2.236 + 0.5) * 0.26 + Math.sin(t * 1.571) * 0.13;
        final double z2 = Math.cos(t * 1.118 + 1.3) * 0.65 + Math.cos(t * 2.718 + 0.7) * 0.26 + Math.cos(t * 1.414 + 2.0) * 0.13;
        final double y2 = Math.sin(t * 1.272 + 3.5) * 0.50 + Math.cos(t * 2.059 + 1.0) * 0.28;
        final Vector dir2 = new Vector(x2, y2, z2).normalize();

        spawnCableLine(world, center, dir1, t, 0.0);
        spawnCableLine(world, center, dir2, t, 1.9);
    }

    /**
     * Draws a single cable as a segmented line from origin outward in direction.
     *
     * @param world     The world in which to spawn particles.
     * @param origin    The base location of the cable (near the player's body).
     * @param direction The primary outward direction of the cable for this tick.
     * @param t         The time parameter driving the oscillation animation.
     * @param phase     A per-cable phase offset so each cable animates independently.
     */
    private void spawnCableLine(final World world, final Location origin,
                                final Vector direction, final double t, final double phase) {
        // Clone before crossProduct
        Vector perp = direction.clone().crossProduct(new Vector(0, 1, 0));
        if (perp.lengthSquared() < 0.001) {
            perp = direction.clone().crossProduct(new Vector(1, 0, 0));
        }
        perp.normalize();
        final Vector perp2 = direction.clone().crossProduct(perp).normalize();

        for (int i = 0; i <= CABLE_SEGMENTS; i++) {
            final double s = (double) i / CABLE_SEGMENTS;
            final double dist = s * radius;

            // Two overlapping waves per axis at different speeds, amplitude grows toward the tip
            final double wave1 = (Math.sin(s * Math.PI * 3.1 + t * 4.2 + phase) * 0.55
                                + Math.sin(s * Math.PI * 1.7 + t * 2.6 + phase * 1.4) * 0.25) * s;
            final double wave2 = (Math.cos(s * Math.PI * 2.6 + t * 3.5 + phase) * 0.40
                                + Math.cos(s * Math.PI * 1.3 + t * 1.9 + phase * 0.8) * 0.20) * s;

            final Location loc = origin.clone()
                    .add(direction.clone().multiply(dist))
                    .add(perp.clone().multiply(wave1))
                    .add(perp2.clone().multiply(wave2));

            // Slightly brighter particles near the whipping tips for visual contrast
            final Particle.DustOptions dust = (s > 0.75) ? CABLE_DUST_TIP : CABLE_DUST;
            world.spawnParticle(Particle.DUST, loc, 1, 0, 0, 0, 0, dust);
        }
    }

    /**
     * Rebuilds the cache of living entities currently within the dome radius.
     */
    private void refreshEntityTracking() {
        entityInDomeCache.clear();
        final Location center = player.getLocation().clone().add(0, 1.0, 0);
        final double scanRadius = radius + 1.0;

        for (final Entity entity : player.getNearbyEntities(scanRadius, scanRadius, scanRadius)) {
            if (!(entity instanceof LivingEntity living)) {
                continue;
            }
            if (entity.equals(player)) {
                continue;
            }
            if (entity.getLocation().distance(center) > radius) {
                continue;
            }
            entityInDomeCache.put(entity.getUniqueId(), living);
        }
    }

    /**
     * Deals one interval of cable-whip damage to every entity currently inside the dome.
     */
    private void damageEntities() {
        for (final LivingEntity living : entityInDomeCache.values()) {
            DamageHandler.damageEntity(living, this.damage, this);
            final Location impact = living.getLocation().add(0, living.getHeight() * 0.5, 0);
            impact.getWorld().playSound(impact, Sound.BLOCK_METAL_HIT, 0.75f, 1.6f + random.nextFloat() * 0.5f);
        }
    }

    /**
     * Plays layered metallic sounds at the player's location to simulate cables cutting the air.
     */
    private void playCableThrashSounds() {
        final Location loc = player.getLocation().clone().add(0, 1, 0);
        final World world = player.getWorld();
        world.playSound(loc, Sound.BLOCK_CHAIN_STEP,         0.70f, 0.85f + random.nextFloat() * 0.35f);
        world.playSound(loc, Sound.BLOCK_METAL_HIT,          0.45f, 1.40f + random.nextFloat() * 0.50f);
        world.playSound(loc, Sound.ENTITY_IRON_GOLEM_ATTACK, 0.30f, 1.55f + random.nextFloat() * 0.45f);
    }

    public void endWithCooldown() {
        bPlayer.addCooldown(this);
        remove();
    }

    public void cancelInstantly() {
        remove();
    }

    @Override
    public void remove() {
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

    public static CableThrash getActiveInstance(final UUID uuid) {
        return ACTIVE_INSTANCES.get(uuid);
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
        return "CableThrash";
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
        return "Thrash two metal cables through the air around you, whipping any target caught "
                + "within range for the duration of the cast.\n"
                + ChatUtils.translateToColor("&fUsage: Sneak (Hold)");
    }
}
