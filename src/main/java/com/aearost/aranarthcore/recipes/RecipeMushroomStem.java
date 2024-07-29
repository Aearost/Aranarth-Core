package com.aearost.aranarthcore.recipes;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.ShapelessRecipe;
import org.bukkit.plugin.Plugin;

public class RecipeMushroomStem {

	public RecipeMushroomStem(Plugin plugin) {
		createRecipe(plugin);
	}

	/**
	 * Creates the recipe for getting a Mushroom Stem.
	 * @param plugin The plugin.
	 */
	private void createRecipe(Plugin plugin) {
		NamespacedKey nk = new NamespacedKey(plugin, "AC_MUSHROOM_STEM");
		ShapelessRecipe recipe = new ShapelessRecipe(nk, new ItemStack(Material.MUSHROOM_STEM, 1));
		
		recipe.addIngredient(2, Material.BONE_MEAL);
		recipe.addIngredient(1, Material.BROWN_MUSHROOM);
		recipe.addIngredient(1, Material.RED_MUSHROOM);
		
		Bukkit.addRecipe(recipe);
	}
		
	
}
