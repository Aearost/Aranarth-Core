package com.aearost.aranarthcore.abilities.waterbending.healing;

import com.aearost.aranarthcore.AranarthCore;
import com.aearost.aranarthcore.utils.ChatUtils;
import org.bukkit.attribute.AttributeInstance;
import com.projectkorra.projectkorra.BendingPlayer;
import com.projectkorra.projectkorra.ability.AddonAbility;
import com.projectkorra.projectkorra.ability.HealingAbility;
import com.projectkorra.projectkorra.attribute.Attribute;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Channels the healing essence of water around a target as a spiraling double helix.
 * The player must first left-click a water, ice, or snow source within range to select it,
 * then hold sneak while looking at a target to begin casting.
 * The water phase (0–4 s) restores health every 0.5 s; the golden phase (4–8 s) grants
 * absorption every 0.5 s. A full cast also clears all negative effects from the target.
 * Releasing sneak early applies the cooldown and disperses the particles outward.
 */
public class HealingHelix extends HealingAbility implements AddonAbility {

    public enum Phase { WATER, GOLDEN }

    private static final long   WATER_DURATION_MS  = 4000L;
    private static final long   GOLDEN_DURATION_MS = 4000L;
    private static final long   HEAL_INTERVAL_MS   = 500L;
    private static final double HEAL_PER_TICK      = 1.0;   // 0.5 hearts = 1 HP
    private static final double MAX_ABSORB              = 8.0;  // 4 hearts total
    private static final long   ABSORPTION_EXPIRY_TICKS = 300L; // 15 seconds
    private static final double HELIX_RADIUS       = 0.6;
    private static final double TARGET_HIT_RADIUS  = 0.6;
    private static final double SOURCE_RANGE       = 5.0;

    private static final Set<Material> VALID_SOURCES = Set.of(
            Material.WATER,
            Material.ICE,
            Material.PACKED_ICE,
            Material.BLUE_ICE,
            Material.SNOW,
            Material.SNOW_BLOCK,
            Material.POWDER_SNOW
    );

    private static final Set<PotionEffectType> NEGATIVE_EFFECTS = Set.of(
            PotionEffectType.BLINDNESS,
            PotionEffectType.DARKNESS,
            PotionEffectType.HUNGER,
            PotionEffectType.LEVITATION,
            PotionEffectType.MINING_FATIGUE,
            PotionEffectType.NAUSEA,
            PotionEffectType.POISON,
            PotionEffectType.SLOWNESS,
            PotionEffectType.WEAKNESS,
            PotionEffectType.WITHER,
            PotionEffectType.BAD_OMEN
    );

    private static final Particle.DustOptions WATER_DUST       = new Particle.DustOptions(Color.fromRGB(40,  120, 255), 1.0f);
    private static final Particle.DustOptions WATER_DUST_DARK  = new Particle.DustOptions(Color.fromRGB(20,   75, 225), 0.8f);
    private static final Particle.DustOptions GOLDEN_DUST      = new Particle.DustOptions(Color.fromRGB(255, 215,  50), 1.1f);
    private static final Particle.DustOptions GOLDEN_DUST_PALE = new Particle.DustOptions(Color.fromRGB(255, 245, 130), 0.8f);

    private static final Map<UUID, HealingHelix>  ACTIVE_INSTANCES      = new HashMap<>();
    private static final Map<UUID, Block>         PENDING_SOURCES       = new HashMap<>();
    private static final Map<UUID, BukkitRunnable> PENDING_SOURCE_TASKS = new HashMap<>();

    @Attribute(Attribute.COOLDOWN)
    private long cooldown;
    @Attribute(Attribute.RANGE)
    private double range;
    @Attribute("WaterDuration")
    private long waterDuration;
    @Attribute("GoldenDuration")
    private long goldenDuration;

    private Phase phase;
    private LivingEntity target;
    private long phaseStart;
    private long lastHealTime;
    private double absorptionGranted;

