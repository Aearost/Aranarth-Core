package com.aearost.aranarthcore.recipes;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.ShapedRecipe;
import org.bukkit.plugin.Plugin;

public class RecipeTuffA {

	public RecipeTuffA(Plugin plugin) {
		createRecipe(plugin);
	}

	/**
	 * Creates the recipe for getting Tuff.
	 * @param plugin The plugin.
	 */
	private void createRecipe(Plugin plugin) {
		NamespacedKey nk = new NamespacedKey(plugin, "AC_TUFF_A");
		ShapedRecipe recipe = new ShapedRecipe(nk, new ItemStack(Material.TUFF, 4));

		recipe.shape("DG", "GD");

		recipe.setIngredient('D', Material.DEEPSLATE);
		recipe.setIngredient('G', Material.GRAVEL);
		
		Bukkit.addRecipe(recipe);
	}
		
	
}
