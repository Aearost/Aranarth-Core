package com.aearost.aranarthcore.event.mob;

import com.aearost.aranarthcore.AranarthCore;
import org.bukkit.Bukkit;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;

public class BabyZombiePreventVillagerDamage implements Listener {

	public BabyZombiePreventVillagerDamage(AranarthCore plugin) {
		Bukkit.getPluginManager().registerEvents(this, plugin);
	}
	
	/**
	 * Prevents baby zombies and baby husks from attacking villagers.
	 * @param e The event.
	 */
	@EventHandler
	public void onZombieAttackVillager(final EntityDamageByEntityEvent e) {
		if (e.getEntity() instanceof Villager villager) {
			if (e.getDamager() instanceof Zombie zombie) {
                if (!zombie.isAdult()) {
					e.setCancelled(true);
				}
			}
		}
	}
}
