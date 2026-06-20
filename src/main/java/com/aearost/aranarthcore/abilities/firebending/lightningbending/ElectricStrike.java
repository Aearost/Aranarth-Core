package com.aearost.aranarthcore.abilities.firebending.lightningbending;

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
import org.bukkit.util.Vector;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public class ElectricStrike extends LightningAbility implements AddonAbility {

    public enum Phase { CHARGING, TRAVELING }

    private static final long CHARGE_DURATION_MS = 1000L;
    private static final double PROJECTILE_SPEED = 5.0;
    private static final double STEP = 0.2;
    private static final double HIT_RADIUS = 0.5;

    private static final Map<UUID, ElectricStrike> ACTIVE_INSTANCES = new HashMap<>();
    private static final Set<UUID> PENDING_CHARGES = new HashSet<>();

    @Attribute(Attribute.DAMAGE)
    private double damage;
    @Attribute(Attribute.COOLDOWN)
    private long cooldown;
    @Attribute(Attribute.RANGE)
    private double range;

    private Phase phase;
    private long chargeStart;
    private boolean chargeComplete;
    private Vector direction;
    private Location shotLocation;
    private double distanceTraveled;

    public ElectricStrike(Player player) {
        super(player);

        if (!bPlayer.canBend(this) || hasActiveInstance(player.getUniqueId())) {
            return;
        }
        if (!PENDING_CHARGES.remove(player.getUniqueId())) {
            return;
        }

        damage = 12.0;
        cooldown = 7000L;
        range = 50.0;

        phase = Phase.CHARGING;
        chargeStart = System.currentTimeMillis();
        chargeComplete = false;

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
            case TRAVELING -> progressTraveling();
        }
    }

    private void progressCharging() {
        if (!player.isSneaking()) {
            remove();
            return;
        }

        spawnChargingParticles();

        if (!chargeComplete && System.currentTimeMillis() - chargeStart >= CHARGE_DURATION_MS) {
            chargeComplete = true;
            player.getWorld().playSound(player.getLocation(), Sound.ENTITY_LIGHTNING_BOLT_IMPACT, 0.4f, 2.0f);
        }
    }

    public void onSneakRelease() {
        if (!chargeComplete) {
            remove();
            return;
        }
        direction = player.getEyeLocation().getDirection().normalize();
        shotLocation = player.getEyeLocation().clone();
        distanceTraveled = 0.0;
        phase = Phase.TRAVELING;
        shotLocation.getWorld().playSound(shotLocation, Sound.ENTITY_BEE_HURT, 0.6f, 0.2f);
    }

    private void progressTraveling() {
        double remaining = PROJECTILE_SPEED;
        int stepIndex = 0;

        while (remaining > 0) {
            double step = Math.min(STEP, remaining);
            shotLocation.add(direction.clone().multiply(step));
            distanceTraveled += step;
            remaining -= step;

            spawnBoltParticle(shotLocation, stepIndex++);

            if (Math.random() < 0.12) {
                shotLocation.getWorld().playSound(shotLocation, Sound.ENTITY_BEE_HURT, 0.5f, 0.2f);
            }

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
     */
    private boolean checkEntityHit(Location loc) {
        for (Entity entity : loc.getWorld().getNearbyEntities(loc, HIT_RADIUS, HIT_RADIUS, HIT_RADIUS)) {
            if (!(entity instanceof LivingEntity living) || entity.equals(player)) continue;

            DamageHandler.damageEntity(living, damage, this);

            for (int k = 0; k < 8; k++) {
                Particle.DustOptions dust = (k % 2 == 0)
                        ? AranarthBendingUtils.LIGHTNING_DUST_BRIGHT
                        : AranarthBendingUtils.LIGHTNING_DUST_BLUE;
                entity.getLocation().getWorld().spawnParticle(Particle.DUST, entity.getLocation(), 1,
                        Math.random() * 0.4, Math.random() * living.getHeight() * 0.5, Math.random() * 0.4, 0, dust);
            }

            entity.getWorld().playSound(entity.getLocation(), Sound.ENTITY_BEE_HURT, 0.8f, 0.2f);
            player.getWorld().playSound(player.getLocation(), Sound.ENTITY_BEE_HURT, 0.6f, 0.2f);
            return true;
        }
        return false;
    }

    /**
     * Spawns a lightning particle at the given bolt position, alternating between
     * LIGHTNING_DUST_BRIGHT and LIGHTNING_DUST_BLUE to match the Discharge visual style.
     *
     * @param pos       World position of the particle.
     * @param stepIndex Current sub-step index, used to alternate dust colors.
     */
    private void spawnBoltParticle(Location pos, int stepIndex) {
        Particle.DustOptions dust = (stepIndex % 2 == 0)
                ? AranarthBendingUtils.LIGHTNING_DUST_BRIGHT
                : AranarthBendingUtils.LIGHTNING_DUST_BLUE;
        pos.getWorld().spawnParticle(Particle.DUST, pos, 1, 0.02, 0.02, 0.02, 0, dust);
        if (Math.random() < 0.3) {
            pos.getWorld().spawnParticle(Particle.ELECTRIC_SPARK, pos, 1, 0.02, 0.02, 0.02, 0.0);
        }
    }

    private void spawnChargingParticles() {
        Location hand = GeneralMethods.getRightSide(player.getLocation(), 0.55)
                .add(0, player.getEyeHeight() - 0.5, 0);
        if (chargeComplete) {
            double time = System.currentTimeMillis() / 600.0;
            for (int i = 0; i < 4; i++) {
                double angle = time + (i * Math.PI / 2.0);
                Location p = hand.clone().add(0.22 * Math.cos(angle), 0.0, 0.22 * Math.sin(angle));
                Particle.DustOptions dust = (i % 2 == 0)
                        ? AranarthBendingUtils.LIGHTNING_DUST_BRIGHT
                        : AranarthBendingUtils.LIGHTNING_DUST_BLUE;
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
                hand.getWorld().spawnParticle(Particle.DUST, hand, 1, 0.1, 0.1, 0.1, 0, AranarthBendingUtils.LIGHTNING_DUST_BLUE);
            }
        }
    }

    public void endWithCooldown() {
        bPlayer.addCooldown(this);
        remove();
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

    public static ElectricStrike getActiveInstance(UUID uuid) {
        return ACTIVE_INSTANCES.get(uuid);
    }

    public Phase getPhase() {
        return phase;
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
        return "ElectricStrike";
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
        return "Charge a focused bolt of lightning at your hand, and unleash a precise electric strike.\n" +
                ChatUtils.translateToColor("&fUsage: Hold Sneak (charge) > Release Sneak (to fire)");
    }
}
