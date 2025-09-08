package com.aearost.aranarthcore.event.player;

import org.bukkit.Bukkit;
import org.bukkit.entity.Entity;
import org.bukkit.event.entity.EntityShootBowEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;

import static com.aearost.aranarthcore.items.CustomItemKeys.ARROW;

/**
 * Handles the auto-refill functionality when consuming of arrows.
 */
public class SpecialArrowShoot {

    public void execute(EntityShootBowEvent e) {
        ItemStack arrowItem = e.getConsumable();

        // Apply the value to the projectile entity
        Bukkit.getLogger().info("A");
        if (arrowItem.hasItemMeta()) {
            Bukkit.getLogger().info("B");
            if (arrowItem.getItemMeta().getPersistentDataContainer().has(ARROW)) {
                Bukkit.getLogger().info("C");
                String type = arrowItem.getItemMeta().getPersistentDataContainer().get(ARROW, PersistentDataType.STRING);
                Entity projectile = e.getProjectile();
                projectile.getPersistentDataContainer().set(ARROW, PersistentDataType.STRING, type);
            }
        }
    }
}
