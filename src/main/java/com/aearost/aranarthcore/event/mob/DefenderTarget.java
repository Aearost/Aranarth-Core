package com.aearost.aranarthcore.event.mob;

import com.aearost.aranarthcore.objects.DefenderMode;
import com.aearost.aranarthcore.utils.DefenderUtils;
import org.bukkit.entity.Monster;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityTargetEvent;

import java.util.UUID;

/**
 * Controls which entities a Defender is allowed to target.
 */
public class DefenderTarget {

    public void execute(EntityTargetEvent e) {
        UUID entityUUID = e.getEntity().getUniqueId();
        if (!DefenderUtils.isDefender(entityUUID)) {
            return;
        }

        // No target
        if (e.getTarget() == null) {
            return;
        }

        // Idle defenders never target anything
        if (DefenderUtils.getDefenderMode(entityUUID) == DefenderMode.IDLE) {
            e.setCancelled(true);
            return;
        }

        // Allow targeting of any hostile mob, EXCEPT defenders from the same dominion
        // Defenders from enemy dominions (future attacker system) are intentionally allowed
        if (e.getTarget() instanceof Monster) {
            UUID targetUUID = e.getTarget().getUniqueId();
            UUID myDominionId = DefenderUtils.getDefenderDominionId(entityUUID);
            if (DefenderUtils.isDefender(targetUUID)
                    && myDominionId != null
                    && myDominionId.equals(DefenderUtils.getDefenderDominionId(targetUUID))) {
                e.setCancelled(true);
            }
            return;
        }

        // Check PvP permissions
        if (e.getTarget() instanceof Player target) {
            UUID dominionId = DefenderUtils.getDefenderDominionId(entityUUID);
            if (dominionId == null || !DefenderUtils.shouldDefenderTarget(dominionId, target)) {
                e.setCancelled(true);
            }
            return;
        }

        // Cancel anything else i.e animals
        e.setCancelled(true);
    }
}
