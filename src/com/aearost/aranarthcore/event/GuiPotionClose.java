package com.aearost.aranarthcore.event;

import java.util.Arrays;
import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import com.aearost.aranarthcore.AranarthCore;
import com.aearost.aranarthcore.utils.AranarthUtils;
import com.aearost.aranarthcore.utils.ChatUtils;

public class GuiPotionClose implements Listener {

	public GuiPotionClose(AranarthCore plugin) {
		Bukkit.getPluginManager().registerEvents(this, plugin);
	}

	/**
	 * Adds the input potions to the player's potion inventory.
	 * 
	 * @param e
	 */
	@EventHandler
	public void onPotionInventoryClose(final InventoryCloseEvent e) {
		if (ChatUtils.stripColor(e.getView().getTitle()).equals("Potions")) {
			Inventory inventory = e.getInventory();
			if (inventory.getContents().length > 0) {
				Player player = (Player) e.getPlayer();
				
				List<ItemStack> potions = AranarthUtils.getPotions(player.getUniqueId());
				List<ItemStack> inventoryPotions = Arrays.asList(inventory.getContents());
				for (ItemStack inventoryPotion : inventoryPotions) {
					potions.add(inventoryPotion);
				}
				AranarthUtils.updatePotions(player.getUniqueId(), inventoryPotions);
			}
		}
		
	}
}
