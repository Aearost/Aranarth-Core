package com.aearost.aranarthcore.utils;

import com.aearost.aranarthcore.AranarthCore;
import com.aearost.aranarthcore.objects.CustomKeys;
import com.aearost.aranarthcore.objects.DefenderMode;
import com.aearost.aranarthcore.objects.DefenderType;
import com.aearost.aranarthcore.objects.Dominion;
import com.aearost.aranarthcore.objects.DominionPermission;
import com.aearost.aranarthcore.objects.DominionRank;
import com.aearost.aranarthcore.objects.Outpost;
import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.*;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.potion.PotionEffect;
import org.bukkit.scheduler.BukkitRunnable;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Manages all Defender data, spawning, entity tracking, and behaviour modes for Dominions.
 */
public class DefenderUtils {

    private static final int[] DEFENDER_LIMITS = {5, 15, 30, 60, 100};

    // Core tracking
    private static final Map<UUID, Map<DefenderType, Integer>> counts = new HashMap<>();
    private static final Map<UUID, UUID> entityToDominion = new HashMap<>();
    private static final Map<UUID, DefenderType> entityToType = new HashMap<>();
    private static final Map<UUID, List<UUID>> dominionToEntities = new HashMap<>();
    private static final Map<UUID, Location> entityToLastLocation = new HashMap<>();

    // Mode tracking
    private static final Map<UUID, DefenderMode> entityToMode = new HashMap<>();
    private static final Map<UUID, Location> entityToGuardPosition = new HashMap<>();
    private static final Map<UUID, UUID> entityToFollowPlayer = new HashMap<>();
    private static final Map<UUID, List<UUID>> playerToFollowers = new HashMap<>();

    // Location assignment where non-null is the outpost UUID
    private static final Map<UUID, UUID> entityToAssignedOutpost = new HashMap<>();

    // Stuck detection
    private static final Map<UUID, Location> entityToStuckCheckLoc = new HashMap<>();
    private static final Map<UUID, Integer> entityToStuckCount = new HashMap<>();

    public static int getDefenderCount(UUID dominionId, DefenderType type) {
        Map<DefenderType, Integer> typeCounts = counts.get(dominionId);
        if (typeCounts == null) {
            return 0;
        }
        return typeCounts.getOrDefault(type, 0);
    }

    public static int getTotalDefenderCount(UUID dominionId) {
        Map<DefenderType, Integer> typeCounts = counts.get(dominionId);
        if (typeCounts == null) {
            return 0;
        }
        return typeCounts.values().stream().mapToInt(Integer::intValue).sum();
    }

    public static int getDefenderLimit(int dominionLevel) {
        if (dominionLevel < 1) {
            return DEFENDER_LIMITS[0];
        }
        if (dominionLevel > 5) {
            return DEFENDER_LIMITS[4];
        }
        return DEFENDER_LIMITS[dominionLevel - 1];
    }

    public static boolean isDefender(UUID entityUUID) {
        return entityToDominion.containsKey(entityUUID);
    }

    public static UUID getDefenderDominionId(UUID entityUUID) {
        return entityToDominion.get(entityUUID);
    }

    public static DefenderType getDefenderType(UUID entityUUID) {
        return entityToType.get(entityUUID);
    }

    public static DefenderMode getDefenderMode(UUID entityUUID) {
        return entityToMode.getOrDefault(entityUUID, DefenderMode.PATROL);
    }

    public static Location getGuardPosition(UUID entityUUID) {
        return entityToGuardPosition.get(entityUUID);
    }

    public static UUID getFollowPlayerId(UUID entityUUID) {
        return entityToFollowPlayer.get(entityUUID);
    }

    public static List<UUID> getFollowDefenders(UUID playerUUID) {
        return playerToFollowers.getOrDefault(playerUUID, new ArrayList<>());
    }

