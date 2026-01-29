package com.aearost.aranarthcore.event.mob;

import org.bukkit.Location;
import org.bukkit.entity.EntityType;
import org.bukkit.event.entity.EntitySpawnEvent;

import java.util.Random;

/**
 * Spawns Breezes at low chances during the month of Ventivor.
 */
public class VentivorBreezeSpawn {
	public void execute(EntitySpawnEvent e) {
		if (isMobForBreezeReplacement(e)) {
			int amount = new Random().nextInt(50);
			if (amount == 0) {
				Location loc = e.getLocation().clone();
				e.setCancelled(true);
				loc.getWorld().spawnEntity(loc, EntityType.BREEZE);
			}
		}
	}

	/**
	 * Determines if the mob can be replaced by a Breeze.
	 * @param e The event.
	 * @return Confirmation if the mob can be replaced by a Breeze.
	 */
	private boolean isMobForBreezeReplacement(EntitySpawnEvent e) {
		EntityType entityType = e.getEntityType();
		String name = e.getLocation().getWorld().getName();
		if (!name.equals("world") && !name.equals("smp") && !name.equals("resource")) {
			return false;
		}

		return entityType == EntityType.ZOMBIE || entityType == EntityType.SKELETON || entityType == EntityType.SPIDER
				|| entityType == EntityType.CREEPER || entityType == EntityType.ENDERMAN;
	}
}
