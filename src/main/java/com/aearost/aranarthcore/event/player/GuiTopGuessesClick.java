package com.aearost.aranarthcore.event.player;

import com.aearost.aranarthcore.gui.GuiTopGuesses;
import com.aearost.aranarthcore.objects.AranarthPlayer;
import com.aearost.aranarthcore.utils.AranarthUtils;
import com.aearost.aranarthcore.utils.ChatUtils;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryType;

/**
 * Handles the logic for navigating the top guesses GUI.
 */
public class GuiTopGuessesClick {

    public void execute(InventoryClickEvent e) {
        if (ChatUtils.stripColorFormatting(e.getView().getTitle()).equals("Top Guesses")) {
            if (e.getClickedInventory() == null) {
                return;
            }

            if (e.getWhoClicked() instanceof Player player) {
                AranarthPlayer aranarthPlayer = AranarthUtils.getPlayer(player.getUniqueId());

                if (e.getClickedInventory().getType() == InventoryType.CHEST) {
                    e.setCancelled(true);
                    if (e.getSlot() == 45) {
                        int currentPage = aranarthPlayer.getCurrentGuiPageNum();
                        if (currentPage > 0) {
                            player.playSound(player, Sound.UI_BUTTON_CLICK, 0.25F, 1);
                            GuiTopGuesses.open(player, currentPage - 1);
                        } else {
                            int playerNum = AranarthUtils.getTopGuesses().size();
                            int maxPages = playerNum % 45 == 0 ? playerNum / 45 : (playerNum / 45) + 1;
                            if (maxPages > 1) {
                                player.playSound(player, Sound.UI_BUTTON_CLICK, 0.25F, 1);
                                GuiTopGuesses.open(player, maxPages - 1);
                            }
                        }
                    } else if (e.getSlot() == 49) {
                        player.closeInventory();
                        player.playSound(player, Sound.UI_BUTTON_CLICK, 0.25F, 1);
                    } else if (e.getSlot() == 53) {
                        int playerNum = AranarthUtils.getTopGuesses().size();
                        int currentPage = aranarthPlayer.getCurrentGuiPageNum();
                        int maxPages = playerNum % 45 == 0 ? playerNum / 45 : (playerNum / 45) + 1;
                        player.playSound(player, Sound.UI_BUTTON_CLICK, 0.25F, 1);
                        if (currentPage + 1 < maxPages) {
                            GuiTopGuesses.open(player, currentPage + 1);
                        } else {
                            GuiTopGuesses.open(player, 0);
                        }
                    }
                }
            }
        }
    }
}
