package com.aearost.aranarthcore.recipes;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.ShapedRecipe;
import org.bukkit.plugin.Plugin;

public class RecipeGodApple {

	public RecipeGodApple(Plugin plugin) {
		createRecipe(plugin);
	}

	/**
	 * Creates the recipe for getting a God Apple.
	 * @param plugin The plugin.
	 */
	private void createRecipe(Plugin plugin) {
		NamespacedKey nk = new NamespacedKey(plugin, "AC_GOD_APPLE");
		ShapedRecipe recipe = new ShapedRecipe(nk, new ItemStack(Material.ENCHANTED_GOLDEN_APPLE, 1));

		recipe.shape("GGG", "GAG", "GGG");

		recipe.setIngredient('A', Material.APPLE);
		recipe.setIngredient('G', Material.GOLD_NUGGET);
		
		Bukkit.addRecipe(recipe);
	}
		
	
}
