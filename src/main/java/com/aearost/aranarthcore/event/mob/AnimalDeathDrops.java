package com.aearost.aranarthcore.event.mob;

import com.aearost.aranarthcore.AranarthCore;
import com.aearost.aranarthcore.enums.Month;
import com.aearost.aranarthcore.utils.AranarthUtils;
import org.bukkit.Bukkit;
import org.bukkit.entity.Animals;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.inventory.ItemStack;

import java.util.Random;

public class AnimalDeathDrops implements Listener {

	public AnimalDeathDrops(AranarthCore plugin) {
		Bukkit.getPluginManager().registerEvents(this, plugin);
	}
	
	/**
	 * Deals with dropping extra drops during the month of Faunivor.
	 * @param e The event.
	 */
	@EventHandler
	public void onAnimalDeath(final EntityDeathEvent e) {
		if (AranarthUtils.getMonth() == Month.FAUNIVOR) {
			if (e.getEntity() instanceof Animals) {
				for (ItemStack drop : e.getDrops()) {
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
	}

}