    /**
     * Returns true if this defender is in FOLLOW mode and the given player is NOT the one
     * being followed. The dominion leader is always exempt from the lock.
     */
    public static boolean isFollowLockedFor(UUID entityUUID, UUID requestingPlayer, UUID dominionLeader) {
        if (getDefenderMode(entityUUID) != DefenderMode.FOLLOW) {
            return false;
        }
        UUID followPlayer = entityToFollowPlayer.get(entityUUID);
        if (followPlayer == null) {
            return false;
        }
        return !requestingPlayer.equals(followPlayer) && !requestingPlayer.equals(dominionLeader);
    }

    public static UUID getAssignedOutpostId(UUID entityUUID) {
        return entityToAssignedOutpost.get(entityUUID);
    }

    /**
     * Assigns a defender to a specific outpost.
     */
    public static void setAssignedOutpost(UUID entityUUID, UUID outpostId) {
        if (outpostId == null) {
            entityToAssignedOutpost.remove(entityUUID);
        } else {
            entityToAssignedOutpost.put(entityUUID, outpostId);
        }
        Entity entity = Bukkit.getEntity(entityUUID);
        if (entity != null) {
            if (outpostId != null) {
                entity.getPersistentDataContainer().set(
                        CustomKeys.DEFENDER_ASSIGNED_OUTPOST, PersistentDataType.STRING, outpostId.toString());
            } else {
                entity.getPersistentDataContainer().remove(CustomKeys.DEFENDER_ASSIGNED_OUTPOST);
            }
        }
    }

    // Persistence accessors (used by PersistenceUtils.saveDefenders)
    public static Map<UUID, UUID> getEntityToDominion() {
        return entityToDominion;
    }

    public static Map<UUID, Location> getEntityToLastLocation() {
        return entityToLastLocation;
    }

    public static Map<UUID, UUID> getEntityToAssignedOutpost() {
        return entityToAssignedOutpost;
    }

    /**
     * Sets the behaviour mode for a defender.
     *
     * @param entityUUID     The defender entity UUID.
     * @param mode           The new mode.
     * @param followPlayerId Player UUID to follow (ignored if not following).
     * @param guardPosition  Location to guard (ignored if not guarding).
     */
    public static void setDefenderMode(UUID entityUUID, DefenderMode mode,
                                       UUID followPlayerId, Location guardPosition) {
        // Clean up old follow tracking when leaving follow mode
        if (mode != DefenderMode.FOLLOW) {
            UUID oldFollow = entityToFollowPlayer.remove(entityUUID);
            if (oldFollow != null) {
                List<UUID> followers = playerToFollowers.get(oldFollow);
                if (followers != null) {
                    followers.remove(entityUUID);
                }
            }
        }

        entityToMode.put(entityUUID, mode);

        Entity entity = Bukkit.getEntity(entityUUID);
        if (entity instanceof Mob mob) {
            mob.setAI(true);
            if (mode == DefenderMode.IDLE) {
                mob.setTarget(null);
            }
            entity.getPersistentDataContainer().set(
                    CustomKeys.DEFENDER_MODE, PersistentDataType.STRING, mode.name());
        }

        if (mode == DefenderMode.FOLLOW && followPlayerId != null) {
            // Avoid duplicate registration
            UUID existing = entityToFollowPlayer.get(entityUUID);
            if (!followPlayerId.equals(existing)) {
                if (existing != null) {
                    List<UUID> old = playerToFollowers.get(existing);
                    if (old != null) {
                        old.remove(entityUUID);
                    }
                }
                entityToFollowPlayer.put(entityUUID, followPlayerId);
                playerToFollowers.computeIfAbsent(followPlayerId, k -> new ArrayList<>()).add(entityUUID);
            }
            if (entity != null) {
                entity.getPersistentDataContainer().set(
                        CustomKeys.DEFENDER_FOLLOW_PLAYER, PersistentDataType.STRING, followPlayerId.toString());
            }
        }

        if (mode == DefenderMode.GUARD && guardPosition != null) {
            entityToGuardPosition.put(entityUUID, guardPosition);
        }

        // Reset stuck state whenever mode changes
        entityToStuckCheckLoc.remove(entityUUID);
        entityToStuckCount.remove(entityUUID);
    }

