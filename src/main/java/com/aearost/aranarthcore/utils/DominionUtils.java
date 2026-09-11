package com.aearost.aranarthcore.utils;

import com.aearost.aranarthcore.AranarthCore;
import com.aearost.aranarthcore.database.DatabaseManager;
import com.aearost.aranarthcore.enums.Month;
import com.aearost.aranarthcore.items.GodAppleFragment;
import com.aearost.aranarthcore.network.NetworkManager;
import com.aearost.aranarthcore.objects.*;
import org.bukkit.*;
import org.bukkit.block.Biome;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.EnchantmentStorageMeta;

import java.awt.Color;
import java.util.*;
import java.util.List;

/**
 * Provides a large variety of utility methods for everything related to land claiming.
 */
public class DominionUtils {

    public static final long CONQUEST_DEADLINE_MS = 7L * 24 * 60 * 60 * 1000; // 1 week
    public static final long CONQUER_COOLDOWN_MS = 3L * 24 * 60 * 60 * 1000; // 3 days
    public static final long REBEL_DEADLINE_MS = 7L * 24 * 60 * 60 * 1000; // 1 week
    public static final long REBEL_COOLDOWN_MS = 3L * 24 * 60 * 60 * 1000; // 3 days
    public static final long REBEL_INACTIVITY_MS = 3L * 24 * 60 * 60 * 1000; // 3 days
    public static final long CONQUEST_INACTIVITY_MS = 3L * 24 * 60 * 60 * 1000; // 3 days
    private static final List<Dominion> dominions = new ArrayList<>();
    private static final Map<UUID, Dominion> dominionById = new HashMap<>();
    private static final Map<String, Dominion> chunkKeyToDominion = new HashMap<>();
    private static final Map<UUID, Dominion> playerToDominion = new HashMap<>();
    private static final Map<UUID, UUID> foodInventoryLocks = new HashMap<>();
    /**
     * Tracks the last in-game date on which the daily food/tax cycle ran, to prevent double-firing.
     */
    private static String lastFoodEvalDate = null;
    /**
     * Dominion IDs that lost a chunk to auto-unclaim during yesterday's food evaluation cycle.
     * Used to detect recovery (no chunk lost today after losing one yesterday).
     */
    private static Set<UUID> yesterdayCrumblingDominionIds = new HashSet<>();
    /**
     * Dominion IDs that lost a chunk to auto-unclaim during today's food evaluation cycle.
     */
    private static final Set<UUID> todayCrumblingDominionIds = new HashSet<>();

    /**
     * Provides the list of Dominions.
     *
     * @return The list of dominions
     */
    public static List<Dominion> getDominions() {
        return dominions;
    }

    /**
     * Provides the Dominion that the player is in.
     *
     * @param uuid The player's UUID.
     * @return The Dominion that the player is in.
     */
    public static Dominion getPlayerDominion(UUID uuid) {
        return playerToDominion.get(uuid);
    }

    /**
     * Provides the Dominion with the given stable ID.
     *
     * @param id The dominion's stable UUID.
     * @return The Dominion, or null if not found.
     */
    public static Dominion getDominionById(UUID id) {
        return dominionById.get(id);
    }

    /**
     * Generates the chunk lookup key for the given chunk.
     *
     * @param chunk The chunk.
     * @return The lookup key string.
     */
    private static String getChunkKey(Chunk chunk) {
        return chunk.getWorld().getName() + ":" + chunk.getX() + ":" + chunk.getZ();
    }

    /**
     * Checks whether a player has a specific permission within the given dominion.
     * Considers the player's rank if they are a member, or the inter-dominion relation if they are an outsider.
     *
     * @param player     The player to check.
     * @param dominion   The dominion to check permissions in.
     * @param permission The permission to verify.
     * @return True if the player has the permission.
     */
    public static boolean hasPermission(Player player, Dominion dominion, DominionPermission permission) {
        // Player-specific overrides take precedence over all rank/relation permissions
        Boolean playerOverride = dominion.getPlayerPermissionOverride(player.getUniqueId(), permission);
        if (playerOverride != null) {
            return playerOverride;
        }

        Dominion playerDominion = getPlayerDominion(player.getUniqueId());
        if (playerDominion != null && playerDominion.isSameDominion(dominion)) {
            DominionRank rank = dominion.getMemberRank(player.getUniqueId());
            if (rank == null) {
                rank = DominionRank.NEWCOMER;
            }
            return dominion.getDominionPermissions().hasPermission(rank, permission);
        }
        // Conqueror bypass build permission for the leader and lieutenants
        if (permission == DominionPermission.BUILD
                && playerDominion != null
                && playerDominion.getConquered().contains(dominion.getLeader())) {
            DominionRank playerRank = playerDominion.getMemberRank(player.getUniqueId());
            if (playerRank == DominionRank.LEADER || playerRank == DominionRank.LIEUTENANT) {
                return true;
            }
        }
        DominionRank relation = getRelationKey(playerDominion, dominion);
        return dominion.getDominionPermissions().hasPermission(relation, permission);
    }

    /**
     * Returns true if the attacker is permitted to apply harmful PvP effects to the target
     * under the current dominion relation rules. Mirrors the logic in DominionProtectionListener
     * so that abilities bypassing EntityDamageEvent (potion effects, stuns, etc.) are still gated.
     *
     * @param attacker The player using the ability.
     * @param target   The player being targeted.
     * @return Whether the attacker may harm the target.
     */
    public static boolean canAttackPlayer(Player attacker, Player target) {
        Dominion attackerDominion = getPlayerDominion(attacker.getUniqueId());
        Dominion targetDominion = getPlayerDominion(target.getUniqueId());

        // Same dominion - respect the member PvP flag
        if (attackerDominion != null && targetDominion != null
                && attackerDominion.isSameDominion(targetDominion)) {
            return attackerDominion.isMemberPvpEnabled();
        }

        // Both players belong to different dominions
        if (attackerDominion != null && targetDominion != null) {
            DominionRank relation = getRelationKey(attackerDominion, targetDominion);
            Dominion chunkDominion = getDominionOfChunk(target.getLocation().getChunk());

            if (relation == DominionRank.ALLIED || relation == DominionRank.TRUCED) {
                // Per-player overrides take precedence; either side allowing PvP permits the attack
                boolean attackerPvp = hasPermission(target, attackerDominion, DominionPermission.PVP);
                boolean targetPvp = hasPermission(attacker, targetDominion, DominionPermission.PVP);
                return attackerPvp || targetPvp;
            }

            if (relation == DominionRank.NEUTRAL) {
                // Blocked only when the target is in their own dominion's land
                return chunkDominion == null || !chunkDominion.isSameDominion(targetDominion);
            }

            // ENEMIED - always allowed
            return true;
        }

        // Attacker has a dominion, target is a wanderer - always allowed
        if (attackerDominion != null) {
            return true;
        }

        // Attacker is a wanderer, target has a dominion - blocked in target's own land
        if (targetDominion != null) {
            Dominion chunkDominion = getDominionOfChunk(target.getLocation().getChunk());
            if (chunkDominion != null && chunkDominion.isSameDominion(targetDominion)) {
                return false;
            }
        }

        return true;
    }

    /**
     * Determines the relation rank between two dominions for the purposes of permission lookups.
     *
     * @param playerDominion The dominion of the acting player (may be null).
     * @param targetDominion The dominion being acted upon.
     * @return The relation rank: ALLIED, TRUCED, ENEMIED, NEUTRAL, or WANDERER.
     */
    public static DominionRank getRelationKey(Dominion playerDominion, Dominion targetDominion) {
        if (playerDominion == null) {
            return DominionRank.WANDERER;
        }
        if (playerDominion.isAllied(targetDominion)) {
            return DominionRank.ALLIED;
        }
        if (playerDominion.isTruced(targetDominion)) {
            return DominionRank.TRUCED;
        }
        if (playerDominion.isEnemied(targetDominion)) {
            return DominionRank.ENEMIED;
        }
        return DominionRank.NEUTRAL;
    }

    /**
     * Returns true if the target player is positively-aligned with the source.
     */
    public static boolean isPositivelyAligned(Player source, Player target) {
        if (source.equals(target)) {
            return true;
        }
        Dominion sourceDominion = getPlayerDominion(source.getUniqueId());
        Dominion targetDominion = getPlayerDominion(target.getUniqueId());
        if (sourceDominion == null || targetDominion == null) {
            return false;
        }
        if (sourceDominion.isSameDominion(targetDominion)) {
            return true;
        }
        DominionRank relation = getRelationKey(sourceDominion, targetDominion);
        return relation == DominionRank.ALLIED || relation == DominionRank.TRUCED;
    }

    /**
     * Returns true if the target player is negatively-aligned with the source.
     */
    public static boolean isNegativelyAligned(Player source, Player target) {
        if (source.equals(target)) {
            return false;
        }
        Dominion sourceDominion = getPlayerDominion(source.getUniqueId());
        Dominion targetDominion = getPlayerDominion(target.getUniqueId());
        if (sourceDominion == null || targetDominion == null) {
            return false;
        }
        return getRelationKey(sourceDominion, targetDominion) == DominionRank.ENEMIED;
    }

    /**
     * Creates a new dominion.
     *
     * @param dominion The dominion being added.
     */
    public static void createDominion(Dominion dominion) {
        dominions.add(dominion);
        dominionById.put(dominion.getId(), dominion);
        for (UUID memberUuid : dominion.getMembers()) {
            playerToDominion.put(memberUuid, dominion);
        }
        for (Chunk chunk : dominion.getChunks()) {
            chunkKeyToDominion.put(getChunkKey(chunk), dominion);
        }
        refreshBiomeCache(dominion);
    }

    /**
     * Removes a player from the playerToDominion lookup map.
     * Use this when removing a member from a dominion before calling updateDominion,
     * since updateDominion uses the same object reference and can't detect the removal.
     *
     * @param uuid The player's UUID.
     */
    public static void removePlayerFromDominion(UUID uuid) {
        playerToDominion.remove(uuid);
    }

    /**
     * Updates an existing dominion with new values, keeping all lookup maps in sync.
     *
     * @param dominion The updated dominion.
     */
    public static void updateDominion(Dominion dominion) {
        int i = 0;
        while (i < dominions.size()) {
            if (dominions.get(i).isSameDominion(dominion)) {
                break;
            }
            i++;
        }
        if (i < dominions.size()) {
            Dominion old = dominions.get(i);

            // Remove old member entries
            for (UUID memberUuid : old.getMembers()) {
                playerToDominion.remove(memberUuid);
            }
            // Remove old chunk entries
            for (Chunk chunk : old.getChunks()) {
                chunkKeyToDominion.remove(getChunkKey(chunk));
            }

            dominions.set(i, dominion);
        } else {
            dominions.add(dominion);
        }

        // Re-populate maps with updated dominion
        dominionById.put(dominion.getId(), dominion);
        for (UUID memberUuid : dominion.getMembers()) {
            playerToDominion.put(memberUuid, dominion);
        }
        for (Chunk chunk : dominion.getChunks()) {
            chunkKeyToDominion.put(getChunkKey(chunk), dominion);
        }

        // Hot-save to MySQL immediately so changes are not lost between 30-minute periodic saves.
        // Skip during shutdown - saveAll()/saveDominions() handles that path synchronously.
        if (!dominion.getChunks().isEmpty()
                && AranarthCore.getInstance() != null && AranarthCore.getInstance().isEnabled()) {
            PersistenceUtils.saveSingleDominionToDatabase(dominion);
        }
    }

    /**
     * Claims the chunk for the dominion or one of its outposts.
     *
     * @param player       The player attempting to claim the chunk.
     * @param chunkToClaim The chunk the player is attempting to claim.
     * @return A chat-formatted message describing the outcome.
     */
    public static String claimChunk(Player player, Chunk chunkToClaim) {
        // Reject if already claimed
        Dominion dominionOfChunk = getDominionOfChunk(chunkToClaim);
        if (dominionOfChunk != null) {
            Dominion playerDominion = getPlayerDominion(player.getUniqueId());
            if (playerDominion != null && playerDominion.isSameDominion(dominionOfChunk)) {
                return "&cThis chunk is already claimed by your dominion";
            }
            return "&cThis chunk is already claimed by &e" + dominionOfChunk.getName();
        }
        Outpost outpostOfChunk = OutpostUtils.getOutpostOfChunk(chunkToClaim);
        if (outpostOfChunk != null) {
            Dominion playerDominion = getPlayerDominion(player.getUniqueId());
            if (playerDominion != null && outpostOfChunk.getDominionId().equals(playerDominion.getId())) {
                return "&cThis chunk is already claimed by your outpost &e" + outpostOfChunk.getName();
            }
            return "&cThis chunk is already claimed by another dominion's outpost";
        }

        Dominion playerDominion = getPlayerDominion(player.getUniqueId());
        if (playerDominion == null) {
            return "&cYou are not part of a dominion!";
        }
        if (!playerDominion.getLeader().equals(player.getUniqueId())
                && playerDominion.getMemberRank(player.getUniqueId()) != DominionRank.LIEUTENANT) {
            return "&cOnly the Leader or Lieutenant can claim land!";
        }
        if (playerDominion.getConqueredRequest() != null) {
            return "&cYou cannot claim chunks while your Dominion is under active conquest!";
        }

        int claimPrice = 250;

        // Adjacent to main dominion
        if (isConnectedToClaims(playerDominion.getChunks(), chunkToClaim)) {
            if (playerDominion.getBalance() < claimPrice) {
                return "&cYour dominion cannot afford this!";
            }
            if (playerDominion.getChunks().size() >= playerDominion.getMaxChunks()) {
                return "&cYou cannot claim more than &e" + playerDominion.getMaxChunks() + " chunks!";
            }
            playerDominion.setBalance(playerDominion.getBalance() - claimPrice);
            PersistenceUtils.saveSingleDominionToDatabase(playerDominion);
            if (NetworkManager.isActive()) {
                NetworkManager.getInstance().publishDominionBalanceAdjust(playerDominion.getId(), -claimPrice);
            }
            playerDominion.getChunks().add(chunkToClaim);
            chunkKeyToDominion.put(getChunkKey(chunkToClaim), playerDominion);
            resizeFoodArray(playerDominion);
            updateDominion(playerDominion);
            DominionLevelUtils.reevaluateDominion(playerDominion);
            refreshBiomeCache(playerDominion);
            return "&e" + playerDominion.getName() + " &7has claimed &e"
                    + playerDominion.getChunks().size() + "/" + playerDominion.getMaxChunks() + " chunks";
        }

        // Adjacent to an outpost (lowest index first)
        List<Outpost> outposts = OutpostUtils.getDominionOutposts(playerDominion.getId());
        for (Outpost outpost : outposts) {
            if (OutpostUtils.isConnectedToOutpostClaims(outpost.getChunks(), chunkToClaim)) {
                return OutpostUtils.claimOutpostChunk(player, outpost, playerDominion, chunkToClaim);
            }
        }

        return "&cThis chunk is not connected to your Dominion or an outpost";
    }

    /**
     * Provides the dominion that owns the chunk.
     *
     * @param chunk The chunk being verified.
     * @return The dominion that owns the chunk.
     */
    public static Dominion getDominionOfChunk(Chunk chunk) {
        return chunkKeyToDominion.get(getChunkKey(chunk));
    }

    /**
     * Returns the dominion that owns the given chunk, checking both main dominion
     * claims and outpost claims. Returns null if the chunk is unclaimed.
     */
    public static Dominion getDominionOfChunkAnywhere(Chunk chunk) {
        Dominion dominion = getDominionOfChunk(chunk);
        if (dominion != null) {
            return dominion;
        }
        Outpost outpost = OutpostUtils.getOutpostOfChunk(chunk);
        if (outpost != null) {
            return getDominionById(outpost.getDominionId());
        }
        return null;
    }

    /**
     * Unclaims the chunk that the user is standing in.
     * If the chunk belongs to an outpost, delegates to {@link OutpostUtils#unclaimOutpostChunk}.
     *
     * @param player The player that is attempting to unclaim the chunk.
     * @return The message of whether the chunk was unclaimed.
     */
    public static String unclaimChunk(Player player) {
        Chunk chunk = player.getLocation().getChunk();

        Outpost outpostOfChunk = OutpostUtils.getOutpostOfChunk(chunk);
        if (outpostOfChunk != null) {
            Dominion playerDominion = getPlayerDominion(player.getUniqueId());
            if (playerDominion == null) {
                return "&cYou are not part of a Dominion";
            }
            if (!outpostOfChunk.getDominionId().equals(playerDominion.getId())) {
                return "&cThis chunk is claimed by another dominion's outpost!";
            }
            return OutpostUtils.unclaimOutpostChunk(player, outpostOfChunk, playerDominion, chunk);
        }

        // Main dominion chunk
        Dominion dominionOfChunk = getDominionOfChunk(chunk);
        if (dominionOfChunk != null) {
            Dominion playerDominion = getPlayerDominion(player.getUniqueId());
            if (playerDominion != null) {
                if (playerDominion.isSameDominion(dominionOfChunk)) {
                    if (playerDominion.getLeader().equals(player.getUniqueId()) || playerDominion.getMemberRank(player.getUniqueId()) == DominionRank.LIEUTENANT) {
                        if (playerDominion.getConqueredRequest() != null) {
                            return "&cYou cannot unclaim chunks while your Dominion is under active conquest!";
                        }
                        Chunk homeChunk = playerDominion.getDominionHome().getChunk();
                        if (chunk.getX() == homeChunk.getX() && chunk.getZ() == homeChunk.getZ()) {
                            return "&cYou cannot unclaim the chunk that the home is in!";
                        } else {
                            if (isAllClaimsConnectedAfterUnclaiming(dominionOfChunk, chunk)) {
                                AranarthPlayer aranarthPlayer = AranarthUtils.getPlayer(player.getUniqueId());
                                aranarthPlayer.setBalance(aranarthPlayer.getBalance() + 125);
                                AranarthUtils.setPlayer(player.getUniqueId(), aranarthPlayer);
                                PersistenceUtils.saveAranarthPlayerImmediately(player.getUniqueId());
                                if (NetworkManager.isActive()) {
                                    NetworkManager.getInstance().publishBalanceAdjust(player.getUniqueId(), 125);
                                }
                                playerDominion.getChunks().remove(chunk);
                                chunkKeyToDominion.remove(getChunkKey(chunk));
                                resizeFoodArray(playerDominion);
                                updateDominion(playerDominion);
                                DominionLevelUtils.reevaluateDominion(playerDominion);
                                refreshBiomeCache(playerDominion);
                                return "&7This chunk has been unclaimed successfully";
                            } else {
                                return "&cYou cannot unclaim this chunk as they all must remain connected!";
                            }
                        }
                    } else {
                        return "&cOnly the Leader or Lieutenant can unclaim land!";
                    }
                } else {
                    return "&cThis chunk not claimed by " + dominionOfChunk.getName() + "!";
                }
            } else {
                return "&cYou are not part of a Dominion";
            }
        } else {
            return "&cThis chunk is not claimed!";
        }
    }

