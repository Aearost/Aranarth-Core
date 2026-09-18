package com.aearost.aranarthcore.event.player;

import com.aearost.aranarthcore.gui.GuiDominionResourceFilters;
import com.aearost.aranarthcore.gui.GuiDominionResources;
import com.aearost.aranarthcore.objects.Dominion;
import com.aearost.aranarthcore.objects.DominionResourceCategory;
import com.aearost.aranarthcore.utils.AranarthUtils;
import com.aearost.aranarthcore.utils.DominionUtils;
import com.aearost.aranarthcore.utils.Lang;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryType;

/**
 * Handles clicks inside the Dominion Resource Filters GUI.
 */
public class GuiDominionResourceFiltersClick {

    public void execute(InventoryClickEvent e) {
        e.setCancelled(true);

        if (e.getClickedInventory() == null || e.getClickedInventory().getType() != InventoryType.CHEST) {
            return;
        }

        Player player = (Player) e.getWhoClicked();
        int slot = e.getSlot();

        // Back button
        if (slot == 22 && e.getCurrentItem() != null && e.getCurrentItem().getType() == Material.BARRIER) {
            new GuiDominionResources(player).openGui();
            player.playSound(player, Sound.UI_BUTTON_CLICK, 0.5F, 1F);
            return;
        }

        // Category toggle slots
        int categoryIndex = GuiDominionResourceFilters.getCategorySlotIndex(slot);
        if (categoryIndex < 0) {
            return;
        }

        Dominion dominion = DominionUtils.getPlayerDominion(player.getUniqueId());
        if (dominion == null) {
            return;
        }

        if (!dominion.getLeader().equals(player.getUniqueId())) {
            player.sendMessage(com.aearost.aranarthcore.utils.ChatUtils.chatMessage(Lang.get("dominion.leader_only_permissions")));
            return;
        }

        DominionResourceCategory category = GuiDominionResourceFilters.getCategoryForSlotIndex(categoryIndex);
        if (category == null) {
            return;
        }

        boolean nowEnabled;
        if (dominion.getDisabledResourceCategories().contains(category)) {
            dominion.getDisabledResourceCategories().remove(category);
            nowEnabled = true;
        } else {
            dominion.getDisabledResourceCategories().add(category);
            nowEnabled = false;
        }
        DominionUtils.updateDominion(dominion);

        // Update the item in-place without closing the GUI
        e.getClickedInventory().setItem(slot, GuiDominionResourceFilters.buildCategoryItem(category, nowEnabled, player));
        player.updateInventory();
        AranarthUtils.playPingSound(player);
    }
}
