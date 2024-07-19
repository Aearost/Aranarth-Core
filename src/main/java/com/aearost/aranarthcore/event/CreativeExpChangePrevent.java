package com.aearost.aranarthcore.event;

import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerExpChangeEvent;

import com.aearost.aranarthcore.AranarthCore;

public class CreativeExpChangePrevent implements Listener {

	public CreativeExpChangePrevent(AranarthCore plugin) {
		Bukkit.getPluginManager().registerEvents(this, plugin);
	}

	/**
	 * Prevents EXP from being dropped in the arena world
	 * 
	 * @param e
	 */
	@EventHandler
	public void onArenaItemDrop(final PlayerExpChangeEvent e) {
		if (e.getPlayer().getLocation().getWorld().getName().toLowerCase().equals("creative")) {
			e.setAmount(0);
		}
	}

}
