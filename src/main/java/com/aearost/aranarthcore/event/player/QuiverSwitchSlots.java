package com.aearost.aranarthcore.event.player;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.Objects;

/**
 * Prevents players from adding anything into a Quiver.
 */
public class QuiverSwitchSlots {
	public void execute(InventoryClickEvent e) {
		// If the user did not click a slot
		if (e.getClickedInventory() == null) {
			return;
		}

		ItemStack cursorItem = e.getCursor();
		ItemStack clickedItem = e.getClickedInventory().getItem(e.getSlot());
		if (Objects.isNull(clickedItem)) {
			return;
		}

		Inventory inventory = e.getClickedInventory();
		if (inventory.getHolder() instanceof Player player) {
			int clickedSlot = e.getSlot();
			ItemStack clickedClone = clickedItem.clone();
			ItemStack cursorClone = cursorItem.clone();

			// Trying to pick up items holding the quiver
			if (cursorItem.getType() == Material.LIGHT_GRAY_BUNDLE) {
				if (Objects.nonNull(cursorItem.getItemMeta()) && cursorItem.getItemMeta().hasLore()) {
					e.setCancelled(true);
					inventory.setItem(clickedSlot, cursorClone);
					player.setItemOnCursor(clickedClone);
				}
			}
			// Trying to place items inside the quiver while holding the item
			else if (clickedItem.getType() == Material.LIGHT_GRAY_BUNDLE
					&& cursorItem.getType() != Material.AIR) {
				if (Objects.nonNull(clickedItem.getItemMeta()) && clickedItem.getItemMeta().hasLore()) {
					e.setCancelled(true);
					inventory.setItem(clickedSlot, cursorClone);
					player.setItemOnCursor(clickedClone);
				}
			}
		}
	}
}
