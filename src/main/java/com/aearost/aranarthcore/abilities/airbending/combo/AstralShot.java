package com.aearost.aranarthcore.abilities.airbending.combo;

import com.aearost.aranarthcore.AranarthCore;
import com.aearost.aranarthcore.utils.AranarthBendingUtils;
import com.aearost.aranarthcore.utils.ChatUtils;
import com.projectkorra.projectkorra.GeneralMethods;
import com.projectkorra.projectkorra.ability.AddonAbility;
import com.projectkorra.projectkorra.ability.ComboAbility;
import com.projectkorra.projectkorra.ability.CoreAbility;
import com.projectkorra.projectkorra.ability.SpiritualAbility;
import com.projectkorra.projectkorra.ability.util.ComboManager.AbilityInformation;
import com.projectkorra.projectkorra.attribute.Attribute;
import com.projectkorra.projectkorra.util.ClickType;
import com.projectkorra.projectkorra.util.DamageHandler;
import io.papermc.paper.datacomponent.item.ResolvableProfile;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Mannequin;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class AstralShot extends SpiritualAbility implements AddonAbility, ComboAbility {

    private static final double HIT_RADIUS = 1.5;
    private static final double STEP_SIZE = 0.1;

    @Attribute(Attribute.COOLDOWN)
    private long cooldown;
    @Attribute(Attribute.DAMAGE)
    private double damage;
    @Attribute(Attribute.RANGE)
    private double range;
    @Attribute(Attribute.SPEED)
    private double speed;

    private Mannequin mannequin;
    private Location shotLocation;
    private Vector direction;
    private double distanceTraveled;
    private final Set<UUID> hitEntities;

    public AstralShot(final Player player) {
        super(player);

        this.hitEntities = new HashSet<>();

        if (!this.bPlayer.canBendIgnoreBindsCooldowns(this)) {
            return;
        }
        if (this.bPlayer.isOnCooldown(this)) {
            return;
        }

        this.cooldown = 10000L;
        this.damage = 2.0;
        this.range = 16.0;
        this.speed = 1.0; // blocks per tick

        // AirBlast fires before the combo is recognised; remove it and wipe its cooldown so the player isn't penalised.
        AranarthBendingUtils.suppressComboTrigger(this.bPlayer, player, "AirBlast");

        // Remove AstralProjections that were created
        Bukkit.getScheduler().runTask(AranarthCore.getInstance(), () -> {
            for (final CoreAbility ability : new ArrayList<>(CoreAbility.getAbilities(player))) {
                if (ability.getName().equals("AstralProjection")) {
                    ability.remove();
                    this.bPlayer.removeCooldown("AstralProjection");
                }
            }
        });

        this.bPlayer.addCooldown(this);
        this.launch();
        this.start();
    }

    private void launch() {
        this.direction = player.getEyeLocation().getDirection().normalize();
        this.shotLocation = player.getEyeLocation().clone();

        final float yaw = (float) Math.toDegrees(Math.atan2(-direction.getX(), direction.getZ()));
        final float pitch = (float) Math.toDegrees(Math.asin(-direction.getY()));
        this.shotLocation.setYaw(yaw);
        this.shotLocation.setPitch(pitch);

        this.mannequin = (Mannequin) player.getWorld().spawnEntity(this.shotLocation, EntityType.MANNEQUIN);
        this.mannequin.setProfile(ResolvableProfile.resolvableProfile(player.getPlayerProfile()));
        this.mannequin.setInvulnerable(true);
        this.mannequin.setGravity(false);
        this.mannequin.setPersistent(false);
        this.mannequin.setNoPhysics(true);
        this.mannequin.setImmovable(true);
        this.mannequin.setGlowing(true);

        player.getWorld().playSound(this.shotLocation, Sound.ENTITY_ENDERMAN_TELEPORT, 1.0f, 1.5f);
    }

    @Override
    public void progress() {
        if (player.isDead() || !player.isOnline()) {
            this.remove();
            return;
        }
        if (this.mannequin == null || this.mannequin.isDead()) {
            this.remove();
            return;
        }

        final int steps = (int) (this.speed / STEP_SIZE);
        for (int i = 0; i < steps; i++) {
            if (this.distanceTraveled >= this.range) {
                this.remove();
                return;
            }

            this.shotLocation.add(this.direction.clone().multiply(STEP_SIZE));
            this.distanceTraveled += STEP_SIZE;

            if (GeneralMethods.isRegionProtectedFromBuild(this, this.shotLocation)) {
                this.remove();
                return;
            }

            if (!this.isTransparent(this.shotLocation.getBlock())) {
                if (!this.isTransparent(this.shotLocation.clone().add(0, 0.2, 0).getBlock())) {
                    this.remove();
                    return;
                }
            }

            if (i % 2 == 0) {
                this.shotLocation.getWorld().spawnParticle(
                        Particle.END_ROD, this.shotLocation, 1, 0.05, 0.05, 0.05, 0.0);
            }
            if (i % 5 == 0) {
                this.shotLocation.getWorld().spawnParticle(
                        Particle.SOUL, this.shotLocation, 1, 0.05, 0.1, 0.05, 0.0);
            }

            this.checkCollisions();
        }

        final float yaw = (float) Math.toDegrees(Math.atan2(-this.direction.getX(), this.direction.getZ()));
        final float pitch = (float) Math.toDegrees(Math.asin(-this.direction.getY()));
        final Location newLoc = this.shotLocation.clone();
        newLoc.setYaw(yaw);
        newLoc.setPitch(pitch);
        this.mannequin.teleport(newLoc);
    }

    private void checkCollisions() {
        for (final LivingEntity entity : this.shotLocation.getWorld().getLivingEntities()) {
            if (entity.equals(player)) continue;
            if (entity.equals(this.mannequin)) continue;
            if (this.hitEntities.contains(entity.getUniqueId())) continue;

            final Location entityCenter = entity.getLocation().add(0, entity.getHeight() / 2.0, 0);
            if (entityCenter.distance(this.shotLocation) <= HIT_RADIUS) {
                this.hitEntities.add(entity.getUniqueId());
                this.applyHitEffects(entity);
            }
        }
    }

    private void applyHitEffects(final LivingEntity entity) {
        final int durationTicks = 60; // 3 seconds
        entity.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, durationTicks, 1, false, true, true));
        entity.addPotionEffect(new PotionEffect(PotionEffectType.POISON, durationTicks, 1, false, true, true));
        entity.addPotionEffect(new PotionEffect(PotionEffectType.NAUSEA, durationTicks, 0, false, true, true));
        DamageHandler.damageEntity(entity, this.damage, this);
        entity.getWorld().playSound(entity.getLocation(), Sound.ENTITY_PHANTOM_HURT, 1.0f, 0.8f);
    }

    @Override
    public void remove() {
        super.remove();
        if (this.mannequin != null && !this.mannequin.isDead()) {
            this.mannequin.remove();
            this.mannequin = null;
        }
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
        return this.shotLocation != null ? this.shotLocation : player.getLocation();
    }

    @Override
    public String getName() {
        return "AstralShot";
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
        return new AstralShot(player);
    }

    @Override
    public ArrayList<AbilityInformation> getCombination() {
        final ArrayList<AbilityInformation> combo = new ArrayList<>();
        combo.add(new AbilityInformation("AstralProjection", ClickType.SHIFT_DOWN));
        combo.add(new AbilityInformation("AstralProjection", ClickType.SHIFT_UP));
        combo.add(new AbilityInformation("AstralProjection", ClickType.SHIFT_DOWN));
        combo.add(new AbilityInformation("AirBlast", ClickType.LEFT_CLICK));
        return combo;
    }

    @Override
    public String getDescription() {
        return "Project your astral form as a spectral shot in the direction you are facing. " +
                "Entities struck by the projection are left temporarily slowed, poisoned, and disoriented, " +
                "as if their body was momentarily possessed. The projection passes through all targets it hits.\n" +
                ChatUtils.translateToColor("Usage: AstralProjection (Tap Sneak) > AstralProjection (Hold Sneak) > AirBlast (Left Click)");
    }
}
