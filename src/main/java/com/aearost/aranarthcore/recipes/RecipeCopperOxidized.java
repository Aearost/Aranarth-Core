package com.aearost.aranarthcore.recipes;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.ShapedRecipe;
import org.bukkit.plugin.Plugin;

public class RecipeCopperOxidized {

	public RecipeCopperOxidized(Plugin plugin) {
		createRecipe(plugin);
	}

	/**
	 * Creates the recipe for getting Oxidized Copper.
	 * @param plugin The plugin.
	 */
	private void createRecipe(Plugin plugin) {
		NamespacedKey nk = new NamespacedKey(plugin, "AC_COPPER_OXIDIZED");
		ShapedRecipe recipe = new ShapedRecipe(nk, new ItemStack(Material.OXIDIZED_COPPER, 8));

		recipe.shape("CCC", "CWC", "CCC");

		recipe.setIngredient('C', Material.WEATHERED_COPPER);
		recipe.setIngredient('W', Material.WIND_CHARGE);
		
		Bukkit.addRecipe(recipe);
	}
		
	
}
