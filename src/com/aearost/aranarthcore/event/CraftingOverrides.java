package com.aearost.aranarthcore.event;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.HumanEntity;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.CraftItemEvent;
import org.bukkit.event.inventory.PrepareItemCraftEvent;
import org.bukkit.inventory.ItemStack;

import com.aearost.aranarthcore.AranarthCore;
import com.aearost.aranarthcore.utils.ChatUtils;

public class CraftingOverrides implements Listener {

	public CraftingOverrides(AranarthCore plugin) {
		Bukkit.getPluginManager().registerEvents(this, plugin);
	}
	
	@EventHandler
	public void preCraftitem(final PrepareItemCraftEvent e) {
		int nullCounter = 0;
		int sugarcaneBlockCounter = 0;
		for (ItemStack is : e.getInventory().getMatrix()) {
			
			if (is == null) {
				nullCounter++;
				continue;
			}
			
			if (is.getType() == Material.BAMBOO_BLOCK && is.getItemMeta().hasLore()) {
				sugarcaneBlockCounter++;
			}
		}
		if (nullCounter == 8 && sugarcaneBlockCounter == 1) {
			e.getInventory().setResult(new ItemStack(Material.SUGAR_CANE, 9));
		}
	}

	/**
	 * Handles cancelling improper crafting recipes
	 * 
	 * @param e
	 */
	@EventHandler
	public void onCraftItem(final CraftItemEvent e) {
		HumanEntity player = e.getWhoClicked();
		for (ItemStack is : e.getInventory().getMatrix()) {
			
			if (is == null) {
				continue;
			}
			
			boolean isHasLore = is.getItemMeta().hasLore();
			
			if (is.getType() == Material.DIAMOND) {
				
				// If it's used in a recipe that it shouldn't be used in
				if (isHasLore) {
					if (e.getRecipe().getResult().getType() != Material.HEAVY_WEIGHTED_PRESSURE_PLATE) {
						e.setCancelled(true);
						player.sendMessage(ChatUtils.chatMessageError("You cannot use a Chorus Diamond to craft this!"));
						return;
					}
				}
				// If the vanilla item is used instead of the custom item
				else {
					if (e.getRecipe().getResult().getType() == Material.HEAVY_WEIGHTED_PRESSURE_PLATE) {
						e.setCancelled(true);
						player.sendMessage(ChatUtils.chatMessageError("You must use a Chorus Diamond to craft this!"));
						return;
					}
				}
				
				
			} else if (is.getType() == Material.BAMBOO_BLOCK) {
				
				if (isHasLore) {
					// Must verify the result is not bamboo either as the two recipes overload each other
					if (e.getRecipe().getResult().getType() != Material.SUGAR_CANE &&
							e.getRecipe().getResult().getType() != Material.BAMBOO) {
						e.setCancelled(true);
						player.sendMessage(ChatUtils.chatMessageError("You cannot use a Sugarcane Block to craft this!"));
						return;
					}
				} else {
					if (e.getRecipe().getResult().getType() == Material.SUGAR_CANE) {
						e.setCancelled(true);
						player.sendMessage(ChatUtils.chatMessageError("You must use a Sugarcane Block to craft this!"));
						return;
					}
				}
			}
		}
	}
}
