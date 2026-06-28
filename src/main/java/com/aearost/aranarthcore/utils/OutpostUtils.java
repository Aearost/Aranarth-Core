package com.aearost.aranarthcore.utils;

import com.aearost.aranarthcore.objects.AranarthPlayer;
import com.aearost.aranarthcore.objects.Dominion;
import com.aearost.aranarthcore.objects.DominionPermission;
import com.aearost.aranarthcore.objects.DominionRank;
import com.aearost.aranarthcore.objects.Outpost;
import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.entity.Player;

import java.text.NumberFormat;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Manages all outpost data and operations.
 */
public class OutpostUtils {

    /**
     * Base chunk limits per outpost index (index 0 = outpost 1, ..., index 3 = outpost 4).
     */
    private static final int[] BASE_CHUNK_LIMITS = {4, 10, 40, 100};

    /**
     * Unlock costs per outpost slot (index 0 = 1st outpost, ..., index 3 = 4th outpost).
     */
    public static final double[] OUTPOST_COSTS = {250_000, 1_000_000, 5_000_000, 25_000_000};

    private static final Map<UUID, Outpost> outpostById = new HashMap<>();
    private static final Map<String, Outpost> chunkKeyToOutpost = new HashMap<>();
    private static final Map<UUID, List<Outpost>> dominionToOutposts = new HashMap<>();

    /**
     * Registers a new outpost in all lookup maps.
     */
    public static void registerOutpost(Outpost outpost) {
        outpostById.put(outpost.getId(), outpost);
        dominionToOutposts.computeIfAbsent(outpost.getDominionId(), k -> new ArrayList<>()).add(outpost);
        for (Chunk chunk : outpost.getChunks()) {
            chunkKeyToOutpost.put(getChunkKey(chunk), outpost);
        }
    }

    /**
     * Updates an existing outpost in the lookup maps (re-syncs chunk index).
     */
    public static void updateOutpost(Outpost outpost) {
        outpostById.put(outpost.getId(), outpost);

        // Refresh chunk map
        chunkKeyToOutpost.entrySet().removeIf(e -> e.getValue().getId().equals(outpost.getId()));
        for (Chunk chunk : outpost.getChunks()) {
            chunkKeyToOutpost.put(getChunkKey(chunk), outpost);
        }
    }

    /**
     * Removes an outpost from all lookup maps and notifies online dominion members.
     */
    public static void disbandOutpost(Dominion dominion, Outpost outpost) {
        DefenderUtils.teleportOutpostDefendersToDominion(dominion, outpost.getId());
        outpostById.remove(outpost.getId());
        chunkKeyToOutpost.entrySet().removeIf(e -> e.getValue().getId().equals(outpost.getId()));
        List<Outpost> list = dominionToOutposts.get(dominion.getId());
        if (list != null) {
            list.remove(outpost);
        }
        notifyMembers(dominion, "&cThe outpost &e" + outpost.getName()
                + " &chas been disbanded!");
    }

    /**
     * Shifts all outpost indices down by one.
     */
    public static void shiftOutpostIndicesDown(UUID dominionId, int disbandedIndex) {
        List<Outpost> all = dominionToOutposts.getOrDefault(dominionId, Collections.emptyList());
        for (Outpost outpost : all) {
            if (outpost.getOutpostIndex() > disbandedIndex) {
                outpost.setOutpostIndex(outpost.getOutpostIndex() - 1);
            }
        }
    }

    /**
     * Returns all outposts for a dominion, sorted by outpost index ascending.
     */
    public static List<Outpost> getDominionOutposts(UUID dominionId) {
        List<Outpost> outposts = dominionToOutposts.getOrDefault(dominionId, Collections.emptyList());
        return outposts.stream()
                .sorted(Comparator.comparingInt(Outpost::getOutpostIndex))
                .collect(Collectors.toList());
    }

    /**
     * Returns the outpost that owns the given chunk, or null if unclaimed by any outpost.
     */
    public static Outpost getOutpostOfChunk(Chunk chunk) {
        return chunkKeyToOutpost.get(getChunkKey(chunk));
    }

    /**
     * Returns the outpost with the given ID, or null.
     */
    public static Outpost getOutpostById(UUID id) {
        return outpostById.get(id);
    }

    /**
     * Returns the outpost the player is currently standing in, or null if not in any outpost.
     */
    public static Outpost getOutpostPlayerIsIn(Player player) {
        return getOutpostOfChunk(player.getLocation().getChunk());
    }

