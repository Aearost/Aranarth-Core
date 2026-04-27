package com.aearost.aranarthcore.event.listener.grouped;

import com.aearost.aranarthcore.AranarthCore;
import io.papermc.paper.event.entity.EntityMoveEvent;
import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.persistence.PersistentDataType;

import static com.aearost.aranarthcore.objects.CustomKeys.ARROW;

/**
 * Handles all logic regarding preventing specified explosions.
 */
public class RootingArrowMovePrevent implements Listener {

	public RootingArrowMovePrevent(AranarthCore plugin) {
		Bukkit.getPluginManager().registerEvents(this, plugin);
	}
	
	/**
	 * Deals with cancelling explosion block damage.
	 * @param e The event.
	 */
	@EventHandler
	public void onPlayerMove(final PlayerMoveEvent e) {
		if (e.getPlayer().getPersistentDataContainer() != null) {
			String type = e.getPlayer().getPersistentDataContainer().get(ARROW, PersistentDataType.STRING);
			if (type != null && type.equals("rooting")) {
				e.setCancelled(true);
			}
		}
	}
	
	
	/**
	 * Deals with cancelling explosion item damage.
	 * @param e The event.
	 */
	@EventHandler
	public void onEntityMove(final EntityMoveEvent e) {
		if (e.getEntity().getPersistentDataContainer() != null) {
			String type = e.getEntity().getPersistentDataContainer().get(ARROW, PersistentDataType.STRING);
			if (type != null && type.equals("rooting")) {
				e.setCancelled(true);
			}
		}
	}
}
