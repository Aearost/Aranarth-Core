package com.aearost.aranarthcore.recipes;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.ShapedRecipe;
import org.bukkit.plugin.Plugin;

public class RecipeChainmailChestplate {

	public RecipeChainmailChestplate(Plugin plugin) {
		createRecipe(plugin);
	}
	
	/**
	 * Creates the recipe for getting a chainmail chestplate
	 * 
	 * @param plugin
	 */
	private void createRecipe(Plugin plugin) {
		NamespacedKey nk = new NamespacedKey(plugin, "AC_CHAINMAIL_CHESTPLATE");
		ShapedRecipe recipe = new ShapedRecipe(nk, new ItemStack(Material.CHAINMAIL_CHESTPLATE, 1));

		recipe.shape("C C", "CCC", "CCC");

		recipe.setIngredient('C', Material.CHAIN);
		
		Bukkit.addRecipe(recipe);
	}
		
	
}