    /**
     * Returns the home location for a defender.
     */
    public static Location getDefenderHomeLocation(UUID entityUUID) {
        UUID dominionId = entityToDominion.get(entityUUID);
        if (dominionId == null) {
            return null;
        }
        UUID assignedOutpostId = entityToAssignedOutpost.get(entityUUID);
        if (assignedOutpostId != null) {
            Outpost outpost = OutpostUtils.getOutpostById(assignedOutpostId);
            if (outpost != null) {
                return outpost.getHome();
            }
        }
        Dominion dominion = DominionUtils.getDominionById(dominionId);
        return dominion != null ? dominion.getDominionHome() : null;
    }

    /**
     * Teleports all follow-mode defenders assigned to the given player to that player's location.
     */
    public static void teleportFollowersToPlayer(Player player) {
        List<UUID> followers = new ArrayList<>(
                playerToFollowers.getOrDefault(player.getUniqueId(), new ArrayList<>()));
        for (UUID followerUUID : followers) {
            if (getDefenderMode(followerUUID) != DefenderMode.FOLLOW) {
                continue;
            }
            Entity entity = Bukkit.getEntity(followerUUID);
            if (entity instanceof LivingEntity le && !le.isDead()) {
                entity.teleport(player.getLocation());
            }
        }
    }

    /**
     * Loads a defender from persistence with default PATROL mode.
     */
    public static void loadAndSpawnAt(UUID dominionId, DefenderType type, Location location) {
        loadAndSpawnAt(dominionId, type, location, DefenderMode.PATROL, null, null);
    }

    /**
     * Loads a defender from persistence with an explicit mode.
     * For follow mode, the location should already be the dominion home (set by PersistenceUtils).
     */
    public static void loadAndSpawnAt(UUID dominionId, DefenderType type, Location location,
                                      DefenderMode mode, UUID followPlayerId, Location guardPos) {
        loadAndSpawnAt(dominionId, type, location, mode, followPlayerId, guardPos, null);
    }

    /**
     * Loads a defender from persistence with an explicit mode and territory assignment.
     */
    public static void loadAndSpawnAt(UUID dominionId, DefenderType type, Location location,
                                      DefenderMode mode, UUID followPlayerId, Location guardPos,
                                      UUID assignedOutpostId) {
        if (location == null || location.getWorld() == null) {
            return;
        }
        Entity entity = location.getWorld().spawnEntity(location, type.getEntityType());
        applyDefenderStats(entity, dominionId, type);
        counts.computeIfAbsent(dominionId, k -> new EnumMap<>(DefenderType.class))
                .merge(type, 1, Integer::sum);
        setDefenderMode(entity.getUniqueId(), mode, followPlayerId, guardPos);
        if (assignedOutpostId != null) {
            entityToAssignedOutpost.put(entity.getUniqueId(), assignedOutpostId);
        }
    }

    private static void spawnEntity(Dominion dominion, DefenderType type) {
        if (dominion.getDominionHome() == null || dominion.getDominionHome().getWorld() == null) {
            return;
        }
        Entity entity = dominion.getDominionHome().getWorld()
                .spawnEntity(dominion.getDominionHome(), type.getEntityType());
        applyDefenderStats(entity, dominion.getId(), type);
        // New purchases default to patrol
        entityToMode.put(entity.getUniqueId(), DefenderMode.PATROL);
    }

