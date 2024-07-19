package com.aearost.aranarthcore.event;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Axolotl;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Fish;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityTargetEvent;

import com.aearost.aranarthcore.AranarthCore;

public class AxolotlPreventFishDamage implements Listener {

	public AxolotlPreventFishDamage(AranarthCore plugin) {
		Bukkit.getPluginManager().registerEvents(this, plugin);
	}

	/**
	 * Ensures that axolotls do not target fish in the enchanted cave at Stashenleda.
	 * 
	 * @param e
	 */
	@EventHandler
	public void onAxolotlDamage(final EntityTargetEvent e) {
		
		if (e.getEntity().getType() == EntityType.AXOLOTL && e.getTarget() instanceof Fish) {
			Location location = e.getEntity().getLocation();
			if (location.getWorld().getName().equals("world")) {
				int x = location.getBlockX();
				int z = location.getBlockZ();
				if (x >= -90 && x <= -62 && z >= -232 && z <= -183) {
					e.setCancelled(true);
				}
			}
		}
	}
	
	
	/**
	 * Ensures that axolotls do not attack fish in the enchanted cave at Stashenleda.
	 * 
	 * @param e
	 */
	@EventHandler
	public void onAxolotlDamage(final EntityDamageByEntityEvent  e) {
		if (e.getDamageSource().getCausingEntity() instanceof Axolotl) {
			Location location = e.getEntity().getLocation();
			if (location.getWorld().getName().equals("world")) {
				int x = location.getBlockX();
				int z = location.getBlockZ();
				if (x >= -90 && x <= -62 && z >= -232 && z <= -183) {
					e.setCancelled(true);
				}
			}
		}
	}
}
