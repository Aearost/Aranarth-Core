package com.aearost.aranarthcore.event.listener.grouped;

import com.aearost.aranarthcore.AranarthCore;
import com.aearost.aranarthcore.objects.Dominion;
import com.aearost.aranarthcore.utils.DominionUtils;
import org.bukkit.Bukkit;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.bukkit.event.entity.EntityExplodeEvent;

/**
 * Handles all logic regarding preventing specified explosions.
 */
public class ExplosionListener implements Listener {

	public ExplosionListener(AranarthCore plugin) {
		Bukkit.getPluginManager().registerEvents(this, plugin);
	}
	
	/**
	 * Deals with cancelling explosion block damage.
	 * @param e The event.
	 */
	@EventHandler
	public void onExplodeBlock(final EntityExplodeEvent e) {
		if (!(e.getEntity() instanceof WindCharge) && !(e.getEntity() instanceof BreezeWindCharge)) {
			// Always prevent these explosions
			if (e.getEntity() instanceof Creeper) {
				e.setCancelled(true);
				return;
			}

			Dominion chunkDominion = DominionUtils.getDominionOfChunk(e.getEntity().getLocation().getChunk());
			if (chunkDominion != null) {
				e.setCancelled(true);
			}
		}
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
