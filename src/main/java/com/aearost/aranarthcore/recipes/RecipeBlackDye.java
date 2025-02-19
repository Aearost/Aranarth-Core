package com.aearost.aranarthcore.recipes;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.ShapelessRecipe;
import org.bukkit.plugin.Plugin;

public class RecipeBlackDye {

	public RecipeBlackDye(Plugin plugin) {
		createRecipe(plugin);
	}
	
	/**
	 * Creates the recipe for getting Black Dye.
	 * @param plugin The plugin.
	 */
	private void createRecipe(Plugin plugin) {
		NamespacedKey nk = new NamespacedKey(plugin, "AC_BLACK_DYE");
		ShapelessRecipe recipe = new ShapelessRecipe(nk, new ItemStack(Material.BLACK_DYE, 2));
		
		recipe.addIngredient(1, Material.CHARCOAL);
		
		Bukkit.addRecipe(recipe);
	}
		
	
}
