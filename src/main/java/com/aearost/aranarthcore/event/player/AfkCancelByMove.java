package com.aearost.aranarthcore.event.player;

import com.aearost.aranarthcore.objects.AranarthPlayer;
import com.aearost.aranarthcore.utils.AranarthUtils;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerMoveEvent;

/**
 * Handles canceling a player's AFK status if they move.
 */
public class AfkCancelByMove {

	public void execute(PlayerMoveEvent e) {
		// If they did not move to a different coordinate and only their mouse
		if (e.getTo() == null) {
			return;
		}

		Player player = e.getPlayer();
		AranarthPlayer aranarthPlayer = AranarthUtils.getPlayer(player.getUniqueId());
		// The player moves while they are AFK
		if (aranarthPlayer.getAfkLocation() != null
				&& aranarthPlayer.getAfkLocation().getSeconds() >= AranarthUtils.getAfkSecondsAmount()) {
			AranarthUtils.toggleAfkStatus(player.getUniqueId(), false);
		}
	}
}
