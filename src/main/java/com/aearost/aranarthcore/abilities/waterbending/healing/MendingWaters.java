package com.aearost.aranarthcore.abilities.waterbending.healing;

import com.aearost.aranarthcore.AranarthCore;
import com.aearost.aranarthcore.objects.AranarthPlayer;
import com.aearost.aranarthcore.utils.AranarthBendingUtils;
import com.aearost.aranarthcore.utils.AranarthUtils;
import com.aearost.aranarthcore.utils.ChatUtils;
import com.projectkorra.projectkorra.BendingPlayer;
import com.projectkorra.projectkorra.GeneralMethods;
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
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public class MendingWaters extends HealingAbility implements AddonAbility {

    public enum Phase {CHARGING, READY}

    private static final long CHARGE_DURATION_MS = 500L;
    private static final long SHOT_COOLDOWN_MS = 200L;
    private static final double SOURCE_RANGE = 5.0;
    private static final double PROJECTILE_SPEED = 1.5;
    private static final double STEP = 0.25;
    private static final double HIT_RADIUS = 0.8;
    private static final double BASE_HEAL = 2.0; // 1 heart = 2 HP

    private static final double[] RANK_MULTIPLIERS = {
            1.0, 1.05, 1.15, 1.3, 1.5, 1.75, 2.0, 2.25, 2.5
    };

    private static final Particle.DustOptions AQUA_DUST =
            new Particle.DustOptions(Color.fromRGB(72, 209, 204), 1.1f);
    private static final Particle.DustOptions AQUA_DUST_BRIGHT =
            new Particle.DustOptions(Color.fromRGB(140, 235, 225), 0.9f);

    private static final Set<Material> VALID_SOURCES = Set.of(
            Material.WATER,
            Material.ICE,
            Material.PACKED_ICE,
            Material.BLUE_ICE,
            Material.SNOW,
            Material.SNOW_BLOCK,
            Material.POWDER_SNOW
    );

    private static final Map<UUID, MendingWaters> ACTIVE_INSTANCES = new HashMap<>();
    private static final Map<UUID, Block> PENDING_SOURCES = new HashMap<>();
    private static final Map<UUID, BukkitRunnable> PENDING_SOURCE_TASKS = new HashMap<>();

    @Attribute(Attribute.COOLDOWN)
    private long cooldown;
    @Attribute(Attribute.RANGE)
    private double range;
    @Attribute("MaxShots")
    private int maxShots;

    private Phase phase;
    private long chargeStart;
    private int shotsRemaining;
    private long lastShotTime;
    private double healAmount;
    private final List<WaterShot> shots = new ArrayList<>();

    /**
     * A single in-flight water projectile.
     */
    private static final class WaterShot {
        Location location;
        double distanceTraveled;

        WaterShot(final Location origin) {
            this.location = origin.clone();
            this.distanceTraveled = 0.0;
        }
    }

    public MendingWaters(final Player player) {
        super(player);

        if (!bPlayer.canBend(this)) {
            return;
        }

        if (!PENDING_SOURCES.containsKey(player.getUniqueId())) {
            return;
        }
        clearPendingSource(player.getUniqueId());

        this.cooldown = 8000L;
        this.range = 20.0;
        this.maxShots = 5;
        this.phase = Phase.CHARGING;
        this.chargeStart = System.currentTimeMillis();
        this.shotsRemaining = this.maxShots;
        this.lastShotTime = 0L;
        this.healAmount = computeHealAmount();

        ACTIVE_INSTANCES.put(player.getUniqueId(), this);
        start();
    }

    /**
     * Returns the per-hit heal amount for this cast, scaled by the user's rank.
     */
    private double computeHealAmount() {
        final AranarthPlayer ap = AranarthUtils.getAranarthPlayers().get(player.getUniqueId());
        final int rank = (ap != null) ? ap.getRank() : 0;
        return BASE_HEAL * RANK_MULTIPLIERS[Math.min(rank, RANK_MULTIPLIERS.length - 1)];
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

        switch (phase) {
            case CHARGING -> progressCharging();
            case READY -> progressReady();
        }

        progressShots();

        if (shotsRemaining <= 0 && shots.isEmpty()) {
            bPlayer.addCooldown(this);
            remove();
        }
    }

    private void progressCharging() {
        if (!player.isSneaking()) {
            bPlayer.addCooldown(this);
            remove();
            return;
        }

        spawnHandParticles(false);

        if (System.currentTimeMillis() - chargeStart >= CHARGE_DURATION_MS) {
            phase = Phase.READY;
            player.getWorld().playSound(player.getLocation(), Sound.BLOCK_WATER_AMBIENT, 0.6f, 1.3f);
        }
    }

    private void progressReady() {
        if (shotsRemaining > 0) {
            spawnHandParticles(true);
        }
    }

    private void progressShots() {
        final Vector lookDir = player.getEyeLocation().getDirection().normalize();

        for (int i = shots.size() - 1; i >= 0; i--) {
            final WaterShot shot = shots.get(i);
            boolean remove = false;
            LivingEntity hitEntity = null;
            Location hitPos = null;

            double remaining = PROJECTILE_SPEED;
            while (remaining > 0 && !remove) {
                final double step = Math.min(STEP, remaining);
                shot.location.add(lookDir.clone().multiply(step));
                shot.distanceTraveled += step;
                remaining -= step;

                final double fraction = Math.min(shot.distanceTraveled / range, 1.0);

                spawnShotParticle(shot.location, fraction);

                if (shot.distanceTraveled > range) {
                    remove = true;
                    break;
                }

                if (shot.location.getBlock().getType().isSolid()
                        && !shot.location.getBlock().isPassable()) {
                    remove = true;
                    break;
                }

                for (final Entity entity : shot.location.getWorld().getNearbyEntities(
                        shot.location, HIT_RADIUS, HIT_RADIUS, HIT_RADIUS)) {
                    if (!(entity instanceof LivingEntity living)) {
                        continue;
                    }
                    if (entity.equals(player)) {
                        continue;
                    }
                    hitEntity = living;
                    hitPos = shot.location.clone();
                    remove = true;
                    break;
                }
            }

            if (hitEntity != null) {
                applyHealingHit(hitEntity, hitPos);
            }
            if (remove) {
                shots.remove(i);
            }
        }
    }

    /**
     * Heals the struck entity up to its maximum health, then returns half the total heal to the user.
     *
     * @param target The entity that was struck.
     * @param hitPos The world position of the collision.
     */
    private void applyHealingHit(final LivingEntity target, final Location hitPos) {
        target.setHealth(Math.min(target.getHealth() + healAmount, target.getMaxHealth()));

        final double selfHeal = healAmount / 2.0;
        player.setHealth(Math.min(player.getHealth() + selfHeal, player.getMaxHealth()));

        hitPos.getWorld().spawnParticle(Particle.DUST, hitPos, 12, 0.25, 0.25, 0.25, 0, AQUA_DUST_BRIGHT);
        final Location heartPos = target.getLocation().clone().add(0, target.getEyeHeight() * 0.5, 0);
        heartPos.getWorld().spawnParticle(Particle.HEART, heartPos, 3, 0.2, 0.3, 0.2, 0);
        target.getWorld().playSound(target.getLocation(), Sound.BLOCK_AMETHYST_CLUSTER_BREAK, 0.5f, 1.8f);
    }

    /**
     * Spawns particles at the user's right hand.
     *
     * @param ready {@code true} once the charge is complete and shots can be fired.
     */
    private void spawnHandParticles(final boolean ready) {
        final Location hand = GeneralMethods.getRightSide(player.getLocation(), 0.55)
                .add(0, player.getEyeHeight() - 0.5, 0);

        if (ready) {
            final double time = System.currentTimeMillis() / 800.0;
            for (int i = 0; i < 4; i++) {
                final double angle = time + (i * Math.PI / 2.0);
                final Location p = hand.clone().add(0.22 * Math.cos(angle), 0.0, 0.22 * Math.sin(angle));
                final Particle.DustOptions dust = (i % 2 == 0)
                        ? AranarthBendingUtils.WATER_DUST : AranarthBendingUtils.WATER_DUST_DARK;
                p.getWorld().spawnParticle(Particle.DUST, p, 1, 0, 0, 0, 0, dust);
            }
        } else {
            if (Math.random() < 0.5) {
                hand.getWorld().spawnParticle(Particle.DRIPPING_WATER, hand, 1, 0.1, 0.1, 0.1, 0.02);
            }
        }
    }

    /**
     * Spawns a single trail particle whose colour blends from dark blue to aqua.
     *
     * @param pos      World position of the particle.
     * @param fraction Distance travelled as a fraction of maximum range [0, 1].
     */
    private void spawnShotParticle(final Location pos, final double fraction) {
        final Particle.DustOptions dust;
        if (Math.random() < fraction) {
            dust = (Math.random() < 0.35) ? AQUA_DUST_BRIGHT : AQUA_DUST;
        } else {
            dust = (Math.random() < 0.35)
                    ? AranarthBendingUtils.WATER_DUST : AranarthBendingUtils.WATER_DUST_DARK;
        }
        pos.getWorld().spawnParticle(Particle.DUST, pos, 1, 0.04, 0.04, 0.04, 0, dust);
        if (Math.random() < 0.12) {
            pos.getWorld().spawnParticle(Particle.SPLASH, pos, 1, 0.04, 0.04, 0.04, 0.01);
        }
    }

    /**
     * Fires one water projectile toward the player's look direction.
     */
    public void onLeftClick() {
        if (phase != Phase.READY || shotsRemaining <= 0) {
            return;
        }

        final long now = System.currentTimeMillis();
        if (now - lastShotTime < SHOT_COOLDOWN_MS) {
            return;
        }
        lastShotTime = now;

        final Location origin = GeneralMethods.getRightSide(player.getLocation(), 0.55)
                .add(0, player.getEyeHeight() - 0.5, 0);

        shots.add(new WaterShot(origin));
        shotsRemaining--;

        player.getWorld().playSound(origin, Sound.BLOCK_WATER_AMBIENT, 0.5f, 1.4f);
    }

    /**
     * Applies the cooldown and immediately removes the ability, discarding all in-flight shots.
     */
    public void endWithCooldown() {
        bPlayer.addCooldown(this);
        remove();
    }

    /**
     * Attempts to register the block as the pending water source for the player.
     * Must be a water, ice, or snow block within range.
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

        final BendingPlayer bp = BendingPlayer.getBendingPlayer(player);
        if (bp != null && bp.isOnCooldown("MendingWaters")) {
            return;
        }

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
        shots.clear();
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

    public static MendingWaters getActiveInstance(final UUID uuid) {
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
        return player.getLocation();
    }

    @Override
    public String getName() {
        return "MendingWaters";
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
        return "Channel your healing abilities to launchable streams of water, allowing you to heal your allies " +
                "from a distance. You also benefit from half of the heal!\n" +
                ChatUtils.translateToColor("&fUsage: Left-click (water source) > Hold Sneak (charge) > Left-click (up to 5x)");
    }
}
