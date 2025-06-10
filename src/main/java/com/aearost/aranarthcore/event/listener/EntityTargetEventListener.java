package com.aearost.aranarthcore.event.listener;

import com.aearost.aranarthcore.AranarthCore;
import com.aearost.aranarthcore.event.mob.*;
import org.bukkit.Bukkit;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityTargetEvent;

public class EntityTargetEventListener implements Listener {

    public EntityTargetEventListener(AranarthCore plugin) {
        Bukkit.getPluginManager().registerEvents(this, plugin);
    }

    /**
     * Centralizes all logic to be called by an entity targeting another.
     */
    @EventHandler
    public void onCreatureSpawn(EntityTargetEvent e) {
        if (e.getEntity() instanceof Guardian || e.getEntity() instanceof PiglinAbstract) {
            new GuardianTargetPrevent().execute(e);
            new PiglinTargetPrevent().execute(e);
        }
    }
}
