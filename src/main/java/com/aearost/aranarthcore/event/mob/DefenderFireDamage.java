package com.aearost.aranarthcore.event.mob;

import com.aearost.aranarthcore.utils.DefenderUtils;
import org.bukkit.event.entity.EntityCombustByBlockEvent;
import org.bukkit.event.entity.EntityCombustByEntityEvent;
import org.bukkit.event.entity.EntityCombustEvent;

/**
 * Prevents Defender entities from being ignited by sunlight.
 */
public class DefenderFireDamage {

    public void execute(EntityCombustEvent e) {
        if (!DefenderUtils.isDefender(e.getEntity().getUniqueId())) {
            return;
        }
        // Allow combustion from entities (fire aspect, flame bow) and blocks (lava, fire)
        if (e instanceof EntityCombustByEntityEvent || e instanceof EntityCombustByBlockEvent) {
            return;
        }
        // Cancel sunlight combustion only
        e.setCancelled(true);
    }
}
