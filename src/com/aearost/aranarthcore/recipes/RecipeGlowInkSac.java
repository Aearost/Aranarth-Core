package com.aearost.aranarthcore.recipes;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.ShapelessRecipe;
import org.bukkit.plugin.Plugin;

public class RecipeGlowInkSac {

	public RecipeGlowInkSac(Plugin plugin) {
		createRecipe(plugin);
	}
	
	/**
	 * Creates the recipe for getting a Glow Ink Sac
	 * 
	 * @param plugin
	 */
	private void createRecipe(Plugin plugin) {
		NamespacedKey nk = new NamespacedKey(plugin, "AC_GLOW_INK_SAC");
		ShapelessRecipe recipe = new ShapelessRecipe(nk, new ItemStack(Material.GLOW_INK_SAC, 1));
		
		recipe.addIngredient(1, Material.INK_SAC);
		recipe.addIngredient(1, Material.GLOWSTONE_DUST);
		
		Bukkit.addRecipe(recipe);
	}
		
	
}
