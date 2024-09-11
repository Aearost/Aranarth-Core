package com.aearost.aranarthcore.event.player;

import com.aearost.aranarthcore.AranarthCore;
import com.aearost.aranarthcore.utils.ChatUtils;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.ItemStack;

import java.util.Objects;

public class GuiQuiverPreventNonArrowAdd implements Listener {

	public GuiQuiverPreventNonArrowAdd(AranarthCore plugin) {
		Bukkit.getPluginManager().registerEvents(this, plugin);
	}

	/**
	 * Prevents players from adding non-arrow items to the arrows inventory.
	 * @param e The event.
	 */
	@EventHandler
	public void onGuiClick(final InventoryClickEvent e) {
		if (ChatUtils.stripColorFormatting(e.getView().getTitle()).equals("Quiver") && e.getView().getType() == InventoryType.CHEST) {
			// If the user did not click a slot
			if (e.getClickedInventory() == null) {
				return;
			}
			
			// If adding a new item to the arrows inventory
			if (e.getClickedInventory().getSize() == 41) {
				ItemStack clickedItem = e.getClickedInventory().getItem(e.getSlot());
				// Ensures a non-empty slot is clicked
				if (Objects.isNull(clickedItem)) {
					// If placing potion back into player slots
					if (Objects.nonNull(e.getCursor())) {
						return;
					}
					e.setCancelled(true);
				}
				
				if (!isItemArrow(clickedItem)) {
					e.setCancelled(true);
				}
			}
		}
	}
	
	private boolean isItemArrow(ItemStack item) {
		return (item.getType() == Material.ARROW
				|| item.getType() == Material.TIPPED_ARROW
				|| item.getType() == Material.SPECTRAL_ARROW);
	}
}
