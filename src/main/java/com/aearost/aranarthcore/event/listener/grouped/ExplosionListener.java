package com.aearost.aranarthcore.event.listener.grouped;

import com.aearost.aranarthcore.AranarthCore;
import com.aearost.aranarthcore.objects.Dominion;
import com.aearost.aranarthcore.utils.AranarthUtils;
import com.aearost.aranarthcore.utils.DefenderUtils;
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
			if (e.getEntity() instanceof Creeper creeper) {
				// Defender creepers: allow explosion entity damage but clear block list
				if (DefenderUtils.isDefender(creeper.getUniqueId())) {
					e.blockList().clear();
					return;
				}
				// Non-defender creepers: cancel entirely (existing behavior)
				e.setCancelled(true);
				return;
			}

			Dominion chunkDominion = DominionUtils.getDominionOfChunk(e.getEntity().getLocation().getChunk());
			if (chunkDominion != null) {
				if (chunkDominion.isExplosionEnabled()) {
					e.blockList().removeIf(block -> AranarthUtils.getLockedContainerAtBlock(block) != null);
				} else {
					e.setCancelled(true);
				}
			} else {
				e.blockList().removeIf(block -> AranarthUtils.getLockedContainerAtBlock(block) != null);
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
				Dominion chunkDominion = DominionUtils.getDominionOfChunk(e.getEntity().getLocation().getChunk());
				if (chunkDominion != null && !chunkDominion.isExplosionEnabled()) {
					e.setCancelled(true);
				}
			}
		}
	}

}
