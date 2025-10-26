package com.aearost.aranarthcore.event.player;

import com.aearost.aranarthcore.objects.Dominion;
import com.aearost.aranarthcore.utils.ChatUtils;
import com.aearost.aranarthcore.utils.DominionUtils;
import org.bukkit.Chunk;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerMoveEvent;

/**
 * Handles Dominion enter and exit messages.
 */
public class DominionChunkChange {

	public void execute(PlayerMoveEvent e) {
		// If they did not move to a different coordinate and only their mouse
		if (e.getTo() == null) {
			return;
		}

		if (e.getTo().getWorld().getName().startsWith("world")) {
			Chunk from = e.getFrom().getChunk();
			Chunk to = e.getTo().getChunk();
			// If it's the same chunk
			if (from.getX() == to.getX() && from.getZ() == to.getZ() && from.getWorld().getName().equals(to.getWorld().getName())) {
				return;
			} else {
				Dominion dominionFrom = DominionUtils.getDominionOfChunk(from);
				Dominion dominionTo = DominionUtils.getDominionOfChunk(to);
				Player player = e.getPlayer();
				// If entering a dominion
				if (dominionFrom == null && dominionTo != null) {
					player.sendMessage(ChatUtils.chatMessage("&7You have entered the dominion of &e" + dominionTo.getName()));
				}
				// If exiting a dominion
				else if (dominionFrom != null && dominionTo == null) {
					player.sendMessage(ChatUtils.chatMessage("&7You have exited the dominion of &e" + dominionFrom.getName()));
				}
				// If entering one dominion next to another
				else if (dominionFrom != null && dominionTo != null) {
					player.sendMessage(ChatUtils.chatMessage("&7You have exited the dominion of &e" + dominionFrom.getName()));
					player.sendMessage(ChatUtils.chatMessage("&7You have entered the dominion of &e" + dominionTo.getName()));
				}
			}
		}
	}
}
