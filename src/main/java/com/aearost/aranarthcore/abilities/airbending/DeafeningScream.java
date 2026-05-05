package com.aearost.aranarthcore.abilities.airbending;

import com.aearost.aranarthcore.utils.ChatUtils;
import com.projectkorra.projectkorra.ability.AddonAbility;
import com.projectkorra.projectkorra.attribute.Attribute;
import com.projectkorra.projectkorra.util.DamageHandler;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.Vector;

import java.util.Random;

public class DeafeningScream extends SoundAbility implements AddonAbility {

    private static final Color TURQUOISE = Color.fromRGB(72, 209, 204);
    private static final Particle.DustOptions RING_DUST = new Particle.DustOptions(TURQUOISE, 0.8f);
    private static final Random RANDOM = new Random();

    private static final int HALO_POINTS = 18;
    private static final double HALO_RADIUS = 0.45;
    private static final double HALO_FORWARD_OFFSET = 0.8;

    @Attribute(Attribute.CHARGE_DURATION)
    private long chargeDuration;
    @Attribute(Attribute.COOLDOWN)
    private long cooldown;
    @Attribute(Attribute.RADIUS)
    private double radius;
    @Attribute(Attribute.DAMAGE)
    private double damage;

    private long chargeStartTime;
    private boolean isCharged;

    public DeafeningScream(Player player) {
        super(player);

        this.chargeDuration = 3000L;
        this.cooldown = 15000L;
        this.radius = 7.0;
        this.damage = 16.0; // 8 hearts

        if (!bPlayer.canBend(this)) {
            remove();
            return;
        }

        this.chargeStartTime = System.currentTimeMillis();
        this.isCharged = false;

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

        if (!player.isSneaking()) {
            if (isCharged) {
                scream();
            } else {
                remove();
            }
            return;
        }

        long elapsed = System.currentTimeMillis() - chargeStartTime;
        if (elapsed >= chargeDuration) {
            isCharged = true;
        }

        if (isCharged) {
            spawnFaceHalo();
        }
    }

    private void scream() {
        Location origin = player.getLocation();

        int screamCount = 5 + RANDOM.nextInt(6); // 5–10 screams
        for (int i = 0; i < screamCount; i++) {
            float pitch = 0.5f + RANDOM.nextFloat(); // random pitch from 0.5 to 1.5
            player.getWorld().playSound(origin, Sound.ENTITY_GHAST_SCREAM, 2.0f, pitch);
        }

        for (LivingEntity entity : origin.getWorld().getLivingEntities()) {
            if (entity.equals(player)) continue;
            if (entity.getLocation().distance(origin) <= radius) {
                DamageHandler.damageEntity(entity, damage, this);
                applySoundDebuff(entity);
            }
        }

        // Penalise the caster for the immense strain
        player.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 80, 2, false, true));

        spawnBurstWave();

        bPlayer.addCooldown(this);
        remove();
    }

    /**
     * Spawns concentric horizontal rings of turquoise dust particles radiating outward
     * to the edge of the burst radius, visually marking the AOE impact area.
     */
    private void spawnBurstWave() {
        Location base = player.getLocation();
        int numRings = 6;
        double[] heights = {0.5, 1.0, 1.5};

        for (int r = 1; r <= numRings; r++) {
            double ringRadius = (double) r / numRings * radius;
            int points = Math.max(12, (int) (ringRadius * 5));

            for (double height : heights) {
                Location center = base.clone().add(0, height, 0);
                for (int i = 0; i < points; i++) {
                    double angle = (2.0 * Math.PI / points) * i;
                    double x = Math.cos(angle) * ringRadius;
                    double z = Math.sin(angle) * ringRadius;
                    center.getWorld().spawnParticle(Particle.DUST, center.clone().add(x, 0, z), 1, 0, 0, 0, 0, RING_DUST);
                }
            }
        }
    }

    /**
     * Spawns a turquoise halo ring in the plane perpendicular to the player's look direction,
     * centred just in front of the player's face as a charge visual cue.
     */
    private void spawnFaceHalo() {
        Location eyeLocation = player.getEyeLocation();
        Vector forward = eyeLocation.getDirection().normalize();

        // Build orthonormal right/up vectors for the ring plane
        Vector worldUp = new Vector(0, 1, 0);
        if (Math.abs(forward.dot(worldUp)) > 0.99) {
            worldUp = new Vector(1, 0, 0);
        }
        Vector right = forward.clone().crossProduct(worldUp).normalize();
        Vector up = right.clone().crossProduct(forward).normalize();

        Location center = eyeLocation.clone().add(forward.clone().multiply(HALO_FORWARD_OFFSET));

        for (int i = 0; i < HALO_POINTS; i++) {
            double angle = (2.0 * Math.PI / HALO_POINTS) * i;
            Vector offset = right.clone().multiply(Math.cos(angle) * HALO_RADIUS)
                    .add(up.clone().multiply(Math.sin(angle) * HALO_RADIUS));
            Location point = center.clone().add(offset);
            point.getWorld().spawnParticle(Particle.DUST, point, 1, 0, 0, 0, 0, RING_DUST);
        }
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
        return "DeafeningScream";
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
    public void load() {}

    @Override
    public void stop() {}

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
        return "Channel your voice to its breaking point, and release a devastating scream that shatters the air around you. " +
                "All nearby enemies are caught in the burst and suffer severe damage, " +
                "though the immense strain leaves you heavily slowed afterward.\n" +
                ChatUtils.translateToColor("&fUsage: Hold Sneak (to charge) > Release Sneak");
    }

}
