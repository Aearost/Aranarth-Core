package com.aearost.aranarthcore.event.listener.grouped;

import com.aearost.aranarthcore.AranarthCore;
import com.aearost.aranarthcore.event.crafting.*;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.HumanEntity;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.CraftItemEvent;
import org.bukkit.event.inventory.PrepareItemCraftEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

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

			if (hasKey(ARANARTHIUM_INGOT, e, ingredient) || hasKey(CLUSTER, e, ingredient) || hasKey(ARMOR_TYPE, e, ingredient)) {
				new CraftingOverridesAranarthium().onCraft(e, ingredient, player);
			}

			if (hasKey(CHORUS_DIAMOND, e, ingredient) || hasKey(HOMEPAD, e, ingredient)) {
				new CraftingOverridesChorusDiamond().onCraft(e, ingredient, player);
			}

			if (hasKey(GOD_APPLE_FRAGMENT, e, ingredient) || result.getType() == Material.ENCHANTED_GOLDEN_APPLE) {
				new CraftingOverridesGodAppleFragment().onCraft(e, ingredient, player);
			}
		}
	}

	private boolean hasKey(NamespacedKey key, CraftItemEvent e, ItemStack ingredient) {
		ItemMeta resultMeta;
		ItemMeta ingredientMeta;
		if (e.getRecipe().getResult().hasItemMeta()) {
			resultMeta = e.getRecipe().getResult().getItemMeta();
			if (resultMeta.getPersistentDataContainer().has(key)) {
				return true;
			}
		}

		if (ingredient.hasItemMeta()) {
			ingredientMeta = ingredient.getItemMeta();
            if (ingredientMeta.getPersistentDataContainer().has(key)) {
				return true;
			}
		}

		return false;
	}
}
