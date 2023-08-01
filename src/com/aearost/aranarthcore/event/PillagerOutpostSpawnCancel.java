package com.aearost.aranarthcore.event;

import org.bukkit.Bukkit;
import org.bukkit.block.Biome;
import org.bukkit.entity.Pillager;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.CreatureSpawnEvent;

import com.aearost.aranarthcore.AranarthCore;

public class PillagerOutpostSpawnCancel implements Listener {

	public PillagerOutpostSpawnCancel(AranarthCore plugin) {
		Bukkit.getPluginManager().registerEvents(this, plugin);
	}

	/**
	 * Deals with preventing pillagers from spawning in Dren al-Sahra
	 * 
	 * @author Aearost
	 *
	 */
	@EventHandler
	public void onPillagerSpawn(final CreatureSpawnEvent e) {

		if (e.getEntity() instanceof Pillager) {
			if (e.getLocation().getBlock().getBiome() == Biome.DESERT) {
				int x = e.getLocation().getBlockX();
				int z = e.getLocation().getBlockZ();
				
				if (x >= 20630 && x <= 20810 && z >= -17920 && z <= -17760) {
					e.setCancelled(true);
				}
				
			}
		}
	}

}
