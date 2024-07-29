package com.aearost.aranarthcore.recipes;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ShapedRecipe;
import org.bukkit.plugin.Plugin;

import com.aearost.aranarthcore.items.Quiver;

public class RecipeQuiver {

	public RecipeQuiver(Plugin plugin) {
		createRecipe(plugin);
	}

	/**
	 * Creates the recipe for getting a Quiver.
	 * @param plugin The plugin.
	 */
	private void createRecipe(Plugin plugin) {
		NamespacedKey nk = new NamespacedKey(plugin, "AC_QUIVER");
		ShapedRecipe recipe = new ShapedRecipe(nk, Quiver.getQuiver());

		recipe.shape("SLS", "LAL", "LLL");

		recipe.setIngredient('S', Material.STRING);
		recipe.setIngredient('L', Material.LEATHER);
		recipe.setIngredient('A', Material.SPECTRAL_ARROW);
		
		Bukkit.addRecipe(recipe);
	}
		
	
}
