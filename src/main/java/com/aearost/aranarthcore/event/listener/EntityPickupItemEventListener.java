package com.aearost.aranarthcore.event.listener;

import com.aearost.aranarthcore.AranarthCore;
import com.aearost.aranarthcore.event.mob.EntityEggPickupCancel;
import com.aearost.aranarthcore.event.player.BlacklistItemPickupPrevent;
import com.aearost.aranarthcore.event.player.CompressorItemPickup;
import com.aearost.aranarthcore.event.player.ShulkerItemPickup;
import org.bukkit.Bukkit;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityPickupItemEvent;

/**
 * Centralizes all logic to be called by a change to a block by an entity.
 */
public class EntityPickupItemEventListener implements Listener {

    public EntityPickupItemEventListener(AranarthCore plugin) {
        Bukkit.getPluginManager().registerEvents(this, plugin);
    }

    @EventHandler
    public void onItemPickup(EntityPickupItemEvent e) {

        if (e.getEntity() instanceof Player player) {
            new CompressorItemPickup().execute(e);
            new ShulkerItemPickup().execute(e);
            new BlacklistItemPickupPrevent().execute(e);
        } else if (e.getEntityType() == EntityType.ZOMBIE || e.getEntityType() == EntityType.ZOMBIE_VILLAGER) {
            new EntityEggPickupCancel().execute(e);
        }

    }

}
