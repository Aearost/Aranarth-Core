package com.aearost.aranarthcore.abilities.spiritual;

import com.aearost.aranarthcore.utils.AranarthBendingUtils;
import com.aearost.aranarthcore.utils.ChatUtils;
import com.projectkorra.projectkorra.GeneralMethods;
import com.projectkorra.projectkorra.ability.AddonAbility;
import com.projectkorra.projectkorra.ability.SpiritualAbility;
import com.projectkorra.projectkorra.attribute.Attribute;
import com.projectkorra.projectkorra.util.DamageHandler;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class EnergyBurst extends SpiritualAbility implements AddonAbility {

    private static final double BURST_RADIUS = 8.0;
    private static final long BURST_DURATION = 1500L; // 30 ticks
    private static final int BOLTS_PER_TICK = 7;

    private static final Map<UUID, EnergyBurst> activeInstances = new HashMap<>();

    @Attribute(Attribute.COOLDOWN)
    private long cooldown;
    @Attribute(Attribute.CHARGE_DURATION)
    private long chargeDuration;
    @Attribute(Attribute.DAMAGE)
    private double damage;

    private enum State {CHARGING, READY, BURSTING}

    private State state;
    private long burstStart;
    private final int heldSlot;

    public EnergyBurst(final Player player) {
        super(player);

        this.cooldown = 12000L;
        this.chargeDuration = 3000L;
        this.damage = 10.0; // 5 hearts
        this.state = State.CHARGING;
        this.heldSlot = player.getInventory().getHeldItemSlot();

        if (!bPlayer.canBend(this)) {
            return;
        }

        start();
        activeInstances.put(player.getUniqueId(), this);
    }

    // -------------------------------------------------------------------------
    // Progress / state machine
    // -------------------------------------------------------------------------

    @Override
    public void progress() {
        if (player.isDead() || !player.isOnline()) {
            remove();
            return;
        }
        switch (state) {
            case CHARGING -> handleCharging();
            case READY -> handleReady();
            case BURSTING -> handleBursting();
        }
    }

    private void handleCharging() {
        if (!player.isSneaking()) {
            remove(); // No cooldown as sneak was released before charge finished
            return;
        }
        if (player.getInventory().getHeldItemSlot() != heldSlot) {
            remove();
            return;
        }

        final long elapsed = System.currentTimeMillis() - getStartTime();
        if (elapsed >= chargeDuration) {
            state = State.READY;
            player.getWorld().playSound(player.getLocation(), Sound.ENTITY_VEX_AMBIENT, 1.5f, 0.5f);
            spawnChargeCompleteBurst();
        }
    }

    private void handleReady() {
        if (player.getInventory().getHeldItemSlot() != heldSlot) {
            remove(); // No cooldown as the ability never was triggered
            return;
        }
        spawnReadyParticles();

        if (!player.isSneaking()) {
            triggerBurst();
        }
    }

    private void triggerBurst() {
        state = State.BURSTING;
        burstStart = System.currentTimeMillis();
        applyBurstEffects();
        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_GENERIC_EXPLODE, 1.0f, 0.5f);
        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_VEX_DEATH, 1.5f, 0.3f);
        player.getWorld().playSound(player.getLocation(), Sound.BLOCK_BEACON_POWER_SELECT, 0.8f, 0.4f);
    }

    private void handleBursting() {
        if (System.currentTimeMillis() - burstStart >= BURST_DURATION) {
            bPlayer.addCooldown(this);
            remove();
            return;
        }
        spawnBurstParticles();
    }

    // -------------------------------------------------------------------------
    // Damage / effect application
    // -------------------------------------------------------------------------

    private void applyBurstEffects() {
        final Location center = player.getLocation().add(0, 1, 0);
        for (final LivingEntity entity : player.getWorld().getLivingEntities()) {
            if (entity.equals(player)) {
                continue;
            }
            if (GeneralMethods.isRegionProtectedFromBuild(this, entity.getLocation())) {
                continue;
            }

            final Location entityCenter = entity.getLocation().add(0, entity.getHeight() / 2.0, 0);
            if (entityCenter.distance(center) > BURST_RADIUS) {
                continue;
            }

            DamageHandler.damageEntity(entity, damage, this);
            AranarthBendingUtils.applyRandomSpiritEffect(entity);
            entity.getWorld().playSound(entity.getLocation(), Sound.ENTITY_VEX_HURT, 1.0f, 1.0f);
        }
    }

    // -------------------------------------------------------------------------
    // Particle helpers
    // -------------------------------------------------------------------------

    private void spawnChargeCompleteBurst() {
        final Location center = player.getLocation().add(0, 1.0, 0);
        for (int i = 0; i < 4; i++) {
            final Particle.DustOptions dust = new Particle.DustOptions(AranarthBendingUtils.SPIRIT_COLORS[i], 1.5f);
            for (int j = 0; j < 6; j++) {
                final double angle = (j / 6.0) * 2 * Math.PI;
                final Location loc = center.clone().add(Math.cos(angle) * 0.9, 0, Math.sin(angle) * 0.9);
                loc.getWorld().spawnParticle(Particle.DUST, loc, 3, 0.1, 0.1, 0.1, 0, dust);
            }
        }
    }

    /**
     * Orbiting colour orbs shown while fully charged and waiting for sneak release.
     */
    private void spawnReadyParticles() {
        final double time = System.currentTimeMillis() / 500.0;
        for (int i = 0; i < 4; i++) {
            final Particle.DustOptions dust = new Particle.DustOptions(AranarthBendingUtils.SPIRIT_COLORS[i], 1.2f);
            final double angle = (i / 4.0) * 2 * Math.PI + time;
            final Location loc = player.getLocation().add(0, 1.2, 0)
                    .add(Math.cos(angle) * 0.8, 0.25 * Math.sin(time + i), Math.sin(angle) * 0.8);
            loc.getWorld().spawnParticle(Particle.DUST, loc, 3, 0.05, 0.05, 0.05, 0, dust);
        }
    }

    /**
     * Each tick during the burst, emits zigzag particle rays from the player outward in random directions.
     */
    private void spawnBurstParticles() {
        final Location center = player.getLocation().add(0, 1.0, 0);
        for (int b = 0; b < BOLTS_PER_TICK; b++) {
            final Color color = AranarthBendingUtils.SPIRIT_COLORS[(int) (Math.random() * 4)];
            final Particle.DustOptions dust = new Particle.DustOptions(color, 0.8f + (float) Math.random() * 0.7f);

            // Random outward direction as a unit vector, scaled to step size
            Vector dir = randomUnitVector().multiply(0.35);
            final Location pos = center.clone();
            final int steps = 10 + (int) (Math.random() * 16);

            for (int s = 0; s < steps; s++) {
                // Zigzag: perturb direction every other step
                if (s % 2 == 1) {
                    dir.add(new Vector(
                            (Math.random() - 0.5) * 0.45,
                            (Math.random() - 0.5) * 0.45,
                            (Math.random() - 0.5) * 0.45));
                    final double len = dir.length();
                    if (len > 0) {
                        dir = dir.multiply(0.35 / len); // re-normalise to fixed speed
                    }
                }

                pos.add(dir);
                if (pos.distance(center) > BURST_RADIUS) {
                    break;
                }

                pos.getWorld().spawnParticle(Particle.DUST, pos, 1, 0, 0, 0, 0, dust);
                if (s % 4 == 0) {
                    pos.getWorld().spawnParticle(Particle.SOUL, pos, 1, 0.05, 0.05, 0.05, 0);
                }
            }
        }
    }

    /**
     * Uniform random unit vector using spherical coordinates.
     */
    private static Vector randomUnitVector() {
        final double theta = Math.random() * 2 * Math.PI;
        final double cosPhi = 2 * Math.random() - 1;
        final double sinPhi = Math.sqrt(1.0 - cosPhi * cosPhi);
        return new Vector(sinPhi * Math.cos(theta), cosPhi, sinPhi * Math.sin(theta));
    }

    // -------------------------------------------------------------------------
    // Called by the listener on slot change
    // -------------------------------------------------------------------------

    public void onSlotChange() {
        if (state == State.BURSTING) {
            bPlayer.addCooldown(this);
        }
        remove();
    }

    // -------------------------------------------------------------------------
    // Lifecycle / registry
    // -------------------------------------------------------------------------

    @Override
    public void remove() {
        super.remove();
        activeInstances.remove(player.getUniqueId());
    }

    public static boolean hasActiveInstance(final UUID uuid) {
        return activeInstances.containsKey(uuid);
    }

    public static EnergyBurst getActiveInstance(final UUID uuid) {
        return activeInstances.get(uuid);
    }

    // -------------------------------------------------------------------------
    // ProjectKorra ability interface
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
        return player.getLocation();
    }

    @Override
    public String getName() {
        return "EnergyBurst";
    }

    @Override
    public void load() {
    }

    @Override
    public void stop() {
        if (player != null) {
            activeInstances.remove(player.getUniqueId());
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
        return "Condense your spiritual energy to a breaking point and release it all at once as an uncontrollable burst of energy. " +
                "Every target caught inside will be damaged and struck by a different negative potion effect.\n" +
                ChatUtils.translateToColor("&fUsage: Hold Sneak until you see particles and then release sneak to fire the attack.");
    }
}
