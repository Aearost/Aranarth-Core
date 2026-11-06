package com.aearost.aranarthcore.utils;

import com.aearost.aranarthcore.AranarthCore;
import com.aearost.aranarthcore.objects.Dominion;
import com.destroystokyo.paper.entity.ai.Goal;
import com.destroystokyo.paper.entity.ai.VanillaGoal;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.*;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import java.util.*;

/**
 * Handles rallying a player's nearby pet wolves and cats using the Horn of the Target.
 */
public class PetRallyUtils {

    private static final int RALLY_INTERVAL_TICKS = 5;
    private static final int CAT_DURATION_TICKS = 300;
    private static final int TRAVEL_TIMEOUT_TICKS = 300;
    private static final int CHASE_TIMEOUT_TICKS = 600;
    private static final double TARGET_SEARCH_RADIUS = 8;
    private static final double ARRIVAL_DISTANCE_SQUARED = 9;
    private static final int FOLLOW_OWNER_PRIORITY = 6;
    private static final Map<UUID, BukkitTask> activeRallies = new HashMap<>();
    private static final Map<UUID, List<UUID>> ralliedPets = new HashMap<>();
    private static final Map<UUID, Goal<Tameable>> removedFollowGoals = new HashMap<>();

    private PetRallyUtils() {
    }

    /**
     * Sends all nearby non-sitting pet wolves and cats of the player to the target.
     *
     * @param player         The player blowing the horn.
     * @param targetLocation The location the pets will be sent to.
     * @param lookedAtEntity The entity the player is looking at, if any.
     * @return The amount of pets that were sent.
     */
    public static int rallyPets(Player player, Location targetLocation, Entity lookedAtEntity) {
        // Using the horn again replaces the previous rally
        endRally(player.getUniqueId());

        // Marks the targeted mob directly so wolves can follow it even if it leaves the target area
        LivingEntity initialTarget = isValidTarget(player, lookedAtEntity)
                ? (LivingEntity) lookedAtEntity
                : getClosestTarget(player, targetLocation);

        List<UUID> pets = new ArrayList<>();
        // Pet UUID - the mob the wolf was sent to attack
        Map<UUID, UUID> assignedTargets = new HashMap<>();
        for (Entity entity : player.getNearbyEntities(64, 64, 64)) {
            if (!(entity instanceof Wolf) && !(entity instanceof Cat)) {
                continue;
            }

            Tameable pet = (Tameable) entity;
            if (!pet.isTamed() || !player.getUniqueId().equals(pet.getOwnerUniqueId()) || pet.isDead()) {
                continue;
            }

            // Sitting pets were told to stay
            if (pet instanceof Sittable sittable && sittable.isSitting()) {
                continue;
            }

            // Only rally pets within 64 blocks of the player
            if (entity.getLocation().distanceSquared(player.getLocation()) > 4096) {
                continue;
            }

            // Prevents the pet from following or teleporting back to the owner while rallied
            Goal<Tameable> followGoal = Bukkit.getMobGoals().getGoal(pet, VanillaGoal.FOLLOW_OWNER);
            if (followGoal != null) {
                Bukkit.getMobGoals().removeGoal(pet, followGoal);
                removedFollowGoals.put(pet.getUniqueId(), followGoal);
            }

            // The horn overrides whatever the wolf was previously chasing
            if (pet instanceof Wolf wolf) {
                wolf.setAngry(false);
                wolf.setTarget(initialTarget);
                if (initialTarget != null) {
                    assignedTargets.put(wolf.getUniqueId(), initialTarget.getUniqueId());
                }
            }
            pets.add(pet.getUniqueId());
        }

        if (pets.isEmpty()) {
            return 0;
        }

        UUID playerUUID = player.getUniqueId();
        ralliedPets.put(playerUUID, pets);
        BukkitTask task = new BukkitRunnable() {
            int ticksElapsed = 0;

            @Override
            public void run() {
                Player owner = Bukkit.getPlayer(playerUUID);
                if (owner == null) {
                    endRally(playerUUID);
                    return;
                }
                ticksElapsed += RALLY_INTERVAL_TICKS;

                Iterator<UUID> iterator = pets.iterator();
                while (iterator.hasNext()) {
                    UUID petUUID = iterator.next();
                    Entity entity = Bukkit.getEntity(petUUID);
                    // Releases pets that died, unloaded, changed worlds, or were told to sit
                    if (!(entity instanceof Tameable pet) || !pet.isValid()
                            || !pet.getWorld().equals(targetLocation.getWorld())
                            || (pet instanceof Sittable sittable && sittable.isSitting())) {
                        restoreFollowGoal(petUUID);
                        iterator.remove();
                        continue;
                    }

                    if (pet instanceof Wolf wolf) {
                        if (!updateWolf(owner, wolf, targetLocation, assignedTargets, ticksElapsed)) {
                            restoreFollowGoal(petUUID);
                            iterator.remove();
                        }
                        continue;
                    }

                    // Cats stay at the target location to repel creepers
                    if (ticksElapsed >= CAT_DURATION_TICKS) {
                        restoreFollowGoal(petUUID);
                        iterator.remove();
                    } else if (pet.getLocation().distanceSquared(targetLocation) > 4) {
                        pet.getPathfinder().moveTo(targetLocation, 1.2);
                    }
                }

                if (pets.isEmpty()) {
                    endRally(playerUUID);
                }
            }
        }.runTaskTimer(AranarthCore.getInstance(), 0L, RALLY_INTERVAL_TICKS);
        activeRallies.put(playerUUID, task);
        return pets.size();
    }

