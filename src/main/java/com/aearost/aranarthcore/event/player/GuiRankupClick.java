package com.aearost.aranarthcore.event.player;

import com.aearost.aranarthcore.AranarthCore;
import com.aearost.aranarthcore.gui.GuiHolder;
import com.aearost.aranarthcore.gui.GuiRankup;
import com.aearost.aranarthcore.network.NetworkManager;
import com.aearost.aranarthcore.objects.AranarthPlayer;
import com.aearost.aranarthcore.utils.*;
import com.gmail.nossr50.datatypes.player.McMMOPlayer;
import com.gmail.nossr50.util.EventUtils;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;

import java.text.NumberFormat;
import java.util.List;

/**
 * Handles logic for clicking in the Rank-Up GUI
 */
public class GuiRankupClick {
    public void execute(InventoryClickEvent e) {
        // If the user did not click a slot
        if (e.getClickedInventory() == null) {
            return;
        }

        e.setCancelled(true);

        // Only the buttons of the GUI itself count, never the player's own inventory
        if (e.getClickedInventory() != e.getView().getTopInventory()) {
            return;
        }
        GuiHolder holder = GuiHolder.of(e.getView().getTopInventory());
        if (holder == null || !(holder.getData() instanceof GuiRankup.RankupOffer(int fromRank, String nextRankId))) {
            return;
        }

        int slot = e.getSlot();
        // Rankup
        if (slot == 14) {
            Player player = (Player) e.getWhoClicked();
            AranarthPlayer aranarthPlayer = AranarthUtils.getPlayer(player.getUniqueId());

            // The rank must not have changed since the GUI was opened, otherwise a stale GUI could rank up twice
            int currentRank = aranarthPlayer.getRank();
            List<Double> rankupCosts = AranarthCore.getInstance().getConfig().getDoubleList("economy.rankup-costs");
            if (currentRank != fromRank || currentRank >= 8 || currentRank < 0 || currentRank >= rankupCosts.size()) {
                player.closeInventory();
                return;
            }
            // Index 0 of the config is the cost to go from rank 0 to rank 1
            double price = rankupCosts.get(currentRank);
            if (!Double.isFinite(price) || price < 0) {
                player.closeInventory();
                return;
            }

            double balance = aranarthPlayer.getBalance();
            if (balance >= price) {
                if (hasMinimumMcmmoTotal(player)) {
                    if (hasMinimumMcmmoPerSkill(player)) {

                        NumberFormat formatter = NumberFormat.getCurrencyInstance();
                        aranarthPlayer.setBalance(balance - price);
                        if (NetworkManager.isActive()) {
                            NetworkManager.getInstance().publishBalanceAdjust(player.getUniqueId(), -price);
                        }
                        aranarthPlayer.setRank(aranarthPlayer.getRank() + 1);
                        AranarthUtils.setPlayer(player.getUniqueId(), aranarthPlayer);
                        PersistenceUtils.saveAranarthPlayerImmediately(player.getUniqueId());

                        DiscordUtils.updateRank(player, aranarthPlayer.getRank(), true);

                        Lang.broadcast("rank.ranked_up", "player", aranarthPlayer.getNickname(), "article", Lang.key("rank.article_" + nextRankId), "rank", Lang.key("rank.title_" + nextRankId));
                        PermissionUtils.evaluatePlayerPermissions(player);
                        if (NetworkManager.isActive()) {
                            NetworkManager.getInstance().publishRankUpdate(player.getUniqueId(), aranarthPlayer.getRank(), aranarthPlayer.getCouncilRank(), aranarthPlayer.getSaintRank(), aranarthPlayer.getArchitectRank());
                        }
                        player.playSound(player, Sound.ENTITY_PLAYER_LEVELUP, 1.0F, 1.0F);
                        player.closeInventory();
                    } else {
                        player.sendMessage(ChatUtils.chatMessage(Lang.getFor(player, "rank.missing_mcmmo_category")));
                        player.closeInventory();
                    }
                } else {
                    player.sendMessage(ChatUtils.chatMessage(Lang.getFor(player, "rank.missing_mcmmo")));
                    player.closeInventory();
                }
            } else {
                player.sendMessage(ChatUtils.chatMessage(Lang.getFor(player, "rank.not_enough_money")));
                player.closeInventory();
            }
        }
        // Cancel
        else if (slot == 12) {
            Player player = (Player) e.getWhoClicked();
            player.playSound(player, Sound.ENTITY_ENDER_EYE_DEATH, 0.8F, 0.5F);
            e.getWhoClicked().closeInventory();
        }
    }

    /**
     * Determines if the player has the minimum amount of overall mcMMO levels to rank up.
     *
     * @param player The player attempting to rank up.
     * @return Confirmation if the player has the minimum amount of overall mcMMO levels to rank up.
     */
    private boolean hasMinimumMcmmoTotal(Player player) {
        McMMOPlayer mcMMOPlayer = EventUtils.getMcMMOPlayer(player);

        AranarthPlayer aranarthPlayer = AranarthUtils.getPlayer(player.getUniqueId());
        List<Integer> requirements = AranarthCore.getInstance().getConfig().getIntegerList("mcmmo.total-level-requirements");
        int rank = aranarthPlayer.getRank();
        int minimum = rank < requirements.size() ? requirements.get(rank) : requirements.get(requirements.size() - 1);

        return mcMMOPlayer.getPowerLevel() >= minimum;
    }

    /**
     * Determines if the player has the minimum sum of mcMMO levels per category to rank up.
     * Each of the three categories (Gathering, Combat, Miscellaneous) must independently meet the minimum.
     *
     * @param player The player attempting to rank up.
     * @return Confirmation if the player has the minimum sum of mcMMO levels per category to rank up.
     */
    private boolean hasMinimumMcmmoPerSkill(Player player) {
        McMMOPlayer mcMMOPlayer = EventUtils.getMcMMOPlayer(player);

        AranarthPlayer aranarthPlayer = AranarthUtils.getPlayer(player.getUniqueId());
        List<Integer> requirements = AranarthCore.getInstance().getConfig().getIntegerList("mcmmo.per-category-requirements");
        int rank = aranarthPlayer.getRank();
        int minimum = rank < requirements.size() ? requirements.get(rank) : requirements.get(requirements.size() - 1);

        if (minimum == 0) {
            return true;
        }

        for (int categoryTotal : AranarthUtils.getMcmmoCategoryTotals(mcMMOPlayer::getSkillLevel)) {
            if (categoryTotal < minimum) {
                return false;
            }
        }
        return true;
    }
}
