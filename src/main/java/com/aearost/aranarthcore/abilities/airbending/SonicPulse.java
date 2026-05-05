package com.aearost.aranarthcore.abilities.airbending;

import com.aearost.aranarthcore.utils.AranarthBendingUtils;
import com.aearost.aranarthcore.utils.ChatUtils;
import com.projectkorra.projectkorra.ability.AddonAbility;
import com.projectkorra.projectkorra.attribute.Attribute;
import com.projectkorra.projectkorra.util.DamageHandler;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class SonicPulse extends SoundAbility implements AddonAbility {

    private static final Particle.DustOptions PULSE_DUST = AranarthBendingUtils.SOUND_PULSE_DUST;

    private static final double MIN_HALO_RADIUS = 0.45;
    private static final double MAX_HALO_RADIUS = 1.67;

    private static final double MAX_PLAYER_DAMAGE = 6.0;
    private static final double MIN_PLAYER_DAMAGE = 2.0;
    private static final double MOB_DAMAGE_MULTIPLIER = 1.5;

    private static final double MIN_KNOCKBACK = 0.5;
    private static final double MAX_KNOCKBACK = 2.8;
    private static final double KNOCKBACK_VERTICAL = 0.4;

    private static final double HIT_THICKNESS = 0.6;
    private static final double PULSE_SPEED = 1.6; // blocks per tick

    // Player recoil - triggers when a pulse hits a solid block within this distance.
    // Scale is curved so ring 1 is 3x its proportional share while ring 4 stays full strength.
    private static final double MAX_RECOIL_DISTANCE = 4.0;
    private static final double RECOIL_HORIZONTAL = 1.5;
    private static final double RECOIL_VERTICAL = 0.6;
    private static final double RECOIL_MIN_SCALE = 3.0 * MIN_KNOCKBACK / MAX_KNOCKBACK; // ~0.536

    @Attribute(Attribute.COOLDOWN)
    private long cooldown;
    @Attribute(Attribute.RANGE)
    private double range;

    private final int numPulses;
    private long startTime;
    private long lastPulseTime;
    private long pulseInterval;
    private int pulsesEmitted;
    private boolean expired;
    private final List<Pulse> activePulses = new ArrayList<>();

    public SonicPulse(Player player) {
        super(player);

        this.cooldown = 10000L;
        this.range = 10.0;
        this.numPulses = 4;

        if (!bPlayer.canBend(this)) {
            remove();
            return;
        }

        long now = System.currentTimeMillis();
        this.startTime = now;
        this.pulseInterval = 2500L / numPulses; // evenly spaced across the duration window
        this.lastPulseTime = now - pulseInterval; // emit the first ring on the very first tick
        this.pulsesEmitted = 0;
        this.expired = false;

        start();
    }

    @Override
    public void progress() {
        if (player.isDead() || !player.isOnline()) {
            remove();
            return;
        }

        // Always advance in-flight pulses so they finish even after the player stops sneaking
        advanceAndRenderPulses();

        if (expired) {
            if (activePulses.isEmpty()) {
                bPlayer.addCooldown(this);
                remove();
            }
            return;
        }

        long now = System.currentTimeMillis();

        boolean durationOver = now - startTime >= 2500L;
        boolean allEmitted = pulsesEmitted >= numPulses;
        boolean stoppedEarly = !player.isSneaking() || !bPlayer.canBend(this);

        if (durationOver || allEmitted || stoppedEarly) {
            expired = true;
            return;
        }

        if (now - lastPulseTime >= pulseInterval) {
            emitPulse(pulsesEmitted);
            pulsesEmitted++;
            lastPulseTime = now;
        }
    }

    // -------------------------------------------------------------------------
    // Pulse lifecycle
    // -------------------------------------------------------------------------

    private void emitPulse(int pulseIndex) {
        double t = (numPulses == 1) ? 0.0 : (double) pulseIndex / (numPulses - 1);

        double visualRadius = MIN_HALO_RADIUS + t * (MAX_HALO_RADIUS - MIN_HALO_RADIUS);
        double hitRadius = visualRadius + 0.3;
        double pDamage = MAX_PLAYER_DAMAGE - t * (MAX_PLAYER_DAMAGE - MIN_PLAYER_DAMAGE);
        double mDamage = pDamage * MOB_DAMAGE_MULTIPLIER;
        double knockback = MIN_KNOCKBACK + t * (MAX_KNOCKBACK - MIN_KNOCKBACK);

        Location eyeLoc = player.getEyeLocation();
        Vector dir = eyeLoc.getDirection().normalize();
        Location start = eyeLoc.clone().add(dir.clone().multiply(0.5));

        activePulses.add(new Pulse(start, dir, visualRadius, hitRadius, pDamage, mDamage, knockback, t));
        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_WARDEN_SONIC_CHARGE, 0.55f, 1.3f + (float) (t * 0.4));
    }

    private void advanceAndRenderPulses() {
        Iterator<Pulse> it = activePulses.iterator();
        while (it.hasNext()) {
            Pulse pulse = it.next();

            if (pulse.consumed || pulse.distanceTraveled >= range) {
                it.remove();
                continue;
            }

            pulse.location.add(pulse.direction.clone().multiply(PULSE_SPEED));
            pulse.distanceTraveled += PULSE_SPEED;

            // Block collision — pulse stops; close enough to the caster triggers recoil
            if (pulse.location.getBlock().getType().isSolid()) {
                if (pulse.distanceTraveled <= MAX_RECOIL_DISTANCE) {
                    applyPlayerRecoil(pulse);
                }
                it.remove();
                continue;
            }

            // Entity collision: ring disappears on first contact
            checkCollisions(pulse);

            if (!pulse.consumed) {
                spawnHaloRing(pulse);
            }
        }
    }

    // -------------------------------------------------------------------------
    // Rendering
    // -------------------------------------------------------------------------

    private void spawnHaloRing(Pulse pulse) {
        Location center = pulse.location;
        Vector forward = pulse.direction;

        Vector worldUp = new Vector(0, 1, 0);
        if (Math.abs(forward.dot(worldUp)) > 0.99) {
            worldUp = new Vector(1, 0, 0);
        }
        Vector right = forward.clone().crossProduct(worldUp).normalize();
        Vector up = right.clone().crossProduct(forward).normalize();

        int points = Math.max(12, (int) (2 * Math.PI * pulse.visualRadius * 3));

        for (int i = 0; i < points; i++) {
            double angle = (2.0 * Math.PI / points) * i;
            Vector offset = right.clone().multiply(Math.cos(angle) * pulse.visualRadius)
                    .add(up.clone().multiply(Math.sin(angle) * pulse.visualRadius));
            Location point = center.clone().add(offset);
            point.getWorld().spawnParticle(Particle.DUST, point, 1, 0, 0, 0, 0, PULSE_DUST);
        }
    }

    // -------------------------------------------------------------------------
    // Collision and effects
    // -------------------------------------------------------------------------

    private void checkCollisions(Pulse pulse) {
        for (LivingEntity entity : pulse.location.getWorld().getLivingEntities()) {
            if (entity.equals(player)) {
                continue;
            }

            Location entityCenter = entity.getLocation().add(0, entity.getHeight() / 2.0, 0);
            Vector toEntity = entityCenter.toVector().subtract(pulse.location.toVector());

            double axialDist = Math.abs(toEntity.dot(pulse.direction));
            if (axialDist > HIT_THICKNESS) {
                continue;
            }

            Vector lateral = toEntity.clone().subtract(
                    pulse.direction.clone().multiply(toEntity.dot(pulse.direction)));
            if (lateral.length() > pulse.hitRadius) {
                continue;
            }

            applyEffects(entity, pulse);
            pulse.consumed = true;
            return;
        }
    }

    private void applyPlayerRecoil(Pulse pulse) {
        // This makes ring 1 roughly 3x stronger than a straight proportional scale would give.
        double scale = RECOIL_MIN_SCALE + pulse.t * (1.0 - RECOIL_MIN_SCALE);
        Vector recoil = pulse.direction.clone().multiply(-(RECOIL_HORIZONTAL * scale))
                .add(new Vector(0, RECOIL_VERTICAL * scale, 0));
        player.setVelocity(recoil);
        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_WARDEN_SONIC_BOOM, 0.8f, 0.7f);
    }

    private void applyEffects(LivingEntity entity, Pulse pulse) {
        double damage = (entity instanceof Player) ? pulse.playerDamage : pulse.mobDamage;
        DamageHandler.damageEntity(entity, damage, this);
        applySoundDebuff(entity);

        Vector knockback = pulse.direction.clone().multiply(pulse.knockbackStrength)
                .add(new Vector(0, KNOCKBACK_VERTICAL, 0));
        entity.setVelocity(knockback);

        entity.getWorld().playSound(entity.getLocation(), Sound.ENTITY_WARDEN_SONIC_BOOM, 0.9f, 1.6f);
    }

    // -------------------------------------------------------------------------
    // Inner class
    // -------------------------------------------------------------------------

    private static class Pulse {
        Location location;
        final Vector direction;
        double distanceTraveled;
        final double visualRadius;
        final double hitRadius;
        final double playerDamage;
        final double mobDamage;
        final double knockbackStrength;
        final double t; // 0.0 = first ring, 1.0 = last ring
        boolean consumed;

        Pulse(Location start, Vector dir,
              double visualRadius, double hitRadius,
              double playerDamage, double mobDamage,
              double knockbackStrength, double t) {
            this.location = start.clone();
            this.direction = dir.clone();
            this.distanceTraveled = 0;
            this.visualRadius = visualRadius;
            this.hitRadius = hitRadius;
            this.playerDamage = playerDamage;
            this.mobDamage = mobDamage;
            this.knockbackStrength = knockbackStrength;
            this.t = t;
            this.consumed = false;
        }
    }

    // -------------------------------------------------------------------------
    // AddonAbility / CoreAbility boilerplate
    // -------------------------------------------------------------------------

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
        return "SonicPulse";
    }

    @Override
    public boolean isSneakAbility() {
        return false;
    }

    @Override
    public boolean isHarmlessAbility() {
        return false;
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
        return "Channel your voice into a cascading series of sonic pulses that tear forward through the air. " +
                "Early rings strike hard dealing higher damage, while the later rings send targets flying with immense force.\n" +
                ChatUtils.translateToColor("&fUsage: Sneak (Hold)");
    }

}
