package com.aearost.aranarthcore.recipes;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ShapedRecipe;
import org.bukkit.plugin.Plugin;

import com.aearost.aranarthcore.items.CompressedCobblestone;

public class RecipeCompressedCobblestoneA {

	public RecipeCompressedCobblestoneA(Plugin plugin) {
		createRecipe(plugin);
	}
	
	/**
	 * Creates the recipe for getting Compressed Cobblestone
	 * 
	 * @param plugin
	 */
	private void createRecipe(Plugin plugin) {
		NamespacedKey nk = new NamespacedKey(plugin, "COMPRESSED_COBBLESTONE_A");
		ShapedRecipe recipe = new ShapedRecipe(nk, CompressedCobblestone.getCompressedCobblestone());

		recipe.shape("CCC", "CCC", "CCC");

		recipe.setIngredient('C', Material.COBBLESTONE);
		
		Bukkit.addRecipe(recipe);
	}
		
	
}
