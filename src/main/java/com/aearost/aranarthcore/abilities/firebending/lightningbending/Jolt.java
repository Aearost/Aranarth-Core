package com.aearost.aranarthcore.abilities.firebending.lightningbending;

import com.aearost.aranarthcore.AranarthCore;
import com.aearost.aranarthcore.utils.AranarthBendingUtils;
import com.aearost.aranarthcore.utils.ChatUtils;
import com.projectkorra.projectkorra.GeneralMethods;
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
import java.util.Set;
import java.util.UUID;

public class Jolt extends LightningAbility implements AddonAbility {

    public enum Phase { CHARGING, READY, TRAVELING }

    private static final long CHARGE_DURATION_MS = 500L;
    private static final double PROJECTILE_SPEED = 4.0;
    private static final double STEP = 0.2;
    private static final double HIT_RADIUS = 0.8;

    private static final Map<UUID, Jolt> ACTIVE_INSTANCES = new HashMap<>();
    private static final Set<UUID> PENDING_CHARGES = new HashSet<>();
    private static final Map<UUID, Long> STUNNED_ENTITIES = new HashMap<>();

    @Attribute(Attribute.DAMAGE)
    private double damage;
    @Attribute(Attribute.COOLDOWN)
    private long cooldown;
    @Attribute(Attribute.RANGE)
    private double range;
    @Attribute("StunChance")
    private double stunChance;
    @Attribute("StunDuration")
    private long stunDuration;

    private Phase phase;
    private long chargeStart;
    private Location shotLocation;
    private double distanceTraveled;

    public Jolt(Player player) {
        super(player);

        if (!bPlayer.canBend(this)) return;
        if (!PENDING_CHARGES.remove(player.getUniqueId())) return;

        damage = 3.0;
        cooldown = 4000L;
        range = 45.0;
        stunChance = 0.5;
        stunDuration = 1000L;

        phase = Phase.CHARGING;
        chargeStart = System.currentTimeMillis();

        ACTIVE_INSTANCES.put(player.getUniqueId(), this);
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

        switch (phase) {
            case CHARGING -> progressCharging();
            case READY -> progressReady();
            case TRAVELING -> progressTraveling();
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
            player.getWorld().playSound(player.getLocation(), Sound.ENTITY_LIGHTNING_BOLT_IMPACT, 0.4f, 2.0f);
        }
    }

    private void progressReady() {
        spawnHandParticles(true);
    }

    private void progressTraveling() {
        final Vector lookDir = player.getEyeLocation().getDirection().normalize();
        double remaining = PROJECTILE_SPEED;

        while (remaining > 0) {
            double step = Math.min(STEP, remaining);
            shotLocation.add(lookDir.clone().multiply(step));
            distanceTraveled += step;
            remaining -= step;

            spawnShotParticle(shotLocation);

            if (distanceTraveled > range) {
                endWithCooldown();
                return;
            }

            if (!isTransparent(shotLocation.getBlock())) {
                endWithCooldown();
                return;
            }

            if (checkEntityHit(shotLocation)) {
                endWithCooldown();
                return;
            }
        }
    }

    /**
     * Checks for a living entity within hit radius of the given location.
     * If one is found, applies damage and a possible electrocution stun.
     * Returns true if an entity was hit.
     */
    private boolean checkEntityHit(Location loc) {
        for (Entity entity : loc.getWorld().getNearbyEntities(loc, HIT_RADIUS, HIT_RADIUS, HIT_RADIUS)) {
            if (!(entity instanceof LivingEntity living) || entity.equals(player)) continue;

            DamageHandler.damageEntity(living, damage, this);

            if (Math.random() < stunChance) {
                applyStun(living, stunDuration);
            }

            for (int k = 0; k < 6; k++) {
                playLightningbendingParticle(
                        entity.getLocation().clone().add(
                                (Math.random() - 0.5) * 0.6,
                                Math.random() * living.getHeight(),
                                (Math.random() - 0.5) * 0.6),
                        (float) Math.random(),
                        (float) Math.random(),
                        (float) Math.random()
                );
            }

            entity.getWorld().playSound(entity.getLocation(), Sound.ENTITY_LIGHTNING_BOLT_IMPACT, 0.6f, 1.5f);
            return true;
        }
        return false;
    }

    public void onLeftClick() {
        if (phase != Phase.READY) return;

        shotLocation = GeneralMethods.getRightSide(player.getLocation(), 0.55)
                .add(0, player.getEyeHeight() - 0.5, 0);
        distanceTraveled = 0.0;
        phase = Phase.TRAVELING;

        player.getWorld().playSound(shotLocation, Sound.ENTITY_BEE_HURT, 0.6f, 0.2f);
    }

