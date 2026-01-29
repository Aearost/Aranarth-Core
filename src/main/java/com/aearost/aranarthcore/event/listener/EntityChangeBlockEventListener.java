package com.aearost.aranarthcore.event.listener;

import com.aearost.aranarthcore.AranarthCore;
import com.aearost.aranarthcore.event.block.ConcretePowderGravityPrevent;
import com.aearost.aranarthcore.event.mob.EndermanPickupCancel;
import org.bukkit.Bukkit;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.FallingBlock;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityChangeBlockEvent;

/**
 * Centralizes all logic to be called by a change to a block by an entity.
 */
public class EntityChangeBlockEventListener implements Listener {

    public EntityChangeBlockEventListener(AranarthCore plugin) {
        Bukkit.getPluginManager().registerEvents(this, plugin);
    }

    @EventHandler
    public void onBlockChange(EntityChangeBlockEvent e) {
        if (e.getEntityType() == EntityType.ENDERMAN) {
            new EndermanPickupCancel().execute(e);
        }
        else if (e.getEntity() instanceof FallingBlock) {
            new ConcretePowderGravityPrevent().execute(e);
        }
    }

}
