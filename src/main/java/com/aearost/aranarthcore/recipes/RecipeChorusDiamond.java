package com.aearost.aranarthcore.recipes;

import com.aearost.aranarthcore.items.ChorusDiamond;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ShapedRecipe;
import org.bukkit.plugin.Plugin;

public class RecipeChorusDiamond {

	public RecipeChorusDiamond(Plugin plugin) {
		createRecipe(plugin);
	}

	/**
	 * Creates the recipe for getting a Chorus Diamond.
	 * @param plugin The plugin.
	 */
	private void createRecipe(Plugin plugin) {
		NamespacedKey nk = new NamespacedKey(plugin, "AC_CHORUS_DIAMOND");
		ShapedRecipe recipe = new ShapedRecipe(nk, ChorusDiamond.getChorusDiamond());

		recipe.shape("CCC", "CDC", "CCC");

		recipe.setIngredient('C', Material.CHORUS_FRUIT);
		recipe.setIngredient('D', Material.DIAMOND);
		
		Bukkit.addRecipe(recipe);
	}
		
	
}
