package com.aearost.aranarthcore.recipes;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ShapelessRecipe;
import org.bukkit.plugin.Plugin;

import com.aearost.aranarthcore.items.HoneyGlazedHam;

public class RecipeHoneyGlazedHam {

	public RecipeHoneyGlazedHam(Plugin plugin) {
		createRecipe(plugin);
	}

	/**
	 * Creates the recipe for getting a Honey Glazed Ham.
	 * @param plugin The plugin.
	 */
	private void createRecipe(Plugin plugin) {
		NamespacedKey nk = new NamespacedKey(plugin, "AC_HONEY_GLAZED_HAM");
		ShapelessRecipe recipe = new ShapelessRecipe(nk, HoneyGlazedHam.getHoneyGlazedHam());
		
		recipe.addIngredient(1, Material.COOKED_PORKCHOP);
		recipe.addIngredient(1, Material.HONEY_BOTTLE);
		
		Bukkit.addRecipe(recipe);
	}
		
	
}
