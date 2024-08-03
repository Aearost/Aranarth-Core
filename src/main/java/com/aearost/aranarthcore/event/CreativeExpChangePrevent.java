package com.aearost.aranarthcore.event;

import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerExpChangeEvent;

import com.aearost.aranarthcore.AranarthCore;

import java.util.Objects;

public class CreativeExpChangePrevent implements Listener {

	public CreativeExpChangePrevent(AranarthCore plugin) {
		Bukkit.getPluginManager().registerEvents(this, plugin);
	}

	/**
	 * Prevents EXP from being dropped in the creative world.
	 * @param e The event.
	 */
	@EventHandler
	public void onCreativeExpDrop(final PlayerExpChangeEvent e) {
		if (Objects.requireNonNull(e.getPlayer().getLocation().getWorld()).getName().equalsIgnoreCase("creative")) {
			e.setAmount(0);
		}
	}

}
