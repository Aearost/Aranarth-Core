package com.aearost.aranarthcore.event.mob;

import com.aearost.aranarthcore.utils.AranarthUtils;
import org.bukkit.event.entity.EntityDeathEvent;

import java.util.Random;

/**
 * Deals with dropping extra drops during the month of Faunivor.
 */
public class FaunivorExtraDeathDrops {
	public void execute(EntityDeathEvent e) {
		// 50% chance of increasing mob drops during Faunivor
		if (new Random().nextBoolean()) {
			AranarthUtils.increaseMobDrops(e);
		}
	}
}
