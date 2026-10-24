package com.aearost.aranarthcore.event.player;

import com.aearost.aranarthcore.gui.GuiVoteTop;
import com.aearost.aranarthcore.objects.AranarthPlayer;
import com.aearost.aranarthcore.utils.AranarthUtils;
import com.aearost.aranarthcore.utils.ChatUtils;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryType;

/**
 * Handles the logic for navigating the top voters GUI.
 */
public class GuiVoteTopClick {

    public void execute(InventoryClickEvent e) {
        if (e.getClickedInventory() == null) {
            return;
        }

        if (e.getWhoClicked() instanceof Player player) {
            String title = ChatUtils.stripColorFormatting(e.getView().getTitle());
            if (!GuiVoteTop.isVoteTopTitle(player, title)) {
                return;
            }
            AranarthPlayer aranarthPlayer = AranarthUtils.getPlayer(player.getUniqueId());

            if (e.getClickedInventory().getType() == InventoryType.CHEST) {
                e.setCancelled(true);

                // The filter is stored when the GUI opens, as the title is localized
                GuiVoteTop.VoteTopFilter filter = GuiVoteTop.getOpenFilter(player.getUniqueId());
                final Integer finalYear = filter.year();
                final Integer finalMonth = filter.month();

                // Previous
                if (e.getSlot() == 45) {
                    int currentPage = aranarthPlayer.getCurrentGuiPageNum();
                    if (currentPage > 0) {
                        player.playSound(player, Sound.UI_BUTTON_CLICK, 0.25F, 1);
                        GuiVoteTop.open(player, finalYear, finalMonth, currentPage - 1);
                    }
                }
                // Exit
                else if (e.getSlot() == 49) {
                    player.closeInventory();
                    player.playSound(player, Sound.UI_BUTTON_CLICK, 0.25F, 1);
                }
                // Next
                else if (e.getSlot() == 53) {
                    int currentPage = aranarthPlayer.getCurrentGuiPageNum();
                    player.playSound(player, Sound.UI_BUTTON_CLICK, 0.25F, 1);
                    GuiVoteTop.open(player, finalYear, finalMonth, currentPage + 1);
                }
            }
        }
    }
}
