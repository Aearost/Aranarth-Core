package com.aearost.aranarthcore.utils;

import com.aearost.aranarthcore.AranarthCore;
import com.aearost.aranarthcore.enums.Month;
import com.aearost.aranarthcore.objects.Dominion;
import com.aearost.aranarthcore.objects.Outpost;
import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.ChunkSnapshot;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.entity.Tameable;

import java.text.NumberFormat;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages the Dominion level system (levels 1–5).
 */
public class DominionLevelUtils {

    private static final int[] MONTH_DAYS = {147, 147, 146, 145, 146, 145, 146, 146, 146, 146, 146, 146, 146, 147, 147};
    public static final int DAYS_PER_YEAR = 2192;

    public static final int MAX_LEVEL = 5;
    public static final int CRITERIA_REQUIRED = 4;
    public static final int CRITERIA_COUNT = 6;

    // Thresholds per level
    private static final int[] MEMBERS_THRESHOLDS = {2, 4, 8, 12};
    private static final double[] BALANCE_THRESHOLDS = {50_000, 500_000, 5_000_000, 50_000_000};
    private static final int[] FARMLAND_THRESHOLDS = {50, 250, 1_000, 5_000};
    private static final int[] LIVESTOCK_THRESHOLDS = {15, 50, 120, 250};
    private static final int[] CHUNKS_THRESHOLDS = {10, 75, 250, 350};
    private static final int[] AGE_THRESHOLDS = {1, 2, 3, 4}; // Aranarth years


    private static final Set<EntityType> FARM_ANIMAL_TYPES = Set.of(
            EntityType.COW, EntityType.CHICKEN, EntityType.SHEEP, EntityType.PIG,
            EntityType.GOAT, EntityType.RABBIT, EntityType.MOOSHROOM
    );

    private static final Set<EntityType> TAMEABLE_LIVESTOCK_TYPES = Set.of(
            EntityType.HORSE, EntityType.DONKEY, EntityType.MULE, EntityType.LLAMA,
            EntityType.WOLF, EntityType.CAT, EntityType.PARROT
    );

    public static final String[] CATEGORY_NAMES = {
            "Members", "Balance", "Farmland", "Livestock", "Chunks", "Age"
    };

    /**
     * Buffer before penalties begin after a level drop.
     * 1 in-game month ≈ 145 Minecraft days × 20 real min/day = 2,900 real min ≈ 2 real days.
     */
    public static final long PENALTY_BUFFER_MS = 174_000_000L; // 145 MC days in ms

    /**
     * Money drained per 30-minute penalty scan cycle, indexed by the level the dominion
     * failed to maintain (index 0 = failed level 2, ..., index 3 = failed level 5).
     */
    private static final double[] PENALTY_MONEY = {5_000, 25_000, 100_000, 500_000};

    /**
     * Food power drained per 30-minute penalty scan cycle (same index mapping as PENALTY_MONEY).
     */
    private static final int[] PENALTY_FOOD_POWER = {200, 500, 1_000, 2_000};

    private static int idx(int targetLevel) {
        return targetLevel - 2;
    }

    public static int getMembersThreshold(int targetLevel) {
        return MEMBERS_THRESHOLDS[idx(targetLevel)];
    }

    public static double getBalanceThreshold(int targetLevel) {
        return BALANCE_THRESHOLDS[idx(targetLevel)];
    }

    public static int getFarmlandThreshold(int targetLevel) {
        return FARMLAND_THRESHOLDS[idx(targetLevel)];
    }

    public static int getLivestockThreshold(int targetLevel) {
        return LIVESTOCK_THRESHOLDS[idx(targetLevel)];
    }

    public static int getChunksThreshold(int targetLevel) {
        return CHUNKS_THRESHOLDS[idx(targetLevel)];
    }

    public static int getAgeThreshold(int targetLevel) {
        return AGE_THRESHOLDS[idx(targetLevel)];
    }

