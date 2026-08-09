package com.aearost.aranarthcore.event.block;

import net.kyori.adventure.sound.Sound;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Sheep;
import org.bukkit.event.player.PlayerShearEntityEvent;
import org.bukkit.inventory.ItemStack;

import java.util.List;

/**
 * Handles logic when shearing a sheep with shears that have the Incantation of Plentiful.
 */
public class IncantationPlentifulShear {

    public void execute(PlayerShearEntityEvent e) {
        Player player = e.getPlayer();

        if (!(e.getEntity() instanceof Sheep clickedSheep)) {
            return;
        }

        List<Entity> nearby = clickedSheep.getNearbyEntities(3, 3, 3);
        for (Entity nearbyEntity : nearby) {
            if (!(nearbyEntity instanceof Sheep nearbySheep)) {
                continue;
            }
            if (!nearbySheep.readyToBeSheared()) {
                continue;
            }
            nearbySheep.shear(Sound.Source.NEUTRAL);
            ItemStack shears = player.getInventory().getItemInMainHand();
            if (shears == null || shears.getType().isAir()) {
                break;
            }
            player.getInventory().setItemInMainHand(shears.damage(1, player));
        }
    }
}
