package com.aearost.aranarthcore.abilities.waterbending.plantbending;

import com.aearost.aranarthcore.utils.AbilityConfig;
import com.aearost.aranarthcore.utils.AranarthBendingUtils;
import com.aearost.aranarthcore.utils.ChatUtils;
import com.aearost.aranarthcore.utils.DominionUtils;
import com.projectkorra.projectkorra.GeneralMethods;
import com.projectkorra.projectkorra.ability.AddonAbility;
import com.projectkorra.projectkorra.ability.PlantAbility;
import com.projectkorra.projectkorra.attribute.Attribute;
import com.projectkorra.projectkorra.util.DamageHandler;
import com.projectkorra.projectkorra.util.TempBlock;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

public class PetalShots extends PlantAbility implements AddonAbility {

    public enum Phase {SOURCING, READY}

    @Attribute(Attribute.COOLDOWN)
    private long cooldown;
    @Attribute(Attribute.DAMAGE)
    private double damage;
    @Attribute(Attribute.RANGE)
    private double range;
    @Attribute("MaxShots")
    private int maxShots;
    @Attribute("SourceRange")
    private double sourceRange;

    private double sourceAngle;
    private double speed;
    private double hitRadius;
    private long shotCooldown;
    private long readyDuration;
    private long minLineDuration;
    private long maxLineDuration;

    private Phase phase;
    private long sourceStartTime;
    private long lineDuration;
    private long readyStartTime;
    private long lastShotTime;
    private int shotsRemaining;
    private Location sourceLocation;
    private Particle.DustOptions lineDust;
    private Particle.DustOptions heldDust;
    private Particle.DustOptions shotDust;
    private List<TempBlock> sourceTempBlocks = new ArrayList<>();
    private final List<PetalShot> shots = new ArrayList<>();

    private static final double STEP = 0.25;
    private static final double LINE_PARTICLE_SPACING = 0.3;
    private static final double HELD_PETAL_RADIUS = 0.22;

    private static final Map<UUID, PetalShots> activeInstances = new HashMap<>();

    /**
     * A single in-flight petal, steered by the caster's look direction.
     */
    private static class PetalShot {
        final Location location;
        double distanceTraveled;

        PetalShot(Location origin) {
            this.location = origin.clone();
        }
    }

    public PetalShots(Player player) {
        super(player);

        if (!bPlayer.canBend(this)) {
            return;
        }

        cooldown = AbilityConfig.getLong("PetalShots.Cooldown");
        damage = AbilityConfig.getDouble("PetalShots.Damage");
        range = AbilityConfig.getDouble("PetalShots.Range");
        maxShots = AbilityConfig.getInt("PetalShots.Shots");
        speed = AbilityConfig.getDouble("PetalShots.Speed");
        hitRadius = AbilityConfig.getDouble("PetalShots.HitRadius");
        shotCooldown = AbilityConfig.getLong("PetalShots.ShotCooldown");
        sourceRange = AbilityConfig.getDouble("PetalShots.SourceRange");
        sourceAngle = AbilityConfig.getDouble("PetalShots.SourceAngle");
        readyDuration = AbilityConfig.getLong("PetalShots.ReadyDuration");
        minLineDuration = AbilityConfig.getLong("PetalShots.MinLineDuration");
        maxLineDuration = AbilityConfig.getLong("PetalShots.MaxLineDuration");

        Block flower = AranarthBendingUtils.findTargetedFlower(this, player, sourceRange, sourceAngle);
        if (flower == null) {
            return;
        }

        Color color = AranarthBendingUtils.getFlowerPetalColor(flower.getType());
        lineDust = new Particle.DustOptions(color, (float) AbilityConfig.getDouble("PetalShots.LinePetalSize"));
        heldDust = new Particle.DustOptions(color, (float) AbilityConfig.getDouble("PetalShots.HeldPetalSize"));
        shotDust = new Particle.DustOptions(color, (float) AbilityConfig.getDouble("PetalShots.ShotPetalSize"));

        sourceLocation = flower.getLocation().add(0.5, 0.5, 0.5);
        sourceTempBlocks = AranarthBendingUtils.breakFlower(flower);
        sourceLocation.getWorld().spawnParticle(Particle.DUST, sourceLocation, 12, 0.3, 0.3, 0.3, 0, lineDust);
        sourceLocation.getWorld().playSound(sourceLocation, Sound.BLOCK_PINK_PETALS_BREAK, 1.0f, 1.2f);

        // The further away the flower is, the longer its petals take to reach the player
        double distanceFraction = Math.min(1.0, sourceLocation.distance(getHandLocation()) / sourceRange);
        lineDuration = minLineDuration + Math.round((maxLineDuration - minLineDuration) * distanceFraction);

        phase = Phase.SOURCING;
        sourceStartTime = System.currentTimeMillis();
        shotsRemaining = maxShots;
        activeInstances.put(player.getUniqueId(), this);
        start();
    }

