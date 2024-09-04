package com.aearost.aranarthcore.event.player;

import com.aearost.aranarthcore.AranarthCore;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;

public class ArenaPlayerKill implements Listener {

	public ArenaPlayerKill(AranarthCore plugin) {
		Bukkit.getPluginManager().registerEvents(this, plugin);
	}

	/**
	 * Restores a user's health once they kill another player in the arena world.
	 * @param e The event.
	 */
	@EventHandler
	public void onPlayerKill(final EntityDamageByEntityEvent e) {
		if (e.getDamager().getWorld().getName().equalsIgnoreCase("arena")) {
			if (e.getDamager() instanceof Player killer) {
				if (e.getEntity() instanceof Player player) {
					if (player.getHealth() - e.getDamage() <= 0) {
						killer.setHealth(20);
					}
				}
			}
		}
	}
}
