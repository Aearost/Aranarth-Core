package com.aearost.aranarthcore.utils;

import com.aearost.aranarthcore.items.key.KeyEpic;
import com.aearost.aranarthcore.items.key.KeyRare;
import com.aearost.aranarthcore.items.key.KeyVote;
import com.aearost.aranarthcore.objects.AranarthPlayer;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.text.NumberFormat;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Centralized utility class for the 28-day (4-week) login streak system.
 */
public class LoginStreakUtils {

    private static final HashMap<UUID, Integer> currentStreakDay = new HashMap<>();
    private static final HashMap<UUID, Long> lastClaimEpochDay = new HashMap<>();
    private static final ZoneId EST = ZoneId.of("America/New_York");
    private static final NumberFormat MONEY_FORMAT = NumberFormat.getInstance();

    // Per-rank money anchors for the 24 money days (index 0 = day 1, index 23 = day 27)
    private static final double[] START_AMOUNTS = {100, 175, 250, 375, 750, 1000, 1500, 2000, 2500};
    private static final double[] END_AMOUNTS   = {1000, 1750, 2500, 5000, 7500, 12500, 20000, 35000, 50000};

    static {
        MONEY_FORMAT.setGroupingUsed(true);
    }

    // -------------------------------------------------------------------------
    // Core state helpers
    // -------------------------------------------------------------------------

    private static long getTodayEpochDay() {
        return LocalDate.now(EST).toEpochDay();
    }

    /**
     * Returns the streak day the player is currently on (1-28).
     * Defaults to 1 if no data exists for the player.
     */
    public static int getStreakDay(UUID uuid) {
        return currentStreakDay.getOrDefault(uuid, 1);
    }

    /**
     * Returns the EST epoch day on which the player last claimed a streak reward.
     * Returns 0 if the player has never claimed.
     */
    public static long getLastClaimEpochDay(UUID uuid) {
        return lastClaimEpochDay.getOrDefault(uuid, 0L);
    }

    // -------------------------------------------------------------------------
    // Streak validation
    // -------------------------------------------------------------------------

    /**
     * Checks whether the player missed a day and resets their streak if so.
     */
    public static boolean ensureStreakValid(UUID uuid) {
        long lastClaim = getLastClaimEpochDay(uuid);
        if (lastClaim == 0) return false; // never claimed — already at day 1, no reset needed

        long today = getTodayEpochDay();
        int day = getStreakDay(uuid);

        // If the player is past day 1 and missed yesterday, reset
        if (day > 1 && lastClaim < today - 1) {
            currentStreakDay.put(uuid, 1);
            return true;
        }
        return false;
    }

    /**
     * Returns {@code true} if the player can claim their current streak day reward.
     * A player may only claim once per EST calendar day.
     */
    public static boolean canClaim(UUID uuid) {
        return getLastClaimEpochDay(uuid) != getTodayEpochDay();
    }

    // -------------------------------------------------------------------------
    // Claiming
    // -------------------------------------------------------------------------

    /**
     * Claims the current streak day reward for the player.
     * Validates the streak, distributes the reward, and advances the day counter.
     * After day 28 the streak cycles back to day 1.
     */
    public static boolean claimStreak(Player player) {
        UUID uuid = player.getUniqueId();

        ensureStreakValid(uuid);

        if (!canClaim(uuid)) return false;

        AranarthPlayer aranarthPlayer = AranarthUtils.getPlayer(uuid);
        int rank = aranarthPlayer.getRank();
        int day = getStreakDay(uuid);

        giveReward(player, aranarthPlayer, day, rank);

        lastClaimEpochDay.put(uuid, getTodayEpochDay());
        currentStreakDay.put(uuid, day == 28 ? 1 : day + 1);

        return true;
    }

    /**
     * Distributes the reward for the given day to the player.
     */
    private static void giveReward(Player player, AranarthPlayer aranarthPlayer, int day, int rank) {
        if (isKeyDay(day)) {
            giveKeyReward(player, day, rank);
        } else {
            double money = getMoneyReward(day, rank);
            aranarthPlayer.setBalance(aranarthPlayer.getBalance() + money);
            AranarthUtils.setPlayer(player.getUniqueId(), aranarthPlayer);
            player.sendMessage(ChatUtils.chatMessage(
                    "&7Day &e" + day + " &7streak reward: &6$" + MONEY_FORMAT.format(money)));
        }
    }

    private static boolean isKeyDay(int day) {
        return day == 7 || day == 14 || day == 21 || day == 28;
    }

