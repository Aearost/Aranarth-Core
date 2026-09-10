package com.aearost.aranarthcore.event.mob;

import com.aearost.aranarthcore.utils.AranarthUtils;
import com.aearost.aranarthcore.utils.MobHeadUtils;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.inventory.ItemStack;

import java.util.Random;

/**
 * Drops a mob-specific head when killed by a player using the Beheading incantation.
 */
public class MobHeadDrop {

    private static final Random RANDOM = new Random();

    public void execute(EntityDeathEvent e) {
        EntityType type = e.getEntityType();
        if (!MobHeadUtils.hasHead(type)) {
            return;
        }

        if (e.getDamageSource().getCausingEntity() == null
                || !(e.getDamageSource().getCausingEntity() instanceof Player attacker)) {
            return;
        }

        // Must be in a survival or SMP world
        String worldName = e.getEntity().getWorld().getName();
        if (!worldName.startsWith("world") && !AranarthUtils.isSmpWorld(worldName)) {
            return;
        }

        ItemStack weapon = attacker.getInventory().getItemInMainHand();
        boolean hasBeheading = AranarthUtils.hasIncantation(weapon, "incantation_beheading");

        int threshold;
        int range;
        if (type == EntityType.WITHER_SKELETON) {
            // Wither skeleton skulls are already rare in vanilla - keep Beheading rates lower
            if (!hasBeheading)                                      { threshold = 1; range = 200; } // 0.5%
            else {
                int level = AranarthUtils.getIncantationLevel(weapon);
                if (level == 1)      { threshold = 1; range = 20; }  // 5%
                else if (level == 2) { threshold = 1; range = 10; }  // 10%
                else if (level == 3) { threshold = 1; range = 5; }   // 20%
                else return;
            }
        } else {
            if (!hasBeheading)                                      { threshold = 1; range = 200; } // 0.5%
            else {
                int level = AranarthUtils.getIncantationLevel(weapon);
                if (level == 1)      { threshold = 1; range = 10; }  // 10%
                else if (level == 2) { threshold = 1; range = 5; }   // 20%
                else if (level == 3) { threshold = 3; range = 10; }  // 30%
                else return;
            }
        }

        int roll = RANDOM.nextInt(range) + 1;
        if (roll > threshold) {
            return;
        }

        ItemStack head = MobHeadUtils.createHead(type);
        if (head != null) {
            e.getDrops().add(head);
        }
    }
}
