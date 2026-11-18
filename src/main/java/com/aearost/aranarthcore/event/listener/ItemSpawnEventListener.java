package com.aearost.aranarthcore.event.listener;

import com.aearost.aranarthcore.AranarthCore;
import com.aearost.aranarthcore.event.block.EssenceMagnetismBlockBreak;
import com.aearost.aranarthcore.event.block.TrialChamberLuck;
import com.aearost.aranarthcore.utils.CropUtils;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
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
        // Dropped sweet berries and glow berries are eaten twice as fast, like every other berry
        ItemStack berries = e.getEntity().getItemStack();
        if (berries.getType() == Material.SWEET_BERRIES || berries.getType() == Material.GLOW_BERRIES) {
            CropUtils.applyBerryEatSpeed(berries);
            e.getEntity().setItemStack(berries);
        }
        if (CropUtils.hasSeasonalLore(e.getEntity().getItemStack())) {
            CropUtils.updateSeedLore(e.getEntity().getItemStack(), e.getLocation().getWorld());
        }

        // Tag any item spawning at an active magnetism break location so it can be pulled later.
        String toolId = EssenceMagnetismBlockBreak.getToolIdForSpawn(e.getLocation());
        if (toolId != null) {
            e.getEntity().getPersistentDataContainer().set(MAGNETISM_TAG, PersistentDataType.STRING, toolId);
        }

        new TrialChamberLuck().execute(e);
    }
}
