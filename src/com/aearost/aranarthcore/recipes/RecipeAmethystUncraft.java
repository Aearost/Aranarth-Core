package com.aearost.aranarthcore.recipes;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.ShapelessRecipe;
import org.bukkit.plugin.Plugin;

public class RecipeAmethystUncraft {

	public RecipeAmethystUncraft(Plugin plugin) {
		createRecipe(plugin);
	}
	
	/**
	 * Creates the recipe for getting amethyst shards from amethyst blocks
	 * 
	 * @param plugin
	 */
	private void createRecipe(Plugin plugin) {
		NamespacedKey nk = new NamespacedKey(plugin, "AC_AMETHYST_UNCRAFT");
		ShapelessRecipe recipe = new ShapelessRecipe(nk, new ItemStack(Material.AMETHYST_SHARD, 4));
		
		recipe.addIngredient(1, Material.AMETHYST_BLOCK);
		
		Bukkit.addRecipe(recipe);
	}
		
	
}