    public void endWithCooldown() {
        bPlayer.addCooldown(this);
        remove();
    }

    private void spawnHandParticles(boolean ready) {
        Location hand = GeneralMethods.getRightSide(player.getLocation(), 0.55)
                .add(0, player.getEyeHeight() - 0.5, 0);

        if (ready) {
            double time = System.currentTimeMillis() / 600.0;
            for (int i = 0; i < 4; i++) {
                double angle = time + (i * Math.PI / 2.0);
                Location p = hand.clone().add(0.22 * Math.cos(angle), 0.0, 0.22 * Math.sin(angle));
                Particle.DustOptions dust = (i % 2 == 0) ? AranarthBendingUtils.LIGHTNING_DUST_BRIGHT : AranarthBendingUtils.LIGHTNING_DUST;
                p.getWorld().spawnParticle(Particle.DUST, p, 1, 0, 0, 0, 0, dust);
            }
            if (Math.random() < 0.4) {
                hand.getWorld().spawnParticle(Particle.ELECTRIC_SPARK, hand, 1, 0.08, 0.08, 0.08, 0.01);
            }
        } else {
            if (Math.random() < 0.5) {
                hand.getWorld().spawnParticle(Particle.ELECTRIC_SPARK, hand, 1, 0.06, 0.06, 0.06, 0.01);
            }
            if (Math.random() < 0.3) {
                hand.getWorld().spawnParticle(Particle.DUST, hand, 1, 0.1, 0.1, 0.1, 0, AranarthBendingUtils.LIGHTNING_DUST);
            }
        }
    }

    /**
     * Spawns a lightning trail particle at the given shot position.
     *
     * @param pos World position of the particle.
     */
    private void spawnShotParticle(Location pos) {
        pos.getWorld().spawnParticle(Particle.ELECTRIC_SPARK, pos, 1, 0.02, 0.02, 0.02, 0.0);
        if (Math.random() < 0.5) {
            pos.getWorld().spawnParticle(Particle.DUST, pos, 1, 0.03, 0.03, 0.03, 0, AranarthBendingUtils.LIGHTNING_DUST_BRIGHT);
        }
        playLightningbendingParticle(pos.clone(), 0f, 0f, 0f);
    }

    private static void applyStun(LivingEntity target, long durationMs) {
        UUID uuid = target.getUniqueId();
        STUNNED_ENTITIES.put(uuid, System.currentTimeMillis() + durationMs);
        int stunTicks = (int) (durationMs / 50L);

        new BukkitRunnable() {
            int ticks = 0;

            @Override
            public void run() {
                if (ticks >= stunTicks || !STUNNED_ENTITIES.containsKey(uuid)) {
                    STUNNED_ENTITIES.remove(uuid);
                    cancel();
                    return;
                }
                if (target.isDead() || (target instanceof Player p && !p.isOnline())) {
                    STUNNED_ENTITIES.remove(uuid);
                    cancel();
                    return;
                }
                target.setVelocity(new Vector(0, target.getVelocity().getY(), 0));
                ticks++;
            }
        }.runTaskTimer(AranarthCore.getInstance(), 0L, 1L);
    }

    public static void markPendingCharge(UUID uuid) {
        PENDING_CHARGES.add(uuid);
    }

    public static void clearPendingCharge(UUID uuid) {
        PENDING_CHARGES.remove(uuid);
    }

    public static boolean hasActiveInstance(UUID uuid) {
        return ACTIVE_INSTANCES.containsKey(uuid);
    }

    public static Jolt getActiveInstance(UUID uuid) {
        return ACTIVE_INSTANCES.get(uuid);
    }

    public static boolean isStunned(UUID uuid) {
        Long expiry = STUNNED_ENTITIES.get(uuid);
        if (expiry == null) return false;
        if (System.currentTimeMillis() >= expiry) {
            STUNNED_ENTITIES.remove(uuid);
            return false;
        }
        return true;
    }

    @Override
    public void remove() {
        ACTIVE_INSTANCES.remove(player.getUniqueId());
        super.remove();
    }

    @Override
    public void stop() {
        if (player != null) ACTIVE_INSTANCES.remove(player.getUniqueId());
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
        return true;
    }

    @Override
    public boolean isHarmlessAbility() {
        return false;
    }

    @Override
    public String getName() {
        return "Jolt";
    }

    @Override
    public void load() {}

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
        return "Charge a focused bolt of lightning at your right hand, rapidly sending out a jolt of lightning. " +
                "Targets struck have a 50% chance to be electrocuted and stunned, " +
                "preventing their movement for one second.\n" +
                ChatUtils.translateToColor("&fUsage: Hold Sneak (to charge) > Left-click (to launch)");
    }
}
