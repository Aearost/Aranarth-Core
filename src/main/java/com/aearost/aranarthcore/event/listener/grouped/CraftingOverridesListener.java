package com.aearost.aranarthcore.event.listener.grouped;

import com.aearost.aranarthcore.AranarthCore;
import com.aearost.aranarthcore.event.crafting.*;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.HumanEntity;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.CraftItemEvent;
import org.bukkit.event.inventory.PrepareItemCraftEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import static com.aearost.aranarthcore.items.CustomItemKeys.*;
import static com.aearost.aranarthcore.items.CustomItemKeys.ARANARTHIUM_INGOT;

public class CraftingOverridesListener implements Listener {

	public CraftingOverridesListener(AranarthCore plugin) {
		Bukkit.getPluginManager().registerEvents(this, plugin);
	}

	/**
	 * Handles all overrides of default vanilla recipes to cater to custom recipes.
	 * @param e The event.
	 */
	@EventHandler
	public void preCraftItem(final PrepareItemCraftEvent e) {
		if (e.getInventory().contains(Material.BAMBOO_BLOCK)) {
			new CraftingOverridesSugarcaneBlock().preCraft(e);
		}
	}

	/**
	 * Handles cancelling improper crafting recipes.
	 * @param e The event.
	 */
	@EventHandler
	public void onCraftItem(final CraftItemEvent e) {
		HumanEntity player = e.getWhoClicked();
		ItemStack result = e.getRecipe().getResult();
		for (ItemStack ingredient : e.getInventory().getMatrix()) {

			if (ingredient == null) {
				continue;
			}

			// If vanilla item is used to craft custom item
			if (result.hasItemMeta()) {
				ItemMeta resultMeta = result.getItemMeta();
				if (resultMeta.getPersistentDataContainer().has(ARANARTHIUM_INGOT, PersistentDataType.STRING)) {
					new CraftingOverridesCluster().onCraft(e, ingredient, player);
				}
			}

			// If custom item is used to craft vanilla recipes
			else if (ingredient.hasItemMeta()) {
				ItemMeta ingredientMeta = ingredient.getItemMeta();
				if (ingredientMeta.getPersistentDataContainer().has(CLUSTER, PersistentDataType.STRING)) {
					new CraftingOverridesCluster().onCraft(e, ingredient, player);
				} else if (ingredientMeta.getPersistentDataContainer().has(ARANARTHIUM_INGOT, PersistentDataType.STRING)) {
					new CraftingOverridesCluster().onCraft(e, ingredient, player);
				}
			}
		}
	}
}
