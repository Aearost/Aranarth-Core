package com.aearost.aranarthcore.abilities.earthbending.combo;

import com.aearost.aranarthcore.utils.AranarthBendingUtils;
import com.projectkorra.projectkorra.GeneralMethods;
import com.projectkorra.projectkorra.ability.AddonAbility;
import com.projectkorra.projectkorra.ability.ComboAbility;
import com.projectkorra.projectkorra.ability.MetalAbility;
import com.projectkorra.projectkorra.ability.util.Collision;
import com.projectkorra.projectkorra.ability.util.ComboManager.AbilityInformation;
import com.projectkorra.projectkorra.attribute.Attribute;
import com.projectkorra.projectkorra.util.ClickType;
import com.projectkorra.projectkorra.util.DamageHandler;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.util.BoundingBox;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

public class CableSlash extends MetalAbility implements AddonAbility, ComboAbility {

    private static final int SLASH_TICKS = 20;
    private static final int SUB_STEPS = 4;
    private static final double HIT_RADIUS = 1.5;
    private static final double YAW_THRESHOLD = 0.01;

    private static final Particle.DustOptions CABLE_DUST =
            new Particle.DustOptions(Color.fromRGB(55, 55, 58), 0.5f);

    @Attribute(Attribute.COOLDOWN)
    private long cooldown;
    @Attribute(Attribute.DAMAGE)
    private double damage;
    @Attribute(Attribute.RANGE)
    private double range;
    @Attribute(Attribute.KNOCKBACK)
    private double knockback;

    private int slashTick;
    private Vector direction;
    private Vector prevDirection;
    private double prevYawDelta;
    private final Set<Entity> affectedEntities = new HashSet<>();


    public CableSlash(final Player player) {
        super(player);

        if (!this.bPlayer.canBendIgnoreBindsCooldowns(this)) {
            return;
        }
        if (this.bPlayer.isOnCooldown(this)) {
            return;
        }
        if (!AranarthBendingUtils.hasMetalArmor(player)) {
            return;
        }

        this.damage    = 5.0;
        this.range     = 10.0;
        this.knockback = 0.3;
        this.cooldown  = 8000L;

        this.direction     = player.getEyeLocation().getDirection().normalize();
        this.prevDirection = this.direction.clone();
        this.prevYawDelta  = 0.0;
        this.slashTick     = 0;

        this.bPlayer.addCooldown(this);

        final Location loc = player.getLocation();
        player.getWorld().playSound(loc, Sound.BLOCK_CHAIN_STEP,         0.8f,  1.4f);
        player.getWorld().playSound(loc, Sound.ENTITY_IRON_GOLEM_ATTACK, 0.9f,  1.8f);

        this.start();

        // MetalCable fires before the combo is recognised- suppress it so the player isn't penalised.
        AranarthBendingUtils.suppressComboTrigger(this.bPlayer, this.player, "MetalCable");
    }

    @Override
    public void progress() {
        if (player.isDead() || !player.isOnline()) {
            remove();
            return;
        }

        slashTick++;

        final Vector curDirection = player.getEyeLocation().getDirection().normalize();

        // Reversal detection
        final double yawDelta = prevDirection.getX() * curDirection.getZ()
                              - prevDirection.getZ() * curDirection.getX();
        if (prevYawDelta != 0.0 && Math.abs(yawDelta) > YAW_THRESHOLD
                && Math.signum(yawDelta) != Math.signum(prevYawDelta)) {
            remove();
            return;
        }
        if (Math.abs(yawDelta) > YAW_THRESHOLD) {
            prevYawDelta = yawDelta;
        }

        final Location hand = GeneralMethods.getMainHandLocation(player);

        for (int s = 1; s <= SUB_STEPS; s++) {
            final double t = (double) s / SUB_STEPS;
            direction = prevDirection.clone()
                    .add(curDirection.clone().subtract(prevDirection).multiply(t))
                    .normalize();
            drawSlashLine(hand);
        }

        // Hit-detection sub-steps (10)
        final int HIT_STEPS = 10;
        for (int s = 1; s <= HIT_STEPS; s++) {
            final double t = (double) s / HIT_STEPS;
            direction = prevDirection.clone()
                    .add(curDirection.clone().subtract(prevDirection).multiply(t))
                    .normalize();
            checkHits(hand);
        }

        direction    = curDirection;
        prevDirection = curDirection;

        if (slashTick >= SLASH_TICKS) {
            remove();
        }
    }

    /**
     * Draws a full-length straight line of cable particles.
     */
    private void drawSlashLine(final Location hand) {
        final int steps = Math.max(2, (int) (range / 0.25));
        for (int i = 0; i <= steps; i++) {
            final double s   = (double) i / steps;
            final Location loc = hand.clone().add(direction.clone().multiply(s * range));
            loc.getWorld().spawnParticle(Particle.DUST, loc, 1, 0, 0, 0, 0, CABLE_DUST);
        }
    }

    private void checkHits(final Location hand) {
        final Location end = hand.clone().add(direction.clone().multiply(range));
        final double r = HIT_RADIUS;
        final BoundingBox lineBounds = new BoundingBox(
                Math.min(hand.getX(), end.getX()) - r,
                Math.min(hand.getY(), end.getY()) - r,
                Math.min(hand.getZ(), end.getZ()) - r,
                Math.max(hand.getX(), end.getX()) + r,
                Math.max(hand.getY(), end.getY()) + r,
                Math.max(hand.getZ(), end.getZ()) + r);

        final Vector origin = hand.toVector();

        for (final Entity entity : hand.getWorld().getNearbyEntities(lineBounds)) {
            if (entity.equals(player)) continue;
            if (!(entity instanceof LivingEntity)) continue;
            if (affectedEntities.contains(entity)) continue;

            if (entity.getBoundingBox().expand(r).rayTrace(origin, direction, range) == null) continue;

            affectedEntities.add(entity);

            if (damage != 0) {
                DamageHandler.damageEntity(entity, damage, this);
            }
            if (knockback != 0) {
                GeneralMethods.setVelocity(this, entity, direction.clone().multiply(knockback));
                entity.setFallDistance(0);
            }

            entity.getWorld().playSound(entity.getLocation(), Sound.ENTITY_IRON_GOLEM_ATTACK, 1.0f, 1.9f);
            entity.getWorld().playSound(entity.getLocation(), Sound.BLOCK_METAL_HIT,          0.6f, 2.0f);
        }
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
        return "CableSlash";
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
        return new CableSlash(player);
    }

    @Override
    public ArrayList<AbilityInformation> getCombination() {
        final ArrayList<AbilityInformation> combo = new ArrayList<>();
        combo.add(new AbilityInformation("EarthSmash",  ClickType.SHIFT_DOWN));
        combo.add(new AbilityInformation("MetalCable",  ClickType.SHIFT_UP));
        combo.add(new AbilityInformation("MetalCable",  ClickType.SHIFT_DOWN));
        combo.add(new AbilityInformation("MetalCable",  ClickType.LEFT_CLICK));
        return combo;
    }

    @Override
    public void handleCollision(final Collision collision) {}

    @Override
    public String getDescription() {
        return "Send out your metal cable and slash it at your foes. "
                + "Move your mouse in a sweeping motion after activation to direct the cable.\n"
                + "Usage: EarthSmash (Hold Sneak) > MetalCable (Release Sneak) > MetalCable (Hold Sneak) > MetalCable (Left Click)";
    }
}
