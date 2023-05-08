package com.aearost.aranarthcore.recipes;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.ShapedRecipe;
import org.bukkit.plugin.Plugin;

public class RecipeNametag {

	public RecipeNametag(Plugin plugin) {
		createRecipe(plugin);
	}
	
	/**
	 * Creates the recipe for getting a Nametag
	 * 
	 * @param plugin
	 */
	private void createRecipe(Plugin plugin) {
		NamespacedKey nk = new NamespacedKey(plugin, "NAMETAG");
		ShapedRecipe recipe = new ShapedRecipe(nk, new ItemStack(Material.NAME_TAG, 1));

		recipe.shape(" SS", "ISP", "IIP");

		recipe.setIngredient('S', Material.STRING);
		recipe.setIngredient('I', Material.INK_SAC);
		recipe.setIngredient('P', Material.PAPER);
		
		Bukkit.addRecipe(recipe);
	}
		
	
}
