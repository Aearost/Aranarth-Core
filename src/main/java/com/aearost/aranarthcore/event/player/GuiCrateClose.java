package com.aearost.aranarthcore.event.player;

import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryCloseEvent;

/**
 * Handles preventing any logic from being done while in the Crates GUIs.
 */
public class GuiCrateClose {
	public void execute(InventoryCloseEvent e) {
		Player player = (Player) e.getPlayer();
		player.playSound(e.getPlayer().getLocation(), Sound.BLOCK_CHEST_CLOSE, 1, 0.6F);
	}
}