    /**
     * Returns a boolean array (length = {@link #CRITERIA_COUNT}) showing which criteria
     * this dominion currently meets for advancing to {@code targetLevel}.
     * <p>
     * Index mapping: 0=Members, 1=Balance, 2=Farmland, 3=Livestock, 4=Chunks, 5=Age
     */
    public static boolean[] getCriteriaStatus(Dominion dominion, int targetLevel) {
        int i = idx(targetLevel);
        return new boolean[]{
                dominion.getMembers().size() >= MEMBERS_THRESHOLDS[i],
                dominion.getBalance() >= BALANCE_THRESHOLDS[i],
                dominion.getCachedFarmlandCount() >= FARMLAND_THRESHOLDS[i],
                dominion.getCachedLivestockCount() >= LIVESTOCK_THRESHOLDS[i],
                dominion.getChunks().size() >= CHUNKS_THRESHOLDS[i],
                getInGameYears(dominion) >= AGE_THRESHOLDS[i]
        };
    }

    /**
     * Counts how many of the 6 criteria are met for the given target level.
     */
    public static int countCriteriaMet(Dominion dominion, int targetLevel) {
        int count = 0;
        for (boolean b : getCriteriaStatus(dominion, targetLevel)) {
            if (b) {
                count++;
            }
        }
        return count;
    }

    /**
     * Evaluates what level this dominion should be at, checking from the highest level
     * downward. Returns 1 if no higher-level criteria are met.
     */
    public static int evaluateLevel(Dominion dominion) {
        for (int level = MAX_LEVEL; level >= 2; level--) {
            if (countCriteriaMet(dominion, level) >= CRITERIA_REQUIRED) {
                return level;
            }
        }
        return 1;
    }

    /**
     * Returns the total number of in-game days elapsed since a given Aranarth date,
     * measured from year 0 day 0.
     */
    public static long toTotalInGameDays(int year, Month month, int day) {
        long total = (long) year * DAYS_PER_YEAR;
        for (int i = 0; i < month.ordinal(); i++) {
            total += MONTH_DAYS[i];
        }
        total += day;
        return total;
    }

    /**
     * Returns the total in-game days for the current server date.
     * Call this when recording a dominion's founding date.
     */
    public static long getCurrentInGameDayTotal() {
        return toTotalInGameDays(AranarthUtils.getYear(), AranarthUtils.getMonth(), AranarthUtils.getDay());
    }

    /**
     * Converts a total-in-game-days value (as stored in foundedTimestamp) back into a
     * formatted calendar string, e.g. "4th of Florivór, 108". Returns null for legacy
     * dominions (foundedTimestamp == 0).
     */
    public static String formatFoundedDate(long totalDays) {
        if (totalDays == 0L) {
            return null;
        }
        int year = (int) (totalDays / DAYS_PER_YEAR);
        long remaining = totalDays % DAYS_PER_YEAR;
        if (remaining == 0) {
            year--;
            remaining = DAYS_PER_YEAR;
        }
        int monthIdx = 0;
        while (monthIdx < MONTH_DAYS.length - 1 && remaining > MONTH_DAYS[monthIdx]) {
            remaining -= MONTH_DAYS[monthIdx];
            monthIdx++;
        }
        Month month = Month.values()[monthIdx];
        int day = (int) remaining;
        String dayWithSuffix = DateUtils.getDayNumWithSuffix(day);
        String monthName = DateUtils.provideMonthName(month);
        return dayWithSuffix + " of " + monthName + ", " + year;
    }

    /**
     * Returns the number of complete in-game years this dominion has existed,
     * based on the custom Aranarth calendar.
     */
    public static long getInGameYears(Dominion dominion) {
        if (dominion.getFoundedTimestamp() == 0L) {
            return Long.MAX_VALUE;
        }
        long elapsed = getCurrentInGameDayTotal() - dominion.getFoundedTimestamp();
        return Math.max(0L, elapsed / DAYS_PER_YEAR);
    }

    /**
     * Returns a human-readable age string, e.g. "2.3 yrs" or "Ancient" for legacy dominions.
     */
    public static String getFormattedAge(Dominion dominion) {
        if (dominion.getFoundedTimestamp() == 0L) {
            return "Ancient";
        }
        long elapsed = getCurrentInGameDayTotal() - dominion.getFoundedTimestamp();
        double years = Math.max(0.0, elapsed / (double) DAYS_PER_YEAR);
        return String.format("%.1f", years);
    }

