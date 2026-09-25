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
            case GuiAranarth.SLOT_CALENDAR -> {
                player.closeInventory();
                player.performCommand("aranarth calendar");
            }
            case GuiAranarth.SLOT_ARANARTHIUM -> {
                player.closeInventory();
                player.performCommand("aranarth aranarthium");
            }
            case GuiAranarth.SLOT_INCANTATIONS -> {
                player.closeInventory();
                player.performCommand("aranarth incantations");
            }
            case GuiAranarth.SLOT_RULES -> {
                player.closeInventory();
                player.performCommand("aranarth rules");
            }
        }
    }
}
