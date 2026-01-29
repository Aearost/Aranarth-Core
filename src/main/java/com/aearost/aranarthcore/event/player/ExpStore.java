package com.aearost.aranarthcore.event.player;

import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;

import java.util.HashMap;
import java.util.Random;

/**
 * Handles storing EXP in the player's held empty bottle.
 */
public class ExpStore {

    public void execute(PlayerInteractEvent e) {
        Player player = e.getPlayer();
        if (e.getHand() == EquipmentSlot.HAND) {
            if (player.getInventory().getItemInMainHand().getType() == Material.GLASS_BOTTLE) {
                int amountToReduce = new Random().nextInt(10) + 10;
                int totalExp = getTotalExp(player);

                if (totalExp >= amountToReduce) {
                    if (player.isSneaking()) {
                        // Reduces by a less than what is gained
                        amountToReduce = amountToReduce * -1;
                        player.giveExp(amountToReduce);

                        player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_CHIME, 1F, 1F);
                        player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_CHIME, 1F, 1.5F);
                        player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_CHIME, 1F, 2F);
                        HashMap<Integer, ItemStack> leftover = player.getInventory().addItem(new ItemStack(Material.EXPERIENCE_BOTTLE));
                        // If the player's inventory was full, drop it to the ground
                        if (!leftover.isEmpty()) {
                            player.getLocation().getWorld().dropItemNaturally(player.getLocation(), leftover.get(0));
                        }
                        player.getInventory().getItemInMainHand().setAmount(player.getInventory().getItemInMainHand().getAmount() - 1);
                    }
                }
            }
        }
    }

    /**
     * Helper method to provide the true total EXP a player has.
     * @param player The player to be analyzed.
     * @return The true total EXP.
     */
    private int getTotalExp(Player player) {
        int level = player.getLevel();
        float progress = player.getExp(); // 0.0 - 1.0

        int exp = 0;

        if (level <= 16) {
            exp = (int) (Math.pow(level, 2) + 6 * level);
        } else if (level <= 31) {
            exp = (int) (2.5 * Math.pow(level, 2) - 40.5 * level + 360);
        } else {
            exp = (int) (4.5 * Math.pow(level, 2) - 162.5 * level + 2220);
        }

        // Add progress within the current level
        exp += Math.round(progress * player.getExpToLevel());
        return exp;
    }
}
