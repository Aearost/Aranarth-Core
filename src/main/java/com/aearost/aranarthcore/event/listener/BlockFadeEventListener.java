package com.aearost.aranarthcore.event.listener;

import com.aearost.aranarthcore.AranarthCore;
import com.aearost.aranarthcore.event.block.CoralDry;
import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockFadeEvent;

/**
 * Centralizes all logic to be called by blocks fading.
 */
public class BlockFadeEventListener implements Listener {

    public BlockFadeEventListener(AranarthCore plugin) {
        Bukkit.getPluginManager().registerEvents(this, plugin);
    }

    @EventHandler
    public void onBlockFade(BlockFadeEvent e) {
        if (e.getBlock().getType().name().contains("CORAL")) {
            new CoralDry().execute(e);
        }
    }
}
