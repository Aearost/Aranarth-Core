package com.aearost.aranarthcore.utils;

import com.aearost.aranarthcore.AranarthCore;
import com.aearost.aranarthcore.objects.CustomKeys;
import com.aearost.aranarthcore.objects.DefenderType;
import com.aearost.aranarthcore.objects.Dominion;
import com.aearost.aranarthcore.objects.DominionPermission;
import com.aearost.aranarthcore.objects.DominionRank;
import com.aearost.aranarthcore.objects.Outpost;
import org.bukkit.Bukkit;
import org.bukkit.Chunk;
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
 * Manages all Defender data, spawning, and entity tracking for Dominions.
 */
public class DefenderUtils {

    private static final int[] DEFENDER_LIMITS = {5, 15, 30, 60, 100};
    private static final Map<UUID, Map<DefenderType, Integer>> counts = new HashMap<>();
    private static final Map<UUID, UUID> entityToDominion = new HashMap<>();
    private static final Map<UUID, DefenderType> entityToType = new HashMap<>();
    private static final Map<UUID, List<UUID>> dominionToEntities = new HashMap<>();
    private static final Map<UUID, org.bukkit.Location> entityToLastLocation = new HashMap<>();

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

    public static Map<UUID, Map<DefenderType, Integer>> getAllCounts() {
        return counts;
    }

    public static Map<UUID, UUID> getEntityToDominion() {
        return entityToDominion;
    }

    /**
     * Returns the last known location cache, used as a fallback during save.
     */
    public static Map<UUID, org.bukkit.Location> getEntityToLastLocation() {
        return entityToLastLocation;
    }

    public static void loadAndSpawnAt(UUID dominionId, DefenderType type, org.bukkit.Location location) {
        if (location == null || location.getWorld() == null) {
            return;
        }
        Entity entity = location.getWorld().spawnEntity(location, type.getEntityType());
        applyDefenderStats(entity, dominionId, type);
        counts.computeIfAbsent(dominionId, k -> new EnumMap<>(DefenderType.class))
                .merge(type, 1, Integer::sum);
    }

    public static void spawnAndRegisterNew(Dominion dominion, DefenderType type) {
        spawnEntity(dominion, type);
    }

    private static void spawnEntity(Dominion dominion, DefenderType type) {
        if (dominion.getDominionHome() == null || dominion.getDominionHome().getWorld() == null) {
            return;
        }
        Entity entity = dominion.getDominionHome().getWorld()
                .spawnEntity(dominion.getDominionHome(), type.getEntityType());
        applyDefenderStats(entity, dominion.getId(), type);
    }

    /**
     * Applies custom stats and tags to a freshly spawned defender entity.
     */
    public static void applyDefenderStats(Entity entity, UUID dominionId, DefenderType type) {
        if (!(entity instanceof LivingEntity living)) {
            return;
        }

        // Tag the entity with PersistentDataContainer
        entity.getPersistentDataContainer().set(
                CustomKeys.DEFENDER_DOMINION_ID, PersistentDataType.STRING, dominionId.toString());
        entity.getPersistentDataContainer().set(
                CustomKeys.DEFENDER_TYPE, PersistentDataType.STRING, type.name());

        // Custom name
        Dominion nameDominion = DominionUtils.getDominionById(dominionId);
        String defenderName = nameDominion != null
                ? "&7Defender of &e" + nameDominion.getName()
                : "&7Defender";
        living.setCustomName(ChatUtils.translateToColor(defenderName));
        living.setCustomNameVisible(true);

        // Max health and current health
        if (living.getAttribute(Attribute.MAX_HEALTH) != null) {
            living.getAttribute(Attribute.MAX_HEALTH).setBaseValue(type.getMaxHealth());
        }
        living.setHealth(type.getMaxHealth());

        // Permanent potion effects
        for (PotionEffect effect : type.getPermanentEffects()) {
            living.addPotionEffect(effect);
        }

        // Force adult form
        if (living instanceof Ageable ageable) {
            ageable.setAdult();
        }

        // Clear all equipment and prevent item pickup
        if (living instanceof Mob mob) {
            mob.setRemoveWhenFarAway(false);
            mob.setCanPickupItems(false);
            if (mob.getEquipment() != null) {
                mob.getEquipment().clear();
                // Skeletons need a bow to function as ranged attackers
                if (type == DefenderType.SKELETON) {
                    mob.getEquipment().setItemInMainHand(new ItemStack(org.bukkit.Material.BOW));
                }
            }
        }

        // Track in memory
        UUID entityUUID = entity.getUniqueId();
        entityToDominion.put(entityUUID, dominionId);
        entityToType.put(entityUUID, type);
        dominionToEntities.computeIfAbsent(dominionId, k -> new ArrayList<>()).add(entityUUID);
        entityToLastLocation.put(entityUUID, entity.getLocation());
    }