    /**
     * Computes each dominion's placement (1-indexed, 1 = best) in all 6 categories.
     *
     * @return Map from dominion UUID to int[6] placement array.
     */
    public static Map<UUID, int[]> computeAllPlacements() {
        List<Dominion> dominions = DominionUtils.getDominions();
        if (dominions == null || dominions.isEmpty()) {
            return Collections.emptyMap();
        }

        Map<UUID, int[]> placements = new HashMap<>();
        for (Dominion d : dominions) {
            placements.put(d.getId(), new int[CRITERIA_COUNT]);
        }

        // Higher value = better rank (rank 1)
        assignPlacements(placements, sortedBy(dominions, (a, b) ->
                        Integer.compare(b.getMembers().size(), a.getMembers().size())), 0,
                (a, b) -> a.getMembers().size() == b.getMembers().size());

        assignPlacements(placements, sortedBy(dominions, (a, b) ->
                        Double.compare(b.getBalance(), a.getBalance())), 1,
                (a, b) -> a.getBalance() == b.getBalance());

        assignPlacements(placements, sortedBy(dominions, (a, b) ->
                        Integer.compare(b.getCachedFarmlandCount(), a.getCachedFarmlandCount())), 2,
                (a, b) -> a.getCachedFarmlandCount() == b.getCachedFarmlandCount());

        assignPlacements(placements, sortedBy(dominions, (a, b) ->
                        Integer.compare(b.getCachedLivestockCount(), a.getCachedLivestockCount())), 3,
                (a, b) -> a.getCachedLivestockCount() == b.getCachedLivestockCount());

        assignPlacements(placements, sortedBy(dominions, (a, b) ->
                        Integer.compare(b.getChunks().size(), a.getChunks().size())), 4,
                (a, b) -> a.getChunks().size() == b.getChunks().size());

        assignPlacements(placements, sortedBy(dominions, (a, b) ->
                        Long.compare(getInGameYears(b), getInGameYears(a))), 5,
                (a, b) -> getInGameYears(a) == getInGameYears(b));

        return placements;
    }

    private static List<Dominion> sortedBy(List<Dominion> dominions, Comparator<Dominion> cmp) {
        List<Dominion> copy = new ArrayList<>(dominions);
        copy.sort(cmp);
        return copy;
    }

    @FunctionalInterface
    private interface TieChecker {
        boolean tied(Dominion a, Dominion b);
    }

    private static void assignPlacements(Map<UUID, int[]> placements, List<Dominion> sorted,
                                         int categoryIdx, TieChecker tieChecker) {
        int rank = 1;
        for (int i = 0; i < sorted.size(); i++) {
            if (i > 0 && !tieChecker.tied(sorted.get(i), sorted.get(i - 1))) {
                rank = i + 1;
            }
            placements.get(sorted.get(i).getId())[categoryIdx] = rank;
        }
    }

    /**
     * Returns dominions sorted by their overall leaderboard score (sum of all 6 placements,
     * lowest score = best). Tiebreaker: higher balance wins.
     */
    public static List<Dominion> getDominionsSortedByPlacement() {
        List<Dominion> dominions = DominionUtils.getDominions();
        if (dominions == null || dominions.isEmpty()) {
            return Collections.emptyList();
        }

        Map<UUID, int[]> placements = computeAllPlacements();
        Map<UUID, Integer> scores = new HashMap<>();
        for (Dominion d : dominions) {
            int[] p = placements.getOrDefault(d.getId(), new int[CRITERIA_COUNT]);
            int total = 0;
            for (int v : p) total += v;
            scores.put(d.getId(), total);
        }

        List<Dominion> sorted = new ArrayList<>(dominions);
        sorted.sort((a, b) -> {
            int sa = scores.getOrDefault(a.getId(), Integer.MAX_VALUE);
            int sb = scores.getOrDefault(b.getId(), Integer.MAX_VALUE);
            if (sa != sb) {
                return Integer.compare(sa, sb);
            }
            return Double.compare(b.getBalance(), a.getBalance()); // tiebreak: higher balance wins
        });
        return sorted;
    }

    /**
     * Computes the total placement score for a single dominion given a pre-computed placement map.
     */
    public static int getTotalScore(Dominion dominion, Map<UUID, int[]> placements) {
        int[] p = placements.getOrDefault(dominion.getId(), new int[CRITERIA_COUNT]);
        int total = 0;
        for (int v : p) total += v;
        return total;
    }

