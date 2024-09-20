package com.aearost.aranarthcore.recipes;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.ShapelessRecipe;
import org.bukkit.plugin.Plugin;

public class RecipeMossCarpet {

	public RecipeMossCarpet(Plugin plugin) {
		createRecipe(plugin);
	}

	/**
	 * Creates the recipe for getting Moss Carpet.
	 * @param plugin The plugin.
	 */
	private void createRecipe(Plugin plugin) {
		NamespacedKey nk = new NamespacedKey(plugin, "AC_MOSS_CARPET");
		ShapelessRecipe recipe = new ShapelessRecipe(nk, new ItemStack(Material.MOSS_CARPET, 8));
		
		recipe.addIngredient(1, Material.MOSS_BLOCK);
		
		Bukkit.addRecipe(recipe);
	}
		
	
}
