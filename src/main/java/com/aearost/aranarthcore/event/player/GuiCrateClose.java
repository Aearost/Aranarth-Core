package com.aearost.aranarthcore.event.player;

import com.aearost.aranarthcore.objects.AranarthPlayer;
import com.aearost.aranarthcore.utils.AranarthUtils;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryCloseEvent;

/**
 * Handles preventing any logic from being done while in the Crates GUIs.
 */
public class GuiCrateClose {
	public void execute(InventoryCloseEvent e) {
		Player player = (Player) e.getPlayer();
		AranarthPlayer aranarthPlayer = AranarthUtils.getPlayer(player.getUniqueId());
		aranarthPlayer.setIsOpeningCrateWithCyclingItem(false);
		AranarthUtils.setPlayer(player.getUniqueId(), aranarthPlayer);
		player.playSound(e.getPlayer(), Sound.BLOCK_CHEST_CLOSE, 1, 0.6F);
	}
}
