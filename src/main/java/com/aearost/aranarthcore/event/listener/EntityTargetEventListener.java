package com.aearost.aranarthcore.event.listener;

import com.aearost.aranarthcore.AranarthCore;
import com.aearost.aranarthcore.event.mob.GuardianTargetPrevent;
import com.aearost.aranarthcore.event.mob.PetTargetPrevent;
import com.aearost.aranarthcore.event.mob.PiglinTargetPrevent;
import org.bukkit.Bukkit;
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
    public void onEntityTarget(EntityTargetEvent e) {
        new GuardianTargetPrevent().execute(e);
        new PiglinTargetPrevent().execute(e);
        new PetTargetPrevent().execute(e);
    }
}
