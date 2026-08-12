package com.aearost.aranarthcore.event.mob;

import org.bukkit.entity.*;
import org.bukkit.event.entity.EntityTargetEvent;
import org.bukkit.potion.PotionEffectType;

/**
 * Prevents mobs from targeting invisible players.
 */
public class InvisibilityMobTargetPrevent {
    public void execute(EntityTargetEvent e) {
        if (!(e.getTarget() instanceof Player player)) return;
        if (!player.hasPotionEffect(PotionEffectType.INVISIBILITY)) return;

        // These mobs pierce invisibility
        if (e.getEntity() instanceof EnderDragon
                || e.getEntity() instanceof Wither
                || e.getEntity() instanceof Guardian
                || e.getEntity() instanceof ElderGuardian) {
            return;
        }

        e.setCancelled(true);
    }
}
