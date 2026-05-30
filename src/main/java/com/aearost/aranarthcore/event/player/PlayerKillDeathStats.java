package com.aearost.aranarthcore.event.player;

import com.aearost.aranarthcore.utils.AranarthUtils;
import com.aearost.aranarthcore.utils.MountUtils;
import org.bukkit.Bukkit;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDeathEvent;

import java.util.UUID;

/**
 * Handles logic to updating the player's kill and death stats as a player dies.
 */
public class PlayerKillDeathStats {

    public void execute(EntityDeathEvent e) {
        Player victim = (Player) e.getEntity();
        Entity causingEntity = e.getDamageSource().getCausingEntity();

        Player killer = null;
        if (causingEntity instanceof Player p) {
            killer = p;
        } else if (causingEntity != null) {
            // If the killing entity is an active mount, attribute the kill to its owner
            String[] info = MountUtils.getActiveMountInfo(causingEntity.getUniqueId());
            if (info != null) {
                try {
                    killer = Bukkit.getPlayer(UUID.fromString(info[0]));
                } catch (IllegalArgumentException ignored) {
                }
            }
        }

        AranarthUtils.updateKillAndDeath(killer, victim, victim.getWorld());
    }
}
