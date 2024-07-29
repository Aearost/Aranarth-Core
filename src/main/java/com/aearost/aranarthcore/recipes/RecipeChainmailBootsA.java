package com.aearost.aranarthcore.recipes;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.ShapedRecipe;
import org.bukkit.plugin.Plugin;

public class RecipeChainmailBootsA {

	public RecipeChainmailBootsA(Plugin plugin) {
		createRecipe(plugin);
	}

	/**
	 * Creates the recipe for getting Chainmail Boots.
	 * @param plugin The plugin.
	 */
	private void createRecipe(Plugin plugin) {
		NamespacedKey nk = new NamespacedKey(plugin, "AC_CHAINMAIL_BOOTS_A");
		ShapedRecipe recipe = new ShapedRecipe(nk, new ItemStack(Material.CHAINMAIL_BOOTS, 1));

		recipe.shape("C C", "C C", "   ");

		recipe.setIngredient('C', Material.CHAIN);
		
		Bukkit.addRecipe(recipe);
	}
		
	
}
