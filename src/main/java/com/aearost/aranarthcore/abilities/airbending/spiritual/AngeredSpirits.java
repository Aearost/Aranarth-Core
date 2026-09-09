package com.aearost.aranarthcore.abilities.airbending.spiritual;

import com.aearost.aranarthcore.utils.AranarthBendingUtils;
import com.aearost.aranarthcore.utils.ChatUtils;
import com.aearost.aranarthcore.utils.DominionUtils;
import com.projectkorra.projectkorra.GeneralMethods;
import com.projectkorra.projectkorra.ability.AddonAbility;
import com.projectkorra.projectkorra.ability.SpiritualAbility;
import com.projectkorra.projectkorra.attribute.Attribute;
import com.projectkorra.projectkorra.util.DamageHandler;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Vex;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public class AngeredSpirits extends SpiritualAbility implements AddonAbility {

    private record ShotType(Color color, PotionEffectType effectType, int amplifier) {
    }

    private static final ShotType[] SHOT_TYPES = {
            new ShotType(AranarthBendingUtils.SPIRIT_COLORS[0], AranarthBendingUtils.SPIRIT_EFFECT_TYPES[0], AranarthBendingUtils.SPIRIT_EFFECT_AMPLIFIER),
            new ShotType(AranarthBendingUtils.SPIRIT_COLORS[1], AranarthBendingUtils.SPIRIT_EFFECT_TYPES[1], AranarthBendingUtils.SPIRIT_EFFECT_AMPLIFIER),
            new ShotType(AranarthBendingUtils.SPIRIT_COLORS[2], AranarthBendingUtils.SPIRIT_EFFECT_TYPES[2], AranarthBendingUtils.SPIRIT_EFFECT_AMPLIFIER),
            new ShotType(AranarthBendingUtils.SPIRIT_COLORS[3], AranarthBendingUtils.SPIRIT_EFFECT_TYPES[3], AranarthBendingUtils.SPIRIT_EFFECT_AMPLIFIER),
    };

    private static final double HIT_RADIUS = 1.5;
    private static final double STEP_SIZE = 0.1;
    private static final long FIRE_WINDOW = 2500L;
    private static final int MAX_SHOTS = 4;
    private static final double MIN_RANGE = 16.0;
    private static final double MAX_RANGE = 24.0;

    // {rightComponent, yComponent, forwardComponent} - indices 0-1 are left shoulder, 2-3 are right shoulder
    private static final double[][] SHOULDER_OFFSETS = {
            {-0.35, 1.50,  0.10},
            {-0.35, 1.40, -0.05},
            { 0.35, 1.50,  0.10},
            { 0.35, 1.40, -0.05},
    };

    private static final Map<UUID, AngeredSpirits> activeInstances = new HashMap<>();

    @Attribute(Attribute.COOLDOWN)
    private long cooldown;
    @Attribute(Attribute.CHARGE_DURATION)
    private long chargeDuration;
    @Attribute(Attribute.DAMAGE)
    private double damage;
    @Attribute(Attribute.SPEED)
    private double speed;

    private enum State {CHARGING, READY, FIRING}

    private State state;
    private int nextShotIndex;
    private long fireWindowStart;
    private final int heldSlot;
    private final int[] shotOrder;
    private final List<SpiritProjectile> activeProjectiles;
    private Location[] spiritSources;
    private int[] shoulderAssignment;
    private double[] wobbleOffset;

    public AngeredSpirits(final Player player) {
        super(player);

        this.activeProjectiles = new ArrayList<>();
        this.cooldown = 6000L;
        this.chargeDuration = 1000L;
        this.damage = 2.0;
        this.speed = 2.0;

        this.state = State.CHARGING;
        this.nextShotIndex = 0;
        this.fireWindowStart = 0L;
        this.heldSlot = player.getInventory().getHeldItemSlot();

        // Build a randomized fire order via Fisher-Yates shuffle
        final int[] order = {0, 1, 2, 3};
        for (int i = order.length - 1; i > 0; i--) {
            final int j = (int) (Math.random() * (i + 1));
            final int tmp = order[i];
            order[i] = order[j];
            order[j] = tmp;
        }
        this.shotOrder = order;

        if (!bPlayer.canBend(this)) {
            return;
        }

        initSpiritSources();
        start();
        activeInstances.put(player.getUniqueId(), this);
    }

    private void initSpiritSources() {
        final Location start = player.getLocation();
        spiritSources = new Location[4];
        wobbleOffset = new double[4];

        // Randomly assign 2 spirits to each shoulder side
        shoulderAssignment = new int[]{0, 1, 2, 3};
        for (int i = 3; i > 0; i--) {
            final int j = (int) (Math.random() * (i + 1));
            final int tmp = shoulderAssignment[i];
            shoulderAssignment[i] = shoulderAssignment[j];
            shoulderAssignment[j] = tmp;
        }

        // Place sources in 4 quadrants around the player with jitter
        for (int i = 0; i < 4; i++) {
            final double baseAngle = (i / 4.0) * 2 * Math.PI;
            final double angle = baseAngle + (Math.random() - 0.5) * Math.PI * 0.6;
            final double dist = 6.0 + Math.random() * 4.0;
            final double height = (Math.random() - 0.3) * 3.0 + 1.0;
            spiritSources[i] = start.clone().add(Math.cos(angle) * dist, height, Math.sin(angle) * dist);
            wobbleOffset[i] = Math.random() * 2 * Math.PI;
        }
    }

    @Override
    public void progress() {
        if (player.isDead() || !player.isOnline()) {
            remove();
            return;
        }

        advanceProjectiles();

        switch (state) {
            case CHARGING -> handleCharging();
            case READY -> handleReady();
            case FIRING -> handleFiring();
        }
    }

    private void handleCharging() {
        if (!player.isSneaking()) {
            remove();
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
        } else {
            spawnChargeParticles(elapsed);
        }
    }

    private void handleReady() {
        if (player.getInventory().getHeldItemSlot() != heldSlot) {
            remove(); // No cooldown - player never fired
            return;
        }

        spawnReadyParticles();

        // Sneak released → open the firing window
        if (!player.isSneaking()) {
            state = State.FIRING;
            fireWindowStart = System.currentTimeMillis();
        }
    }

    private void handleFiring() {
        if (player.getInventory().getHeldItemSlot() != heldSlot) {
            bPlayer.addCooldown(this);
            remove();
            return;
        }

        if (System.currentTimeMillis() - fireWindowStart >= FIRE_WINDOW) {
            bPlayer.addCooldown(this);
            remove();
            return;
        }

        spawnReadyParticles();

        if (nextShotIndex >= MAX_SHOTS && activeProjectiles.isEmpty()) {
            bPlayer.addCooldown(this);
            remove();
        }
    }

    /**
     * Called by AranarthCoreBendingListener when the player left-clicks.
     * Fires the next spirit projectile during the firing window.
     */
    public void onLeftClick() {
        if (state != State.FIRING || nextShotIndex >= MAX_SHOTS) {
            return;
        }

        final ShotType shotType = SHOT_TYPES[shotOrder[nextShotIndex]];
        nextShotIndex++;

        final double range = MIN_RANGE + Math.random() * (MAX_RANGE - MIN_RANGE);
        final Location eyeLoc = player.getEyeLocation();
        final Vector direction = eyeLoc.getDirection().normalize();

        activeProjectiles.add(new SpiritProjectile(eyeLoc.clone(), direction, shotType, range));
        player.getWorld().playSound(eyeLoc, Sound.ENTITY_VEX_AMBIENT, 1.0f, 1.1f + nextShotIndex * 0.1f);
    }

    /**
     * Called by AranarthCoreBendingListener on slot change.
     */
    public void onSlotChange() {
        if (state == State.FIRING) {
            bPlayer.addCooldown(this);
        }
        remove();
    }

    private void advanceProjectiles() {
        final Iterator<SpiritProjectile> it = activeProjectiles.iterator();
        while (it.hasNext()) {
            final SpiritProjectile proj = it.next();
            if (!proj.advance()) {
                proj.cleanup();
                it.remove();
            }
        }
    }

    private void spawnChargeParticles(final long elapsed) {
        if (spiritSources == null) return;
        final double progress = (double) elapsed / chargeDuration;

        // Player-local axes for shoulder placement
        final Location playerLoc = player.getLocation();
        final Vector forward = playerLoc.getDirection().clone().setY(0);
        if (forward.lengthSquared() > 0.001) {
            forward.normalize();
        } else {
            forward.setZ(1);
        }
        final Vector right = new Vector(0, 1, 0).crossProduct(forward).normalize();

        // Reveal one spirit at a time
        final int activeCount = Math.min((int) (progress * 4) + 1, 4);
        for (int c = 0; c < activeCount; c++) {
            final double spiritAppearAt = c / 4.0;
            final double spiritDuration = 1.0 - spiritAppearAt;
            final double spiritProgress = spiritDuration > 0
                    ? Math.min(1.0, (progress - spiritAppearAt) / spiritDuration)
                    : 1.0;

            final ShotType shotType = SHOT_TYPES[shotOrder[c]];
            final double[] off = SHOULDER_OFFSETS[shoulderAssignment[c]];

            // Shoulder position in world space
            final Location shoulder = playerLoc.clone().add(
                    right.getX() * off[0] + forward.getX() * off[2],
                    off[1],
                    right.getZ() * off[0] + forward.getZ() * off[2]);

            final Vector srcVec = spiritSources[c].toVector();
            final Vector toShoulder = shoulder.toVector().subtract(srcVec);
            final Vector pathDir = toShoulder.lengthSquared() > 0.001
                    ? toShoulder.clone().normalize()
                    : new Vector(0, 1, 0);

            // Two perpendicular axes for 3D wobble
            final Vector perp1 = computePerpendicular(pathDir);
            final Vector perp2 = pathDir.clone().crossProduct(perp1).normalize();

            // Draw a trailing blob
            final double tailT = Math.max(0.0, spiritProgress - 0.35);

            final int numParticles = 6;
            for (int p = 0; p < numParticles; p++) {
                final double t = numParticles > 1
                        ? tailT + (spiritProgress - tailT) * (p / (double) (numParticles - 1))
                        : spiritProgress;

                // Path position + 3D sine/cosine wobble that dampens to zero at the shoulder
                final Vector particlePos = srcVec.clone().add(toShoulder.clone().multiply(t));
                final double wobbleAmp = 0.30 * (1.0 - t * t);
                final double w1 = Math.sin(t * 5.0 * Math.PI + wobbleOffset[c]) * wobbleAmp;
                final double w2 = Math.cos(t * 3.5 * Math.PI + wobbleOffset[c] * 1.3) * wobbleAmp * 0.6;
                particlePos.add(perp1.clone().multiply(w1)).add(perp2.clone().multiply(w2));

                // Size grows from 0.2 (at source) to 1.2 (at shoulder)
                final float size = 0.2f + (float) (t * 1.0f);
                final Particle.DustOptions dust = new Particle.DustOptions(shotType.color(), size);
                spiritSources[c].getWorld().spawnParticle(
                        Particle.DUST, particlePos.toLocation(spiritSources[c].getWorld()), 1, 0, 0, 0, 0, dust);
            }
        }
    }

    private static Vector computePerpendicular(final Vector v) {
        final Vector arbitrary = Math.abs(v.getX()) < 0.9 ? new Vector(1, 0, 0) : new Vector(0, 1, 0);
        return v.getCrossProduct(arbitrary).normalize();
    }

    /**
     * One-time burst of colored particles to signal the charge is complete.
     */
    private void spawnChargeCompleteBurst() {
        final Location center = player.getLocation().add(0, 1.0, 0);
        for (int i = 0; i < MAX_SHOTS; i++) {
            final Particle.DustOptions dust = new Particle.DustOptions(SHOT_TYPES[shotOrder[i]].color(), 1.5f);
            for (int j = 0; j < 6; j++) {
                final double angle = (j / 6.0) * 2 * Math.PI;
                final Location loc = center.clone()
                        .add(Math.cos(angle) * 0.9, 0, Math.sin(angle) * 0.9);
                loc.getWorld().spawnParticle(Particle.DUST, loc, 3, 0.1, 0.1, 0.1, 0, dust);
            }
        }
    }

    /**
     * Orbiting colored orbs showing remaining shots.
     */
    private void spawnReadyParticles() {
        final double time = System.currentTimeMillis() / 500.0;
        for (int i = 0; i < MAX_SHOTS; i++) {
            if (i < nextShotIndex) {
                continue;
            }
            final Particle.DustOptions dust = new Particle.DustOptions(SHOT_TYPES[shotOrder[i]].color(), 1.2f);
            final double angle = (i / (double) MAX_SHOTS) * 2 * Math.PI + time;
            final Location loc = player.getLocation().add(0, 1.2, 0)
                    .add(Math.cos(angle) * 0.8, 0.25 * Math.sin(time + i), Math.sin(angle) * 0.8);
            loc.getWorld().spawnParticle(Particle.DUST, loc, 3, 0.05, 0.05, 0.05, 0, dust);
        }
    }

    @Override
    public void remove() {
        super.remove();
        activeInstances.remove(player.getUniqueId());
        for (final SpiritProjectile proj : activeProjectiles) {
            proj.cleanup();
        }
        activeProjectiles.clear();
    }

    // -------------------------------------------------------------------------
    // Static registry
    // -------------------------------------------------------------------------

    public static boolean hasActiveInstance(UUID uuid) {
        return activeInstances.containsKey(uuid);
    }

    public static AngeredSpirits getActiveInstance(UUID uuid) {
        return activeInstances.get(uuid);
    }

    // -------------------------------------------------------------------------
    // PK ability interface
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
        return "AngeredSpirits";
    }

    @Override
    public void load() {
    }

    @Override
    public void stop() {
        if (player != null) {
            activeInstances.remove(player.getUniqueId());
        }
        if (activeProjectiles == null) {
            return;
        }
        for (final SpiritProjectile proj : activeProjectiles) {
            proj.cleanup();
        }
        activeProjectiles.clear();
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
        return "Channel the wrath of restless spirits, summoning four to your side. " +
                "Once charged, release sneak and unleash the spirits one at a time, each applying a different negative potion effect.\n" +
                ChatUtils.translateToColor("&fUsage: Sneak (Hold) > Sneak (Release) > Left-click (4 times)");
    }

    // -----------------------------------------------------------------------------------------------------------------

    private class SpiritProjectile {

        private final Location location;
        private final Vector direction;
        private final ShotType shotType;
        private final double range;
        private double distanceTraveled;
        private Vex vex;
        private final Set<UUID> hitEntities;

        SpiritProjectile(final Location startLocation, final Vector direction, final ShotType shotType, final double range) {
            this.location = startLocation.clone();
            this.direction = direction.clone().normalize();
            this.shotType = shotType;
            this.range = range;
            this.distanceTraveled = 0;
            this.hitEntities = new HashSet<>();

            this.vex = (Vex) startLocation.getWorld().spawnEntity(startLocation, EntityType.VEX);
            this.vex.setAI(false);
            this.vex.setGravity(false);
            this.vex.setPersistent(false);
            this.vex.setInvulnerable(true);
            this.vex.setSilent(true);
        }

        boolean advance() {
            if (vex == null || vex.isDead()) {
                return false;
            }

            final int steps = (int) (speed / STEP_SIZE);
            for (int i = 0; i < steps; i++) {
                if (distanceTraveled >= range) {
                    return false;
                }

                location.add(direction.clone().multiply(STEP_SIZE));
                distanceTraveled += STEP_SIZE;

                if (GeneralMethods.isRegionProtectedFromBuild(AngeredSpirits.this, location)) {
                    return false;
                }

                if (!isTransparent(location.getBlock())) {
                    if (!isTransparent(location.clone().add(0, 0.2, 0).getBlock())) {
                        return false;
                    }
                }

                final Particle.DustOptions dust = new Particle.DustOptions(shotType.color(), 0.8f);
                if (i % 2 == 0) {
                    location.getWorld().spawnParticle(Particle.DUST, location, 2, 0.1, 0.1, 0.1, 0, dust);
                }
                if (i % 3 == 0) {
                    location.getWorld().spawnParticle(Particle.SOUL, location, 1, 0.05, 0.05, 0.05, 0);
                }

                checkCollisions();
            }

            vex.teleport(location);
            return true;
        }

        private void checkCollisions() {
            for (final LivingEntity entity : location.getWorld().getLivingEntities()) {
                if (entity.equals(player)) {
                    continue;
                }
                if (entity.equals(vex)) {
                    continue;
                }
                if (hitEntities.contains(entity.getUniqueId())) {
                    continue;
                }

                final Location entityCenter = entity.getLocation().add(0, entity.getHeight() / 2.0, 0);
                if (entityCenter.distance(location) <= HIT_RADIUS) {
                    hitEntities.add(entity.getUniqueId());
                    applyHitEffects(entity);
                }
            }
        }

        private void applyHitEffects(final LivingEntity entity) {
            if (entity instanceof Player targetPlayer && !DominionUtils.canAttackPlayer(player, targetPlayer)) {
                return;
            }
            entity.addPotionEffect(new PotionEffect(shotType.effectType(), AranarthBendingUtils.SPIRIT_EFFECT_DURATION, shotType.amplifier(), false, true, true));
            double actualDamage = Math.random() < 0.5 ? 2.0 : 3.0;
            DamageHandler.damageEntity(entity, actualDamage, AngeredSpirits.this);
            entity.getWorld().playSound(entity.getLocation(), Sound.ENTITY_VEX_HURT, 1.0f, 1.2f);
        }

        void cleanup() {
            if (vex != null && !vex.isDead()) {
                vex.remove();
                vex = null;
            }
        }
    }
}
