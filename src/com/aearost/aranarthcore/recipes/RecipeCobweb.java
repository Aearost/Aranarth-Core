package com.aearost.aranarthcore.recipes;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.ShapedRecipe;
import org.bukkit.plugin.Plugin;

public class RecipeCobweb {

	public RecipeCobweb(Plugin plugin) {
		createRecipe(plugin);
	}
	
	/**
	 * Creates the recipe for getting a cobweb
	 * 
	 * @param plugin
	 */
	private void createRecipe(Plugin plugin) {
		NamespacedKey nk = new NamespacedKey(plugin, "AC_COBWEB");
		ShapedRecipe recipe = new ShapedRecipe(nk, new ItemStack(Material.COBWEB, 1));

		recipe.shape("S S", " S ", "S S");

		recipe.setIngredient('S', Material.STRING);
		
		Bukkit.addRecipe(recipe);
	}
		
	
}
