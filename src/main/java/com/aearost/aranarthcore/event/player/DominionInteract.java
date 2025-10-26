package com.aearost.aranarthcore.event.player;

import com.aearost.aranarthcore.objects.Dominion;
import com.aearost.aranarthcore.utils.ChatUtils;
import com.aearost.aranarthcore.utils.DominionUtils;
import org.bukkit.event.player.PlayerInteractEvent;

/**
 * Handles the interacting with blocks within a Dominion.
 */
public class DominionInteract {

	public void execute(PlayerInteractEvent e) {
		// If the player is attempting to place or break a block
		if (e.getClickedBlock() != null) {
			Dominion dominion = DominionUtils.getDominionOfChunk(e.getClickedBlock().getChunk());
			// If the block is in a dominion
			if (dominion != null) {
				Dominion playerDominion = DominionUtils.getPlayerDominion(e.getPlayer().getUniqueId());
				// If the player is not in the dominion of the block
				if (playerDominion == null || !dominion.getOwner().equals(playerDominion.getOwner())) {
					e.setCancelled(true);
					e.getPlayer().sendMessage(ChatUtils.chatMessage("&cYou are not in the Dominion of &e" + dominion.getName()));
				}
			}
		}

	}

}
