package com.aearost.aranarthcore.recipes;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.ShapedRecipe;
import org.bukkit.plugin.Plugin;

public class RecipeLodestone {

	public RecipeLodestone(Plugin plugin) {
		createRecipe(plugin);
	}
	
	/**
	 * Creates the recipe for getting a lodestone.
	 * 
	 * @param plugin
	 */
	private void createRecipe(Plugin plugin) {
		NamespacedKey nk = new NamespacedKey(plugin, "AC_LODESTONE");
		ShapedRecipe recipe = new ShapedRecipe(nk, new ItemStack(Material.LODESTONE, 1));

		recipe.shape("CCC", "CIC", "CCC");

		recipe.setIngredient('C', Material.CHISELED_STONE_BRICKS);
		recipe.setIngredient('I', Material.IRON_BLOCK);
		
		Bukkit.addRecipe(recipe);
	}
		
	
}
