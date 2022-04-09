package com.aearost.aranarthcore.recipes;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.ShapelessRecipe;
import org.bukkit.plugin.Plugin;

public class RecipeCompressedCobblestoneB {

	public RecipeCompressedCobblestoneB(Plugin plugin) {
		createRecipe(plugin);
	}
	
	/**
	 * Creates the recipe for getting cobblestone from Compressed Cobblestone
	 * 
	 * @param plugin
	 */
	private void createRecipe(Plugin plugin) {
		NamespacedKey nk = new NamespacedKey(plugin, "COMPRESSED_COBBLESTONE_B");
		ShapelessRecipe recipe = new ShapelessRecipe(nk, new ItemStack(Material.COBBLESTONE, 9));
		
		recipe.addIngredient(1, Material.COBBLESTONE);
		
		Bukkit.addRecipe(recipe);
	}
		
	
}
