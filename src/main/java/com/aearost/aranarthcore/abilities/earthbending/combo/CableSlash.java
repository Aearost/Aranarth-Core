package com.aearost.aranarthcore.abilities.earthbending.combo;

import com.aearost.aranarthcore.AranarthCore;
import com.projectkorra.projectkorra.GeneralMethods;
import com.projectkorra.projectkorra.ability.AddonAbility;
import com.projectkorra.projectkorra.ability.ComboAbility;
import com.projectkorra.projectkorra.ability.CoreAbility;
import com.projectkorra.projectkorra.ability.MetalAbility;
import com.projectkorra.projectkorra.ability.util.Collision;
import com.projectkorra.projectkorra.ability.util.ComboManager.AbilityInformation;
import com.projectkorra.projectkorra.attribute.Attribute;
import com.projectkorra.projectkorra.command.Commands;
import com.projectkorra.projectkorra.event.BendingReloadEvent;
import com.projectkorra.projectkorra.firebending.combo.ComboStream;
import com.projectkorra.projectkorra.object.HorizontalVelocityTracker;
import com.projectkorra.projectkorra.util.ClickType;
import com.projectkorra.projectkorra.util.DamageHandler;
import com.projectkorra.projectkorra.util.ParticleEffect;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.List;

public class CableSlash extends MetalAbility implements AddonAbility, ComboAbility {

    private static Listener metalCableBlocker;
    private static final double RECALL_SPEED = 1.5;

    private int progressCounter;
    @Attribute(Attribute.COOLDOWN)
    private long cooldown;
    @Attribute(Attribute.DAMAGE)
    private double damage;
    @Attribute(Attribute.SPEED)
    private double speed;
    @Attribute(Attribute.RANGE)
    private double range;
    @Attribute(Attribute.KNOCKBACK)
    private double knockback;

    private Location origin;
    private Location currentLoc;
    private Location destination;
    private Vector direction;
    private final ArrayList<Entity> affectedEntities;
    private ArrayList<ComboStream> tasks;
    private double radius;
    private boolean recalling;
    private final ArrayList<Location> recallPoints;
    private int recallCounter;

    public CableSlash(final Player player) {
        super(player);

        this.affectedEntities = new ArrayList<>();
        this.tasks = new ArrayList<>();
        this.recallPoints = new ArrayList<>();

        if (!this.bPlayer.canBendIgnoreBindsCooldowns(this)) {
            return;
        }
        if (this.bPlayer.isOnCooldown(this)) {
            return;
        }

        this.damage = 5.0;
        this.range = 10.0;
        this.speed = 1.0;
        this.knockback = 0.3;
        this.cooldown = 8000L;
        this.radius = 1.5;

        this.bPlayer.addCooldown(this);
        this.start();

        // MetalCable fires before the combo is recognised, so it's already active by the time
        // CableSlash starts. Remove it and wipe its cooldown so the player isn't penalised.
        Bukkit.getScheduler().runTask(AranarthCore.getInstance(), () -> {
            for (final CoreAbility ability : new ArrayList<>(CoreAbility.getAbilities(this.player))) {
                if (ability.getName().equals("MetalCable")) {
                    ability.remove();
                    this.bPlayer.removeCooldown("MetalCable");
                }
            }
        });
    }

    @Override
    public void progress() {
        this.progressCounter++;
        if (this.player.isDead() || !this.player.isOnline()) {
            this.remove();
            return;
        } else if (this.currentLoc != null && GeneralMethods.isRegionProtectedFromBuild(this, this.currentLoc)) {
            this.remove();
            return;
        }

        if (this.origin == null) {
            this.direction = this.player.getEyeLocation().getDirection().normalize();
            this.origin = GeneralMethods.getMainHandLocation(player).add(this.direction.clone().multiply(10));
        }

        if (this.progressCounter < 8) {
            return;
        }

        if (this.recalling) {
            this.manageRecall();
            return;
        }

        if (this.destination == null) {
            this.destination = GeneralMethods.getMainHandLocation(player).add(
                    GeneralMethods.getMainHandLocation(player).getDirection().normalize().multiply(10));
            final Vector origToDest = GeneralMethods.getDirection(this.origin, this.destination);
            final Location hand = GeneralMethods.getMainHandLocation(player);
            for (double i = 0; i < 30; i++) {
                final Location endLoc = this.origin.clone().add(origToDest.clone().multiply(i / 30));
                if (GeneralMethods.locationEqualsIgnoreDirection(hand, endLoc)) {
                    continue;
                }
                final Vector vec = GeneralMethods.getDirection(hand, endLoc);
                final ComboStream fs = new ComboStream(this.player, this, vec, hand, this.range, this.speed);
                fs.setDensity(1);
                fs.setSpread(0F);
                fs.setUseNewParticles(true);
                fs.setParticleEffect(ParticleEffect.SMOKE_NORMAL);
                fs.setCollides(false);
                fs.start();
                this.tasks.add(fs);
            }
        }

        this.manageMetalVectors();
    }

