package com.aearost.aranarthcore.event.listener;

import com.aearost.aranarthcore.AranarthCore;
import com.aearost.aranarthcore.event.world.ArenaItemDrops;
import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.ItemSpawnEvent;

/**
 * Centralizes all logic to be called by items spawning.
 */
public class ItemSpawnEventListener implements Listener {

    public ItemSpawnEventListener(AranarthCore plugin) {
        Bukkit.getPluginManager().registerEvents(this, plugin);
    }

    @EventHandler
    public void onItemSpawn(ItemSpawnEvent e) {
        if (e.getLocation().getWorld().getName().equalsIgnoreCase("arena")) {
            new ArenaItemDrops().execute(e);
        }
    }
}
