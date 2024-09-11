package com.aearost.aranarthcore.event.mob;

import com.aearost.aranarthcore.AranarthCore;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.EntityType;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.potion.PotionEffectType;

public class ZombieHorseSpawn implements Listener {

	public ZombieHorseSpawn(AranarthCore plugin) {
		Bukkit.getPluginManager().registerEvents(this, plugin);
	}
	
	/**
	 * Deals with spawning a zombie horse when a horse dies as it is
	 * killed with the Weakness potion effect.
	 * @param e The event.
	 */
	@EventHandler
	public void onHorseDeath(final EntityDeathEvent e) {
		if (e.getEntityType() == EntityType.HORSE) {
			if (e.getEntity().hasPotionEffect(PotionEffectType.WEAKNESS)) {
				World world = e.getEntity().getWorld();
				world.spawnEntity(e.getEntity().getLocation(), EntityType.ZOMBIE_HORSE);
				world.playSound(e.getEntity().getLocation(), Sound.ENTITY_LIGHTNING_BOLT_THUNDER, 10000F, 1F);
			}
		}
	}

}
