package com.aearost.aranarthcore.event.mob;

import org.bukkit.Material;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.inventory.ItemStack;

import java.util.Random;

/**
 * Deals with dropping extra drops during the month of Faunivor.
 */
public class FaunivorExtraDeathDrops {
	public void execute(EntityDeathEvent e) {
		for (ItemStack drop : e.getDrops()) {
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