    /**
     * Updates the behaviour of a rallied wolf.
     *
     * @param owner           The player who blew the horn.
     * @param wolf            The rallied wolf.
     * @param targetLocation  The target location of the rally.
     * @param assignedTargets The mobs that each wolf was sent to attack.
     * @param ticksElapsed    How long the rally has been active.
     * @return Whether the wolf is still rallied.
     */
    private static boolean updateWolf(Player owner, Wolf wolf, Location targetLocation,
                                      Map<UUID, UUID> assignedTargets, int ticksElapsed) {
        UUID wolfUUID = wolf.getUniqueId();

        // Chases the assigned mob until it dies
        if (assignedTargets.containsKey(wolfUUID)) {
            if (!(Bukkit.getEntity(assignedTargets.get(wolfUUID)) instanceof LivingEntity target)
                    || !target.isValid() || target.isDead() || ticksElapsed >= CHASE_TIMEOUT_TICKS) {
                wolf.setTarget(null);
                return false;
            }
            // Prevents the wolf from switching to a mob of its own choosing while rallied
            if (!target.equals(wolf.getTarget())) {
                wolf.setTarget(target);
            }
            return true;
        }

        // Travels to the target location
        if (wolf.getLocation().distanceSquared(targetLocation) > ARRIVAL_DISTANCE_SQUARED) {
            if (ticksElapsed >= TRAVEL_TIMEOUT_TICKS) {
                wolf.setTarget(null);
                return false;
            }
            if (wolf.getTarget() != null) {
                wolf.setTarget(null);
            }
            wolf.getPathfinder().moveTo(targetLocation, 1.4);
            return true;
        }

        // Attacks a mob found at the target location, otherwise immediately returns to normal
        LivingEntity found = getClosestTarget(owner, targetLocation);
        if (found != null) {
            assignedTargets.put(wolfUUID, found.getUniqueId());
            wolf.setTarget(found);
            return true;
        }
        return false;
    }

    /**
     * Ends the player's active rally and returns the rallied pets to their default behaviour.
     *
     * @param playerUUID The UUID of the player.
     */
    public static void endRally(UUID playerUUID) {
        BukkitTask task = activeRallies.remove(playerUUID);
        if (task != null) {
            task.cancel();
        }

        List<UUID> pets = ralliedPets.remove(playerUUID);
        if (pets != null) {
            for (UUID petUUID : pets) {
                restoreFollowGoal(petUUID);
            }
        }
    }

    /**
     * Ends all active rallies. Called when the plugin is disabled so no pet is left without its follow owner goal.
     */
    public static void endAllRallies() {
        for (UUID playerUUID : new ArrayList<>(activeRallies.keySet())) {
            endRally(playerUUID);
        }
    }

    /**
     * Re-adds the follow owner goal that was removed from the pet when it was rallied.
     *
     * @param petUUID The UUID of the pet.
     */
    private static void restoreFollowGoal(UUID petUUID) {
        Goal<Tameable> followGoal = removedFollowGoals.remove(petUUID);
        if (followGoal == null) {
            return;
        }

        if (Bukkit.getEntity(petUUID) instanceof Tameable pet && pet.isValid()
                && !Bukkit.getMobGoals().hasGoal(pet, VanillaGoal.FOLLOW_OWNER)) {
            Bukkit.getMobGoals().addGoal(pet, FOLLOW_OWNER_PRIORITY, followGoal);
        }
    }