    private void manageMetalVectors() {
        for (int i = 0; i < this.tasks.size(); i++) {
            if (this.tasks.get(i).isRemoved()) {
                this.recallPoints.add(this.tasks.get(i).getLocation().clone());
                this.tasks.remove(i);
                i--;
            }
        }
        if (this.tasks.isEmpty()) {
            if (!this.recallPoints.isEmpty()) {
                final Vector eyeDir = this.player.getEyeLocation().getDirection().normalize();
                final Vector eyePos = this.player.getEyeLocation().toVector();
                Location best = null;
                double bestDot = Double.NEGATIVE_INFINITY;
                for (final Location point : this.recallPoints) {
                    final double dot = point.toVector().subtract(eyePos).normalize().dot(eyeDir);
                    if (dot > bestDot) {
                        bestDot = dot;
                        best = point;
                    }
                }
                this.recallPoints.clear();
                this.recallPoints.add(best);
                this.recalling = true;
            } else {
                this.remove();
            }
            return;
        }
        for (int i = 0; i < this.tasks.size(); i++) {
            final ComboStream fstream = this.tasks.get(i);
            final Location loc = fstream.getLocation();

            if (GeneralMethods.isRegionProtectedFromBuild(this, loc)) {
                fstream.remove();
                return;
            }

            if (!this.isTransparent(loc.getBlock())) {
                if (!this.isTransparent(loc.clone().add(0, 0.2, 0).getBlock())) {
                    fstream.remove();
                    return;
                }
            }

            GeneralMethods.displayColoredParticle("#6e6e6e", loc);

            if (i % 3 == 0) {
                for (final Entity entity : GeneralMethods.getEntitiesAroundPoint(loc, this.radius)) {
                    if (GeneralMethods.isRegionProtectedFromBuild(this, entity.getLocation())) {
                        this.remove();
                        return;
                    }
                    if (!entity.equals(this.player) && !(entity instanceof Player && Commands.invincible.contains(entity.getName()))) {
                        if (this.knockback != 0) {
                            final Vector force = fstream.getLocation().getDirection();
                            GeneralMethods.setVelocity(this, entity, force.clone().multiply(this.knockback));
                            new HorizontalVelocityTracker(entity, this.player, 200L, this);
                            entity.setFallDistance(0);
                        }
                        if (!this.affectedEntities.contains(entity)) {
                            this.affectedEntities.add(entity);
                            if (this.damage != 0 && entity instanceof LivingEntity) {
                                DamageHandler.damageEntity(entity, this.damage, this);
                            }
                        }
                    }
                }
            }
        }
    }

    private void manageRecall() {
        this.recallCounter++;
        final Location target = GeneralMethods.getMainHandLocation(this.player);

        for (int i = 0; i < this.recallPoints.size(); i++) {
            final Location point = this.recallPoints.get(i);
            if (point.distanceSquared(target) <= (RECALL_SPEED + 0.5) * (RECALL_SPEED + 0.5)) {
                this.recallPoints.remove(i);
                i--;
                continue;
            }
            point.add(GeneralMethods.getDirection(point, target).normalize().multiply(RECALL_SPEED));
            final Vector trailDir = GeneralMethods.getDirection(point, target);
            final double trailDist = point.distance(target);
            final int steps = (int) (trailDist / 0.4);
            for (int j = 0; j <= steps; j++) {
                GeneralMethods.displayColoredParticle("#444444", point.clone().add(trailDir.clone().multiply(j * 0.4)));
            }
            // 3x3 grid of tip particles on the plane perpendicular to travel direction
            final Vector perp1 = (Math.abs(trailDir.getY()) < 0.9 ? new Vector(0, 1, 0) : new Vector(1, 0, 0))
                    .crossProduct(trailDir).normalize().multiply(0.3);
            final Vector perp2 = trailDir.clone().crossProduct(perp1).normalize().multiply(0.3);
            for (int dx = -1; dx <= 1; dx++) {
                for (int dy = -1; dy <= 1; dy++) {
                    GeneralMethods.displayColoredParticle("#444444",
                            point.clone().add(perp1.clone().multiply(dx)).add(perp2.clone().multiply(dy)));
                }
            }
        }

        if (this.recallPoints.isEmpty()) {
            this.remove();
            return;
        }

        if (this.recallCounter % 3 == 0) {
            final Location soundLoc = this.player.getLocation();
            this.player.getWorld().playSound(soundLoc, Sound.ENTITY_BREEZE_IDLE_AIR, 0.55F, 0.7F);
            this.player.getWorld().playSound(soundLoc, Sound.ENTITY_BREEZE_IDLE_AIR, 0.4F, 0.77F);
            this.player.getWorld().playSound(soundLoc, Sound.ENTITY_BREEZE_IDLE_AIR, 0.25F, 0.85F);
        }
    }

