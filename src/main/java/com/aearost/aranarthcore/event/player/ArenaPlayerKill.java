package com.aearost.aranarthcore.event.player;

import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageByEntityEvent;

/**
 * Restores a user's health once they kill another player in the arena world.
 */
public class ArenaPlayerKill {
	public void execute(EntityDamageByEntityEvent e) {
		if (e.getDamager() instanceof Player killer) {
			if (e.getEntity() instanceof Player player) {
				if (player.getHealth() - e.getDamage() <= 0) {
					killer.setHealth(20);
				}
			}
		}
	}
}
