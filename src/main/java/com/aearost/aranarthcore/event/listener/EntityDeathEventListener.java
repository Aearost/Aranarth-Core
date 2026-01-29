package com.aearost.aranarthcore.event.listener;

import com.aearost.aranarthcore.AranarthCore;
import com.aearost.aranarthcore.enums.Month;
import com.aearost.aranarthcore.event.mob.FaunivorExtraDeathDrops;
import com.aearost.aranarthcore.event.mob.GoatDeath;
import com.aearost.aranarthcore.event.mob.WanderingTraderDeath;
import com.aearost.aranarthcore.event.mob.ZombieHorseSpawn;
import com.aearost.aranarthcore.utils.AranarthUtils;
import org.bukkit.Bukkit;
import org.bukkit.entity.Animals;
import org.bukkit.entity.EntityType;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;

public class EntityDeathEventListener implements Listener {

    public EntityDeathEventListener(AranarthCore plugin) {
        Bukkit.getPluginManager().registerEvents(this, plugin);
    }

    /**
     * Centralizes all logic to be called by an entity dying.
     */
    @EventHandler
    public void onEntityDeath(EntityDeathEvent e) {
        if (e.getEntityType() == EntityType.HORSE) {
            new ZombieHorseSpawn().execute(e);
        } else if (e.getEntityType() == EntityType.WANDERING_TRADER) {
            new WanderingTraderDeath().execute(e);
        } else if (e.getEntityType() == EntityType.GOAT) {
            new GoatDeath().execute(e);
        }

        if (AranarthUtils.getMonth() == Month.FAUNIVOR) {
            if (e.getEntity() instanceof Animals) {
                new FaunivorExtraDeathDrops().execute(e);
            }
        }
    }
}