    private static void giveKeyReward(Player player, int day, int rank) {
        ItemStack key;
        String keyColor;
        String keyName;
        int count;

        if (rank <= 2) {
            switch (day) {
                case 7  -> { key = new KeyVote().getItem(); keyColor = "&a"; keyName = "Vote Crate Key"; count = 3; }
                case 14 -> { key = new KeyRare().getItem(); keyColor = "&6"; keyName = "Rare Crate Key"; count = 1; }
                case 21 -> { key = new KeyRare().getItem(); keyColor = "&6"; keyName = "Rare Crate Key"; count = 2; }
                case 28 -> { key = new KeyEpic().getItem(); keyColor = "&3"; keyName = "Epic Crate Key"; count = 1; }
                default -> { return; }
            }
        } else if (rank <= 5) {
            switch (day) {
                case 7  -> { key = new KeyRare().getItem(); keyColor = "&6"; keyName = "Rare Crate Key"; count = 1; }
                case 14 -> { key = new KeyRare().getItem(); keyColor = "&6"; keyName = "Rare Crate Key"; count = 2; }
                case 21 -> { key = new KeyRare().getItem(); keyColor = "&6"; keyName = "Rare Crate Key"; count = 3; }
                case 28 -> { key = new KeyEpic().getItem(); keyColor = "&3"; keyName = "Epic Crate Key"; count = 1; }
                default -> { return; }
            }
        } else {
            switch (day) {
                case 7  -> { key = new KeyRare().getItem(); keyColor = "&6"; keyName = "Rare Crate Key"; count = 1; }
                case 14 -> { key = new KeyRare().getItem(); keyColor = "&6"; keyName = "Rare Crate Key"; count = 3; }
                case 21 -> { key = new KeyEpic().getItem(); keyColor = "&3"; keyName = "Epic Crate Key"; count = 1; }
                case 28 -> { key = new KeyEpic().getItem(); keyColor = "&3"; keyName = "Epic Crate Key"; count = 3; }
                default -> { return; }
            }
        }

        key.setAmount(count);
        Map<Integer, ItemStack> leftover = player.getInventory().addItem(key);
        for (ItemStack overflow : leftover.values()) {
            player.getWorld().dropItemNaturally(player.getLocation(), overflow);
        }
        String countStr = count == 1 ? "" : count + "x ";
        player.sendMessage(ChatUtils.chatMessage(
                "&7Day &e" + day + " &7streak reward: " + keyColor + countStr + keyName));
    }

    // -------------------------------------------------------------------------
    // Reward calculations
    // -------------------------------------------------------------------------

    /**
     * Returns the money reward for the given money day and rank.
     * Scales linearly from rank start to rank end amounts across the 24 money days.
     */
    public static double getMoneyReward(int day, int rank) {
        int index = getMoneyIndex(day);
        double raw = START_AMOUNTS[rank] + (index / 23.0) * (END_AMOUNTS[rank] - START_AMOUNTS[rank]);
        return roundMoney((int) Math.round(raw));
    }

    /**
     * Maps a money day (non-key day) to a 0-based index across the 24 money days.
     * Week 1 days 1-6 → 0-5, Week 2 days 8-13 → 6-11,
     * Week 3 days 15-20 → 12-17, Week 4 days 22-27 → 18-23.
     */
    private static int getMoneyIndex(int day) {
        if (day <= 6)  return day - 1;
        if (day <= 13) return day - 2;
        if (day <= 20) return day - 3;
        return day - 4;
    }

    /**
     * Rounds the calculated money reward to a clean interval based on magnitude.
     */
    private static double roundMoney(int value) {
        if (value < 100)    return Math.round(value / 5.0) * 5;
        if (value < 500)    return Math.round(value / 10.0) * 10;
        if (value < 10000)  return Math.round(value / 100.0) * 100;
        return Math.round(value / 1000.0) * 1000;
    }

    /**
     * Returns a color-formatted display string for the day's reward (used in the GUI lore).
     */
    public static String getRewardDisplayName(int day, int rank) {
        if (isKeyDay(day)) {
            return getKeyDisplayName(day, rank);
        }
        return "&6$" + MONEY_FORMAT.format(getMoneyReward(day, rank));
    }

    private static String getKeyDisplayName(int day, int rank) {
        if (rank <= 2) {
            return switch (day) {
                case 7  -> "&a3x Vote Crate Key";
                case 14 -> "&61x Rare Crate Key";
                case 21 -> "&62x Rare Crate Key";
                case 28 -> "&31x Epic Crate Key";
                default -> "";
            };
        } else if (rank <= 5) {
            return switch (day) {
                case 7  -> "&61x Rare Crate Key";
                case 14 -> "&62x Rare Crate Key";
                case 21 -> "&63x Rare Crate Key";
                case 28 -> "&31x Epic Crate Key";
                default -> "";
            };
        } else {
            return switch (day) {
                case 7  -> "&61x Rare Crate Key";
                case 14 -> "&63x Rare Crate Key";
                case 21 -> "&31x Epic Crate Key";
                case 28 -> "&33x Epic Crate Key";
                default -> "";
            };
        }
    }

    // -------------------------------------------------------------------------
    // Persistence support
    // -------------------------------------------------------------------------

    public static HashMap<UUID, Integer> getCurrentStreakDayMap() {
        return currentStreakDay;
    }

    public static HashMap<UUID, Long> getLastClaimEpochDayMap() {
        return lastClaimEpochDay;
    }

    public static void setStreakDay(UUID uuid, int day) {
        currentStreakDay.put(uuid, day);
    }

    public static void setLastClaimEpochDay(UUID uuid, long epochDay) {
        lastClaimEpochDay.put(uuid, epochDay);
    }
}
