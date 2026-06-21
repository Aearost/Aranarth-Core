package com.aearost.aranarthcore.abilities.firebending.lightningbending;

import com.aearost.aranarthcore.utils.AranarthBendingUtils;
import com.aearost.aranarthcore.utils.ChatUtils;
import com.projectkorra.projectkorra.GeneralMethods;
import com.projectkorra.projectkorra.ProjectKorra;
import com.projectkorra.projectkorra.ability.AddonAbility;
import com.projectkorra.projectkorra.ability.LightningAbility;
import com.projectkorra.projectkorra.attribute.Attribute;
import com.projectkorra.projectkorra.region.RegionProtection;
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
import java.util.Map;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

public class LightningBurst extends LightningAbility implements AddonAbility {

    private static final Map<UUID, LightningBurst> activeInstances = new HashMap<>();

    private static final long COOLDOWN = 12000L;
    private static final long CHARGE_UP = 1750L;
    private static final long AVATAR_COOLDOWN = 1000L;
    private static final long AVATAR_CHARGE_UP = 750L;
    private static final double RADIUS = 12.0;
    private static final double DAMAGE = 8.0;
    private static final float SOUND_VOLUME = 0.6f;
    private static final int SOUND_INTERVAL = 6;
    private static final double STUN_CHANCE = 0.25;
    private static final long STUN_DURATION_MS = 1000L;

    private final Random rand = new Random();

    @Attribute(Attribute.COOLDOWN)
    private long cooldown;
    @Attribute(Attribute.CHARGE_DURATION)
    private long chargeUp;

    private boolean charged;
    private double chargeParticleAngle = 0;

    public LightningBurst(final Player player) {
        super(player);
        if (!bPlayer.canBend(this) || hasAbility(player, LightningBurst.class)) {
            return;
        }

        this.cooldown = COOLDOWN;
        this.chargeUp = CHARGE_UP;
        this.charged = false;

        if (bPlayer.isAvatarState()) {
            this.chargeUp = AVATAR_CHARGE_UP;
            this.cooldown = AVATAR_COOLDOWN;
        }

        start();
        activeInstances.put(player.getUniqueId(), this);
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
        if (RegionProtection.isRegionProtected(player, player.getLocation(), this)) {
            remove();
            return;
        }

        if (!player.isSneaking()) {
            if (this.charged) {
                final Location fake = player.getLocation().add(0, -2, 0);
                fake.setPitch(0);
                for (int i = -180; i < 180; i += 55) {
                    fake.setYaw(i);
                    for (double j = -180; j <= 180; j += 55) {
                        final Location temp = fake.clone();
                        Vector dir = fake.getDirection().clone().multiply(2 * Math.cos(Math.toRadians(j)));
                        temp.add(dir);
                        temp.setY(temp.getY() + 2 + (2 * Math.sin(Math.toRadians(j))));
                        dir = GeneralMethods.getDirection(player.getLocation().add(0, 0, 0), temp);
                        spawnBolt(player.getLocation().clone().add(0, 1, 0).setDirection(dir), RADIUS, 1, 20, true);
                    }
                }
                bPlayer.addCooldown(this);
            }
            remove();
        } else if (System.currentTimeMillis() > getStartTime() + chargeUp) {
            this.charged = true;
            displayCharging();
        } else {
            chargeParticleAngle += 0.3;
            final Location center = player.getLocation().clone().add(0, 1, 0);
            for (int offset = 0; offset < 2; offset++) {
                final double angle = chargeParticleAngle + (Math.PI * offset);
                final Location particleLoc = center.clone().add(0.6 * Math.cos(angle), 0, 0.6 * Math.sin(angle));
                final Particle.DustOptions chargeDust = ThreadLocalRandom.current().nextBoolean()
                        ? AranarthBendingUtils.LIGHTNING_DUST_BRIGHT
                        : AranarthBendingUtils.LIGHTNING_DUST_BLUE;
                particleLoc.getWorld().spawnParticle(Particle.DUST, particleLoc, 1, 0, 0, 0, 0, chargeDust);
                if (ThreadLocalRandom.current().nextDouble() < 0.15) {
                    particleLoc.getWorld().spawnParticle(Particle.ELECTRIC_SPARK, particleLoc, 1, 0.05, 0.05, 0.05, 0);
                }
            }
            if (ThreadLocalRandom.current().nextDouble() < 0.2) {
                playLightningbendingChargingSound(player.getLocation());
            }
        }
    }

    private void spawnBolt(final Location location, final double max, final double gap, final int arc, final boolean doDamage) {
        new Bolt(location, max, gap, arc, doDamage).runTaskTimer(ProjectKorra.plugin, 0L, 1L);
    }