    /**
     * Computes the 1-indexed overall rank for a dominion in the sorted-by-placement list.
     */
    public static int getOverallRank(Dominion dominion, List<Dominion> sortedByPlacement) {
        for (int i = 0; i < sortedByPlacement.size(); i++) {
            if (sortedByPlacement.get(i).getId().equals(dominion.getId())) {
                return i + 1;
            }
        }
        return -1;
    }

    /**
     * Entry point for the 30-minute periodic scan. Must be called on the main thread.
     * <p>
     * 1. Counts livestock synchronously (requires main-thread entity access).
     * 2. Captures ChunkSnapshots synchronously, then counts farmland asynchronously.
     * 3. On completion, syncs back to the main thread and re-evaluates all dominion levels.
     */
    public static void runPeriodicScan() {
        List<Dominion> dominions = DominionUtils.getDominions();
        if (dominions == null || dominions.isEmpty()) {
            return;
        }

        for (Dominion dominion : dominions) {
            dominion.setCachedLivestockCount(scanLivestock(dominion));
        }

        // We capture world min/max heights at the same time (they can't be accessed off-thread).
        record SnapshotBundle(ChunkSnapshot snapshot, int minY, int maxY) {
        }
        Map<UUID, List<SnapshotBundle>> snapshotMap = new HashMap<>();
        for (Dominion dominion : dominions) {
            List<SnapshotBundle> bundles = new ArrayList<>();
            for (Chunk chunk : dominion.getChunks()) {
                int minY = chunk.getWorld().getMinHeight();
                int maxY = chunk.getWorld().getMaxHeight();
                bundles.add(new SnapshotBundle(chunk.getChunkSnapshot(), minY, maxY));
            }
            for (Outpost outpost : OutpostUtils.getDominionOutposts(dominion.getId())) {
                for (Chunk chunk : outpost.getChunks()) {
                    int minY = chunk.getWorld().getMinHeight();
                    int maxY = chunk.getWorld().getMaxHeight();
                    bundles.add(new SnapshotBundle(chunk.getChunkSnapshot(), minY, maxY));
                }
            }
            snapshotMap.put(dominion.getId(), bundles);
        }

        Bukkit.getScheduler().runTaskAsynchronously(AranarthCore.getInstance(), () -> {
            Map<UUID, Integer> farmlandCounts = new ConcurrentHashMap<>();
            for (Map.Entry<UUID, List<SnapshotBundle>> entry : snapshotMap.entrySet()) {
                int count = 0;
                for (SnapshotBundle bundle : entry.getValue()) {
                    count += countFarmlandInSnapshot(bundle.snapshot(), bundle.minY(), bundle.maxY());
                }
                farmlandCounts.put(entry.getKey(), count);
            }

            Bukkit.getScheduler().runTask(AranarthCore.getInstance(), () -> {
                for (Dominion dominion : dominions) {
                    Integer count = farmlandCounts.get(dominion.getId());
                    if (count != null) {
                        dominion.setCachedFarmlandCount(count);
                    }
                }
                reevaluateAllLevels(dominions);
                applyLevelPenalties(dominions);
            });
        });
    }

    private static int scanLivestock(Dominion dominion) {
        int count = 0;
        for (Chunk chunk : dominion.getChunks()) {
            for (Entity entity : chunk.getEntities()) {
                if (isCountedLivestock(entity)) {
                    count++;
                }
            }
        }
        for (Outpost outpost : OutpostUtils.getDominionOutposts(dominion.getId())) {
            for (Chunk chunk : outpost.getChunks()) {
                for (Entity entity : chunk.getEntities()) {
                    if (isCountedLivestock(entity)) {
                        count++;
                    }
                }
            }
        }
        return count;
    }

    public static boolean isCountedLivestock(Entity entity) {
        EntityType type = entity.getType();
        if (FARM_ANIMAL_TYPES.contains(type)) {
            return true;
        }
        if (TAMEABLE_LIVESTOCK_TYPES.contains(type) && entity instanceof Tameable tameable) {
            return tameable.isTamed();
        }
        return false;
    }

