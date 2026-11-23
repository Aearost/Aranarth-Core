package com.aearost.aranarthcore.recipes.aranarthium;

import com.aearost.aranarthcore.items.aranarthium.ingots.AranarthiumEcho;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ShapedRecipe;
import org.bukkit.plugin.Plugin;

public class RecipeEchoAranarthium {

	public RecipeEchoAranarthium(Plugin plugin) {
		createRecipe(plugin);
	}

	/**
	 * Creates the recipe for getting Echo Aranarthium.
	 * @param plugin The plugin.
	 */
	private void createRecipe(Plugin plugin) {
		NamespacedKey nk = new NamespacedKey(plugin, "AC_ECHO_ARANARTHIUM");
		ShapedRecipe recipe = new ShapedRecipe(nk, new AranarthiumEcho().getItem());

		recipe.shape("SHS", "XEX", "LCL");

		recipe.setIngredient('S', Material.SCULK_SENSOR);
		recipe.setIngredient('H', Material.SCULK_SHRIEKER);
		recipe.setIngredient('X', Material.ECHO_SHARD);
		recipe.setIngredient('E', Material.ECHO_SHARD);
		recipe.setIngredient('L', Material.LAPIS_BLOCK);
		recipe.setIngredient('C', Material.SCULK_CATALYST);

		Bukkit.addRecipe(recipe);
	}

}
