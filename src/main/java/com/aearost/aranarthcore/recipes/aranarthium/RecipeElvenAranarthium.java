package com.aearost.aranarthcore.recipes.aranarthium;

import com.aearost.aranarthcore.items.aranarthium.ingots.AranarthiumElven;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ShapedRecipe;
import org.bukkit.plugin.Plugin;

public class RecipeElvenAranarthium {

	public RecipeElvenAranarthium(Plugin plugin) {
		createRecipe(plugin);
	}

	/**
	 * Creates the recipe for getting Elven Aranarthium.
	 * @param plugin The plugin.
	 */
	private void createRecipe(Plugin plugin) {
		NamespacedKey nk = new NamespacedKey(plugin, "AC_ELVEN_ARANARTHIUM");
		ShapedRecipe recipe = new ShapedRecipe(nk, new AranarthiumElven().getItem());

		recipe.shape("APA", "WSW", "EFE");

		recipe.setIngredient('A', Material.ENCHANTED_GOLDEN_APPLE);
		recipe.setIngredient('P', Material.PHANTOM_MEMBRANE);
		recipe.setIngredient('W', Material.WIND_CHARGE);
		recipe.setIngredient('S', Material.ECHO_SHARD);
		recipe.setIngredient('F', Material.FEATHER);
		recipe.setIngredient('E', Material.EMERALD_BLOCK);

		Bukkit.addRecipe(recipe);
	}
		
	
}
