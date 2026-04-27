package com.aearost.aranarthcore.recipes.aranarthium;

import com.aearost.aranarthcore.items.aranarthium.ingots.AranarthiumAquatic;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ShapedRecipe;
import org.bukkit.plugin.Plugin;

public class RecipeAquaticAranarthium {

	public RecipeAquaticAranarthium(Plugin plugin) {
		createRecipe(plugin);
	}

	/**
	 * Creates the recipe for getting Aquatic Aranarthium.
	 * @param plugin The plugin.
	 */
	private void createRecipe(Plugin plugin) {
		NamespacedKey nk = new NamespacedKey(plugin, "AC_AQUATIC_ARANARTHIUM");
		ShapedRecipe recipe = new ShapedRecipe(nk, new AranarthiumAquatic().getItem());

		recipe.shape("LWL", "PEP", "DHD");

		recipe.setIngredient('L', Material.SEA_LANTERN);
		recipe.setIngredient('W', Material.WATER_BUCKET);
		recipe.setIngredient('P', Material.DARK_PRISMARINE);
		recipe.setIngredient('E', Material.ECHO_SHARD);
		recipe.setIngredient('D', Material.DIAMOND_BLOCK);
		recipe.setIngredient('H', Material.HEART_OF_THE_SEA);

		Bukkit.addRecipe(recipe);
	}
		
	
}
