package com.aearost.aranarthcore.event;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.HumanEntity;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.CraftItemEvent;
import org.bukkit.inventory.ItemStack;

import com.aearost.aranarthcore.AranarthCore;
import com.aearost.aranarthcore.utils.ChatUtils;

public class CraftingOverrides implements Listener {

	public CraftingOverrides(AranarthCore plugin) {
		Bukkit.getPluginManager().registerEvents(this, plugin);
	}

	/**
	 * Handles cancelling improper crafting recipes
	 * 
	 * @param e
	 */
	@EventHandler
	public void onCraftChorusDiamond(final CraftItemEvent e) {
		HumanEntity player = e.getWhoClicked();
		for (ItemStack is : e.getInventory().getMatrix()) {
			
			if (is == null) {
				continue;
			}
			
			if (is.getType() == Material.DIAMOND) {
				boolean isHasLore = is.getItemMeta().hasLore();
				
				if (isHasLore) {
					if (e.getRecipe().getResult().getType() != Material.HEAVY_WEIGHTED_PRESSURE_PLATE) {
						e.setCancelled(true);
						player.sendMessage(ChatUtils.chatMessage("&cYou cannot use a Chorus Diamond to craft this!"));
						return;
					}
				} else {
					if (e.getRecipe().getResult().getType() == Material.HEAVY_WEIGHTED_PRESSURE_PLATE) {
						e.setCancelled(true);
						player.sendMessage(ChatUtils.chatMessage("&cYou must use a Chorus Diamond to craft this!"));
						return;
					}
				}
				
				
			} else if (is.getType() == Material.COBBLESTONE) {
				
				boolean isHasLore = is.getItemMeta().hasLore();
				
				// If the ingredient is compressed cobblestone
				if (isHasLore) {
					if (e.getRecipe().getResult().getType() == Material.COBBLESTONE) {
						// If trying to craft more compressed cobblestone
						if (e.getRecipe().getResult().getAmount() == 1) {
							e.setCancelled(true);
							player.sendMessage(ChatUtils.chatMessage("&cYou cannot use compressed cobblestone to craft this!"));
							return;
						}
					} else {
						e.setCancelled(true);
						player.sendMessage(ChatUtils.chatMessage("&cYou cannot use compressed cobblestone to craft this!"));
						return;
					}
				} else {
					// If trying to uncraft cobblestone for more cobblestone
					if (e.getRecipe().getResult().getAmount() == 9) {
						e.setCancelled(true);
						player.sendMessage(ChatUtils.chatMessage("&cYou must use compressed cobblestone to uncraft this!"));
						return;
					}
				}
			}
		}
}}
