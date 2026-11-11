package com.aearost.aranarthcore.event.mob;

import org.bukkit.entity.*;
import org.bukkit.event.entity.EntityTargetEvent;
import org.bukkit.potion.PotionEffectType;

/**
 * Prevents mobs from targeting invisible players.
 */
public class InvisibilityMobTargetPrevent {

    // Mobs already targeting the player are cleared within this radius when they turn invisible
    private static final double CLEAR_RADIUS = 64;

    public void execute(EntityTargetEvent e) {
        if (!(e.getTarget() instanceof Player player)) return;
        if (!player.hasPotionEffect(PotionEffectType.INVISIBILITY)) return;
        if (piercesInvisibility(e.getEntity())) return;

        e.setCancelled(true);
    }

    /**
     * Makes every nearby mob that is already targeting the player forget them.
     */
    public static void clearTargets(Player player) {
        if (!player.isOnline()) return;
        for (Entity entity : player.getNearbyEntities(CLEAR_RADIUS, CLEAR_RADIUS, CLEAR_RADIUS)) {
            if (!(entity instanceof Mob mob)) continue;
            if (piercesInvisibility(mob)) continue;
            if (player.equals(mob.getTarget())) {
                mob.setTarget(null);
            }
        }
    }

    /**
     * Bosses and Wardens pierce invisibility so that they cannot be cheesed.
     */
    private static boolean piercesInvisibility(Entity entity) {
        return entity instanceof EnderDragon
                || entity instanceof Wither
                || entity instanceof Guardian
                || entity instanceof Warden;
    }
}