    /**
     * Returns the base chunk limit for a given outpost index (1–4).
     */
    public static int getBaseChunkLimit(int outpostIndex) {
        return BASE_CHUNK_LIMITS[outpostIndex - 1];
    }

    /**
     * Returns the maximum chunks this outpost can claim, including bought extras.
     */
    public static int getOutpostMaxChunks(Outpost outpost, Dominion dominion) {
        return getBaseChunkLimit(outpost.getOutpostIndex()) + dominion.getBoughtOutpostChunks();
    }

    /**
     * Returns how many outpost slots are allowed at the given dominion level.
     */
    public static int allowedOutpostCount(int dominionLevel) {
        return Math.max(0, dominionLevel - 1);
    }

    /**
     * Returns the next available outpost index for a dominion, or -1 if at the cap.
     */
    public static int getNextOutpostIndex(Dominion dominion) {
        int allowed = allowedOutpostCount(dominion.getDominionLevel());
        List<Outpost> existing = getDominionOutposts(dominion.getId());
        Set<Integer> usedIndices = existing.stream()
                .map(Outpost::getOutpostIndex)
                .collect(Collectors.toSet());
        for (int i = 1; i <= allowed; i++) {
            if (!usedIndices.contains(i)) {
                return i;
            }
        }
        return -1;
    }

    /**
     * Calculates the total cost of buying additional outpost chunks
     * Formula: 50000 * 1.06^(currentBought + i) per chunk.
     */
    public static double calculateBuyOutpostChunksCost(int currentBought, int amount) {
        double total = 0;
        for (int i = 0; i < amount; i++) {
            total += 50_000 * Math.pow(1.06, currentBought + i);
        }
        return total;
    }

