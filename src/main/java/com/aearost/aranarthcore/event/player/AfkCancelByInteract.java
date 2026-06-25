package com.aearost.aranarthcore.event.player;

import com.aearost.aranarthcore.objects.AranarthPlayer;
import com.aearost.aranarthcore.utils.AranarthUtils;
import org.bukkit.entity.Player;

/**
 * Handles canceling a player's AFK status when they interact with an inventory or container.
 */
public class AfkCancelByInteract {

	public void execute(Player player) {
		AranarthPlayer aranarthPlayer = AranarthUtils.getPlayer(player.getUniqueId());
		if (aranarthPlayer.getAfkLocation() != null
				&& aranarthPlayer.getAfkLocation().getSeconds() >= AranarthUtils.getAfkSecondsAmount()) {
			AranarthUtils.toggleAfkStatus(player.getUniqueId(), false);
		}
	}
}
