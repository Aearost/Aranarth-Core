package com.aearost.aranarthcore.recipes;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.ShapedRecipe;
import org.bukkit.plugin.Plugin;

public class RecipeTuffB {

	public RecipeTuffB(Plugin plugin) {
		createRecipe(plugin);
	}

	/**
	 * Creates the recipe for getting Tuff.
	 * @param plugin The plugin.
	 */
	private void createRecipe(Plugin plugin) {
		NamespacedKey nk = new NamespacedKey(plugin, "AC_TUFF_B");
		ShapedRecipe recipe = new ShapedRecipe(nk, new ItemStack(Material.TUFF, 4));
		
		recipe.shape("GD", "DG");

		recipe.setIngredient('D', Material.DEEPSLATE);
		recipe.setIngredient('G', Material.GRAVEL);
		
		Bukkit.addRecipe(recipe);
	}
		
	
}
