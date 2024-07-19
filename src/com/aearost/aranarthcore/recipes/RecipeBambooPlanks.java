package com.aearost.aranarthcore.recipes;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.ShapelessRecipe;
import org.bukkit.plugin.Plugin;

public class RecipeBambooPlanks {

	public RecipeBambooPlanks(Plugin plugin) {
		createRecipe(plugin);
	}
	
	/**
	 * Creates the recipe for getting bamboo planks from bamboo blocks
	 * 
	 * @param plugin
	 */
	private void createRecipe(Plugin plugin) {
		NamespacedKey nk = new NamespacedKey(plugin, "AC_BAMBOO_PLANKS");
		ShapelessRecipe recipe = new ShapelessRecipe(nk, new ItemStack(Material.BAMBOO_PLANKS, 4));
		
		recipe.addIngredient(2, Material.BAMBOO_BLOCK);
		
		Bukkit.addRecipe(recipe);
	}
		
	
}
