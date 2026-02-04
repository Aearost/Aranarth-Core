package com.aearost.aranarthcore.event.player;

import com.aearost.aranarthcore.utils.AranarthUtils;
import org.bukkit.Material;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;

import java.util.Random;

/**
 * Handles logic to drop player heads at a low rate when killed.
 */
public class PlayerHeadDrop {

    public void execute(EntityDeathEvent e) {
        Player player = (Player) e.getEntity();
        if (player.getWorld().getName().startsWith("world") || player.getWorld().getName().startsWith("smp")) {
            if (e.getDamageSource().getCausingEntity() != null
                    && e.getDamageSource().getCausingEntity().getType() == EntityType.PLAYER) {
                if (e.getDamageSource().getCausingEntity() != null) {
                    if (e.getDamageSource().getCausingEntity() instanceof Player attacker) {
                        Random random = new Random();
                        ItemStack weapon = attacker.getInventory().getItemInMainHand();

                        // 10% chance
                        int range = 50;
                        if (AranarthUtils.hasIncantation(weapon, "beheading")) {
                            int level = AranarthUtils.getIncantationLevel(weapon);
                            if (level == 1) {
                                // 20% chance
                                range = 25;
                            } else if (level == 2) {
                                // 33% chance
                                range = 15;
                            } else if (level == 3) {
                                // 50% chance
                                range = 10;
                            }
                        }

                        int chance = random.nextInt(range) + 1;
                        if (chance <= 5) {
                            ItemStack skull = new ItemStack(Material.PLAYER_HEAD);
                            SkullMeta meta = (SkullMeta) skull.getItemMeta();
                            meta.setOwningPlayer(player);
                            skull.setItemMeta(meta);
                            e.getDrops().add(skull);
                        }
                    }
                }
            }
        }
    }
}
