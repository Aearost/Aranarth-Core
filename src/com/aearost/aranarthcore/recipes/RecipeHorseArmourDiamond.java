package com.aearost.aranarthcore.recipes;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.ShapedRecipe;
import org.bukkit.plugin.Plugin;

public class RecipeHorseArmourDiamond {

	public RecipeHorseArmourDiamond(Plugin plugin) {
		createRecipe(plugin);
	}
	
	/**
	 * Creates the recipe for getting a diamond horse armour
	 * 
	 * @param plugin
	 */
	private void createRecipe(Plugin plugin) {
		NamespacedKey nk = new NamespacedKey(plugin, "AC_DIAMOND_HORSE_ARMOUR");
		ShapedRecipe recipe = new ShapedRecipe(nk, new ItemStack(Material.DIAMOND_HORSE_ARMOR, 1));

		recipe.shape("  D", "DSD", "DDD");

		recipe.setIngredient('S', Material.SADDLE);
		recipe.setIngredient('D', Material.DIAMOND);
		
		Bukkit.addRecipe(recipe);
	}
		
	
}
