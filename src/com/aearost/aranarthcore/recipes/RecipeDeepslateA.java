package com.aearost.aranarthcore.recipes;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.ShapelessRecipe;
import org.bukkit.plugin.Plugin;

public class RecipeDeepslateA {

	public RecipeDeepslateA(Plugin plugin) {
		createRecipe(plugin);
	}
	
	/**
	 * Creates the recipe for getting Deepslate from Cobblestone
	 * 
	 * @param plugin
	 */
	private void createRecipe(Plugin plugin) {
		NamespacedKey nk = new NamespacedKey(plugin, "AC_DEEPSLATE_A");
		ShapelessRecipe recipe = new ShapelessRecipe(nk, new ItemStack(Material.COBBLED_DEEPSLATE, 1));
		
		recipe.addIngredient(2, Material.COBBLESTONE);
		
		Bukkit.addRecipe(recipe);
	}
	
}
