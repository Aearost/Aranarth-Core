package com.aearost.aranarthcore.event.player;

import com.aearost.aranarthcore.enums.Pronouns;
import com.aearost.aranarthcore.gui.GuiRankup;
import com.aearost.aranarthcore.objects.AranarthPlayer;
import com.aearost.aranarthcore.utils.AranarthUtils;
import com.aearost.aranarthcore.utils.ChatUtils;
import com.aearost.aranarthcore.utils.Lang;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;

/**
 * Handles logic for clicking in the Ranks GUI
 */
public class GuiRanksClick {
	public void execute(InventoryClickEvent e) {
		// If the user did not click a slot
		if (e.getClickedInventory() == null) {
			return;
		}

		e.setCancelled(true);
		// Only the GUI's own buttons count, never the player's inventory
		if (e.getClickedInventory() != e.getView().getTopInventory()) {
		    return;
		}

		Player player = (Player) e.getWhoClicked();
		AranarthPlayer aranarthPlayer = AranarthUtils.getPlayer(player.getUniqueId());
		int rank = aranarthPlayer.getRank();

		int slot = e.getSlot();

		boolean isRankup = false;
		boolean isClickedRankSameAsCurrent = false;
		boolean isClickedRankLowerThanCurrent = false;
		boolean isClickedRankHigherThanCurrent = false;

		String[] rankupCosts = new String[] { Lang.getFor(player, "rank.cost_free"), "$1,000", "$5,000", "$15,000", "$50,000", "$125,000", "$500,000",
				"$2,500,000", "$10,000,000" };
		// Hardcoded indexes of all the upgradable ranks
		// Peasant, Esquire, Knight, Baron, Count, Duke, Prince, King, Emperor
		int[] positions = new int[] { 4, 12, 14, 20, 22, 24, 30, 32, 40 };
		int clickedRank = 0;

		// Cycles through the options of clicked ranks
		while (clickedRank < positions.length) {
			if (slot == positions[clickedRank]) {
				// If they clicked on the iterated rank
				if (rank == clickedRank) {
					isClickedRankSameAsCurrent = true;
					break;
				}
				// If they clicked on a previous rank
				else if (rank > clickedRank) {
					isClickedRankLowerThanCurrent = true;
					break;
				}
				// If they clicked on a rank after the rank above their own
				else if (clickedRank > rank + 1) {
					isClickedRankHigherThanCurrent = true;
					break;
				}
				// If they clicked on the next rank from their own (ranking up)
				else if (clickedRank == rank + 1) {
					isRankup = true;
					break;
				}

			}
			clickedRank++;
		}

		Pronouns pronouns = aranarthPlayer.getPronouns();

		// Feedback Messages and Functionality
		if (isClickedRankSameAsCurrent) {
			player.sendMessage(ChatUtils.chatMessage(Lang.getFor(player, "rank.already_rank",
					"article", Lang.key("rank.article_" + pronouns.getRankId(clickedRank)), "rank", Lang.key("rank.title_" + pronouns.getRankId(clickedRank)))));
			player.playSound(player, Sound.ENTITY_ENDER_EYE_DEATH, 0.5F, 0.5F);
			player.closeInventory();
		} else if (isClickedRankLowerThanCurrent) {
			player.sendMessage(ChatUtils.chatMessage(Lang.getFor(player, "rank.cannot_rank_down",
					"article", Lang.key("rank.article_" + pronouns.getRankId(clickedRank)), "rank", Lang.key("rank.title_" + pronouns.getRankId(clickedRank)))));
			player.playSound(player, Sound.ENTITY_ENDER_EYE_DEATH, 0.5F, 0.5F);
			player.closeInventory();
		} else if (isClickedRankHigherThanCurrent) {
			player.sendMessage(ChatUtils.chatMessage(Lang.getFor(player, "rank.must_rankup_first", "rank", Lang.key("rank.title_" + pronouns.getRankId(rank + 1)))));
			player.playSound(player, Sound.ENTITY_ENDER_EYE_DEATH, 0.8F, 0.5F);
			player.closeInventory();
		} else if (isRankup) {
			String rankupCost = rankupCosts[clickedRank];

			GuiRankup gui = new GuiRankup(player, pronouns.getRankId(clickedRank), rankupCost);
			gui.openGui();
		}
	}
}
