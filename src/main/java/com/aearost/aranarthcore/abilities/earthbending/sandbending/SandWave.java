package com.aearost.aranarthcore.abilities.earthbending.sandbending;

import com.aearost.aranarthcore.AranarthCore;
import com.aearost.aranarthcore.utils.AranarthBendingUtils;
import com.aearost.aranarthcore.utils.ChatUtils;
import com.projectkorra.projectkorra.ability.AddonAbility;
import com.projectkorra.projectkorra.ability.SandAbility;
import com.projectkorra.projectkorra.attribute.Attribute;
import com.projectkorra.projectkorra.util.DamageHandler;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.Vector;

import org.bukkit.scheduler.BukkitTask;

import java.util.*;

public class SandWave extends SandAbility implements AddonAbility {

    // -------------------------------------------------------------------------
    // Constants
    // -------------------------------------------------------------------------

    /**
     * Height of the flat low sections (start and tail) above the ground.
     */
    private static final double MIN_WAVE_HEIGHT = 0.15;
    /**
     * Maximum height of the wave crest at full buildup.
     */
    private static final double MAX_WAVE_HEIGHT = 1.4;
    /**
     * How far the crest overhangs the base at the end of the crash phase.
     */
    private static final double MAX_LEAN = 1.1;
    /**
     * Length of the back slope behind the face (gives the wave visual body/depth).
     */
    private static final double BACK_SLOPE_LENGTH = 3.5;

    private static final double DEPTH_STEP = 0.28; // back-slope sample step (blocks)
    private static final double WIDTH_STEP = 0.28; // width sample step (blocks)
    private static final double HEIGHT_STEP = 0.22; // vertical fill step on front face (blocks)

    /**
     * Height profile over the full travel distance.
     * Index 0 = start, last index = range. Values 1–8, normalised to [0,1].
     * Shape: gradual rise → peak → fast crash → long flat low tail.
     */
    private static final double[] WAVE_PROFILE =
            {1, 1, 2, 2, 3, 4, 5, 6, 7, 8, 8, 8, 7, 6, 4, 2, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1};

    private static final double PEAK_T = 9.0 / (WAVE_PROFILE.length - 1);
    private static final double CRASH_END_T = 16.0 / (WAVE_PROFILE.length - 1);

    /**
     * Returns a height fraction in [0,1] for normalised travel position t ∈ [0,1].
     */
    private static double waveProfile(double t) {
        double idx = Math.max(0, Math.min(t, 1.0)) * (WAVE_PROFILE.length - 1);
        int lo = (int) idx;
        int hi = Math.min(lo + 1, WAVE_PROFILE.length - 1);
        double val = WAVE_PROFILE[lo] + (WAVE_PROFILE[hi] - WAVE_PROFILE[lo]) * (idx - lo);
        return (val - 1.0) / 7.0; // normalise 1–8 → 0–1
    }

    private static final long WAVE_DURATION = 1500L;
    private static final long SOUND_INTERVAL = 350L;

    // -------------------------------------------------------------------------
    // Configurable attributes
    // -------------------------------------------------------------------------

    @Attribute(Attribute.COOLDOWN)
    private long cooldown;
    @Attribute(Attribute.DAMAGE)
    private double damage;
    @Attribute(Attribute.RANGE)
    private double range;
    @Attribute("Width")
    private double width;
    @Attribute(Attribute.KNOCKBACK)
    private double knockbackForce;

    // -------------------------------------------------------------------------
    // State
    // -------------------------------------------------------------------------

    private Location origin = null;
    private Vector direction = null;
    private Vector perp = null;
    private Particle.DustOptions[] dustPalette;

    private double distanceTraveled;
    private long startTime = 0;
    private long lastSoundTime;

    private final Set<UUID> hitEntities = new HashSet<>();
    private final Random random = new Random();

    private static final Map<UUID, SandWave> activeInstances = new HashMap<>();
    private static final Map<UUID, Block> pendingSources = new HashMap<>();
    private static final Map<UUID, BukkitTask> indicatorTasks = new HashMap<>();

    // -------------------------------------------------------------------------
    // Constructor
    // -------------------------------------------------------------------------

    public SandWave(Player player, Block sourceBlock) {
        super(player);

        if (!bPlayer.canBend(this)) {
            return;
        }

        dustPalette = AranarthBendingUtils.pickSandDustPalette(sourceBlock.getType());

        cooldown = 12000;
        damage = 4.0;
        range = 16.0;
        width = 5.0;
        knockbackForce = 1.5;

        Vector dir = player.getLocation().getDirection();
        dir.setY(0);
        if (dir.lengthSquared() < 0.001) {
            return;
        }
        dir.normalize();
        direction = dir;
        perp = new Vector(-direction.getZ(), 0, direction.getX()).normalize();

        origin = sourceBlock.getLocation().clone().add(0.5, 1.0, 0.5);

        distanceTraveled = 0;
        startTime = System.currentTimeMillis();
        lastSoundTime = startTime - SOUND_INTERVAL;

        activeInstances.put(player.getUniqueId(), this);
        start();
    }

