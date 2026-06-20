package com.aearost.aranarthcore.event.listener;

import com.aearost.aranarthcore.AranarthCore;
import com.aearost.aranarthcore.objects.Dominion;
import com.aearost.aranarthcore.utils.DominionLevelUtils;
import com.aearost.aranarthcore.utils.DominionUtils;
import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityTameEvent;

public class EntityTameEventListener implements Listener {

    public EntityTameEventListener(AranarthCore plugin) {
        Bukkit.getPluginManager().registerEvents(this, plugin);
    }

    /**
     * Increments the owning dominion's cached livestock count when a tameable mob is tamed.
     */
    @EventHandler
    public void onEntityTame(EntityTameEvent e) {
        if (DominionLevelUtils.isCountedLivestock(e.getEntity())) {
            Dominion dominion = DominionUtils.getDominionOfChunkAnywhere(e.getEntity().getLocation().getChunk());
            if (dominion != null) {
                dominion.setCachedLivestockCount(dominion.getCachedLivestockCount() + 1);
                DominionLevelUtils.reevaluateDominion(dominion);
            }
        }
    }
}
