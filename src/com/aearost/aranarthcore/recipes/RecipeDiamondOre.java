package com.aearost.aranarthcore.recipes;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.ShapedRecipe;
import org.bukkit.plugin.Plugin;

public class RecipeDiamondOre {

	public RecipeDiamondOre(Plugin plugin) {
		createRecipe(plugin);
	}
	
	/**
	 * Creates the recipe for getting a diamond ore
	 * 
	 * @param plugin
	 */
	private void createRecipe(Plugin plugin) {
		NamespacedKey nk = new NamespacedKey(plugin, "AC_DIAMOND_ORE");
		ShapedRecipe recipe = new ShapedRecipe(nk, new ItemStack(Material.DIAMOND_ORE, 1));

		recipe.shape("SSS", "SOS", "SSS");

		recipe.setIngredient('S', Material.STONE);
		recipe.setIngredient('O', Material.DEEPSLATE_DIAMOND_ORE);
		
		Bukkit.addRecipe(recipe);
	}
		
	
}
