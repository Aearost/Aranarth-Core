package com.aearost.aranarthcore.event.listener;

import com.aearost.aranarthcore.AranarthCore;
import com.aearost.aranarthcore.enums.Month;
import com.aearost.aranarthcore.event.block.*;
import com.aearost.aranarthcore.event.player.HomepadBreak;
import com.aearost.aranarthcore.event.player.PlayerShopDestroy;
import com.aearost.aranarthcore.event.world.ArenaBlockBreak;
import com.aearost.aranarthcore.utils.AranarthUtils;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;

/**
 * Centralizes all logic to be called by blocks being broken.
 */
public class BlockBreakEventListener implements Listener {

    public BlockBreakEventListener(AranarthCore plugin) {
        Bukkit.getPluginManager().registerEvents(this, plugin);
    }

    @EventHandler
    public void onBlockBreak(BlockBreakEvent e) {
        Material type = e.getBlock().getType();
        if (type == Material.HEAVY_WEIGHTED_PRESSURE_PLATE) {
            new HomepadBreak().execute(e);
        } else if (type == Material.BUDDING_AMETHYST) {
            new BuddingAmethystBreak().execute(e);
        } else if (AranarthUtils.isBlockCrop(e.getBlock().getType())) {
            new CropHarvest().execute(e);
        } else if (type == Material.TORCHFLOWER || type == Material.TORCHFLOWER_CROP) {
            new TorchflowerBreak().execute(e);
        } else if (type == Material.PITCHER_PLANT || type == Material.PITCHER_CROP) {
            new PitcherPlantBreak().execute(e);
        } else if (e.getBlock().getWorld().getName().equalsIgnoreCase("arena")) {
            new ArenaBlockBreak().execute(e);
        } else {
            if (e.getBlock().getType().name().endsWith("_ORE")) {
                new OreBreak().execute(e);
            }
            new PlayerShopDestroy().execute(e);
        }

        if (AranarthUtils.getMonth() == Month.FOLLIVOR) {
            new LogExtraDrops().execute(e);
        }
    }
}
