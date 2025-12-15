package com.aearost.aranarthcore.recipes;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.ShapedRecipe;
import org.bukkit.plugin.Plugin;

public class RecipeChainmailHelmetA {

	public RecipeChainmailHelmetA(Plugin plugin) {
		createRecipe(plugin);
	}

	/**
	 * Creates the recipe for getting a Chainmail Helmet.
	 * @param plugin The plugin.
	 */
	private void createRecipe(Plugin plugin) {
		NamespacedKey nk = new NamespacedKey(plugin, "AC_CHAINMAIL_HELMET_A");
		ShapedRecipe recipe = new ShapedRecipe(nk, new ItemStack(Material.CHAINMAIL_HELMET, 1));

		recipe.shape("CCC", "C C", "   ");

		recipe.setIngredient('C', Material.IRON_CHAIN);
		
		Bukkit.addRecipe(recipe);
	}
		
	
}
