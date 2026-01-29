package com.aearost.aranarthcore.event.listener;

import com.aearost.aranarthcore.AranarthCore;
import com.aearost.aranarthcore.event.block.AncientPlantGrow;
import com.aearost.aranarthcore.event.block.CropGrowBoost;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockGrowEvent;

/**
 * Centralizes all logic to be called by blocks growing.
 */
public class BlockGrowEventListener implements Listener {

    public BlockGrowEventListener(AranarthCore plugin) {
        Bukkit.getPluginManager().registerEvents(this, plugin);
    }

    @EventHandler
    public void onBlockGrow(BlockGrowEvent e) {
        Material type = e.getBlock().getType();
        if (e.getBlock().getType() == Material.TORCHFLOWER_CROP
                || e.getBlock().getType() == Material.PITCHER_CROP) {
            new AncientPlantGrow().execute(e);
        } else {
            new CropGrowBoost().execute(e);
        }
    }
}
