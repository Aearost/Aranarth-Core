package com.aearost.aranarthcore.recipes;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.ShapelessRecipe;
import org.bukkit.plugin.Plugin;

public class RecipeMushroomBlockRed {

	public RecipeMushroomBlockRed(Plugin plugin) {
		createRecipe(plugin);
	}
	
	/**
	 * Creates the recipe for getting a Red Mushroom Block.
	 * 
	 * @param plugin
	 */
	private void createRecipe(Plugin plugin) {
		NamespacedKey nk = new NamespacedKey(plugin, "AC_MUSHROOM_BLOCK_RED");
		ShapelessRecipe recipe = new ShapelessRecipe(nk, new ItemStack(Material.RED_MUSHROOM_BLOCK, 1));
		
		recipe.addIngredient(4, Material.RED_MUSHROOM);
		
		Bukkit.addRecipe(recipe);
	}
		
	
}
