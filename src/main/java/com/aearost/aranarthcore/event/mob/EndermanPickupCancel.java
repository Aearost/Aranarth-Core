package com.aearost.aranarthcore.event.mob;

import org.bukkit.event.entity.EntityChangeBlockEvent;

/**
 * Handles logic to prevent the picking up of blocks for Endermen.
 */
public class EndermanPickupCancel {

    public void execute(EntityChangeBlockEvent e) {
        e.setCancelled(true);
    }
}
