package com.aearost.aranarthcore.event.listener;

import com.aearost.aranarthcore.AranarthCore;
import com.aearost.aranarthcore.enums.Month;
import com.aearost.aranarthcore.event.mob.BabyZombiePreventVillagerDamage;
import com.aearost.aranarthcore.event.player.ArenaPlayerKill;
import com.aearost.aranarthcore.event.player.FaunivorExtraAttackDamage;
import com.aearost.aranarthcore.utils.AranarthUtils;
import org.bukkit.Bukkit;
import org.bukkit.entity.Villager;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;

public class EntityDamageByEntityEventListener implements Listener {

    public EntityDamageByEntityEventListener(AranarthCore plugin) {
        Bukkit.getPluginManager().registerEvents(this, plugin);
    }

    /**
     * Centralizes all logic to be called by an entity being damaged by another entity.
     */
    @EventHandler
    public void onEntityDamageEntity(EntityDamageByEntityEvent e) {
        if (e.getDamager().getWorld().getName().equalsIgnoreCase("arena")) {
            new ArenaPlayerKill().execute(e);
        } else {
            if (e.getEntity() instanceof Villager villager) {
                new BabyZombiePreventVillagerDamage().execute(e);
            }

            if (AranarthUtils.getMonth() == Month.FAUNIVOR) {
                new FaunivorExtraAttackDamage().execute(e);
            }
        }


    }
}
