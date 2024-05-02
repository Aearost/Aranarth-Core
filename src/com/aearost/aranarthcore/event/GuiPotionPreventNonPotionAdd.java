package com.aearost.aranarthcore.event;

import java.util.Objects;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;

import com.aearost.aranarthcore.AranarthCore;
import com.aearost.aranarthcore.utils.ChatUtils;

public class GuiPotionPreventNonPotionAdd implements Listener {

	public GuiPotionPreventNonPotionAdd(AranarthCore plugin) {
		Bukkit.getPluginManager().registerEvents(this, plugin);
	}

	/**
	 * Prevents players from adding non-potion items to the potion inventory
	 * 
	 * @author Aearost
	 *
	 */
	@EventHandler
	public void onGuiClick(final InventoryClickEvent e) {
		if (ChatUtils.stripColor(e.getView().getTitle()).equals("Potions")) {
			// If the user did not click a slot
			if (e.getClickedInventory() == null) {
				return;
			}
			
			// If adding a new item to the blacklist
			if (e.getClickedInventory().getSize() == 41) {
				ItemStack clickedItem = e.getClickedInventory().getItem(e.getSlot());
				// Ensures a non-empty slot is clicked
				if (Objects.isNull(clickedItem)) {
					return;
				}
				
				if (clickedItem.getType() != Material.POTION
						&& clickedItem.getType() != Material.SPLASH_POTION
						&& clickedItem.getType() != Material.LINGERING_POTION) {
					return;
				}
			}
		}
	}
	
}
