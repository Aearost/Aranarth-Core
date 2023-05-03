package com.aearost.aranarthcore.recipes;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.ShapedRecipe;
import org.bukkit.plugin.Plugin;

public class RecipeHorseArmourLeather {

	public RecipeHorseArmourLeather(Plugin plugin) {
		createRecipe(plugin);
	}
	
	/**
	 * Creates the recipe for getting a leather horse armour
	 * 
	 * @param plugin
	 */
	private void createRecipe(Plugin plugin) {
		NamespacedKey nk = new NamespacedKey(plugin, "LEATHER_HORSE_ARMOUR");
		ShapedRecipe recipe = new ShapedRecipe(nk, new ItemStack(Material.LEATHER_HORSE_ARMOR, 1));

		recipe.shape("  L", "LSL", "LLL");

		recipe.setIngredient('S', Material.SADDLE);
		recipe.setIngredient('L', Material.LEATHER);
		
		Bukkit.addRecipe(recipe);
	}
		
	
}
