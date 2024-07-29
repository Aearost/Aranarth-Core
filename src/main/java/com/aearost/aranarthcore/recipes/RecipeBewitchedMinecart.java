package com.aearost.aranarthcore.recipes;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ShapedRecipe;
import org.bukkit.plugin.Plugin;

import com.aearost.aranarthcore.items.BewitchedMinecart;

public class RecipeBewitchedMinecart {

	public RecipeBewitchedMinecart(Plugin plugin) {
		createRecipe(plugin);
	}

	/**
	 * Creates the recipe for getting a Bewitched Minecart.
	 * @param plugin The plugin.
	 */
	private void createRecipe(Plugin plugin) {
		NamespacedKey nk = new NamespacedKey(plugin, "AC_BEWITCHED_MINECART");
		ShapedRecipe recipe = new ShapedRecipe(nk, BewitchedMinecart.getBewitchedMinecart());

		recipe.shape("I I", "III", "RDR");

		recipe.setIngredient('I', Material.IRON_INGOT);
		recipe.setIngredient('R', Material.REDSTONE);
		recipe.setIngredient('D', Material.DIAMOND);
		
		Bukkit.addRecipe(recipe);
	}
		
	
}
