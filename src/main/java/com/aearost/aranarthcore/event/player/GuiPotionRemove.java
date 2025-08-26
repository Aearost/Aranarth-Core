package com.aearost.aranarthcore.event.player;

import com.aearost.aranarthcore.objects.AranarthPlayer;
import com.aearost.aranarthcore.utils.AranarthUtils;
import com.aearost.aranarthcore.utils.ChatUtils;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;

/**
 * Removes an input quantity of a potion from the player's stored potions.
 */
public class GuiPotionRemove {
	public void execute(InventoryClickEvent e) {
		if (e.getView().getType() == InventoryType.CHEST) {
			// If the user did not click a slot
			if (e.getClickedInventory() == null) {
				return;
			}
			
			// If clicking an item in the player's inventory
			if (e.getClickedInventory().getType() == InventoryType.PLAYER) {
				e.setCancelled(true);
			}
			// Clicking in the potions inventory
			else {
				ItemStack clickedItem = e.getClickedInventory().getItem(e.getSlot());
				if (Objects.isNull(clickedItem)) {
					e.setCancelled(true);
					return;
				}

				AranarthPlayer aranarthPlayer = AranarthUtils.getPlayer(e.getWhoClicked().getUniqueId());
				HashMap<ItemStack, Integer> potionsAndAmounts = AranarthUtils.getPotionsAndAmounts((Player) e.getWhoClicked());
				List<ItemStack> potions = aranarthPlayer.getPotions();
				int currentAmount = potionsAndAmounts.get(clickedItem);
				int toBeRemoved = aranarthPlayer.getPotionQuantityToRemove();

				List<Integer> indexesToRemove = new ArrayList<>();

				// Finds which potions in the list will be removed
				for (int i = 0; i < potions.size(); i++) {
					if (toBeRemoved == 0) {
						break;
					}

					if (potions.get(i).isSimilar(clickedItem)) {
						indexesToRemove.add(i);
						toBeRemoved--;
					}
				}

				// Removes the potions that were selected
				for (int index : indexesToRemove) {
					potions.remove(index);
				}

				aranarthPlayer.setPotions(potions);
				e.getWhoClicked().sendMessage(ChatUtils.chatMessage("&7You have removed &e" + aranarthPlayer.getPotionQuantityToRemove() + " &7of the potion!"));
				AranarthUtils.setPlayer(e.getWhoClicked().getUniqueId(), aranarthPlayer);

				e.setCancelled(true);
				e.getWhoClicked().closeInventory();
			}
		}
	}
	
}
