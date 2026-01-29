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
		if (e.getLocation().getWorld().isThundering()) {

			if (AranarthUtils.getMonth() == Month.AESTIVOR) {
				// 12.5% chance of it being charged
				if (new Random().nextInt(8) == 0) {
					Creeper creeper = (Creeper) e.getEntity();
					creeper.setPowered(true);
				}
			} else {
				// 50% chance of it being charged in any other month
				if (new Random().nextInt(50) == 0) {
					Creeper creeper = (Creeper) e.getEntity();
					creeper.setPowered(true);
				}
			}
		}
	}
}