    public HealingHelix(final Player player) {
        super(player);

        if (!bPlayer.canBend(this)) {
            return;
        }

        if (!PENDING_SOURCES.containsKey(player.getUniqueId())) {
            return;
        }
        clearPendingSource(player.getUniqueId());

        this.cooldown       = 15000L;
        this.range          = 15.0;
        this.waterDuration  = WATER_DURATION_MS;
        this.goldenDuration = GOLDEN_DURATION_MS;

        LivingEntity found = findTarget();
        if (found == null) {
            return;
        }

        this.target                = found;
        this.phase                 = Phase.WATER;
        this.phaseStart            = System.currentTimeMillis();
        this.lastHealTime          = System.currentTimeMillis();
        this.absorptionGranted     = 0.0;

        ACTIVE_INSTANCES.put(player.getUniqueId(), this);
        start();
    }

    @Override
    public void progress() {
        if (player.isDead() || !player.isOnline()) {
            remove();
            return;
        }

        if (!bPlayer.canBend(this)) {
            remove();
            return;
        }

        if (!target.isValid()) {
            finishWithCooldown(false);
            return;
        }

        if (!player.isSneaking()) {
            finishWithCooldown(false);
            return;
        }

        switch (phase) {
            case WATER  -> progressWater();
            case GOLDEN -> progressGolden();
        }
    }

    private void progressWater() {
        long elapsed = System.currentTimeMillis() - phaseStart;
        if (elapsed >= waterDuration) {
            beginGoldenPhase();
            return;
        }

        double progress = (double) elapsed / waterDuration;
        spawnWaterParticles(0.0, progress);

        if (Math.random() < 0.08) {
            target.getWorld().playSound(target.getLocation(), Sound.BLOCK_WATER_AMBIENT, 0.2f, 0.7f);
        }

        if (System.currentTimeMillis() - lastHealTime >= HEAL_INTERVAL_MS) {
            applyHeal();
            lastHealTime = System.currentTimeMillis();
        }
    }

    private void beginGoldenPhase() {
        phase        = Phase.GOLDEN;
        phaseStart   = System.currentTimeMillis();
        lastHealTime = System.currentTimeMillis();
        player.getWorld().playSound(target.getLocation(), Sound.BLOCK_AMETHYST_CLUSTER_BREAK, 0.5f, 1.4f);
    }

    private void progressGolden() {
        long elapsed = System.currentTimeMillis() - phaseStart;
        if (elapsed >= goldenDuration) {
            // Fill any remaining absorption so a full cast always delivers the full amount.
            double remaining = MAX_ABSORB - absorptionGranted;
            if (remaining > 0) {
                grantAbsorption(remaining);
            }
            scheduleAbsorptionExpiry();
            finishWithCooldown(true);
            return;
        }

        double progress = (double) elapsed / goldenDuration;
        // Below the transformation front: golden; above it: still water-coloured.
        spawnGoldenParticles(progress);
        spawnWaterParticles(progress, 1.0);

        if (Math.random() < 0.08) {
            target.getWorld().playSound(target.getLocation(), Sound.BLOCK_WATER_AMBIENT, 0.2f, 0.7f);
        }

        if (absorptionGranted < MAX_ABSORB && System.currentTimeMillis() - lastHealTime >= HEAL_INTERVAL_MS) {
            grantAbsorption(1.0);
            lastHealTime = System.currentTimeMillis();
        }
    }

    /**
     * Grants {@code amount} HP of absorption by raising the target's MAX_ABSORPTION attribute
     * base value so the call to setAbsorptionAmount isn't clamped to zero (MC 1.21+ clamps
     * absorptionAmount to the max_absorption attribute, which has a default base of 0).
     * Stacks on top of any existing absorption from other sources.
     */
    private void grantAbsorption(double amount) {
        if (!target.isValid()) return;
        AttributeInstance attr = target.getAttribute(org.bukkit.attribute.Attribute.MAX_ABSORPTION);
        if (attr != null) {
            attr.setBaseValue(attr.getBaseValue() + amount);
        }
        target.setAbsorptionAmount(target.getAbsorptionAmount() + amount);
        absorptionGranted += amount;
    }

