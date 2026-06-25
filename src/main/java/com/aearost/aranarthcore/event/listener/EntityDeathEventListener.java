package com.aearost.aranarthcore.event.listener;

import com.aearost.aranarthcore.AranarthCore;
import com.aearost.aranarthcore.enums.Month;
import com.aearost.aranarthcore.event.mob.*;
import com.aearost.aranarthcore.event.player.DefenderKillDeathMessage;
import com.aearost.aranarthcore.event.player.DominionDeath;
import com.aearost.aranarthcore.utils.DefenderUtils;
import com.aearost.aranarthcore.event.player.MountKillDeathMessage;
import com.aearost.aranarthcore.event.player.PlayerHeadDrop;
import com.aearost.aranarthcore.event.player.PlayerKillDeathStats;
import com.aearost.aranarthcore.event.player.PlayerKillMoneySteal;
import com.aearost.aranarthcore.utils.AranarthUtils;
import org.bukkit.Bukkit;
import org.bukkit.entity.Animals;
import org.bukkit.entity.EntityType;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.PlayerDeathEvent;

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
        } else if (e.getEntityType() == EntityType.PHANTOM) {
            new PhantomSpawnNotify().execute(e);
        } else if (e.getEntityType() == EntityType.PLAYER) {
            new PlayerHeadDrop().execute(e);
            new DominionDeath().execute(e);
            new PlayerKillDeathStats().execute(e);
            new PlayerKillMoneySteal().execute(e);
            new MountKillDeathMessage().execute((PlayerDeathEvent) e);
            new DefenderKillDeathMessage().execute((PlayerDeathEvent) e);
        }

        // If the mob was a sentinel
        if (e.getEntityType() == EntityType.HORSE || e.getEntityType() == EntityType.IRON_GOLEM
                || e.getEntityType() == EntityType.WOLF) {
            new SentinelDeath().execute(e);
        }

        if (e.getEntity() instanceof Animals) {
            new ExtraDropsFromBoneArrow().execute(e);
            // Applies even further buffs
            if (AranarthUtils.getMonth() == Month.FAUNIVOR) {
                new FaunivorExtraDeathDrops().execute(e);
            }
        }

        if (e.getEntityType() != EntityType.PLAYER) {
            new ArdentArmorMobDrops().execute(e);
        }

        if (DefenderUtils.isDefender(e.getEntity().getUniqueId())) {
            new DefenderDeath().execute(e);
        }
    }
}