    @Override
    public void progress() {
        if (player.isDead() || !player.isOnline() || !player.getWorld().equals(sourceLocation.getWorld())) {
            if (phase == Phase.READY) {
                discardUnfiredShots();
            }
            remove();
            return;
        }

        switch (phase) {
            case SOURCING -> progressSourcing();
            case READY -> progressReady();
        }
    }

    private void progressSourcing() {
        if (!player.isSneaking() || !bPlayer.canBendIgnoreCooldowns(this)) {
            remove();
            return;
        }

        double fraction = (double) (System.currentTimeMillis() - sourceStartTime) / lineDuration;
        drawSourceLine(Math.min(1.0, fraction));

        if (fraction >= 1.0) {
            phase = Phase.READY;
            readyStartTime = System.currentTimeMillis();
            player.getWorld().playSound(player.getLocation(), Sound.BLOCK_AZALEA_LEAVES_PLACE, 1.0f, 1.4f);
        }
    }

    private void progressReady() {
        if (!bPlayer.canBendIgnoreCooldowns(this)
                || System.currentTimeMillis() - readyStartTime >= readyDuration) {
            discardUnfiredShots();
        }

        Vector lookDir = player.getEyeLocation().getDirection().normalize();
        shots.removeIf(shot -> progressShot(shot, lookDir));

        if (shotsRemaining <= 0 && shots.isEmpty()) {
            remove();
            return;
        }
        drawHeldPetals();
    }

    /**
     * Advances a petal by one tick's worth of movement in the caster's current look direction.
     *
     * @return Whether the petal should be removed.
     */
    private boolean progressShot(PetalShot shot, Vector lookDir) {
        World world = shot.location.getWorld();
        double remaining = speed;
        while (remaining > 0) {
            double step = Math.min(STEP, remaining);
            shot.location.add(lookDir.clone().multiply(step));
            shot.distanceTraveled += step;
            remaining -= step;

            Block block = shot.location.getBlock();
            if (shot.distanceTraveled > range || (block.getType().isSolid() && !block.isPassable())) {
                return true;
            }

            world.spawnParticle(Particle.DUST, shot.location, 1, 0, 0, 0, 0, shotDust);

            for (Entity entity : GeneralMethods.getEntitiesAroundPoint(shot.location, hitRadius)) {
                if (!(entity instanceof LivingEntity living) || entity instanceof ArmorStand
                        || entity.getUniqueId().equals(player.getUniqueId())) {
                    continue;
                }
                if (living instanceof Player targetPlayer && !DominionUtils.canAttackPlayer(player, targetPlayer)) {
                    continue;
                }
                DamageHandler.damageEntity(living, damage, this);
                world.spawnParticle(Particle.DUST, shot.location, 8, 0.2, 0.2, 0.2, 0, shotDust);
                world.playSound(shot.location, Sound.BLOCK_PINK_PETALS_BREAK, 0.8f, 1.6f);
                return true;
            }
        }
        return false;
    }