    private static int countFarmlandInSnapshot(ChunkSnapshot snapshot, int minY, int maxY) {
        int count = 0;
        for (int x = 0; x < 16; x++) {
            for (int z = 0; z < 16; z++) {
                for (int y = minY; y < maxY; y++) {
                    if (snapshot.getBlockType(x, y, z) == Material.FARMLAND) {
                        count++;
                    }
                }
            }
        }
        return count;
    }

    /**
     * Re-evaluates each dominion's level and notifies online members if it changed.
     * Also manages the {@code levelDropTimestamp} used by the penalty system:
     * <ul>
     *   <li>Level drops: starts the penalty clock if not already running.</li>
     *   <li>Level rises: clears the penalty clock (dominion recovered).</li>
     *   <li>Level unchanged: penalty clock is left as-is.</li>
     * </ul>
     * Must be called on the main thread.
     */
    private static void reevaluateAllLevels(List<Dominion> dominions) {
        for (Dominion dominion : dominions) {
            int newLevel = evaluateLevel(dominion);
            int oldLevel = dominion.getDominionLevel();

            if (newLevel < oldLevel) {
                // Start penalty clock if not already running
                if (dominion.getLevelDropTimestamp() == 0L) {
                    dominion.setLevelDropTimestamp(System.currentTimeMillis());
                }
                dominion.setDominionLevel(newLevel);
                notifyMembers(dominion, "&7Your dominion &e" + dominion.getName()
                        + " &7has &cdropped to &6Level " + newLevel + "&7!"
                        + " &cRegain 4/6 criteria within 1 in-game month to avoid penalties!");
            } else if (newLevel > oldLevel) {
                // Clear penalty clock
                if (dominion.getLevelDropTimestamp() != 0L) {
                    dominion.setLevelDropTimestamp(0L);
                    notifyMembers(dominion, "&7Your dominion &e" + dominion.getName()
                            + " &7has recovered - penalties have been lifted!");
                }
                dominion.setDominionLevel(newLevel);
                notifyMembersLevelUp(dominion, newLevel);
            }
            // Level unchanged: do not modify levelDropTimestamp
        }
    }

    /**
     * Re-evaluates a single dominion's level immediately.
     */
    public static void reevaluateDominion(Dominion dominion) {
        int newLevel = evaluateLevel(dominion);
        int oldLevel = dominion.getDominionLevel();

        if (newLevel < oldLevel) {
            if (dominion.getLevelDropTimestamp() == 0L) {
                dominion.setLevelDropTimestamp(System.currentTimeMillis());
            }
            dominion.setDominionLevel(newLevel);
            notifyMembers(dominion, "&cYour dominion &e" + dominion.getName()
                    + " &chas dropped to &6Level " + newLevel + "&7!"
                    + " &cRegain 4/6 criteria within 1 in-game month to avoid penalties!");
        } else if (newLevel > oldLevel) {
            if (dominion.getLevelDropTimestamp() != 0L) {
                dominion.setLevelDropTimestamp(0L);
                notifyMembers(dominion, "&7Your dominion is no longer subject to penalties");
            }
            dominion.setDominionLevel(newLevel);
            notifyMembersLevelUp(dominion, newLevel);
        }
    }

    private static void notifyMembers(Dominion dominion, String message) {
        for (UUID memberUuid : dominion.getMembers()) {
            var member = Bukkit.getPlayer(memberUuid);
            if (member != null && member.isOnline()) {
                member.sendMessage(ChatUtils.chatMessage(message));
            }
        }
    }

    private static void notifyMembersLevelUp(Dominion dominion, int newLevel) {
        for (UUID memberUuid : dominion.getMembers()) {
            Player member = Bukkit.getPlayer(memberUuid);
            if (member != null && member.isOnline()) {
                member.sendMessage(ChatUtils.chatMessage(
                        "&7Your Dominion has ranked up to &eLevel " + newLevel));
                member.playSound(member, Sound.ENTITY_PLAYER_LEVELUP, 1.0F, 1.5F);
            }
        }
    }