    /**
     * Applies custom stats and tags to a freshly spawned defender entity.
     */
    public static void applyDefenderStats(Entity entity, UUID dominionId, DefenderType type) {
        if (!(entity instanceof LivingEntity living)) {
            return;
        }

        entity.getPersistentDataContainer().set(
                CustomKeys.DEFENDER_DOMINION_ID, PersistentDataType.STRING, dominionId.toString());
        entity.getPersistentDataContainer().set(
                CustomKeys.DEFENDER_TYPE, PersistentDataType.STRING, type.name());

        Dominion nameDominion = DominionUtils.getDominionById(dominionId);
        String defenderName = nameDominion != null
                ? "&7Defender of &e" + nameDominion.getName()
                : "&7Defender";
        living.setCustomName(ChatUtils.translateToColor(defenderName));
        living.setCustomNameVisible(true);

        if (living.getAttribute(Attribute.MAX_HEALTH) != null) {
            living.getAttribute(Attribute.MAX_HEALTH).setBaseValue(type.getMaxHealth());
        }
        living.setHealth(type.getMaxHealth());

        for (PotionEffect effect : type.getPermanentEffects()) {
            living.addPotionEffect(effect);
        }

        if (living instanceof Ageable ageable) {
            ageable.setAdult();
        }

        if (living instanceof Mob mob) {
            mob.setRemoveWhenFarAway(false);
            mob.setCanPickupItems(false);
            if (mob.getEquipment() != null) {
                mob.getEquipment().clear();
                if (type == DefenderType.SKELETON) {
                    mob.getEquipment().setItemInMainHand(new ItemStack(org.bukkit.Material.BOW));
                }
            }
        }

        UUID entityUUID = entity.getUniqueId();
        entityToDominion.put(entityUUID, dominionId);
        entityToType.put(entityUUID, type);
        dominionToEntities.computeIfAbsent(dominionId, k -> new ArrayList<>()).add(entityUUID);
        entityToLastLocation.put(entityUUID, entity.getLocation());
    }

    /**
     * Attempts to purchase one defender of the given type for the dominion.
     */
    public static String purchaseDefender(Dominion dominion, DefenderType type) {
        int limit = getDefenderLimit(dominion.getDominionLevel());
        int current = getTotalDefenderCount(dominion.getId());
        if (current >= limit) {
            return "&cYour dominion cannot purchase any more defenders";
        }
        double price = type.getPurchasePrice();
        if (dominion.getBalance() < price) {
            return "&cYour dominion cannot afford this";
        }
        dominion.setBalance(dominion.getBalance() - price);
        counts.computeIfAbsent(dominion.getId(), k -> new EnumMap<>(DefenderType.class))
                .merge(type, 1, Integer::sum);
        spawnEntity(dominion, type);
        DominionUtils.updateDominion(dominion);
        return "&7A &e" + type.getDisplayName()
                + " defender &7has been purchased for &6$" + NumberFormat.getInstance().format((long) price);
    }

    /**
     * Sells a defender and refunds the dominion.
     *
     * @param dominion   The owning dominion.
     * @param type       The defender type to sell.
     * @param entityUUID The specific entity to sell, or {@code null} to pick an arbitrary one of {@code type}.
     */
    public static String sellDefender(Dominion dominion, DefenderType type, UUID entityUUID) {
        if (entityUUID == null) {
            // Selling any tracked entity of this type
            if (getDefenderCount(dominion.getId(), type) <= 0) {
                return "&cYou do not have any &e" + type.getDisplayName() + " &cdefenders &7to sell";
            }
            List<UUID> entities = dominionToEntities.get(dominion.getId());
            if (entities != null) {
                for (UUID eUUID : new ArrayList<>(entities)) {
                    if (type.equals(entityToType.get(eUUID))) {
                        entityUUID = eUUID;
                        break;
                    }
                }
            }
        } else {
            // Selling a specific defender via the manage defender GUI
            if (!entityToType.containsKey(entityUUID)) {
                return "&cThis defender could not be found";
            }
        }

        if (entityUUID != null) {
            removeEntityFromTracking(entityUUID);
            Entity entity = Bukkit.getEntity(entityUUID);
            if (entity != null) {
                entity.remove();
            }
        }

        decrementCount(dominion.getId(), type);

        double refund = type.getSellPrice();
        dominion.setBalance(dominion.getBalance() + refund);
        DominionUtils.updateDominion(dominion);
        return "&7A &e" + type.getDisplayName()
                + " defender &7has been sold for &6$" + NumberFormat.getInstance().format((long) refund);
    }

