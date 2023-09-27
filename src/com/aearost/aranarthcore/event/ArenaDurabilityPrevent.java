package com.aearost.aranarthcore.event;

import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerItemDamageEvent;

import com.aearost.aranarthcore.AranarthCore;

public class ArenaDurabilityPrevent implements Listener {

	public ArenaDurabilityPrevent(AranarthCore plugin) {
		Bukkit.getPluginManager().registerEvents(this, plugin);
	}

	/**
	 * Prevents durability from being affected in the arena world.
	 * 
	 * @param e
	 */
	@EventHandler
	public void onArenaBlockMelt(final PlayerItemDamageEvent e) {
		if (e.getPlayer().getWorld().getName().toLowerCase().equals("arena")) {
			e.setCancelled(true);
		}
	}
}