    /**
     * Disbands the input Dominion.
     *
     * @param dominion The Dominion to be disbanded.
     */
    public static void disbandDominion(Dominion dominion) {
        Bukkit.broadcastMessage(ChatUtils.chatMessage(Lang.get("dominion.disbanded_broadcast", "name", dominion.getName())));
        DiscordUtils.dominionMessage(dominion, "The Dominion of " + dominion.getName() + " &7has been disbanded", Color.RED);
        DefenderUtils.sellAllDominionDefenders(dominion);
        AranarthPlayer aranarthPlayer = AranarthUtils.getPlayer(dominion.getLeader());
        aranarthPlayer.setBalance(aranarthPlayer.getBalance() + dominion.getBalance());
        PersistenceUtils.saveAranarthPlayerImmediately(dominion.getLeader());
        if (NetworkManager.isActive()) {
            NetworkManager.getInstance().publishBalanceAdjust(dominion.getLeader(), dominion.getBalance());
        }
        if (Bukkit.getOfflinePlayer(dominion.getLeader()).isOnline()) {
            Player leader = Bukkit.getPlayer(dominion.getLeader());
            leader.sendMessage(ChatUtils.chatMessage(Lang.get("dominion.balance_returned")));
            ItemStack[] foodReserves = dominion.getFood();
            if (foodReserves != null) {
                for (ItemStack item : foodReserves) {
                    if (item != null && item.getType() != Material.AIR) {
                        leader.getWorld().dropItemNaturally(leader.getLocation(), item);
                    }
                }
            }
        }

        for (Player onlinePlayer : Bukkit.getOnlinePlayers()) {
            onlinePlayer.playSound(onlinePlayer, Sound.ENTITY_WITHER_SPAWN, 0.5F, 1.5F);
        }

        // Clean up outposts
        for (Outpost outpost : new ArrayList<>(OutpostUtils.getDominionOutposts(dominion.getId()))) {
            OutpostUtils.disbandOutpost(dominion, outpost);
        }

        // Clean up lookup maps
        dominionById.remove(dominion.getId());
        for (UUID memberUuid : dominion.getMembers()) {
            playerToDominion.remove(memberUuid);
        }
        chunkKeyToDominion.values().removeIf(d -> d.isSameDominion(dominion));

        dominions.remove(dominion);

        // Delete from database so it does not resurrect on reload
        if (DatabaseManager.isActive()) {
            final UUID dominionId = dominion.getId();
            Bukkit.getScheduler().runTaskAsynchronously(AranarthCore.getInstance(), () -> {
                DatabaseManager db = DatabaseManager.getInstance();
                db.deleteDominion(dominionId);
                db.deleteDominionPermissions(dominionId);
                db.deleteDominionPlayerPerms(dominionId);
                db.deleteDominionPlots(dominionId);
                db.deleteDefendersForDominion(dominionId);
            });
        }

        // Notify the other server so it evicts this dominion from its own memory
        if (NetworkManager.isActive()) {
            NetworkManager.getInstance().publishDominionDisband(dominion.getId());
        }
    }

    /**
     * Removes a dominion from all in-memory structures without touching the database or
     * publishing any network events. Used by the cross-server disband handler so the
     * receiving server can clean up its own memory after the originating server has already
     * handled DB deletion and announcements.
     *
     * @param dominion The Dominion to evict.
     */
    public static void evictDominionFromMemory(Dominion dominion) {
        DefenderUtils.sellAllDominionDefenders(dominion);
        for (Outpost outpost : new ArrayList<>(OutpostUtils.getDominionOutposts(dominion.getId()))) {
            OutpostUtils.evictOutpostFromMemory(outpost.getId());
        }
        dominionById.remove(dominion.getId());
        for (UUID memberUuid : dominion.getMembers()) {
            playerToDominion.remove(memberUuid);
        }
        chunkKeyToDominion.values().removeIf(d -> d.isSameDominion(dominion));
        dominions.remove(dominion);
    }

