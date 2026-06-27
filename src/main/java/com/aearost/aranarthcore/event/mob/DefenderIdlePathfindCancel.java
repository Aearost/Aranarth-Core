package com.aearost.aranarthcore.event.mob;

import com.aearost.aranarthcore.objects.DefenderMode;
import com.aearost.aranarthcore.utils.DefenderUtils;
import com.destroystokyo.paper.event.entity.EntityPathfindEvent;

import java.util.UUID;

/**
 * Cancels pathfinding for idle defenders.
 */
public class DefenderIdlePathfindCancel {

    public void execute(EntityPathfindEvent e) {
        UUID uuid = e.getEntity().getUniqueId();
        if (DefenderUtils.isDefender(uuid) && DefenderUtils.getDefenderMode(uuid) == DefenderMode.IDLE) {
            e.setCancelled(true);
        }
    }
}
