package com.aearost.aranarthcore.event;

import org.bukkit.Bukkit;
import org.bukkit.entity.Item;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.bukkit.event.entity.EntityExplodeEvent;

import com.aearost.aranarthcore.AranarthCore;

public class ExplosionPrevent implements Listener {

	public ExplosionPrevent(AranarthCore plugin) {
		Bukkit.getPluginManager().registerEvents(this, plugin);
	}
	
	/**
	 * Deals with cancelling explosion block damage.
	 * @param e The event.
	 */
	@EventHandler
	public void onExplodeBlock(final EntityExplodeEvent e) {
		e.setCancelled(true);
	}
	
	
	/**
	 * Deals with cancelling explosion item damage.
	 * @param e The event.
	 */
	@EventHandler
	public void onExplodeItem(final EntityDamageEvent e) {
		if (e.getEntity() instanceof Item) {
			if (e.getCause() == DamageCause.BLOCK_EXPLOSION || e.getCause() == DamageCause.ENTITY_EXPLOSION) {
				e.setCancelled(true);
			}
		}
	}

}
