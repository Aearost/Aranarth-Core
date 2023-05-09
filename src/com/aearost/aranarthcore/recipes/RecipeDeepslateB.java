package com.aearost.aranarthcore.recipes;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.ShapelessRecipe;
import org.bukkit.plugin.Plugin;

public class RecipeDeepslateB {

	public RecipeDeepslateB(Plugin plugin) {
		createRecipe(plugin);
	}
	
	/**
	 * Creates the recipe for getting Cobblestone from Deepslate
	 * 
	 * @param plugin
	 */
	private void createRecipe(Plugin plugin) {
		NamespacedKey nk = new NamespacedKey(plugin, "AC_DEEPSLATE_B");
		ShapelessRecipe recipe = new ShapelessRecipe(nk, new ItemStack(Material.COBBLESTONE, 2));
		
		recipe.addIngredient(1, Material.COBBLED_DEEPSLATE);
		
		Bukkit.addRecipe(recipe);
	}
	
}
