package com.aearost.aranarthcore.event.listener.grouped;

import com.aearost.aranarthcore.AranarthCore;
import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBurnEvent;
import org.bukkit.event.block.BlockSpreadEvent;

/**
 * Handles all logic regarding preventing fire spread and burning.
 */
public class FireProtection implements Listener {

	public FireProtection(AranarthCore plugin) {
		Bukkit.getPluginManager().registerEvents(this, plugin);
	}

	/**
	 * Prevents fire from spreading.
	 */
	@EventHandler
	public void onFireSpread(BlockSpreadEvent e) {
		String worldName = e.getBlock().getWorld().getName();
		if (worldName.startsWith("world") || worldName.startsWith("smp") || worldName.startsWith("resource")) {
			e.setCancelled(true);
		}
	}

	/**
	 * Prevents fire from burning blocks in Dominions.
	 */
	@EventHandler
	public void onFireBurn(BlockBurnEvent e) {
		String worldName = e.getBlock().getWorld().getName();
		if (worldName.startsWith("world") || worldName.startsWith("smp") || worldName.startsWith("resource")) {
			e.setCancelled(true);
		}
	}

}
