package com.aearost.aranarthcore.event.mob;

import com.aearost.aranarthcore.AranarthCore;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.EntityType;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityPickupItemEvent;

public class EntityEggPickupCancel implements Listener {

	public EntityEggPickupCancel(AranarthCore plugin) {
		Bukkit.getPluginManager().registerEvents(this, plugin);
	}

	/**
	 * Deals with cancelling zombies, baby zombies, and zombie villagers from
	 * picking up eggs which prevented despawning.
	 * @param e The event.
	 */
	@EventHandler
	public void onEggPickupCancel(final EntityPickupItemEvent e) {
		if (e.getItem().getItemStack().getType() == Material.EGG) {
			if (e.getEntityType() == EntityType.ZOMBIE || e.getEntityType() == EntityType.ZOMBIE_VILLAGER) {
				e.setCancelled(true);
			}
		}
	}

}
