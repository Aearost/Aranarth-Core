package com.aearost.aranarthcore.recipes;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.ShapelessRecipe;
import org.bukkit.plugin.Plugin;

public class RecipeStick {

	public RecipeStick(Plugin plugin) {
		createRecipe(plugin);
	}

	/**
	 * Creates the recipe for getting Sticks.
	 * @param plugin The plugin.
	 */
	private void createRecipe(Plugin plugin) {
		NamespacedKey nk = new NamespacedKey(plugin, "AC_STICK");
		ShapelessRecipe recipe = new ShapelessRecipe(nk, new ItemStack(Material.STICK, 4));
		
		recipe.addIngredient(1, Material.MANGROVE_ROOTS);
		
		Bukkit.addRecipe(recipe);
	}
		
	
}
