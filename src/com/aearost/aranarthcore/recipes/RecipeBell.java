package com.aearost.aranarthcore.recipes;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.ShapedRecipe;
import org.bukkit.plugin.Plugin;

public class RecipeBell {

	public RecipeBell(Plugin plugin) {
		createRecipe(plugin);
	}
	
	/**
	 * Creates the recipe for getting a Bell
	 * 
	 * @param plugin
	 */
	private void createRecipe(Plugin plugin) {
		NamespacedKey nk = new NamespacedKey(plugin, "AC_BELL");
		ShapedRecipe recipe = new ShapedRecipe(nk, new ItemStack(Material.BELL, 1));

		recipe.shape("CGC", "GGG", "GIG");

		recipe.setIngredient('C', Material.CHAIN);
		recipe.setIngredient('G', Material.GOLD_INGOT);
		recipe.setIngredient('I', Material.IRON_NUGGET);
		
		Bukkit.addRecipe(recipe);
	}
		
	
}
