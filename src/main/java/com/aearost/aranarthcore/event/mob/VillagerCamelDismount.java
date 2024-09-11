package com.aearost.aranarthcore.event.mob;

import com.aearost.aranarthcore.AranarthCore;
import org.bukkit.Bukkit;
import org.bukkit.entity.Camel;
import org.bukkit.entity.Entity;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDismountEvent;

public class VillagerCamelDismount implements Listener {

	public VillagerCamelDismount(AranarthCore plugin) {
		Bukkit.getPluginManager().registerEvents(this, plugin);
	}

	/**
	 * Removes a villager that is riding a camel when the player dismounts.
	 * @param e The event.
	 */
	@EventHandler
	public void onDismount(final EntityDismountEvent e) {
		if (e.getDismounted() instanceof Camel camel) {
            for (Entity entity : camel.getPassengers()) {
				camel.removePassenger(entity);
			}
		}
	}
	
}
