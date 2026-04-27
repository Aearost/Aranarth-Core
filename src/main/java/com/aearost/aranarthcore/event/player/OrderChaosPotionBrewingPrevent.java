package com.aearost.aranarthcore.event.player;

import com.aearost.aranarthcore.enums.Month;
import com.aearost.aranarthcore.utils.AranarthUtils;
import com.aearost.aranarthcore.utils.ChatUtils;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.ItemStack;

/**
 * Limits the brewing of the Potion of Order and the Potion of Chaos to the month of Florivor.
 */
public class OrderChaosPotionBrewingPrevent {
	public void execute(InventoryClickEvent e) {
		if (e.getClickedInventory().getType() == InventoryType.PLAYER) {
			if (e.getWhoClicked() instanceof Player player) {
				if (AranarthUtils.getMonth() != Month.FLORIVOR) {
					ItemStack clickedItem = e.getClickedInventory().getItem(e.getSlot());
					// Right shift click in inventory
					// Left AND right click in inventory
					if (clickedItem != null) {
						if (clickedItem.getType() == Material.OPEN_EYEBLOSSOM || clickedItem.getType() == Material.CLOSED_EYEBLOSSOM) {
							ItemStack copy = clickedItem.clone();
							e.setCancelled(true);
							e.getView().getTopInventory().setItem(3, null);
							player.getInventory().setItem(e.getSlot(), copy);
							player.sendMessage(ChatUtils.chatMessage("&cYou can only use this ingredient during Florivor"));
							// Sometimes shift-clicks result in a lost eyeblossom - this refunds it
							if (e.isShiftClick()) {
								player.getInventory().addItem(new ItemStack(clickedItem.getType()));
							}
							return;
						}
					}

					ItemStack ingredientItem = e.getView().getTopInventory().getItem(3);
					// Left shift click in inventory
					if (ingredientItem != null) {
						if (ingredientItem.getType() == Material.OPEN_EYEBLOSSOM || ingredientItem.getType() == Material.CLOSED_EYEBLOSSOM) {
							e.setCancelled(true);
							ItemStack copy = ingredientItem.clone();
							e.getView().getTopInventory().setItem(3, null);
							player.getInventory().setItem(e.getSlot(), copy);
							player.sendMessage(ChatUtils.chatMessage("&cYou can only use this ingredient during Florivor"));
						}
					}
				}
			}
		}
	}
}
