package com.aearost.aranarthcore.event.mob;

import com.aearost.aranarthcore.utils.DefenderUtils;
import org.bukkit.event.entity.EntityDeathEvent;

/**
 * Handles the death of a Defender entity, decrementing the Dominion's defender count
 * and suppressing item/experience drops.
 */
public class DefenderDeath {

    public void execute(EntityDeathEvent e) {
        if (!DefenderUtils.isDefender(e.getEntity().getUniqueId())) {
            return;
        }
        e.getDrops().clear();
        e.setDroppedExp(0);
        DefenderUtils.onDefenderDeath(e.getEntity().getUniqueId());
    }
}
