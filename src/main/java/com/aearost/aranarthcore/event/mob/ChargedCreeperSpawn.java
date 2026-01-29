package com.aearost.aranarthcore.event.mob;

import com.aearost.aranarthcore.enums.Month;
import com.aearost.aranarthcore.utils.AranarthUtils;
import org.bukkit.entity.Creeper;
import org.bukkit.event.entity.EntitySpawnEvent;

import java.util.Random;

/**
 * Spawns creepers as charged during thunderstorms and increased rates during Aestivor.
 */
public class ChargedCreeperSpawn {
	public void execute(EntitySpawnEvent e) {
		if (e.getLocation().getWorld().isThundering()) {

			if (AranarthUtils.getMonth() == Month.AESTIVOR) {
				// 20% chance of it being charged
				if (new Random().nextInt(5) == 0) {
					Creeper creeper = (Creeper) e.getEntity();
					creeper.setPowered(true);
				}
			} else {
				// 5% chance of it being charged in any other month
				if (new Random().nextInt(20) == 0) {
					Creeper creeper = (Creeper) e.getEntity();
					creeper.setPowered(true);
				}
			}
		}
	}
}
