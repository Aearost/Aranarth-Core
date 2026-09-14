package com.aearost.aranarthcore.event.player;

import com.aearost.aranarthcore.gui.GuiMcstats;
import com.aearost.aranarthcore.utils.ChatUtils;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryType;

/**
 * Handles clicks in the /mcstats GUI.
 */
public class GuiMcstatsClick {

    public void execute(InventoryClickEvent e) {
        String title = ChatUtils.stripColorFormatting(e.getView().getTitle());
        if (!title.equals(com.aearost.aranarthcore.utils.Lang.get(GuiMcstats.TITLE_SELF_KEY)) && !title.endsWith(com.aearost.aranarthcore.utils.Lang.get(GuiMcstats.TITLE_SUFFIX_KEY))) {
            return;
        }

        if (e.getClickedInventory() == null) {
            return;
        }

        if (e.getWhoClicked() instanceof Player player) {
            if (e.getClickedInventory().getType() == InventoryType.CHEST) {
                e.setCancelled(true);
                if (e.getSlot() == 49) {
                    player.closeInventory();
                    player.playSound(player, Sound.UI_BUTTON_CLICK, 0.25F, 1);
                }
            }
        }
    }
}
