package com.aearost.aranarthcore.event.player;

import com.aearost.aranarthcore.gui.GuiJobsJoin;
import com.aearost.aranarthcore.gui.GuiJobsLeave;
import com.aearost.aranarthcore.gui.GuiJobsStats;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;

public class GuiJobsClick {

    public void execute(InventoryClickEvent e) {
        e.setCancelled(true);
        if (!(e.getWhoClicked() instanceof Player player)) return;
        if (e.getClickedInventory() == null) return;
        if (!e.getClickedInventory().equals(e.getView().getTopInventory())) return;

        int slot = e.getRawSlot();

        if (slot == 11) {
            player.closeInventory();
            new GuiJobsJoin(player).openGui();
        } else if (slot == 13) {
            player.closeInventory();
            new GuiJobsStats(player).openGui();
        } else if (slot == 15) {
            player.closeInventory();
            new GuiJobsLeave(player).openGui();
        }
    }
}
