package com.aearost.aranarthcore.event.block;

import com.aearost.aranarthcore.gui.GuiFletchingTable;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerInteractEvent;

/**
 * Handles enabling the custom functionality involving the Fletching Table.
 */
public class FletchingTableClick {

    public void execute(PlayerInteractEvent e) {
        Block block = e.getClickedBlock();
        if (block.getType() == Material.FLETCHING_TABLE) {
            e.setCancelled(true);
            Player player = e.getPlayer();
            GuiFletchingTable gui = new GuiFletchingTable(player);
            gui.openGui();
        }
    }

}
