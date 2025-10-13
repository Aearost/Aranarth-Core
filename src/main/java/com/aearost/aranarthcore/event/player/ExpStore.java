package com.aearost.aranarthcore.event.player;

import org.bukkit.Bukkit;
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
        Bukkit.getLogger().info("A");
        if (e.getHand() == EquipmentSlot.HAND) {
            Bukkit.getLogger().info("B");
            if (player.getInventory().getItemInMainHand().getType() == Material.GLASS_BOTTLE) {
                Bukkit.getLogger().info("C");
                int amountToReduce = new Random().nextInt(10) + 10;
                int totalExp = getTotalExp(player);

                Bukkit.getLogger().info("Total: " + totalExp);
                Bukkit.getLogger().info("EXP: " + player.getExp());
                Bukkit.getLogger().info("EXP to Level: " + player.getExpToLevel());
                Bukkit.getLogger().info("To reduce: " + amountToReduce);

                if (totalExp >= amountToReduce) {
                    Bukkit.getLogger().info("D");
                    if (player.isSneaking()) {
                        Bukkit.getLogger().info("E");
                        // Reduces by a less than what is gained
                        amountToReduce = amountToReduce * -1;
                        player.giveExp(amountToReduce);

                        player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_CHIME, 1F, 1F);
                        player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_CHIME, 1F, 1.5F);
                        player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_CHIME, 1F, 2F);
                        HashMap<Integer, ItemStack> leftover = player.getInventory().addItem(new ItemStack(Material.EXPERIENCE_BOTTLE));
                        // If the player's inventory was full, drop it to the ground
                        if (!leftover.isEmpty()) {
                            Bukkit.getLogger().info("F");
                            player.getLocation().getWorld().dropItemNaturally(player.getLocation(), leftover.get(0));
                        }
                        player.getInventory().getItemInMainHand().setAmount(player.getInventory().getItemInMainHand().getAmount() - 1);
                    }
                }
            }
        }
    }

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
