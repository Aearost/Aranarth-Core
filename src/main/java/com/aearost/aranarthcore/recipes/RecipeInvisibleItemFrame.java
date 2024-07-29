package com.aearost.aranarthcore.recipes;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ShapedRecipe;
import org.bukkit.plugin.Plugin;

import com.aearost.aranarthcore.items.InvisibleItemFrame;

public class RecipeInvisibleItemFrame {

	public RecipeInvisibleItemFrame(Plugin plugin) {
		createRecipe(plugin);
	}

	/**
	 * Creates the recipe for getting an Invisible Item Frame.
	 * @param plugin The plugin.
	 */
	private void createRecipe(Plugin plugin) {
		NamespacedKey nk = new NamespacedKey(plugin, "AC_INVISIBLE_ITEM_FRAME");
		ShapedRecipe recipe = new ShapedRecipe(nk, InvisibleItemFrame.getInvisibleItemFrame());

		recipe.shape("LGL", "GIG", "LGL");

		recipe.setIngredient('L', Material.LAPIS_LAZULI);
		recipe.setIngredient('G', Material.GLOWSTONE_DUST);
		recipe.setIngredient('I', Material.ITEM_FRAME);
		
		Bukkit.addRecipe(recipe);
	}
		
	
}
