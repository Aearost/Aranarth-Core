package com.aearost.aranarthcore.recipes.aranarthium;

import com.aearost.aranarthcore.items.aranarthium.ingots.EnflamedAranarthium;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ShapedRecipe;
import org.bukkit.plugin.Plugin;

public class RecipeEnflamedAranarthium {

	public RecipeEnflamedAranarthium(Plugin plugin) {
		createRecipe(plugin);
	}

	/**
	 * Creates the recipe for getting Enflamed Aranarthium.
	 * @param plugin The plugin.
	 */
	private void createRecipe(Plugin plugin) {
		NamespacedKey nk = new NamespacedKey(plugin, "AC_ENFLAMED_ARANARTHIUM");
		ShapedRecipe recipe = new ShapedRecipe(nk, EnflamedAranarthium.getEnflamedAranarthiumIngot());

		recipe.shape("MLM", "BEB", "RBR");

		recipe.setIngredient('M', Material.MAGMA_CREAM);
		recipe.setIngredient('L', Material.LAVA_BUCKET);
		recipe.setIngredient('E', Material.ECHO_SHARD);
		recipe.setIngredient('B', Material.BLAZE_POWDER);
		recipe.setIngredient('R', Material.RESIN_CLUMP);

		Bukkit.addRecipe(recipe);
	}
		
	
}
