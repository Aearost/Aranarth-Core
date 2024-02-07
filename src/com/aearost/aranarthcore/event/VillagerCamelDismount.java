package com.aearost.aranarthcore.event;

import org.bukkit.Bukkit;
import org.bukkit.entity.Camel;
import org.bukkit.entity.Entity;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.spigotmc.event.entity.EntityDismountEvent;

import com.aearost.aranarthcore.AranarthCore;

public class VillagerCamelDismount implements Listener {

	public VillagerCamelDismount(AranarthCore plugin) {
		Bukkit.getPluginManager().registerEvents(this, plugin);
	}

	/**
	 * Removes a villager that is riding a camel when the player dismounts.
	 * 
	 * @param e
	 */
	@EventHandler
	public void onDismount(final EntityDismountEvent e) {
		if (e.getDismounted() instanceof Camel) {
			Camel camel = (Camel) e.getDismounted();
			for (Entity entity : camel.getPassengers()) {
				camel.removePassenger(entity);
			}
		}
	}
	
}
