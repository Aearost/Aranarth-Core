package com.aearost.aranarthcore.recipes;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ShapedRecipe;
import org.bukkit.plugin.Plugin;

import com.aearost.aranarthcore.items.HomePad;

public class RecipeHomePad {

	public RecipeHomePad(Plugin plugin) {
		createRecipe(plugin);
	}
	
	/**
	 * Creates the recipe for getting a home pad
	 * 
	 * @param plugin
	 */
	private void createRecipe(Plugin plugin) {
		NamespacedKey nk = new NamespacedKey(plugin, "HOME_PAD");
		ShapedRecipe recipe = new ShapedRecipe(nk, HomePad.getHomePad());

		recipe.shape("LDL", "GPG", "LDL");

		recipe.setIngredient('L', Material.LAPIS_BLOCK);
		recipe.setIngredient('D', Material.DIAMOND);
		recipe.setIngredient('G', Material.GOLD_BLOCK);
		recipe.setIngredient('P', Material.HEAVY_WEIGHTED_PRESSURE_PLATE);
		
		Bukkit.addRecipe(recipe);
	}
		
	
}
