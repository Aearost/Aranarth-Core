package com.aearost.aranarthcore.event.world;

import org.bukkit.event.player.PlayerItemDamageEvent;

/**
 * Prevents durability from being affected in the arena world.
 */
public class ArenaDurabilityPrevent {
	public void execute(final PlayerItemDamageEvent e) {
		e.setCancelled(true);
	}
}
