package com.aearost.aranarthcore.event.player;

import com.aearost.aranarthcore.gui.GuiRecipeView;
import com.aearost.aranarthcore.gui.GuiRecipes;
import com.aearost.aranarthcore.utils.AranarthUtils;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;

/**
 * Handles clicks inside the Recipe Viewer GUI.
 */
public class GuiRecipeViewClick {

    public void execute(InventoryClickEvent e) {
        e.setCancelled(true);
        if (!(e.getWhoClicked() instanceof Player player)) {
            return;
        }
        if (e.getClickedInventory() == null || e.getClickedInventory() != e.getView().getTopInventory()) {
            return;
        }

        // Return to the recipe list on the same page and search
        if (e.getSlot() == GuiRecipeView.SLOT_BACK) {
            int currentPage = AranarthUtils.getPlayer(player.getUniqueId()).getCurrentGuiPageNum();
            String filter = GuiRecipes.getActiveFilter(player.getUniqueId());
            new GuiRecipes(player, currentPage, filter).openGui();
        }
    }
}
