package com.aearost.aranarthcore.event.listener.grouped;

import com.aearost.aranarthcore.AranarthCore;
import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;

/**
 * Prevents crops from being trampled by both players and other mobs
 */
public class DominionInteract implements Listener {

	public DominionInteract(AranarthCore plugin) {
		Bukkit.getPluginManager().registerEvents(this, plugin);
	}

	/**
	 * Prevents players from placing blocks in another Dominion.
	 */
	@EventHandler
	public void onPlace(BlockPlaceEvent e) {

	}

	/**
	 * Prevents players from breaking blocks in another Dominion.
	 */
	@EventHandler
	public void onBreak(BlockBreakEvent e) {

	}

	/**
	 * Prevents players from interacting with non-alive entities in another Dominion.
	 */
	@EventHandler
	public void onTrample(PlayerInteractEntityEvent e) {

		// ONLY DO THIS IF IT'S AN ITEM FRAME OR ARMOR STAND

	}

	/**
	 * All validation logic for interacting with another Dominion.
	 */
	private void applyLogic() {
		// If the player is attempting to place or break a block
//		if (e.getClickedBlock() != null) {
//			Dominion dominion = DominionUtils.getDominionOfChunk(e.getClickedBlock().getChunk());
//			// If the block is in a dominion
//			if (dominion != null) {
//				Dominion playerDominion = DominionUtils.getPlayerDominion(e.getPlayer().getUniqueId());
//				// If the player is not in the dominion of the block
//				if (playerDominion == null || !dominion.getOwner().equals(playerDominion.getOwner())) {
//					e.setCancelled(true);
//					e.getPlayer().sendMessage(ChatUtils.chatMessage("&cYou are not in the Dominion of &e" + dominion.getName()));
//				}
//			}
//		}
	}
}
