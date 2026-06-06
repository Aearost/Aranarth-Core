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
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.util.Vector;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class MetalBlade extends MetalAbility implements AddonAbility {

    public enum Phase {CHARGING, READY}

    private static final long CHARGE_DURATION_MS = 750L;
    private static final long MAX_DURATION_MS = 6000L;
    private static final int MAX_HITS = 3;
    private static final double BLADE_HALF_WIDTH = 0.09;
    private static final int BLADE_STEPS = 6;

    private static final Particle.DustOptions BLADE_DUST =
            new Particle.DustOptions(Color.fromRGB(185, 190, 200), 1.0f);
    private static final Particle.DustOptions CHARGE_DUST =
            new Particle.DustOptions(Color.fromRGB(130, 135, 145), 0.6f);
    private static boolean applyingBladeHit = false;
    private static final Map<UUID, MetalBlade> ACTIVE_INSTANCES = new HashMap<>();

    @Attribute(Attribute.COOLDOWN)
    private long cooldown;
    @Attribute(Attribute.DAMAGE)
    private double damage;

    private Phase phase;
    private long chargeStartTime;
    private long readyStartTime;
    private int hitsLanded;

    public MetalBlade(final Player player) {
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

        this.cooldown = 8000L;
        this.damage = 5.0;
        this.phase = Phase.CHARGING;
        this.chargeStartTime = System.currentTimeMillis();
        this.hitsLanded = 0;

        ACTIVE_INSTANCES.put(player.getUniqueId(), this);
        this.start();
    }

    @Override
    public void progress() {
        if (player.isDead() || !player.isOnline()) {
            remove();
            return;
        }

        switch (phase) {
            case CHARGING -> progressCharging();
            case READY -> progressReady();
        }
    }

    private void progressCharging() {
        if (!player.isSneaking()) {
            remove();
            return;
        }

        final double chargeProgress = Math.min(1.0,
                (double) (System.currentTimeMillis() - chargeStartTime) / CHARGE_DURATION_MS);
        spawnBladeParticles(chargeProgress, CHARGE_DUST);

        if (chargeProgress >= 1.0) {
            readyStartTime = System.currentTimeMillis();
            phase = Phase.READY;
            final Location loc = player.getLocation();
            player.getWorld().playSound(loc, Sound.ENTITY_IRON_GOLEM_ATTACK, 0.7f, 1.9f);
            player.getWorld().playSound(loc, Sound.BLOCK_METAL_HIT, 1.0f, 1.6f);
        }
    }

    private void progressReady() {
        if (System.currentTimeMillis() - readyStartTime >= MAX_DURATION_MS) {
            endWithCooldown();
            return;
        }

        spawnBladeParticles(1.0, BLADE_DUST);
    }

    /**
     * Draws the metallic blade extending downward from the player's main hand.
     * @param progress Fraction of the blade that has formed (0.0–1.0).
     * @param dust     The particle dust options to use for this frame.
     */
    private void spawnBladeParticles(final double progress, final Particle.DustOptions dust) {
        final Vector forward = player.getEyeLocation().getDirection().normalize();
        final Vector right = forward.crossProduct(new Vector(0, 1, 0)).normalize();
        final Vector down = new Vector(0, -1, 0);

        // Derive the right arm's world-space position from the player's yaw only
        final Location feet = player.getLocation();
        final double yawRad = Math.toRadians(feet.getYaw());

        // Rotating 90° clockwise in XZ gives the true right arm direction
        final Vector armRight = new Vector(-Math.cos(yawRad), 0, -Math.sin(yawRad));

        // Arm is 0.35 blocks to the right of body center, hand sits 0.65 blocks above feet
        final Location origin = feet.clone().add(armRight.multiply(0.35)).add(0, 0.65, 0);

        // Blade spine with a gentle taper to a point at the bottom
        final int formedSteps = (int) Math.ceil(progress * BLADE_STEPS);
        for (int i = 0; i <= formedSteps; i++) {
            final double t = (double) i / BLADE_STEPS;
            final Location spineLoc = origin.clone().add(down.clone().multiply(t * 0.4));
            spineLoc.getWorld().spawnParticle(Particle.DUST, spineLoc, 1, 0, 0, 0, 0, dust);

            final double halfWidth = BLADE_HALF_WIDTH * (1.0 - t);
            if (halfWidth > 0.015) {
                spineLoc.getWorld().spawnParticle(Particle.DUST,
                        spineLoc.clone().add(right.clone().multiply(halfWidth)),
                        1, 0, 0, 0, 0, dust);
                spineLoc.getWorld().spawnParticle(Particle.DUST,
                        spineLoc.clone().subtract(right.clone().multiply(halfWidth)),
                        1, 0, 0, 0, 0, dust);
            }
        }
    }

    /**
     * Cancels the vanilla damage and replaces it with the blade's ability damage.
     *
     * @param entity The living entity that was struck.
     * @param event  The entity damage event to cancel.
     */
    public void onMeleeHit(final LivingEntity entity, final EntityDamageByEntityEvent event) {
        if (applyingBladeHit) {
            return;
        }
        applyingBladeHit = true;
        event.setCancelled(true);
        DamageHandler.damageEntity(entity, this.damage, this);
        applyingBladeHit = false;

        final Location impactLoc = entity.getLocation().add(0, entity.getHeight() * 0.5, 0);
        impactLoc.getWorld().spawnParticle(Particle.DUST, impactLoc, 8, 0.12, 0.18, 0.12, 0, BLADE_DUST);
        impactLoc.getWorld().playSound(impactLoc, Sound.ENTITY_IRON_GOLEM_ATTACK, 1.0f, 1.9f);
        impactLoc.getWorld().playSound(impactLoc, Sound.BLOCK_METAL_HIT, 0.8f, 2.0f);

        hitsLanded++;
        if (hitsLanded >= MAX_HITS) {
            endWithCooldown();
        }
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

    public static MetalBlade getActiveInstance(final UUID uuid) {
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
        return this.cooldown;
    }

    @Override
    public Location getLocation() {
        return player.getLocation();
    }

    @Override
    public String getName() {
        return "MetalBlade";
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
        return "Condense metal into a deadly blade along your arm," +
                "empowering your next three melee strikes with razor-sharp force.\n"
                + ChatUtils.translateToColor("&fUsage: Hold Sneak (charge) > Left-Click (up to 3)");
    }
}
