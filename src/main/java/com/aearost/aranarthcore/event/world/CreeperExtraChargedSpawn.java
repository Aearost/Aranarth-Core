package com.aearost.aranarthcore.event.world;

import com.aearost.aranarthcore.enums.Month;
import com.aearost.aranarthcore.utils.AranarthUtils;
import org.bukkit.entity.Creeper;
import org.bukkit.event.entity.EntitySpawnEvent;

import java.util.Random;

/**
 * Spawns creepers as charged with a 12.5% chance during a thunderstorm in the month of Aestivor.
 */
public class CreeperExtraChargedSpawn {
	public void execute(EntitySpawnEvent e) {
		if (AranarthUtils.getMonth() == Month.AESTIVOR) {
			if (e.getLocation().getWorld().isThundering()) {
				// 12.5% chance of it being charged
				int rand = new Random().nextInt(8);
				if (rand == 0) {
					Creeper creeper = (Creeper) e.getEntity();
					creeper.setPowered(true);
				}
			}
		}
	}

}
