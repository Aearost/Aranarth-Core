package com.aearost.aranarthcore.event.listener.grouped;

import com.aearost.aranarthcore.AranarthCore;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityInteractEvent;
import org.bukkit.event.player.PlayerInteractEvent;

/**
 * Prevents crops from being trampled by both players and other mobs
 */
public class SoilTrampleListener implements Listener {

	public SoilTrampleListener(AranarthCore plugin) {
		Bukkit.getPluginManager().registerEvents(this, plugin);
	}

	/**
	 * Prevents crops from being trampled by a player.
	 */
	@EventHandler
	public void onTrample(PlayerInteractEvent e) {
		if (e.getAction() == Action.PHYSICAL && e.getClickedBlock().getType() == Material.FARMLAND) {
			e.setCancelled(true);
		}
	}

	/**
	 * Prevents crops from being trampled by mobs
	 */
	@EventHandler
	public void onTrample(EntityInteractEvent e) {
		if (e.getBlock().getType() == Material.FARMLAND) {
			e.setCancelled(true);
		}
	}
}
