package com.aearost.aranarthcore.event.mob;

import org.bukkit.entity.EntityType;
import org.bukkit.event.entity.EntitySpawnEvent;

import java.util.Random;

/**
 * Spawns Phantoms at an increased quantity.
 */
public class ObscurvorPhantomSpawn {
	public void execute(EntitySpawnEvent e) {
		if (e.getEntityType() == EntityType.PHANTOM) {
			if (new Random().nextBoolean()) {
				e.getLocation().getWorld().spawnEntity(e.getLocation(), EntityType.PHANTOM);
			}
		}
	}

}