    /**
     * Provides the closest mob that wolves can attack near the target location.
     *
     * @param player         The player who blew the horn.
     * @param targetLocation The target location of the rally.
     * @return The closest mob, or null if there is none.
     */
    private static LivingEntity getClosestTarget(Player player, Location targetLocation) {
        LivingEntity closest = null;
        double closestDistance = Double.MAX_VALUE;
        for (Entity entity : targetLocation.getWorld().getNearbyEntities(targetLocation,
                TARGET_SEARCH_RADIUS, TARGET_SEARCH_RADIUS, TARGET_SEARCH_RADIUS)) {
            if (!isValidTarget(player, entity)) {
                continue;
            }
            double distance = entity.getLocation().distanceSquared(targetLocation);
            if (distance < closestDistance) {
                closest = (LivingEntity) entity;
                closestDistance = distance;
            }
        }
        return closest;
    }

    /**
     * Determines whether the entity is a mob that rallied wolves are allowed to attack.
     *
     * @param player The player who blew the horn.
     * @param entity The entity.
     * @return Whether rallied wolves are allowed to attack the entity.
     */
    private static boolean isValidTarget(Player player, Entity entity) {
        if (!(entity instanceof Mob mob) || mob.isDead()) {
            return false;
        }

        UUID entityUUID = entity.getUniqueId();
        if (DefenderUtils.isDefender(entityUUID)) {
            UUID defenderDominionId = DefenderUtils.getDefenderDominionId(entityUUID);
            Dominion playerDominion = DominionUtils.getPlayerDominion(player.getUniqueId());
            if (defenderDominionId == null
                    || (playerDominion != null && playerDominion.getId().equals(defenderDominionId))) {
                return false;
            }
            return DefenderUtils.shouldDefenderTarget(defenderDominionId, player);
        }
        if (MountUtils.isActiveMount(entityUUID)) {
            return canAttackOwner(player, getMountOwner(entityUUID));
        }
        if (entity instanceof Tameable tameable && tameable.isTamed()) {
            return canAttackOwner(player, tameable.getOwnerUniqueId());
        }

        // Protects name-tagged and leashed mobs
        if (entity.customName() != null || mob.isLeashed()) {
            return false;
        }

        if (entity instanceof Enemy) {
            return true;
        }
        // Villagers, golems, and allays are never attacked
        if (entity instanceof AbstractVillager || entity instanceof Golem || entity instanceof Allay) {
            return false;
        }
        return canHarmPassiveMob(player, entity);
    }

    /**
     * Determines whether the player has PvP access against the owner of a pet or mount.
     *
     * @param player    The player who blew the horn.
     * @param ownerUUID The UUID of the mob's owner.
     * @return Whether the player is allowed to attack the owner's mob.
     */
    private static boolean canAttackOwner(Player player, UUID ownerUUID) {
        if (ownerUUID == null || ownerUUID.equals(player.getUniqueId())) {
            return false;
        }
        Player owner = Bukkit.getPlayer(ownerUUID);
        return owner != null && DominionUtils.canAttackPlayer(player, owner);
    }

    /**
     * Provides the owner of an active mount.
     *
     * @param mountUUID The UUID of the mount entity.
     * @return The UUID of the mount's owner, or null if the owner is not online.
     */
    private static UUID getMountOwner(UUID mountUUID) {
        for (Player onlinePlayer : Bukkit.getOnlinePlayers()) {
            if (mountUUID.equals(MountUtils.getActiveMountEntityUUID(onlinePlayer.getUniqueId()))) {
                return onlinePlayer.getUniqueId();
            }
        }
        return null;
    }

    /**
     * Determines whether the player is allowed to harm a passive mob at its location.
     *
     * @param player The player who blew the horn.
     * @param entity The passive mob.
     * @return Whether the player is allowed to harm the passive mob.
     */
    private static boolean canHarmPassiveMob(Player player, Entity entity) {
        Dominion chunkDominion = DominionUtils.getDominionOfChunkAnywhere(entity.getLocation().getChunk());
        if (chunkDominion == null || AranarthUtils.getPlayer(player.getUniqueId()).isInAdminMode()) {
            return true;
        }
        Dominion playerDominion = DominionUtils.getPlayerDominion(player.getUniqueId());
        return playerDominion != null
                && (playerDominion.isSameDominion(chunkDominion) || playerDominion.isEnemied(chunkDominion));
    }

}
