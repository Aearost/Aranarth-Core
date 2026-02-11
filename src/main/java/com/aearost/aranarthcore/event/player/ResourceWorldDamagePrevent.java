package com.aearost.aranarthcore.event.player;

import com.aearost.aranarthcore.utils.ChatUtils;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageEvent;

/**
 * Prevents players from damaging each other in the Resource world.
 */
public class ResourceWorldDamagePrevent {

    public void execute(EntityDamageEvent e) {
        if (e.getDamageSource().getCausingEntity() != null && e.getDamageSource().getCausingEntity() instanceof Player attacker) {
            if (e.getEntity() instanceof Player target) {
                if (target.getWorld().getName().startsWith("resource")) {
                    e.setCancelled(true);
                    attacker.sendMessage(ChatUtils.chatMessage("&cYou cannot attack players in the Resource World!"));
                }
            }
        }
    }

}
