package com.aearost.aranarthcore.recipes;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.ShapelessRecipe;
import org.bukkit.plugin.Plugin;

public class RecipeMushroomBlockBrownUncraft {

	public RecipeMushroomBlockBrownUncraft(Plugin plugin) {
		createRecipe(plugin);
	}

	/**
	 * Creates the recipe for getting Brown Mushrooms.
	 * @param plugin The plugin.
	 */
	private void createRecipe(Plugin plugin) {
		NamespacedKey nk = new NamespacedKey(plugin, "AC_MUSHROOM_BLOCK_BROWN_UNCRAFT");
		ShapelessRecipe recipe = new ShapelessRecipe(nk, new ItemStack(Material.BROWN_MUSHROOM, 4));
		
		recipe.addIngredient(1, Material.BROWN_MUSHROOM_BLOCK);
		
		Bukkit.addRecipe(recipe);
	}
		
	
}
