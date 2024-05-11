package com.aearost.aranarthcore.event;

import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.LingeringPotionSplashEvent;
import org.bukkit.event.entity.PotionSplashEvent;
import org.bukkit.event.player.PlayerItemConsumeEvent;

import com.aearost.aranarthcore.AranarthCore;

public class PotionConsume implements Listener {

	public PotionConsume(AranarthCore plugin) {
		Bukkit.getPluginManager().registerEvents(this, plugin);
	}

	/**
	 * Handles the auto-refill functionality when consuming of regular potions.
	 * 
	 * @author Aearost
	 *
	 */
	@EventHandler
	public void onPotionUse(final PlayerItemConsumeEvent e) {
		System.out.println("A");
	}

	/**
	 * Handles the auto-refill functionality when throwing splash potions.
	 * 
	 * @author Aearost
	 *
	 */
	@EventHandler
	public void onPotionUse(final PotionSplashEvent e) {
		System.out.println("B");
	}
	
	/**
	 * Handles the auto-refill functionality when throwing of lingering potions.
	 * 
	 * @author Aearost
	 *
	 */
	@EventHandler
	public void onPotionUse(final LingeringPotionSplashEvent e) {
		System.out.println("C");
	}

}
