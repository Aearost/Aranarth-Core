package com.aearost.aranarthcore.event.mob;

import org.bukkit.Location;
import org.bukkit.entity.*;
import org.bukkit.event.entity.CreatureSpawnEvent;

import java.util.Random;

/**
 * Handles extra baby spawn rates during the month of Calorvor.
 */
public class BabyMobSpawn {
	public void execute(CreatureSpawnEvent e) {
		if (e.getEntity() instanceof Ageable ageable) {
			if (ageable instanceof Animals animal) {
				if (!animal.isAdult()) {
					int rand = new Random().nextInt(2);
					// 50% chance of having a twin
					if (rand == 0) {
						Location loc = e.getLocation();
						Animals twin = (Animals) loc.getWorld().spawnEntity(loc, e.getEntityType());
						twin.setBaby();
					}
				}
			}
		}
	}
}
