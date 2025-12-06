package com.aearost.aranarthcore.event.mob;

import org.bukkit.Material;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.inventory.EntityEquipment;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

/**
 * Deals with dropping extra drops during the month of Faunivor.
 */
public class FaunivorExtraDeathDrops {
	public void execute(EntityDeathEvent e) {
		EntityEquipment equipment = e.getEntity().getEquipment();
		List<ItemStack> equipmentList = new ArrayList<>();
        equipmentList.addAll(Arrays.asList(equipment.getArmorContents()));
		equipmentList.add(equipment.getItemInMainHand());
		equipmentList.add(equipment.getItemInOffHand());

		for (ItemStack drop : e.getDrops()) {
			// Avoid duplication of held items or worn armor
			if (equipmentList.contains(drop)) {
				continue;
			}

			// Avoids duplication of saddles and armor on mounts
			if (drop.getType() == Material.SADDLE || drop.getType().name().contains("_ARMOR")) {
				continue;
			}

			int rand = new Random().nextInt(4);
			// 50% chance to increase the drop by 1
			if (rand <= 1) {
				drop.setAmount(drop.getAmount() + 1);
			}
			// 25% chance to increase the drop by 2
			else if (rand == 3) {
				drop.setAmount(drop.getAmount() + 2);
			}
		}
	}
}
