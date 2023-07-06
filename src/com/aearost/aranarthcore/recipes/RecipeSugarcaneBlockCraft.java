package com.aearost.aranarthcore.recipes;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ShapedRecipe;
import org.bukkit.plugin.Plugin;

import com.aearost.aranarthcore.items.SugarcaneBlock;

public class RecipeSugarcaneBlockCraft {

	public RecipeSugarcaneBlockCraft(Plugin plugin) {
		createRecipe(plugin);
	}
	
	/**
	 * Creates the recipe for getting a sugarcane block
	 * 
	 * @param plugin
	 */
	private void createRecipe(Plugin plugin) {
		NamespacedKey nk = new NamespacedKey(plugin, "AC_SUGARCANE_BLOCK_FROM_SUGARCANE");
		ShapedRecipe recipe = new ShapedRecipe(nk, SugarcaneBlock.getSugarcaneBlock());

		recipe.shape("SSS", "SSS", "SSS");

		recipe.setIngredient('S', Material.SUGAR_CANE);
		
		Bukkit.addRecipe(recipe);
	}
		
	
}