    /**
     * Draws the line of petals as it grows from the flower to the player's hand.
     *
     * @param fraction How far along the line has travelled [0, 1].
     */
    private void drawSourceLine(double fraction) {
        Vector toHand = getHandLocation().toVector().subtract(sourceLocation.toVector());
        double drawLength = toHand.length() * fraction;
        if (drawLength < 0.1) {
            return;
        }
        Vector dir = toHand.normalize();
        World world = sourceLocation.getWorld();
        for (double d = 0; d < drawLength; d += LINE_PARTICLE_SPACING) {
            world.spawnParticle(Particle.DUST, sourceLocation.clone().add(dir.clone().multiply(d)),
                    1, 0, 0, 0, 0, lineDust);
        }
    }

    /**
     * Circles one petal around the player's hand for each shot remaining.
     */
    private void drawHeldPetals() {
        Location hand = getHandLocation();
        double time = System.currentTimeMillis() / 300.0;
        for (int i = 0; i < shotsRemaining; i++) {
            double angle = time + i * Math.PI * 2 / maxShots;
            Location petal = hand.clone().add(
                    HELD_PETAL_RADIUS * Math.cos(angle), 0, HELD_PETAL_RADIUS * Math.sin(angle));
            player.getWorld().spawnParticle(Particle.DUST, petal, 1, 0, 0, 0, 0, heldDust);
        }
    }

    /**
     * Fires one petal once the line has reached the player, regardless of whether they are still sneaking.
     */
    public void onLeftClick() {
        if (phase != Phase.READY || shotsRemaining <= 0) {
            return;
        }

        long now = System.currentTimeMillis();
        if (now - lastShotTime < shotCooldown) {
            return;
        }
        lastShotTime = now;

        Location origin = getHandLocation();
        shots.add(new PetalShot(origin));
        shotsRemaining--;
        if (shotsRemaining <= 0) {
            bPlayer.addCooldown(this);
        }

        float pitch = 0.8f + ThreadLocalRandom.current().nextFloat() * 0.6f;
        origin.getWorld().playSound(origin, Sound.ENTITY_BREEZE_LAND, 1.0f, pitch);
        origin.getWorld().playSound(origin, Sound.BLOCK_PINK_PETALS_BREAK, 1.0f, pitch);
    }

    /**
     * Cancels the ability if the line has not reached the player yet, with no cooldown as no shots were granted.
     */
    public void onSneakRelease() {
        if (phase == Phase.SOURCING) {
            remove();
        }
    }

    /**
     * Cancels the ability while sourcing, otherwise loses the unfired shots and leaves in-flight petals to finish.
     */
    public void cancelFromSlotChange() {
        if (phase == Phase.SOURCING) {
            remove();
        } else {
            discardUnfiredShots();
        }
    }

    /**
     * Loses any unfired shots and starts the cooldown, which has already begun if the last shot was fired.
     */
    private void discardUnfiredShots() {
        if (shotsRemaining > 0) {
            shotsRemaining = 0;
            bPlayer.addCooldown(this);
        }
    }

    private Location getHandLocation() {
        return GeneralMethods.getRightSide(player.getLocation(), 0.55)
                .add(0, player.getEyeHeight() - 0.5, 0);
    }

    public static boolean hasActiveInstance(UUID uuid) {
        return activeInstances.containsKey(uuid);
    }

    public static PetalShots getActiveInstance(UUID uuid) {
        return activeInstances.get(uuid);
    }

    public Phase getPhase() {
        return phase;
    }

    @Override
    public void remove() {
        super.remove();
        AranarthBendingUtils.revertFlower(sourceTempBlocks);
        shots.clear();
        activeInstances.remove(player.getUniqueId());
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
    public String getName() {
        return "PetalShots";
    }

    @Override
    public Location getLocation() {
        return player.getLocation();
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
        return "Bend the water in a flower's petals, and rapidly fire them at your target. " +
                "The fired petals are fully controllable, with the petals taking on the color of their flower.\n" +
                ChatUtils.translateToColor("&fUsage: Hold Sneak (at a flower, until its petals reach you) > Left-click (up to 5 shots)");
    }
}
