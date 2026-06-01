package com.aearost.aranarthcore.event.player;

import com.aearost.aranarthcore.gui.GuiTopKills;
import com.aearost.aranarthcore.objects.AranarthPlayer;
import com.aearost.aranarthcore.utils.AranarthUtils;
import com.aearost.aranarthcore.utils.ChatUtils;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryType;

/**
 * Handles the logic for navigating the top kills GUI.
 */
public class GuiTopKillsClick {
	public void execute(InventoryClickEvent e) {
		if (ChatUtils.stripColorFormatting(e.getView().getTitle()).equals("Top Kills")) {
			// If the user did not click a slot
			if (e.getClickedInventory() == null) {
				return;
			}

			if (e.getWhoClicked() instanceof Player player) {
				AranarthPlayer aranarthPlayer = AranarthUtils.getPlayer(player.getUniqueId());

				if (e.getClickedInventory().getType() == InventoryType.CHEST) {
					e.setCancelled(true);
					// If they click Previous, bring them back to the previous page
					if (e.getSlot() == 45) {
						int currentPage = aranarthPlayer.getCurrentGuiPageNum();
						if (currentPage > 0) {
							player.playSound(player, Sound.UI_BUTTON_CLICK, 0.25F, 1);
							GuiTopKills.open(player, currentPage - 1);
						} else if (currentPage == 0) {
							int playerNum = AranarthUtils.getTopKills(player.getWorld()).size();
							int maxPages;
							// If the amount is a multiple of 45
							if (playerNum % 45 == 0) {
								maxPages = playerNum / 45;
							} else {
								maxPages = (int) (double) (playerNum / 45) + 1;
							}
							if (maxPages > 1) {
								player.playSound(player, Sound.UI_BUTTON_CLICK, 0.25F, 1);
								GuiTopKills.open(player, maxPages - 1);
							}
						}
					}
					// If they click Exit
					else if (e.getSlot() == 49) {
						player.closeInventory();
						player.playSound(player, Sound.UI_BUTTON_CLICK, 0.25F, 1);
					}
					// If they click Next
					else if (e.getSlot() == 53) {
						int playerNum = AranarthUtils.getTopKills(player.getWorld()).size();
						int currentPage = aranarthPlayer.getCurrentGuiPageNum();
						int maxPages;

						// If the amount is a multiple of 45
						if (playerNum % 45 == 0) {
							maxPages = playerNum / 45;
						} else {
							maxPages = (int) (double) (playerNum / 45) + 1;
						}
						player.playSound(player, Sound.UI_BUTTON_CLICK, 0.25F, 1);
						if (currentPage + 1 < maxPages) {
							GuiTopKills.open(player, currentPage + 1);
						} else {
							GuiTopKills.open(player, 0);
						}
					}
				}
			}
		}
	}

}
