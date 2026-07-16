package com.aearost.aranarthcore.event.player;

import com.aearost.aranarthcore.utils.AranarthUtils;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerItemConsumeEvent;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.concurrent.ThreadLocalRandom;

/**
 * Grants bonus effects to Fae Aranarthium wearers when consuming mushroom or suspicious stew.
 */
public class FaeMushroomStewBonus {

    public void execute(PlayerItemConsumeEvent e) {
        Player player = e.getPlayer();
        if (!AranarthUtils.isWearingArmorType(player, "fae")) {
            return;
        }

        Material type = e.getItem().getType();
        if (type != Material.MUSHROOM_STEW && type != Material.SUSPICIOUS_STEW) {
            return;
        }

        // Always grant Regeneration III for 8s
        if (type == Material.MUSHROOM_STEW) {
            player.addPotionEffect(new PotionEffect(PotionEffectType.REGENERATION, 160, 2));
        }

        // Random bonus effect
        int roll = ThreadLocalRandom.current().nextInt(3);
        switch (roll) {
            case 0 -> player.addPotionEffect(new PotionEffect(PotionEffectType.HASTE, 600, 0));
            case 1 -> player.addPotionEffect(new PotionEffect(PotionEffectType.FIRE_RESISTANCE, 600, 0));
            case 2 -> player.addPotionEffect(new PotionEffect(PotionEffectType.ABSORPTION, 600, 0));
        }
    }
}
