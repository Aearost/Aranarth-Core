package com.aearost.aranarthcore.event.mob;

import org.bukkit.entity.Pillager;
import org.bukkit.event.entity.CreatureSpawnEvent;

/**
 * Deals with preventing pillagers from spawning by their outpost locations.
 */
public class PillagerOutpostSpawnCancel {

	public void execute(CreatureSpawnEvent e) {
		if (e.getEntity() instanceof Pillager) {
			int x = e.getLocation().getBlockX();
			int z = e.getLocation().getBlockZ();
			
			if (x >= 20630 && x <= 20810 && z >= -17920 && z <= -17760) {
				e.setCancelled(true);
			} else if (x >= -692350 && x <= 692800 && z >= 700140 && z <= 700740) {
				e.setCancelled(true);
			}
		}
	}
}
