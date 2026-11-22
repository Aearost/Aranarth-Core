package com.aearost.aranarthcore.event.player;

import com.aearost.aranarthcore.abilities.airbending.spiritual.AstralProjection;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.potion.PotionEffectType;

/**
 * Removes Invisibility from a player when they take or deal damage.
 */
public class InvisibilityBreakOnDamage {
    public void execute(EntityDamageByEntityEvent e) {
        if (e.isCancelled()) {
            return;
        }

        Entity source = e.getDamager();
        if (e.getDamager() instanceof Projectile proj && proj.getShooter() instanceof Entity shooter) {
            source = shooter;
        }

        // Break the attacker's invisibility, whatever they hit
        if (source instanceof Player attacker) {
            breakInvisibility(attacker);
        }

        // Break the victim's invisibility when they are hit by anything other than themselves
        if (e.getEntity() instanceof Player damaged && !source.equals(damaged)) {
            breakInvisibility(damaged);
        }
    }

    private void breakInvisibility(Player player) {
        // The Invisibility of AstralProjection is part of the ability
        if (player.hasPotionEffect(PotionEffectType.INVISIBILITY) && !AstralProjection.isProjecting(player.getUniqueId())) {
            player.removePotionEffect(PotionEffectType.INVISIBILITY);
        }
    }
}
