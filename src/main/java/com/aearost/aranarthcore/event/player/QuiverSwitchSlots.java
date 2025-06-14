package com.aearost.aranarthcore.event.player;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
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
		Bukkit.getLogger().info("A");
		if (inventory.getHolder() instanceof Player player) {
			int clickedSlot = e.getSlot();
			ItemStack clickedClone = clickedItem.clone();
			ItemStack cursorClone = cursorItem.clone();

			Bukkit.getLogger().info("A");
			// Trying to pick up items holding the quiver
			if (cursorItem.getType().name().endsWith("_BUNDLE")) {
				Bukkit.getLogger().info("B");
				if (cursorItem.hasItemMeta()) {
					Bukkit.getLogger().info("C");
					ItemMeta cursorMeta = cursorItem.getItemMeta();
					if (cursorMeta.getPersistentDataContainer().has(QUIVER)) {
						Bukkit.getLogger().info("D");
						e.setCancelled(true);
						inventory.setItem(clickedSlot, cursorClone);
						player.setItemOnCursor(clickedClone);
					}
				}
			} else if (clickedItem.getType().name().endsWith("_BUNDLE") && cursorItem.getType() != Material.AIR) {
				Bukkit.getLogger().info("E");
				if (clickedItem.hasItemMeta()) {
					ItemMeta clickedMeta = clickedItem.getItemMeta();
					Bukkit.getLogger().info("F");
					if (clickedMeta.getPersistentDataContainer().has(QUIVER)) {
						Bukkit.getLogger().info("G");
						e.setCancelled(true);
						inventory.setItem(clickedSlot, cursorClone);
						player.setItemOnCursor(clickedClone);
					}
				}

			}

//			if (cursorItem.getType() == Material.LIGHT_GRAY_BUNDLE) {
//				if (Objects.nonNull(cursorItem.getItemMeta()) && cursorItem.getItemMeta().hasLore()) {
//					e.setCancelled(true);
//					inventory.setItem(clickedSlot, cursorClone);
//					player.setItemOnCursor(clickedClone);
//				}
//			}
//			// Trying to place items inside the quiver while holding the item
//			else if (clickedItem.getType() == Material.LIGHT_GRAY_BUNDLE
//					&& cursorItem.getType() != Material.AIR) {
//				if (Objects.nonNull(clickedItem.getItemMeta()) && clickedItem.getItemMeta().hasLore()) {
//					e.setCancelled(true);
//					inventory.setItem(clickedSlot, cursorClone);
//					player.setItemOnCursor(clickedClone);
//				}
//			}
		}
	}
}
