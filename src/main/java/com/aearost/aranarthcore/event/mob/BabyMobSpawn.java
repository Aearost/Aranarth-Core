package com.aearost.aranarthcore.event.mob;

import org.bukkit.Location;
import org.bukkit.entity.Ageable;
import org.bukkit.entity.Animals;
import org.bukkit.entity.Tameable;
import org.bukkit.event.entity.EntityBreedEvent;

import java.util.Random;

/**
 * Handles extra baby spawn rates during the month of Calorvor.
 */
public class BabyMobSpawn {
	public void execute(EntityBreedEvent e) {
		if (e.getEntity() instanceof Ageable ageable) {
			if (ageable instanceof Animals animal) {
				if (!animal.isAdult()) {
					int rand = new Random().nextInt(2);
					// 50% chance of having a twin
					if (rand == 0) {
						Location loc = e.getEntity().getLocation();

						if (e.getEntity() instanceof Tameable babyTameable) {
							if (e.getMother() instanceof Tameable mother && e.getFather() instanceof Tameable father) {
								if (mother.isTamed() && father.isTamed()) {
									babyTameable.setTamed(true);
									Tameable twin = (Tameable) loc.getWorld().spawnEntity(loc, e.getEntityType());
									twin.setTamed(true);
									twin.setBaby();
									twin.setOwner(babyTameable.getOwner());
									return;
								}
							}
						}

						Animals twin = (Animals) loc.getWorld().spawnEntity(loc, e.getEntityType());
						twin.setBaby();
					}
				}
			}
		}
	}
}
