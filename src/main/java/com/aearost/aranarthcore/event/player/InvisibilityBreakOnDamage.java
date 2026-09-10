package com.aearost.aranarthcore.event.player;

import com.aearost.aranarthcore.event.listener.misc.InvisibilityListener;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffectType;

/**
 * Removes Invisibility from a player when they take or deal damage.
 */
public class InvisibilityBreakOnDamage {
    public void execute(EntityDamageByEntityEvent e) {
        if (e.isCancelled()) {
            return;
        }

        Player attacker = null;
        if (e.getDamager() instanceof Player p) {
            attacker = p;
        } else if (e.getDamager() instanceof Projectile proj && proj.getShooter() instanceof Player p) {
            attacker = p;
        }

        if (e.getEntity() instanceof Player damaged && damaged.hasPotionEffect(PotionEffectType.INVISIBILITY)
                && attacker != null) {
            // Break attacker's invisibility as well
            if (attacker.hasPotionEffect(PotionEffectType.INVISIBILITY)) {
                attacker.removePotionEffect(PotionEffectType.INVISIBILITY);
            }

            // Read stored armor values BEFORE restoreArmor(), which clears the map entry
            double[] armorValues = InvisibilityListener.getStoredArmorValues(damaged.getUniqueId());

            // Restore armor items to the player's inventory
            InvisibilityListener.restoreArmor(damaged);
            damaged.removePotionEffect(PotionEffectType.INVISIBILITY);

            // Manually apply the vanilla armor reduction formula using the snapshotted armor values
            applyArmorReduction(e, damaged, armorValues);
            return;
        }

        // Break invisibility on the attacker for all other cases (hitting a mob, etc.)
        if (attacker != null && attacker.hasPotionEffect(PotionEffectType.INVISIBILITY)) {
            attacker.removePotionEffect(PotionEffectType.INVISIBILITY);
        }
    }

    /**
     * Applies the vanilla armor and Protection enchantment damage reduction to the event.
     */
    private void applyArmorReduction(EntityDamageByEntityEvent e, Player damaged, double[] armorValues) {
        double armor = armorValues[0];
        double toughness = armorValues[1];
        if (armor <= 0) {
            return;
        }

        double rawDamage = e.getDamage();

        // Vanilla armor reduction: effectiveArmor capped at 20, damage reduced by effectiveArmor/25
        double effectiveArmor = Math.min(20.0, Math.max(armor / 5.0, armor - (4.0 * rawDamage) / (toughness + 8.0)));
        double damageAfterArmor = rawDamage * (1.0 - effectiveArmor / 25.0);

        // Protection enchantment reduction (EPF capped at 20)
        int totalEPF = 0;
        for (ItemStack piece : damaged.getInventory().getArmorContents()) {
            if (piece != null && !piece.getType().isAir()) {
                totalEPF += piece.getEnchantmentLevel(Enchantment.PROTECTION);
            }
        }
        totalEPF = Math.min(totalEPF, 20);
        double finalDamage = damageAfterArmor * (1.0 - totalEPF / 25.0);

        e.setDamage(finalDamage);
    }
}
