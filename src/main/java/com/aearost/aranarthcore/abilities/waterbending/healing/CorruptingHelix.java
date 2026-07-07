package com.aearost.aranarthcore.abilities.waterbending.healing;

import com.aearost.aranarthcore.AranarthCore;
import com.aearost.aranarthcore.objects.Dominion;
import com.aearost.aranarthcore.utils.AranarthBendingUtils;
import com.aearost.aranarthcore.objects.DominionRank;
import com.aearost.aranarthcore.utils.ChatUtils;
import com.aearost.aranarthcore.utils.DominionUtils;
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
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public class CorruptingHelix extends HealingAbility implements AddonAbility {

    public enum Phase {WATER, PURPLE}

    private static final long WATER_DURATION_MS = 4000L;
    private static final long PURPLE_DURATION_MS = 4000L;
    private static final long SLOWNESS_INTERVAL_MS = 1000L;
    private static final long DAMAGE_INTERVAL_MS = 500L;
    private static final double DAMAGE_PER_TICK = 1.0;
    private static final int MAX_WATER_SLOWNESS = 4;
    private static final double HELIX_RADIUS = 0.6;
    private static final double TARGET_HIT_RADIUS = 0.6;
    private static final double SOURCE_RANGE = 5.0;

    private static final Set<Material> VALID_SOURCES = Set.of(
            Material.WATER,
            Material.ICE,
            Material.PACKED_ICE,
            Material.BLUE_ICE,
            Material.SNOW,
            Material.SNOW_BLOCK,
            Material.POWDER_SNOW
    );

    private static final Particle.DustOptions PURPLE_DUST = new Particle.DustOptions(Color.fromRGB(120, 30, 175), 1.2f);
    private static final Particle.DustOptions PURPLE_DUST_PALE = new Particle.DustOptions(Color.fromRGB(160, 90, 220), 1.0f);

    private static final Map<UUID, CorruptingHelix> ACTIVE_INSTANCES = new HashMap<>();
    private static final Map<UUID, Block> PENDING_SOURCES = new HashMap<>();
    private static final Map<UUID, BukkitRunnable> PENDING_SOURCE_TASKS = new HashMap<>();

    @Attribute(Attribute.COOLDOWN)
    private long cooldown;
    @Attribute(Attribute.RANGE)
    private double range;
    @Attribute("WaterDuration")
    private long waterDuration;
    @Attribute("PurpleDuration")
    private long purpleDuration;

    private Phase phase;
    private LivingEntity target;
    private long phaseStart;
    private long lastSlownessTime;
    private long lastDamageTime;
    private int slownessLevel;
    private boolean decayStarted;
    private boolean blindnessApplied;
    private boolean rootApplied;

    public CorruptingHelix(final Player player) {
        super(player);

        if (!bPlayer.canBend(this)) {
            return;
        }

        if (!PENDING_SOURCES.containsKey(player.getUniqueId())) {
            return;
        }
        clearPendingSource(player.getUniqueId());

        this.cooldown = 15000L;
        this.range = 15.0;
        this.waterDuration = WATER_DURATION_MS;
        this.purpleDuration = PURPLE_DURATION_MS;

        LivingEntity found = findTarget();
        if (found == null) {
            return;
        }

        this.target = found;
        this.phase = Phase.WATER;
        this.phaseStart = System.currentTimeMillis();
        this.lastSlownessTime = System.currentTimeMillis();
        this.lastDamageTime = System.currentTimeMillis();
        this.slownessLevel = 0;
        this.decayStarted = false;
        this.blindnessApplied = false;
        this.rootApplied = false;

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
            case WATER -> progressWater();
            case PURPLE -> progressPurple();
        }
    }

    private void progressWater() {
        long elapsed = System.currentTimeMillis() - phaseStart;
        if (elapsed >= waterDuration) {
            beginPurplePhase();
            return;
        }

        double progress = (double) elapsed / waterDuration;
        spawnWaterParticles(0.0, progress);

        if (Math.random() < 0.08) {
            target.getWorld().playSound(target.getLocation(), Sound.BLOCK_WATER_AMBIENT, 0.2f, 0.7f);
        }

        if (!blindnessApplied && elapsed >= 1000L) {
            target.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, 160, 0, false, true, true), true);
            blindnessApplied = true;
        }

        if (!rootApplied && elapsed >= 2000L) {
            target.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 80, 127, false, true, true), true);
            slownessLevel = MAX_WATER_SLOWNESS;
            rootApplied = true;
        }

        if (!rootApplied && slownessLevel < MAX_WATER_SLOWNESS
                && System.currentTimeMillis() - lastSlownessTime >= SLOWNESS_INTERVAL_MS) {
            applySlownessTick();
            lastSlownessTime = System.currentTimeMillis();
        }
    }

    private void beginPurplePhase() {
        // Guarantee Slowness IV is active before the purple phase begins
        if (slownessLevel < MAX_WATER_SLOWNESS) {
            slownessLevel = MAX_WATER_SLOWNESS;
            if (target.isValid()) {
                target.addPotionEffect(new PotionEffect(
                        PotionEffectType.SLOWNESS, 40, slownessLevel - 1, false, true, true), true);
            }
        }
        phase = Phase.PURPLE;
        phaseStart = System.currentTimeMillis();
        lastDamageTime = System.currentTimeMillis();
        player.getWorld().playSound(target.getLocation(), Sound.BLOCK_AMETHYST_CLUSTER_BREAK, 0.5f, 0.6f);
    }

    private void progressPurple() {
        long elapsed = System.currentTimeMillis() - phaseStart;
        if (elapsed >= purpleDuration) {
            finishWithCooldown(true);
            return;
        }

        double progress = (double) elapsed / purpleDuration;
        spawnPurpleParticles(progress);
        spawnWaterParticles(progress, 1.0);

        if (Math.random() < 0.08) {
            target.getWorld().playSound(target.getLocation(), Sound.BLOCK_WATER_AMBIENT, 0.2f, 0.7f);
        }

        // Keep the target fully immobilized throughout the purple phase
        target.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 40, 127, false, true, true), true);

        if (System.currentTimeMillis() - lastDamageTime >= DAMAGE_INTERVAL_MS) {
            applyDamageTick();
            lastDamageTime = System.currentTimeMillis();
        }
    }

    private void applySlownessTick() {
        if (!target.isValid()) {
            return;
        }
        slownessLevel++;
        target.addPotionEffect(new PotionEffect(
                PotionEffectType.SLOWNESS, 40, slownessLevel - 1, false, true, true), true);
    }

    /**
     * Deals one damage tick directly to the target's health, bypassing all protections.
     */
    private void applyDamageTick() {
        if (!target.isValid()) {
            finishWithCooldown(false);
            return;
        }
        target.setHealth(Math.max(0.0, target.getHealth() - DAMAGE_PER_TICK));
        player.getWorld().playSound(target.getLocation(), Sound.ENTITY_PLAYER_HURT, 0.4f, 1.2f);
    }

    /**
     * Schedules a per-second staircase decay of the Slowness effect applied to the target.
     * If the ability ended during the purple phase, the stun is removed immediately and
     * decay begins from the maximum water-phase level (Slowness IV).
     */
    private void scheduleSlownessDecay() {
        if (decayStarted || target == null || phase == null || !target.isValid()) {
            return;
        }

        target.removePotionEffect(PotionEffectType.BLINDNESS);
        int startLevel;
        if (phase == Phase.PURPLE) {
            target.removePotionEffect(PotionEffectType.SLOWNESS);
            startLevel = MAX_WATER_SLOWNESS;
        } else {
            startLevel = slownessLevel;
        }

        if (startLevel <= 0) {
            return;
        }

        decayStarted = true;
        new SlownessDecayRunnable(target, startLevel).runTaskTimer(AranarthCore.getInstance(), 20L, 20L);
    }

    /**
     * Spawns the dark-blue double helix from the target's feet up to their full height.
     *
     * @param bottomFraction Fraction of height at which the visible band starts (0 = feet).
     * @param topFraction    Fraction of height at which the visible band ends (1 = full height).
     */
    private void spawnWaterParticles(double bottomFraction, double topFraction) {
        if (topFraction <= bottomFraction) {
            return;
        }
        Location base = target.getLocation();
        double totalHeight = target.getHeight();
        double bandBottom = bottomFraction * totalHeight;
        double bandTop = topFraction * totalHeight;
        double bandHeight = bandTop - bandBottom;
        if (bandHeight <= 0) {
            return;
        }

        double time = System.currentTimeMillis() / 2000.0;
        int pointsPerBlock = 14;
        int totalPoints = Math.max(2, (int) (bandHeight * pointsPerBlock));
        int denom = Math.max(1, totalPoints - 1);

        for (int strand = 0; strand < 2; strand++) {
            double strandOffset = strand * Math.PI;
            for (int i = 0; i < totalPoints; i++) {
                double t = (double) i / denom;
                double y = bandBottom + t * bandHeight;
                double angle = (y / totalHeight) * 2.0 * Math.PI + strandOffset + time;
                double x = HELIX_RADIUS * Math.cos(angle);
                double z = HELIX_RADIUS * Math.sin(angle);

                Location loc = base.clone().add(x, y, z);
                Particle.DustOptions dust = (i % 3 == 0) ? AranarthBendingUtils.WATER_DUST_DARK : AranarthBendingUtils.WATER_DUST;
                loc.getWorld().spawnParticle(Particle.DUST, loc, 1, 0, 0, 0, 0, dust);

                if (Math.random() < 0.08) {
                    loc.getWorld().spawnParticle(Particle.SPLASH, loc, 1, 0.04, 0.04, 0.04, 0.01);
                }
            }
        }
    }

    /**
     * Spawns the bold purple double helix from the target's feet up to their full height.
     * @param progress Fraction [0, 1] of the target's height transformed to purple.
     */
    private void spawnPurpleParticles(double progress) {
        if (progress <= 0) {
            return;
        }
        Location base = target.getLocation();
        double totalHeight = target.getHeight();
        double revealHeight = progress * totalHeight;

        double time = System.currentTimeMillis() / 2000.0;
        int pointsPerBlock = 14;
        int totalPoints = Math.max(2, (int) (revealHeight * pointsPerBlock));
        int denom = Math.max(1, totalPoints - 1);

        for (int strand = 0; strand < 2; strand++) {
            double strandOffset = strand * Math.PI;
            for (int i = 0; i < totalPoints; i++) {
                double t = (double) i / denom;
                double y = t * revealHeight;
                double angle = (y / totalHeight) * 2.0 * Math.PI + strandOffset + time;
                double x = HELIX_RADIUS * Math.cos(angle);
                double z = HELIX_RADIUS * Math.sin(angle);

                Location loc = base.clone().add(x, y, z);
                Particle.DustOptions dust = (i % 4 == 0) ? PURPLE_DUST_PALE : PURPLE_DUST;
                loc.getWorld().spawnParticle(Particle.DUST, loc, 1, 0, 0, 0, 0, dust);

                if (Math.random() < 0.06) {
                    loc.getWorld().spawnParticle(Particle.WITCH, loc, 1, 0.04, 0.04, 0.04, 0);
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

                double t = (iteration[0] + 1.0) / maxIterations;
                double radius = HELIX_RADIUS + t * 2.0;
                int count = Math.max(1, (int) (10 * (1.0 - t * 0.7)));

                Particle.DustOptions dust = (endPhase == Phase.PURPLE) ? PURPLE_DUST : AranarthBendingUtils.WATER_DUST;

                for (int i = 0; i < count; i++) {
                    double angle = Math.random() * 2.0 * Math.PI;
                    double yOffset = Math.random() * totalHeight * endProgress;
                    double x = radius * Math.cos(angle);
                    double z = radius * Math.sin(angle);
                    Location loc = base.clone().add(x, yOffset, z);
                    loc.getWorld().spawnParticle(Particle.DUST, loc, 1, 0.1, 0.1, 0.1, 0, dust);
                }

                if (endPhase == Phase.WATER) {
                    double angle = Math.random() * 2.0 * Math.PI;
                    double yOff = Math.random() * totalHeight * endProgress * 0.5;
                    base.getWorld().spawnParticle(Particle.SPLASH,
                            base.clone().add(radius * Math.cos(angle), yOff, radius * Math.sin(angle)),
                            2, 0.1, 0.1, 0.1, 0.04);
                }

                iteration[0]++;
            }
        }.runTaskTimer(AranarthCore.getInstance(), 0L, 3L);
    }

    private LivingEntity findTarget() {
        Location eye = player.getEyeLocation();
        Vector direction = eye.getDirection().normalize();
        Dominion casterDominion = DominionUtils.getPlayerDominion(player.getUniqueId());
        LivingEntity closest = null;
        double closestDist = Double.MAX_VALUE;

        for (Entity entity : player.getNearbyEntities(range, range, range)) {
            if (!(entity instanceof LivingEntity living)) {
                continue;
            }
            if (entity.equals(player)) {
                continue;
            }
            if (!entity.isValid()) {
                continue;
            }

            if (entity instanceof Player targetPlayer) {
                Dominion targetDominion = DominionUtils.getPlayerDominion(targetPlayer.getUniqueId());
                if (casterDominion != null && targetDominion != null) {
                    if (casterDominion.isSameDominion(targetDominion)) {
                        continue;
                    }
                    DominionRank relation = DominionUtils.getRelationKey(casterDominion, targetDominion);
                    if (relation == DominionRank.ALLIED || relation == DominionRank.TRUCED) {
                        continue;
                    }
                }
            }

            double dist = entity.getLocation().distance(eye);
            if (dist > range) {
                continue;
            }

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

    private void finishWithCooldown(boolean completed) {
        long elapsed = System.currentTimeMillis() - phaseStart;
        long phaseDuration = (phase == Phase.WATER) ? waterDuration : purpleDuration;
        double endProgress = Math.min(1.0, (double) elapsed / phaseDuration);
        if (target.isValid()) {
            scheduleDissipation(phase, endProgress, target.getLocation().clone(), target.getHeight());
        }
        bPlayer.addCooldown(this);
        remove();
    }

    public void cancelFromDamage() {
        finishWithCooldown(false);
    }

    public void endWithCooldown() {
        finishWithCooldown(false);
    }

    /**
     * Attempts to register the block as the pending water source for the player.
     * Must be water, ice, or snow.
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
        if (bp != null && bp.isOnCooldown("CorruptingHelix")) {
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
        scheduleSlownessDecay();
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

    public static CorruptingHelix getActiveInstance(final UUID uuid) {
        return ACTIVE_INSTANCES.get(uuid);
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
        return target != null && target.isValid() ? target.getLocation() : player.getLocation();
    }

    @Override
    public String getName() {
        return "CorruptingHelix";
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
        return "Channel a dark form of spiritual waterbending, and corrupt the soul of a target, " +
                "applying an increasing slow to a full stop, and draining the target's health.\n" +
                ChatUtils.translateToColor("&fUsage: Left-click (water source) > Sneak (Hold at target)");
    }

    private static final class SlownessDecayRunnable extends BukkitRunnable {
        private final LivingEntity target;
        private int level;

        private SlownessDecayRunnable(final LivingEntity target, final int startLevel) {
            this.target = target;
            this.level = startLevel;
        }

        @Override
        public void run() {
            level--;
            if (level <= 0 || !target.isValid()) {
                target.removePotionEffect(PotionEffectType.SLOWNESS);
                cancel();
                return;
            }
            target.removePotionEffect(PotionEffectType.SLOWNESS);
            target.addPotionEffect(new PotionEffect(
                    PotionEffectType.SLOWNESS,
                    level * 20,
                    level - 1,
                    false, true, true));
        }
    }

}
