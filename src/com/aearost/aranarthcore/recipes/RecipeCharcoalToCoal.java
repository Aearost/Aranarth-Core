package com.aearost.aranarthcore.recipes;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.ShapelessRecipe;
import org.bukkit.plugin.Plugin;

public class RecipeCharcoalToCoal {

	public RecipeCharcoalToCoal(Plugin plugin) {
		createRecipe(plugin);
	}
	
	/**
	 * Creates the recipe for getting Coal from Charcoal
	 * 
	 * @param plugin
	 */
	private void createRecipe(Plugin plugin) {
		NamespacedKey nk = new NamespacedKey(plugin, "COAL_FROM_CHARCOAL");
		ShapelessRecipe recipe = new ShapelessRecipe(nk, new ItemStack(Material.COAL, 1));
		
		recipe.addIngredient(1, Material.CHARCOAL);
		
		Bukkit.addRecipe(recipe);
	}
	
}
