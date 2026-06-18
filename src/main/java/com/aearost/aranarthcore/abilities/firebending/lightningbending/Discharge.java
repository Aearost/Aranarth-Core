package com.aearost.aranarthcore.abilities.firebending.lightningbending;

import com.aearost.aranarthcore.AranarthCore;
import com.aearost.aranarthcore.utils.ChatUtils;
import com.projectkorra.projectkorra.GeneralMethods;
import com.projectkorra.projectkorra.ability.AddonAbility;
import com.projectkorra.projectkorra.ability.LightningAbility;
import com.projectkorra.projectkorra.attribute.Attribute;
import com.projectkorra.projectkorra.util.DamageHandler;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
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
    private static final Map<UUID, Long> stunnedEntities = new HashMap<>();

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

            for (int j = 0; j < 5; j++) {
                playLightningbendingParticle(l.clone(), 0f, 0f, 0f);

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
                applyStun(living, stunDuration);
            }

            for (int k = 0; k < 5; k++) {
                playLightningbendingParticle(
                        entity.getLocation(),
                        (float) rand.nextDouble(),
                        (float) rand.nextDouble(),
                        (float) rand.nextDouble()
                );
            }

            entity.getWorld().playSound(entity.getLocation(), Sound.ENTITY_BEE_HURT, 0.6f, 0.2f);
            player.getWorld().playSound(player.getLocation(), Sound.ENTITY_BEE_HURT, 0.6f, 0.2f);

            return true;
        }

        return false;
    }

    private static void applyStun(LivingEntity target, long durationMs) {
        UUID uuid = target.getUniqueId();
        long expiry = System.currentTimeMillis() + durationMs;
        stunnedEntities.put(uuid, expiry);
        int stunTicks = (int) (durationMs / 50L);

        new BukkitRunnable() {
            int ticks = 0;

            @Override
            public void run() {
                if (ticks >= stunTicks || !stunnedEntities.containsKey(uuid)) {
                    stunnedEntities.remove(uuid);
                    cancel();
                    return;
                }
                if (target.isDead() || (target instanceof Player p && !p.isOnline())) {
                    stunnedEntities.remove(uuid);
                    cancel();
                    return;
                }
                target.setVelocity(new Vector(0, target.getVelocity().getY(), 0));
                ticks++;
            }
        }.runTaskTimer(AranarthCore.getInstance(), 0L, 1L);
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
        Long expiry = stunnedEntities.get(uuid);
        if (expiry == null) return false;
        if (System.currentTimeMillis() >= expiry) {
            stunnedEntities.remove(uuid);
            return false;
        }
        return true;
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