    /**
     * Re-registers an existing world entity as a defender after a server restart.
     */
    public static void reRegisterEntity(Entity entity, UUID dominionId, DefenderType type) {
        if (!(entity instanceof LivingEntity living)) {
            return;
        }

        // Re-tag PDC in case it was somehow lost
        entity.getPersistentDataContainer().set(
                CustomKeys.DEFENDER_DOMINION_ID, PersistentDataType.STRING, dominionId.toString());
        entity.getPersistentDataContainer().set(
                CustomKeys.DEFENDER_TYPE, PersistentDataType.STRING, type.name());

        // Re-apply name
        Dominion nameDominion = DominionUtils.getDominionById(dominionId);
        String defenderName = nameDominion != null ? "Defender of " + nameDominion.getName() : "Defender";
        living.setCustomName(ChatUtils.translateToColor("&c" + defenderName));
        living.setCustomNameVisible(true);

        // Re-apply max health attribute (but keep current HP)
        if (living.getAttribute(Attribute.MAX_HEALTH) != null) {
            living.getAttribute(Attribute.MAX_HEALTH).setBaseValue(type.getMaxHealth());
        }

        // Re-apply permanent potion effects
        for (PotionEffect effect : type.getPermanentEffects()) {
            living.addPotionEffect(effect);
        }

        // Force adult
        if (living instanceof Ageable ageable) {
            ageable.setAdult();
        }

        // Mob flags and equipment
        if (living instanceof Mob mob) {
            mob.setRemoveWhenFarAway(false);
            mob.setCanPickupItems(false);
            if (mob.getEquipment() != null) {
                mob.getEquipment().clear();
                if (type == DefenderType.SKELETON) {
                    mob.getEquipment().setItemInMainHand(new org.bukkit.inventory.ItemStack(org.bukkit.Material.BOW));
                }
            }
        }

        // Restore tracking
        UUID entityUUID = entity.getUniqueId();
        entityToDominion.put(entityUUID, dominionId);
        entityToType.put(entityUUID, type);
        dominionToEntities.computeIfAbsent(dominionId, k -> new ArrayList<>()).add(entityUUID);
        counts.computeIfAbsent(dominionId, k -> new EnumMap<>(DefenderType.class))
                .merge(type, 1, Integer::sum);
    }

    /**
     * Attempts to purchase one defender of the given type for the dominion.
     *
     * @return A chat-formatted result message.
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
     * Attempts to sell one defender of the given type for the dominion.
     *
     * @return A chat-formatted result message.
     */
    public static String sellDefender(Dominion dominion, DefenderType type) {
        int current = getDefenderCount(dominion.getId(), type);
        if (current <= 0) {
            return "&7You do not have any &e" + type.getDisplayName() + " &edefenders &7to sell";
        }

        // Find and remove one entity of this type
        List<UUID> entities = dominionToEntities.get(dominion.getId());
        UUID toRemove = null;
        if (entities != null) {
            for (UUID eUUID : new ArrayList<>(entities)) {
                if (type.equals(entityToType.get(eUUID))) {
                    toRemove = eUUID;
                    break;
                }
            }
        }
        if (toRemove != null) {
            removeEntityFromTracking(toRemove);
            Entity entity = Bukkit.getEntity(toRemove);
            if (entity != null) {
                entity.remove();
            }
        }

        // Decrement count
        Map<DefenderType, Integer> typeCounts = counts.get(dominion.getId());
        if (typeCounts != null) {
            int newCount = typeCounts.getOrDefault(type, 1) - 1;
            if (newCount <= 0) {
                typeCounts.remove(type);
            } else {
                typeCounts.put(type, newCount);
            }
        }

        double refund = type.getSellPrice();
        dominion.setBalance(dominion.getBalance() + refund);
        DominionUtils.updateDominion(dominion);
        return "&7A &e" + type.getDisplayName()
                + " defender &7has been sold for &6$" + NumberFormat.getInstance().format((long) refund);
    }

