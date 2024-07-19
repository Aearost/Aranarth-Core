package com.aearost.aranarthcore.event;

import org.bukkit.Bukkit;
import org.bukkit.entity.Item;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.bukkit.event.entity.EntityExplodeEvent;

import com.aearost.aranarthcore.AranarthCore;

public class CreeperExplodeDeny implements Listener {

	public CreeperExplodeDeny(AranarthCore plugin) {
		Bukkit.getPluginManager().registerEvents(this, plugin);
	}
	
	/**
	 * Deals with cancelling explosion block damage
	 * 
	 * @author Aearost
	 *
	 */
	@EventHandler
	public void onCreeperExplodeBlock(final EntityExplodeEvent e) {
		e.setCancelled(true);
	}
	
	
	/**
	 * Deals with cancelling explosion item damage
	 * 
	 * @author Aearost
	 *
	 */
	@EventHandler
	public void onCreeperExplodeItem(final EntityDamageEvent e) {
		if (e.getEntity() instanceof Item) {
			if (e.getCause() == DamageCause.BLOCK_EXPLOSION || e.getCause() == DamageCause.ENTITY_EXPLOSION) {
				e.setCancelled(true);
			}
		}
	}

}