    // -------------------------------------------------------------------------
    // Progress
    // -------------------------------------------------------------------------

    @Override
    public void progress() {
        if (!player.isOnline()) {
            remove();
            return;
        }

        long elapsed = System.currentTimeMillis() - startTime;

        double targetDistance = Math.min((elapsed / (double) WAVE_DURATION) * range, range);
        while (distanceTraveled < targetDistance) {
            double step = Math.min(0.25, targetDistance - distanceTraveled);
            distanceTraveled += step;

            if (isBlocked(distanceTraveled)) {
                bPlayer.addCooldown(this);
                remove();
                return;
            }
        }

        // --- Compute wave height and lean from distance traveled ---
        double t = distanceTraveled / range;
        double waveHeight = MIN_WAVE_HEIGHT + (MAX_WAVE_HEIGHT - MIN_WAVE_HEIGHT) * waveProfile(t);
        // Lean only builds after the peak, reaching MAX_LEAN when the crash ends
        double currentLean;
        if (t <= PEAK_T) {
            currentLean = 0.0;
        } else {
            double crashT = Math.min((t - PEAK_T) / (CRASH_END_T - PEAK_T), 1.0);
            currentLean = MAX_LEAN * Math.sqrt(crashT);
        }

        renderWave(waveHeight, currentLean);

        Location waveFront = getWaveFront();
        checkEntityCollisions(waveFront, waveHeight);

        long now = System.currentTimeMillis();
        if (now - lastSoundTime >= SOUND_INTERVAL) {
            World world = waveFront.getWorld();
            world.playSound(waveFront, Sound.BLOCK_SAND_FALL,
                    0.9f + (float) (waveHeight / MAX_WAVE_HEIGHT) * 0.5f,
                    0.55f + random.nextFloat() * 0.2f);
            world.playSound(waveFront, Sound.ENTITY_BREEZE_IDLE_GROUND,
                    0.5f + (float) (waveHeight / MAX_WAVE_HEIGHT) * 0.4f,
                    0.50f + random.nextFloat() * 0.2f);
            lastSoundTime = now;
        }

        if (distanceTraveled >= range || elapsed >= WAVE_DURATION + 200) {
            bPlayer.addCooldown(this);
            remove();
        }
    }

    // -------------------------------------------------------------------------
    // Obstacle detection
    // -------------------------------------------------------------------------

    /**
     * Stops the wave when the terrain rises by more than 1 block in a half-block
     * span — catches steep walls while allowing gentle uphill slopes.
     */
    private boolean isBlocked(double dist) {
        if (dist < 0.5) {
            return false;
        }
        World world = origin.getWorld();
        int prevBx = (int) (origin.getX() + direction.getX() * (dist - 0.5));
        int prevBz = (int) (origin.getZ() + direction.getZ() * (dist - 0.5));
        int currBx = (int) (origin.getX() + direction.getX() * dist);
        int currBz = (int) (origin.getZ() + direction.getZ() * dist);
        return world.getHighestBlockYAt(currBx, currBz)
                - world.getHighestBlockYAt(prevBx, prevBz) > 1;
    }

    // -------------------------------------------------------------------------
    // Terrain helper
    // -------------------------------------------------------------------------

    private Location getWaveFront() {
        World world = origin.getWorld();
        double fx = origin.getX() + direction.getX() * distanceTraveled;
        double fz = origin.getZ() + direction.getZ() * distanceTraveled;
        int groundY = world.getHighestBlockYAt((int) fx, (int) fz);
        return new Location(world, fx, groundY + 1.0, fz);
    }

    // -------------------------------------------------------------------------
    // Rendering
    // -------------------------------------------------------------------------