    /**
     * Schedules a task to remove the absorption we granted after 15 seconds. Reverting both the
     * attribute base value and the current absorption amount preserves any absorption the target
     * had from other sources while correctly expiring our contribution.
     */
    private void scheduleAbsorptionExpiry() {
        if (absorptionGranted <= 0) return;
        final LivingEntity expiryTarget = target;
        final double toRemove = absorptionGranted;
        new BukkitRunnable() {
            @Override
            public void run() {
                if (expiryTarget.isValid()) {
                    AttributeInstance attr = expiryTarget.getAttribute(org.bukkit.attribute.Attribute.MAX_ABSORPTION);
                    if (attr != null) {
                        attr.setBaseValue(Math.max(0.0, attr.getBaseValue() - toRemove));
                    }
                    expiryTarget.setAbsorptionAmount(Math.max(0.0, expiryTarget.getAbsorptionAmount() - toRemove));
                }
            }
        }.runTaskLater(AranarthCore.getInstance(), ABSORPTION_EXPIRY_TICKS);
    }

    private void applyHeal() {
        if (!target.isValid()) return;
        double healed = Math.min(target.getHealth() + HEAL_PER_TICK, target.getMaxHealth());
        if (target.getHealth() < target.getMaxHealth()) {
            target.setHealth(healed);
        }
    }

    /**
     * Removes all negative potion effects from the target as a reward for a full cast.
     */
    private void clearNegativeEffects() {
        if (!target.isValid()) return;
        for (PotionEffectType type : NEGATIVE_EFFECTS) {
            target.removePotionEffect(type);
        }
    }

    /**
     * Spawns the dark-blue double helix from the target's feet up to {@code topFraction}
     * of their full height. The helix completes 1 full rotation (2π) across the total height.
     *
     * @param bottomFraction Fraction of height at which the visible band starts (0 = feet).
     * @param topFraction    Fraction of height at which the visible band ends (1 = full height).
     */
    private void spawnWaterParticles(double bottomFraction, double topFraction) {
        if (topFraction <= bottomFraction) return;
        Location base = target.getLocation();
        double totalHeight = target.getHeight();
        double bandBottom  = bottomFraction * totalHeight;
        double bandTop     = topFraction * totalHeight;
        double bandHeight  = bandTop - bandBottom;
        if (bandHeight <= 0) return;

        double time = System.currentTimeMillis() / 2000.0;
        int pointsPerBlock = 14;
        int totalPoints = Math.max(2, (int) (bandHeight * pointsPerBlock));
        int denom = Math.max(1, totalPoints - 1);

        for (int strand = 0; strand < 2; strand++) {
            double strandOffset = strand * Math.PI;
            for (int i = 0; i < totalPoints; i++) {
                double t     = (double) i / denom;
                double y     = bandBottom + t * bandHeight;
                double angle = (y / totalHeight) * 2.0 * Math.PI + strandOffset + time;
                double x     = HELIX_RADIUS * Math.cos(angle);
                double z     = HELIX_RADIUS * Math.sin(angle);

                Location loc = base.clone().add(x, y, z);
                Particle.DustOptions dust = (i % 3 == 0) ? WATER_DUST_DARK : WATER_DUST;
                loc.getWorld().spawnParticle(Particle.DUST, loc, 1, 0, 0, 0, 0, dust);

                if (Math.random() < 0.08) {
                    loc.getWorld().spawnParticle(Particle.SPLASH, loc, 1, 0.04, 0.04, 0.04, 0.01);
                }
            }
        }
    }

    /**
     * Spawns the golden double helix from the target's feet up to {@code progress} of their
     * full height, representing the transformation front rising during the golden phase.
     *
     * @param progress Fraction [0, 1] of the target's height transformed to golden.
     */
    private void spawnGoldenParticles(double progress) {
        if (progress <= 0) return;
        Location base = target.getLocation();
        double totalHeight  = target.getHeight();
        double revealHeight = progress * totalHeight;

        double time = System.currentTimeMillis() / 2000.0;
        int pointsPerBlock = 14;
        int totalPoints = Math.max(2, (int) (revealHeight * pointsPerBlock));
        int denom = Math.max(1, totalPoints - 1);

        for (int strand = 0; strand < 2; strand++) {
            double strandOffset = strand * Math.PI;
            for (int i = 0; i < totalPoints; i++) {
                double t     = (double) i / denom;
                double y     = t * revealHeight;
                double angle = (y / totalHeight) * 2.0 * Math.PI + strandOffset + time;
                double x     = HELIX_RADIUS * Math.cos(angle);
                double z     = HELIX_RADIUS * Math.sin(angle);

                Location loc = base.clone().add(x, y, z);
                Particle.DustOptions dust = (i % 4 == 0) ? GOLDEN_DUST_PALE : GOLDEN_DUST;
                loc.getWorld().spawnParticle(Particle.DUST, loc, 1, 0, 0, 0, 0, dust);

                if (Math.random() < 0.06) {
                    loc.getWorld().spawnParticle(Particle.END_ROD, loc, 1, 0.04, 0.04, 0.04, 0.01);
                }
            }
        }
    }