    /**
     * Determines if the input chunk is connected to the rest of the claims of the dominion.
     *
     * @param dominionChunks The chunks of the Dominion.
     * @param chunk          The chunk.
     * @return Confirmation if the input chunk is connected to the rest of the claims of the dominion.
     */
    public static boolean isConnectedToClaims(List<Chunk> dominionChunks, Chunk chunk) {
        int chunkX = chunk.getX();
        int chunkZ = chunk.getZ();
        for (Chunk dominionChunk : dominionChunks) {
            if (!dominionChunk.getWorld().getName().equals(chunk.getWorld().getName())) {
                continue;
            }

            int differenceX = Math.abs(dominionChunk.getX() - chunkX);
            int differenceZ = Math.abs(dominionChunk.getZ() - chunkZ);

            // Adjacent on one axis, same on the other
            if ((differenceX == 1 && differenceZ == 0) || (differenceX == 0 && differenceZ == 1)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Determines if unclaiming the input claim would result in an unconnected claim.
     *
     * @param dominion      The dominion.
     * @param chunkToRemove The chunk attempting to be unclaimed.
     * @return Confirmation if unclaiming the input claim would result in an unconnected claim.
     */
    public static boolean isAllClaimsConnectedAfterUnclaiming(Dominion dominion, Chunk chunkToRemove) {
        // Creates a copy of the chunks to see if the claims will remain connected once the chunk is removed
        List<Chunk> remaining = new ArrayList<>(dominion.getChunks());
        remaining.remove(chunkToRemove);

        // Will always be connected
        if (remaining.size() <= 1) {
            return true;
        }

        Set<Chunk> visited = new HashSet<>();
        Deque<Chunk> queue = new ArrayDeque<>();

        // Start from any remaining chunk
        Chunk start = remaining.get(0);
        queue.add(start);
        visited.add(start);

        while (!queue.isEmpty()) {
            Chunk current = queue.poll();

            for (Chunk other : remaining) {
                if (visited.contains(other)) {
                    continue;
                }

                if (isSideAdjacent(current, other)) {
                    visited.add(other);
                    queue.add(other);
                }
            }
        }

        // If we couldn't reach every chunk, the claims split
        return visited.size() == remaining.size();
    }

    /**
     * Verifies whether the two chunks are side by side.
     *
     * @param a The first chunk.
     * @param b The second chunk.
     * @return Confirmation whether the two chunks are side by side.
     */
    private static boolean isSideAdjacent(Chunk a, Chunk b) {
        if (!a.getWorld().equals(b.getWorld())) {
            return false;
        }

        // Only one of two can differ
        int dx = Math.abs(a.getX() - b.getX());
        int dz = Math.abs(a.getZ() - b.getZ());

        if (dx + dz == 1) {
            return true;
        }
        return false;
    }

    /**
     * Determines if the player is a wanderer - not a member of any Dominion.
     *
     * @param uuid The player's UUID.
     * @return True if the player has no Dominion.
     */
    public static boolean isWanderer(UUID uuid) {
        return getPlayerDominion(uuid) == null;
    }

    /**
     * Updates the leader of the Dominion by updating all references to the Dominion's Leader.
     *
     * @param dominionBeingUpdated The Dominion that is being updated.
     * @param newLeader            The UUID of the new leader of the Dominion.
     * @param isDeleting           If the Dominion is being deleted.
     */
    public static void updateDominionLeader(Dominion dominionBeingUpdated, UUID newLeader, boolean isDeleting) {
        UUID oldLeader = dominionBeingUpdated.getLeader();
        for (Dominion dominion : getDominions()) {
            for (int i = 0; i < dominion.getAllianceRequests().size(); i++) {
                if (dominion.getAllianceRequests().get(i).equals(oldLeader)) {
                    if (!isDeleting) {
                        dominion.getAllianceRequests().set(i, newLeader);
                    } else {
                        dominion.getAllianceRequests().remove(oldLeader);
                    }
                    break;
                }
            }
            for (int i = 0; i < dominion.getTruceRequests().size(); i++) {
                if (dominion.getTruceRequests().get(i).equals(oldLeader)) {
                    if (!isDeleting) {
                        dominion.getTruceRequests().set(i, newLeader);
                    } else {
                        dominion.getTruceRequests().remove(oldLeader);
                    }
                    break;
                }
            }
            for (int i = 0; i < dominion.getNeutralRequests().size(); i++) {
                if (dominion.getNeutralRequests().get(i).equals(oldLeader)) {
                    if (!isDeleting) {
                        dominion.getNeutralRequests().set(i, newLeader);
                    } else {
                        dominion.getNeutralRequests().remove(oldLeader);
                    }
                    break;
                }
            }
            for (int i = 0; i < dominion.getAllied().size(); i++) {
                if (dominion.getAllied().get(i).equals(oldLeader)) {
                    if (!isDeleting) {
                        dominion.getAllied().set(i, newLeader);
                    } else {
                        dominion.getAllied().remove(oldLeader);
                    }
                    break;
                }
            }
            for (int i = 0; i < dominion.getTruced().size(); i++) {
                if (dominion.getTruced().get(i).equals(oldLeader)) {
                    if (!isDeleting) {
                        dominion.getTruced().set(i, newLeader);
                    } else {
                        dominion.getTruced().remove(oldLeader);
                    }
                    break;
                }
            }
            for (int i = 0; i < dominion.getEnemied().size(); i++) {
                if (dominion.getEnemied().get(i).equals(oldLeader)) {
                    if (!isDeleting) {
                        dominion.getEnemied().set(i, newLeader);
                    } else {
                        dominion.getEnemied().remove(oldLeader);
                    }
                    break;
                }
            }
            for (int i = 0; i < dominion.getConquered().size(); i++) {
                if (dominion.getConquered().get(i).equals(oldLeader)) {
                    if (!isDeleting) {
                        dominion.getConquered().set(i, newLeader);
                    } else {
                        dominion.getConquered().remove(oldLeader);
                    }
                    break;
                }
            }
            if (dominion.getConqueredRequest() != null) {
                if (dominion.getConqueredRequest().equals(oldLeader)) {
                    if (!isDeleting) {
                        dominion.setConqueredRequest(newLeader);
                    } else {
                        dominion.setConqueredRequest(null);
                    }
                }
            }
            if (dominion.getRebelRequest() != null) {
                if (dominion.getRebelRequest().equals(oldLeader)) {
                    if (!isDeleting) {
                        dominion.setRebelRequest(newLeader);
                    } else {
                        dominion.setRebelRequest(null);
                    }
                }
            }

            // Skip re-saving the dominion being disbanded - disbandDominion handles DB deletion
            if (!isDeleting || !dominion.isSameDominion(dominionBeingUpdated)) {
                updateDominion(dominion);
            }
        }
        if (!isDeleting) {
            // Demote old leader to Lieutenant and promote new leader
            dominionBeingUpdated.setMemberRank(oldLeader, DominionRank.LIEUTENANT);
            dominionBeingUpdated.setMemberRank(newLeader, DominionRank.LEADER);
            dominionBeingUpdated.setLeader(newLeader);
            updateDominion(dominionBeingUpdated);
        } else {
            disbandDominion(dominionBeingUpdated);
        }
    }

    /**
     * Consumes contents of a Dominion's designated food inventory.
     */
    public static void reEvaluateFoodInventory() {
        String today = AranarthUtils.getYear() + "-" + AranarthUtils.getMonth() + "-" + AranarthUtils.getDay();
        if (today.equals(lastFoodEvalDate)) {
            return;
        }
        lastFoodEvalDate = today;

        // Rotate crumbling sets
        yesterdayCrumblingDominionIds = new HashSet<>(todayCrumblingDominionIds);
        todayCrumblingDominionIds.clear();

        // Close all inventories before evaluating
        for (Player onlinePlayer : Bukkit.getOnlinePlayers()) {
            String foodTitle = ChatUtils.stripColorFormatting(onlinePlayer.getOpenInventory().getTitle());
            if (foodTitle.endsWith(" Food") || foodTitle.matches(".+'s Food \\(\\d+/\\d+\\)")) {
                onlinePlayer.closeInventory();
            }
        }

        for (Dominion dominion : new ArrayList<>(getDominions())) {
            // Each server only consumes food for its own dominions.
            // SMP dominions have world names stored with a "smp:" prefix; Survival dominions do not.
            String homeWorldName = dominion.getDominionHomeWorldName();
            boolean isSmpDominion = homeWorldName != null && homeWorldName.startsWith("smp:");
            if (AranarthCore.isSmpServer() != isSmpDominion) {
                continue;
            }

            // Conquered dominions are exempt from daily food/money/land taxes
            if (getConquerorOfDominion(dominion) != null) {
                continue;
            }

            // If the dominion has more chunks than its member count allows, unclaim one chunk
            // per day with priority over food/money consumption until within the limit
            if (dominion.getChunks().size() > dominion.getMaxChunks()) {
                Chunk chunkToRemove = null;
                for (Chunk chunk : dominion.getChunks()) {
                    if (dominion.getChunks().size() > 1 && dominion.getDominionHome().getChunk().equals(chunk)) {
                        continue;
                    }
                    if (isAllClaimsConnectedAfterUnclaiming(dominion, chunk)) {
                        chunkToRemove = chunk;
                        break;
                    }
                }
                if (chunkToRemove != null) {
                    removePenaltyChunk(dominion, chunkToRemove);
                    if (!todayCrumblingDominionIds.contains(dominion.getId())) {
                        todayCrumblingDominionIds.add(dominion.getId());
                        DiscordUtils.dominionMessage(dominion, "The Dominion of " + dominion.getName() + " is starting to crumble", new Color(180, 70, 0));
                    }
                    for (UUID memberUuid : dominion.getMembers()) {
                        if (Bukkit.getOfflinePlayer(memberUuid).isOnline()) {
                            Player member = Bukkit.getPlayer(memberUuid);
                            member.sendMessage(ChatUtils.chatMessage(Lang.get("dominion.chunk_over_limit", "name", dominion.getName())));
                            member.sendMessage(ChatUtils.chatMessage(Lang.get("dominion.chunk_auto_unclaimed")));
                        }
                    }
                }
                continue;
            }

            int totalFoodPower = getTotalFoodPower(dominion);

            int powerBeingConsumed = DominionLevelUtils.getDailyFoodPower(dominion.getDominionLevel());

            if (totalFoodPower >= powerBeingConsumed) {
                consumeFood(dominion, powerBeingConsumed);
                PersistenceUtils.saveSingleDominionToDatabase(dominion);
                if (Bukkit.getOfflinePlayer(dominion.getLeader()).isOnline()) {
                    Player onlineLeader = Bukkit.getPlayer(dominion.getLeader());
                    onlineLeader.sendMessage(ChatUtils.chatMessage(Lang.get("dominion.food_consumed", "name", dominion.getName())));
                }
                // No chunk lost today but lost one yesterday
                if (yesterdayCrumblingDominionIds.contains(dominion.getId())) {
                    DiscordUtils.dominionMessage(dominion, "The Dominion of " + dominion.getName() + " has recovered and prevented their demise", new Color(50, 180, 50));
                }
            } else {
                double dailyCost = DominionLevelUtils.getDailyBalanceCost(dominion.getDominionLevel());
                int result = consumeMoneyOrLand(dominion, dailyCost);
                if (result == -1) {
                    updateDominionLeader(dominion, null, true);
                } else {
                    // Land was consumed - mark as crumbling
                    if (result == 0 && !todayCrumblingDominionIds.contains(dominion.getId())) {
                        todayCrumblingDominionIds.add(dominion.getId());
                        DiscordUtils.dominionMessage(dominion, "The Dominion of " + dominion.getName() + " is starting to crumble", new Color(180, 70, 0));
                    }
                    // Money was consumed - check recovery
                    if (result == 1 && yesterdayCrumblingDominionIds.contains(dominion.getId())) {
                        DiscordUtils.dominionMessage(dominion, "The Dominion of " + dominion.getName() + " has recovered and prevented their demise", new Color(50, 180, 50));
                    }
                    for (UUID memberUuid : dominion.getMembers()) {
                        if (Bukkit.getOfflinePlayer(memberUuid).isOnline()) {
                            Player member = Bukkit.getPlayer(memberUuid);
                            member.sendMessage(ChatUtils.chatMessage(Lang.get("dominion.food_not_enough", "name", dominion.getName())));
                            // Money was consumed
                            if (result == 1) {
                                member.sendMessage(ChatUtils.chatMessage(Lang.get("dominion.tax_paid_money", "amount", (int) dailyCost)));
                            }
                            // Land was consumed
                            else {
                                member.sendMessage(ChatUtils.chatMessage(Lang.get("dominion.tax_paid_chunk")));
                            }
                        }
                    }
                }
            }
        }
    }

    /**
     * Provides the total food power of the Dominion.
     *
     * @param dominion The Dominion.
     * @return The total food power of the Dominion.
     */
    public static int getTotalFoodPower(Dominion dominion) {
        int totalPower = 0;
        for (ItemStack food : dominion.getFood()) {
            if (food == null || food.getType() == Material.AIR) {
                continue;
            }

            // Determines if there's an increase, or if the Dominion is conquered, a decrease
            int amplifier = dominion.getConquered().size();
            if (amplifier == 0) {
                if (getConquerorOfDominion(dominion) != null) {
                    amplifier = -1;
                }
            }
            int powerOfFoodItem = getClaimFoodPower(food.getType(), amplifier);
            totalPower += (powerOfFoodItem * food.getAmount());
        }
        return totalPower;
    }

    /**
     * Determines the claim power that the input item contains.
     *
     * @param type      The Material of item being provided.
     * @param amplifier The amplifier to be applied to the item, limited to 5.
     * @return The claim power that the input item contains.
     */
    public static int getClaimFoodPower(Material type, int amplifier) {
        int power = 0;
        if (type == Material.ENCHANTED_GOLDEN_APPLE || type == Material.CAKE) {
            power = 500;
        } else if (type == Material.MUSHROOM_STEW || type == Material.BEETROOT_SOUP) {
            power = 150;
        } else if (type == Material.HAY_BLOCK || type == Material.RABBIT_STEW) {
            power = 50;
        } else if (type == Material.COOKED_PORKCHOP || type == Material.COOKED_MUTTON
                || type == Material.COOKED_BEEF || type == Material.COOKED_CHICKEN || type == Material.COOKED_RABBIT) {
            power = 48;
        } else if (type == Material.COOKED_COD || type == Material.COOKED_SALMON) {
            power = 40;
        } else if (type == Material.GOLDEN_APPLE) {
            power = 32;
        } else if (type == Material.PUMPKIN_PIE) {
            power = 25;
        } else if (type == Material.DRIED_KELP_BLOCK) {
            power = 20;
        } else if (type == Material.BREAD || type == Material.APPLE) {
            power = 16;
        } else if (type == Material.GOLDEN_CARROT) {
            power = 10;
        } else if (type == Material.PORKCHOP || type == Material.MUTTON
                || type == Material.BEEF || type == Material.CHICKEN || type == Material.RABBIT) {
            power = 30;
        } else if (type == Material.COD || type == Material.SALMON) {
            power = 25;
        } else if (type == Material.POISONOUS_POTATO) {
            power = 6;
        } else if (type == Material.WHEAT || type == Material.BEETROOT) {
            power = 5;
        } else if (type == Material.BAKED_POTATO) {
            power = 3;
        } else if (type == Material.CARROT || type == Material.POTATO || type == Material.DRIED_KELP) {
            power = 2;
        } else if (type == Material.MELON_SLICE || type == Material.SWEET_BERRIES || type == Material.GLOW_BERRIES
                || type == Material.CHORUS_FRUIT || type == Material.COOKIE) {
            power = 1;
        }

        // Limited to an amplifier of 5
        if (amplifier > 5) {
            amplifier = 5;
        }

        // Provides different yields based on the input
        power = switch (amplifier) {
            case -1 -> (int) (power * 0.75);
            case 1 -> (int) (power * 1.25);
            case 2 -> (int) (power * 1.5);
            case 3 -> (int) (power * 1.75);
            case 4 -> power * 2;
            case 5 -> (int) (power * 2.5);
            default -> power;
        };

        // Apply seasonal multiplier based on food source (crop yield, animal spawn rate, or apple drop rate)
        double seasonalMult = getSeasonalFoodMultiplier(type, AranarthUtils.getMonth());
        return (int) Math.round(power * seasonalMult);
    }

    /**
     * Returns the seasonal multiplier for a food item based on the current month.
     * Crop-based foods scale with their crop's yield multiplier (at half-strength).
     * Meat and fish scale with passive mob spawn rates (at half-strength).
     * Apples and golden apples are buffed during Solarvor when apple drop rates are increased.
     *
     * @param type  The food material.
     * @param month The current month.
     * @return The seasonal multiplier to apply to the food's power.
     */
    private static double getSeasonalFoodMultiplier(Material type, Month month) {
        // Crop-based foods: scale with yield multiplier at half-strength
        Material cropSource = getCropSourceForFood(type);
        if (cropSource != null) {
            double yieldMult = CropUtils.getCropYieldMultiplier(month, cropSource);
            return 1.0 + (yieldMult - 1.0) * 0.5;
        }

        // Meat and fish: scale with passive mob spawn rates at half-strength
        if (isMeatOrFish(type)) {
            double animalMult = getAnimalSpawnMultiplier(month);
            return 1.0 + (animalMult - 1.0) * 0.5;
        }

        // Apples and golden apples: buffed in Solarvor when apple drop rates are 10x
        if (type == Material.APPLE || type == Material.GOLDEN_APPLE || type == Material.ENCHANTED_GOLDEN_APPLE) {
            return month == Month.SOLARVOR ? 1.5 : 1.0;
        }

        return 1.0;
    }

    /**
     * Maps a food item to its primary crop source for seasonal scaling.
     * Returns null if the item has no crop source.
     */
    private static Material getCropSourceForFood(Material type) {
        return switch (type) {
            case WHEAT, BREAD, HAY_BLOCK, CAKE -> Material.WHEAT_SEEDS;
            case CARROT, GOLDEN_CARROT -> Material.CARROT;
            case POTATO, BAKED_POTATO, POISONOUS_POTATO -> Material.POTATO;
            case BEETROOT, BEETROOT_SOUP -> Material.BEETROOT_SEEDS;
            case MELON_SLICE -> Material.MELON_SEEDS;
            case PUMPKIN_PIE -> Material.PUMPKIN_SEEDS;
            case SWEET_BERRIES -> Material.SWEET_BERRIES;
            case COOKIE -> Material.COCOA_BEANS;
            default -> null;
        };
    }

    /**
     * Returns true if the item is a meat or fish product (raw or cooked), or rabbit stew.
     */
    private static boolean isMeatOrFish(Material type) {
        return switch (type) {
            case PORKCHOP, COOKED_PORKCHOP,
                 MUTTON, COOKED_MUTTON,
                 BEEF, COOKED_BEEF,
                 CHICKEN, COOKED_CHICKEN,
                 RABBIT, COOKED_RABBIT, RABBIT_STEW,
                 COD, COOKED_COD,
                 SALMON, COOKED_SALMON -> true;
            default -> false;
        };
    }

    /**
     * Returns the passive animal spawn multiplier for the given month.
     * Mirrors the values used in DateUtils.applyMobSpawnRates().
     */
    private static double getAnimalSpawnMultiplier(Month month) {
        return switch (month) {
            case FAUNIVOR -> 3.00;
            case FOLLIVOR, STRIGAVOR -> 2.00;
            case ARDORVOR, SOLARVOR, CALORVOR -> 1.50;
            case AESTIVOR -> 1.25;
            case AQUINVOR, VENTIVOR, FLORIVOR, UMBRAVOR -> 1.00;
            case IGNIVOR -> 0.60;
            case GLACIVOR -> 0.50;
            case OBSCURVOR -> 0.40;
            case FRIGORVOR -> 0.25;
        };
    }

    /**
     * Returns the food reserve percentage (0.0-1.0) for the given dominion.
     * @param dominion The dominion.
     * @return Food reserve percentage, clamped to [0.0, 1.0].
     */
    public static double getBaseFoodPercentage(Dominion dominion) {
        int totalSlots = getFoodArraySize(dominion);
        if (totalSlots == 0) return 0.0;
        int filledSlots = 0;
        for (ItemStack food : dominion.getFood()) {
            if (food != null && food.getType() != Material.AIR) {
                filledSlots++;
            }
        }
        return Math.min(1.0, filledSlots / (double) totalSlots);
    }

    /**
     * Converts a raw food reserve percentage (0.0-1.0) to a resource claim yield multiplier.
     * @param foodPct Raw food reserve percentage (0.0-1.0).
     * @return Yield multiplier in [0.33, 1.0].
     */
    public static double foodPercentageToYield(double foodPct) {
        if (foodPct >= 1.0) return 1.0;
        if (foodPct <= 0.5) return 0.33;
        // 0.33 at food of 0.5, 1.0 at food of 1.0
        return 0.33 + (foodPct - 0.5) * 1.34;
    }

    /**
     * Returns the correct food array size for a dominion based on its rank level.
     * Level 1 = 18 slots (2 rows), Level 2 = 45 slots (5 rows),
     * Level 3 = 90 slots (2 pages), Level 4 = 135 slots (3 pages), Level 5 = 225 slots (5 pages).
     */
    public static int getFoodArraySize(Dominion dominion) {
        int level = dominion.getDominionLevel();
        if (level <= 1) {
            return 18;
        } else if (level == 2) {
            return 45;
        } else if (level == 3) {
            return 90;
        } else if (level == 4) {
            return 135;
        } else {
            return 225;
        }
    }

    /**
     * Removes a specific chunk from a dominion, updates the internal chunk-key index,
     * resizes the food array, and persists the change.
     * Used by the level penalty system. The caller is responsible for verifying
     * connectivity before calling this method.
     *
     * @param dominion The dominion to remove the chunk from.
     * @param chunk    The chunk to remove.
     */
    public static void removePenaltyChunk(Dominion dominion, Chunk chunk) {
        AranarthUtils.removeLocksInChunk(chunk);
        dominion.getChunks().remove(chunk);
        chunkKeyToDominion.remove(getChunkKey(chunk));
        resizeFoodArray(dominion);
        updateDominion(dominion);
        refreshBiomeCache(dominion);
    }

    /**
     * Resizes the dominion's food array to match the correct size for its current chunk count.
     * Items in slots that fall outside the new size are lost when shrinking.
     */
    public static void resizeFoodArray(Dominion dominion) {
        int targetSize = getFoodArraySize(dominion);
        ItemStack[] current = dominion.getFood();
        if (current.length == targetSize) {
            return;
        }
        ItemStack[] resized = new ItemStack[targetSize];
        System.arraycopy(current, 0, resized, 0, Math.min(current.length, targetSize));
        dominion.setFood(resized);
    }

    /**
     * Locks the food inventory of a dominion to a specific player.
     */
    public static void lockFoodInventory(UUID dominionId, UUID playerUUID) {
        foodInventoryLocks.put(dominionId, playerUUID);
    }

    /**
     * Unlocks the food inventory of a dominion.
     */
    public static void unlockFoodInventory(UUID dominionId) {
        foodInventoryLocks.remove(dominionId);
    }

    /**
     * Returns true if the food inventory is locked by a player other than the given one.
     */
    public static boolean isFoodInventoryLockedByOther(UUID dominionId, UUID playerUUID) {
        UUID holder = foodInventoryLocks.get(dominionId);
        return holder != null && !holder.equals(playerUUID);
    }

    /**
     * Compacts the food array by merging identical items and shifting everything to the front.
     * Respects each item's max stack size.
     */
    public static ItemStack[] compactFoodArray(ItemStack[] food) {
        List<ItemStack> merged = new ArrayList<>();
        for (ItemStack item : food) {
            if (item == null || item.getType() == Material.AIR) {
                continue;
            }
            ItemStack copy = item.clone();
            boolean stacked = false;
            for (ItemStack existing : merged) {
                if (existing.isSimilar(copy) && existing.getAmount() < existing.getMaxStackSize()) {
                    int space = existing.getMaxStackSize() - existing.getAmount();
                    int toAdd = Math.min(space, copy.getAmount());
                    existing.setAmount(existing.getAmount() + toAdd);
                    copy.setAmount(copy.getAmount() - toAdd);
                    if (copy.getAmount() <= 0) {
                        stacked = true;
                        break;
                    }
                }
            }
            if (!stacked) {
                merged.add(copy);
            }
        }
        ItemStack[] result = new ItemStack[food.length];
        for (int i = 0; i < merged.size() && i < result.length; i++) {
            result[i] = merged.get(i);
        }
        return result;
    }

    /**
     * Determines whether the input Material is a food item.
     *
     * @param type The type of item being verified.
     * @return Confirmation whether the input Material is a food item.
     */
    public static boolean isFoodItem(Material type) {
        return getClaimFoodPower(type, 0) > 0;
    }

    /**
     * Consumes food from the Dominion's food reserve.
     *
     * @param dominion           The dominion.
     * @param powerBeingConsumed The total power amount being consumed.
     */
    public static void consumeFood(Dominion dominion, int powerBeingConsumed) {
        // Do not consume more items as the previous item is still being consumed
        if (dominion.getFoodPowerBeingConsumed() >= powerBeingConsumed) {
            dominion.setFoodPowerBeingConsumed(dominion.getFoodPowerBeingConsumed() - powerBeingConsumed);
            updateDominion(dominion);
            return;
        }

        // Set the default to consider what's currently being consumed still
        int combinedPowerOfItems = dominion.getFoodPowerBeingConsumed();

        // Go through the food currently in the reserves
        for (ItemStack food : dominion.getFood()) {
            if (food == null || food.getType() == Material.AIR) {
                continue;
            }

            // Determines if there's an increase, or if the Dominion is conquered, a decrease
            int amplifier = dominion.getConquered().size();
            if (amplifier == 0) {
                if (getConquerorOfDominion(dominion) != null) {
                    amplifier = -1;
                }
            }

            int powerOfItem = getClaimFoodPower(food.getType(), amplifier);
            // Take one quantity at a time of that particular item
            for (int quantity = food.getAmount(); quantity > 0; quantity--) {
                food.setAmount(food.getAmount() - 1);
                combinedPowerOfItems += powerOfItem;

                // If there is no more power needed
                if (combinedPowerOfItems >= powerBeingConsumed) {
                    int remainingPower = combinedPowerOfItems - powerBeingConsumed;
                    dominion.setFoodPowerBeingConsumed(remainingPower);
                    updateDominion(dominion);
                    return;
                }
            }
        }
    }

    /**
     * Consumes money or land the Dominion has when there is not enough food available.
     *
     * @param dominion       The dominion.
     * @param moneyToConsume The amount of money to be consumed.
     * @return 1 if consuming money, 0 if consuming a chunk, -1 if disbanding the Dominion.
     */
    public static int consumeMoneyOrLand(Dominion dominion, double moneyToConsume) {
        if (dominion.getBalance() >= moneyToConsume) {
            dominion.setBalance(dominion.getBalance() - moneyToConsume);
            updateDominion(dominion);
            return 1;
        } else {
            Chunk chunkToRemove = null;
            // Only the balance runs out, start unclaiming the outer chunks, where the last unclaimed chunk will be the home
            for (Chunk chunk : dominion.getChunks()) {
                // Unclaim the dominion home last
                if (dominion.getChunks().size() > 1 && dominion.getDominionHome().getChunk().equals(chunk)) {
                    continue;
                }

                if (isAllClaimsConnectedAfterUnclaiming(dominion, chunk)) {
                    chunkToRemove = chunk;
                    break;
                }
            }

            // If unclaiming the last chunk
            if (dominion.getChunks().size() == 1) {
                return -1;
            } else {
                dominion.getChunks().remove(chunkToRemove);
                chunkKeyToDominion.remove(getChunkKey(chunkToRemove));
                resizeFoodArray(dominion);
                updateDominion(dominion);
                refreshBiomeCache(dominion);
                return 0;
            }
        }
    }

    /**
     * Provides the list of biomes that a Dominion has access to claim from.
     * Uses pre-computed caches populated at startup and on every claim/unclaim.
     *
     * @param dominion The Dominion.
     * @return The list of biomes that a Dominion has access to claim from.
     */
    public static List<Biome> getResourceClaimTypes(Dominion dominion) {
        Set<Biome> biomes = new LinkedHashSet<>(dominion.getCachedBiomes());
        for (Outpost outpost : OutpostUtils.getDominionOutposts(dominion.getId())) {
            biomes.addAll(outpost.getCachedBiomes());
        }
        return new ArrayList<>(biomes);
    }

    /**
     * Recomputes and stores the biome cache for a dominion from its current chunk list.
     * Should be called after any chunk is claimed or unclaimed.
     */
    public static void refreshBiomeCache(Dominion dominion) {
        World world = dominion.getDominionHome().getWorld();
        dominion.setCachedBiomes(sampleBiomesFromChunks(dominion.getChunks(), world));
    }

    /**
     * Recomputes and stores the biome cache for an outpost from its current chunk list.
     * Should be called after any outpost chunk is claimed or unclaimed.
     */
    public static void refreshOutpostBiomeCache(Outpost outpost) {
        World world = outpost.getHome().getWorld();
        outpost.setCachedBiomes(sampleBiomesFromChunks(outpost.getChunks(), world));
    }

    /**
     * Samples biomes from a list of chunks, restricted to the specified world.
     * Chunks belonging to a different world are skipped to prevent cross-world biome contamination -
     * for example, an outpost in the nether should never pick up overworld biomes at the same chunk
     * coordinates, even if a different world happens to have claims there.
     * Overworld chunks are sampled at the surface height of each column so that underground biome
     * zones (e.g. Cherry Grove extending below a mountain at Y=63) are not included. Non-overworld
     * chunks are sampled every 16 blocks across the world's full height range to account for 3D
     * biome placement.
     *
     * @param chunks        The chunks to sample biomes from.
     * @param expectedWorld The world these chunks must belong to; chunks in any other world are ignored.
     *                      If null (e.g. world not loaded on this server), returns an empty set.
     */
    private static Set<Biome> sampleBiomesFromChunks(List<Chunk> chunks, World expectedWorld) {
        Set<Biome> biomes = new LinkedHashSet<>();
        if (expectedWorld == null) {
            return biomes;
        }
        for (Chunk chunk : chunks) {
            World world = chunk.getWorld();
            if (world == null || !world.equals(expectedWorld)) {
                continue;
            }
            Chunk live = world.getChunkAt(chunk.getX(), chunk.getZ());
            if (world.getEnvironment() == World.Environment.NORMAL) {
                sampleBiomesAtSurface(live, biomes);
            } else {
                for (int y = world.getMinHeight(); y < world.getMaxHeight(); y += 16) {
                    sampleBiomesAtY(live, y, biomes);
                }
            }
        }
        return biomes;
    }

    private static void sampleBiomesAtY(Chunk chunk, int y, Set<Biome> biomes) {
        for (int x = 0; x < 16; x++) {
            for (int z = 0; z < 16; z++) {
                Biome biome = chunk.getBlock(x, y, z).getBiome();
                if (biome != Biome.LUSH_CAVES && biome != Biome.DRIPSTONE_CAVES
                        && biome != Biome.DEEP_DARK) {
                    biomes.add(biome);
                }
            }
        }
    }

    /**
     * Samples biomes from an overworld chunk by reading each column at its actual surface height.
     * This prevents underground biome zones (e.g. a mountain biome extending below sea level) from
     * appearing in the set even though no surface terrain of that biome exists in the claim.
     */
    private static void sampleBiomesAtSurface(Chunk chunk, Set<Biome> biomes) {
        World world = chunk.getWorld();
        int baseX = chunk.getX() << 4;
        int baseZ = chunk.getZ() << 4;
        for (int x = 0; x < 16; x++) {
            for (int z = 0; z < 16; z++) {
                int surfaceY = world.getHighestBlockYAt(baseX + x, baseZ + z);
                Biome biome = chunk.getBlock(x, surfaceY, z).getBiome();
                if (biome != Biome.LUSH_CAVES && biome != Biome.DRIPSTONE_CAVES
                        && biome != Biome.DEEP_DARK) {
                    biomes.add(biome);
                }
            }
        }
    }

    /**
     * Provides the name of the biome.
     *
     * @param biome The biome.
     * @return The name of the biome.
     */
    public static String getBiomeName(Biome biome) {
        String[] parts = biome.toString().split("/");
        String nameWithExtra = parts[2].substring(11);
        String name = nameWithExtra.split("]")[0];
        String formatted = ChatUtils.getFormattedItemName(name);
        if (biome == Biome.THE_END) {
            formatted = "The" + formatted.substring(3);
        }
        return formatted;
    }

    // - Resource drop data classes for biome tables -

    public static class ResourceDrop {
        public final ItemStack item;
        public final int maxAmount;
        public final int subChanceOdds;
        public final String lore;
        public final boolean previewOnly;

        public ResourceDrop(ItemStack item) {
            this(item, 0, 0, null, false);
        }

        public ResourceDrop(ItemStack item, String lore) {
            this(item, 0, 0, lore, false);
        }

        public ResourceDrop(ItemStack item, int maxAmount, int subChanceOdds, String lore) {
            this(item, maxAmount, subChanceOdds, lore, false);
        }

        public ResourceDrop(ItemStack item, int maxAmount, int subChanceOdds, String lore, boolean previewOnly) {
            this.item = item;
            this.maxAmount = maxAmount;
            this.subChanceOdds = subChanceOdds;
            this.lore = lore;
            this.previewOnly = previewOnly;
        }
    }

    public static class SimulationGroup {
        public final String name;
        public final int odds;
        public final List<ResourceDrop> drops;

        public SimulationGroup(String name, int odds, List<ResourceDrop> drops) {
            this.name = name;
            this.odds = odds;
            this.drops = drops;
        }
    }

    public static class VarietyGroup {
        public final List<ResourceDrop> options;

        public VarietyGroup(List<ResourceDrop> options) {
            this.options = options;
        }
    }

    public static class BiomeTable {
        public final List<ResourceDrop> guaranteed;
        public final List<SimulationGroup> simulations;
        public final List<ResourceDrop> rares;
        public final List<VarietyGroup> varieties;

        public BiomeTable(List<ResourceDrop> guaranteed, List<SimulationGroup> simulations,
                          List<ResourceDrop> rares, List<VarietyGroup> varieties) {
            this.guaranteed = guaranteed;
            this.simulations = simulations;
            this.rares = rares;
            this.varieties = varieties;
        }
    }

    public static String formatPct(int odds) {
        double pct = 100.0 / odds;
        if (pct == Math.floor(pct)) {
            return (int) pct + "%";
        }
        String formatted = String.format("%.2f", pct);
        formatted = formatted.replaceAll("\\.?0+$", "");
        return formatted + "%";
    }

    private static ItemStack resolveAmount(ResourceDrop drop, Random random) {
        int amount = drop.maxAmount > 0
                ? random.nextInt(drop.maxAmount - drop.item.getAmount() + 1) + drop.item.getAmount()
                : drop.item.getAmount();
        ItemStack result = drop.item.clone();
        result.setAmount(amount);
        return result;
    }

    public static BiomeTable buildBiomeTable(Dominion dominion, Biome biome) {
        int rank = dominion.getDominionLevel();

        int[] commonOdds = {10, 8, 5, 3, 1};
        int[] rareOdds = {20, 15, 10, 5, 2};
        int[] elytraOdds = {200, 100, 65, 50, 40};
        int[] mendingOdds = {16, 12, 8, 6, 4};
        int[] godAppleOdds = {16, 12, 8, 4, 1};

        List<ResourceDrop> guaranteed = new ArrayList<>();
        List<SimulationGroup> simulations = new ArrayList<>();
        List<ResourceDrop> rares = new ArrayList<>();
        List<VarietyGroup> varieties = new ArrayList<>();

        // Shared simulation group builders
        List<ResourceDrop> fortressDrops = new ArrayList<>();
        fortressDrops.add(new ResourceDrop(new ItemStack(Material.NETHER_BRICKS, 64)));
        fortressDrops.add(new ResourceDrop(new ItemStack(Material.NETHER_BRICKS, 64)));
        fortressDrops.add(new ResourceDrop(new ItemStack(Material.NETHER_WART, 32)));
        fortressDrops.add(new ResourceDrop(new ItemStack(Material.BLAZE_ROD, 2), 6, 0, null));
        fortressDrops.add(new ResourceDrop(new ItemStack(Material.WITHER_SKELETON_SKULL, 1), 0, 15,
                "Wither Skeleton Skull (" + formatPct(15) + " if simulating Nether Fortress)"));
        SimulationGroup netherFortress = new SimulationGroup("Nether Fortress", 8, fortressDrops);

        List<ResourceDrop> bastionDrops = new ArrayList<>();
        bastionDrops.add(new ResourceDrop(new ItemStack(Material.BLACKSTONE, 64)));
        bastionDrops.add(new ResourceDrop(new ItemStack(Material.BLACKSTONE, 64)));
        bastionDrops.add(new ResourceDrop(new ItemStack(Material.POLISHED_BLACKSTONE_BRICKS, 64)));
        bastionDrops.add(new ResourceDrop(new ItemStack(Material.POLISHED_BLACKSTONE_BRICKS, 64)));
        bastionDrops.add(new ResourceDrop(new ItemStack(Material.GILDED_BLACKSTONE, 32)));
        bastionDrops.add(new ResourceDrop(new ItemStack(Material.GOLDEN_CARROT, 32)));
        bastionDrops.add(new ResourceDrop(new ItemStack(Material.NETHERITE_UPGRADE_SMITHING_TEMPLATE, 1), 0, 3,
                "Smithing Template (" + formatPct(3) + " if simulating Bastion Remnant)"));
        SimulationGroup bastionRemnant = new SimulationGroup("Bastion Remnant", 12, bastionDrops);

        List<ResourceDrop> seaTempleDrops = new ArrayList<>();
        seaTempleDrops.add(new ResourceDrop(new ItemStack(Material.PRISMARINE, 64)));
        seaTempleDrops.add(new ResourceDrop(new ItemStack(Material.PRISMARINE_BRICKS, 64)));
        seaTempleDrops.add(new ResourceDrop(new ItemStack(Material.DARK_PRISMARINE, 16)));
        seaTempleDrops.add(new ResourceDrop(new ItemStack(Material.SEA_LANTERN, 16)));
        SimulationGroup seaTemple = new SimulationGroup("Sea Temple", 8, seaTempleDrops);

        ItemStack mendingBook = new ItemStack(Material.ENCHANTED_BOOK, 1);
        EnchantmentStorageMeta mendingMeta = (EnchantmentStorageMeta) mendingBook.getItemMeta();
        mendingMeta.addStoredEnchant(Enchantment.MENDING, 1, true);
        mendingBook.setItemMeta(mendingMeta);
        List<ResourceDrop> endCityDrops = new ArrayList<>();
        endCityDrops.add(new ResourceDrop(new ItemStack(Material.END_STONE_BRICKS, 64)));
        endCityDrops.add(new ResourceDrop(new ItemStack(Material.END_STONE_BRICKS, 64)));
        endCityDrops.add(new ResourceDrop(new ItemStack(Material.PURPUR_BLOCK, 64)));
        endCityDrops.add(new ResourceDrop(new ItemStack(Material.PURPUR_PILLAR, 64)));
        endCityDrops.add(new ResourceDrop(new ItemStack(Material.END_ROD, 16)));
        endCityDrops.add(new ResourceDrop(new ItemStack(Material.ELYTRA, 1), 0, elytraOdds[rank - 1],
                "Elytra (" + formatPct(elytraOdds[rank - 1]) + " if simulating End City)"));
        endCityDrops.add(new ResourceDrop(mendingBook, 0, mendingOdds[rank - 1],
                "Mending Book (" + formatPct(mendingOdds[rank - 1]) + " if simulating End City)"));
        SimulationGroup endCity = new SimulationGroup("End City", 5, endCityDrops);

        // Oceans, Rivers, Beaches, Islands
        if (biome == Biome.OCEAN) {
            guaranteed.add(new ResourceDrop(new ItemStack(Material.GRAVEL, 64)));
            guaranteed.add(new ResourceDrop(new ItemStack(Material.SAND, 32)));
            guaranteed.add(new ResourceDrop(new ItemStack(Material.STONE, 32)));
            guaranteed.add(new ResourceDrop(new ItemStack(Material.KELP, 32)));
            guaranteed.add(new ResourceDrop(new ItemStack(Material.COAL_ORE, 4)));
            guaranteed.add(new ResourceDrop(new ItemStack(Material.LAPIS_ORE, 4)));
            guaranteed.add(new ResourceDrop(new ItemStack(Material.INK_SAC, 8)));
            guaranteed.add(new ResourceDrop(new ItemStack(Material.COD, 16)));
            rares.add(new ResourceDrop(new ItemStack(Material.NAUTILUS_SHELL, 1), 0, commonOdds[rank - 1],
                    "Rare Drop (" + formatPct(commonOdds[rank - 1]) + " chance)"));
        } else if (biome == Biome.RIVER) {
            guaranteed.add(new ResourceDrop(new ItemStack(Material.GRASS_BLOCK, 32)));
            guaranteed.add(new ResourceDrop(new ItemStack(Material.SAND, 32)));
            guaranteed.add(new ResourceDrop(new ItemStack(Material.GRAVEL, 32)));
            guaranteed.add(new ResourceDrop(new ItemStack(Material.CLAY, 16)));
            guaranteed.add(new ResourceDrop(new ItemStack(Material.STONE, 32)));
            guaranteed.add(new ResourceDrop(new ItemStack(Material.OAK_LOG, 4)));
            guaranteed.add(new ResourceDrop(new ItemStack(Material.SUGAR_CANE, 8)));
            guaranteed.add(new ResourceDrop(new ItemStack(Material.COAL_ORE, 4)));
            guaranteed.add(new ResourceDrop(new ItemStack(Material.IRON_ORE, 2)));
            guaranteed.add(new ResourceDrop(new ItemStack(Material.INK_SAC, 8)));
            guaranteed.add(new ResourceDrop(new ItemStack(Material.SALMON, 16)));
        } else if (biome == Biome.FROZEN_OCEAN) {
            guaranteed.add(new ResourceDrop(new ItemStack(Material.GRAVEL, 64)));
            guaranteed.add(new ResourceDrop(new ItemStack(Material.SAND, 32)));
            guaranteed.add(new ResourceDrop(new ItemStack(Material.DIRT, 32)));
            guaranteed.add(new ResourceDrop(new ItemStack(Material.STONE, 32)));
            guaranteed.add(new ResourceDrop(new ItemStack(Material.ICE, 64)));
            guaranteed.add(new ResourceDrop(new ItemStack(Material.PACKED_ICE, 64)));
            guaranteed.add(new ResourceDrop(new ItemStack(Material.BLUE_ICE, 8)));
            guaranteed.add(new ResourceDrop(new ItemStack(Material.COAL_ORE, 4)));
            guaranteed.add(new ResourceDrop(new ItemStack(Material.IRON_ORE, 2)));
            guaranteed.add(new ResourceDrop(new ItemStack(Material.SALMON, 8)));
            guaranteed.add(new ResourceDrop(new ItemStack(Material.INK_SAC, 4)));
            rares.add(new ResourceDrop(new ItemStack(Material.NAUTILUS_SHELL, 1), 0, commonOdds[rank - 1],
                    "Rare Drop (" + formatPct(commonOdds[rank - 1]) + " chance)"));
        } else if (biome == Biome.FROZEN_RIVER) {
            guaranteed.add(new ResourceDrop(new ItemStack(Material.GRAVEL, 32)));
            guaranteed.add(new ResourceDrop(new ItemStack(Material.DIRT, 32)));
            guaranteed.add(new ResourceDrop(new ItemStack(Material.SAND, 32)));
            guaranteed.add(new ResourceDrop(new ItemStack(Material.CLAY, 8)));
            guaranteed.add(new ResourceDrop(new ItemStack(Material.STONE, 32)));
            guaranteed.add(new ResourceDrop(new ItemStack(Material.ICE, 32)));
            guaranteed.add(new ResourceDrop(new ItemStack(Material.SUGAR_CANE, 8)));
            guaranteed.add(new ResourceDrop(new ItemStack(Material.COAL_ORE, 4)));
            guaranteed.add(new ResourceDrop(new ItemStack(Material.IRON_ORE, 2)));
            guaranteed.add(new ResourceDrop(new ItemStack(Material.INK_SAC, 4)));
            guaranteed.add(new ResourceDrop(new ItemStack(Material.SALMON, 16)));
        } else if (biome == Biome.BEACH) {
            guaranteed.add(new ResourceDrop(new ItemStack(Material.SAND, 32)));
            guaranteed.add(new ResourceDrop(new ItemStack(Material.STONE, 32)));
            guaranteed.add(new ResourceDrop(new ItemStack(Material.SUGAR_CANE, 8)));
            guaranteed.add(new ResourceDrop(new ItemStack(Material.COD, 16)));
            rares.add(new ResourceDrop(new ItemStack(Material.TURTLE_SCUTE, 1), 0, rareOdds[rank - 1],
                    "Rare Drop (" + formatPct(rareOdds[rank - 1]) + " chance)"));
            rares.add(new ResourceDrop(new ItemStack(Material.HEART_OF_THE_SEA, 1), 0, 25,
                    "Rare Drop (" + formatPct(25) + " chance)"));
        } else if (biome == Biome.DEEP_OCEAN) {
            guaranteed.add(new ResourceDrop(new ItemStack(Material.GRAVEL, 64)));
            guaranteed.add(new ResourceDrop(new ItemStack(Material.GRAVEL, 64)));
            guaranteed.add(new ResourceDrop(new ItemStack(Material.SAND, 32)));
            guaranteed.add(new ResourceDrop(new ItemStack(Material.STONE, 32)));
            guaranteed.add(new ResourceDrop(new ItemStack(Material.KELP, 32)));
            guaranteed.add(new ResourceDrop(new ItemStack(Material.COAL_ORE, 4)));
            guaranteed.add(new ResourceDrop(new ItemStack(Material.LAPIS_ORE, 4)));
            guaranteed.add(new ResourceDrop(new ItemStack(Material.INK_SAC, 8)));
            guaranteed.add(new ResourceDrop(new ItemStack(Material.COD, 16)));
            rares.add(new ResourceDrop(new ItemStack(Material.NAUTILUS_SHELL, 1), 0, commonOdds[rank - 1],
                    "Rare Drop (" + formatPct(commonOdds[rank - 1]) + " chance)"));
            simulations.add(seaTemple);
        } else if (biome == Biome.STONY_SHORE) {
            guaranteed.add(new ResourceDrop(new ItemStack(Material.STONE, 64)));
            guaranteed.add(new ResourceDrop(new ItemStack(Material.GRAVEL, 64)));
            guaranteed.add(new ResourceDrop(new ItemStack(Material.COAL_ORE, 8)));
            guaranteed.add(new ResourceDrop(new ItemStack(Material.IRON_ORE, 2)));
            guaranteed.add(new ResourceDrop(new ItemStack(Material.COPPER_ORE, 2)));
            guaranteed.add(new ResourceDrop(new ItemStack(Material.COD, 8)));
        } else if (biome == Biome.SNOWY_BEACH) {
            guaranteed.add(new ResourceDrop(new ItemStack(Material.SAND, 32)));
            guaranteed.add(new ResourceDrop(new ItemStack(Material.SNOW, 32)));
            guaranteed.add(new ResourceDrop(new ItemStack(Material.COAL_ORE, 8)));
            guaranteed.add(new ResourceDrop(new ItemStack(Material.IRON_ORE, 2)));
            guaranteed.add(new ResourceDrop(new ItemStack(Material.SALMON, 8)));
        } else if (biome == Biome.WARM_OCEAN) {
            guaranteed.add(new ResourceDrop(new ItemStack(Material.SAND, 64)));
            guaranteed.add(new ResourceDrop(new ItemStack(Material.STONE, 32)));
            guaranteed.add(new ResourceDrop(new ItemStack(Material.SEA_PICKLE, 8)));
            guaranteed.add(new ResourceDrop(new ItemStack(Material.COAL_ORE, 4)));
            guaranteed.add(new ResourceDrop(new ItemStack(Material.LAPIS_ORE, 4)));
            guaranteed.add(new ResourceDrop(new ItemStack(Material.INK_SAC, 8)));
            guaranteed.add(new ResourceDrop(new ItemStack(Material.TROPICAL_FISH, 16)));
            guaranteed.add(new ResourceDrop(new ItemStack(Material.PUFFERFISH, 2)));
            // Coral - previewOnly variety items, actual selection done in getResourcesByDominionAndBiome
            String coralNote = "1 of 5 coral types per claim";
            guaranteed.add(new ResourceDrop(new ItemStack(Material.TUBE_CORAL_BLOCK, 8), 0, 0, coralNote, true));
            guaranteed.add(new ResourceDrop(new ItemStack(Material.TUBE_CORAL, 4), 0, 0, coralNote, true));
            guaranteed.add(new ResourceDrop(new ItemStack(Material.TUBE_CORAL_FAN, 4), 0, 0, coralNote, true));
            guaranteed.add(new ResourceDrop(new ItemStack(Material.BRAIN_CORAL_BLOCK, 8), 0, 0, coralNote, true));
            guaranteed.add(new ResourceDrop(new ItemStack(Material.BRAIN_CORAL, 4), 0, 0, coralNote, true));
            guaranteed.add(new ResourceDrop(new ItemStack(Material.BRAIN_CORAL_FAN, 4), 0, 0, coralNote, true));
            guaranteed.add(new ResourceDrop(new ItemStack(Material.BUBBLE_CORAL_BLOCK, 8), 0, 0, coralNote, true));
            guaranteed.add(new ResourceDrop(new ItemStack(Material.BUBBLE_CORAL, 4), 0, 0, coralNote, true));
            guaranteed.add(new ResourceDrop(new ItemStack(Material.BUBBLE_CORAL_FAN, 4), 0, 0, coralNote, true));
            guaranteed.add(new ResourceDrop(new ItemStack(Material.FIRE_CORAL_BLOCK, 8), 0, 0, coralNote, true));
            guaranteed.add(new ResourceDrop(new ItemStack(Material.FIRE_CORAL, 4), 0, 0, coralNote, true));
            guaranteed.add(new ResourceDrop(new ItemStack(Material.FIRE_CORAL_FAN, 4), 0, 0, coralNote, true));
            guaranteed.add(new ResourceDrop(new ItemStack(Material.HORN_CORAL_BLOCK, 8), 0, 0, coralNote, true));
            guaranteed.add(new ResourceDrop(new ItemStack(Material.HORN_CORAL, 4), 0, 0, coralNote, true));
            guaranteed.add(new ResourceDrop(new ItemStack(Material.HORN_CORAL_FAN, 4), 0, 0, coralNote, true));
            rares.add(new ResourceDrop(new ItemStack(Material.NAUTILUS_SHELL, 1), 0, commonOdds[rank - 1],
                    "Rare Drop (" + formatPct(commonOdds[rank - 1]) + " chance)"));
        } else if (biome == Biome.LUKEWARM_OCEAN) {
            guaranteed.add(new ResourceDrop(new ItemStack(Material.SAND, 64)));
            guaranteed.add(new ResourceDrop(new ItemStack(Material.STONE, 32)));
            guaranteed.add(new ResourceDrop(new ItemStack(Material.KELP, 32)));
            guaranteed.add(new ResourceDrop(new ItemStack(Material.COAL_ORE, 4)));
            guaranteed.add(new ResourceDrop(new ItemStack(Material.LAPIS_ORE, 4)));
            guaranteed.add(new ResourceDrop(new ItemStack(Material.INK_SAC, 8)));
            guaranteed.add(new ResourceDrop(new ItemStack(Material.TROPICAL_FISH, 4)));
            guaranteed.add(new ResourceDrop(new ItemStack(Material.PUFFERFISH, 2)));
            guaranteed.add(new ResourceDrop(new ItemStack(Material.COD, 8)));
            rares.add(new ResourceDrop(new ItemStack(Material.NAUTILUS_SHELL, 1), 0, commonOdds[rank - 1],
                    "Rare Drop (" + formatPct(commonOdds[rank - 1]) + " chance)"));
        } else if (biome == Biome.COLD_OCEAN) {
            guaranteed.add(new ResourceDrop(new ItemStack(Material.GRAVEL, 64)));
            guaranteed.add(new ResourceDrop(new ItemStack(Material.STONE, 32)));
            guaranteed.add(new ResourceDrop(new ItemStack(Material.KELP, 32)));
            guaranteed.add(new ResourceDrop(new ItemStack(Material.COAL_ORE, 4)));
            guaranteed.add(new ResourceDrop(new ItemStack(Material.COD, 8)));
            guaranteed.add(new ResourceDrop(new ItemStack(Material.SALMON, 8)));
            guaranteed.add(new ResourceDrop(new ItemStack(Material.INK_SAC, 8)));
            rares.add(new ResourceDrop(new ItemStack(Material.NAUTILUS_SHELL, 1), 0, commonOdds[rank - 1],
                    "Rare Drop (" + formatPct(commonOdds[rank - 1]) + " chance)"));
        } else if (biome == Biome.DEEP_LUKEWARM_OCEAN) {
            guaranteed.add(new ResourceDrop(new ItemStack(Material.SAND, 64)));
            guaranteed.add(new ResourceDrop(new ItemStack(Material.STONE, 32)));
            guaranteed.add(new ResourceDrop(new ItemStack(Material.KELP, 32)));
            guaranteed.add(new ResourceDrop(new ItemStack(Material.COAL_ORE, 4)));
            guaranteed.add(new ResourceDrop(new ItemStack(Material.LAPIS_ORE, 4)));
            guaranteed.add(new ResourceDrop(new ItemStack(Material.INK_SAC, 8)));
            guaranteed.add(new ResourceDrop(new ItemStack(Material.COD, 4)));
            guaranteed.add(new ResourceDrop(new ItemStack(Material.TROPICAL_FISH, 16)));
            rares.add(new ResourceDrop(new ItemStack(Material.NAUTILUS_SHELL, 1), 0, commonOdds[rank - 1],
                    "Rare Drop (" + formatPct(commonOdds[rank - 1]) + " chance)"));
            simulations.add(seaTemple);
        } else if (biome == Biome.DEEP_COLD_OCEAN) {
            guaranteed.add(new ResourceDrop(new ItemStack(Material.GRAVEL, 64)));
            guaranteed.add(new ResourceDrop(new ItemStack(Material.SAND, 32)));
            guaranteed.add(new ResourceDrop(new ItemStack(Material.STONE, 32)));
            guaranteed.add(new ResourceDrop(new ItemStack(Material.KELP, 32)));
            guaranteed.add(new ResourceDrop(new ItemStack(Material.COAL_ORE, 4)));
            guaranteed.add(new ResourceDrop(new ItemStack(Material.LAPIS_ORE, 4)));
            guaranteed.add(new ResourceDrop(new ItemStack(Material.INK_SAC, 8)));
            guaranteed.add(new ResourceDrop(new ItemStack(Material.COD, 16)));
            rares.add(new ResourceDrop(new ItemStack(Material.NAUTILUS_SHELL, 1), 0, commonOdds[rank - 1],
                    "Rare Drop (" + formatPct(commonOdds[rank - 1]) + " chance)"));
            simulations.add(seaTemple);
        } else if (biome == Biome.DEEP_FROZEN_OCEAN) {
            guaranteed.add(new ResourceDrop(new ItemStack(Material.GRAVEL, 64)));
            guaranteed.add(new ResourceDrop(new ItemStack(Material.STONE, 32)));
            guaranteed.add(new ResourceDrop(new ItemStack(Material.KELP, 32)));
            guaranteed.add(new ResourceDrop(new ItemStack(Material.COAL_ORE, 4)));
            guaranteed.add(new ResourceDrop(new ItemStack(Material.LAPIS_ORE, 4)));
            guaranteed.add(new ResourceDrop(new ItemStack(Material.INK_SAC, 8)));
            guaranteed.add(new ResourceDrop(new ItemStack(Material.SALMON, 16)));
            rares.add(new ResourceDrop(new ItemStack(Material.NAUTILUS_SHELL, 1), 0, commonOdds[rank - 1],
                    "Rare Drop (" + formatPct(commonOdds[rank - 1]) + " chance)"));
            simulations.add(seaTemple);
        } else if (biome == Biome.MUSHROOM_FIELDS) {
            guaranteed.add(new ResourceDrop(new ItemStack(Material.MYCELIUM, 64)));
            guaranteed.add(new ResourceDrop(new ItemStack(Material.STONE, 64)));
            guaranteed.add(new ResourceDrop(new ItemStack(Material.RED_MUSHROOM_BLOCK, 32)));
            guaranteed.add(new ResourceDrop(new ItemStack(Material.BROWN_MUSHROOM_BLOCK, 32)));
            guaranteed.add(new ResourceDrop(new ItemStack(Material.MUSHROOM_STEM, 32)));
            guaranteed.add(new ResourceDrop(new ItemStack(Material.COAL_ORE, 4)));
            guaranteed.add(new ResourceDrop(new ItemStack(Material.IRON_ORE, 2)));
            rares.add(new ResourceDrop(new ItemStack(Material.DIAMOND, 1), 0, commonOdds[rank - 1],
                    "Rare Drop (" + formatPct(commonOdds[rank - 1]) + " chance)"));

        // Flat Biomes
        } else if (biome == Biome.PLAINS) {
            guaranteed.add(new ResourceDrop(new ItemStack(Material.GRASS_BLOCK, 64)));
            guaranteed.add(new ResourceDrop(new ItemStack(Material.STONE, 64)));
            guaranteed.add(new ResourceDrop(new ItemStack(Material.OAK_LOG, 8)));
            guaranteed.add(new ResourceDrop(new ItemStack(Material.COAL_ORE, 4)));
            guaranteed.add(new ResourceDrop(new ItemStack(Material.IRON_ORE, 2)));
            guaranteed.add(new ResourceDrop(new ItemStack(Material.DANDELION, 8)));
            guaranteed.add(new ResourceDrop(new ItemStack(Material.BEEF, 16)));
            String flowerNote3 = "1 of 3 possible per claim";
            guaranteed.add(new ResourceDrop(new ItemStack(Material.POPPY, 8), 0, 0, flowerNote3, true));
            guaranteed.add(new ResourceDrop(new ItemStack(Material.OXEYE_DAISY, 8), 0, 0, flowerNote3, true));
            guaranteed.add(new ResourceDrop(new ItemStack(Material.CORNFLOWER, 8), 0, 0, flowerNote3, true));
        } else if (biome == Biome.SUNFLOWER_PLAINS) {
            guaranteed.add(new ResourceDrop(new ItemStack(Material.GRASS_BLOCK, 64)));
            guaranteed.add(new ResourceDrop(new ItemStack(Material.STONE, 64)));
            guaranteed.add(new ResourceDrop(new ItemStack(Material.OAK_LOG, 8)));
            guaranteed.add(new ResourceDrop(new ItemStack(Material.COAL_ORE, 4)));
            guaranteed.add(new ResourceDrop(new ItemStack(Material.IRON_ORE, 2)));
            guaranteed.add(new ResourceDrop(new ItemStack(Material.DANDELION, 8)));
            guaranteed.add(new ResourceDrop(new ItemStack(Material.SUNFLOWER, 16)));
            guaranteed.add(new ResourceDrop(new ItemStack(Material.BEEF, 16)));
            String flowerNote3s = "1 of 3 possible per claim";
            guaranteed.add(new ResourceDrop(new ItemStack(Material.POPPY, 8), 0, 0, flowerNote3s, true));
            guaranteed.add(new ResourceDrop(new ItemStack(Material.OXEYE_DAISY, 8), 0, 0, flowerNote3s, true));
            guaranteed.add(new ResourceDrop(new ItemStack(Material.CORNFLOWER, 8), 0, 0, flowerNote3s, true));
        } else if (biome == Biome.SPARSE_JUNGLE) {
            guaranteed.add(new ResourceDrop(new ItemStack(Material.GRASS_BLOCK, 64)));
            guaranteed.add(new ResourceDrop(new ItemStack(Material.STONE, 64)));
            guaranteed.add(new ResourceDrop(new ItemStack(Material.JUNGLE_LOG, 8)));
            guaranteed.add(new ResourceDrop(new ItemStack(Material.OAK_LOG, 2)));
            guaranteed.add(new ResourceDrop(new ItemStack(Material.VINE, 4)));
            guaranteed.add(new ResourceDrop(new ItemStack(Material.MELON, 2)));
            guaranteed.add(new ResourceDrop(new ItemStack(Material.PUMPKIN, 2)));
            guaranteed.add(new ResourceDrop(new ItemStack(Material.COCOA_BEANS, 2)));
            guaranteed.add(new ResourceDrop(new ItemStack(Material.COAL_ORE, 4)));
            guaranteed.add(new ResourceDrop(new ItemStack(Material.IRON_ORE, 2)));
            guaranteed.add(new ResourceDrop(new ItemStack(Material.CHICKEN, 16)));
        } else if (biome == Biome.SNOWY_PLAINS) {
            guaranteed.add(new ResourceDrop(new ItemStack(Material.GRASS_BLOCK, 64)));
            guaranteed.add(new ResourceDrop(new ItemStack(Material.GRASS_BLOCK, 64)));
            guaranteed.add(new ResourceDrop(new ItemStack(Material.SNOW, 64)));
            guaranteed.add(new ResourceDrop(new ItemStack(Material.STONE, 64)));
            guaranteed.add(new ResourceDrop(new ItemStack(Material.SPRUCE_LOG, 8)));
            guaranteed.add(new ResourceDrop(new ItemStack(Material.COAL_ORE, 4)));
            guaranteed.add(new ResourceDrop(new ItemStack(Material.IRON_ORE, 2)));
            guaranteed.add(new ResourceDrop(new ItemStack(Material.RABBIT, 16)));
        } else if (biome == Biome.ICE_SPIKES) {
            guaranteed.add(new ResourceDrop(new ItemStack(Material.SNOW_BLOCK, 64)));
            guaranteed.add(new ResourceDrop(new ItemStack(Material.PACKED_ICE, 64)));
            guaranteed.add(new ResourceDrop(new ItemStack(Material.ICE, 32)));
            guaranteed.add(new ResourceDrop(new ItemStack(Material.STONE, 64)));
            guaranteed.add(new ResourceDrop(new ItemStack(Material.COAL_ORE, 4)));
            guaranteed.add(new ResourceDrop(new ItemStack(Material.IRON_ORE, 2)));
            guaranteed.add(new ResourceDrop(new ItemStack(Material.RABBIT, 16)));

        // Forests
        } else if (biome == Biome.FOREST) {
            guaranteed.add(new ResourceDrop(new ItemStack(Material.GRASS_BLOCK, 64)));
            guaranteed.add(new ResourceDrop(new ItemStack(Material.STONE, 64)));
            guaranteed.add(new ResourceDrop(new ItemStack(Material.OAK_LOG, 16)));
            guaranteed.add(new ResourceDrop(new ItemStack(Material.BIRCH_LOG, 16)));
            guaranteed.add(new ResourceDrop(new ItemStack(Material.LEAF_LITTER, 16)));
            guaranteed.add(new ResourceDrop(new ItemStack(Material.COAL_ORE, 4)));
            guaranteed.add(new ResourceDrop(new ItemStack(Material.IRON_ORE, 2)));
            guaranteed.add(new ResourceDrop(new ItemStack(Material.APPLE, 4)));
            guaranteed.add(new ResourceDrop(new ItemStack(Material.RABBIT, 16)));
            int godOddsF = godAppleOdds[rank - 1];
            if (AranarthUtils.getMonth() == Month.SOLARVOR) {
                godOddsF = Math.max(1, godOddsF / 4);
            }
            rares.add(new ResourceDrop(new GodAppleFragment().getItem(), 0, godOddsF,
                    "Rare Drop (" + formatPct(godOddsF) + " chance)"));
        } else if (biome == Biome.TAIGA) {
            guaranteed.add(new ResourceDrop(new ItemStack(Material.GRASS_BLOCK, 64)));
            guaranteed.add(new ResourceDrop(new ItemStack(Material.STONE, 64)));
            guaranteed.add(new ResourceDrop(new ItemStack(Material.SPRUCE_LOG, 32)));
            guaranteed.add(new ResourceDrop(new ItemStack(Material.SWEET_BERRIES, 16)));
            guaranteed.add(new ResourceDrop(new ItemStack(Material.COAL_ORE, 4)));
            guaranteed.add(new ResourceDrop(new ItemStack(Material.IRON_ORE, 2)));
            guaranteed.add(new ResourceDrop(new ItemStack(Material.RABBIT, 16)));
        } else if (biome == Biome.SWAMP) {
            guaranteed.add(new ResourceDrop(new ItemStack(Material.GRASS_BLOCK, 64)));
            guaranteed.add(new ResourceDrop(new ItemStack(Material.STONE, 64)));
            guaranteed.add(new ResourceDrop(new ItemStack(Material.OAK_LOG, 32)));
            guaranteed.add(new ResourceDrop(new ItemStack(Material.CLAY, 32)));
            guaranteed.add(new ResourceDrop(new ItemStack(Material.FIREFLY_BUSH, 8)));
            guaranteed.add(new ResourceDrop(new ItemStack(Material.BROWN_MUSHROOM, 8)));
            guaranteed.add(new ResourceDrop(new ItemStack(Material.RED_MUSHROOM, 8)));
            guaranteed.add(new ResourceDrop(new ItemStack(Material.LILY_PAD, 16)));
            guaranteed.add(new ResourceDrop(new ItemStack(Material.COAL_ORE, 4)));
            guaranteed.add(new ResourceDrop(new ItemStack(Material.IRON_ORE, 2)));
            guaranteed.add(new ResourceDrop(new ItemStack(Material.SLIME_BALL, 2)));
        } else if (biome == Biome.MANGROVE_SWAMP) {
            guaranteed.add(new ResourceDrop(new ItemStack(Material.GRASS_BLOCK, 16)));
            guaranteed.add(new ResourceDrop(new ItemStack(Material.MUD, 64)));
            guaranteed.add(new ResourceDrop(new ItemStack(Material.MUD, 64)));
            guaranteed.add(new ResourceDrop(new ItemStack(Material.STONE, 64)));
            guaranteed.add(new ResourceDrop(new ItemStack(Material.MANGROVE_LOG, 64)));
            guaranteed.add(new ResourceDrop(new ItemStack(Material.MANGROVE_ROOTS, 32)));
            guaranteed.add(new ResourceDrop(new ItemStack(Material.MUDDY_MANGROVE_ROOTS, 32)));
            guaranteed.add(new ResourceDrop(new ItemStack(Material.MOSS_CARPET, 32)));
            guaranteed.add(new ResourceDrop(new ItemStack(Material.LILY_PAD, 8)));
            guaranteed.add(new ResourceDrop(new ItemStack(Material.COAL_ORE, 4)));
            guaranteed.add(new ResourceDrop(new ItemStack(Material.IRON_ORE, 2)));
        } else if (biome == Biome.JUNGLE) {
            guaranteed.add(new ResourceDrop(new ItemStack(Material.GRASS_BLOCK, 64)));
            guaranteed.add(new ResourceDrop(new ItemStack(Material.STONE, 64)));
            guaranteed.add(new ResourceDrop(new ItemStack(Material.JUNGLE_LOG, 32)));
            guaranteed.add(new ResourceDrop(new ItemStack(Material.OAK_LOG, 4)));
            guaranteed.add(new ResourceDrop(new ItemStack(Material.BAMBOO, 4)));
            guaranteed.add(new ResourceDrop(new ItemStack(Material.VINE, 16)));
            guaranteed.add(new ResourceDrop(new ItemStack(Material.MELON, 2)));
            guaranteed.add(new ResourceDrop(new ItemStack(Material.COCOA_BEANS, 4)));
            guaranteed.add(new ResourceDrop(new ItemStack(Material.COAL_ORE, 4)));
            guaranteed.add(new ResourceDrop(new ItemStack(Material.IRON_ORE, 2)));
            guaranteed.add(new ResourceDrop(new ItemStack(Material.CHICKEN, 16)));
        } else if (biome == Biome.BAMBOO_JUNGLE) {
            guaranteed.add(new ResourceDrop(new ItemStack(Material.GRASS_BLOCK, 16)));
            guaranteed.add(new ResourceDrop(new ItemStack(Material.PODZOL, 64)));
            guaranteed.add(new ResourceDrop(new ItemStack(Material.STONE, 64)));
            guaranteed.add(new ResourceDrop(new ItemStack(Material.JUNGLE_LOG, 4)));
            guaranteed.add(new ResourceDrop(new ItemStack(Material.OAK_LOG, 8)));
            guaranteed.add(new ResourceDrop(new ItemStack(Material.BAMBOO, 64)));
            guaranteed.add(new ResourceDrop(new ItemStack(Material.MELON, 2)));
            guaranteed.add(new ResourceDrop(new ItemStack(Material.COAL_ORE, 4)));
            guaranteed.add(new ResourceDrop(new ItemStack(Material.IRON_ORE, 2)));
            guaranteed.add(new ResourceDrop(new ItemStack(Material.CHICKEN, 16)));
        } else if (biome == Biome.BIRCH_FOREST || biome == Biome.OLD_GROWTH_BIRCH_FOREST) {
            guaranteed.add(new ResourceDrop(new ItemStack(Material.GRASS_BLOCK, 64)));
            guaranteed.add(new ResourceDrop(new ItemStack(Material.STONE, 64)));
            guaranteed.add(new ResourceDrop(new ItemStack(Material.BIRCH_LOG, 32)));
            guaranteed.add(new ResourceDrop(new ItemStack(Material.WILDFLOWERS, 32)));
            guaranteed.add(new ResourceDrop(new ItemStack(Material.COAL_ORE, 4)));
            guaranteed.add(new ResourceDrop(new ItemStack(Material.IRON_ORE, 2)));
            guaranteed.add(new ResourceDrop(new ItemStack(Material.RABBIT, 16)));
        } else if (biome == Biome.DARK_FOREST) {
            guaranteed.add(new ResourceDrop(new ItemStack(Material.GRASS_BLOCK, 64)));
            guaranteed.add(new ResourceDrop(new ItemStack(Material.STONE, 64)));
            guaranteed.add(new ResourceDrop(new ItemStack(Material.DARK_OAK_LOG, 64)));
            guaranteed.add(new ResourceDrop(new ItemStack(Material.OAK_LOG, 16)));
            guaranteed.add(new ResourceDrop(new ItemStack(Material.BROWN_MUSHROOM_BLOCK, 16)));
            guaranteed.add(new ResourceDrop(new ItemStack(Material.RED_MUSHROOM_BLOCK, 16)));
            guaranteed.add(new ResourceDrop(new ItemStack(Material.MUSHROOM_STEM, 16)));
            guaranteed.add(new ResourceDrop(new ItemStack(Material.LEAF_LITTER, 16)));
            guaranteed.add(new ResourceDrop(new ItemStack(Material.COAL_ORE, 4)));
            guaranteed.add(new ResourceDrop(new ItemStack(Material.IRON_ORE, 2)));
            guaranteed.add(new ResourceDrop(new ItemStack(Material.IRON_ORE, 2)));
            guaranteed.add(new ResourceDrop(new ItemStack(Material.APPLE, 4)));
            int godOddsD = godAppleOdds[rank - 1];
            if (AranarthUtils.getMonth() == Month.SOLARVOR) {
                godOddsD = Math.max(1, godOddsD / 4);
            }
            rares.add(new ResourceDrop(new GodAppleFragment().getItem(), 0, godOddsD,
                    "Rare Drop (" + formatPct(godOddsD) + " chance)"));
        } else if (biome == Biome.PALE_GARDEN) {
            guaranteed.add(new ResourceDrop(new ItemStack(Material.GRASS_BLOCK, 64)));
            guaranteed.add(new ResourceDrop(new ItemStack(Material.STONE, 64)));
            guaranteed.add(new ResourceDrop(new ItemStack(Material.PALE_OAK_LOG, 64)));
            guaranteed.add(new ResourceDrop(new ItemStack(Material.PALE_MOSS_BLOCK, 16)));
            guaranteed.add(new ResourceDrop(new ItemStack(Material.PALE_HANGING_MOSS, 8)));
            guaranteed.add(new ResourceDrop(new ItemStack(Material.COAL_ORE, 4)));
            guaranteed.add(new ResourceDrop(new ItemStack(Material.IRON_ORE, 2)));
            guaranteed.add(new ResourceDrop(new ItemStack(Material.RESIN_CLUMP, 8)));
        } else if (biome == Biome.SNOWY_TAIGA) {
            guaranteed.add(new ResourceDrop(new ItemStack(Material.GRASS_BLOCK, 64)));
            guaranteed.add(new ResourceDrop(new ItemStack(Material.STONE, 64)));
            guaranteed.add(new ResourceDrop(new ItemStack(Material.SPRUCE_LOG, 32)));
            guaranteed.add(new ResourceDrop(new ItemStack(Material.SNOW, 32)));
            guaranteed.add(new ResourceDrop(new ItemStack(Material.COAL_ORE, 4)));
            guaranteed.add(new ResourceDrop(new ItemStack(Material.IRON_ORE, 2)));
            guaranteed.add(new ResourceDrop(new ItemStack(Material.RABBIT, 16)));
        } else if (biome == Biome.OLD_GROWTH_PINE_TAIGA || biome == Biome.OLD_GROWTH_SPRUCE_TAIGA) {
            guaranteed.add(new ResourceDrop(new ItemStack(Material.GRASS_BLOCK, 32)));
            guaranteed.add(new ResourceDrop(new ItemStack(Material.PODZOL, 32)));
            guaranteed.add(new ResourceDrop(new ItemStack(Material.COARSE_DIRT, 32)));
            guaranteed.add(new ResourceDrop(new ItemStack(Material.STONE, 64)));
            guaranteed.add(new ResourceDrop(new ItemStack(Material.MOSSY_COBBLESTONE, 16)));
            guaranteed.add(new ResourceDrop(new ItemStack(Material.SPRUCE_LOG, 64)));
            guaranteed.add(new ResourceDrop(new ItemStack(Material.COAL_ORE, 4)));
            guaranteed.add(new ResourceDrop(new ItemStack(Material.IRON_ORE, 2)));
            guaranteed.add(new ResourceDrop(new ItemStack(Material.BROWN_MUSHROOM, 16)));
            guaranteed.add(new ResourceDrop(new ItemStack(Material.RABBIT, 16)));
        } else if (biome == Biome.FLOWER_FOREST) {
            guaranteed.add(new ResourceDrop(new ItemStack(Material.GRASS_BLOCK, 64)));
            guaranteed.add(new ResourceDrop(new ItemStack(Material.STONE, 64)));
            guaranteed.add(new ResourceDrop(new ItemStack(Material.OAK_LOG, 16)));
            guaranteed.add(new ResourceDrop(new ItemStack(Material.BIRCH_LOG, 16)));
            guaranteed.add(new ResourceDrop(new ItemStack(Material.LEAF_LITTER, 16)));
            guaranteed.add(new ResourceDrop(new ItemStack(Material.APPLE, 4)));
            guaranteed.add(new ResourceDrop(new ItemStack(Material.RABBIT, 16)));
            guaranteed.add(new ResourceDrop(new ItemStack(Material.COAL_ORE, 4)));
            guaranteed.add(new ResourceDrop(new ItemStack(Material.IRON_ORE, 2)));
            String flowerNote14 = "1 of 14 possible per claim";
            guaranteed.add(new ResourceDrop(new ItemStack(Material.DANDELION, 8), 0, 0, flowerNote14, true));
            guaranteed.add(new ResourceDrop(new ItemStack(Material.POPPY, 8), 0, 0, flowerNote14, true));
            guaranteed.add(new ResourceDrop(new ItemStack(Material.WHITE_TULIP, 8), 0, 0, flowerNote14, true));
            guaranteed.add(new ResourceDrop(new ItemStack(Material.PINK_TULIP, 8), 0, 0, flowerNote14, true));
            guaranteed.add(new ResourceDrop(new ItemStack(Material.ORANGE_TULIP, 8), 0, 0, flowerNote14, true));
            guaranteed.add(new ResourceDrop(new ItemStack(Material.RED_TULIP, 8), 0, 0, flowerNote14, true));
            guaranteed.add(new ResourceDrop(new ItemStack(Material.ALLIUM, 8), 0, 0, flowerNote14, true));
            guaranteed.add(new ResourceDrop(new ItemStack(Material.AZURE_BLUET, 8), 0, 0, flowerNote14, true));
            guaranteed.add(new ResourceDrop(new ItemStack(Material.OXEYE_DAISY, 8), 0, 0, flowerNote14, true));
            guaranteed.add(new ResourceDrop(new ItemStack(Material.LILY_OF_THE_VALLEY, 8), 0, 0, flowerNote14, true));
            guaranteed.add(new ResourceDrop(new ItemStack(Material.CORNFLOWER, 8), 0, 0, flowerNote14, true));
            guaranteed.add(new ResourceDrop(new ItemStack(Material.LILAC, 4), 0, 0, flowerNote14, true));
            guaranteed.add(new ResourceDrop(new ItemStack(Material.PEONY, 4), 0, 0, flowerNote14, true));
            guaranteed.add(new ResourceDrop(new ItemStack(Material.ROSE_BUSH, 4), 0, 0, flowerNote14, true));
            int godOddsFF = godAppleOdds[rank - 1];
            if (AranarthUtils.getMonth() == Month.SOLARVOR) {
                godOddsFF = Math.max(1, godOddsFF / 4);
            }
            rares.add(new ResourceDrop(new GodAppleFragment().getItem(), 0, godOddsFF,
                    "Rare Drop (" + formatPct(godOddsFF) + " chance)"));

        // Mountains and Large Hills
        } else if (biome == Biome.WINDSWEPT_HILLS) {
            guaranteed.add(new ResourceDrop(new ItemStack(Material.GRASS_BLOCK, 32)));
            guaranteed.add(new ResourceDrop(new ItemStack(Material.STONE, 64)));
            guaranteed.add(new ResourceDrop(new ItemStack(Material.STONE, 64)));
            guaranteed.add(new ResourceDrop(new ItemStack(Material.GRANITE, 16)));
            guaranteed.add(new ResourceDrop(new ItemStack(Material.DIORITE, 16)));
            guaranteed.add(new ResourceDrop(new ItemStack(Material.ANDESITE, 16)));
            guaranteed.add(new ResourceDrop(new ItemStack(Material.SPRUCE_LOG, 4)));
            guaranteed.add(new ResourceDrop(new ItemStack(Material.OAK_LOG, 4)));
            guaranteed.add(new ResourceDrop(new ItemStack(Material.COAL_ORE, 8)));
            guaranteed.add(new ResourceDrop(new ItemStack(Material.COPPER_ORE, 8)));
            guaranteed.add(new ResourceDrop(new ItemStack(Material.IRON_ORE, 8)));
            guaranteed.add(new ResourceDrop(new ItemStack(Material.GOLD_ORE, 4)));
            guaranteed.add(new ResourceDrop(new ItemStack(Material.EMERALD_ORE, 2)));
            guaranteed.add(new ResourceDrop(new ItemStack(Material.MUTTON, 16)));
        } else if (biome == Biome.WINDSWEPT_FOREST) {
            guaranteed.add(new ResourceDrop(new ItemStack(Material.GRASS_BLOCK, 32)));
            guaranteed.add(new ResourceDrop(new ItemStack(Material.STONE, 64)));
            guaranteed.add(new ResourceDrop(new ItemStack(Material.STONE, 64)));
            guaranteed.add(new ResourceDrop(new ItemStack(Material.GRANITE, 16)));
            guaranteed.add(new ResourceDrop(new ItemStack(Material.DIORITE, 16)));
            guaranteed.add(new ResourceDrop(new ItemStack(Material.ANDESITE, 16)));
            guaranteed.add(new ResourceDrop(new ItemStack(Material.SPRUCE_LOG, 16)));
            guaranteed.add(new ResourceDrop(new ItemStack(Material.OAK_LOG, 4)));
            guaranteed.add(new ResourceDrop(new ItemStack(Material.COAL_ORE, 8)));
            guaranteed.add(new ResourceDrop(new ItemStack(Material.COPPER_ORE, 8)));
            guaranteed.add(new ResourceDrop(new ItemStack(Material.IRON_ORE, 8)));
            guaranteed.add(new ResourceDrop(new ItemStack(Material.GOLD_ORE, 4)));
            guaranteed.add(new ResourceDrop(new ItemStack(Material.EMERALD_ORE, 2)));
            guaranteed.add(new ResourceDrop(new ItemStack(Material.MUTTON, 16)));
        } else if (biome == Biome.WINDSWEPT_GRAVELLY_HILLS) {
            guaranteed.add(new ResourceDrop(new ItemStack(Material.GRASS_BLOCK, 32)));
            guaranteed.add(new ResourceDrop(new ItemStack(Material.GRAVEL, 32)));
            guaranteed.add(new ResourceDrop(new ItemStack(Material.STONE, 64)));
            guaranteed.add(new ResourceDrop(new ItemStack(Material.STONE, 64)));
            guaranteed.add(new ResourceDrop(new ItemStack(Material.SPRUCE_LOG, 8)));
            guaranteed.add(new ResourceDrop(new ItemStack(Material.COAL_ORE, 8)));
            guaranteed.add(new ResourceDrop(new ItemStack(Material.COPPER_ORE, 8)));
            guaranteed.add(new ResourceDrop(new ItemStack(Material.IRON_ORE, 8)));
            guaranteed.add(new ResourceDrop(new ItemStack(Material.GOLD_ORE, 4)));
            guaranteed.add(new ResourceDrop(new ItemStack(Material.EMERALD_ORE, 2)));
            guaranteed.add(new ResourceDrop(new ItemStack(Material.MUTTON, 16)));
        } else if (biome == Biome.WINDSWEPT_SAVANNA) {
            guaranteed.add(new ResourceDrop(new ItemStack(Material.GRASS_BLOCK, 32)));
            guaranteed.add(new ResourceDrop(new ItemStack(Material.COARSE_DIRT, 16)));
            guaranteed.add(new ResourceDrop(new ItemStack(Material.STONE, 64)));
            guaranteed.add(new ResourceDrop(new ItemStack(Material.STONE, 64)));
            guaranteed.add(new ResourceDrop(new ItemStack(Material.ACACIA_LOG, 16)));
            guaranteed.add(new ResourceDrop(new ItemStack(Material.COAL_ORE, 8)));
            guaranteed.add(new ResourceDrop(new ItemStack(Material.COPPER_ORE, 8)));
            guaranteed.add(new ResourceDrop(new ItemStack(Material.IRON_ORE, 8)));
            guaranteed.add(new ResourceDrop(new ItemStack(Material.GOLD_ORE, 4)));
            guaranteed.add(new ResourceDrop(new ItemStack(Material.EMERALD_ORE, 2)));
            guaranteed.add(new ResourceDrop(new ItemStack(Material.BEEF, 16)));
        } else if (biome == Biome.GROVE) {
            guaranteed.add(new ResourceDrop(new ItemStack(Material.SNOW_BLOCK, 64)));
            guaranteed.add(new ResourceDrop(new ItemStack(Material.STONE, 64)));
            guaranteed.add(new ResourceDrop(new ItemStack(Material.STONE, 64)));
            guaranteed.add(new ResourceDrop(new ItemStack(Material.SPRUCE_LOG, 32)));
            guaranteed.add(new ResourceDrop(new ItemStack(Material.COAL_ORE, 8)));
            guaranteed.add(new ResourceDrop(new ItemStack(Material.COPPER_ORE, 8)));
            guaranteed.add(new ResourceDrop(new ItemStack(Material.IRON_ORE, 8)));
            guaranteed.add(new ResourceDrop(new ItemStack(Material.GOLD_ORE, 4)));
            guaranteed.add(new ResourceDrop(new ItemStack(Material.EMERALD_ORE, 2)));
            guaranteed.add(new ResourceDrop(new ItemStack(Material.RABBIT, 16)));
        } else if (biome == Biome.FROZEN_PEAKS) {
            guaranteed.add(new ResourceDrop(new ItemStack(Material.SNOW_BLOCK, 64)));
            guaranteed.add(new ResourceDrop(new ItemStack(Material.PACKED_ICE, 64)));
            guaranteed.add(new ResourceDrop(new ItemStack(Material.STONE, 64)));
            guaranteed.add(new ResourceDrop(new ItemStack(Material.COAL_ORE, 8)));
            guaranteed.add(new ResourceDrop(new ItemStack(Material.COPPER_ORE, 8)));
            guaranteed.add(new ResourceDrop(new ItemStack(Material.IRON_ORE, 8)));
            guaranteed.add(new ResourceDrop(new ItemStack(Material.GOLD_ORE, 4)));
            guaranteed.add(new ResourceDrop(new ItemStack(Material.EMERALD_ORE, 2)));
            guaranteed.add(new ResourceDrop(new ItemStack(Material.MUTTON, 16)));
        } else if (biome == Biome.MEADOW) {
            guaranteed.add(new ResourceDrop(new ItemStack(Material.GRASS_BLOCK, 64)));
            guaranteed.add(new ResourceDrop(new ItemStack(Material.STONE, 64)));
            guaranteed.add(new ResourceDrop(new ItemStack(Material.DANDELION, 8)));
            guaranteed.add(new ResourceDrop(new ItemStack(Material.CORNFLOWER, 8)));
            guaranteed.add(new ResourceDrop(new ItemStack(Material.WILDFLOWERS, 32)));
            guaranteed.add(new ResourceDrop(new ItemStack(Material.COAL_ORE, 8)));
            guaranteed.add(new ResourceDrop(new ItemStack(Material.COPPER_ORE, 8)));
            guaranteed.add(new ResourceDrop(new ItemStack(Material.IRON_ORE, 8)));
            guaranteed.add(new ResourceDrop(new ItemStack(Material.GOLD_ORE, 4)));
            guaranteed.add(new ResourceDrop(new ItemStack(Material.EMERALD_ORE, 2)));
            guaranteed.add(new ResourceDrop(new ItemStack(Material.MUTTON, 16)));
        } else if (biome == Biome.JAGGED_PEAKS || biome == Biome.SNOWY_SLOPES) {
            guaranteed.add(new ResourceDrop(new ItemStack(Material.SNOW_BLOCK, 64)));
            guaranteed.add(new ResourceDrop(new ItemStack(Material.STONE, 64)));
            guaranteed.add(new ResourceDrop(new ItemStack(Material.STONE, 64)));
            guaranteed.add(new ResourceDrop(new ItemStack(Material.COAL_ORE, 8)));
            guaranteed.add(new ResourceDrop(new ItemStack(Material.COPPER_ORE, 8)));
            guaranteed.add(new ResourceDrop(new ItemStack(Material.IRON_ORE, 8)));
            guaranteed.add(new ResourceDrop(new ItemStack(Material.GOLD_ORE, 4)));
            guaranteed.add(new ResourceDrop(new ItemStack(Material.EMERALD_ORE, 2)));
            guaranteed.add(new ResourceDrop(new ItemStack(Material.MUTTON, 16)));
        } else if (biome == Biome.STONY_PEAKS) {
            guaranteed.add(new ResourceDrop(new ItemStack(Material.STONE, 64)));
            guaranteed.add(new ResourceDrop(new ItemStack(Material.STONE, 64)));
            guaranteed.add(new ResourceDrop(new ItemStack(Material.CALCITE, 32)));
            guaranteed.add(new ResourceDrop(new ItemStack(Material.COAL_ORE, 8)));
            guaranteed.add(new ResourceDrop(new ItemStack(Material.COPPER_ORE, 8)));
            guaranteed.add(new ResourceDrop(new ItemStack(Material.IRON_ORE, 8)));
            guaranteed.add(new ResourceDrop(new ItemStack(Material.GOLD_ORE, 4)));
            guaranteed.add(new ResourceDrop(new ItemStack(Material.EMERALD_ORE, 2)));
            guaranteed.add(new ResourceDrop(new ItemStack(Material.MUTTON, 16)));
        } else if (biome == Biome.CHERRY_GROVE) {
            guaranteed.add(new ResourceDrop(new ItemStack(Material.GRASS_BLOCK, 64)));
            guaranteed.add(new ResourceDrop(new ItemStack(Material.STONE, 64)));
            guaranteed.add(new ResourceDrop(new ItemStack(Material.CHERRY_LOG, 32)));
            guaranteed.add(new ResourceDrop(new ItemStack(Material.PINK_PETALS, 32)));
            guaranteed.add(new ResourceDrop(new ItemStack(Material.COAL_ORE, 8)));
            guaranteed.add(new ResourceDrop(new ItemStack(Material.COPPER_ORE, 8)));
            guaranteed.add(new ResourceDrop(new ItemStack(Material.IRON_ORE, 8)));
            guaranteed.add(new ResourceDrop(new ItemStack(Material.GOLD_ORE, 4)));
            guaranteed.add(new ResourceDrop(new ItemStack(Material.EMERALD_ORE, 2)));
            guaranteed.add(new ResourceDrop(new ItemStack(Material.RABBIT, 16)));

        // Dry and Desert Biomes
        } else if (biome == Biome.DESERT) {
            guaranteed.add(new ResourceDrop(new ItemStack(Material.SAND, 64)));
            guaranteed.add(new ResourceDrop(new ItemStack(Material.SAND, 64)));
            guaranteed.add(new ResourceDrop(new ItemStack(Material.SANDSTONE, 64)));
            guaranteed.add(new ResourceDrop(new ItemStack(Material.STONE, 64)));
            guaranteed.add(new ResourceDrop(new ItemStack(Material.CACTUS, 8)));
            guaranteed.add(new ResourceDrop(new ItemStack(Material.CACTUS_FLOWER, 4)));
            guaranteed.add(new ResourceDrop(new ItemStack(Material.COAL_ORE, 4)));
            guaranteed.add(new ResourceDrop(new ItemStack(Material.GOLD_ORE, 2)));
            guaranteed.add(new ResourceDrop(new ItemStack(Material.RABBIT, 16)));
            rares.add(new ResourceDrop(new ItemStack(Material.BONE_BLOCK, 32), 0, rareOdds[rank - 1],
                    "Rare Drop (" + formatPct(rareOdds[rank - 1]) + " chance)"));
        } else if (biome == Biome.SAVANNA || biome == Biome.SAVANNA_PLATEAU) {
            guaranteed.add(new ResourceDrop(new ItemStack(Material.GRASS_BLOCK, 64)));
            guaranteed.add(new ResourceDrop(new ItemStack(Material.STONE, 64)));
            guaranteed.add(new ResourceDrop(new ItemStack(Material.ACACIA_LOG, 32)));
            guaranteed.add(new ResourceDrop(new ItemStack(Material.COAL_ORE, 4)));
            guaranteed.add(new ResourceDrop(new ItemStack(Material.GOLD_ORE, 2)));
            guaranteed.add(new ResourceDrop(new ItemStack(Material.BEEF, 16)));
            rares.add(new ResourceDrop(new ItemStack(Material.ARMADILLO_SCUTE, 1), 0, commonOdds[rank - 1],
                    "Rare Drop (" + formatPct(commonOdds[rank - 1]) + " chance)"));
        } else if (biome == Biome.BADLANDS || biome == Biome.ERODED_BADLANDS) {
            guaranteed.add(new ResourceDrop(new ItemStack(Material.RED_SAND, 32)));
            guaranteed.add(new ResourceDrop(new ItemStack(Material.STONE, 32)));
            guaranteed.add(new ResourceDrop(new ItemStack(Material.RED_SANDSTONE, 32)));
            guaranteed.add(new ResourceDrop(new ItemStack(Material.TERRACOTTA, 64)));
            guaranteed.add(new ResourceDrop(new ItemStack(Material.COAL_ORE, 4)));
            guaranteed.add(new ResourceDrop(new ItemStack(Material.GOLD_ORE, 4)));
            String terracottaNote = "1 of 6 possible per claim";
            guaranteed.add(new ResourceDrop(new ItemStack(Material.RED_TERRACOTTA, 16), 0, 0, terracottaNote, true));
            guaranteed.add(new ResourceDrop(new ItemStack(Material.ORANGE_TERRACOTTA, 16), 0, 0, terracottaNote, true));
            guaranteed.add(new ResourceDrop(new ItemStack(Material.YELLOW_TERRACOTTA, 16), 0, 0, terracottaNote, true));
            guaranteed.add(new ResourceDrop(new ItemStack(Material.BROWN_TERRACOTTA, 16), 0, 0, terracottaNote, true));
            guaranteed.add(new ResourceDrop(new ItemStack(Material.LIGHT_GRAY_TERRACOTTA, 16), 0, 0, terracottaNote, true));
            guaranteed.add(new ResourceDrop(new ItemStack(Material.WHITE_TERRACOTTA, 16), 0, 0, terracottaNote, true));
            rares.add(new ResourceDrop(new ItemStack(Material.ARMADILLO_SCUTE, 1), 0, commonOdds[rank - 1],
                    "Rare Drop (" + formatPct(commonOdds[rank - 1]) + " chance)"));
        } else if (biome == Biome.WOODED_BADLANDS) {
            guaranteed.add(new ResourceDrop(new ItemStack(Material.GRASS_BLOCK, 32)));
            guaranteed.add(new ResourceDrop(new ItemStack(Material.COARSE_DIRT, 32)));
            guaranteed.add(new ResourceDrop(new ItemStack(Material.STONE, 32)));
            guaranteed.add(new ResourceDrop(new ItemStack(Material.TERRACOTTA, 32)));
            guaranteed.add(new ResourceDrop(new ItemStack(Material.OAK_LOG, 32)));
            guaranteed.add(new ResourceDrop(new ItemStack(Material.LEAF_LITTER, 16)));
            guaranteed.add(new ResourceDrop(new ItemStack(Material.COAL_ORE, 4)));
            guaranteed.add(new ResourceDrop(new ItemStack(Material.GOLD_ORE, 4)));
            String terracottaNoteW = "1 of 6 possible per claim";
            guaranteed.add(new ResourceDrop(new ItemStack(Material.RED_TERRACOTTA, 8), 0, 0, terracottaNoteW, true));
            guaranteed.add(new ResourceDrop(new ItemStack(Material.ORANGE_TERRACOTTA, 8), 0, 0, terracottaNoteW, true));
            guaranteed.add(new ResourceDrop(new ItemStack(Material.YELLOW_TERRACOTTA, 8), 0, 0, terracottaNoteW, true));
            guaranteed.add(new ResourceDrop(new ItemStack(Material.BROWN_TERRACOTTA, 8), 0, 0, terracottaNoteW, true));
            guaranteed.add(new ResourceDrop(new ItemStack(Material.LIGHT_GRAY_TERRACOTTA, 8), 0, 0, terracottaNoteW, true));
            guaranteed.add(new ResourceDrop(new ItemStack(Material.WHITE_TERRACOTTA, 8), 0, 0, terracottaNoteW, true));
            rares.add(new ResourceDrop(new ItemStack(Material.ARMADILLO_SCUTE, 1), 0, commonOdds[rank - 1],
                    "Rare Drop (" + formatPct(commonOdds[rank - 1]) + " chance)"));

        // Nether and End
        } else if (biome == Biome.NETHER_WASTES) {
            guaranteed.add(new ResourceDrop(new ItemStack(Material.NETHERRACK, 64)));
            guaranteed.add(new ResourceDrop(new ItemStack(Material.NETHERRACK, 64)));
            guaranteed.add(new ResourceDrop(new ItemStack(Material.BLACKSTONE, 32)));
            guaranteed.add(new ResourceDrop(new ItemStack(Material.MAGMA_BLOCK, 8)));
            guaranteed.add(new ResourceDrop(new ItemStack(Material.NETHER_QUARTZ_ORE, 32)));
            guaranteed.add(new ResourceDrop(new ItemStack(Material.NETHER_GOLD_ORE, 16)));
            rares.add(new ResourceDrop(new ItemStack(Material.GHAST_TEAR, 1), 0, commonOdds[rank - 1],
                    "Rare Drop (" + formatPct(commonOdds[rank - 1]) + " chance)"));
            rares.add(new ResourceDrop(new ItemStack(Material.NETHERITE_SCRAP, 2), 0, commonOdds[rank - 1],
                    "Rare Drop (" + formatPct(commonOdds[rank - 1]) + " chance)"));
            simulations.add(netherFortress);
            simulations.add(bastionRemnant);
        } else if (biome == Biome.SOUL_SAND_VALLEY) {
            guaranteed.add(new ResourceDrop(new ItemStack(Material.SOUL_SAND, 32)));
            guaranteed.add(new ResourceDrop(new ItemStack(Material.SOUL_SOIL, 32)));
            guaranteed.add(new ResourceDrop(new ItemStack(Material.GRAVEL, 8)));
            guaranteed.add(new ResourceDrop(new ItemStack(Material.NETHERRACK, 32)));
            guaranteed.add(new ResourceDrop(new ItemStack(Material.BASALT, 16)));
            guaranteed.add(new ResourceDrop(new ItemStack(Material.BLACKSTONE, 32)));
            guaranteed.add(new ResourceDrop(new ItemStack(Material.BONE_BLOCK, 8)));
            guaranteed.add(new ResourceDrop(new ItemStack(Material.NETHER_QUARTZ_ORE, 8)));
            guaranteed.add(new ResourceDrop(new ItemStack(Material.NETHER_GOLD_ORE, 4)));
            guaranteed.add(new ResourceDrop(new ItemStack(Material.BONE, 4)));
            guaranteed.add(new ResourceDrop(new ItemStack(Material.GHAST_TEAR, rank)));
            rares.add(new ResourceDrop(new ItemStack(Material.NETHERITE_SCRAP, 1), 0, commonOdds[rank - 1],
                    "Rare Drop (" + formatPct(commonOdds[rank - 1]) + " chance)"));
            rares.add(new ResourceDrop(new ItemStack(Material.DRIED_GHAST, 1), 0, 8,
                    "Rare Drop (" + formatPct(8) + " chance)"));
            simulations.add(netherFortress);
            simulations.add(bastionRemnant);
        } else if (biome == Biome.CRIMSON_FOREST) {
            guaranteed.add(new ResourceDrop(new ItemStack(Material.CRIMSON_NYLIUM, 64)));
            guaranteed.add(new ResourceDrop(new ItemStack(Material.NETHERRACK, 64)));
            guaranteed.add(new ResourceDrop(new ItemStack(Material.BLACKSTONE, 64)));
            guaranteed.add(new ResourceDrop(new ItemStack(Material.CRIMSON_STEM, 64)));
            guaranteed.add(new ResourceDrop(new ItemStack(Material.SHROOMLIGHT, 16)));
            guaranteed.add(new ResourceDrop(new ItemStack(Material.WEEPING_VINES, 16)));
            guaranteed.add(new ResourceDrop(new ItemStack(Material.CRIMSON_FUNGUS, 16)));
            guaranteed.add(new ResourceDrop(new ItemStack(Material.NETHER_QUARTZ_ORE, 16)));
            guaranteed.add(new ResourceDrop(new ItemStack(Material.NETHER_GOLD_ORE, 8)));
            guaranteed.add(new ResourceDrop(new ItemStack(Material.PORKCHOP, 16)));
            rares.add(new ResourceDrop(new ItemStack(Material.NETHERITE_SCRAP, 1), 0, commonOdds[rank - 1],
                    "Rare Drop (" + formatPct(commonOdds[rank - 1]) + " chance)"));
            simulations.add(netherFortress);
            simulations.add(bastionRemnant);
        } else if (biome == Biome.WARPED_FOREST) {
            guaranteed.add(new ResourceDrop(new ItemStack(Material.WARPED_NYLIUM, 32)));
            guaranteed.add(new ResourceDrop(new ItemStack(Material.NETHERRACK, 64)));
            guaranteed.add(new ResourceDrop(new ItemStack(Material.BLACKSTONE, 32)));
            guaranteed.add(new ResourceDrop(new ItemStack(Material.WARPED_STEM, 32)));
            guaranteed.add(new ResourceDrop(new ItemStack(Material.SHROOMLIGHT, 8)));
            guaranteed.add(new ResourceDrop(new ItemStack(Material.TWISTING_VINES, 8)));
            guaranteed.add(new ResourceDrop(new ItemStack(Material.WARPED_FUNGUS, 8)));
            guaranteed.add(new ResourceDrop(new ItemStack(Material.NETHER_QUARTZ_ORE, 8)));
            guaranteed.add(new ResourceDrop(new ItemStack(Material.NETHER_GOLD_ORE, 4)));
            guaranteed.add(new ResourceDrop(new ItemStack(Material.ENDER_PEARL, rank)));
            rares.add(new ResourceDrop(new ItemStack(Material.NETHERITE_SCRAP, 1), 0, commonOdds[rank - 1],
                    "Rare Drop (" + formatPct(commonOdds[rank - 1]) + " chance)"));
            simulations.add(netherFortress);
            simulations.add(bastionRemnant);
        } else if (biome == Biome.BASALT_DELTAS) {
            guaranteed.add(new ResourceDrop(new ItemStack(Material.NETHERRACK, 32)));
            guaranteed.add(new ResourceDrop(new ItemStack(Material.BASALT, 64)));
            guaranteed.add(new ResourceDrop(new ItemStack(Material.BASALT, 64)));
            guaranteed.add(new ResourceDrop(new ItemStack(Material.BLACKSTONE, 64)));
            guaranteed.add(new ResourceDrop(new ItemStack(Material.BLACKSTONE, 64)));
            guaranteed.add(new ResourceDrop(new ItemStack(Material.MAGMA_BLOCK, 8)));
            guaranteed.add(new ResourceDrop(new ItemStack(Material.NETHER_QUARTZ_ORE, 16)));
            guaranteed.add(new ResourceDrop(new ItemStack(Material.NETHER_GOLD_ORE, 16)));
            guaranteed.add(new ResourceDrop(new ItemStack(Material.MAGMA_CREAM, 4)));
            rares.add(new ResourceDrop(new ItemStack(Material.NETHERITE_SCRAP, 1), 0, commonOdds[rank - 1],
                    "Rare Drop (" + formatPct(commonOdds[rank - 1]) + " chance)"));
            rares.add(new ResourceDrop(new ItemStack(Material.GHAST_TEAR, rank), 0, 0,
                    "0 to " + rank + " Ghast Tears (random amount per claim)", true));
            simulations.add(netherFortress);
            simulations.add(bastionRemnant);
        } else if (biome == Biome.THE_END || biome == Biome.END_BARRENS || biome == Biome.END_HIGHLANDS
                || biome == Biome.END_MIDLANDS || biome == Biome.SMALL_END_ISLANDS) {
            guaranteed.add(new ResourceDrop(new ItemStack(Material.END_STONE, 64)));
            guaranteed.add(new ResourceDrop(new ItemStack(Material.END_STONE, 64)));
            guaranteed.add(new ResourceDrop(new ItemStack(Material.OBSIDIAN, 16)));
            guaranteed.add(new ResourceDrop(new ItemStack(Material.CHORUS_FRUIT, 16)));
            guaranteed.add(new ResourceDrop(new ItemStack(Material.PURPUR_BLOCK, 4)));
            simulations.add(endCity);
        }

        return new BiomeTable(guaranteed, simulations, rares, varieties);
    }

    /**
     * Provides the icon associated to the biome.
     *
     * @param biome The biome.
     * @return The icon.
     */
    public static List<ItemStack> getResourcesByDominionAndBiome(Dominion dominion, Biome biome, double yieldMultiplier) {
        BiomeTable table = buildBiomeTable(dominion, biome);
        int rank = dominion.getDominionLevel();
        Random random = new Random();
        List<ItemStack> items = new ArrayList<>();

        // Guaranteed drops
        for (ResourceDrop drop : table.guaranteed) {
            if (drop.previewOnly) continue;
            items.add(resolveAmount(drop, random));
        }

        // Simulation drops
        for (SimulationGroup sim : table.simulations) {
            if (random.nextInt(sim.odds) == 0) {
                for (ResourceDrop drop : sim.drops) {
                    if (drop.previewOnly) continue;
                    if (drop.subChanceOdds > 0 && random.nextInt(drop.subChanceOdds) != 0) continue;
                    items.add(resolveAmount(drop, random));
                }
            }
        }

        // Rare drops
        for (ResourceDrop drop : table.rares) {
            if (drop.previewOnly) continue;
            if (drop.subChanceOdds > 0 && random.nextInt(drop.subChanceOdds) != 0) continue;
            items.add(resolveAmount(drop, random));
        }

        // Variety selections - biome-specific random choice logic
        if (biome == Biome.WARM_OCEAN) {
            int coralType = random.nextInt(5);
            if (coralType == 0) {
                items.add(new ItemStack(Material.TUBE_CORAL_BLOCK, 8));
                items.add(new ItemStack(Material.TUBE_CORAL, 4));
                items.add(new ItemStack(Material.TUBE_CORAL_FAN, 4));
            } else if (coralType == 1) {
                items.add(new ItemStack(Material.BRAIN_CORAL_BLOCK, 8));
                items.add(new ItemStack(Material.BRAIN_CORAL, 4));
                items.add(new ItemStack(Material.BRAIN_CORAL_FAN, 4));
            } else if (coralType == 2) {
                items.add(new ItemStack(Material.BUBBLE_CORAL_BLOCK, 8));
                items.add(new ItemStack(Material.BUBBLE_CORAL, 4));
                items.add(new ItemStack(Material.BUBBLE_CORAL_FAN, 4));
            } else if (coralType == 3) {
                items.add(new ItemStack(Material.FIRE_CORAL_BLOCK, 8));
                items.add(new ItemStack(Material.FIRE_CORAL, 4));
                items.add(new ItemStack(Material.FIRE_CORAL_FAN, 4));
            } else {
                items.add(new ItemStack(Material.HORN_CORAL_BLOCK, 8));
                items.add(new ItemStack(Material.HORN_CORAL, 4));
                items.add(new ItemStack(Material.HORN_CORAL_FAN, 4));
            }
        } else if (biome == Biome.PLAINS || biome == Biome.SUNFLOWER_PLAINS) {
            int flowerNum = random.nextInt(3);
            if (flowerNum == 0) {
                items.add(new ItemStack(Material.POPPY, 8));
            } else if (flowerNum == 1) {
                items.add(new ItemStack(Material.OXEYE_DAISY, 8));
            } else {
                items.add(new ItemStack(Material.CORNFLOWER, 8));
            }
        } else if (biome == Biome.FLOWER_FOREST) {
            int flowerNum = random.nextInt(14);
            if (flowerNum == 0) { items.add(new ItemStack(Material.DANDELION, 8)); }
            else if (flowerNum == 1) { items.add(new ItemStack(Material.POPPY, 8)); }
            else if (flowerNum == 2) { items.add(new ItemStack(Material.WHITE_TULIP, 8)); }
            else if (flowerNum == 3) { items.add(new ItemStack(Material.PINK_TULIP, 8)); }
            else if (flowerNum == 4) { items.add(new ItemStack(Material.ORANGE_TULIP, 8)); }
            else if (flowerNum == 5) { items.add(new ItemStack(Material.RED_TULIP, 8)); }
            else if (flowerNum == 6) { items.add(new ItemStack(Material.ALLIUM, 8)); }
            else if (flowerNum == 7) { items.add(new ItemStack(Material.AZURE_BLUET, 8)); }
            else if (flowerNum == 8) { items.add(new ItemStack(Material.OXEYE_DAISY, 8)); }
            else if (flowerNum == 9) { items.add(new ItemStack(Material.LILY_OF_THE_VALLEY, 8)); }
            else if (flowerNum == 10) { items.add(new ItemStack(Material.CORNFLOWER, 8)); }
            else if (flowerNum == 11) { items.add(new ItemStack(Material.LILAC, 4)); }
            else if (flowerNum == 12) { items.add(new ItemStack(Material.PEONY, 4)); }
            else { items.add(new ItemStack(Material.ROSE_BUSH, 4)); }
        } else if (biome == Biome.BADLANDS || biome == Biome.ERODED_BADLANDS) {
            int terracottaVariant = random.nextInt(6);
            if (terracottaVariant == 0) { items.add(new ItemStack(Material.RED_TERRACOTTA, 16)); }
            else if (terracottaVariant == 1) { items.add(new ItemStack(Material.ORANGE_TERRACOTTA, 16)); }
            else if (terracottaVariant == 2) { items.add(new ItemStack(Material.YELLOW_TERRACOTTA, 16)); }
            else if (terracottaVariant == 3) { items.add(new ItemStack(Material.BROWN_TERRACOTTA, 16)); }
            else if (terracottaVariant == 4) { items.add(new ItemStack(Material.LIGHT_GRAY_TERRACOTTA, 16)); }
            else { items.add(new ItemStack(Material.WHITE_TERRACOTTA, 16)); }
        } else if (biome == Biome.WOODED_BADLANDS) {
            int terracottaVariant = random.nextInt(6);
            if (terracottaVariant == 0) { items.add(new ItemStack(Material.RED_TERRACOTTA, 8)); }
            else if (terracottaVariant == 1) { items.add(new ItemStack(Material.ORANGE_TERRACOTTA, 8)); }
            else if (terracottaVariant == 2) { items.add(new ItemStack(Material.YELLOW_TERRACOTTA, 8)); }
            else if (terracottaVariant == 3) { items.add(new ItemStack(Material.BROWN_TERRACOTTA, 8)); }
            else if (terracottaVariant == 4) { items.add(new ItemStack(Material.LIGHT_GRAY_TERRACOTTA, 8)); }
            else { items.add(new ItemStack(Material.WHITE_TERRACOTTA, 8)); }
        }

        // Special: Basalt Deltas ghast tear - random 0 to rank amount
        if (biome == Biome.BASALT_DELTAS) {
            int ghastTears = random.nextInt(rank + 1);
            if (ghastTears > 0) {
                items.add(new ItemStack(Material.GHAST_TEAR, ghastTears));
            }
        }

        // Apply food yield multiplier to item quantities before rank duplication
        if (yieldMultiplier < 1.0) {
            List<ItemStack> yieldScaled = new ArrayList<>();
            for (ItemStack item : items) {
                int scaled = (int) Math.round(item.getAmount() * yieldMultiplier);
                if (scaled > 0) {
                    item.setAmount(scaled);
                    yieldScaled.add(item);
                }
            }
            items = yieldScaled;
        }

        List<ItemStack> multiplierItems = new ArrayList<>();
        for (ItemStack item : items) {
            multiplierItems.add(item);

            // Skip multiplying these rare/special items
            if (item.getType() == Material.NAUTILUS_SHELL || item.getType() == Material.NETHERITE_SCRAP
                    || item.getType() == Material.TURTLE_SCUTE || item.getType() == Material.ARMADILLO_SCUTE
                    || item.getType() == Material.GHAST_TEAR || item.getType() == Material.DIAMOND
                    || item.getType() == Material.HEART_OF_THE_SEA
                    || item.isSimilar(new GodAppleFragment().getItem())) {
                continue;
            }

            // Rank 1 = 1x, rank 2 = 2x, rank 3 = 3x, rank 4 = 4x, rank 5 = 5x
            for (int i = 1; i < rank; i++) {
                multiplierItems.add(item);
            }
        }

        return multiplierItems;
    }

    /**
     * Provides the rewards for the Dominion.
     */
    public static void provideDominionRewards() {
        for (Dominion dominion : getDominions()) {
            int claimableAmount = dominion.getClaimableResources();
            OfflinePlayer player = Bukkit.getOfflinePlayer(dominion.getLeader());

            boolean isAmountIncreasing = false;
            boolean isClaimPrevented = false;
            int maxClaimableResourcesAmount = getMaxClaimableResourcesAmount(dominion);

            if (claimableAmount < maxClaimableResourcesAmount) {
                if (isAllowedToClaimResources(dominion)) {
                    double yieldThisWeek = foodPercentageToYield(getBaseFoodPercentage(dominion));
                    int toAdd = Math.min(2, maxClaimableResourcesAmount - claimableAmount);
                    claimableAmount += toAdd;
                    dominion.setClaimableResources(claimableAmount);
                    for (int j = 0; j < toAdd; j++) {
                        dominion.getClaimFoodYields().add(yieldThisWeek);
                    }
                    isAmountIncreasing = true;
                } else {
                    isClaimPrevented = true;
                }
            }

            if (!dominion.getConquered().isEmpty()) {
                // $500 per week per conquered Dominion, limit of 5 conquered rewards per week
                int money = dominion.getConquered().size() >= 5 ? 2500 : dominion.getConquered().size() * 500;
                dominion.setBalance(dominion.getBalance() + money);
                // Deduct $75/week per vassal as upkeep cost (conqueror funds the conquered dominion's taxes)
                int upkeepCost = dominion.getConquered().size() >= 5 ? 375 : dominion.getConquered().size() * 75;
                dominion.setBalance(dominion.getBalance() - upkeepCost);
            }

            // If the dominion is over its chunk limit, unclaim one chunk per week
            int maxChunks = dominion.getMaxChunks();
            if (dominion.getChunks().size() > maxChunks) {
                Chunk homeChunk = dominion.getDominionHome().getChunk();
                Chunk chunkToRemove = null;
                for (Chunk chunk : dominion.getChunks()) {
                    if (chunk.getX() == homeChunk.getX() && chunk.getZ() == homeChunk.getZ()
                            && chunk.getWorld().equals(homeChunk.getWorld())) {
                        continue;
                    }
                    if (isAllClaimsConnectedAfterUnclaiming(dominion, chunk)) {
                        chunkToRemove = chunk;
                        break;
                    }
                }
                if (chunkToRemove != null) {
                    dominion.getChunks().remove(chunkToRemove);
                    chunkKeyToDominion.remove(getChunkKey(chunkToRemove));
                    resizeFoodArray(dominion);
                    refreshBiomeCache(dominion);
                    if (player.isOnline()) {
                        player.getPlayer().sendMessage(ChatUtils.chatMessage(
                                "&cYour Dominion is over its chunk limit and has lost a chunk! "
                                        + "&7(" + dominion.getChunks().size() + "/" + maxChunks + " chunks)"));
                    }
                }
            }

            updateDominion(dominion);

            if (player.isOnline()) {
                if (isAmountIncreasing) {
                    int foodPct = (int) Math.round(getBaseFoodPercentage(dominion) * 100);
                    int yieldPct = (int) Math.round(foodPercentageToYield(foodPct / 100.0) * 100);
                    player.getPlayer().sendMessage(ChatUtils.chatMessage(Lang.get("dominion.new_week_resources")));
                    player.getPlayer().sendMessage(ChatUtils.chatMessage(Lang.get("dominion.new_week_yield", "food", foodPct, "yield", yieldPct)));
                } else {
                    if (isClaimPrevented) {
                        player.getPlayer().sendMessage(ChatUtils.chatMessage(Lang.get("dominion.claim_failed")));
                    } else {
                        player.getPlayer().sendMessage(ChatUtils.chatMessage(Lang.get("dominion.claim_available")));
                    }
                }

            }
        }
    }

    /**
     * Provides the UUID of the Dominion leader that conquered the input Dominion.
     *
     * @param dominion The Dominion.
     * @return The UUID of the Dominion leader that conquered the input Dominion.
     */
    public static UUID getConquerorOfDominion(Dominion dominion) {
        for (Dominion dominionInList : getDominions()) {
            if (dominionInList.getConquered().contains(dominion.getLeader())) {
                return dominionInList.getLeader();
            }
        }
        return null;
    }

    /**
     * Provides the maximum amount of resources that the Dominion is permitted to claim.
     *
     * @param dominion The Dominion.
     * @return The maximum amount of resources that the Dominion is permitted to claim.
     */
    public static int getMaxClaimableResourcesAmount(Dominion dominion) {
        // If the Dominion is conquered, they are limited to a total of 8 claims
        if (getConquerorOfDominion(dominion) != null) {
            return 8;
        }
        // Base amount by dominion level
        int[] baseByLevel = {8, 12, 20, 28, 36};
        int total = baseByLevel[dominion.getDominionLevel() - 1];
        total += dominion.getConquered().size() * 8;
        total += OutpostUtils.getDominionOutposts(dominion.getLeader()).size() * 4;
        return total;
    }

    /**
     * Determines whether the Dominion will be permitted to claim resources.
     * 50% chance for conquered Dominions, 100% chance for non-conquered.
     *
     * @param dominion The Dominion.
     * @return Whether the Dominion will be permitted to claim resources.
     */
    private static boolean isAllowedToClaimResources(Dominion dominion) {
        // If the Dominion is conquered, there is a 50% chance that the claim will be skipped
        if (getConquerorOfDominion(dominion) != null) {
            return new Random().nextBoolean();
        }

        // Always allowed to claim if not conquered
        return true;
    }

    /**
     * Provides the color code for the given rank.
     *
     * @param rank The rank.
     * @return The color code string.
     */
    public static String getRankColor(DominionRank rank) {
        return switch (rank) {
            case LEADER -> "&6";
            case LIEUTENANT -> "&b";
            case CITIZEN -> "&a";
            case NEWCOMER -> "&7";
            case ALLIED -> "&5";
            case TRUCED -> "&d";
            case NEUTRAL -> "&f";
            case WANDERER -> "&e";
            case ENEMIED -> "&c";
        };
    }

    /**
     * Provides the formatted Dominion rank name.
     *
     * @param rank The rank.
     * @return The formatted Dominion rank name.
     */
    public static String getFormattedRankName(DominionRank rank) {
        return getRankColor(rank) + (rank.name().toUpperCase()).charAt(0) + (rank.name().toLowerCase()).substring(1);
    }

    /**
     * Verifies active conquest/rebellion requests and resolves any that have reached a deadline or activity threshold.
     */
    public static void checkAndProcessConquestDeadlines() {
        long now = System.currentTimeMillis();

        List<Dominion> conquestInactive = new ArrayList<>();
        List<Dominion> conquestExpired = new ArrayList<>();
        List<Dominion> rebellionInactive = new ArrayList<>();
        List<Dominion> rebellionExpired = new ArrayList<>();

        for (Dominion dominion : getDominions()) {
            // Conquest checks (inactivity takes priority over expiry)
            if (dominion.getConqueredRequest() != null && dominion.getConqueredRequestTimestamp() > 0) {
                long defenderLastSeen = dominion.getConqueredRequestDefenderLastSeen();
                // If no defender has logged on for 3 consecutive days, auto-conquer
                if (defenderLastSeen > 0 && now - defenderLastSeen >= CONQUEST_INACTIVITY_MS) {
                    conquestInactive.add(dominion);
                }
                // If the 7-day conquest window expires with defenders having been active
                else if (now - dominion.getConqueredRequestTimestamp() >= CONQUEST_DEADLINE_MS) {
                    conquestExpired.add(dominion);
                }
            }
            // Rebellion checks (inactivity takes priority over expiry)
            if (dominion.getRebelRequest() != null && dominion.getRebelRequestTimestamp() > 0) {
                long lastSeen = dominion.getRebelRequestConquerorLastSeen();
                // If the conquerors are inactive for 3 consecutive days, rebellion auto-succeeds
                if (lastSeen > 0 && now - lastSeen >= REBEL_INACTIVITY_MS) {
                    rebellionInactive.add(dominion);
                }
                // If the 7-day rebellion window expires with no resolution
                else if (now - dominion.getRebelRequestTimestamp() >= REBEL_DEADLINE_MS) {
                    rebellionExpired.add(dominion);
                }
            }
        }

        NetworkManager nm = NetworkManager.isActive() ? NetworkManager.getInstance() : null;

        // Conquest auto-success, no defender logged on for 3 consecutive days
        for (Dominion defender : conquestInactive) {
            Dominion conqueror = getPlayerDominion(defender.getConqueredRequest());
            defender.setConqueredRequest(null);
            defender.setConqueredRequestTimestamp(0L);
            defender.setConqueredRequestDefenderLastSeen(0L);

            if (conqueror == null) {
                updateDominion(defender);
                continue;
            }

            List<UUID> conquered = conqueror.getConquered();
            conquered.add(defender.getLeader());
            conquered.addAll(defender.getConquered());
            defender.setConquered(new ArrayList<>());
            conqueror.setConquered(conquered);
            conqueror.setLastConquerAttemptTimestamp(now);
            defender.setLastRebelAttemptTimestamp(now);
            defender.setConqueredTimestamp(now);
            updateDominion(defender);
            updateDominion(conqueror);
            String conquestAutoMsg = ChatUtils.chatMessage(defender.getName() + " &4has been conquered by &e" + conqueror.getName());
            for (Player onlinePlayer : Bukkit.getOnlinePlayers()) {
                int domVol = AranarthUtils.getPlayer(onlinePlayer.getUniqueId()).getDominionSoundVolume();
                if (domVol > 0) {
                    onlinePlayer.playSound(onlinePlayer, Sound.ITEM_GOAT_HORN_SOUND_7, domVol / 100f, 1F);
                }
                onlinePlayer.sendMessage(conquestAutoMsg);
            }
            DiscordUtils.dominionMessage(defender, defender.getName() + " has been conquered by " + conqueror.getName(), new Color(101, 0, 0));
            if (nm != null) {
                nm.publishBroadcast(conquestAutoMsg);
                // a=defender, b=conqueror
                nm.publishDominionConquestUpdate("conquest_auto", defender, conqueror);
            }
        }

        // Conquest expired, 7-day window ended with defenders having been active, no outcome
        for (Dominion defender : conquestExpired) {
            Dominion conqueror = getPlayerDominion(defender.getConqueredRequest());
            defender.setConqueredRequest(null);
            defender.setConqueredRequestTimestamp(0L);
            defender.setConqueredRequestDefenderLastSeen(0L);

            if (conqueror == null) {
                updateDominion(defender);
                continue;
            }

            conqueror.setLastConquerAttemptTimestamp(now);
            updateDominion(defender);
            updateDominion(conqueror);
            String conquestExpiredMsg = ChatUtils.chatMessage(Lang.get("dominion.conquest_failed_broadcast", "conqueror", conqueror.getName(), "defender", defender.getName()));
            for (Player onlinePlayer : Bukkit.getOnlinePlayers()) {
                int domVol = AranarthUtils.getPlayer(onlinePlayer.getUniqueId()).getDominionSoundVolume();
                if (domVol > 0) {
                    onlinePlayer.playSound(onlinePlayer, Sound.ITEM_GOAT_HORN_SOUND_0, domVol / 100f, 1F);
                }
                onlinePlayer.sendMessage(conquestExpiredMsg);
            }
            DiscordUtils.dominionMessage(defender, conqueror.getName() + "'s conquest of " + defender.getName() + " has failed", new Color(135, 245, 220));
            if (nm != null) {
                nm.publishBroadcast(conquestExpiredMsg);
                // a=defender, b=conqueror
                nm.publishDominionConquestUpdate("conquest_expired", defender, conqueror);
            }
        }

        // Rebellion auto-success, conqueror was inactive for 3 consecutive days
        for (Dominion conqueror : rebellionInactive) {
            Dominion rebel = getPlayerDominion(conqueror.getRebelRequest());
            conqueror.setRebelRequest(null);
            conqueror.setRebelRequestTimestamp(0L);
            conqueror.setRebelRequestConquerorLastSeen(0L);

            if (rebel == null) {
                updateDominion(conqueror);
                continue;
            }

            List<UUID> conquered = conqueror.getConquered();
            conquered.remove(rebel.getLeader());
            conqueror.setConquered(conquered);
            rebel.setLastRebelAttemptTimestamp(now);
            updateDominion(conqueror);
            updateDominion(rebel);
            String rebelAutoMsg = ChatUtils.chatMessage(rebel.getName() + " &dhas successfully rebelled against &e" + conqueror.getName());
            for (Player onlinePlayer : Bukkit.getOnlinePlayers()) {
                int domVol = AranarthUtils.getPlayer(onlinePlayer.getUniqueId()).getDominionSoundVolume();
                if (domVol > 0) {
                    onlinePlayer.playSound(onlinePlayer, Sound.ITEM_GOAT_HORN_SOUND_0, domVol / 100f, 1F);
                }
                onlinePlayer.sendMessage(rebelAutoMsg);
            }
            DiscordUtils.dominionMessage(conqueror, rebel.getName() + " has successfully rebelled against " + conqueror.getName(), new Color(135, 245, 220));
            if (nm != null) {
                nm.publishBroadcast(rebelAutoMsg);
                // a=rebel, b=conqueror
                nm.publishDominionConquestUpdate("rebel_auto", rebel, conqueror);
            }
        }

        // Rebellion expired, 7 days passed with no resolution so the dominion stays conquered
        for (Dominion conqueror : rebellionExpired) {
            Dominion rebel = getPlayerDominion(conqueror.getRebelRequest());
            conqueror.setRebelRequest(null);
            conqueror.setRebelRequestTimestamp(0L);
            conqueror.setRebelRequestConquerorLastSeen(0L);

            if (rebel != null) {
                rebel.setLastRebelAttemptTimestamp(now);
                updateDominion(rebel);
            }
            updateDominion(conqueror);

            if (rebel != null) {
                String rebelExpiredMsg = ChatUtils.chatMessage(Lang.get("dominion.rebellion_failed_broadcast", "rebel", rebel.getName(), "conqueror", conqueror.getName()));
                for (Player onlinePlayer : Bukkit.getOnlinePlayers()) {
                    int domVol = AranarthUtils.getPlayer(onlinePlayer.getUniqueId()).getDominionSoundVolume();
                    if (domVol > 0) {
                        onlinePlayer.playSound(onlinePlayer, Sound.ITEM_GOAT_HORN_SOUND_4, domVol / 100f, 1F);
                    }
                    onlinePlayer.sendMessage(rebelExpiredMsg);
                }
                DiscordUtils.dominionMessage(conqueror, rebel.getName() + "'s rebellion against " + conqueror.getName() + " has failed", new Color(101, 0, 0));
                if (nm != null) {
                    nm.publishBroadcast(rebelExpiredMsg);
                    // a=rebel, b=conqueror
                    nm.publishDominionConquestUpdate("rebel_expired", rebel, conqueror);
                }
            }
        }
    }

    /**
     * Provides the death penalty multiplier that applies to the victim based on the
     * current war state between the victim's and killer's dominions.
     *
     * @param victimUuid The UUID of the player who died.
     * @param killerUuid The UUID of the player who killed them.
     * @return The multiplier to apply to the victim's death penalties (1 or 3).
     */
    public static int getDeathPenaltyMultiplier(UUID victimUuid, UUID killerUuid) {
        Dominion victimDom = getPlayerDominion(victimUuid);
        Dominion killerDom = getPlayerDominion(killerUuid);
        // One of the two is null
        if (victimDom == null || killerDom == null || victimDom.isSameDominion(killerDom)) {
            return 1;
        }

        // Active conquest: killerDom is conquering victimDom (victim is defender)
        if (victimDom.getConqueredRequest() != null
                && victimDom.getConqueredRequest().equals(killerDom.getLeader())) {
            return 3;
        }
        // Active conquest: victimDom is conquering killerDom (victim is attacker)
        if (killerDom.getConqueredRequest() != null
                && killerDom.getConqueredRequest().equals(victimDom.getLeader())) {
            return 3;
        }

        // Active rebellion: victimDom is the conqueror being rebelled against by killerDom
        if (victimDom.getRebelRequest() != null
                && victimDom.getRebelRequest().equals(killerDom.getLeader())
                && victimDom.getConquered().contains(killerDom.getLeader())) {
            return 3;
        }
        // Active rebellion: killerDom is the conqueror, victimDom is the rebelling dominion → 1x for rebel
        if (killerDom.getRebelRequest() != null
                && killerDom.getRebelRequest().equals(victimDom.getLeader())
                && killerDom.getConquered().contains(victimDom.getLeader())) {
            return 1;
        }

        return 1;
    }
}
