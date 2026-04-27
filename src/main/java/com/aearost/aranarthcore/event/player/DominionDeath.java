package com.aearost.aranarthcore.event.player;

import com.aearost.aranarthcore.objects.Dominion;
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

    public void execute(EntityDeathEvent e) {
        Player player = (Player) e.getEntity();
        if (player.getWorld().getName().startsWith("world") || player.getWorld().getName().startsWith("smp")
                || player.getWorld().getName().startsWith("resource")) {
            Dominion dominion = DominionUtils.getPlayerDominion(player.getUniqueId());
            if (dominion != null) {
                boolean wasKilledByPlayer = e.getDamageSource().getCausingEntity() != null && e.getDamageSource().getCausingEntity() instanceof Player;
                int moneyToConsume = 100;
                if (wasKilledByPlayer) {
                    moneyToConsume = 500;
                }

                // Prioritize food decrease
                if (new Random().nextBoolean()) {
                    boolean wasFoodDecreased = attemptFoodDecrease(dominion, wasKilledByPlayer);
                    if (!wasFoodDecreased) {
                        int result = DominionUtils.consumeMoneyOrLand(dominion, moneyToConsume);
                        if (result == 1) {
                            displayMoneyLoss(dominion, wasKilledByPlayer);
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
                        displayMoneyLoss(dominion, wasKilledByPlayer);
                    }
                    // Prioritizing food decrease before chunk selling
                    else {
                        boolean wasFoodDecreased = attemptFoodDecrease(dominion, wasKilledByPlayer);
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
     * Handles logic when attempting to decrease a Dominion's food reserves.
     * @param dominion The Dominion.
     * @param wasKilledByPlayer Whether the death was caused by another player.
     * @return Whether the food reserves were depleted or not.
     */
    private boolean attemptFoodDecrease(Dominion dominion, boolean wasKilledByPlayer) {
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

        if (totalFoodPower >= powerBeingConsumed) {
            DominionUtils.consumeFood(dominion, powerBeingConsumed);
            displayFoodLoss(dominion, wasKilledByPlayer);
            return true;
        } else {
            return false;
        }
    }

    /**
     * Informs members of the input Dominion that money was lost by a player's death.
     * @param dominion The Dominion.
     * @param wasKilledByPlayer Whether the death was caused by another player.
     */
    private void displayFoodLoss(Dominion dominion, boolean wasKilledByPlayer) {
        String severelyKeyword = wasKilledByPlayer ? "severely " : "";
        for (Player onlinePlayer : Bukkit.getOnlinePlayers()) {
            if (dominion.getMembers().contains(onlinePlayer.getUniqueId())) {
                onlinePlayer.sendMessage(ChatUtils.chatMessage(
                        "&cYour Dominion's food reserves have been " + severelyKeyword + "depleted"));
            }
        }
    }

    /**
     * Informs members of the input Dominion that money was lost by a player's death.
     * @param dominion The Dominion.
     * @param wasKilledByPlayer Whether the death was caused by another player.
     */
    private void displayMoneyLoss(Dominion dominion, boolean wasKilledByPlayer) {
        String severelyKeyword = wasKilledByPlayer ? "severely " : "";
        for (Player onlinePlayer : Bukkit.getOnlinePlayers()) {
            if (dominion.getMembers().contains(onlinePlayer.getUniqueId())) {
                onlinePlayer.sendMessage(ChatUtils.chatMessage(
                        "&cYour Dominion's balance has been " + severelyKeyword + "depleted"));
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
