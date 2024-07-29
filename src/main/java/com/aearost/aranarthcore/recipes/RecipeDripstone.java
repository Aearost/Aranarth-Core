package com.aearost.aranarthcore.recipes;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.ShapelessRecipe;
import org.bukkit.plugin.Plugin;

public class RecipeDripstone {

	public RecipeDripstone(Plugin plugin) {
		createRecipe(plugin);
	}

	/**
	 * Creates the recipe for getting Dripstone.
	 * @param plugin The plugin.
	 */
	private void createRecipe(Plugin plugin) {
		NamespacedKey nk = new NamespacedKey(plugin, "AC_DRIPSTONE");
		ShapelessRecipe recipe = new ShapelessRecipe(nk, new ItemStack(Material.POINTED_DRIPSTONE, 4));
		
		recipe.addIngredient(1, Material.DRIPSTONE_BLOCK);
		
		Bukkit.addRecipe(recipe);
	}
	
}
