package com.aearost.aranarthcore.recipes;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.ShapelessRecipe;
import org.bukkit.plugin.Plugin;

public class RecipeMushroomStew {

	public RecipeMushroomStew(Plugin plugin) {
		createRecipe(plugin);
	}

	/**
	 * Creates the recipe for getting Mushroom Stew from nether fungus.
	 * @param plugin The plugin.
	 */
	private void createRecipe(Plugin plugin) {
		NamespacedKey nk = new NamespacedKey(plugin, "AC_MUSHROOM_STEW");
		ShapelessRecipe recipe = new ShapelessRecipe(nk, new ItemStack(Material.MUSHROOM_STEW, 1));
		
		recipe.addIngredient(1, Material.BOWL);
		recipe.addIngredient(1, Material.WARPED_FUNGUS);
		recipe.addIngredient(1, Material.CRIMSON_FUNGUS);
		
		Bukkit.addRecipe(recipe);
	}
		
	
}
