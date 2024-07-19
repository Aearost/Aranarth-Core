package com.aearost.aranarthcore.recipes;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.ShapedRecipe;
import org.bukkit.plugin.Plugin;

public class RecipeSaddleA {

	public RecipeSaddleA(Plugin plugin) {
		createRecipe(plugin);
	}
	
	/**
	 * Creates the recipe for getting a saddle
	 * 
	 * @param plugin
	 */
	private void createRecipe(Plugin plugin) {
		NamespacedKey nk = new NamespacedKey(plugin, "AC_RECIPE_SADDLE_A");
		ShapedRecipe recipe = new ShapedRecipe(nk, new ItemStack(Material.SADDLE, 1));

		recipe.shape("LLL", "I I", "   ");

		recipe.setIngredient('L', Material.LEATHER);
		recipe.setIngredient('I', Material.IRON_INGOT);
		
		Bukkit.addRecipe(recipe);
	}
		
	
}
