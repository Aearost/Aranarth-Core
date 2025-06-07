package com.aearost.aranarthcore.recipes.aranarthium;

import com.aearost.aranarthcore.items.aranarthium.ingots.AranarthiumArdent;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ShapedRecipe;
import org.bukkit.plugin.Plugin;

public class RecipeMannishAranarthium {

	public RecipeMannishAranarthium(Plugin plugin) {
		createRecipe(plugin);
	}

	/**
	 * Creates the recipe for getting Mannish Aranarthium.
	 * @param plugin The plugin.
	 */
	private void createRecipe(Plugin plugin) {
		NamespacedKey nk = new NamespacedKey(plugin, "AC_MANNISH_ARANARTHIUM");
		ShapedRecipe recipe = new ShapedRecipe(nk, new AranarthiumArdent().getItem());

		recipe.shape("DSD", "IEI", "BDB");

		recipe.setIngredient('D', Material.DEEPSLATE);
		recipe.setIngredient('S', Material.STONE);
		recipe.setIngredient('E', Material.ECHO_SHARD);
		recipe.setIngredient('I', Material.IRON_INGOT);
		recipe.setIngredient('B', Material.IRON_BLOCK);

		Bukkit.addRecipe(recipe);
	}
		
	
}