    /**
     * Schedules a brief outward-dissipation burst after the ability ends.
     *
     * @param endPhase    Phase the ability was in when it ended.
     * @param endProgress Fraction [0, 1] of the phase that was completed.
     * @param base        Cloned base location of the target's feet.
     * @param totalHeight Height of the target entity.
     */
    private void scheduleDissipation(final Phase endPhase, final double endProgress,
                                     final Location base, final double totalHeight) {
        final int[] iteration = {0};
        final int maxIterations = 5;

        new BukkitRunnable() {
            @Override
            public void run() {
                if (iteration[0] >= maxIterations) {
                    cancel();
                    return;
                }

                double t      = (iteration[0] + 1.0) / maxIterations;
                double radius = HELIX_RADIUS + t * 2.0;
                int count     = Math.max(1, (int) (10 * (1.0 - t * 0.7)));

                Particle.DustOptions dust = (endPhase == Phase.GOLDEN) ? GOLDEN_DUST : WATER_DUST;

                for (int i = 0; i < count; i++) {
                    double angle   = Math.random() * 2.0 * Math.PI;
                    double yOffset = Math.random() * totalHeight * endProgress;
                    double x = radius * Math.cos(angle);
                    double z = radius * Math.sin(angle);
                    Location loc = base.clone().add(x, yOffset, z);
                    loc.getWorld().spawnParticle(Particle.DUST, loc, 1, 0.1, 0.1, 0.1, 0, dust);
                }

                if (endPhase == Phase.WATER) {
                    double angle = Math.random() * 2.0 * Math.PI;
                    double yOff  = Math.random() * totalHeight * endProgress * 0.5;
                    base.getWorld().spawnParticle(Particle.SPLASH,
                            base.clone().add(radius * Math.cos(angle), yOff, radius * Math.sin(angle)),
                            2, 0.1, 0.1, 0.1, 0.04);
                }

                iteration[0]++;
            }
        }.runTaskTimer(AranarthCore.getInstance(), 0L, 3L);
    }

    /**
     * Performs a ray trace from the caster's eye to find the nearest valid living entity
     * within range that the caster is looking at.
     */
    private LivingEntity findTarget() {
        Location eye     = player.getEyeLocation();
        Vector direction = eye.getDirection().normalize();
        LivingEntity closest   = null;
        double closestDist = Double.MAX_VALUE;

        for (Entity entity : player.getNearbyEntities(range, range, range)) {
            if (!(entity instanceof LivingEntity living)) continue;
            if (entity.equals(player)) continue;
            if (!entity.isValid()) continue;

            double dist = entity.getLocation().distance(eye);
            if (dist > range) continue;

            if (entity.getBoundingBox().expand(TARGET_HIT_RADIUS)
                    .rayTrace(eye.toVector(), direction, range) == null) {
                continue;
            }

            if (dist < closestDist) {
                closestDist = dist;
                closest = living;
            }
        }
        return closest;
    }

    /**
     * @param completed True when the golden phase ran to its natural end (full 8-second cast).
     *                  A completed cast also clears all negative effects from the target.
     */
    private void finishWithCooldown(boolean completed) {
        long elapsed       = System.currentTimeMillis() - phaseStart;
        long phaseDuration = (phase == Phase.WATER) ? waterDuration : goldenDuration;
        double endProgress = Math.min(1.0, (double) elapsed / phaseDuration);
        if (target.isValid()) {
            if (completed) {
                clearNegativeEffects();
            }
            // Schedule expiry for any absorption granted so far if not already scheduled
            // (completion schedules before calling here; early cancellation does not)
            if (!completed) {
                scheduleAbsorptionExpiry();
            }
            scheduleDissipation(phase, endProgress, target.getLocation().clone(), target.getHeight());
        }
        bPlayer.addCooldown(this);
        remove();
    }

