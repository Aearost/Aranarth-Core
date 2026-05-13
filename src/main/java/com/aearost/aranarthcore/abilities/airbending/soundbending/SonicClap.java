package com.aearost.aranarthcore.abilities.airbending.soundbending;

import com.aearost.aranarthcore.AranarthCore;
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
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public class SonicClap extends SoundAbility implements AddonAbility {

    private static final Particle.DustOptions CLAP_DUST = new Particle.DustOptions(
            AranarthBendingUtils.SOUND_COLOR_PULSE, 0.6f);

    private static final double HALO_RADIUS = 0.55;
    private static final double HIT_RADIUS = 1.1;
    private static final double HALO_SPACING = 1.2;
    private static final double CLAP_SPEED = 1.5; // blocks per tick
    private static final double BASE_PLAYER_DAMAGE = 5.0;
    private static final double MOB_DAMAGE_MULTIPLIER = 1.5;

    @Attribute(Attribute.COOLDOWN)
    private long cooldown;
    @Attribute(Attribute.RANGE)
    private double range;

    private final List<ClapWave> waves = new ArrayList<>();
    private final Set<UUID> hitEntities = new HashSet<>();

    public SonicClap(Player player) {
        super(player);

        this.cooldown = 3000L;
        this.range = 12.0;

        if (!bPlayer.canBend(this)) {
            remove();
            return;
        }

        Location eyeLoc = player.getEyeLocation();
        Vector dir = eyeLoc.getDirection().normalize();

        // Horizontal vector perpendicular to the look direction for side-by-side offsets.
        // Fallback to world X if the player is looking straight up or down
        Vector flatDir = new Vector(dir.getX(), 0, dir.getZ());
        Vector right = (flatDir.lengthSquared() < 0.001)
                ? new Vector(1, 0, 0)
                : new Vector(-dir.getZ(), 0, dir.getX()).normalize();

        for (int i = -1; i <= 1; i++) {
            Location start = eyeLoc.clone().add(right.clone().multiply(i * HALO_SPACING));
            waves.add(new ClapWave(start, dir));
        }

        playClapSound(player.getLocation());
        bPlayer.addCooldown(this);
        start();
    }

    @Override
    public void progress() {
        if (player.isDead() || !player.isOnline()) {
            remove();
            return;
        }

        Iterator<ClapWave> it = waves.iterator();
        while (it.hasNext()) {
            ClapWave wave = it.next();

            if (wave.distanceTraveled >= range) {
                it.remove();
                continue;
            }

            wave.prevLocation = wave.location.clone();
            wave.location.add(wave.direction.clone().multiply(CLAP_SPEED));
            wave.distanceTraveled += CLAP_SPEED;

            if (isGlass(wave.location.getBlock().getType())) {
                shatterGlass(wave.location.getBlock());
            } else if (wave.location.getBlock().getType().isSolid()) {
                it.remove();
                continue;
            }

            checkCollisions(wave);
            spawnHalo(wave);
        }

        if (waves.isEmpty()) {
            remove();
        }
    }

    // -------------------------------------------------------------------------
    // Sound
    // -------------------------------------------------------------------------

    private static void playClapSound(Location loc) {
        // Thicker initial cracking sound
        loc.getWorld().playSound(loc, Sound.BLOCK_NOTE_BLOCK_SNARE, 2.0f, 0.60f);
        loc.getWorld().playSound(loc, Sound.BLOCK_NOTE_BLOCK_SNARE, 2.0f, 0.56f);
        loc.getWorld().playSound(loc, Sound.BLOCK_NOTE_BLOCK_SNARE, 2.0f, 0.52f);
        loc.getWorld().playSound(loc, Sound.BLOCK_NOTE_BLOCK_SNARE, 2.0f, 0.48f);
        loc.getWorld().playSound(loc, Sound.BLOCK_NOTE_BLOCK_SNARE, 2.0f, 0.44f);
        loc.getWorld().playSound(loc, Sound.BLOCK_NOTE_BLOCK_SNARE, 2.0f, 0.40f);
        loc.getWorld().playSound(loc, Sound.BLOCK_NOTE_BLOCK_SNARE, 2.0f, 0.35f);
        // Echo for the reverb tail, blending into the body
        new BukkitRunnable() {
            @Override
            public void run() {
                loc.getWorld().playSound(loc, Sound.BLOCK_NOTE_BLOCK_SNARE, 1.75f, 0.58f);
                loc.getWorld().playSound(loc, Sound.BLOCK_NOTE_BLOCK_SNARE, 1.75f, 0.54f);
                loc.getWorld().playSound(loc, Sound.BLOCK_NOTE_BLOCK_SNARE, 1.75f, 0.50f);
                loc.getWorld().playSound(loc, Sound.BLOCK_NOTE_BLOCK_SNARE, 1.75f, 0.46f);
                loc.getWorld().playSound(loc, Sound.BLOCK_NOTE_BLOCK_SNARE, 1.75f, 0.42f);
                loc.getWorld().playSound(loc, Sound.BLOCK_NOTE_BLOCK_SNARE, 1.75f, 0.37f);
            }
        }.runTaskLater(AranarthCore.getInstance(), 1L);

        new BukkitRunnable() {
            @Override
            public void run() {
                loc.getWorld().playSound(loc, Sound.BLOCK_NOTE_BLOCK_SNARE, 0.75f, 0.54f);
                loc.getWorld().playSound(loc, Sound.BLOCK_NOTE_BLOCK_SNARE, 0.75f, 0.50f);
                loc.getWorld().playSound(loc, Sound.BLOCK_NOTE_BLOCK_SNARE, 0.75f, 0.46f);
                loc.getWorld().playSound(loc, Sound.BLOCK_NOTE_BLOCK_SNARE, 0.75f, 0.42f);
            }
        }.runTaskLater(AranarthCore.getInstance(), 2L);
    }

    private void spawnHalo(ClapWave wave) {
        Location center = wave.location;
        Vector forward = wave.direction;

        Vector worldUp = new Vector(0, 1, 0);
        if (Math.abs(forward.dot(worldUp)) > 0.99) {
            worldUp = new Vector(1, 0, 0);
        }
        Vector right = forward.clone().crossProduct(worldUp).normalize();
        Vector up = right.clone().crossProduct(forward).normalize();

        int points = Math.max(10, (int) (2 * Math.PI * HALO_RADIUS * 5));

        for (int i = 0; i < points; i++) {
            double angle = (2.0 * Math.PI / points) * i;
            Vector offset = right.clone().multiply(Math.cos(angle) * HALO_RADIUS)
                    .add(up.clone().multiply(Math.sin(angle) * HALO_RADIUS));
            Location point = center.clone().add(offset);
            point.getWorld().spawnParticle(Particle.DUST, point, 1, 0, 0, 0, 0, CLAP_DUST);
        }
    }

    private void checkCollisions(ClapWave wave) {
        Vector p1 = wave.prevLocation.toVector();
        Vector step = wave.location.toVector().subtract(p1);
        double stepLenSq = step.lengthSquared();

        for (LivingEntity entity : wave.location.getWorld().getLivingEntities()) {
            if (entity.equals(player)) {
                continue;
            }
            if (hitEntities.contains(entity.getUniqueId())) {
                continue;
            }

            Vector entityCenter = entity.getLocation().add(0, entity.getHeight() / 2.0, 0).toVector();

            double t = (stepLenSq > 0) ? entityCenter.clone().subtract(p1).dot(step) / stepLenSq : 0;
            t = Math.max(0, Math.min(1, t));

            Vector closest = p1.clone().add(step.clone().multiply(t));
            if (entityCenter.distanceSquared(closest) > HIT_RADIUS * HIT_RADIUS) {
                continue;
            }

            hitEntities.add(entity.getUniqueId());
            double damage = (entity instanceof Player) ? BASE_PLAYER_DAMAGE : BASE_PLAYER_DAMAGE * MOB_DAMAGE_MULTIPLIER;
            DamageHandler.damageEntity(entity, damage, this);
            entity.getWorld().playSound(entity.getLocation(), Sound.ENTITY_WARDEN_SONIC_BOOM, 0.7f, 1.8f);
        }
    }

    private static class ClapWave {
        Location location;
        Location prevLocation;
        final Vector direction;
        double distanceTraveled;

        ClapWave(Location start, Vector direction) {
            this.location = start.clone();
            this.prevLocation = start.clone();
            this.direction = direction.clone();
            this.distanceTraveled = 0;
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
        return "SonicClap";
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
        return "Unleash a sharp sonic clap that sends three waves of concussive force tearing through the air, " +
                "striking ny target caught in their path.\n" +
                ChatUtils.translateToColor("&fUsage: Left-Click");
    }

}
