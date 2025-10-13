package com.aearost.aranarthcore.event.player;

import com.aearost.aranarthcore.enums.Pronouns;
import com.aearost.aranarthcore.gui.GuiRankup;
import com.aearost.aranarthcore.objects.AranarthPlayer;
import com.aearost.aranarthcore.utils.AranarthUtils;
import com.aearost.aranarthcore.utils.ChatUtils;
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

		Player player = (Player) e.getWhoClicked();
		AranarthPlayer aranarthPlayer = AranarthUtils.getPlayer(player.getUniqueId());
		int rank = aranarthPlayer.getRank();

		int slot = e.getSlot();

		// Saint ranks provide the URL to the server shop
		if (slot == 47) {
			player.sendMessage(ChatUtils.translateToColor("&3Saint I: &b________________"));
			player.closeInventory();
			return;
		} else if (slot == 49) {
			player.sendMessage(ChatUtils.translateToColor("&6Saint II: &e________________"));
			player.closeInventory();
			return;
		} else if (slot == 51) {
			player.sendMessage(ChatUtils.translateToColor("&4Saint III: &c________________"));
			player.closeInventory();
			return;
		}

		boolean isRankup = false;
		boolean isClickedRankSameAsCurrent = false;
		boolean isClickedRankLowerThanCurrent = false;
		boolean isClickedRankHigherThanCurrent = false;

		String[] maleRanks = new String[] { "&a&lPeasant", "&d&lEsquire", "&7&lKnight", "&5&lBaron", "&8&lCount",
				"&6&lDuke", "&b&lPrince", "&9&lKing", "&4&lEmperor" };
		String[] femaleRanks = new String[] { "&a&lPeasant", "&d&lEsquire", "&7&lKnight", "&5&lBaroness",
				"&8&lCountess", "&6&lDuchess", "&b&lPrincess", "&9&lQueen", "&4&lEmpress" };
		String[] rankupCosts = new String[] { "FREE", "$250", "$1,250", "$5,000", "$10,000", "$25,000", "$100,000",
				"$500,000", "$2,500,000" };
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
		String aOrAn = "a";
		if (slot == 12 || slot == 40) {
			aOrAn = "an";
		}

		// Feedback Messages and Functionality
		if (isClickedRankSameAsCurrent) {
			if (pronouns == Pronouns.MALE) {
				player.sendMessage(ChatUtils
						.translateToColor("&cYou are already " + aOrAn + " " + maleRanks[clickedRank] + "&c!"));
			} else {
				player.sendMessage(ChatUtils.translateToColor(
						"&cYou are already " + aOrAn + " " + femaleRanks[clickedRank] + "&c!"));
			}
			player.playSound(player.getLocation(), Sound.ENTITY_ENDER_EYE_DEATH, 0.5F, 0.5F);
			player.closeInventory();
		} else if (isClickedRankLowerThanCurrent) {
			if (pronouns == Pronouns.MALE) {
				player.sendMessage(ChatUtils.translateToColor(
						"&cYou cannot rank back down to " + aOrAn + " " + maleRanks[clickedRank] + "&c!"));
			} else {
				player.sendMessage(ChatUtils.translateToColor(
						"&cYou cannot rank back down to " + aOrAn + " " + femaleRanks[clickedRank] + "&c!"));
			}
			player.playSound(player.getLocation(), Sound.ENTITY_ENDER_EYE_DEATH, 0.5F, 0.5F);
			player.closeInventory();
		} else if (isClickedRankHigherThanCurrent) {
			if (pronouns == Pronouns.MALE) {
				player.sendMessage(
						ChatUtils.translateToColor("&cYou must rankup to " + maleRanks[rank + 1] + " &cfirst!"));
			} else {
				player.sendMessage(
						ChatUtils.translateToColor("&cYou must rankup to " + femaleRanks[rank + 1] + " &cfirst!"));
			}
			player.playSound(player.getLocation(), Sound.ENTITY_ENDER_EYE_DEATH, 0.8F, 0.5F);
			player.closeInventory();
		} else if (isRankup) {
			String rankupCost = rankupCosts[clickedRank];

			String rankName = "";
			if (pronouns == Pronouns.MALE) {
				rankName = maleRanks[clickedRank];
			} else {
				rankName = femaleRanks[clickedRank];
			}
			GuiRankup gui = new GuiRankup(player, rankName, rankupCost);
			gui.openGui();
		}
	}
}
