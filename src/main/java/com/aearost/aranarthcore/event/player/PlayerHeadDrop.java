package com.aearost.aranarthcore.event.player;

import com.aearost.aranarthcore.objects.AranarthPlayer;
import com.aearost.aranarthcore.utils.AranarthUtils;
import com.aearost.aranarthcore.utils.MobHeadUtils;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.inventory.ItemStack;

import java.util.Random;

/**
 * Handles logic to drop player heads at a low rate when killed.
 */
public class PlayerHeadDrop {

    public void execute(EntityDeathEvent e) {
        Player player = (Player) e.getEntity();
        if (player.getWorld().getName().startsWith("world") || AranarthUtils.isSmpWorld(player.getWorld().getName())) {
            Player attacker = AranarthUtils.getKillingPlayer(e);
            if (attacker != null) {
                Random random = new Random();
                ItemStack weapon = attacker.getInventory().getItemInMainHand();

                // 5% base chance (1/20)
                int threshold = 1;
                int range = 20;
                if (AranarthUtils.hasEssence(weapon, "essence_beheading")) {
                    int level = AranarthUtils.getEssenceLevel(weapon);
                    if (level == 1) {
                        // 25% chance
                        threshold = 1;
                        range = 4;
                    } else if (level == 2) {
                        // 50% chance
                        threshold = 1;
                        range = 2;
                    } else if (level == 3) {
                        // 75% chance
                        threshold = 3;
                        range = 4;
                    }
                }

                int chance = random.nextInt(range) + 1;
                if (chance <= threshold) {
                    AranarthPlayer aranarthPlayer = AranarthUtils.getPlayer(player.getUniqueId());
                    e.getDrops().add(MobHeadUtils.createPlayerHead(player, "&e" + aranarthPlayer.getNickname() + "&e's Skull"));
                }
            }
        }
    }
}
