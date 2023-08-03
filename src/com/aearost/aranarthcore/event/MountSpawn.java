package com.aearost.aranarthcore.event;

import java.util.Random;

import org.bukkit.Bukkit;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.AbstractHorse;
import org.bukkit.entity.Camel;
import org.bukkit.entity.Horse;
import org.bukkit.entity.SkeletonHorse;
import org.bukkit.entity.ZombieHorse;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.CreatureSpawnEvent;

import com.aearost.aranarthcore.AranarthCore;

public class MountSpawn implements Listener {

	public MountSpawn(AranarthCore plugin) {
		Bukkit.getPluginManager().registerEvents(this, plugin);
	}

	/**
	 * Deals with overriding the default spawn behaviour for horses and camels
	 * Determines the values in brackets of probability
	 * 
	 * @author Aearost
	 *
	 */
	@EventHandler
	public void onHorseSpawn(final CreatureSpawnEvent e) {

		if (e.getEntity() instanceof AbstractHorse) {
			AbstractHorse horse = null;
			if (e.getEntity() instanceof Horse) {
				horse = (Horse) e.getEntity();
			} else if (e.getEntity() instanceof SkeletonHorse) {
				horse = (SkeletonHorse) e.getEntity();
			} else if (e.getEntity() instanceof ZombieHorse) {
				horse = (ZombieHorse) e.getEntity();
			} else {
				// Donkeys, Mules, Llamas, etc
				return;
			}
			
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
			
			// Without this, skeleton horses and zombie horses will not be rideable
			// and will spawn with very low health
			if (horse instanceof SkeletonHorse || horse instanceof ZombieHorse) {
				horse.setTamed(true);
				horse.setHealth(horse.getAttribute(Attribute.GENERIC_MAX_HEALTH).getValue());
			}
			
			// For testing only!!! Remove before final compile
			// horse.setAdult();
			// horse.setTamed(true);
		} else if (e.getEntity() instanceof Camel) {
			Camel camel = (Camel) e.getEntity();
			Random r = new Random();
			
			// A maximum limit of 40 hearts (80 half-hearts) --> 80
			// Will need a minimum of 8 hearts (16 half-hearts)
			final int healthBracket = r.nextInt(10) + 1;
			final int healthMin;
			final int healthMax;
			if (healthBracket < 5) {
				healthMin = 20;
				healthMax = 34;
			} else if (healthBracket < 9) {
				healthMin = 35;
				healthMax = 54;
			} else {
				healthMin = 55;
				healthMax = 80;
			}
			final int healthValue = r.nextInt((healthMax - healthMin) + 1) + healthMin;
			camel.getAttribute(Attribute.GENERIC_MAX_HEALTH).setBaseValue(healthValue);
			
			// A maximum limit of 4.5 blocks of jump --> 0.909
			// A minimum limit of 1 blocks of jump --> 0.382
			final int jumpBracket = r.nextInt(10) + 1;
			final double jumpMin;
			final double jumpMax;
			if (jumpBracket < 5) {
				jumpMin = 0.382;
				jumpMax = 0.44;
			} else if (jumpBracket < 9) {
				jumpMin = 0.45;
				jumpMax = 0.84;
			} else {
				jumpMin = 0.85;
				jumpMax = 0.909;
			}
			final double jumpValue = jumpMin + (jumpMax - jumpMin) * r.nextDouble();
			camel.getAttribute(Attribute.HORSE_JUMP_STRENGTH).setBaseValue(jumpValue);
			
			// A maximum limit of 18 m/s --> 0.428
			// A minimum limit of 8 m/s --> 0.19
			final int speedBracket = r.nextInt(10) + 1;
			final double speedMin;
			final double speedMax;
			if (speedBracket < 5) {
				speedMin = 0.19;
				speedMax = 0.24;
			} else if (speedBracket < 9) {
				speedMin = 0.25;
				speedMax = 0.34;
			} else {
				speedMin = 0.35;
				speedMax = 0.428;
			}
			final double speedValue = speedMin + (speedMax - speedMin) * r.nextDouble();
			camel.getAttribute(Attribute.GENERIC_MOVEMENT_SPEED).setBaseValue(speedValue);
			
			// For testing only!!! Remove before final compile
			// camel.setAdult();
			// camel.setTamed(true);
		}
		
	}

}
