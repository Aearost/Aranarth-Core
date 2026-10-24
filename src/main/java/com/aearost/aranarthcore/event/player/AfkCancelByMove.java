package com.aearost.aranarthcore.event.player;

import org.bukkit.event.player.PlayerMoveEvent;

/**
 * Handles canceling a player's AFK status, or resetting their AFK timer, if they move or look around.
 */
public class AfkCancelByMove {

	public void execute(PlayerMoveEvent e) {
		// Fires for both position changes (WASD) and rotation-only changes (mouse movement)
		if (e.getTo() == null) {
			return;
		}
		new AfkCancelByInteract().execute(e.getPlayer());
	}
}
