package com.aearost.aranarthcore.event.listener;

import com.aearost.aranarthcore.AranarthCore;
import com.aearost.aranarthcore.event.block.*;
import com.aearost.aranarthcore.event.player.PlayerAutoReplenishSlot;
import com.aearost.aranarthcore.items.Homepad;
import com.aearost.aranarthcore.items.SugarcaneBlock;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.inventory.ItemStack;

/**
 * Centralizes all logic to be called by blocks being placed.
 */
public class BlockPlaceEventListener implements Listener {

    public BlockPlaceEventListener(AranarthCore plugin) {
        Bukkit.getPluginManager().registerEvents(this, plugin);
    }

    @EventHandler
    public void onBlockPlace(BlockPlaceEvent e) {
        ItemStack is = e.getItemInHand();

        // Skip auto-replenish of slot
        if (is.getType() == Material.HOPPER) {
            new HopperPlace().execute(e);
        } else if (is.isSimilar(new SugarcaneBlock().getItem())) {
            new SugarcaneBlockPlace().execute(e);
        } else {
            if (is.isSimilar((new Homepad().getItem()))) {
                new HomepadPlace().execute(e);
            } else if (is.getType() == Material.TORCHFLOWER) {
                new TorchflowerPlace().execute(e);
            } else if (is.getType() == Material.PITCHER_PLANT) {
                new PitcherPlantPlace().execute(e);
            }
            new RandomizerBlockPlace().execute(e);
            new PlayerAutoReplenishSlot().execute(e);
        }
    }
}
