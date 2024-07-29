package com.aearost.aranarthcore.recipes;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.ShapelessRecipe;
import org.bukkit.plugin.Plugin;

public class RecipeSugarcaneBlockUncraft {

	public RecipeSugarcaneBlockUncraft(Plugin plugin) {
		createRecipe(plugin);
	}

	/**
	 * Creates the recipe for getting Sugarcane.
	 * @param plugin The plugin.
	 */
	private void createRecipe(Plugin plugin) {
		NamespacedKey nk = new NamespacedKey(plugin, "AC_SUGARCANE_FROM_SUGARCANE_BLOCK");
		ShapelessRecipe recipe = new ShapelessRecipe(nk, new ItemStack(Material.SUGAR_CANE, 9));
		
		recipe.addIngredient(1, Material.BAMBOO_BLOCK);
		
		Bukkit.addRecipe(recipe);
	}
	
}
