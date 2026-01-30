package com.aearost.aranarthcore.event.player;

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
                Random random = new Random();
                int chance = random.nextInt(100) + 1;
                // Only a 5% chance of a head being dropped
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
