package com.aearost.aranarthcore.event.player;

import org.bukkit.Bukkit;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.AbstractArrow;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityShootBowEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;

import static com.aearost.aranarthcore.items.CustomItemKeys.ARROW;

/**
 * Handles adding the Persistent Data attributes to the custom special arrows.
 */
public class SpecialArrowShoot {

    public void execute(EntityShootBowEvent e) {
        ItemStack arrowItem = e.getConsumable();
        if (e.getEntity() instanceof Player player) {
            // Apply the value to the projectile entity
            if (arrowItem.hasItemMeta()) {
                if (arrowItem.getItemMeta().getPersistentDataContainer().has(ARROW)) {
                    Bukkit.getLogger().info("It's a special arrow: " + arrowItem.getAmount());
                    String arrowType = arrowItem.getItemMeta().getPersistentDataContainer().get(ARROW, PersistentDataType.STRING);
                    Entity projectile = e.getProjectile();
                    projectile.getPersistentDataContainer().set(ARROW, PersistentDataType.STRING, arrowType);

                    // Manual arrow reduction if using a bow with infinity for special arrows
                    if (e.getBow().containsEnchantment(Enchantment.INFINITY)) {
                        for (int i = 0; i < player.getInventory().getContents().length; i++) {
                            ItemStack item = player.getInventory().getContents()[i];
                            if (item == null) {
                                continue;
                            }

                            if (item.getType().name().endsWith("ARROW")) {
                                item.setAmount(item.getAmount() - 1);
                                // Also allows the arrow to be picked up
                                if (projectile instanceof Arrow arrow) {
                                    arrow.setPickupStatus(AbstractArrow.PickupStatus.ALLOWED);
                                    return;
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
