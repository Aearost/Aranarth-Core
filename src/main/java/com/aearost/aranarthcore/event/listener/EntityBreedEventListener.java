package com.aearost.aranarthcore.event.listener;

import com.aearost.aranarthcore.AranarthCore;
import com.aearost.aranarthcore.enums.Month;
import com.aearost.aranarthcore.event.mob.BabyMobSpawn;
import com.aearost.aranarthcore.utils.AranarthUtils;
import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityBreedEvent;

public class EntityBreedEventListener implements Listener {

    public EntityBreedEventListener(AranarthCore plugin) {
        Bukkit.getPluginManager().registerEvents(this, plugin);
    }

    /**
     * Centralizes all logic to be called by an entity being bred.
     */
    @EventHandler
    public void onEntityBreed(EntityBreedEvent e) {
        if (AranarthUtils.getMonth() == Month.CALORVOR) {
            new BabyMobSpawn().execute(e);
        }
    }
}
