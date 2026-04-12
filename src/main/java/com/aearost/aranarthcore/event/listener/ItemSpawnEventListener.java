package com.aearost.aranarthcore.event.listener;

import com.aearost.aranarthcore.AranarthCore;
import com.aearost.aranarthcore.utils.CropUtils;
import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.ItemSpawnEvent;

/**
 * Centralizes all logic to be called by an item being spawned.
 */
public class ItemSpawnEventListener implements Listener {

    public ItemSpawnEventListener(AranarthCore plugin) {
        Bukkit.getPluginManager().registerEvents(this, plugin);
    }

    @EventHandler
    public void onItemSpawn(ItemSpawnEvent e) {
        if (CropUtils.isCropSeed(e.getEntity().getItemStack().getType())) {
            CropUtils.updateSeedLore(e.getEntity().getItemStack(), e.getLocation().getWorld());
        }
    }
}
