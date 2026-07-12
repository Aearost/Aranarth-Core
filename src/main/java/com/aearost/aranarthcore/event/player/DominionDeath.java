package com.aearost.aranarthcore.event.player;

import com.aearost.aranarthcore.objects.Dominion;
import com.aearost.aranarthcore.objects.DominionRank;
import com.aearost.aranarthcore.utils.AranarthUtils;
import com.aearost.aranarthcore.utils.ChatUtils;
import com.aearost.aranarthcore.utils.DominionUtils;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDeathEvent;

import java.util.Random;

/**
 * Handles logic to penalize a Dominion when one of its players dies.
 * Increased drawbacks if killed by a player
 * 50/50 chance of depleting either food or balance.
 * If neither are sufficient, unclaim a chunk.
 */
public class DominionDeath {

    // The percentage of the Dominion's balance lost on death (0.005 = 0.5%).
    // Consider reducing to 0.0025 (0.25%) if this proves too punishing over time.
    private static final double BALANCE_LOSS_PERCENT = 0.005;
    // Maximum money that can be lost to the balance percentage per death.
    private static final int BALANCE_LOSS_MAX = 50000;
    // Flat base money lost on death (floor when balance scaling is lower).
    private static final int BASE_MONEY_MOB = 100;
    private static final int BASE_MONEY_PVP = 500;

    public void execute(EntityDeathEvent e) {
        Player player = (Player) e.getEntity();
        if (player.getWorld().getName().startsWith("world") || AranarthUtils.isSmpWorld(player.getWorld().getName())
                || player.getWorld().getName().startsWith("resource")) {
            Dominion dominion = DominionUtils.getPlayerDominion(player.getUniqueId());
            if (dominion != null) {
                boolean wasKilledByPlayer = e.getDamageSource().getCausingEntity() != null && e.getDamageSource().getCausingEntity() instanceof Player;

                double roleMultiplier = getRoleMultiplier(dominion, player);
                int scaledAmount = (int) Math.min(dominion.getBalance() * BALANCE_LOSS_PERCENT * roleMultiplier, BALANCE_LOSS_MAX);
                int flatBase = wasKilledByPlayer ? BASE_MONEY_PVP : BASE_MONEY_MOB;
                int moneyToConsume = Math.max(flatBase, scaledAmount);

                int warMultiplier = 1;
                if (wasKilledByPlayer) {
                    Player killer = (Player) e.getDamageSource().getCausingEntity();
                    warMultiplier = DominionUtils.getDeathPenaltyMultiplier(player.getUniqueId(), killer.getUniqueId());
                    moneyToConsume *= warMultiplier;
                }

                // Prioritize food decrease
                if (new Random().nextBoolean()) {
                    boolean wasFoodDecreased = attemptFoodDecrease(dominion, wasKilledByPlayer, warMultiplier);
                    if (!wasFoodDecreased) {
                        int result = DominionUtils.consumeMoneyOrLand(dominion, moneyToConsume);
                        if (result == 1) {
                            displayMoneyLoss(dominion, wasKilledByPlayer, warMultiplier);
                        } else {
                            // Selling a chunk
                            if (result == 0) {
                                displayChunkLoss(dominion);
                            }
                            // The Dominion is disbanded
                            else {
                                DominionUtils.updateDominionLeader(dominion, null, true);
                            }
                        }
                    }
                }
                // Prioritize balance decrease
                else {
                    // Do not apply chunk selling logic if there is not enough money
                    if (dominion.getBalance() >= moneyToConsume) {
                        DominionUtils.consumeMoneyOrLand(dominion, moneyToConsume);
                        displayMoneyLoss(dominion, wasKilledByPlayer, warMultiplier);
                    }
                    // Prioritizing food decrease before chunk selling
                    else {
                        boolean wasFoodDecreased = attemptFoodDecrease(dominion, wasKilledByPlayer, warMultiplier);
                        if (!wasFoodDecreased) {
                            // Will only do a chunk decrease due to balance being exceeded
                            int result = DominionUtils.consumeMoneyOrLand(dominion, moneyToConsume);
                            if (result == 0) {
                                displayChunkLoss(dominion);
                            } else {
                                DominionUtils.updateDominionLeader(dominion, null, true);
                            }
                        }
                    }
                }
            }
        }
    }

