package com.aearost.aranarthcore.event.listener;

import com.aearost.aranarthcore.AranarthCore;
import com.aearost.aranarthcore.event.mob.WanderingTraderSpawnAnnounce;
import com.aearost.aranarthcore.event.world.CreeperExtraChargedSpawn;
import org.bukkit.Bukkit;
import org.bukkit.entity.EntityType;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

public class EntitySpawnEventListener implements Listener {

    public EntitySpawnEventListener(AranarthCore plugin) {
        Bukkit.getPluginManager().registerEvents(this, plugin);
    }

    /**
     * Centralizes all logic to be called by an entity spawning.
     */
    @EventHandler
    public void onEntitySpawn(org.bukkit.event.entity.EntitySpawnEvent e) {
        if (e.getEntityType() == EntityType.WANDERING_TRADER) {
            new WanderingTraderSpawnAnnounce().execute(e);
        } else if (e.getEntityType() == EntityType.CREEPER) {
            new CreeperExtraChargedSpawn().execute(e);
        }
    }
}
