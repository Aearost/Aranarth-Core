package com.aearost.aranarthcore.event;

import org.bukkit.Bukkit;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.Horse;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.CreatureSpawnEvent;

import com.aearost.aranarthcore.AranarthCore;

public class HorseSpawn implements Listener {

	public HorseSpawn(AranarthCore plugin) {
		Bukkit.getPluginManager().registerEvents(this, plugin);
	}

	/**
	 * Deals with overriding the default spawn behaviour for horses
	 * 
	 * @author Aearost
	 *
	 */
	@EventHandler
	public void onHorseSpawn(final CreatureSpawnEvent e) {

		if (e.getEntity() instanceof Horse) {
			Horse horse = (Horse) e.getEntity();

			// A maximum limit of 30 hearts (60 half-hearts) --> 60
			// Will need a minimum of 8 hearts (16 half-hearts)
			horse.getAttribute(Attribute.GENERIC_MAX_HEALTH).setBaseValue(60);
			
			// A maximum limit of 8 blocks of jump --> 1.28
			// A minimum limit of 2 blocks of jump --> 0.57
			horse.getAttribute(Attribute.HORSE_JUMP_STRENGTH).setBaseValue(0.57);
			
			// A maximum limit of 25 m/s --> 0.592417062
			// A minimum limit of 8 m/s --> 0.19
			horse.getAttribute(Attribute.GENERIC_MOVEMENT_SPEED).setBaseValue(0.19);
			
			// For testing only!!! Remove before final compile
			horse.setAdult();
			horse.setTamed(true);
		}
		
	}

}
