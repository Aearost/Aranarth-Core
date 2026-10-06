package com.aearost.aranarthcore.event.player;

import com.aearost.aranarthcore.gui.GuiRecipeView;
import com.aearost.aranarthcore.gui.GuiRecipes;
import com.aearost.aranarthcore.objects.RecipeGuideEntry;
import com.aearost.aranarthcore.utils.AranarthUtils;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;

/**
 * Handles clicks inside the Recipes guide GUI.
 */
public class GuiRecipesClick {

    public void execute(InventoryClickEvent e) {
        e.setCancelled(true);
        if (!(e.getWhoClicked() instanceof Player player)) {
            return;
        }
        if (e.getClickedInventory() == null || e.getClickedInventory() != e.getView().getTopInventory()) {
            return;
        }
        ItemStack clicked = e.getCurrentItem();
        if (clicked == null || clicked.getType() == Material.AIR) {
            return;
        }

        int slot = e.getSlot();
        int currentPage = AranarthUtils.getPlayer(player.getUniqueId()).getCurrentGuiPageNum();
        String filter = GuiRecipes.getActiveFilter(player.getUniqueId());

        // Search button
        if (slot == GuiRecipes.SLOT_SEARCH && clicked.getType() == Material.SPYGLASS) {
            GuiRecipes.initiateSearch(player);
            return;
        }

        // Back button (from search results)
        if (slot == GuiRecipes.SLOT_SEARCH && clicked.getType() == Material.ARROW) {
            GuiRecipes.clearFilter(player.getUniqueId());
            new GuiRecipes(player, 0).populateInto(e.getView().getTopInventory());
            return;
        }

        // Previous page
        if (slot == GuiRecipes.SLOT_PREVIOUS && clicked.getType() == Material.RED_WOOL) {
            if (currentPage > 0) {
                new GuiRecipes(player, currentPage - 1, filter).populateInto(e.getView().getTopInventory());
            }
            return;
        }

        // Next page
        if (slot == GuiRecipes.SLOT_NEXT && clicked.getType() == Material.LIME_WOOL) {
            new GuiRecipes(player, currentPage + 1, filter).populateInto(e.getView().getTopInventory());
            return;
        }

        // Return to the Aranarth guide
        if (slot == GuiRecipes.SLOT_GUIDE) {
            GuiRecipes.clearFilter(player.getUniqueId());
            player.performCommand("aranarth");
            return;
        }

        RecipeGuideEntry entry = GuiRecipes.getEntryAt(currentPage, filter, slot);
        if (entry != null) {
            new GuiRecipeView(player, entry).openGui();
        }
    }
}
