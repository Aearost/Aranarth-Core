package com.aearost.aranarthcore.event.mob;

import com.aearost.aranarthcore.AranarthCore;
import org.bukkit.Bukkit;
import org.bukkit.entity.Pillager;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.CreatureSpawnEvent;

public class PillagerOutpostSpawnCancel implements Listener {

	public PillagerOutpostSpawnCancel(AranarthCore plugin) {
		Bukkit.getPluginManager().registerEvents(this, plugin);
	}

	/**
	 * Deals with preventing pillagers from spawning by their outpost locations.
	 * @param e The event.
	 */
	@EventHandler
	public void onPillagerSpawn(final CreatureSpawnEvent e) {
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
