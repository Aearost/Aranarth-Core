package com.aearost.aranarthcore.utils;

import com.aearost.aranarthcore.items.key.KeyEpic;
import com.aearost.aranarthcore.items.key.KeyRare;
import com.aearost.aranarthcore.objects.AranarthPlayer;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.text.NumberFormat;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Centralized utility class for the 7-day login streak system.
 */
public class LoginStreakUtils {

    private static final HashMap<UUID, Integer> currentStreakDay = new HashMap<>();
    private static final HashMap<UUID, Long> lastClaimEpochDay = new HashMap<>();
    private static final ZoneId EST = ZoneId.of("America/New_York");
    private static final NumberFormat MONEY_FORMAT = NumberFormat.getInstance();

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
     * Returns the streak day the player is currently on (1-7).
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
        currentStreakDay.put(uuid, day == 7 ? 1 : day + 1);

        return true;
    }

    /**
     * Distributes the reward for the day to the player based on their rank.
     */
    private static void giveReward(Player player, AranarthPlayer aranarthPlayer, int day, int rank) {
        if (day == 4) {
            int count = getDiamondCount(rank);
            ItemStack diamonds = new ItemStack(Material.DIAMOND, count);
            Map<Integer, ItemStack> leftover = player.getInventory().addItem(diamonds);
            for (ItemStack overflow : leftover.values()) {
                player.getWorld().dropItemNaturally(player.getLocation(), overflow);
            }
            player.sendMessage(ChatUtils.chatMessage(
                    "&7Day &e" + day + " &7streak reward: &e" + count + " Diamonds"));

        } else if (day == 7) {
            ItemStack key = rank <= 2 ? new KeyRare().getItem() : new KeyEpic().getItem();
            Map<Integer, ItemStack> leftover = player.getInventory().addItem(key);
            for (ItemStack overflow : leftover.values()) {
                player.getWorld().dropItemNaturally(player.getLocation(), overflow);
            }
            String keyColor = rank <= 2 ? "&6" : "&3";
            String keyName = rank <= 2 ? "Rare Crate Key" : "Epic Crate Key";
            player.sendMessage(ChatUtils.chatMessage(
                    "&7Day &e" + day + " &7streak reward: " + keyColor + keyName));

        } else {
            double money = getMoneyReward(day, rank);
            aranarthPlayer.setBalance(aranarthPlayer.getBalance() + money);
            AranarthUtils.setPlayer(player.getUniqueId(), aranarthPlayer);
            player.sendMessage(ChatUtils.chatMessage(
                    "&7Day &e" + day + " &7streak reward: &6$" + MONEY_FORMAT.format(money)));
        }
    }

    // -------------------------------------------------------------------------
    // Reward calculations
    // -------------------------------------------------------------------------

    /**
     * Returns the money reward for the given money day (1, 2, 3, 5, or 6) and rank.
     * Scales linearly from rank 0 to rank 8 and is rounded to a clean interval.
     */
    public static double getMoneyReward(int day, int rank) {
        // 5 progression steps, one per money day
        double[] rank0Values = {50, 100, 150, 200, 250};
        double[] rank8Values = {2500, 4500, 6500, 8500, 10000};

        int index = switch (day) {
            case 1 -> 0;
            case 2 -> 1;
            case 3 -> 2;
            case 5 -> 3;
            case 6 -> 4;
            default -> 0;
        };

        double raw = rank0Values[index] + (rank / 8.0) * (rank8Values[index] - rank0Values[index]);
        return roundMoney((int) Math.round(raw));
    }

    /**
     * Rounds the calculated money reward based on the value of it.
     * @param value The pre-rounded value.
     * @return The rounded value.
     */
    private static double roundMoney(int value) {
        if (value < 100) return Math.round(value / 5.0) * 5;
        if (value < 500) return Math.round(value / 10.0) * 10;
        if (value < 10000) return Math.round(value / 100.0) * 100;
        return Math.round(value / 1000.0) * 1000;
    }

    /**
     * Returns the number of diamonds awarded on day 4 for the given rank.
     * Scales from 4 (rank 0) to 64 (rank 8).
     */
    public static int getDiamondCount(int rank) {
        return (int) Math.round(4 + (rank / 8.0) * 60);
    }

    /**
     * Returns a color-formatted display string for the day's reward (used in the GUI lore).
     */
    public static String getRewardDisplayName(int day, int rank) {
        if (day == 4) {
            return "&b" + getDiamondCount(rank) + " Diamonds";
        } else if (day == 7) {
            return rank <= 2 ? "&6Rare Crate Key" : "&5Epic Crate Key";
        } else {
            return "&6$" + MONEY_FORMAT.format(getMoneyReward(day, rank));
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
