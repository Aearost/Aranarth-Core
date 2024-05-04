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
		System.out.println("A");
		if (ChatUtils.stripColor(e.getView().getTitle()).equals("Potions")) {
			System.out.println("B");
			// If the user did not click a slot
			if (e.getClickedInventory() == null) {
				System.out.println("C");
				return;
			}
			
			// If adding a new item to the blacklist
			if (e.getClickedInventory().getSize() == 41) {
				System.out.println("D");
				ItemStack clickedItem = e.getClickedInventory().getItem(e.getSlot());
				// Ensures a non-empty slot is clicked
				if (Objects.isNull(clickedItem)) {
					// If placing potion back into player slots
					if (Objects.nonNull(e.getCursor())) {
						return;
					}
					System.out.println("E");
					e.setCancelled(true);
					return;
				}
				
				if (clickedItem.getType() != Material.POTION
						&& clickedItem.getType() != Material.SPLASH_POTION
						&& clickedItem.getType() != Material.LINGERING_POTION) {
					System.out.println("F");
					e.setCancelled(true);
					return;
				}
			}
			System.out.println("G");
		}
	}
	
}
