package com.aearost.aranarthcore.recipes;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.ShapedRecipe;
import org.bukkit.plugin.Plugin;

public class RecipeHorseArmorIron {

	public RecipeHorseArmorIron(Plugin plugin) {
		createRecipe(plugin);
	}

	/**
	 * Creates the recipe for getting an Iron Horse Armor.
	 * @param plugin The plugin.
	 */
	private void createRecipe(Plugin plugin) {
		NamespacedKey nk = new NamespacedKey(plugin, "AC_RECIPE_IRON_HORSE_ARMOR");
		ShapedRecipe recipe = new ShapedRecipe(nk, new ItemStack(Material.IRON_HORSE_ARMOR, 1));

		recipe.shape("  I", "ISI", "III");

		recipe.setIngredient('S', Material.SADDLE);
		recipe.setIngredient('I', Material.IRON_INGOT);
		
		Bukkit.addRecipe(recipe);
	}
		
	
}
