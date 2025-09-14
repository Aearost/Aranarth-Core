package com.aearost.aranarthcore.event.listener;

import com.aearost.aranarthcore.AranarthCore;
import com.aearost.aranarthcore.event.world.StashenledaPathSnowPrevent;
import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockFormEvent;

/**
 * Centralizes all logic to be called by blocks forming.
 */
public class BlockFormEventListener implements Listener {

    public BlockFormEventListener(AranarthCore plugin) {
        Bukkit.getPluginManager().registerEvents(this, plugin);
    }

    @EventHandler
    public void onBlockForm(BlockFormEvent e) {
        new StashenledaPathSnowPrevent().execute(e);
    }
}
