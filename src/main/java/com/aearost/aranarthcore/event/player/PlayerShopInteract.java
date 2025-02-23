package com.aearost.aranarthcore.event.player;

import com.aearost.aranarthcore.AranarthCore;
import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.SignChangeEvent;

public class PlayerShopInteract implements Listener {

	public PlayerShopInteract(AranarthCore plugin) {
		Bukkit.getPluginManager().registerEvents(this, plugin);
	}

	/**
	 * Handles the interacting with a player shop.
	 * @param e The event.
	 */
	@EventHandler
	public void onPlayerShopInteract(final SignChangeEvent e) {

	}

}