    /**
     * Returns the role-based multiplier for balance loss on death.
     * NEWCOMER 0.25x / CITIZEN 0.75x / LIEUTENANT 1.5x / LEADER 2.0x
     */
    private double getRoleMultiplier(Dominion dominion, Player player) {
        DominionRank rank = dominion.getMemberRank(player.getUniqueId());
        if (rank == null) return 1.0;
        return switch (rank) {
            case NEWCOMER -> 0.25;
            case CITIZEN -> 0.75;
            case LIEUTENANT -> 1.5;
            case LEADER -> 2.0;
            default -> 1.0;
        };
    }

    /**
     * Handles logic when attempting to decrease a Dominion's food reserves.
     * @param dominion The Dominion.
     * @param wasKilledByPlayer Whether the death was caused by another player.
     * @param warMultiplier The war penalty multiplier (1 = normal, 3 = at war).
     * @return Whether the food reserves were depleted or not.
     */
    private boolean attemptFoodDecrease(Dominion dominion, boolean wasKilledByPlayer, int warMultiplier) {
        int totalFoodPower = DominionUtils.getTotalFoodPower(dominion);
        int powerBeingConsumed = 0;
        // Consume 100 power per day for <=25 chunks
        if (dominion.getChunks().size() <= 25) {
            powerBeingConsumed = 50;
        } else if (dominion.getChunks().size() <= 100) {
            powerBeingConsumed = 125;
        } else {
            powerBeingConsumed = 250;
        }

        if (wasKilledByPlayer) {
            // Equivalent of 2 days worth of food
            powerBeingConsumed *= 4;
        }

        powerBeingConsumed *= warMultiplier;

        if (totalFoodPower >= powerBeingConsumed) {
            DominionUtils.consumeFood(dominion, powerBeingConsumed);
            displayFoodLoss(dominion, wasKilledByPlayer, warMultiplier);
            return true;
        } else {
            return false;
        }
    }

    /**
     * Informs members of the input Dominion that food was lost by a player's death.
     * @param dominion The Dominion.
     * @param wasKilledByPlayer Whether the death was caused by another player.
     * @param warMultiplier The war penalty multiplier (1 = normal, 3 = at war).
     */
    private void displayFoodLoss(Dominion dominion, boolean wasKilledByPlayer, int warMultiplier) {
        String severelyKeyword = wasKilledByPlayer ? "severely " : "";
        String warSuffix = warMultiplier > 1 ? " &4(" + warMultiplier + "x war penalty)" : "";
        for (Player onlinePlayer : Bukkit.getOnlinePlayers()) {
            if (dominion.getMembers().contains(onlinePlayer.getUniqueId())) {
                onlinePlayer.sendMessage(ChatUtils.chatMessage(
                        "&cYour Dominion's food reserves have been " + severelyKeyword + "depleted" + warSuffix));
            }
        }
    }

    /**
     * Informs members of the input Dominion that money was lost by a player's death.
     * @param dominion The Dominion.
     * @param wasKilledByPlayer Whether the death was caused by another player.
     * @param warMultiplier The war penalty multiplier (1 = normal, 3 = at war).
     */
    private void displayMoneyLoss(Dominion dominion, boolean wasKilledByPlayer, int warMultiplier) {
        String severelyKeyword = wasKilledByPlayer ? "severely " : "";
        String warSuffix = warMultiplier > 1 ? " &4(" + warMultiplier + "x war penalty)" : "";
        for (Player onlinePlayer : Bukkit.getOnlinePlayers()) {
            if (dominion.getMembers().contains(onlinePlayer.getUniqueId())) {
                onlinePlayer.sendMessage(ChatUtils.chatMessage(
                        "&cYour Dominion's balance has been " + severelyKeyword + "depleted" + warSuffix));
            }
        }
    }

    /**
     * Informs members of the input Dominion that land was lost by a player's death.
     * @param dominion The Dominion.
     */
    private void displayChunkLoss(Dominion dominion) {
        for (Player onlinePlayer : Bukkit.getOnlinePlayers()) {
            if (dominion.getMembers().contains(onlinePlayer.getUniqueId())) {
                onlinePlayer.sendMessage(ChatUtils.chatMessage(
                        "&cYour Dominion has lost some of its land"));
            }
        }
    }

}