    private void displayCharging() {
        final Location fake = player.getLocation().clone();
        fake.setPitch(0);
        for (int i = -180; i < 180; i += 55) {
            fake.setYaw(i);
            for (double j = -180; j <= 180; j += 55) {
                if (rand.nextInt(100) == 0) {
                    final Location temp = fake.clone();
                    Vector dir = fake.getDirection().clone().multiply(1.2 * Math.cos(Math.toRadians(j)));
                    temp.add(dir);
                    temp.setY(temp.getY() + 1.2 + (1.2 * Math.sin(Math.toRadians(j))));
                    dir = GeneralMethods.getDirection(temp, player.getLocation().add(0, 1, 0));
                    spawnBolt(temp.clone().setDirection(dir), 1, 0.2, 20, false);
                }
            }
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
        return "LightningBurst";
    }

    @Override
    public boolean isHarmlessAbility() {
        return false;
    }

    @Override
    public boolean isSneakAbility() {
        return true;
    }

    @Override
    public String getAuthor() {
        return "jedk1, Cozmyc (Maintainer), Aearost (Maintainer)";
    }

    @Override
    public String getVersion() {
        return "1.0";
    }

    @Override
    public String getDescription() {
        return "Release a powerful electrical outburst of energy in a sphere surrounding you.\n"
                + ChatUtils.translateToColor("&fUsage: Hold Sneak (charge) > Release Sneak (burst)");
    }

    @Override
    public void remove() {
        activeInstances.remove(player.getUniqueId());
        super.remove();
    }

    @Override
    public void load() {}

    @Override
    public void stop() {}

    public static boolean hasActiveInstance(final UUID uuid) {
        return activeInstances.containsKey(uuid);
    }

    public boolean isCharged() {
        return charged;
    }

    public void setCharged(final boolean charged) {
        this.charged = charged;
    }

    public class Bolt extends BukkitRunnable {

        private Location location;
        private final float initYaw;
        private final float initPitch;
        private double step;
        private final double max;
        private final double gap;
        private final int arc;
        private final boolean doDamage;

        public Bolt(final Location location, final double max, final double gap, final int arc, final boolean doDamage) {
            this.location = location.clone();
            this.max = max;
            this.gap = gap;
            this.arc = arc;
            this.doDamage = doDamage;
            this.initYaw = location.getYaw();
            this.initPitch = location.getPitch();
        }

        @Override
        public void run() {
            if (step >= max) {
                cancel();
                return;
            }
            if (RegionProtection.isRegionProtected(player, location, LightningBurst.this) || !isTransparent(location.getBlock())) {
                cancel();
                return;
            }

            final double stepSize = 0.2;
            for (double i = 0; i < gap; i += stepSize) {
                step += stepSize;
                location = location.add(location.getDirection().clone().multiply(stepSize));
                final Particle.DustOptions boltDust = ThreadLocalRandom.current().nextBoolean()
                        ? AranarthBendingUtils.LIGHTNING_DUST_BRIGHT
                        : AranarthBendingUtils.LIGHTNING_DUST_BLUE;
                location.getWorld().spawnParticle(Particle.DUST, location, 1, 0, 0, 0, 0, boltDust);
                if (ThreadLocalRandom.current().nextDouble() < 0.3) {
                    location.getWorld().spawnParticle(Particle.ELECTRIC_SPARK, location, 1, 0, 0, 0, 0);
                }
                emitFirebendingLight(location);
            }

            switch (rand.nextInt(3)) {
                case 0 -> location.setYaw(initYaw - arc);
                case 1 -> location.setYaw(initYaw + arc);
                default -> location.setYaw(initYaw);
            }
            switch (rand.nextInt(3)) {
                case 0 -> location.setPitch(initPitch - arc);
                case 1 -> location.setPitch(initPitch + arc);
                default -> location.setPitch(initPitch);
            }

            if (rand.nextInt(SOUND_INTERVAL) == 0) {
                location.getWorld().playSound(location, Sound.ENTITY_BEE_HURT, SOUND_VOLUME, 0.2f);
            }

            if (doDamage) {
                for (final Entity entity : GeneralMethods.getEntitiesAroundPoint(location, 2)) {
                    if (entity instanceof LivingEntity lent && entity.getEntityId() != player.getEntityId()) {
                        DamageHandler.damageEntity(lent, DAMAGE, LightningBurst.this);
                        AranarthBendingUtils.applyElectrocution(lent, STUN_DURATION_MS, STUN_CHANCE);
                    }
                }
            }
        }
    }
}