    /**
     * Applies level-drop penalties for all dominions that have been non-compliant.
     */
    public static void applyLevelPenalties(List<Dominion> dominions) {
        long now = System.currentTimeMillis();
        NumberFormat formatter = NumberFormat.getCurrencyInstance();

        for (Dominion dominion : dominions) {
            long dropTs = dominion.getLevelDropTimestamp();
            if (dropTs == 0L) {
                continue; // No penalty
            }
            if (now - dropTs < PENALTY_BUFFER_MS) {
                continue; // Still within buffer
            }
            if (DominionUtils.getConquerorOfDominion(dominion) != null) {
                continue; // Exempt if conquered
            }

            // Excess outpost penalty
            int allowedOutposts = OutpostUtils.allowedOutpostCount(dominion.getDominionLevel());
            List<Outpost> outposts = OutpostUtils.getDominionOutposts(dominion.getId());
            if (outposts.size() > allowedOutposts) {
                // Find the highest-index excess outpost
                Outpost excess = outposts.get(outposts.size() - 1);
                boolean disbanded = OutpostUtils.removePenaltyChunkFromOutpost(dominion, excess);
                if (disbanded) {
                    notifyMembers(dominion, "&cOutpost &e" + excess.getName()
                            + " &chas been lost due to your dominion failing to maintain &6Level "
                            + (excess.getOutpostIndex() + 1));
                } else {
                    notifyMembers(dominion, "&cA chunk has been lost from outpost &e" + excess.getName()
                            + " &cdue to your dominion failing to maintain &6Level "
                            + (excess.getOutpostIndex() + 1));
                }
                continue;
            }

            // Normal level-drop penalty
            int failedLevel = Math.min(dominion.getDominionLevel() + 1, MAX_LEVEL);
            int penaltyIdx = failedLevel - 2;
            double moneyPenalty = PENALTY_MONEY[penaltyIdx];
            int foodPenalty = PENALTY_FOOD_POWER[penaltyIdx];

            if (dominion.getBalance() >= moneyPenalty) {
                dominion.setBalance(dominion.getBalance() - moneyPenalty);
                DominionUtils.updateDominion(dominion);
                notifyMembers(dominion, "&e" + dominion.getName()
                        + " &chas been penalized &6" + formatter.format(moneyPenalty)
                        + " &cfor failing to maintain &6Level " + failedLevel);
                continue;
            }

            int totalFoodPower = DominionUtils.getTotalFoodPower(dominion);
            if (totalFoodPower > 0) {
                DominionUtils.consumeFood(dominion, Math.min(foodPenalty, totalFoodPower));
                notifyMembers(dominion, "&e" + dominion.getName()
                        + " &chad food reserves drained as a penalty for failing to maintain &6Level "
                        + failedLevel);
                continue;
            }

            // Work through outposts highest-index first, then main dominion.
            List<Outpost> currentOutposts = OutpostUtils.getDominionOutposts(dominion.getId());
            if (!currentOutposts.isEmpty()) {
                Outpost highestOutpost = currentOutposts.get(currentOutposts.size() - 1);
                boolean disbanded = OutpostUtils.removePenaltyChunkFromOutpost(dominion, highestOutpost);
                if (disbanded) {
                    notifyMembers(dominion, "&cOutpost &e" + highestOutpost.getName()
                            + " &chas been disbanded as a penalty for failing to maintain &6Level " + failedLevel);
                } else {
                    notifyMembers(dominion, "&cA chunk has been lost from outpost &e" + highestOutpost.getName()
                            + " &cas a penalty for failing to maintain &6Level " + failedLevel);
                }
                continue;
            }

            // No outposts left, unclaim from the main dominion
            if (dominion.getChunks().size() == 1) {
                // Only the home chunk remains, disband the dominion
                DominionUtils.updateDominionLeader(dominion, null, true);
                return;
            }

            Chunk homeChunk = dominion.getDominionHome().getChunk();
            Chunk toRemove = null;
            List<Chunk> chunks = dominion.getChunks();
            for (int i = chunks.size() - 1; i >= 0; i--) {
                Chunk candidate = chunks.get(i);
                if (candidate.equals(homeChunk)) {
                    continue;
                }
                if (DominionUtils.isAllClaimsConnectedAfterUnclaiming(dominion, candidate)) {
                    toRemove = candidate;
                    break;
                }
            }

            if (toRemove != null) {
                DominionUtils.removePenaltyChunk(dominion, toRemove);
                notifyMembers(dominion, "&cA chunk has been lost from &e" + dominion.getName()
                        + " &cas a penalty for failing to maintain &6Level " + failedLevel);
            }
        }
    }
}
