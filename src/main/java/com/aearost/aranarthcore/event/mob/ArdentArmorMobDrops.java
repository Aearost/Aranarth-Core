package com.aearost.aranarthcore.event.mob;

import com.aearost.aranarthcore.utils.AranarthUtils;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.inventory.EntityEquipment;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Increases mob drops by 1.5x when the killer is wearing full ardent aranarthium.
 */
public class ArdentArmorMobDrops {

    public void execute(EntityDeathEvent e) {
        Player killer = e.getEntity().getKiller();
        if (killer == null || !AranarthUtils.isWearingArmorType(killer, "ardent")) {
            return;
        }

        EntityEquipment equipment = e.getEntity().getEquipment();
        List<ItemStack> equipmentList = new ArrayList<>(Arrays.asList(equipment.getArmorContents()));
        equipmentList.add(equipment.getItemInMainHand());
        equipmentList.add(equipment.getItemInOffHand());

        for (ItemStack drop : e.getDrops()) {
            // Avoid duplicating worn armor or held items
            if (equipmentList.contains(drop)) {
                continue;
            }
            // Skip saddles, horse armor, and armor stands
            if (drop.getType() == Material.SADDLE
                    || drop.getType().name().contains("_ARMOR")
                    || drop.getType() == Material.ARMOR_STAND) {
                continue;
            }
            drop.setAmount((int) Math.ceil(drop.getAmount() * 1.5));
        }
    }
}
