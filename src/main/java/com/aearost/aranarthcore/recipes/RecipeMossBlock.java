package com.aearost.aranarthcore.recipes;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.ShapelessRecipe;
import org.bukkit.plugin.Plugin;

public class RecipeMossBlock {

	public RecipeMossBlock(Plugin plugin) {
		createRecipe(plugin);
	}

	/**
	 * Creates the recipe for getting Moss Block.
	 * @param plugin The plugin.
	 */
	private void createRecipe(Plugin plugin) {
		NamespacedKey nk = new NamespacedKey(plugin, "AC_MOSS_BLOCK");
		ShapelessRecipe recipe = new ShapelessRecipe(nk, new ItemStack(Material.MOSS_BLOCK, 1));
		
		recipe.addIngredient(8, Material.MOSS_CARPET);
		
		Bukkit.addRecipe(recipe);
	}
		
	
}
