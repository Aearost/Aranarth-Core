package com.aearost.aranarthcore.recipes;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.ShapelessRecipe;
import org.bukkit.plugin.Plugin;

public class RecipeMushroomBlockRedUncraft {

	public RecipeMushroomBlockRedUncraft(Plugin plugin) {
		createRecipe(plugin);
	}
	
	/**
	 * Creates the recipe for getting Red Mushrooms from their Block form.
	 * 
	 * @param plugin
	 */
	private void createRecipe(Plugin plugin) {
		NamespacedKey nk = new NamespacedKey(plugin, "AC_MUSHROOM_BLOCK_RED_UNCRAFT");
		ShapelessRecipe recipe = new ShapelessRecipe(nk, new ItemStack(Material.RED_MUSHROOM, 4));
		
		recipe.addIngredient(1, Material.RED_MUSHROOM_BLOCK);
		
		Bukkit.addRecipe(recipe);
	}
		
	
}
