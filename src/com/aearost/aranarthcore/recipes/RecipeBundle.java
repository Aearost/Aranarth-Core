package com.aearost.aranarthcore.recipes;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.ShapedRecipe;
import org.bukkit.plugin.Plugin;

public class RecipeBundle {

	public RecipeBundle(Plugin plugin) {
		createRecipe(plugin);
	}
	
	/**
	 * Creates the recipe for getting a Bundle
	 * 
	 * @param plugin
	 */
	private void createRecipe(Plugin plugin) {
		NamespacedKey nk = new NamespacedKey(plugin, "AC_BUNDLE");
		ShapedRecipe recipe = new ShapedRecipe(nk, new ItemStack(Material.BUNDLE, 1));

		recipe.shape("SLS", "L L", "LLL");

		recipe.setIngredient('S', Material.STRING);
		recipe.setIngredient('L', Material.LEATHER);
		
		Bukkit.addRecipe(recipe);
	}
		
	
}
