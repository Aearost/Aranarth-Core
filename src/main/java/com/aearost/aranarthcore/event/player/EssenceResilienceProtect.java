package com.aearost.aranarthcore.event.player;

import com.aearost.aranarthcore.utils.AranarthUtils;
import org.bukkit.entity.Item;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.PlayerItemDamageEvent;

/**
 * Handles the effects of Essence of Resilience.
 */
public class EssenceResilienceProtect {

    public void executeDurability(PlayerItemDamageEvent e) {
        if (AranarthUtils.hasEssence(e.getItem(), "essence_resilience")) {
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
        if (AranarthUtils.hasEssence(itemEntity.getItemStack(), "essence_resilience")) {
            e.setCancelled(true);
        }
    }
}