    /**
     * Removes references of the Defender upon their death.
     */
    public static void onDefenderDeath(UUID entityUUID) {
        UUID dominionId = entityToDominion.get(entityUUID);
        DefenderType type = entityToType.get(entityUUID);
        if (dominionId == null || type == null) {
            return;
        }

        removeEntityFromTracking(entityUUID);
        decrementCount(dominionId, type);
    }

    private static void removeEntityFromTracking(UUID entityUUID) {
        UUID dominionId = entityToDominion.remove(entityUUID);
        entityToType.remove(entityUUID);
        entityToLastLocation.remove(entityUUID);

        // Mode and location cleanup
        entityToMode.remove(entityUUID);
        entityToGuardPosition.remove(entityUUID);
        entityToAssignedOutpost.remove(entityUUID);
        UUID followPlayerId = entityToFollowPlayer.remove(entityUUID);
        if (followPlayerId != null) {
            List<UUID> followers = playerToFollowers.get(followPlayerId);
            if (followers != null) {
                followers.remove(entityUUID);
            }
        }
        entityToStuckCheckLoc.remove(entityUUID);
        entityToStuckCount.remove(entityUUID);

        if (dominionId != null) {
            List<UUID> entities = dominionToEntities.get(dominionId);
            if (entities != null) {
                entities.remove(entityUUID);
            }
        }
    }

    /**
     * Determines whether a defender should target the given player, respecting PvP rules.
     */
    public static boolean shouldDefenderTarget(UUID defenderDominionId, Player target) {
        Dominion defenderDominion = DominionUtils.getDominionById(defenderDominionId);
        if (defenderDominion == null) {
            return false;
        }

        Dominion targetDominion = DominionUtils.getPlayerDominion(target.getUniqueId());

        if (targetDominion != null && targetDominion.isSameDominion(defenderDominion)) {
            return defenderDominion.isMemberPvpEnabled();
        }

        DominionRank relation = (targetDominion == null)
                ? DominionRank.WANDERER
                : DominionUtils.getRelationKey(defenderDominion, targetDominion);

        if (defenderDominion.getDominionPermissions()
                .hasPermission(relation, DominionPermission.DEFENDER_TARGETING)) {
            return true;
        }

        if (targetDominion != null) {
            if (relation == DominionRank.ALLIED || relation == DominionRank.TRUCED) {
                boolean defenderPvp = defenderDominion.getDominionPermissions()
                        .hasPermission(relation, DominionPermission.PVP);
                boolean targetPvp = targetDominion.getDominionPermissions()
                        .hasPermission(relation, DominionPermission.PVP);
                return defenderPvp && targetPvp;
            }
            if (relation == DominionRank.NEUTRAL || relation == DominionRank.ENEMIED) {
                return true;
            }
        }
        return true;
    }

    /**
     * Implements boundary enforcement for patrolling defenders, handling stuck defenders by teleporting them home.
     */
    public static void startBoundaryTask() {
        new BukkitRunnable() {
            @Override
            public void run() {
                for (Map.Entry<UUID, UUID> entry : new HashMap<>(entityToDominion).entrySet()) {
                    Entity entity = Bukkit.getEntity(entry.getKey());
                    if (!(entity instanceof LivingEntity living) || living.isDead()) {
                        continue;
                    }

                    UUID entityUUID = entry.getKey();
                    DefenderMode mode = getDefenderMode(entityUUID);

                    // Only patrolling defenders use territory boundary enforcement
                    if (mode != DefenderMode.PATROL) {
                        clearStuckState(entityUUID);
                        continue;
                    }

                    UUID dominionId = entry.getValue();
                    Dominion dominion = DominionUtils.getDominionById(dominionId);
                    if (dominion == null || dominion.getChunks().isEmpty()) {
                        continue;
                    }

                    Chunk defenderChunk = entity.getLocation().getChunk();
                    UUID assignedOutpostId = getAssignedOutpostId(entityUUID);
                    List<Chunk> assignedChunks;
                    if (assignedOutpostId != null) {
                        Outpost assignedOutpost = OutpostUtils.getOutpostById(assignedOutpostId);
                        assignedChunks = assignedOutpost != null ? assignedOutpost.getChunks() : dominion.getChunks();
                    } else {
                        assignedChunks = dominion.getChunks();
                    }

                    boolean withinBounds = isWithinOneChebyshev(defenderChunk, assignedChunks);

                    if (withinBounds) {
                        clearStuckState(entityUUID);
                        continue;
                    }

                    // Out of bounds - pathfind back
                    if (entity instanceof Mob mob) {
                        // Teleport home if unable to pathfind back within 30 seconds
                        if (checkStuck(entityUUID, entity, 6)) {
                            Location home = getDefenderHomeLocation(entityUUID);
                            if (home != null) {
                                entity.teleport(home);
                                setDefenderMode(entityUUID, DefenderMode.PATROL, null, null);
                            }
                            continue;
                        }

                        Chunk nearest = findNearestChunk(defenderChunk, assignedChunks);
                        if (nearest != null) {
                            Location target = nearest.getWorld().getBlockAt(
                                    (nearest.getX() << 4) + 8,
                                    entity.getLocation().getBlockY(),
                                    (nearest.getZ() << 4) + 8).getLocation();
                            mob.getPathfinder().moveTo(target, 1.2);
                        }
                    }
                }
            }
        }.runTaskTimer(AranarthCore.getInstance(), 100L, 100L);
    }

