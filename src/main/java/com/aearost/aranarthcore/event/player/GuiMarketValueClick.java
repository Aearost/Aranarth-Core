package com.aearost.aranarthcore.event.player;

import com.aearost.aranarthcore.gui.GuiMarketValue;
import com.aearost.aranarthcore.utils.AranarthUtils;
import com.aearost.aranarthcore.utils.TradeMarketUtils;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.List;

/**
 * Handles clicks in the market value GUI (view-only, search and navigation).
 */
public class GuiMarketValueClick {

    public void execute(InventoryClickEvent e) {
        if (e.getClickedInventory() == null) {
            return;
        }
        if (!(e.getWhoClicked() instanceof Player player)) {
            return;
        }

        e.setCancelled(true);

        if (e.getClickedInventory() != e.getView().getTopInventory()) {
            return;
        }

        int slot = e.getSlot();
        Inventory topInv = e.getView().getTopInventory();
        int currentPage = GuiMarketValue.playerPage.getOrDefault(player.getUniqueId(), 0);
        String filter = GuiMarketValue.getActiveFilter(player.getUniqueId());

        List<TradeMarketUtils.MarketEntry> entries = TradeMarketUtils.getAllEntries();
        if (filter != null && !filter.isEmpty()) {
            String lowerFilter = filter.toLowerCase();
            entries = entries.stream()
                    .filter(entry -> entry.displayName().toLowerCase().contains(lowerFilter)
                            || entry.key().toLowerCase().contains(lowerFilter))
                    .toList();
        }
        int totalPages = Math.max(1, (int) Math.ceil((double) entries.size() / GuiMarketValue.ITEMS_PER_PAGE));

        if (slot == GuiMarketValue.SLOT_SEARCH) {
            ItemStack clicked = e.getCurrentItem();
            if (clicked == null) {
                return;
            }
            if (clicked.getType() == Material.SPYGLASS) {
                GuiMarketValue.initiateSearch(player);
            } else if (clicked.getType() == Material.ARROW) {
                GuiMarketValue.clearFilter(player.getUniqueId());
                new GuiMarketValue(player, 0, null).populateInto(topInv);
            }
        } else if (slot == GuiMarketValue.SLOT_PREV) {
            if (currentPage > 0) {
                player.playSound(player, Sound.UI_BUTTON_CLICK, 0.25F, 1);
                new GuiMarketValue(player, currentPage - 1, filter).populateInto(topInv);
            }
        } else if (slot == GuiMarketValue.SLOT_NEXT) {
            if (currentPage < totalPages - 1) {
                player.playSound(player, Sound.UI_BUTTON_CLICK, 0.25F, 1);
                new GuiMarketValue(player, currentPage + 1, filter).populateInto(topInv);
            }
        } else if (slot == GuiMarketValue.SLOT_CLOSE) {
            player.playSound(player, Sound.UI_BUTTON_CLICK, 0.25F, 1);
            player.closeInventory();
        } else if (slot >= GuiMarketValue.ITEMS_START && slot <= GuiMarketValue.ITEMS_END) {
            AranarthUtils.playPingSound(player);
        }
    }
}