    @Override
    public void remove() {
        super.remove();
        for (final ComboStream task : this.tasks) {
            task.cancel();
        }
    }

    @Override
    public void handleCollision(final Collision collision) {
        if (collision.isRemovingFirst()) {
            final ArrayList<ComboStream> newTasks = new ArrayList<>();
            final double collisionDistanceSquared = Math.pow(
                    this.getCollisionRadius() + collision.getAbilitySecond().getCollisionRadius(), 2);
            for (final ComboStream task : this.tasks) {
                if (task.getLocation().distanceSquared(collision.getLocationSecond()) > collisionDistanceSquared) {
                    newTasks.add(task);
                } else {
                    task.cancel();
                }
            }
            this.setTasks(newTasks);
        }
    }

    @Override
    public List<Location> getLocations() {
        final ArrayList<Location> locations = new ArrayList<>();
        for (final ComboStream task : this.tasks) {
            locations.add(task.getLocation());
        }
        return locations;
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
        return this.origin;
    }

    @Override
    public String getName() {
        return "CableSlash";
    }

    /**
     * Called when the ability is loaded by PK. This is where the developer
     * registers Listeners and Permissions.
     */
    @Override
    public void load() {
        metalCableBlocker = new Listener() {
            @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
            public void onPKReload(final BendingReloadEvent e) {
                new ArrayList<>(CoreAbility.getAbilities(CableSlash.class)).forEach(CoreAbility::remove);
            }
        };
        Bukkit.getPluginManager().registerEvents(metalCableBlocker, AranarthCore.getInstance());
    }

    /**
     * Called whenever ProjectKorra stops and the ability is unloaded. This
     * method is useful for cleaning up leftover objects such as frozen blocks.
     * Any CoreAbility instances do not need to be cleaned up by stop method, as
     * they will be cleaned up by {@link CoreAbility#removeAll()}.
     */
    @Override
    public void stop() {
        if (metalCableBlocker != null) {
            HandlerList.unregisterAll(metalCableBlocker);
            metalCableBlocker = null;
        }
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
        return new CableSlash(player);
    }

    @Override
    public ArrayList<AbilityInformation> getCombination() {
        final ArrayList<AbilityInformation> combo = new ArrayList<>();
        combo.add(new AbilityInformation("EarthSmash", ClickType.SHIFT_DOWN));
        combo.add(new AbilityInformation("MetalCable", ClickType.SHIFT_UP));
        combo.add(new AbilityInformation("MetalCable", ClickType.SHIFT_DOWN));
        combo.add(new AbilityInformation("MetalCable", ClickType.LEFT_CLICK));
        return combo;
    }

    @Override
    public String getDescription() {
        return "Send out your metal cable and slash it at your foes to stun them. "
                + "The radius and direction of CableSlash are controlled by moving your mouse in a sweeping motion. " +
                "For example, if you want to use CableSlash upward, then move your mouse upward right after you left click CableSlash.\n"
                + "Usage: EarthSmash (Hold Sneak) > MetalCable (Release Sneak) > MetalCable (Hold Sneak) > MetalCable (Left Click)";
    }

    public ArrayList<ComboStream> getTasks() {
        return this.tasks;
    }

    public void setTasks(final ArrayList<ComboStream> tasks) {
        this.tasks = tasks;
    }
}
