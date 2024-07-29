package com.aearost.aranarthcore.recipes;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.ShapelessRecipe;
import org.bukkit.plugin.Plugin;

public class RecipeRootedDirt {

	public RecipeRootedDirt(Plugin plugin) {
		createRecipe(plugin);
	}

	/**
	 * Creates the recipe for getting a Rooted Dirt.
	 * @param plugin The plugin.
	 */
	private void createRecipe(Plugin plugin) {
		NamespacedKey nk = new NamespacedKey(plugin, "AC_ROOTED_DIRT");
		ShapelessRecipe recipe = new ShapelessRecipe(nk, new ItemStack(Material.ROOTED_DIRT, 1));
		
		recipe.addIngredient(1, Material.DIRT);
		recipe.addIngredient(1, Material.WHEAT_SEEDS);
		recipe.addIngredient(1, Material.BONE_MEAL);
		
		Bukkit.addRecipe(recipe);
	}
	
}
