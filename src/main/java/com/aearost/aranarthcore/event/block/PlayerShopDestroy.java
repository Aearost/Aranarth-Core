package com.aearost.aranarthcore.event.block;

import com.aearost.aranarthcore.AranarthCore;
import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.SignChangeEvent;

public class PlayerShopDestroy implements Listener {

	public PlayerShopDestroy(AranarthCore plugin) {
		Bukkit.getPluginManager().registerEvents(this, plugin);
	}

	/**
	 * Handles the deletion of a player shop.
	 * @param e The event.
	 */
	@EventHandler
	public void onPlayerShopDestroy(final SignChangeEvent e) {

	}

}