    public static void startTargetingTask() {
        new BukkitRunnable() {
            @Override
            public void run() {
                for (Map.Entry<UUID, UUID> entry : new HashMap<>(entityToDominion).entrySet()) {
                    Entity entity = Bukkit.getEntity(entry.getKey());
                    if (!(entity instanceof Mob mob) || mob.isDead()) {
                        continue;
                    }

                    UUID entityUUID = entry.getKey();
                    UUID myDominionId = entry.getValue();
                    DefenderMode mode = getDefenderMode(entityUUID);

                    if (mode == DefenderMode.IDLE) {
                        continue;
                    }

                    // Validate and clear the current target
                    LivingEntity currentTarget = mob.getTarget();
                    if (currentTarget != null && !currentTarget.isDead()
                            && currentTarget instanceof Monster) {
                        UUID tUUID = currentTarget.getUniqueId();
                        boolean isSameDominionDefender = isDefender(tUUID)
                                && myDominionId.equals(entityToDominion.get(tUUID));
                        if (!isSameDominionDefender) {
                            if (mode == DefenderMode.GUARD) {
                                // Drop target if it has wandered outside the 30-block guard radius
                                Location guardPos = entityToGuardPosition.get(entityUUID);
                                if (guardPos != null && guardPos.getWorld() != null
                                        && guardPos.getWorld().equals(currentTarget.getLocation().getWorld())
                                        && guardPos.distanceSquared(currentTarget.getLocation()) > 900) {
                                    mob.setTarget(null);
                                } else {
                                    continue;
                                }
                            } else {
                                // Valid for patrol and follow modes
                                continue;
                            }
                        }
                    }

                    Dominion dominion = DominionUtils.getDominionById(myDominionId);
                    LivingEntity priorityTarget = null;
                    double priorityDistSq = Double.MAX_VALUE;
                    LivingEntity fallbackTarget = null;
                    double fallbackDistSq = Double.MAX_VALUE;

                    // Guard mode will filter by 30-block guard distance
                    double scanRadius = 16;
                    double maxGuardDistSq = Double.MAX_VALUE;
                    Location guardPos = null;
                    if (mode == DefenderMode.GUARD) {
                        guardPos = entityToGuardPosition.get(entityUUID);
                        if (guardPos != null) {
                            scanRadius = 30;
                            maxGuardDistSq = 900; // 30^2
                        }
                    }

                    for (Entity nearby : mob.getNearbyEntities(scanRadius, 8, scanRadius)) {
                        if (!(nearby instanceof Monster nearbyMonster)) {
                            continue;
                        }
                        UUID nUUID = nearby.getUniqueId();
                        if (isDefender(nUUID) && myDominionId.equals(entityToDominion.get(nUUID))) {
                            continue;
                        }

                        // Guard mode will reject targets outside 30 blocks of guard position
                        if (mode == DefenderMode.GUARD && guardPos != null
                                && guardPos.getWorld() != null
                                && guardPos.getWorld().equals(nearby.getLocation().getWorld())
                                && guardPos.distanceSquared(nearby.getLocation()) > maxGuardDistSq) {
                            continue;
                        }

                        double distSq = nearby.getLocation().distanceSquared(mob.getLocation());

                        if (nearbyMonster instanceof Mob nearbyMob
                                && nearbyMob.getTarget() instanceof Player targetPlayer) {
                            boolean isPriority = false;
                            if (mode == DefenderMode.FOLLOW) {
                                UUID followId = entityToFollowPlayer.get(entityUUID);
                                isPriority = followId != null
                                        && targetPlayer.getUniqueId().equals(followId);
                            } else if (dominion != null) {
                                isPriority = dominion.getMembers().contains(targetPlayer.getUniqueId());
                            }
                            if (isPriority && distSq < priorityDistSq) {
                                priorityDistSq = distSq;
                                priorityTarget = nearbyMonster;
                            }
                        }

                        if (distSq < fallbackDistSq) {
                            fallbackDistSq = distSq;
                            fallbackTarget = nearbyMonster;
                        }
                    }

                    LivingEntity chosen = priorityTarget != null ? priorityTarget : fallbackTarget;
                    if (chosen != null) {
                        mob.setTarget(chosen);
                    }
                }
            }
        }.runTaskTimer(AranarthCore.getInstance(), 40L, 40L);
    }

