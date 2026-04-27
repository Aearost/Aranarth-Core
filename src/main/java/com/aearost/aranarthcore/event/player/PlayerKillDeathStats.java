package com.aearost.aranarthcore.event.player;

import com.aearost.aranarthcore.utils.AranarthUtils;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDeathEvent;

/**
 * Handles logic to updating the player's kill and death stats as a player dies.
 */
public class PlayerKillDeathStats {

    public void execute(EntityDeathEvent e) {
        Player victim = (Player) e.getEntity();
        if (e.getDamageSource().getCausingEntity() != null && e.getDamageSource().getCausingEntity() instanceof Player killer) {
            AranarthUtils.updateKillAndDeath(killer, victim, victim.getWorld());
        } else {
            AranarthUtils.updateKillAndDeath(null, victim, victim.getWorld());
        }
    }
}
