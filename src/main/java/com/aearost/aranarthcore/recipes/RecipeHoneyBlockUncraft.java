package com.aearost.aranarthcore.recipes;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.ShapelessRecipe;
import org.bukkit.plugin.Plugin;

public class RecipeHoneyBlockUncraft {

	public RecipeHoneyBlockUncraft(Plugin plugin) {
		createRecipe(plugin);
	}

	/**
	 * Creates the recipe for getting Honeycomb.
	 * @param plugin The plugin.
	 */
	private void createRecipe(Plugin plugin) {
		NamespacedKey nk = new NamespacedKey(plugin, "AC_HONEYCOMB");
		ShapelessRecipe recipe = new ShapelessRecipe(nk, new ItemStack(Material.HONEYCOMB, 4));
		
		recipe.addIngredient(1, Material.HONEYCOMB_BLOCK);
		
		Bukkit.addRecipe(recipe);
	}
		
	
}
