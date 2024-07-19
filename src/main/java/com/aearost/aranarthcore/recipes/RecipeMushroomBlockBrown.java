package com.aearost.aranarthcore.recipes;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.ShapelessRecipe;
import org.bukkit.plugin.Plugin;

public class RecipeMushroomBlockBrown {

	public RecipeMushroomBlockBrown(Plugin plugin) {
		createRecipe(plugin);
	}
	
	/**
	 * Creates the recipe for getting a Brown Mushroom Block.
	 * 
	 * @param plugin
	 */
	private void createRecipe(Plugin plugin) {
		NamespacedKey nk = new NamespacedKey(plugin, "AC_MUSHROOM_BLOCK_BROWN");
		ShapelessRecipe recipe = new ShapelessRecipe(nk, new ItemStack(Material.BROWN_MUSHROOM_BLOCK, 1));
		
		recipe.addIngredient(4, Material.BROWN_MUSHROOM);
		
		Bukkit.addRecipe(recipe);
	}
		
	
}