    /**
     * Renders the wave as a vertical face plus a back slope.
     */
    private void renderWave(double waveHeight, double currentLean) {
        if (waveHeight < 0.05) {
            return;
        }
        World world = origin.getWorld();
        double halfW = width / 2.0;

        // Back slope
        for (double back = DEPTH_STEP; back <= BACK_SLOPE_LENGTH; back += DEPTH_STEP) {
            double d = distanceTraveled - back;
            if (d < 0) {
                continue;
            }

            double cx = origin.getX() + direction.getX() * d;
            double cz = origin.getZ() + direction.getZ() * d;
            int groundY = world.getHighestBlockYAt((int) cx, (int) cz);

            // Height decreases linearly towards the rear of the slope
            double sliceH = waveHeight * (1.0 - back / BACK_SLOPE_LENGTH);
            if (sliceH < 0.05) {
                continue;
            }

            for (double w = -halfW; w <= halfW; w += WIDTH_STEP) {
                double nw = w / halfW;
                double columnH = sliceH * (1.0 - nw * nw); // parabolic width taper
                if (columnH < 0.05) {
                    continue;
                }

                double px = cx + perp.getX() * w;
                double pz = cz + perp.getZ() * w;

                // Surface only - one particle at the top of the arch curve
                Location loc = new Location(world, px, groundY + 1.0 + columnH, pz);
                if (!loc.getBlock().getType().isSolid()) {
                    world.spawnParticle(Particle.DUST, loc, 1, 0.03, 0.03, 0.03, 0,
                            dustPalette[random.nextInt(dustPalette.length)]);
                }
            }
        }

        // Front face (with crash lean)
        double faceCx = origin.getX() + direction.getX() * distanceTraveled;
        double faceCz = origin.getZ() + direction.getZ() * distanceTraveled;
        int faceGroundY = world.getHighestBlockYAt((int) faceCx, (int) faceCz);

        for (double w = -halfW; w <= halfW; w += WIDTH_STEP) {
            double nw = w / halfW;
            double columnH = waveHeight * (1.0 - nw * nw); // parabolic width taper
            if (columnH < 0.05) {
                continue;
            }

            double baseX = faceCx + perp.getX() * w;
            double baseZ = faceCz + perp.getZ() * w;

            for (double h = 0; h <= columnH; h += HEIGHT_STEP) {
                // Quadratic lean: base stays put, crest tips furthest forward
                double leanOffset = currentLean * Math.pow(h / columnH, 2.0);
                double px = baseX + direction.getX() * leanOffset;
                double pz = baseZ + direction.getZ() * leanOffset;

                Location loc = new Location(world, px, faceGroundY + 1.0 + h, pz);
                if (!loc.getBlock().getType().isSolid()) {
                    world.spawnParticle(Particle.DUST, loc, 1, 0.03, 0.03, 0.03, 0,
                            dustPalette[random.nextInt(dustPalette.length)]);
                }
            }
        }
    }

    // -------------------------------------------------------------------------
    // Entity collision
    // -------------------------------------------------------------------------

    private void checkEntityCollisions(Location waveFront, double waveHeight) {
        double hitWidth = width / 2.0 + 0.5;
        double hitHeight = waveHeight + 0.5;
        double hitDepth = 1.0 + MAX_LEAN; // account for leaning crest

        for (Entity entity : waveFront.getWorld().getNearbyEntities(
                waveFront, hitWidth + 1, hitHeight, hitDepth + 1)) {

            if (!(entity instanceof LivingEntity living)) {
                continue;
            }
            if (entity.equals(player)) {
                continue;
            }
            if (hitEntities.contains(entity.getUniqueId())) {
                continue;
            }

            Location eLoc = entity.getLocation().add(0, 1, 0);
            Vector toEntity = eLoc.toVector().subtract(waveFront.toVector());

            double forwardDist = toEntity.dot(direction);
            double perpDist = Math.abs(toEntity.dot(perp));
            double heightDiff = eLoc.getY() - waveFront.getY();

            // Allow hit both at the base and under the leaning crest
            if (forwardDist < -hitDepth || forwardDist > hitDepth) {
                continue;
            }
            if (perpDist > hitWidth) {
                continue;
            }
            if (heightDiff < -0.5 || heightDiff > hitHeight) {
                continue;
            }

            hitEntities.add(entity.getUniqueId());

            DamageHandler.damageEntity(living, damage, this);
            living.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, 40, 0, false, true));

            Vector knockback = direction.clone().normalize().multiply(knockbackForce);
            knockback.setY(0.4);
            living.setVelocity(knockback);
        }
    }

    // -------------------------------------------------------------------------
    // Static helpers
    // -------------------------------------------------------------------------

    public static boolean hasActiveInstance(UUID uuid) {
        return activeInstances.containsKey(uuid);
    }

    public static SandWave getActiveInstance(UUID uuid) {
        return activeInstances.get(uuid);
    }

    public static void setPendingSource(UUID uuid, Block block) {
        clearPendingSource(uuid);
        pendingSources.put(uuid, block);

        Particle.DustOptions indicator = AranarthBendingUtils.pickSandDustPalette(block.getType())[0];
        BukkitTask task = Bukkit.getScheduler().runTaskTimer(AranarthCore.getInstance(), () ->
                        block.getWorld().spawnParticle(Particle.DUST,
                                block.getLocation().clone().add(0.5, 1.05, 0.5),
                                3, 0.25, 0.05, 0.25, 0,
                                indicator),
                0L, 1L);
        indicatorTasks.put(uuid, task);
    }

    public static Block getPendingSource(UUID uuid) {
        return pendingSources.get(uuid);
    }

    public static boolean hasPendingSource(UUID uuid) {
        return pendingSources.containsKey(uuid);
    }

    public static void clearPendingSource(UUID uuid) {
        pendingSources.remove(uuid);
        BukkitTask task = indicatorTasks.remove(uuid);
        if (task != null) {
            task.cancel();
        }
    }

    @Override
    public void remove() {
        super.remove();
        activeInstances.remove(player.getUniqueId());
    }

    // -------------------------------------------------------------------------
    // Required overrides
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
    public Location getLocation() {
        return origin;
    }

    @Override
    public String getName() {
        return "SandWave";
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
        return "Create a wave of sand that surges forward, blinding and knocking back hit targets.\n" +
                ChatUtils.translateToColor("&fUsage: Tap Sneak (sand source) > Left-click");
    }
}
