package com.aearost.aranarthcore.event.listener;

import com.aearost.aranarthcore.AranarthCore;
import com.aearost.aranarthcore.enums.Month;
import com.aearost.aranarthcore.event.mob.BabyMobSpawn;
import com.aearost.aranarthcore.event.mob.ParrotJumpCancelDismount;
import com.aearost.aranarthcore.event.mob.PillagerOutpostSpawnCancel;
import com.aearost.aranarthcore.event.mob.MountSpawn;
import com.aearost.aranarthcore.utils.AranarthUtils;
import org.bukkit.Bukkit;
import org.bukkit.entity.AbstractHorse;
import org.bukkit.entity.Parrot;
import org.bukkit.entity.Pillager;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.CreatureSpawnEvent;

public class CreatureSpawnEventListener implements Listener {

    public CreatureSpawnEventListener(AranarthCore plugin) {
        Bukkit.getPluginManager().registerEvents(this, plugin);
    }

    /**
     * Centralizes all logic to be called by a creature spawning.
     */
    @EventHandler
    public void onCreatureSpawn(CreatureSpawnEvent e) {
        if (e.getEntity() instanceof AbstractHorse) {
            new MountSpawn().execute(e);
        } else if (e.getEntity() instanceof Pillager) {
            new PillagerOutpostSpawnCancel().execute(e);
        } else if (e.getEntity() instanceof Parrot) {
            new ParrotJumpCancelDismount().execute(e);
        } else if (AranarthUtils.getMonth() == Month.CALORVOR) {
            new BabyMobSpawn().execute(e);
        }
    }
}
