package com.aearost.aranarthcore.recipes;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.ShapelessRecipe;
import org.bukkit.plugin.Plugin;

public class RecipeGreenDye {

	public RecipeGreenDye(Plugin plugin) {
		createRecipe(plugin);
	}

	/**
	 * Creates the recipe for getting green dye.
	 * @param plugin The plugin.
	 */
	private void createRecipe(Plugin plugin) {
		NamespacedKey nk = new NamespacedKey(plugin, "AC_GREEN_DYE");
		ShapelessRecipe recipe = new ShapelessRecipe(nk, new ItemStack(Material.GREEN_DYE, 2));
		
		recipe.addIngredient(1, Material.BLUE_DYE);
		recipe.addIngredient(1, Material.YELLOW_DYE);
		
		Bukkit.addRecipe(recipe);
	}
		
	
}
