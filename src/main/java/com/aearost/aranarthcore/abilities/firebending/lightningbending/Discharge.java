package com.aearost.aranarthcore.abilities.firebending.lightningbending;

import com.aearost.aranarthcore.utils.AranarthBendingUtils;
import com.aearost.aranarthcore.utils.ChatUtils;
import com.projectkorra.projectkorra.GeneralMethods;
import com.projectkorra.projectkorra.ability.AddonAbility;
import com.projectkorra.projectkorra.ability.LightningAbility;
import com.projectkorra.projectkorra.attribute.Attribute;
import com.projectkorra.projectkorra.util.DamageHandler;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.UUID;

/**
 * A lightning ability that fires a branching bolt of electricity in the direction the player is
 * looking at the moment of activation. The bolt spawns additional forks every three ticks and
 * advances each branch toward the aim direction until it strikes a solid block, hits an entity,
 * or its duration expires. Entities struck take damage and are knocked back; each hit carries a
 * 25% chance to stun the target, freezing their horizontal movement for one second.
 */
public class Discharge extends LightningAbility implements AddonAbility {

    @Attribute(Attribute.DAMAGE)
    private double damage;
    @Attribute(Attribute.COOLDOWN)
    private long cooldown;
    @Attribute("AvatarCooldown")
    private long avatarCooldown;
    @Attribute(Attribute.DURATION)
    private long duration;
    @Attribute("EntityCollisionRadius")
    private double entityCollisionRadius;
    @Attribute("StunChance")
    private double stunChance;
    @Attribute("StunDuration")
    private long stunDuration;

    private Location location;
    private final Vector direction;
    private boolean hit;
    private int spaces;
    private double branchSpace;
    private final HashMap<Integer, Location> branches = new HashMap<>();
    private final Random rand = new Random();

    private static final Map<UUID, Discharge> activeInstances = new HashMap<>();

    public Discharge(Player player) {
        super(player);

        direction = player.getEyeLocation().getDirection().normalize();

        if (!bPlayer.canBend(this) || hasActiveInstance(player.getUniqueId())) {
            return;
        }

        damage = 4.0;
        cooldown = 5000;
        avatarCooldown = 500;
        duration = 1000;
        entityCollisionRadius = 1.0;
        stunChance = 0.25;
        stunDuration = 1000;
        branchSpace = 0.2;

        if (bPlayer.isAvatarState()) {
            cooldown = avatarCooldown;
        }

        activeInstances.put(player.getUniqueId(), this);
        bPlayer.addCooldown(this);
        start();
    }

    @Override
    public void progress() {
        if (player.isDead() || !player.isOnline()) {
            cleanup();
            remove();
            return;
        }

        if (!bPlayer.canBendIgnoreCooldowns(this)) {
            cleanup();
            remove();
            return;
        }

        if (System.currentTimeMillis() < getStartTime() + duration && !hit) {
            advanceLocation();
        } else {
            cleanup();
            remove();
        }
    }

    private void advanceLocation() {
        if (location == null) {
            location = player.getEyeLocation().clone();
            branches.put(branches.size() + 1, location);
        }

        spaces++;
        if (spaces % 3 == 0) {
            Location prevBranch = branches.get(1);
            branches.put(branches.size() + 1, prevBranch);
        }

        List<Integer> toRemove = new ArrayList<>();

        for (int i : branches.keySet()) {
            Location origin = branches.get(i);
            if (origin == null) continue;

            Location l = origin.clone();

            if (!isTransparent(l.getBlock())) {
                toRemove.add(i);
                continue;
            }

            l.add(createBranch(), createBranch(), createBranch());
            branchSpace += 0.001;

            for (int j = 0; j < 10; j++) {
                Particle.DustOptions dust = (j % 2 == 0) ? AranarthBendingUtils.LIGHTNING_DUST_BRIGHT : AranarthBendingUtils.LIGHTNING_DUST_BLUE;
                l.getWorld().spawnParticle(Particle.DUST, l.clone(), 1, 0.03, 0.03, 0.03, 0, dust);

                if (rand.nextInt(6) == 0) {
                    l.getWorld().playSound(l, Sound.ENTITY_BEE_HURT, 0.6f, 0.2f);
                }

                if (checkEntityHit(l)) {
                    hit = true;
                    return;
                }

                l = l.add(direction.clone().multiply(0.2));
            }

            branches.put(i, l);
        }

        for (int i : toRemove) {
            branches.remove(i);
        }
    }

    /**
     * Checks for a living entity within range of the given location.
     * If one is found, applies knockback, damage, and a possible stun.
     * Returns true if an entity was hit.
     */
    private boolean checkEntityHit(Location l) {
        Vector vec = l.toVector();

        for (Entity entity : l.getWorld().getNearbyEntities(l, entityCollisionRadius, entityCollisionRadius, entityCollisionRadius)) {
            if (!(entity instanceof LivingEntity living) || entity.equals(player)) {
                continue;
            }

            Vector knockback = entity.getLocation().toVector().subtract(vec).normalize().multiply(0.8);
            GeneralMethods.setVelocity(this, entity, knockback);
            DamageHandler.damageEntity(living, damage, this);

            if (rand.nextDouble() < stunChance) {
                AranarthBendingUtils.applyElectrocution(living, stunDuration);
            }

            for (int k = 0; k < 5; k++) {
                Particle.DustOptions dust = (k % 2 == 0) ? AranarthBendingUtils.LIGHTNING_DUST_BRIGHT : AranarthBendingUtils.LIGHTNING_DUST_BLUE;
                entity.getLocation().getWorld().spawnParticle(Particle.DUST, entity.getLocation(), 1,
                        rand.nextDouble() * 0.5, rand.nextDouble() * 0.5, rand.nextDouble() * 0.5, 0, dust);
            }

            entity.getWorld().playSound(entity.getLocation(), Sound.ENTITY_BEE_HURT, 0.6f, 0.2f);
            player.getWorld().playSound(player.getLocation(), Sound.ENTITY_BEE_HURT, 0.6f, 0.2f);

            return true;
        }

        return false;
    }

    private double createBranch() {
        return switch (rand.nextInt(3)) {
            case 0 -> branchSpace;
            case 2 -> -branchSpace;
            default -> 0.0;
        };
    }

    private void cleanup() {
        activeInstances.remove(player.getUniqueId());
    }

    public static boolean hasActiveInstance(UUID uuid) {
        return activeInstances.containsKey(uuid);
    }

    public static Discharge getActiveInstance(UUID uuid) {
        return activeInstances.get(uuid);
    }

    public static boolean isStunned(UUID uuid) {
        return AranarthBendingUtils.isElectrocuted(uuid);
    }

    @Override
    public long getCooldown() {
        return cooldown;
    }

    @Override
    public Location getLocation() {
        return location != null ? location : player.getLocation();
    }

    @Override
    public List<Location> getLocations() {
        return new ArrayList<>(branches.values());
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
    public String getName() {
        return "Discharge";
    }

    @Override
    public void load() {}

    @Override
    public void stop() {
        if (player != null) cleanup();
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
        return "Fire lightning from your fingertips in a branching bolt that zaps anything in its path. " +
                "Direct hits have a 25% chance to stun the target, preventing movement for 1 second.\n" +
                ChatUtils.translateToColor("&fUsage: Left-Click");
    }
}
