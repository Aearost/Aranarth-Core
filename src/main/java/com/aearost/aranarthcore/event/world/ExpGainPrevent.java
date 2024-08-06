package com.aearost.aranarthcore.event.world;

import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerExpChangeEvent;

import com.aearost.aranarthcore.AranarthCore;

public class ExpGainPrevent implements Listener {

	public ExpGainPrevent(AranarthCore plugin) {
		Bukkit.getPluginManager().registerEvents(this, plugin);
	}

	/**
	 * Prevents EXP from being picked up in the arena and creative world.
	 * @param e The event.
	 */
	@EventHandler
	public void onExpDrop(final PlayerExpChangeEvent e) {
		if (e.getPlayer().getLocation().getWorld().getName().equalsIgnoreCase("creative")
		|| e.getPlayer().getLocation().getWorld().getName().equalsIgnoreCase("arena")) {
			e.setAmount(0);
		}
	}

}
