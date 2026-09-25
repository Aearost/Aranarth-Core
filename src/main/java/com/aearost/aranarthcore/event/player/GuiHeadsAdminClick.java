package com.aearost.aranarthcore.event.player;

import com.aearost.aranarthcore.gui.GuiHeads;
import com.aearost.aranarthcore.objects.HeadEntry;
import com.aearost.aranarthcore.utils.AranarthUtils;
import com.aearost.aranarthcore.utils.HeadsDatabaseManager;
import com.aearost.aranarthcore.utils.MobHeadUtils;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.HashMap;
import java.util.List;

/**
 * Handles clicks in the admin heads GUI (free, no exchange required).
 */
public class GuiHeadsAdminClick {

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
        int currentPage = GuiHeads.playerPage.getOrDefault(player.getUniqueId(), 0);
        String filter = GuiHeads.getActiveFilter(player.getUniqueId());

        List<HeadEntry> heads = HeadsDatabaseManager.getExchangeableHeads();
        if (filter != null && !filter.isEmpty()) {
            String lowerFilter = filter.toLowerCase();
            heads = heads.stream()
                    .filter(h -> h.name().toLowerCase().contains(lowerFilter))
                    .toList();
        }
        int totalPages = Math.max(1, (int) Math.ceil((double) heads.size() / GuiHeads.HEADS_PER_PAGE));

        if (slot == GuiHeads.SLOT_SEARCH) {
            ItemStack clicked = e.getCurrentItem();
            if (clicked == null) {
                return;
            }
            if (clicked.getType() == Material.SPYGLASS) {
                GuiHeads.initiateSearch(player);
            } else if (clicked.getType() == Material.ARROW) {
                GuiHeads.clearFilter(player.getUniqueId());
                new GuiHeads(player, 0, true).populateInto(topInv);
            }
        } else if (slot == GuiHeads.SLOT_PREV) {
            if (currentPage > 0) {
                player.playSound(player, Sound.UI_BUTTON_CLICK, 0.25F, 1);
                new GuiHeads(player, currentPage - 1, true, filter).populateInto(topInv);
            }
        } else if (slot == GuiHeads.SLOT_NEXT) {
            if (currentPage < totalPages - 1) {
                player.playSound(player, Sound.UI_BUTTON_CLICK, 0.25F, 1);
                new GuiHeads(player, currentPage + 1, true, filter).populateInto(topInv);
            }
        } else if (slot == GuiHeads.SLOT_CLOSE) {
            player.closeInventory();
            player.playSound(player, Sound.UI_BUTTON_CLICK, 0.25F, 1);
        } else if (slot >= GuiHeads.HEADS_START && slot <= GuiHeads.HEADS_END) {
            int index = currentPage * GuiHeads.HEADS_PER_PAGE + (slot - GuiHeads.HEADS_START);
            if (index < 0 || index >= heads.size()) {
                return;
            }

            HeadEntry entry = heads.get(index);
            ItemStack head = MobHeadUtils.createCustomHead(entry.texture(), "&f" + entry.name());
            HashMap<Integer, ItemStack> leftover = player.getInventory().addItem(head);
            for (ItemStack drop : leftover.values()) {
                player.getWorld().dropItemNaturally(player.getLocation(), drop);
            }
            AranarthUtils.playPingSound(player);
        }
    }
}
