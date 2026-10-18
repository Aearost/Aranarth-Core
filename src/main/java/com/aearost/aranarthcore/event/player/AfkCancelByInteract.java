package com.aearost.aranarthcore.event.player;

import com.aearost.aranarthcore.objects.AranarthPlayer;
import com.aearost.aranarthcore.utils.AranarthUtils;
import org.bukkit.entity.Player;

/**
 * Handles canceling a player's AFK status, or resetting their AFK timer, when they perform any real activity.
 */
public class AfkCancelByInteract {

	public void execute(Player player) {
		AranarthPlayer aranarthPlayer = AranarthUtils.getPlayer(player.getUniqueId());
		if (aranarthPlayer == null || aranarthPlayer.getAfkLocation() == null) {
			return;
		}

		if (aranarthPlayer.getAfkLocation().getSeconds() >= AranarthUtils.getAfkSecondsAmount()) {
			AranarthUtils.toggleAfkStatus(player.getUniqueId(), false);
		}
		// Reset their AFK timer so activity without changing blocks still counts
		else {
			aranarthPlayer.setAfkLocation(null);
			AranarthUtils.setPlayer(player.getUniqueId(), aranarthPlayer);
		}
	}
}
