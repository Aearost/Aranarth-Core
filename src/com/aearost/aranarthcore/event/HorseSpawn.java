package com.aearost.aranarthcore.event;

import java.util.Random;

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
	 * Determines the values in brackets of probability
	 * 
	 * @author Aearost
	 *
	 */
	@EventHandler
	public void onHorseSpawn(final CreatureSpawnEvent e) {

		if (e.getEntity() instanceof Horse) {
			Horse horse = (Horse) e.getEntity();
			Random r = new Random();
			
			// A maximum limit of 30 hearts (60 half-hearts) --> 60
			// Will need a minimum of 8 hearts (16 half-hearts)
			final int healthBracket = r.nextInt(10) + 1;
			final int healthMin;
			final int healthMax;
			if (healthBracket < 5) {
				healthMin = 16;
				healthMax = 34;
			} else if (healthBracket < 9) {
				healthMin = 35;
				healthMax = 44;
			} else {
				healthMin = 45;
				healthMax = 60;
			}
			final int healthValue = r.nextInt((healthMax - healthMin) + 1) + healthMin;
			horse.getAttribute(Attribute.GENERIC_MAX_HEALTH).setBaseValue(healthValue);
			
			// A maximum limit of 8 blocks of jump --> 1.28
			// A minimum limit of 2 blocks of jump --> 0.57
			final int jumpBracket = r.nextInt(10) + 1;
			final double jumpMin;
			final double jumpMax;
			if (jumpBracket < 5) {
				jumpMin = 0.57;
				jumpMax = 0.84;
			} else if (jumpBracket < 9) {
				jumpMin = 0.85;
				jumpMax = 0.99;
			} else {
				jumpMin = 1.00;
				jumpMax = 1.28;
			}
			final double jumpValue = jumpMin + (jumpMax - jumpMin) * r.nextDouble();
			horse.getAttribute(Attribute.HORSE_JUMP_STRENGTH).setBaseValue(jumpValue);
			
			// A maximum limit of 25 m/s --> 0.592417062
			// A minimum limit of 8 m/s --> 0.19
			final int speedBracket = r.nextInt(10) + 1;
			final double speedMin;
			final double speedMax;
			if (speedBracket < 5) {
				speedMin = 0.19;
				speedMax = 0.24;
			} else if (speedBracket < 9) {
				speedMin = 0.25;
				speedMax = 0.44;
			} else {
				speedMin = 0.45;
				speedMax = 0.592417062;
			}
			final double speedValue = speedMin + (speedMax - speedMin) * r.nextDouble();
			horse.getAttribute(Attribute.GENERIC_MOVEMENT_SPEED).setBaseValue(speedValue);
			
			// For testing only!!! Remove before final compile
			// horse.setAdult();
			// horse.setTamed(true);
		}
		
	}

}
