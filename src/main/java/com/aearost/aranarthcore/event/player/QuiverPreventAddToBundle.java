package com.aearost.aranarthcore.event.player;

import java.util.Objects;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;

import com.aearost.aranarthcore.AranarthCore;

public class QuiverPreventAddToBundle implements Listener {

	public QuiverPreventAddToBundle(AranarthCore plugin) {
		Bukkit.getPluginManager().registerEvents(this, plugin);
	}

	/**
	 * Prevents players from adding non-potion items to the potion inventory.
	 * @param e The event.
	 */
	@EventHandler
	public void onGuiClick(final InventoryClickEvent e) {
		// If the user did not click a slot
		if (e.getClickedInventory() == null) {
			return;
		}

		// If adding a new item to the blacklist
		if (e.getClick() == ClickType.RIGHT || e.getClick() == ClickType.SHIFT_RIGHT
				|| e.getClick() == ClickType.CREATIVE) {
			ItemStack cursorItem = e.getCursor();
			ItemStack clickedItem = e.getClickedInventory().getItem(e.getSlot());
			if (Objects.isNull(clickedItem)) {
				return;
			}
			
			ItemStack currentItem = e.getCurrentItem();
			if (Objects.isNull(currentItem)) {
				return;
			}
			
			if (cursorItem.getType() == Material.BUNDLE) {
				if (Objects.nonNull(cursorItem.getItemMeta()) && cursorItem.getItemMeta().hasLore()) {
					e.setCancelled(true);
				} else if (clickedItem.getType() == Material.BUNDLE) {
					e.setCancelled(true);
				}
			}
		}
	}
}
