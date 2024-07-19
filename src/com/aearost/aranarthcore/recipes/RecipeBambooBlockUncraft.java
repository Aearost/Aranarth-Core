package com.aearost.aranarthcore.recipes;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.ShapelessRecipe;
import org.bukkit.plugin.Plugin;

public class RecipeBambooBlockUncraft {

	public RecipeBambooBlockUncraft(Plugin plugin) {
		createRecipe(plugin);
	}
	
	/**
	 * Creates the recipe for getting bamboo from bamboo blocks
	 * 
	 * @param plugin
	 */
	private void createRecipe(Plugin plugin) {
		NamespacedKey nk = new NamespacedKey(plugin, "AC_BAMBOO_FROM_BAMBOO_BLOCK");
		ShapelessRecipe recipe = new ShapelessRecipe(nk, new ItemStack(Material.BAMBOO, 9));
		
		recipe.addIngredient(1, Material.BAMBOO_BLOCK);
		
		Bukkit.addRecipe(recipe);
	}
	
}
