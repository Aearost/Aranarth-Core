package com.aearost.aranarthcore.event.player;

import com.aearost.aranarthcore.objects.Dominion;
import com.aearost.aranarthcore.utils.DominionUtils;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;

/**
 * Updates the food contents of the Dominion's Food inventory.
 */
public class GuiDominionFoodClose {
	public void execute(InventoryCloseEvent e) {
		Inventory inventory = e.getInventory();
		if (inventory.getContents().length > 0) {
			Player player = (Player) e.getPlayer();
			Dominion dominion = DominionUtils.getPlayerDominion(player.getUniqueId());
			player.playSound(player, Sound.BLOCK_CHEST_CLOSE, 1F, 1F);
			dominion.setFood(inventory.getContents());
			DominionUtils.updateDominion(dominion);
		}
	}
}
