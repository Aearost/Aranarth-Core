package com.aearost.aranarthcore.event.mob;

import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.EntityType;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.potion.PotionEffectType;

/**
 * Deals with spawning a zombie horse when a horse dies as it is
 * killed with the Weakness potion effect.
 */
public class ZombieHorseSpawn {
	public void execute(EntityDeathEvent e) {
		if (e.getEntity().hasPotionEffect(PotionEffectType.WEAKNESS)) {
			World world = e.getEntity().getWorld();
			world.spawnEntity(e.getEntity().getLocation(), EntityType.ZOMBIE_HORSE);
			world.playSound(e.getEntity().getLocation(), Sound.ENTITY_LIGHTNING_BOLT_THUNDER, 10000F, 1F);
		}
	}
}
