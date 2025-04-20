package com.aearost.aranarthcore.event.world;

import com.aearost.aranarthcore.AranarthCore;
import com.aearost.aranarthcore.utils.AranarthUtils;
import org.bukkit.Bukkit;
import org.bukkit.entity.Creeper;
import org.bukkit.entity.EntityType;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntitySpawnEvent;

import java.util.Random;

public class CreeperExtraChargedSpawn implements Listener {

	public CreeperExtraChargedSpawn(AranarthCore plugin) {
		Bukkit.getPluginManager().registerEvents(this, plugin);
	}

	/**
	 * Spawns creepers as charged with a 10% chance during a thunderstorm in the month of Aestivor.
	 * @param e The event.
	 */
	@EventHandler
	public void onCreeperSpawn(final EntitySpawnEvent e) {
		if (AranarthUtils.getMonth() == 4) {
			if (e.getEntityType() == EntityType.CREEPER) {
				if (e.getLocation().getWorld().isThundering()) {
					// 10% chance of it being charged
					int rand = new Random().nextInt(10);
					if (rand == 0) {
						Creeper creeper = (Creeper) e.getEntity();
						creeper.setPowered(true);
					}
				}
			}
		}
	}

}
