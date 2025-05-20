package com.aearost.aranarthcore.event.mob;

import com.aearost.aranarthcore.enums.Month;
import com.aearost.aranarthcore.utils.AranarthUtils;
import org.bukkit.Material;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.inventory.ItemStack;

import java.util.Random;

/**
 * Deals with dropping mutton if a Goat is killed.
 */
public class GoatDeath {
	public void execute(EntityDeathEvent e) {
		Random r = new Random();
		int dropCount = r.nextInt(3) + 1;
		if (AranarthUtils.getMonth() == Month.FAUNIVOR) {
			dropCount = dropCount + 1;
		}

		if (e.getEntity().isVisualFire()) {
			e.getEntity().getLocation().getWorld().dropItemNaturally(
					e.getEntity().getLocation(), new ItemStack(Material.COOKED_MUTTON, dropCount));
		} else {
			e.getEntity().getLocation().getWorld().dropItemNaturally(
					e.getEntity().getLocation(), new ItemStack(Material.MUTTON, dropCount));
		}
	}
}
