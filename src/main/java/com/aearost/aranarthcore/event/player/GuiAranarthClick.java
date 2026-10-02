package com.aearost.aranarthcore.event.player;

import com.aearost.aranarthcore.gui.GuiAranarth;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;

/**
 * Handles clicks in the Aranarth guide main menu GUI.
 */
public class GuiAranarthClick {

    public void execute(InventoryClickEvent e) {
        if (e.getClickedInventory() == null) {
            return;
        }
        e.setCancelled(true);

        Player player = (Player) e.getWhoClicked();
        int slot = e.getSlot();

        switch (slot) {
            case GuiAranarth.SLOT_RULES -> {
                player.closeInventory();
                player.performCommand("aranarth rules");
            }
            case GuiAranarth.SLOT_BENDING -> {
                player.closeInventory();
                player.performCommand("aranarth bending");
            }
            case GuiAranarth.SLOT_DOMINIONS -> {
                player.closeInventory();
                player.performCommand("aranarth dominions");
            }
            case GuiAranarth.SLOT_RANKS -> {
                player.closeInventory();
                player.performCommand("aranarth ranks");
            }
            case GuiAranarth.SLOT_ECONOMY -> {
                player.closeInventory();
                player.performCommand("aranarth economy");
            }
            case GuiAranarth.SLOT_CALENDAR -> {
                player.closeInventory();
                player.performCommand("aranarth calendar");
            }
            case GuiAranarth.SLOT_ARANARTHIUM -> {
                player.closeInventory();
                player.performCommand("aranarth aranarthium");
            }
            case GuiAranarth.SLOT_ESSENCES -> {
                player.closeInventory();
                player.performCommand("aranarth essences");
            }
            case GuiAranarth.SLOT_PERKS -> {
                player.closeInventory();
                player.performCommand("aranarth perks");
            }
            case GuiAranarth.SLOT_MECHANICS -> {
                player.closeInventory();
                player.performCommand("aranarth mechanics");
            }
            case GuiAranarth.SLOT_RECIPES -> {
                player.closeInventory();
                player.performCommand("aranarth recipes");
            }
        }
    }
}
