package com.aearost.aranarthcore.recipes;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.ShapedRecipe;
import org.bukkit.plugin.Plugin;

public class RecipeGildedBlackstone {

	public RecipeGildedBlackstone(Plugin plugin) {
		createRecipe(plugin);
	}

	/**
	 * Creates the recipe for getting a Gilded Blackstone.
	 * @param plugin The plugin.
	 */
	private void createRecipe(Plugin plugin) {
		NamespacedKey nk = new NamespacedKey(plugin, "AC_GILDED_BLACKSTONE");
		ShapedRecipe recipe = new ShapedRecipe(nk, new ItemStack(Material.GILDED_BLACKSTONE, 1));

		recipe.shape(" G ", "GBG", " G ");

		recipe.setIngredient('G', Material.GOLD_NUGGET);
		recipe.setIngredient('B', Material.BLACKSTONE);
		
		Bukkit.addRecipe(recipe);
	}
		
	
}
