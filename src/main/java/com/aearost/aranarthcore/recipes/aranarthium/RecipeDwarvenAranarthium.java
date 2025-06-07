package com.aearost.aranarthcore.recipes.aranarthium;

import com.aearost.aranarthcore.items.aranarthium.ingots.AranarthiumDwarven;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ShapedRecipe;
import org.bukkit.plugin.Plugin;

public class RecipeDwarvenAranarthium {

	public RecipeDwarvenAranarthium(Plugin plugin) {
		createRecipe(plugin);
	}

	/**
	 * Creates the recipe for getting Dwarven Aranarthium.
	 * @param plugin The plugin.
	 */
	private void createRecipe(Plugin plugin) {
		NamespacedKey nk = new NamespacedKey(plugin, "AC_DWARVEN_ARANARTHIUM");
		ShapedRecipe recipe = new ShapedRecipe(nk, new AranarthiumDwarven().getItem());

		recipe.shape("BGB", "GEG", "BRB");

		recipe.setIngredient('B', Material.GLOW_BERRIES);
		recipe.setIngredient('E', Material.ECHO_SHARD);
		recipe.setIngredient('G', Material.GOLD_INGOT);
		recipe.setIngredient('R', Material.RABBIT_FOOT);

		Bukkit.addRecipe(recipe);
	}
		
	
}
