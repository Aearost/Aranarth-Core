package com.aearost.aranarthcore.event.player;

import com.aearost.aranarthcore.utils.AranarthUtils;
import org.bukkit.entity.Item;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.PlayerItemDamageEvent;

/**
 * Handles the effects of Incantation of Resilience.
 */
public class IncantationResilienceProtect {

    public void executeDurability(PlayerItemDamageEvent e) {
        if (AranarthUtils.hasIncantation(e.getItem(), "incantation_resilience")) {
            e.setCancelled(true);
        }
    }

    public void executeFireLava(EntityDamageEvent e) {
        if (!(e.getEntity() instanceof Item itemEntity)) return;
        EntityDamageEvent.DamageCause cause = e.getCause();
        if (cause != EntityDamageEvent.DamageCause.FIRE
                && cause != EntityDamageEvent.DamageCause.FIRE_TICK
                && cause != EntityDamageEvent.DamageCause.LAVA) {
            return;
        }
        if (AranarthUtils.hasIncantation(itemEntity.getItemStack(), "incantation_resilience")) {
            e.setCancelled(true);
        }
    }
}
