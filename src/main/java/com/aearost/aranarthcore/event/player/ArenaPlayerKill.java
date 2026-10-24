package com.aearost.aranarthcore.event.player;

import com.aearost.aranarthcore.utils.AranarthUtils;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.PlayerDeathEvent;

/**
 * Restores a user's health once they kill another player in the arena world.
 */
public class ArenaPlayerKill {
	public void execute(PlayerDeathEvent e) {
		Player victim = e.getEntity();
		Player killer = AranarthUtils.getKillingPlayer(e);
		if (killer == null || killer.equals(victim) || killer.isDead()
				|| !killer.getWorld().getName().equalsIgnoreCase("arena")) {
			return;
		}

		AttributeInstance maxHealth = killer.getAttribute(Attribute.MAX_HEALTH);
		killer.setHealth(maxHealth != null ? maxHealth.getValue() : 20);
		// Prevents any lingering fire from immediately taking away the restored health
		killer.setFireTicks(0);
	}
}
