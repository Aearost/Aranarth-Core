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

		recipe.shape("CWC", "SES", "DHD");

		recipe.setIngredient('C', Material.PRISMARINE_CRYSTALS);
		recipe.setIngredient('W', Material.WATER_BUCKET);
		recipe.setIngredient('E', Material.ECHO_SHARD);
		recipe.setIngredient('S', Material.PRISMARINE_SHARD);
		recipe.setIngredient('D', Material.DIAMOND);
		recipe.setIngredient('H', Material.HEART_OF_THE_SEA);

		Bukkit.addRecipe(recipe);
	}
		
	
}
