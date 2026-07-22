package com.aearost.aranarthcore.event.mob;

import com.aearost.aranarthcore.objects.DefenderType;
import com.aearost.aranarthcore.utils.DefenderUtils;
import org.bukkit.entity.AbstractArrow;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Tameable;
import org.bukkit.event.entity.EntityDamageByEntityEvent;

import java.util.Random;
import java.util.UUID;

/**
 * Applies custom damage values when a Defender attacks a player, and enforces PvP permission rules.
 */
public class DefenderCombat {

    private static final Random RANDOM = new Random();

    public void execute(EntityDamageByEntityEvent e) {

        // Same-dominion defenders never damage each other
        UUID targetUUID = e.getEntity().getUniqueId();
        UUID sourceUUID = getDefenderSource(e);
        if (DefenderUtils.isDefender(targetUUID) && sourceUUID != null) {
            UUID sourceDominion = DefenderUtils.getDefenderDominionId(sourceUUID);
            UUID targetDominion = DefenderUtils.getDefenderDominionId(targetUUID);
            if (sourceDominion != null && sourceDominion.equals(targetDominion)) {
                e.setCancelled(true);
                return;
            }
            // Different-dominion defenders can damage each other (attacker system)
        }

        // Defender is the attacker and the target is a tamed pet
        if (e.getEntity() instanceof Tameable pet
                && pet.getOwner() instanceof Player petOwner) {
            UUID defenderUUID = getDefenderSource(e);
            if (defenderUUID != null) {
                UUID dominionId = DefenderUtils.getDefenderDominionId(defenderUUID);
                if (dominionId != null && !DefenderUtils.shouldDefenderTarget(dominionId, petOwner)) {
                    e.setCancelled(true);
                    return;
                }
            }
        }

        // Defender is the attacker and the target is a player
        if (e.getEntity() instanceof Player target) {
            UUID defenderUUID = getDefenderSource(e);
            if (defenderUUID != null) {
                UUID dominionId = DefenderUtils.getDefenderDominionId(defenderUUID);
                DefenderType defType = DefenderUtils.getDefenderType(defenderUUID);
                if (dominionId == null || defType == null) {
                    return;
                }

                if (!DefenderUtils.shouldDefenderTarget(dominionId, target)) {
                    e.setCancelled(true);
                    return;
                }
                // Apply random damage within the type's configured range
                double damage = defType.getMinDamage()
                        + RANDOM.nextDouble() * (defType.getMaxDamage() - defType.getMinDamage());
                e.setDamage(damage);
            }
        }

        // A player is attacking a Defender
        else if (DefenderUtils.isDefender(e.getEntity().getUniqueId())
                && e.getDamager() instanceof Player attacker) {
            UUID dominionId = DefenderUtils.getDefenderDominionId(e.getEntity().getUniqueId());
            if (dominionId == null) {
                return;
            }
            if (!DefenderUtils.shouldDefenderTarget(dominionId, attacker)) {
                e.setCancelled(true);
            }
        }

        // A tamed pet is attacking a Defender
        else if (DefenderUtils.isDefender(e.getEntity().getUniqueId())
                && e.getDamager() instanceof Tameable pet
                && pet.getOwner() instanceof Player owner) {
            UUID dominionId = DefenderUtils.getDefenderDominionId(e.getEntity().getUniqueId());
            if (dominionId == null) {
                return;
            }
            if (!DefenderUtils.shouldDefenderTarget(dominionId, owner)) {
                e.setCancelled(true);
            }
        }
    }

    /**
     * Returns the UUID of the Defender responsible for the damage.
     */
    private UUID getDefenderSource(EntityDamageByEntityEvent e) {
        // Direct melee
        if (DefenderUtils.isDefender(e.getDamager().getUniqueId())) {
            return e.getDamager().getUniqueId();
        }
        // Arrow shot by a Defender skeleton
        if (e.getDamager() instanceof AbstractArrow arrow
                && arrow.getShooter() instanceof LivingEntity shooter
                && DefenderUtils.isDefender(shooter.getUniqueId())) {
            return shooter.getUniqueId();
        }
        return null;
    }
}
