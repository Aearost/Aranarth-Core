package com.aearost.aranarthcore.event.player;

import com.aearost.aranarthcore.gui.GuiMarketPrice;
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
 * Handles clicks in the market price GUI (view-only, search and navigation).
 */
public class GuiMarketPriceClick {

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
        int currentPage = GuiMarketPrice.playerPage.getOrDefault(player.getUniqueId(), 0);
        String filter = GuiMarketPrice.getActiveFilter(player.getUniqueId());

        List<TradeMarketUtils.MarketEntry> entries = TradeMarketUtils.getAllEntries();
        if (filter != null && !filter.isEmpty()) {
            String lowerFilter = filter.toLowerCase();
            entries = entries.stream()
                    .filter(entry -> entry.displayName().toLowerCase().contains(lowerFilter)
                            || entry.key().toLowerCase().contains(lowerFilter))
                    .toList();
        }
        int totalPages = Math.max(1, (int) Math.ceil((double) entries.size() / GuiMarketPrice.ITEMS_PER_PAGE));

        if (slot == GuiMarketPrice.SLOT_SEARCH) {
            ItemStack clicked = e.getCurrentItem();
            if (clicked == null) {
                return;
            }
            if (clicked.getType() == Material.SPYGLASS) {
                GuiMarketPrice.initiateSearch(player);
            } else if (clicked.getType() == Material.ARROW) {
                GuiMarketPrice.clearFilter(player.getUniqueId());
                new GuiMarketPrice(player, 0, null).populateInto(topInv);
            }
        } else if (slot == GuiMarketPrice.SLOT_PREV) {
            if (currentPage > 0) {
                player.playSound(player, Sound.UI_BUTTON_CLICK, 0.25F, 1);
                new GuiMarketPrice(player, currentPage - 1, filter).populateInto(topInv);
            }
        } else if (slot == GuiMarketPrice.SLOT_NEXT) {
            if (currentPage < totalPages - 1) {
                player.playSound(player, Sound.UI_BUTTON_CLICK, 0.25F, 1);
                new GuiMarketPrice(player, currentPage + 1, filter).populateInto(topInv);
            }
        } else if (slot == GuiMarketPrice.SLOT_CLOSE) {
            player.playSound(player, Sound.UI_BUTTON_CLICK, 0.25F, 1);
            player.closeInventory();
        } else if (slot >= GuiMarketPrice.ITEMS_START && slot <= GuiMarketPrice.ITEMS_END) {
            AranarthUtils.playPingSound(player);
        }
    }
}