    public static void onDefenderDeath(UUID entityUUID) {
        UUID dominionId = entityToDominion.get(entityUUID);
        DefenderType type = entityToType.get(entityUUID);
        if (dominionId == null || type == null) {
            return;
        }

        removeEntityFromTracking(entityUUID);

        Map<DefenderType, Integer> typeCounts = counts.get(dominionId);
        if (typeCounts != null) {
            int newCount = typeCounts.getOrDefault(type, 1) - 1;
            if (newCount <= 0) {
                typeCounts.remove(type);
            } else {
                typeCounts.put(type, newCount);
            }
        }
    }

    private static void removeEntityFromTracking(UUID entityUUID) {
        UUID dominionId = entityToDominion.remove(entityUUID);
        entityToType.remove(entityUUID);
        entityToLastLocation.remove(entityUUID);
        if (dominionId != null) {
            List<UUID> entities = dominionToEntities.get(dominionId);
            if (entities != null) {
                entities.remove(entityUUID);
            }
        }
    }

    /**
     * Determines whether a defender should target the given player, respecting the PvP permission rules.
     */
    public static boolean shouldDefenderTarget(UUID defenderDominionId, Player target) {
        Dominion defenderDominion = DominionUtils.getDominionById(defenderDominionId);
        if (defenderDominion == null) {
            return false;
        }

        Dominion targetDominion = DominionUtils.getPlayerDominion(target.getUniqueId());

        // Same dominion members — only target if member PvP is enabled
        if (targetDominion != null && targetDominion.isSameDominion(defenderDominion)) {
            return defenderDominion.isMemberPvpEnabled();
        }

        // Get relation from defenderDominion's perspective toward targetDominion
        DominionRank relation = (targetDominion == null)
                ? DominionRank.WANDERER
                : DominionUtils.getRelationKey(defenderDominion, targetDominion);

        // Forces targeting for this relation
        if (defenderDominion.getDominionPermissions().hasPermission(relation, DominionPermission.DEFENDER_TARGETING)) {
            return true;
        }

        // Mirror handlePvP logic (defender dominion is attacker, target player is target)
        if (targetDominion != null) {
            if (relation == DominionRank.ALLIED || relation == DominionRank.TRUCED) {
                // Both sides must have PVP enabled
                boolean defenderPvp = defenderDominion.getDominionPermissions()
                        .hasPermission(relation, DominionPermission.PVP);
                boolean targetPvp = targetDominion.getDominionPermissions()
                        .hasPermission(relation, DominionPermission.PVP);
                return defenderPvp && targetPvp;
            }
            if (relation == DominionRank.NEUTRAL) {
                // Neutral players in the defender's own land are attackable
                return true;
            }
            if (relation == DominionRank.ENEMIED) {
                return true;
            }
        }

        // Target is a wanderer (no dominion), attackable by any dominion member
        return true;
    }

