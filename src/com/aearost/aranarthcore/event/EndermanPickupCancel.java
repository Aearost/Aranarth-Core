package com.aearost.aranarthcore.event;

import org.bukkit.Bukkit;
import org.bukkit.entity.EntityType;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityChangeBlockEvent;

import com.aearost.aranarthcore.AranarthCore;

public class EndermanPickupCancel implements Listener {

	public EndermanPickupCancel(AranarthCore plugin) {
		Bukkit.getPluginManager().registerEvents(this, plugin);
	}
	
	/**
	 * Deals with cancelling endermen picking up blocks
	 * 
	 * @author Aearost
	 *
	 */
	@EventHandler
	public void onEndermanPickupBlock(final EntityChangeBlockEvent e) {
		if (e.getEntityType() == EntityType.ENDERMAN) {
			e.setCancelled(true);
		}
	}

}
