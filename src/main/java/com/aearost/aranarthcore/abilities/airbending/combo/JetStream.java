package com.aearost.aranarthcore.abilities.airbending.combo;

import com.aearost.aranarthcore.utils.AbilityConfig;
import com.aearost.aranarthcore.utils.AranarthBendingUtils;
import com.aearost.aranarthcore.utils.ChatUtils;
import com.aearost.aranarthcore.utils.DominionUtils;
import com.projectkorra.projectkorra.GeneralMethods;
import com.projectkorra.projectkorra.ability.AddonAbility;
import com.projectkorra.projectkorra.ability.AirAbility;
import com.projectkorra.projectkorra.ability.ComboAbility;
import com.projectkorra.projectkorra.ability.CoreAbility;
import com.projectkorra.projectkorra.ability.util.ComboManager.AbilityInformation;
import com.projectkorra.projectkorra.attribute.Attribute;
import com.projectkorra.projectkorra.util.ClickType;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class JetStream extends AirAbility implements AddonAbility, ComboAbility {

    public enum Phase {STREAMING, RELEASED}

    private static final double STEP_SIZE = 0.5;
    private static final double SOUND_STOP_RANGE = 64.0;
    private static final Set<UUID> GLIDING_PLAYERS = new HashSet<>();

    @Attribute(Attribute.COOLDOWN)
    private long cooldown;
    @Attribute(Attribute.DURATION)
    private long duration;
    @Attribute(Attribute.SPEED)
    private double speed;
    @Attribute(Attribute.RADIUS)
    private double radius;

    private double turnRate;
    private double pullStrength;
    private double carryRange;
    private long fallProtection;
    private long doubleTapWindow;

    private Phase phase;
    private Vector direction;
    private Location streamLocation;
    private long releaseTime;
    private int ticks;
    private long lastSneakPressTime;
    private final Set<Entity> carriedEntities = new HashSet<>();
    private final Set<Entity> releasedEntities = new HashSet<>();

    public JetStream(final Player player) {
        super(player);

        if (!this.bPlayer.canBendIgnoreBindsCooldowns(this)) {
            return;
        }
        if (this.bPlayer.isOnCooldown(this)) {
            return;
        }
        if (CoreAbility.hasAbility(player, JetStream.class)) {
            return;
        }

        this.cooldown = AbilityConfig.getLong("JetStream.Cooldown");
        this.duration = AbilityConfig.getLong("JetStream.Duration");
        this.speed = AbilityConfig.getDouble("JetStream.Speed");
        this.radius = AbilityConfig.getDouble("JetStream.Radius");
        this.turnRate = AbilityConfig.getDouble("JetStream.TurnRate");
        this.pullStrength = AbilityConfig.getDouble("JetStream.PullStrength");
        this.carryRange = AbilityConfig.getDouble("JetStream.CarryRange");
        this.fallProtection = AbilityConfig.getLong("JetStream.FallProtection");
        this.doubleTapWindow = AbilityConfig.getLong("JetStream.DoubleTapWindow");

        // Tornado starts on the last sneak - suppress it (the final AirBlast is blocked by the bending listener)
        AranarthBendingUtils.suppressComboTrigger(this.bPlayer, player, "Tornado");

        this.phase = Phase.STREAMING;
        this.direction = player.getEyeLocation().getDirection().normalize();
        this.streamLocation = player.getLocation().add(0, player.getHeight() / 2, 0);
        this.carriedEntities.add(player);

        this.bPlayer.addCooldown(this);

        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_BREEZE_WIND_BURST, 1.5f, 0.8f);
        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_ENDER_DRAGON_FLAP, 1.0f, 1.4f);
        startGliding(player);

        this.start();
    }

    @Override
    public void progress() {
        if (player.isDead() || !player.isOnline()) {
            this.remove();
            return;
        }

        switch (this.phase) {
            case STREAMING -> progressStreaming();
            case RELEASED -> progressReleased();
        }
    }

    private void progressStreaming() {
        if (System.currentTimeMillis() - this.getStartTime() >= this.duration) {
            release();
            return;
        }

        freeDetachedEntities();
        // The stream only follows the caster's gaze while they are still riding it
        if (this.carriedEntities.contains(player)) {
            steer();
        }

        // The stream passes through blocks, catching anything along the segment it travels this tick
        final Location previous = this.streamLocation.clone();
        for (double d = STEP_SIZE; d <= this.speed; d += STEP_SIZE) {
            final Location point = previous.clone().add(this.direction.clone().multiply(d));
            if (GeneralMethods.isRegionProtectedFromBuild(this, point)) {
                release();
                return;
            }
            playAirbendingParticles(point, 3, this.radius * 0.5, this.radius * 0.5, this.radius * 0.5);
            pullEntities(point);
        }
        this.streamLocation = previous.clone().add(this.direction.clone().multiply(this.speed));

        carryEntities(previous);
        protectReleasedEntities();

        if (this.ticks % 4 == 0) {
            playAirbendingSound(this.streamLocation);
        }
        this.ticks++;
    }

    /**
     * Turns the stream slightly toward where the caster is looking, capped by the turn rate.
     */
    private void steer() {
        final Vector look = player.getEyeLocation().getDirection().normalize();
        final double angle = this.direction.angle(look);
        final Vector axis = this.direction.getCrossProduct(look);
        if (angle <= this.turnRate || axis.lengthSquared() < 1.0E-8) {
            this.direction = look;
        } else {
            this.direction = this.direction.rotateAroundNonUnitAxis(axis, this.turnRate).normalize();
        }
    }

    /**
     * Catches any movable entity (mobs, players, items, experience orbs, armor stands, etc.) that comes into contact with the stream.
     */
    private void pullEntities(final Location point) {
        for (final Entity entity : GeneralMethods.getEntitiesAroundPoint(point, this.radius)) {
            // The caster is only carried from the start, so they are not re-caught once freed
            if (entity.equals(player)) {
                continue;
            }
            if (this.carriedEntities.contains(entity) || this.releasedEntities.contains(entity)) {
                continue;
            }
            if (entity instanceof ArmorStand armorStand && !armorStand.canMove()) {
                continue;
            }
            if (entity instanceof Player targetPlayer && !DominionUtils.canAttackPlayer(player, targetPlayer)) {
                continue;
            }
            if (GeneralMethods.isRegionProtectedFromBuild(this, entity.getLocation())) {
                continue;
            }

            this.carriedEntities.add(entity);
            if (entity instanceof Player targetPlayer) {
                startGliding(targetPlayer);
            }
            entity.getWorld().playSound(entity.getLocation(), Sound.ENTITY_BREEZE_SHOOT, 1.0f, 1.2f);
        }
    }

    /**
     * Frees any carried entity (including the caster) that is no longer touching the stream.
     */
    private void freeDetachedEntities() {
        this.carriedEntities.removeIf(entity -> {
            if (entity.isDead() || !entity.isValid()) {
                stopGliding(entity);
                return true;
            }
            if (!entity.getWorld().equals(this.streamLocation.getWorld())
                    || getCenter(entity).distance(this.streamLocation) > this.carryRange) {
                stopGliding(entity);
                this.releasedEntities.add(entity);
                return true;
            }
            return false;
        });
    }

    /**
     * Moves all carried entities along with the stream, drawing them in toward its current point.
     */
    private void carryEntities(final Location point) {
        for (final Entity entity : this.carriedEntities) {
            final Vector pull = point.toVector().subtract(getCenter(entity).toVector()).multiply(this.pullStrength);
            entity.setVelocity(this.direction.clone().multiply(this.speed).add(pull));
            entity.setFallDistance(0f);
            // Re-applied every tick as the server otherwise drops gliding without an elytra
            if (entity instanceof Player targetPlayer) {
                targetPlayer.setGliding(true);
            }
        }
    }

    /**
     * Gets the location at the middle of the entity's hitbox.
     */
    private static Location getCenter(final Entity entity) {
        return entity.getLocation().add(0, entity.getHeight() / 2, 0);
    }

    /**
     * Called by the bending listener when the caster presses sneak while the stream is active.
     */
    public void onSneakPress() {
        if (this.phase != Phase.STREAMING) {
            return;
        }
        final long now = System.currentTimeMillis();
        if (now - this.lastSneakPressTime <= this.doubleTapWindow) {
            release();
            return;
        }
        this.lastSneakPressTime = now;
    }

    /**
     * Ends the stream.
     */
    private void release() {
        this.phase = Phase.RELEASED;
        this.releaseTime = System.currentTimeMillis();

        for (final Entity entity : this.carriedEntities) {
            stopGliding(entity);
            if (!entity.isDead() && entity.isValid()) {
                this.releasedEntities.add(entity);
            }
        }
        this.carriedEntities.clear();

        this.streamLocation.getWorld().playSound(this.streamLocation, Sound.ENTITY_BREEZE_WIND_BURST, 1.0f, 1.4f);
    }

    /**
     * Places a player in elytra flight for as long as they ride the stream, with the elytra flight sound following them.
     */
    private void startGliding(final Player target) {
        GLIDING_PLAYERS.add(target.getUniqueId());
        target.setGliding(true);
        target.getWorld().playSound(target, Sound.ITEM_ELYTRA_FLYING, SoundCategory.PLAYERS, 1.0f, 1.0f);
    }

    /**
     * Returns a player to normal movement if they were placed in elytra flight by the stream.
     */
    private void stopGliding(final Entity entity) {
        if (GLIDING_PLAYERS.remove(entity.getUniqueId()) && entity instanceof LivingEntity livingEntity) {
            livingEntity.setGliding(false);
            for (final Player nearby : entity.getLocation().getNearbyPlayers(SOUND_STOP_RANGE)) {
                nearby.stopSound(Sound.ITEM_ELYTRA_FLYING, SoundCategory.PLAYERS);
            }
        }
    }

    private void progressReleased() {
        if (System.currentTimeMillis() - this.releaseTime >= this.fallProtection) {
            this.remove();
            return;
        }

        protectReleasedEntities();
        if (this.releasedEntities.isEmpty()) {
            this.remove();
        }
    }

    /**
     * Negates fall damage for freed entities (including the caster) until each one lands.
     */
    private void protectReleasedEntities() {
        this.releasedEntities.removeIf(entity -> entity.isDead() || !entity.isValid() || entity.isOnGround());
        for (final Entity entity : this.releasedEntities) {
            entity.setFallDistance(0f);
        }
    }

    @Override
    public void remove() {
        super.remove();
        for (final Entity entity : this.carriedEntities) {
            stopGliding(entity);
        }
        this.carriedEntities.clear();
    }

    /**
     * Determines if the player is currently held in elytra flight by a JetStream.
     *
     * @param uuid The UUID of the player.
     * @return Whether the player is gliding in a JetStream.
     */
    public static boolean isGliding(final UUID uuid) {
        return GLIDING_PLAYERS.contains(uuid);
    }

    /**
     * Determines if the caster is still being carried by the stream.
     *
     * @return Whether the caster is riding the stream.
     */
    public boolean isCasterRiding() {
        return this.phase == Phase.STREAMING && this.carriedEntities.contains(player);
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
        return this.streamLocation != null ? this.streamLocation : player.getLocation();
    }

    @Override
    public String getName() {
        return "JetStream";
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
    public Object createNewComboInstance(final Player player) {
        return new JetStream(player);
    }

    @Override
    public ArrayList<AbilityInformation> getCombination() {
        final ArrayList<AbilityInformation> combo = new ArrayList<>();
        combo.add(new AbilityInformation("AirBurst", ClickType.SHIFT_DOWN));
        combo.add(new AbilityInformation("AirShield", ClickType.SHIFT_UP));
        combo.add(new AbilityInformation("Tornado", ClickType.SHIFT_DOWN));
        combo.add(new AbilityInformation("AirBlast", ClickType.LEFT_CLICK));
        return combo;
    }

    @Override
    public String getDescription() {
        return "Unleash a supercharged jet of wind that launches you forward at rapid speeds, dragging anything caught in its path along with you.\n" +
                ChatUtils.translateToColor("&7Usage: AirBurst (Hold Sneak) > AirShield (Release Sneak) > Tornado (Hold Sneak) > AirBlast (Left Click)");
    }
}