    /**
     * Starts the follow task for defenders marked in follow mode.
     */
    public static void startFollowTask() {
        new BukkitRunnable() {
            @Override
            public void run() {
                for (Map.Entry<UUID, UUID> entry : new HashMap<>(entityToFollowPlayer).entrySet()) {
                    UUID entityUUID = entry.getKey();
                    UUID followPlayerId = entry.getValue();

                    if (getDefenderMode(entityUUID) != DefenderMode.FOLLOW) {
                        continue;
                    }

                    Entity entity = Bukkit.getEntity(entityUUID);
                    if (!(entity instanceof Mob mob) || mob.isDead()) {
                        continue;
                    }

                    Player followPlayer = Bukkit.getPlayer(followPlayerId);
                    if (followPlayer == null || !followPlayer.isOnline()) {
                        continue;
                    }

                    if (!followPlayer.getWorld().getName().startsWith("world")) {
                        continue;
                    }

                    // Cross-world teleport
                    if (!entity.getWorld().equals(followPlayer.getWorld())) {
                        entity.teleport(followPlayer.getLocation());
                        clearStuckState(entityUUID);
                        continue;
                    }

                    double distSq = entity.getLocation().distanceSquared(followPlayer.getLocation());

                    if (distSq > 1024) { // If more than 32 blocks, teleport immediately
                        entity.teleport(followPlayer.getLocation());
                        clearStuckState(entityUUID);
                    } else if (distSq > 25) { // 5–32 blocks, try to pathfind
                        mob.getPathfinder().moveTo(followPlayer.getLocation(), 1.3);
                        // Teleport to the player if exceeding 30 seconds
                        if (checkStuck(entityUUID, entity, 15)) {
                            entity.teleport(followPlayer.getLocation());
                        }
                    } else {
                        clearStuckState(entityUUID);
                    }
                }
            }
        }.runTaskTimer(AranarthCore.getInstance(), 40L, 40L);
    }

