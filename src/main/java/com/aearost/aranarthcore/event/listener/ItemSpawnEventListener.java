package com.aearost.aranarthcore.event.listener;

import com.aearost.aranarthcore.AranarthCore;
import com.aearost.aranarthcore.event.block.IncantationMagnetismBlockBreak;
import com.aearost.aranarthcore.utils.CropUtils;
import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.ItemSpawnEvent;
import org.bukkit.persistence.PersistentDataType;

import static com.aearost.aranarthcore.objects.CustomKeys.MAGNETISM_TAG;

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

        // Tag any item spawning at an active magnetism break location so it can be pulled later.
        String toolId = IncantationMagnetismBlockBreak.getToolIdForSpawn(e.getLocation());
        if (toolId != null) {
            e.getEntity().getPersistentDataContainer().set(MAGNETISM_TAG, PersistentDataType.STRING, toolId);
        }
    }
}