    /**
     * Starts a repeating task that walks defenders back toward their dominion's land.
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

                    UUID dominionId = entry.getValue();
                    Dominion dominion = DominionUtils.getDominionById(dominionId);
                    if (dominion == null || dominion.getChunks().isEmpty()) {
                        continue;
                    }

                    Chunk defenderChunk = entity.getLocation().getChunk();

                    boolean withinBounds = isWithinOneChebyshev(defenderChunk, dominion.getChunks());

                    if (!withinBounds) {
                        for (Outpost outpost : OutpostUtils.getDominionOutposts(dominionId)) {
                            if (isWithinOneChebyshev(defenderChunk, outpost.getChunks())) {
                                withinBounds = true;
                                break;
                            }
                        }
                    }

                    if (!withinBounds && entity instanceof Mob mob) {
                        // Find the nearest claimed chunk and walk toward its centre
                        Chunk nearest = findNearestChunk(defenderChunk, dominion.getChunks());
                        if (nearest == null) {
                            for (Outpost outpost : OutpostUtils.getDominionOutposts(dominionId)) {
                                Chunk candidate = findNearestChunk(defenderChunk, outpost.getChunks());
                                if (candidate != null && (nearest == null
                                        || chunkDistance(defenderChunk, candidate) < chunkDistance(defenderChunk, nearest))) {
                                    nearest = candidate;
                                }
                            }
                        }
                        if (nearest != null) {
                            org.bukkit.Location target = nearest.getWorld().getBlockAt(
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

    /**
     * Returns the chunk in the list closest (by Chebyshev distance) to the defender's chunk,
     * or null if the list is empty.
     */
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

    /**
     * Returns true if the given chunk is within Chebyshev distance 1 of any chunk in the list.
     * This allows movement to all 8 surrounding chunks (including diagonals).
     */
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

    /**
     * Forces defenders to acquire the nearest hostile mob target if they don't already have one.
     */
    public static void startTargetingTask() {
        // Runs every 2 seconds
        new BukkitRunnable() {
            @Override
            public void run() {
                for (Map.Entry<UUID, UUID> entry : new HashMap<>(entityToDominion).entrySet()) {
                    Entity entity = Bukkit.getEntity(entry.getKey());
                    if (!(entity instanceof Mob mob) || mob.isDead()) {
                        continue;
                    }

                    UUID myDominionId = entry.getValue();

                    // If already locked onto a valid hostile, leave it alone
                    LivingEntity currentTarget = mob.getTarget();
                    if (currentTarget != null && !currentTarget.isDead()
                            && currentTarget instanceof Monster) {
                        UUID tUUID = currentTarget.getUniqueId();
                        boolean isSameDominionDefender = isDefender(tUUID)
                                && myDominionId.equals(entityToDominion.get(tUUID));
                        if (!isSameDominionDefender) {
                            continue;
                        }
                    }

                    // Scan for valid hostiles, prioritizing those actively targeting a member of this dominion
                    Dominion dominion = DominionUtils.getDominionById(myDominionId);
                    LivingEntity priorityTarget = null;
                    double priorityDistSq = Double.MAX_VALUE;
                    LivingEntity fallbackTarget = null;
                    double fallbackDistSq = Double.MAX_VALUE;

                    for (Entity nearby : mob.getNearbyEntities(16, 8, 16)) {
                        if (!(nearby instanceof Monster nearbyMonster)) {
                            continue;
                        }
                        UUID nUUID = nearby.getUniqueId();
                        if (isDefender(nUUID) && myDominionId.equals(entityToDominion.get(nUUID))) {
                            continue;
                        }

                        double distSq = nearby.getLocation().distanceSquared(mob.getLocation());

                        // This mob is targeting a dominion member
                        if (dominion != null
                                && nearbyMonster instanceof Mob nearbyMob
                                && nearbyMob.getTarget() instanceof Player targetPlayer
                                && dominion.getMembers().contains(targetPlayer.getUniqueId())
                                && distSq < priorityDistSq) {
                            priorityDistSq = distSq;
                            priorityTarget = nearbyMonster;
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
     * Starts the slow health-regeneration task for all tracked defenders.
     */
    public static void startRegenTask() {
        new BukkitRunnable() {
            @Override
            public void run() {
                for (Map.Entry<UUID, DefenderType> entry : new HashMap<>(entityToType).entrySet()) {
                    Entity entity = Bukkit.getEntity(entry.getKey());
                    if (entity instanceof LivingEntity living && !living.isDead()) {
                        // Cache location while the entity is loaded
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
}
