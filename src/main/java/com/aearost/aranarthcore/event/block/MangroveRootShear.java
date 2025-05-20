package com.aearost.aranarthcore.event.block;

import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.Damageable;

/**
 * Allows a player to remove the roots from a Muddy Mangrove Roots block.
 */
public class MangroveRootShear {

    public void execute(PlayerInteractEvent e) {
        if (e.getHand() == EquipmentSlot.HAND) {
            ItemStack heldItem = e.getPlayer().getInventory().getItemInMainHand();
            if (heldItem.getType() == Material.SHEARS) {
                if (e.getClickedBlock().getType() == Material.MUDDY_MANGROVE_ROOTS) {
                    e.setCancelled(true);
                    e.getClickedBlock().setType(Material.MUD);
                    e.getClickedBlock().getWorld().dropItemNaturally(
                            e.getClickedBlock().getLocation(), new ItemStack(Material.HANGING_ROOTS, 1));
                    Damageable damageable = (Damageable) heldItem.getItemMeta();
                    damageable.setDamage(damageable.getDamage() + 1);
                    heldItem.setItemMeta(damageable);
                    e.getPlayer().playSound(e.getPlayer(), Sound.ENTITY_BOGGED_SHEAR, 0.25F, 1);
                }
            }
        }
    }
}
