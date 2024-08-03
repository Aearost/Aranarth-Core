package com.aearost.aranarthcore.recipes;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.ShapelessRecipe;
import org.bukkit.plugin.Plugin;

public class RecipeGlowstoneUncraft {

	public RecipeGlowstoneUncraft(Plugin plugin) {
		createRecipe(plugin);
	}

	/**
	 * Creates the recipe for getting Glowstone Dust.
	 * @param plugin The plugin.
	 */
	private void createRecipe(Plugin plugin) {
		NamespacedKey nk = new NamespacedKey(plugin, "AC_GLOWSTONE_UNCRAFT");
		ShapelessRecipe recipe = new ShapelessRecipe(nk, new ItemStack(Material.GLOWSTONE_DUST, 4));
		
		recipe.addIngredient(1, Material.GLOWSTONE);
		
		Bukkit.addRecipe(recipe);
	}
	
}