    /**
     * Starts the guard-return task for defenders marked in guard mode.
     */
    public static void startGuardTask() {
        new BukkitRunnable() {
            @Override
            public void run() {
                for (Map.Entry<UUID, Location> entry : new HashMap<>(entityToGuardPosition).entrySet()) {
                    UUID entityUUID = entry.getKey();

                    if (getDefenderMode(entityUUID) != DefenderMode.GUARD) {
                        continue;
                    }

                    Entity entity = Bukkit.getEntity(entityUUID);
                    if (!(entity instanceof Mob mob) || mob.isDead()) {
                        continue;
                    }

                    // If actively chasing a target, leave it alone
                    if (mob.getTarget() != null && !mob.getTarget().isDead()) {
                        continue;
                    }

                    // No target — stop any ongoing pathfinding so the defender stands still
                    mob.getPathfinder().stopPathfinding();
                    clearStuckState(entityUUID);
                }
            }
        }.runTaskTimer(AranarthCore.getInstance(), 60L, 60L);
    }

    /**
     * Starts the slow health-regeneration task for all tracked defenders.
     */
    public static void startRegenTask() {
        new BukkitRunnable() {
            @Override
            public void run() {
                for (Map.Entry<UUID, DefenderType> entry : new HashMap<>(entityToType).entrySet()) {
                    Entity entity = Bukkit.getEntity(entry.getKey());
                    if (entity instanceof LivingEntity living && !living.isDead()) {
                        entityToLastLocation.put(entry.getKey(), entity.getLocation());
                        double max = entry.getValue().getMaxHealth();
                        if (living.getHealth() < max) {
                            living.setHealth(Math.min(living.getHealth() + 1.0, max));
                        }
                    }
                }
            }
        }.runTaskTimer(AranarthCore.getInstance(), 200L, 200L);
    }

    /**
     * Clears the stuck-detection state for an entity.
     */
    private static void clearStuckState(UUID entityUUID) {
        entityToStuckCheckLoc.remove(entityUUID);
        entityToStuckCount.remove(entityUUID);
    }

    /**
     * Checks whether an entity is stuck and increments its stuck counter.
     */
    private static boolean checkStuck(UUID entityUUID, Entity entity, int threshold) {
        Location lastLoc = entityToStuckCheckLoc.get(entityUUID);
        Location currentLoc = entity.getLocation();
        if (lastLoc != null && lastLoc.getWorld() != null
                && lastLoc.getWorld().equals(currentLoc.getWorld())
                && lastLoc.distanceSquared(currentLoc) < 4.0) {
            if (entityToStuckCount.merge(entityUUID, 1, Integer::sum) >= threshold) {
                clearStuckState(entityUUID);
                return true;
            }
        } else {
            entityToStuckCount.remove(entityUUID);
        }
        entityToStuckCheckLoc.put(entityUUID, currentLoc);
        return false;
    }

    /**
     * Decrements the defender type count for a dominion, removing the entry entirely if it reaches zero.
     */
    private static void decrementCount(UUID dominionId, DefenderType type) {
        Map<DefenderType, Integer> typeCounts = counts.get(dominionId);
        if (typeCounts == null) {
            return;
        }
        int newCount = typeCounts.getOrDefault(type, 1) - 1;
        if (newCount <= 0) {
            typeCounts.remove(type);
        } else {
            typeCounts.put(type, newCount);
        }
    }

    private static Chunk findNearestChunk(Chunk from, List<Chunk> candidates) {
        Chunk nearest = null;
        int bestDist = Integer.MAX_VALUE;
        for (Chunk c : candidates) {
            if (!c.getWorld().equals(from.getWorld())) {
                continue;
            }
            int dist = chunkDistance(from, c);
            if (dist < bestDist) {
                bestDist = dist;
                nearest = c;
            }
        }
        return nearest;
    }

    private static int chunkDistance(Chunk a, Chunk b) {
        return Math.max(Math.abs(a.getX() - b.getX()), Math.abs(a.getZ() - b.getZ()));
    }

    private static boolean isWithinOneChebyshev(Chunk defenderChunk, List<Chunk> claimedChunks) {
        for (Chunk claimed : claimedChunks) {
            if (!claimed.getWorld().equals(defenderChunk.getWorld())) {
                continue;
            }
            int dx = Math.abs(claimed.getX() - defenderChunk.getX());
            int dz = Math.abs(claimed.getZ() - defenderChunk.getZ());
            if (dx <= 1 && dz <= 1) {
                return true;
            }
        }
        return false;
    }
}