    /** Called by {@link com.aearost.aranarthcore.event.listener.grouped.AranarthCoreBendingListener} on slot change. */
    public void endWithCooldown() {
        finishWithCooldown(false);
    }

    /**
     * Attempts to register {@code block} as the pending water source for the player.
     * The block must be a valid water, ice, or snow material and within {@value SOURCE_RANGE} blocks.
     * A repeating smoke particle task runs at the source until the source is consumed or cleared.
     */
    public static void trySelectSource(final Player player, final Block block) {
        if (!VALID_SOURCES.contains(block.getType())) {
            return;
        }
        if (!block.getWorld().equals(player.getWorld())) {
            return;
        }
        if (block.getLocation().distance(player.getLocation()) > SOURCE_RANGE) {
            return;
        }
        BendingPlayer bp = BendingPlayer.getBendingPlayer(player);
        if (bp != null && bp.isOnCooldown("HealingHelix")) {
            return;
        }

        // Cancel any task from a previously-selected source.
        clearPendingSource(player.getUniqueId());

        PENDING_SOURCES.put(player.getUniqueId(), block);

        final Location smokeLoc = block.getLocation().clone().add(0.5, 0.5, 0.5);
        final BukkitRunnable smokeTask = new BukkitRunnable() {
            @Override
            public void run() {
                if (!PENDING_SOURCES.containsKey(player.getUniqueId())) {
                    cancel();
                    return;
                }
                block.getWorld().spawnParticle(Particle.SMOKE, smokeLoc, 4, 0, 0, 0, 0, null, true);
            }
        };
        smokeTask.runTaskTimer(AranarthCore.getInstance(), 0L, 1L);
        PENDING_SOURCE_TASKS.put(player.getUniqueId(), smokeTask);
    }

    /**
     * Clears any absorption hearts granted by HealingHelix that survived a server shutdown
     * (the scheduled expiry task never ran). If the player has absorption amount but no active
     * ABSORPTION potion effect, both the amount and the MAX_ABSORPTION attribute base value are
     * reset so the hearts don't linger indefinitely.
     */
    public static void cleanupOrphanedAbsorption(final Player player) {
        if (player.getAbsorptionAmount() <= 0) return;
        if (player.hasPotionEffect(PotionEffectType.ABSORPTION)) return;
        AttributeInstance attr = player.getAttribute(org.bukkit.attribute.Attribute.MAX_ABSORPTION);
        if (attr != null) {
            attr.setBaseValue(0);
        }
        player.setAbsorptionAmount(0);
    }

    public static boolean hasPendingSource(final UUID uuid) {
        return PENDING_SOURCES.containsKey(uuid);
    }

    public static void clearPendingSource(final UUID uuid) {
        PENDING_SOURCES.remove(uuid);
        final BukkitRunnable task = PENDING_SOURCE_TASKS.remove(uuid);
        if (task != null) {
            task.cancel();
        }
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

    public static boolean hasActiveInstance(final UUID uuid) {
        return ACTIVE_INSTANCES.containsKey(uuid);
    }

    public static HealingHelix getActiveInstance(final UUID uuid) {
        return ACTIVE_INSTANCES.get(uuid);
    }

    @Override
    public boolean isSneakAbility() {
        return true;
    }

    @Override
    public boolean isHarmlessAbility() {
        return true;
    }

    @Override
    public long getCooldown() {
        return cooldown;
    }

    @Override
    public Location getLocation() {
        return target != null && target.isValid() ? target.getLocation() : player.getLocation();
    }

    @Override
    public String getName() {
        return "HealingHelix";
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
        return "Channel the restorative power of water around a target, weaving a spiraling double helix that rises and transforms into golden energy. " +
                "The water phase restores up to 4 hearts of health, and the golden phase grants up to 4 absorption hearts. " +
                "A full cast also cleanses all negative effects from the target.\n" +
                ChatUtils.translateToColor("&fUsage: Left-click a water/ice/snow source > Sneak (Hold, aim at a target)");
    }

}
