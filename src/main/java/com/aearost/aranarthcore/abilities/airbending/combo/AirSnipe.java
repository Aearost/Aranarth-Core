package com.aearost.aranarthcore.abilities.airbending.combo;

import com.aearost.aranarthcore.utils.AranarthBendingUtils;
import com.aearost.aranarthcore.utils.ChatUtils;
import com.projectkorra.projectkorra.GeneralMethods;
import com.projectkorra.projectkorra.ability.AirAbility;
import com.projectkorra.projectkorra.ability.AddonAbility;
import com.projectkorra.projectkorra.ability.ComboAbility;
import com.projectkorra.projectkorra.ability.util.ComboManager.AbilityInformation;
import com.projectkorra.projectkorra.attribute.Attribute;
import com.projectkorra.projectkorra.util.ClickType;
import com.projectkorra.projectkorra.util.DamageHandler;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.MainHand;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public class AirSnipe extends AirAbility implements AddonAbility, ComboAbility {

    public enum Phase { CHARGING, READY, FIRING }

    private static final double HIT_RADIUS = 0.4;
    private static final double STEP_SIZE = 0.25;
    private static final Map<UUID, AirSnipe> ACTIVE = new HashMap<>();

    @Attribute(Attribute.COOLDOWN)
    private long cooldown;
    @Attribute(Attribute.DAMAGE)
    private double damage;
    @Attribute(Attribute.RANGE)
    private double range;
    @Attribute(Attribute.SPEED)
    private double speed;

    private Phase phase;
    private long chargeTime;
    private long chargeStartTime;
    private double animAngle;

    // Used only during FIRING
    private Location shotLocation;
    private Vector direction;
    private double distanceTraveled;
    private final Set<LivingEntity> hitEntities = new HashSet<>();

    public AirSnipe(final Player player) {
        super(player);

        if (!this.bPlayer.canBendIgnoreBindsCooldowns(this)) {
            return;
        }
        if (this.bPlayer.isOnCooldown(this)) {
            return;
        }
        if (ACTIVE.containsKey(player.getUniqueId())) {
            return;
        }

        // Read AirSwipe's configured charge time so this mirrors that ability's visual cue
        this.chargeTime = getConfig().getLong("Abilities.Air.AirSwipe.MaxChargeTime", 1500L);

        // Suppress the AirSwipe that just started on this same SHIFT_DOWN so it doesn't also run
        AranarthBendingUtils.suppressComboTrigger(this.bPlayer, player, "AirSwipe");

        this.cooldown = 9000L;
        this.damage = 8.0;
        this.range = 40.0;
        this.speed = 20.0;

        this.phase = Phase.CHARGING;
        this.chargeStartTime = System.currentTimeMillis();
        this.animAngle = 0;

        ACTIVE.put(player.getUniqueId(), this);
        this.start();
    }

    /**
     * Called by the bending listener when the player left-clicks with AirPunch while READY.
     */
    public void fire() {
        if (this.phase != Phase.READY) {
            return;
        }
        this.phase = Phase.FIRING;
        this.direction = player.getEyeLocation().getDirection().normalize();
        this.shotLocation = player.getEyeLocation().clone();

        this.bPlayer.addCooldown(this);

        // Loud, bullet-like firing sound
        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_BREEZE_LAND, 2.0f, 2.0f);
        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_ARROW_SHOOT, 1.5f, 1.6f);

        // Burst of dust particles extending from the hand toward the shot direction
        Location hand = getHandPosition();
        for (int i = 0; i < 8; i++) {
            Location burstLoc = hand.clone().add(this.direction.clone().multiply(i * 0.25));
            hand.getWorld().spawnParticle(Particle.DUST, burstLoc, 2, 0.03, 0.03, 0.03, 0,
                    new Particle.DustOptions(Color.WHITE, 0.8f));
            if (i % 3 == 0) {
                hand.getWorld().spawnParticle(Particle.CRIT, burstLoc,
                        1, 0.04, 0.04, 0.04, 0);
            }
        }

        // AirPunch fires on the same left-click - suppress it so the player isn't penalised
        AranarthBendingUtils.suppressComboTrigger(this.bPlayer, player, "AirPunch");
    }

    @Override
    public void progress() {
        if (player.isDead() || !player.isOnline()) {
            this.remove();
            return;
        }

        switch (this.phase) {
            case CHARGING -> progressCharging();
            case READY    -> progressReady();
            case FIRING   -> progressFiring();
        }
    }

    /**
     * Returns the player's actual hand position.
     */
    private Location getHandPosition() {
        double yawRad = Math.toRadians(player.getLocation().getYaw());
        // Clockwise 90 degrees from facing in XZ - the right-arm direction
        Vector armDir = new Vector(-Math.cos(yawRad), 0, -Math.sin(yawRad));
        if (player.getMainHand() != MainHand.RIGHT) armDir.multiply(-1);
        double handY = player.isSneaking() ? 0.5 : 0.9;
        return player.getLocation().clone().add(armDir.multiply(0.35)).add(0, handY, 0);
    }

    /**
     * Spawns a single white dust particle with a given size directly at loc.
     */
    private void spawnVortexParticle(final Location loc, final float size) {
        loc.getWorld().spawnParticle(Particle.DUST, loc, 1, 0, 0, 0, 0,
                new Particle.DustOptions(Color.WHITE, size));
    }

    private void progressCharging() {
        if (!player.isSneaking()) {
            this.remove();
            return;
        }

        double chargeProgress = Math.min(
                (double) (System.currentTimeMillis() - this.chargeStartTime) / this.chargeTime, 1.0);

        Location hand = getHandPosition();
        double yaw = Math.toRadians(player.getLocation().getYaw());
        Vector up   = new Vector(0, 1, 0);
        Vector side = new Vector(-Math.cos(yaw), 0, -Math.sin(yaw));

        // Density increases and particle size decreases as charge fills
        int arms = 2;
        int particlesPerArm = 4 + (int) (chargeProgress * 5); // 4 up to 9
        float dustSize = (float) (1.4 - chargeProgress * 0.9); // 1.4 down to 0.5
        double maxRadius = 0.42 - chargeProgress * 0.18;       // 0.42 down to 0.24
        double turns = 1.25;

        for (int a = 0; a < arms; a++) {
            double armOffset = a * Math.PI;
            for (int i = 0; i < particlesPerArm; i++) {
                double t = (double) i / (particlesPerArm - 1); // 0 = outer, 1 = inner
                double radius = maxRadius * (1.0 - t);
                double angle  = this.animAngle + armOffset + t * turns * Math.PI * 2.0;
                spawnVortexParticle(
                        hand.clone()
                                .add(up.clone().multiply(radius * Math.cos(angle)))
                                .add(side.clone().multiply(radius * Math.sin(angle))),
                        dustSize);
            }
        }
        this.animAngle += 0.45;

        if (System.currentTimeMillis() >= this.chargeStartTime + this.chargeTime) {
            this.phase = Phase.READY;
            // Airy ping to signal the shot is ready to fire
            player.getWorld().playSound(player.getLocation(), Sound.ENTITY_BREEZE_LAND, 0.7f, 2.0f);
        }
    }

    private void progressReady() {
        if (!player.isSneaking()) {
            this.remove();
            return;
        }

        // Tight fast orbit at the hand - fully condensed vortex ready to fire
        Location hand = getHandPosition();
        double yaw = Math.toRadians(player.getLocation().getYaw());
        Vector up   = new Vector(0, 1, 0);
        Vector side = new Vector(-Math.cos(yaw), 0, -Math.sin(yaw));

        int points = 5;
        double radius = 0.10;
        for (int i = 0; i < points; i++) {
            double angle = this.animAngle + (i * Math.PI * 2.0 / points);
            spawnVortexParticle(
                    hand.clone()
                            .add(up.clone().multiply(radius * Math.cos(angle)))
                            .add(side.clone().multiply(radius * Math.sin(angle))),
                    0.5f);
        }
        this.animAngle += 0.80;
    }

    private void progressFiring() {
        final int steps = (int) (this.speed / STEP_SIZE);
        for (int i = 0; i < steps; i++) {
            if (this.distanceTraveled >= this.range) {
                this.remove();
                return;
            }

            this.shotLocation.add(this.direction.clone().multiply(STEP_SIZE));
            this.distanceTraveled += STEP_SIZE;

            if (i % 4 == 0) {
                if (GeneralMethods.isRegionProtectedFromBuild(this, this.shotLocation)) {
                    this.remove();
                    return;
                }
                if (!this.isTransparent(this.shotLocation.getBlock())) {
                    this.remove();
                    return;
                }
            }

            if (i % 3 == 0) {
                this.shotLocation.getWorld().spawnParticle(Particle.DUST, this.shotLocation,
                        2, 0.04, 0.04, 0.04, 0,
                        new Particle.DustOptions(Color.WHITE, 0.7f));
                // Sparse spell flicker for a little crispness
                if (i % 12 == 0) {
                    this.shotLocation.getWorld().spawnParticle(Particle.CRIT,
                            this.shotLocation, 1, 0.05, 0.05, 0.05, 0);
                }
            }

            if (this.checkCollision()) {
                return;
            }
        }

        if (this.distanceTraveled >= this.range) {
            this.remove();
        }
    }

    private boolean checkCollision() {
        for (final LivingEntity entity : this.shotLocation.getNearbyLivingEntities(HIT_RADIUS)) {
            if (entity.equals(player)) continue;
            if (this.hitEntities.contains(entity)) continue;
            if (GeneralMethods.isRegionProtectedFromBuild(this, entity.getLocation())) continue;

            this.hitEntities.add(entity);
            DamageHandler.damageEntity(entity, this.damage, this);
            entity.getWorld().playSound(entity.getLocation(), Sound.ENTITY_PLAYER_HURT, 1.0f, 1.4f);
            this.remove();
            return true;
        }
        return false;
    }

    @Override
    public void remove() {
        super.remove();
        ACTIVE.remove(player.getUniqueId());
    }

    public static AirSnipe getActiveInstance(final UUID uuid) {
        return ACTIVE.get(uuid);
    }

    public Phase getPhase() {
        return this.phase;
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
    public long getCooldown() {
        return this.cooldown;
    }

    @Override
    public Location getLocation() {
        if (this.phase == Phase.FIRING && this.shotLocation != null) {
            return this.shotLocation;
        }
        return player.getLocation();
    }

    @Override
    public String getName() {
        return "AirSnipe";
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
    public Object createNewComboInstance(final Player player) {
        return new AirSnipe(player);
    }

    @Override
    public ArrayList<AbilityInformation> getCombination() {
        final ArrayList<AbilityInformation> combo = new ArrayList<>();
        combo.add(new AbilityInformation("AirPunch", ClickType.SHIFT_DOWN));
        combo.add(new AbilityInformation("AirPunch", ClickType.SHIFT_UP));
        combo.add(new AbilityInformation("AirPunch", ClickType.SHIFT_DOWN));
        combo.add(new AbilityInformation("AirPunch", ClickType.SHIFT_UP));
        combo.add(new AbilityInformation("AirSwipe", ClickType.SHIFT_DOWN));
        return combo;
    }

    @Override
    public String getDescription() {
        return "Channel the force of airbending into a single focused shot that strikes with precision strength.\n" +
                ChatUtils.translateToColor("&7Usage: AirPunch (Tap Sneak) > AirPunch (Tap Sneak) > AirSwipe (Hold Sneak until charged) > AirPunch (Left Click)");
    }
}
