package com.aearost.aranarthcore.event.player;

import com.aearost.aranarthcore.gui.GuiCreativeItems;
import com.aearost.aranarthcore.utils.AranarthUtils;
import com.aearost.aranarthcore.utils.ChatUtils;
import com.aearost.aranarthcore.utils.Lang;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;

import java.util.HashMap;

/**
 * Handles clicks inside the Creative custom items GUI.
 */
public class GuiCreativeItemsClick {

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
        String filter = GuiCreativeItems.getActiveFilter(player.getUniqueId());

        // Search button
        if (slot == GuiCreativeItems.SLOT_SEARCH && clicked.getType() == Material.SPYGLASS) {
            GuiCreativeItems.initiateSearch(player);
            return;
        }

        // Back button (from search results)
        if (slot == GuiCreativeItems.SLOT_SEARCH && clicked.getType() == Material.ARROW) {
            GuiCreativeItems.clearFilter(player.getUniqueId());
            new GuiCreativeItems(player, 0).populateInto(e.getView().getTopInventory());
            return;
        }

        // Previous page
        if (slot == GuiCreativeItems.SLOT_PREVIOUS && clicked.getType() == Material.RED_WOOL) {
            if (currentPage > 0) {
                new GuiCreativeItems(player, currentPage - 1, filter).populateInto(e.getView().getTopInventory());
            }
            return;
        }

        // Next page
        if (slot == GuiCreativeItems.SLOT_NEXT && clicked.getType() == Material.LIME_WOOL) {
            new GuiCreativeItems(player, currentPage + 1, filter).populateInto(e.getView().getTopInventory());
            return;
        }

        ItemStack item = GuiCreativeItems.getItemAt(currentPage, filter, slot);
        if (item == null) {
            return;
        }

        // Re-checked on every take in case the player is no longer in creative mode in a Creative world
        if (!GuiCreativeItems.canUse(player)) {
            player.closeInventory();
            player.sendMessage(ChatUtils.chatMessage(Lang.getFor(player, "creative.items_creative_only")));
            return;
        }

        if (e.getClick() == ClickType.SHIFT_LEFT || e.getClick() == ClickType.SHIFT_RIGHT || e.getClick() == ClickType.MIDDLE) {
            item.setAmount(item.getMaxStackSize());
        } else {
            item.setAmount(1);
        }

        HashMap<Integer, ItemStack> leftover = player.getInventory().addItem(item);
        if (!leftover.isEmpty()) {
            player.sendMessage(ChatUtils.chatMessage(Lang.getFor(player, "creative.items_inventory_full")));
        }
    }
}
