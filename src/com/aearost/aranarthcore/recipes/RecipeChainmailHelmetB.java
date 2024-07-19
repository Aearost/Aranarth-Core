package com.aearost.aranarthcore.recipes;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.ShapedRecipe;
import org.bukkit.plugin.Plugin;

public class RecipeChainmailHelmetB {

	public RecipeChainmailHelmetB(Plugin plugin) {
		createRecipe(plugin);
	}
	
	/**
	 * Creates the recipe for getting a chainmail helmet
	 * 
	 * @param plugin
	 */
	private void createRecipe(Plugin plugin) {
		NamespacedKey nk = new NamespacedKey(plugin, "AC_CHAINMAIL_HELMET_B");
		ShapedRecipe recipe = new ShapedRecipe(nk, new ItemStack(Material.CHAINMAIL_HELMET, 1));

		recipe.shape("   ", "CCC", "C C");

		recipe.setIngredient('C', Material.CHAIN);
		
		Bukkit.addRecipe(recipe);
	}
		
	
}