    /**
     * Returns true if the input chunk is side-adjacent to at least one chunk in the outpost's chunks
     * within the same world.
     */
    public static boolean isConnectedToOutpostClaims(List<Chunk> outpostChunks, Chunk chunk) {
        int cx = chunk.getX();
        int cz = chunk.getZ();
        for (Chunk existing : outpostChunks) {
            if (!existing.getWorld().getName().equals(chunk.getWorld().getName())) {
                continue;
            }
            int dx = Math.abs(existing.getX() - cx);
            int dz = Math.abs(existing.getZ() - cz);
            if ((dx == 1 && dz == 0) || (dx == 0 && dz == 1)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Returns true if removing the input chunk from the outpost's chunks would leave
     * all remaining chunks still connected (BFS check). Single-chunk outposts always pass.
     */
    public static boolean isAllOutpostClaimsConnectedAfterUnclaiming(Outpost outpost, Chunk chunkToRemove) {
        List<Chunk> remaining = new ArrayList<>(outpost.getChunks());
        remaining.remove(chunkToRemove);
        if (remaining.size() <= 1) {
            return true;
        }
        Set<Chunk> visited = new HashSet<>();
        Deque<Chunk> queue = new ArrayDeque<>();
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
        return visited.size() == remaining.size();
    }

    /**
     * Attempts to claim the input chunk for the given outpost.
     * Deducts $250 from the dominion balance on success.
     *
     * @return A chat-formatted message describing the outcome.
     */
    public static String claimOutpostChunk(Player player, Outpost outpost, Dominion dominion, Chunk chunkToClaim) {
        int claimPrice = 1000;
        if (dominion.getBalance() < claimPrice) {
            return "&cYour dominion cannot afford this!";
        }
        int maxChunks = getOutpostMaxChunks(outpost, dominion);
        if (outpost.getChunks().size() >= maxChunks) {
            return "&cOutpost &e" + outpost.getName() + " &chas reached the limit of &e" + maxChunks + " &cchunks";
        }
        if (!outpost.getChunks().isEmpty() && !isConnectedToOutpostClaims(outpost.getChunks(), chunkToClaim)) {
            return "&cThis chunk is not connected to your outpost!";
        }
        dominion.setBalance(dominion.getBalance() - claimPrice);
        outpost.getChunks().add(chunkToClaim);
        chunkKeyToOutpost.put(getChunkKey(chunkToClaim), outpost);
        DominionUtils.updateDominion(dominion);
        return "&e" + outpost.getName() + " &7has claimed &e"
                + outpost.getChunks().size() + "/" + maxChunks + " outpost chunks";
    }

    /**
     * Attempts to unclaim the chunk the player is standing in from the given outpost.
     * @return A chat-formatted message describing the outcome.
     */
    public static String unclaimOutpostChunk(Player player, Outpost outpost, Dominion dominion, Chunk chunk) {
        if (!dominion.getLeader().equals(player.getUniqueId())
                && dominion.getMemberRank(player.getUniqueId()) != DominionRank.LIEUTENANT) {
            return "&cOnly the Leader or a Lieutenant can unclaim outpost land!";
        }
        if (!DominionUtils.hasPermission(player, dominion, DominionPermission.MANAGE_OUTPOSTS)
                && !dominion.getLeader().equals(player.getUniqueId())) {
            return "&cYou do not have permission to manage outposts";
        }
        Chunk homeChunk = outpost.getHome().getChunk();
        if (chunk.getX() == homeChunk.getX() && chunk.getZ() == homeChunk.getZ()
                && chunk.getWorld().equals(homeChunk.getWorld())) {
            return "&cYou cannot unclaim the outpost home chunk";
        }
        if (!isAllOutpostClaimsConnectedAfterUnclaiming(outpost, chunk)) {
            return "&cYou cannot unclaim this chunk as it would disconnect your outpost";
        }
        outpost.getChunks().remove(chunk);
        chunkKeyToOutpost.remove(getChunkKey(chunk));
        DominionUtils.updateDominion(dominion);

        // Refund half the claim price
        AranarthPlayer aranarthPlayer = AranarthUtils.getPlayer(player.getUniqueId());
        aranarthPlayer.setBalance(aranarthPlayer.getBalance() + (500));
        AranarthUtils.setPlayer(player.getUniqueId(), aranarthPlayer);

        return "&7Outpost chunk unclaimed successfully";
    }

    /**
     * Force-removes one chunk from an outpost for penalty purposes (no refund).
     *
     * @return true if the outpost was disbanded (home chunk reached), false if a chunk was removed.
     */
    public static boolean removePenaltyChunkFromOutpost(Dominion dominion, Outpost outpost) {
        Chunk homeChunk = outpost.getHome().getChunk();

        if (outpost.getChunks().size() == 1) {
            // Only the home chunk left, disband the outpost
            AranarthUtils.removeLocksInChunk(homeChunk);
            disbandOutpost(dominion, outpost);
            return true;
        }

        Chunk toRemove = null;
        List<Chunk> chunks = outpost.getChunks();
        for (int i = chunks.size() - 1; i >= 0; i--) {
            Chunk candidate = chunks.get(i);
            if (candidate.getX() == homeChunk.getX() && candidate.getZ() == homeChunk.getZ()
                    && candidate.getWorld().equals(homeChunk.getWorld())) {
                continue;
            }
            if (isAllOutpostClaimsConnectedAfterUnclaiming(outpost, candidate)) {
                toRemove = candidate;
                break;
            }
        }

        if (toRemove != null) {
            AranarthUtils.removeLocksInChunk(toRemove);
            chunks.remove(toRemove);
            chunkKeyToOutpost.remove(getChunkKey(toRemove));
            updateOutpost(outpost);
            notifyMembers(dominion, "&cA chunk has been lost from outpost &e" + outpost.getName()
                    + " &cfor failing to maintain the required dominion level");
        }
        return false;
    }

    public static String getChunkKey(Chunk chunk) {
        return chunk.getWorld().getName() + ":" + chunk.getX() + ":" + chunk.getZ();
    }

    private static boolean isSideAdjacent(Chunk a, Chunk b) {
        if (!a.getWorld().equals(b.getWorld())) {
            return false;
        }
        int dx = Math.abs(a.getX() - b.getX());
        int dz = Math.abs(a.getZ() - b.getZ());
        return dx + dz == 1;
    }

    private static void notifyMembers(Dominion dominion, String message) {
        for (UUID memberUuid : dominion.getMembers()) {
            Player member = Bukkit.getPlayer(memberUuid);
            if (member != null && member.isOnline()) {
                member.sendMessage(ChatUtils.chatMessage(message));
            }
        }
    }

    /**
     * Returns a formatted cost string for purchasing the nth outpost slot.
     */
    public static String getFormattedOutpostCost(int outpostIndex) {
        NumberFormat fmt = NumberFormat.getCurrencyInstance();
        return fmt.format(OUTPOST_COSTS[outpostIndex - 1]);
    }

    /**
     * Returns the total number of chunks claimed across all outposts of the given dominion.
     */
    public static int getTotalOutpostChunkCount(UUID dominionId) {
        int total = 0;
        for (Outpost outpost : getDominionOutposts(dominionId)) {
            total += outpost.getChunks().size();
        }
        return total;
    }
}
