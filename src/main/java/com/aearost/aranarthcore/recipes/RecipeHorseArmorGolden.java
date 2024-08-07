package com.aearost.aranarthcore.recipes;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.ShapedRecipe;
import org.bukkit.plugin.Plugin;

public class RecipeHorseArmorGolden {

	public RecipeHorseArmorGolden(Plugin plugin) {
		createRecipe(plugin);
	}

	/**
	 * Creates the recipe for getting a Golden Horse Armor.
	 * @param plugin The plugin.
	 */
	private void createRecipe(Plugin plugin) {
		NamespacedKey nk = new NamespacedKey(plugin, "AC_GOLDEN_HORSE_ARMOR");
		ShapedRecipe recipe = new ShapedRecipe(nk, new ItemStack(Material.GOLDEN_HORSE_ARMOR, 1));

		recipe.shape("  G", "GSG", "GGG");

		recipe.setIngredient('S', Material.SADDLE);
		recipe.setIngredient('G', Material.GOLD_INGOT);
		
		Bukkit.addRecipe(recipe);
	}
		
	
}
