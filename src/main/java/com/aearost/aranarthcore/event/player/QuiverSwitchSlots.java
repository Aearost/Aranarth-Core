package com.aearost.aranarthcore.event.player;

import org.bukkit.Material;
import org.bukkit.entity.HumanEntity;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.Objects;

import static com.aearost.aranarthcore.items.CustomItemKeys.QUIVER;

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


		int clickedSlot = e.getSlot();
		ItemStack clickedClone = clickedItem.clone();
		ItemStack cursorClone = cursorItem.clone();
		HumanEntity player = e.getWhoClicked();

		// Trying to pick up items holding the quiver
		if (cursorItem.getType().name().endsWith("_BUNDLE")) {
			if (cursorItem.hasItemMeta()) {
				ItemMeta cursorMeta = cursorItem.getItemMeta();
				if (cursorMeta.getPersistentDataContainer().has(QUIVER)) {
					e.setCancelled(true);
					inventory.setItem(clickedSlot, cursorClone);
					player.setItemOnCursor(clickedClone);
				}
			}
		} else if (clickedItem.getType().name().endsWith("_BUNDLE") && cursorItem.getType() != Material.AIR) {
			if (clickedItem.hasItemMeta()) {
				ItemMeta clickedMeta = clickedItem.getItemMeta();
				if (clickedMeta.getPersistentDataContainer().has(QUIVER)) {
					e.setCancelled(true);
					inventory.setItem(clickedSlot, cursorClone);
					player.setItemOnCursor(clickedClone);
				}
			}
		}
	}
}
