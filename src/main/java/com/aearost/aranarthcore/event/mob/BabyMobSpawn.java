package com.aearost.aranarthcore.event.mob;

import com.aearost.aranarthcore.AranarthCore;
import com.aearost.aranarthcore.enums.Month;
import com.aearost.aranarthcore.utils.AranarthUtils;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.CreatureSpawnEvent;

import java.util.Random;

public class BabyMobSpawn implements Listener {

	public BabyMobSpawn(AranarthCore plugin) {
		Bukkit.getPluginManager().registerEvents(this, plugin);
	}

	/**
	 * Handles extra baby spawn rates during the month of Calorvor.
	 * @param e The event.
	 */
	@EventHandler
	public void onBabySpawn(final CreatureSpawnEvent e) {
		if (AranarthUtils.getMonth() == Month.CALORVOR) {
			if (e.getSpawnReason() == CreatureSpawnEvent.SpawnReason.BREEDING) {
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
	}
}
