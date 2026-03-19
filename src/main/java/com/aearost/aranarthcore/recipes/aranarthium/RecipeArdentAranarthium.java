package com.aearost.aranarthcore.recipes.aranarthium;

import com.aearost.aranarthcore.items.aranarthium.ingots.AranarthiumArdent;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ShapedRecipe;
import org.bukkit.plugin.Plugin;

public class RecipeArdentAranarthium {

	public RecipeArdentAranarthium(Plugin plugin) {
		createRecipe(plugin);
	}

	/**
	 * Creates the recipe for getting Ardent Aranarthium.
	 * @param plugin The plugin.
	 */
	private void createRecipe(Plugin plugin) {
		NamespacedKey nk = new NamespacedKey(plugin, "AC_ARDENT_ARANARTHIUM");
		ShapedRecipe recipe = new ShapedRecipe(nk, new AranarthiumArdent().getItem());

		recipe.shape("RDR", "OEO", "IDI");

		recipe.setIngredient('R', Material.RAW_IRON_BLOCK);
		recipe.setIngredient('D', Material.DEEPSLATE);
		recipe.setIngredient('O', Material.OBSIDIAN);
		recipe.setIngredient('E', Material.ECHO_SHARD);
		recipe.setIngredient('I', Material.IRON_BLOCK);

		Bukkit.addRecipe(recipe);
	}
		
	
}
