package com.aearost.aranarthcore.event.player;

import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;

/**
 * Handles storing EXP in the player's held empty bottle.
 */
public class ExpBottle {

    public void execute(PlayerInteractEvent e) {
        Player player = e.getPlayer();
        if (e.getHand() == EquipmentSlot.HAND) {
            if (player.getInventory().getItemInMainHand().getType() == Material.GLASS_BOTTLE) {
                if (player.getLevel() > 0) {
                    if (player.isSneaking()) {
                        player.getInventory().addItem(new ItemStack(Material.EXPERIENCE_BOTTLE));
                        player.setLevel(player.getLevel() - 1);
                        player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_CHIME, 1F, 1F);
                        player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_CHIME, 1F, 1.5F);
                        player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_CHIME, 1F, 2F);
                    }
                }
            }
        }
    }
}
