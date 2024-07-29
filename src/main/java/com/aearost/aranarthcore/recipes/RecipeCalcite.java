package com.aearost.aranarthcore.recipes;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.ShapelessRecipe;
import org.bukkit.plugin.Plugin;

public class RecipeCalcite {

	public RecipeCalcite(Plugin plugin) {
		createRecipe(plugin);
	}

	/**
	 * Creates the recipe for getting Calcite.
	 * @param plugin The plugin.
	 */
	private void createRecipe(Plugin plugin) {
		NamespacedKey nk = new NamespacedKey(plugin, "AC_CALCITE");
		ShapelessRecipe recipe = new ShapelessRecipe(nk, new ItemStack(Material.CALCITE, 3));
		
		recipe.addIngredient(1, Material.BONE_BLOCK);
		recipe.addIngredient(2, Material.DIORITE);
		
		Bukkit.addRecipe(recipe);
	}
		
	
}
