package com.aearost.aranarthcore.event.player;

import com.aearost.aranarthcore.gui.GuiDominionFood;
import com.aearost.aranarthcore.gui.GuiHolder;
import com.aearost.aranarthcore.objects.AranarthPlayer;
import com.aearost.aranarthcore.objects.Dominion;
import com.aearost.aranarthcore.utils.AranarthUtils;
import com.aearost.aranarthcore.utils.DominionUtils;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.UUID;

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
		GuiHolder holder = GuiHolder.of(inventory);
		if (holder == null || !(holder.getData() instanceof UUID dominionId)) {
			return;
		}
		// Only the player holding the lock may write, otherwise two open copies could both be saved
		if (!DominionUtils.isFoodInventoryLockedBy(dominionId, player.getUniqueId())) {
			Bukkit.getLogger().warning("[AC] " + player.getName() + " closed a Dominion food inventory without holding its lock, the changes were discarded");
			return;
		}
		Dominion dominion = DominionUtils.getDominionById(dominionId);
		if (dominion == null) {
			DominionUtils.unlockFoodInventory(dominionId);
			return;
		}
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

		// Compact the full food array and release the lock
		ItemStack[] compacted = DominionUtils.compactFoodArray(dominion.getFood());
		dominion.setFood(compacted);
		DominionUtils.unlockFoodInventory(dominion.getId());
		DominionUtils.updateDominion(dominion);
	}
}
