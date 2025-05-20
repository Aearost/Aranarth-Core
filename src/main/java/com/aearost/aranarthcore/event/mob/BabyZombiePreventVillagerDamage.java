package com.aearost.aranarthcore.event.mob;

import org.bukkit.entity.*;
import org.bukkit.event.entity.EntityDamageByEntityEvent;

/**
 * Prevents baby zombies and baby husks from attacking villagers.
 */
public class BabyZombiePreventVillagerDamage {
	public void execute(EntityDamageByEntityEvent e) {
		if (e.getDamager() instanceof Zombie zombie) {
			if (!zombie.isAdult()) {
				e.setCancelled(true);
			}
		}
	}
}
