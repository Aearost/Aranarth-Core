package com.aearost.aranarthcore.event.block;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.event.block.BlockBreakEvent;

/**
 * Handles block breaks for pickaxes with the Incantation of Preservation.
 */
public class IncantationPreservationBlockBreak {
    public void execute(BlockBreakEvent e) {
        Material type = e.getBlock().getType();
        Location location = e.getBlock().getLocation();

        if (type == Material.SPAWNER || type == Material.TRIAL_SPAWNER || type == Material.REINFORCED_DEEPSLATE) {
            e.setDropItems(false);
            location.getWorld().dropItemNaturally(location, new ItemStack(type, 1));
        } else if (type == Material.DRAGON_EGG) {
            // Cancel to prevent the vanilla teleport behavior, then manually remove and drop
            e.setCancelled(true);
            e.getBlock().setType(Material.AIR);
            location.getWorld().dropItemNaturally(location, new ItemStack(Material.DRAGON_EGG, 1));
        }
    }
}
