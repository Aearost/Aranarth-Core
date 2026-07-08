package com.aearost.aranarthcore.event.player;

import com.aearost.aranarthcore.gui.GuiDominionFood;
import com.aearost.aranarthcore.objects.AranarthPlayer;
import com.aearost.aranarthcore.objects.Dominion;
import com.aearost.aranarthcore.utils.AranarthUtils;
import com.aearost.aranarthcore.utils.DominionUtils;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

/**
 * Updates the food contents of the Dominion's Food inventory.
 */
public class GuiDominionFoodClose {
	public void execute(InventoryCloseEvent e) {
		Inventory inventory = e.getInventory();
		if (inventory.getContents().length == 0) {
			return;
		}

		Player player = (Player) e.getPlayer();
		Dominion dominion = DominionUtils.getPlayerDominion(player.getUniqueId());
		player.playSound(player, Sound.BLOCK_CHEST_CLOSE, 1F, 1F);

		// Save the current page's items into the full food array
		if (dominion.getDominionLevel() >= 3) {
			// currentGuiPageNum still holds the old page here
			AranarthPlayer aranarthPlayer = AranarthUtils.getPlayer(player.getUniqueId());
			int currentPage = aranarthPlayer.getCurrentGuiPageNum();
			int foodOffset = currentPage * GuiDominionFood.FOOD_SLOTS_PER_PAGE;
			ItemStack[] food = dominion.getFood();
			for (int i = 0; i < GuiDominionFood.FOOD_SLOTS_PER_PAGE && foodOffset + i < food.length; i++) {
				food[foodOffset + i] = inventory.getItem(i);
			}
			dominion.setFood(food);
		} else {
			dominion.setFood(inventory.getContents());
		}

		// If this is just a page flip, persist the current state but skip compact and unlock
		if (DominionUtils.isFoodNavigating(player.getUniqueId())) {
			DominionUtils.updateDominion(dominion);
			return;
		}

		// Real closure - compact the full food array and release the lock
		ItemStack[] compacted = DominionUtils.compactFoodArray(dominion.getFood());
		dominion.setFood(compacted);
		DominionUtils.unlockFoodInventory(dominion.getId());
		DominionUtils.updateDominion(dominion);
	}
}
